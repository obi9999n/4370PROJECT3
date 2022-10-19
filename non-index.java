       /************************************************************************************
     * Select the tuples satisfying the given key predicate (key = value). Retrieve
     * using a linear scan.
     *
     * @param keyVal
     *            the given key value
     * @return a table with the tuple satisfying the key predicate
     */
    public Table Non-Index-Select(KeyType keyVal) {
        ArrayList<Integer> keyIndexes = new ArrayList<Integer>();
        HashSet keyNames = new HashSet(Arrays.asList(key));
        for(int i = 0; i < attribute.length; i++){
            if(keyNames.contains(attribute[i])){
                keyIndexes.add(i);
            }
        }
        List<Comparable[]> rows = new ArrayList<>();
        for(int i = 0; i < tuples.size(); i++){
            Comparable[] currentTuple = tuples.get(i);
            List<Comparable> keyValues = new ArrayList<Comparable>();
            for(int j = 0; j < keyIndexes.size(); j++){
                keyValues.add(currentTuple[keyIndexes.get(j)]);
            }
            KeyType keyToCompare = new KeyType(keyValues.toArray(new Comparable[0]));
            if(keyToCompare.equals(keyVal)){
                rows.add(tuples.get(i));
            }
        }

        return new Table(name + count++, attribute, domain, key, rows);
    } // select 







/************************************************************************************
     * Join this table and table2 by performing an "equi-join".  Tuples from both tables
     * are compared requiring attributes1 to equal attributes2.  Disambiguate attribute
     * names by append "2" to the end of any duplicate attribute name.
     *
     * @author  Lin Zhao
     *
     * #usage movie.join ("studioNo", "name", studio)
     *
     * @param attribute1  the attributes of this table to be compared (Foreign Key)
     * @param attribute2  the attributes of table2 to be compared (Primary Key)
     * @param table2      the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table join (String attributes1, String attributes2, Table table2)
    {
        out.println ("RA> " + name + ".join (" + attributes1 + ", " + attributes2 + ", "
                                               + table2.name + ")");

        String [] t_attrs = attributes1.split (" ");
        String [] u_attrs = attributes2.split (" ");

        List <Comparable []> rows = new ArrayList <> ();

		if (t_attrs.length != u_attrs.length) {
			System.out.println("Cannot Perform Join Operator");
			return null;
		}
 		
		for (Comparable[] tuple1 : tuples) {
			for (Comparable[] tuple2 : table2.tuples) {
				
			    Comparable[] Attri1 = this.extract(tuple1, t_attrs);
			    Comparable[] Attri2 = table2.extract(tuple2, u_attrs);
			
			    boolean flag = true;
			    
				// Judge if attributes1 in table1 is equal to attributes2 in table 2
			    for (int i = 0; i < Attri1.length; i++) {
				
				    if (!Attri1[i].equals(Attri2[i])) {
						flag = false;
						break;
					}
				}

				// Concatenate tuples from table1&2 to form a new tuple
			    if (flag) {
				    Comparable[] join_tuple = ArrayUtil.concat(tuple1, tuple2);
				    rows.add(join_tuple);
				}
			}
		}

		// Disambiguate attribute names by append "2" to the end of any duplicate attribute name.
		// Here we just need to rename the attribute names in table2 then concatenate them to those in table1 
		String[] attribute2_new = table2.attribute;
		
		for (int j = 0; j < t_attrs.length; j++) {
			for (int k = 0; k < attribute2_new.length; ++k) {
				
				if (attribute2_new[k].equals(t_attrs[j])) {
					
					String tmp_attri = t_attrs[j] + "2"; 
					attribute2_new[j] = tmp_attri;
				}
			}
		}
		

		return new Table (name + count++, ArrayUtil.concat (attribute, attribute2_new),
				ArrayUtil.concat (domain, table2.domain), key, rows);
	} // join

