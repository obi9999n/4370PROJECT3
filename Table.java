
/****************************************************************************************
 * @file  Table.java
 *
 * @author   John Miller
 */

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.Boolean.*;
import static java.lang.System.out;

/****************************************************************************************
 * This class implements relational database tables (including attribute names,
 * domains
 * and a list of tuples. Five basic relational algebra operators are provided:
 * project,
 * select, union, minus and join. The insert data manipulation operator is also
 * provided.
 * Missing are update and delete data manipulation operators.
 */
public class Table
        implements Serializable {
    /**
     * Relative path for storage directory
     */
    private static final String DIR = "store" + File.separator;

    /**
     * Filename extension for database files
     */
    private static final String EXT = ".dbf";

    /**
     * Counter for naming temporary tables.
     */
    private static int count = 0;

    /**
     * Table name.
     */
    private final String name;

    /**
     * Array of attribute names.
     */
    private final String[] attribute;

    /**
     * Array of attribute domains: a domain may be
     * integer types: Long, Integer, Short, Byte
     * real types: Double, Float
     * string types: Character, String
     */
    private final Class[] domain;

    /**
     * Collection of tuples (data storage).
     */
    private final List<Comparable[]> tuples;

    public List<Comparable[]> getTuples() {
        return tuples;
    }

    /**
     * Primary key.
     */
    private final String[] key;

    /**
     * Index into tuples (maps key to tuple number).
     */
    private final Map<KeyType, Comparable[]> index;

    /**
     * The supported map types.
     */
    private enum MapType {
        NO_MAP, TREE_MAP, LINHASH_MAP, BPTREE_MAP
    }

    /**
     * The map type to be used for indices. Change as needed.
     */
    private static final MapType mType = MapType.TREE_MAP;

    /************************************************************************************
     * Make a map (index) given the MapType.
     */
    private static Map<KeyType, Comparable[]> makeMap() {
        return switch (mType) {
            case TREE_MAP -> new TreeMap<>();
            case LINHASH_MAP -> new LinHashMap<>(KeyType.class, Comparable[].class);
            default -> null;
        }; // switch
    } // makeMap

    // -----------------------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------------------

    /************************************************************************************
     * Construct an empty table from the meta-data specifications.
     *
     * @param _name      the name of the relation
     * @param _attribute the string containing attributes names
     * @param _domain    the string containing attribute domains (data types)
     * @param _key       the primary key
     */
    public Table(String _name, String[] _attribute, Class[] _domain, String[] _key) {
        name = _name;
        attribute = _attribute;
        domain = _domain;
        key = _key;
        tuples = new ArrayList<>();
        index = makeMap();

    } // primary constructor

    /************************************************************************************
     * Construct a table from the meta-data specifications and data in _tuples list.
     *
     * @param _name      the name of the relation
     * @param _attribute the string containing attributes names
     * @param _domain    the string containing attribute domains (data types)
     * @param _key       the primary key
     * @param _tuples    the list of tuples containing the data
     */
    public Table(String _name, String[] _attribute, Class[] _domain, String[] _key,
            List<Comparable[]> _tuples) {
        name = _name;
        attribute = _attribute;
        domain = _domain;
        key = _key;
        tuples = _tuples;
        index = makeMap();
    } // constructor

    /************************************************************************************
     * Construct an empty table from the raw string specifications.
     *
     * @param _name      the name of the relation
     * @param attributes the string containing attributes names
     * @param domains    the string containing attribute domains (data types)
     * @param _key       the primary key
     */
    public Table(String _name, String attributes, String domains, String _key) {
        this(_name, attributes.split(" "), findClass(domains.split(" ")), _key.split(" "));

        out.println("DDL> create table " + name + " (" + attributes + ")");
    } // constructor

    // ----------------------------------------------------------------------------------
    // Public Methods
    // ----------------------------------------------------------------------------------

    /************************************************************************************
     * Project the tuples onto a lower dimension by keeping only the given
     * attributes.
     * Check whether the original key is included in the projection.
     *
     * #usage movie.project ("title year studioNo")
     *
     * @param attributes the attributes to project onto
     * @return a table of projected tuples
     */
    public Table project(String attributes) {
        out.println("RA> " + name + ".project (" + attributes + ")");
        String[] attrs = attributes.split(" ");
        Class[] colDomain = extractDom(match(attrs), domain);
        String[] newKey = (Arrays.asList(attrs).containsAll(Arrays.asList(key))) ? key : attrs;

        List<Comparable[]> rows = new ArrayList<>();
        Map<String, Integer> m = new HashMap<String, Integer>();

        Map<String, Integer> attributeIdx = new HashMap<String, Integer>();

        for (int a = 0; a < this.attribute.length; a++) {
            String attributeA = this.attribute[a];
            attributeIdx.put(attributeA, a);
        }
        for (int i = 0; i < this.attribute.length; i++) {
            m.put(this.attribute[i], i);
        }
        // for each tuple in the designated table...
        for (int i = 0; i < this.tuples.size(); i++) {
            // create a new entry that has the size of the "attrs" array, representing
            // each column in the projection
            Comparable[] newEntry = new Comparable[attrs.length];
            for (int j = 0; j < attrs.length; j++) {
                // recreating the key for that tuple using KeyType and Comparable[]
                Comparable[] keyGenerator = new Comparable[this.key.length];
                int v = 0;
                while (v < this.key.length) {
                    keyGenerator[v] = this.tuples.get(i)[m.get(this.key[v])];
                    v++;
                }

                KeyType key = new KeyType(keyGenerator);

                // use that key to find the correct value using the "index" treeMap
                newEntry[j] = index.get(key)[attributeIdx.get(attrs[j])];
            }
            // add the newly created entry "newEntry" to rows
            rows.add(newEntry);
        }
        // use "rows" to create the newly returned table
        return new Table(name + count++, attrs, colDomain, newKey, rows);
    } // project

    /************************************************************************************
     * Select the tuples satisfying the given predicate (Boolean function).
     *
     * #usage movie.select (t -> t[movie.col("year")].equals (1977))
     *
     * @param predicate the check condition for tuples
     * @return a table with tuples satisfying the predicate
     */
    public Table select(Predicate<Comparable[]> predicate) {
        out.println("RA> " + name + ".select (" + predicate + ")");

        return new Table(name + count++, attribute, domain, key,
                tuples.stream().filter(t -> predicate.test(t))
                        .collect(Collectors.toList()));
    } // select

    /************************************************************************************
     * Select the tuples satisfying the given key predicate (key = value). Use an
     * index
     * (Map) to retrieve the tuple with the given key value.
     *
     * @param keyVal the given key value
     * @return a table with the tuple satisfying the key predicate
     */
    public Table select(KeyType keyVal) {
        out.println("RA> " + name + ".select (" + keyVal + ")");

        List<Comparable[]> rows = new ArrayList<>();
        // use keyVal to get correct entry from table, add it to rows

        for (Comparable[] entry : this.tuples) {
            if (this.index.get(keyVal) == entry) {
                rows.add(entry);
                break;
            }
        }
        return new Table(name + count++, attribute, domain, key, rows);
    } // select

    /************************************************************************************
     * Union this table and table2. Check that the two tables are compatible.
     *
     * #usage movie.union (show)
     *
     * @param table2 the rhs table in the union operation
     * @return a table representing the union
     */
    public Table union(Table table2) {
        out.println("RA> " + name + ".union (" + table2.name + ")");
        if (!compatible(table2))
            return null;

        List<Comparable[]> rows = new ArrayList<>();

        // create set of every tuple from each table (this.Table & table2)
        Set<Comparable[]> tableSet = new HashSet<Comparable[]>();
        for (Comparable[] entry : this.tuples) {
            tableSet.add(entry);
        }
        for (Comparable[] entry2 : table2.tuples) {
            tableSet.add(entry2);
        }

        // duplicates are automatically handled in HashSets
        /*
         * add every tuple in "tableSet" to table
         */
        for (Comparable[] e : tableSet) {
            rows.add(e);
        }
        return new Table(name + count++, attribute, domain, key, rows);
    } // union

    /**
     * Worker function to compare difference for Minus compariso
     * 
     * @author Nkomo
     * 
     * @param subj      the tuple that is to be checked if exists in Diff table.
     * @param diffTable the table that contains the list of tuples to be minused
     */
    private BiPredicate<Comparable[], Table> kEEP_BiPredicate = (subj, diffTable) -> {
        return (diffTable.getTuples().stream()
                .filter(t -> t.equals(subj))
                .findFirst()
                .orElse(null) != null) ? false : true;
    };

    /************************************************************************************
     * Take the difference of this table and table2. Check that the two tables are
     * compatible.
     *
     * #usage movie.minus (show)
     *
     * @param table2 The rhs table in the minus operation
     * @return a table representing the difference
     */
    public Table minus(Table table2) {
        out.println("RA> " + name + ".minus (" + table2.name + ")");
        if (!compatible(table2))
            return null;

        /*
         * for every entry in the table2 check if the entry fulfills
         * the kEEP_BiPredicate logic, if true, add tuple to the stream output
         */

        return new Table(name + count++, attribute, domain, key,
                this.tuples.stream()
                        .filter(t -> kEEP_BiPredicate.test(t, table2))
                        .collect(Collectors.toList()));
    } // minus

    // function for removing duplicates
    public Comparable[] removeDups(Comparable[] arr) {
        Map<Comparable, Integer> map = new HashMap<Comparable, Integer>();
        for (int i = 0; i < arr.length; i++) {
            Comparable c = arr[i];
            if (map.containsKey(c) == false) {
                map.put(c, i);
            }
        }

        Comparable[] compArr = new Comparable[map.size()];

        for (Map.Entry<Comparable, Integer> entry : map.entrySet()) {
            compArr[entry.getValue()] = entry.getKey();
        }

        return compArr;
    }

    /************************************************************************************
     * Join this table and table2 by performing an "equi-join". Tuples from both
     * tables
     * are compared requiring attributes1 to equal attributes2. Disambiguate
     * attribute
     * names by append "2" to the end of any duplicate attribute name. Implement
     * using
     * a Nested Loop Join algorithm.
     *
     * #usage movie.join ("studioNo", "name", studio)
     *
     * @param attribute1 the attributes of this table to be compared (Foreign Key)
     * @param attribute2 the attributes of table2 to be compared (Primary Key)
     * @param table2     the rhs table in the join operation
     * @return a table with tuples satisfying the equality predicate
     */
    public Table join(String attributes1, String attributes2, Table table2) {
        out.println("RA> " + name + ".join (" + attributes1 + ", " + attributes2 + ", "
                + table2.name + ")");

        List<Comparable[]> rows = new ArrayList<>();
        String[] a_attrs = attributes1.split(" ");
        String[] b_attrs = attributes2.split(" ");

        // maps for finding key attribute indexes for dynamic key generation
        Map<String, Integer> kA = new HashMap<String, Integer>();
        Map<String, Integer> kB = new HashMap<String, Integer>();
        for (int i = 0; i < this.attribute.length; i++) {
            kA.put(this.attribute[i], i);
        }

        for (int i = 0; i < table2.attribute.length; i++) {
            kB.put(table2.attribute[i], i);
        }

        // maps to help find index of respective attribute in table
        Map<String, Integer> attributeIdxA = new HashMap<String, Integer>();
        Map<String, Integer> attributeIdxB = new HashMap<String, Integer>();
        for (int a = 0; a < this.attribute.length; a++) {
            String attributeA = this.attribute[a];
            attributeIdxA.put(attributeA, a);
        }
        for (int b = 0; b < table2.attribute.length; b++) {
            String attributeB = table2.attribute[b];
            attributeIdxB.put(attributeB, b);
        }

        for (int i = 0; i < this.tuples.size(); i++) {
            // get tuple from rhs table
            Comparable[] tupleA = this.tuples.get(i);
            for (int j = 0; j < table2.tuples.size(); j++) {
                // get tuple from lhs table
                Comparable[] tupleB = table2.tuples.get(j);
                // get keys for entry of tupleA and tupleb, dynamically genrerated
                Comparable[] keyGeneratorA = new Comparable[this.key.length];
                Comparable[] keyGeneratorB = new Comparable[table2.key.length];

                int v = 0;
                while (v < this.key.length) {
                    keyGeneratorA[v] = this.tuples.get(i)[kA.get(this.key[v])];
                    v++;
                }

                int w = 0;
                while (w < table2.key.length) {
                    keyGeneratorB[w] = table2.tuples.get(j)[kB.get(table2.key[w])];
                    w++;
                }
                KeyType keyA = new KeyType(keyGeneratorA);
                KeyType keyB = new KeyType(keyGeneratorB);

                // check attribute predicate for each attribute array element
                int checks = a_attrs.length;
                int x = 0;
                int y = 0;
                while (x < a_attrs.length) {
                    if (index.get(keyA)[attributeIdxA.get(a_attrs[x])] == table2.index.get(keyB)[attributeIdxB
                            .get(b_attrs[y])]) {
                        checks--;
                    }
                    x++;
                    y++;
                }
                if (checks == 0)
                    rows.add(ArrayUtil.concat(tupleA, tupleB));
            }
        }

        return new Table(name + count++, ArrayUtil.concat(attribute, table2.attribute),
                ArrayUtil.concat(domain, table2.domain), key, rows);
    } // join

    /************************************************************************************
     * Join this table and table2 by performing an "equi-join". Same as above, but
     * implemented
     * using an Index Join algorithm.
     *
     * @param attribute1 the attributes of this table to be compared (Foreign Key)
     * @param attribute2 the attributes of table2 to be compared (Primary Key)
     * @param table2     the rhs table in the join operation
     * @return a table with tuples satisfying the equality predicate
     */
    public Table i_join(String attributes1, String attributes2, Table table2) {
        return null;
    } // i_join

    /************************************************************************************
     * Join this table and table2 by performing an "equi-join". Same as above, but
     * implemented
     * using a Hash Join algorithm.
     *
     * @param attribute1 the attributes of this table to be compared (Foreign Key)
     * @param attribute2 the attributes of table2 to be compared (Primary Key)
     * @param table2     the rhs table in the join operation
     * @return a table with tuples satisfying the equality predicate
     */
    public Table h_join(String attributes1, String attributes2, Table table2) {

        // D O N O T I M P L E M E N T

        return null;
    } // h_join

    /************************************************************************************
     * Join this table and table2 by performing an "natural join". Tuples from both
     * tables
     * are compared requiring common attributes to be equal. The duplicate column is
     * also
     * eliminated.
     *
     * #usage movieStar.join (starsIn)
     *
     * @param table2 the rhs table in the join operation
     * @return a table with tuples satisfying the equality predicate
     */
    public Table join(Table table2) {
        out.println("RA> " + name + ".join (" + table2.name + ")");

        List<Comparable[]> rows = new ArrayList<>();

        // get list of attrubutes that will be compared between both tables
        Set<String> tableattr = new HashSet<String>();
        List<String> compareAtrributes = new ArrayList<String>();
        for (String s : this.attribute) {
            tableattr.add(s);
        }
        for (int i = 0; i < table2.attribute.length; i++) {
            if (tableattr.contains(table2.attribute[i]))
                compareAtrributes.add(table2.attribute[i]);
        }

        if (compareAtrributes.size() == 0)
            return new Table(name + count++, ArrayUtil.concat(attribute, table2.attribute),
                    ArrayUtil.concat(domain, table2.domain), key, rows);

        // get new attribute list that excludes duplicates
        for (int i = 0; i < table2.attribute.length; i++) {
            if (!tableattr.contains(table2.attribute[i]))
                tableattr.add(table2.attribute[i]);
        }
        String[] newAttr = new String[tableattr.size()];
        int x = 0;

        for (String str : this.attribute) {
            if (tableattr.contains(str)) {
                tableattr.remove(str);
                newAttr[x] = str;
            }
            x++;
        }

        for (String strg : table2.attribute) {
            if (tableattr.contains(strg)) {
                newAttr[x] = strg;
            }
            x++;
        }

        // get new domain and key array based on "newAttr"
        String[] newKey = (Arrays.asList(newAttr).containsAll(Arrays.asList(key))) ? key : newAttr;

        // maps for finding key attribute indexes for dynamic key generation
        Map<String, Integer> kA = new HashMap<String, Integer>();
        Map<String, Integer> kB = new HashMap<String, Integer>();
        for (int i = 0; i < this.attribute.length; i++) {
            kA.put(this.attribute[i], i);
        }
        for (int i = 0; i < table2.attribute.length; i++) {
            kB.put(table2.attribute[i], i);
        }

        // maps to help find index of respective attribute in table
        Map<String, Integer> attributeIdxA = new HashMap<String, Integer>();
        Map<String, Integer> attributeIdxB = new HashMap<String, Integer>();
        for (int a = 0; a < this.attribute.length; a++) {
            String attributeA = this.attribute[a];
            attributeIdxA.put(attributeA, a);
        }
        for (int b = 0; b < table2.attribute.length; b++) {
            String attributeB = table2.attribute[b];
            attributeIdxB.put(attributeB, b);
        }

        for (int i = 0; i < this.tuples.size(); i++) {
            // get tuple from rhs table
            Comparable[] tupleA = this.tuples.get(i);
            for (int j = 0; j < table2.tuples.size(); j++) {
                // get tuple from lhs table
                Comparable[] tupleB = table2.tuples.get(j);
                // get keys for entry of tupleA and tupleb, dynamically genrerated
                Comparable[] keyGeneratorA = new Comparable[this.key.length];
                Comparable[] keyGeneratorB = new Comparable[table2.key.length];
                int v = 0;
                while (v < this.key.length) {
                    keyGeneratorA[v] = this.tuples.get(i)[kA.get(this.key[v])];
                    v++;
                }
                int w = 0;
                while (w < table2.key.length) {
                    keyGeneratorB[w] = table2.tuples.get(j)[kB.get(table2.key[w])];
                    w++;
                }
                KeyType keyA = new KeyType(keyGeneratorA);
                KeyType keyB = new KeyType(keyGeneratorB);

                // check attribute predicate for each attribute array element
                int checks = compareAtrributes.size();
                int z = 0;
                while (z < compareAtrributes.size()) {
                    if (index.get(keyA)[attributeIdxA.get(compareAtrributes.get(z))] == table2.index
                            .get(keyB)[attributeIdxB
                                    .get(compareAtrributes.get(z))]) {
                        checks--;
                    }
                    z++;
                }
                if (checks == 0)
                    rows.add(removeDups(ArrayUtil.concat(tupleA, tupleB)));
            }
        }

        String[] finalAttribute = new String[compareAtrributes.size()];
        int t = 0;

        for (String c : compareAtrributes) {
            finalAttribute[t] = c;
            t++;
        }

        Class[] colDomain = extractDom(match(finalAttribute), domain);
        // FIX - eliminate duplicate columns
        return new Table(name + count++, newAttr,
                colDomain, newKey, rows);
    } // join

    /************************************************************************************
     * Return the column position for the given attribute name.
     *
     * @param attr the given attribute name
     * @return a column position
     */
    public int col(String attr) {
        for (var i = 0; i < attribute.length; i++) {
            if (attr.equals(attribute[i]))
                return i;
        } // for

        return -1; // not found
    } // col

    /************************************************************************************
     * Insert a tuple to the table.
     *
     * #usage movie.insert ("'Star_Wars'", 1977, 124, "T", "Fox", 12345)
     *
     * @param tup the array of attribute values forming the tuple
     * @return whether insertion was successful
     */
    public boolean insert(Comparable[] tup) {
        out.println("DML> insert into " + name + " values ( " + Arrays.toString(tup) + " )");

        if (typeCheck(tup)) {
            tuples.add(tup);
            var keyVal = new Comparable[key.length];
            var cols = match(key);
            for (var j = 0; j < keyVal.length; j++)
                keyVal[j] = tup[cols[j]];
            if (mType != MapType.NO_MAP)
                index.put(new KeyType(keyVal), tup);
            return true;
        } else {
            return false;
        } // if
    } // insert

    /************************************************************************************
     * Get the name of the table.
     *
     * @return the table's name
     */
    public String getName() {
        return name;
    } // getName

    /************************************************************************************
     * Print this table.
     */
    public void print() {
        out.println("\n Table " + name);
        out.print("|-");
        out.print("---------------".repeat(attribute.length));
        out.println("-|");
        out.print("| ");
        for (var a : attribute)
            out.printf("%15s", a);
        out.println(" |");
        out.print("|-");
        out.print("---------------".repeat(attribute.length));
        out.println("-|");
        for (var tup : tuples) {
            out.print("| ");
            for (var attr : tup)
                out.printf("%15s", attr);
            out.println(" |");
        } // for
        out.print("|-");
        out.print("---------------".repeat(attribute.length));
        out.println("-|");
    } // print

    /************************************************************************************
     * Print this table's index (Map).
     */
    public void printIndex() {
        out.println("\n Index for " + name);
        out.println("-------------------");
        if (mType != MapType.NO_MAP) {
            for (var e : index.entrySet()) {
                out.println(e.getKey() + " -> " + Arrays.toString(e.getValue()));
            } // for
        } // if
        out.println("-------------------");
    } // printIndex

    /************************************************************************************
     * Load the table with the given name into memory.
     *
     * @param name the name of the table to load
     */
    public static Table load(String name) {
        Table tab = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DIR + name + EXT));
            tab = (Table) ois.readObject();
            ois.close();
        } catch (IOException ex) {
            out.println("load: IO Exception");
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            out.println("load: Class Not Found Exception");
            ex.printStackTrace();
        } // try
        return tab;
    } // load

    /************************************************************************************
     * Save this table in a file.
     */
    public void save() {
        try {
            var oos = new ObjectOutputStream(new FileOutputStream(DIR + name + EXT));
            oos.writeObject(this);
            oos.close();
        } catch (IOException ex) {
            out.println("save: IO Exception");
            ex.printStackTrace();
        } // try
    } // save

    // ----------------------------------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------------------------------

    /************************************************************************************
     * Determine whether the two tables (this and table2) are compatible, i.e., have
     * the same number of attributes each with the same corresponding domain.
     *
     * @param table2 the rhs table
     * @return whether the two tables are compatible
     */
    private boolean compatible(Table table2) {
        if (domain.length != table2.domain.length) {
            out.println("compatible ERROR: table have different arity");
            return false;
        } // if
        for (var j = 0; j < domain.length; j++) {
            if (domain[j] != table2.domain[j]) {
                out.println("compatible ERROR: tables disagree on domain " + j);
                return false;
            } // if
        } // for
        return true;
    } // compatible

    /************************************************************************************
     * Match the column and attribute names to determine the domains.
     *
     * @param column the array of column names
     * @return an array of column index positions
     */
    private int[] match(String[] column) {
        int[] colPos = new int[column.length];

        for (var j = 0; j < column.length; j++) {
            var matched = false;
            for (var k = 0; k < attribute.length; k++) {
                if (column[j].equals(attribute[k])) {
                    matched = true;
                    colPos[j] = k;
                } // for
            } // for
            if (!matched) {
                out.println("match: domain not found for " + column[j]);
            } // if
        } // for

        return colPos;
    } // match

    /************************************************************************************
     * Extract the attributes specified by the column array from tuple t.
     *
     * @param t      the tuple to extract from
     * @param column the array of column names
     * @return a smaller tuple extracted from tuple t
     */
    private Comparable[] extract(Comparable[] t, String[] column) {
        var tup = new Comparable[column.length];
        var colPos = match(column);
        for (var j = 0; j < column.length; j++)
            tup[j] = t[colPos[j]];
        return tup;
    } // extract

    /************************************************************************************
     * Check the size of the tuple (number of elements in list) as well as the type
     * of
     * each value to ensure it is from the right domain.
     *
     * @param t the tuple as a list of attribute values
     * @return whether the tuple has the right size and values that comply
     *         with the given domains
     */
    private boolean typeCheck(Comparable[] t) {
        // T O B E I M P L E M E N T E D

        return true;
    } // typeCheck

    /************************************************************************************
     * Find the classes in the "java.lang" package with given names.
     *
     * @param className the array of class name (e.g., {"Integer", "String"})
     * @return an array of Java classes
     */
    private static Class[] findClass(String[] className) {
        var classArray = new Class[className.length];

        for (var i = 0; i < className.length; i++) {
            try {
                classArray[i] = Class.forName("java.lang." + className[i]);
            } catch (ClassNotFoundException ex) {
                out.println("findClass: " + ex);
            } // try
        } // for

        return classArray;
    } // findClass

    /************************************************************************************
     * Extract the corresponding domains.
     *
     * @param colPos the column positions to extract.
     * @param group  where to extract from
     * @return the extracted domains
     */
    private Class[] extractDom(int[] colPos, Class[] group) {
        var obj = new Class[colPos.length];

        for (var j = 0; j < colPos.length; j++) {
            obj[j] = group[colPos[j]];
        } // for

        return obj;
    } // extractDom

} // Table class
