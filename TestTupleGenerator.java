 
/*****************************************************************************************
 * @file  TestTupleGenerator.java
 *
 * @author   Sadiq Charaniya, John Miller
 */

import static java.lang.System.out;

/*****************************************************************************************
 * This class tests the TupleGenerator on the Student Registration Database defined in the
 * Kifer, Bernstein and Lewis 2006 database textbook (see figure 3.6).  The primary keys
 * (see figure 3.6) and foreign keys (see example 3.2.2) are as given in the textbook.
 */
public class TestTupleGenerator
{
    private static Table student;
    private static Table professor;
    private static Table course;
    private static Table teaching;
    private static Table transcript;

    /*************************************************************************************
     * The main method is the driver for TestGenerator.
     * @param args  the command-line arguments
     */
    public static void main (String [] args)
    {
        var test = new TupleGeneratorImpl ();

        
        
        test.addRelSchema ("Student",
                           "id name address status",
                           "Integer String String String",
                           "id",
                           null);
        
        test.addRelSchema ("Professor",
                           "id name deptId",
                           "Integer String String",
                           "id",
                           null);
        
        test.addRelSchema ("Course",
                           "crsCode deptId crsName descr",
                           "String String String String",
                           "crsCode",
                           null);
        
        test.addRelSchema ("Teaching",
                           "crsCode semester profId",
                           "String String Integer",
                           "crsCode semester",
                           new String [][] {{ "profId", "Professor", "id" },
                                            { "crsCode", "Course", "crsCode" }});
        
        test.addRelSchema ("Transcript",
                           "studId crsCode semester grade",
                           "Integer String String String",
                           "studId crsCode semester",
                           new String [][] {{ "studId", "Student", "id"},
                                            { "crsCode", "Course", "crsCode" },
                                            { "crsCode semester", "Teaching", "crsCode semester" }});

        var tables = new String [] { "Student", "Professor", "Course", "Teaching", "Transcript" };
        var tups   = new int [] { 10000, 1000, 2000, 50000, 5000 };
    
        var resultTest = test.generate (tups);
        init(resultTest);
        
    } // main
    
    private static void init(Comparable[][][] resultTest) 
    {
        student = new Table(
                "Student",
                "id name address status",
                "Integer String String String",
                "id");

        professor = new Table(
                "Professor",
                "id name deptId",
                "Integer String String",
                "id");
        
        course = new Table(
                "Course",
                "crsCode deptId crsName descr",
                "String String String String",
                "crsCode");
        
        teaching = new Table(
                "Teaching",
                "crsCode semester profId",
                "String String Integer",
                "crsCode semester");

        transcript = new Table(
            "Transcript",
            "studId crsCode semester grade",
            "Integer String String String",
            "studId crsCode semester");

        Table ref;
        for (var i = 0; i < resultTest.length; i++) {
            switch (i) {
                case 0 -> ref = student;
                case 1 -> ref = professor;
                case 2 -> ref = course;
                case 3 -> ref = teaching;
                case 4 -> ref = transcript;
                default -> ref = null;
            } // switch
            for (var j = 0; j < resultTest [i].length; j++) {
                ref.insert(resultTest [i][j]);
                // for (var k = 0; k < resultTest [i][j].length; k++) {
                //     out.print (resultTest [i][j][k] + ",");
                    
                // } // for
                out.println ();
            } // for
            out.println ();
        } // for
    }


    private void run(Comparable[][][] tuples)
    {
        // populate tables
        // init();
    }

} // TestTupleGenerator

