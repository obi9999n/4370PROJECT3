
/*****************************************************************************************
 * @file  MovieDB.java
 *
 * @author   John Miller
 */

import static java.lang.System.out;

/*****************************************************************************************
 * The MovieDB class makes a Movie Database. It serves as a template for making
 * other
 * databases. See "Database Systems: The Complete Book", second edition, page 26
 * for more
 * information on the Movie Database schema.
 */
class MovieDB {
    /*************************************************************************************
     * Main method for creating, populating and querying a Movie Database.
     * 
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        out.println();

        Table movie = new Table("movie", "title year length genre studioName producerNo",
                "String Integer Integer String String Integer", "title year");

        Table movieB = new Table("movie", "title year length genre studioName producerNo dummyMovieB",
                "String Integer Integer String String Integer", "title year");

        Table cinema = new Table("cinema", "title year length genre studioName producerNo",
                "String Integer Integer String String Integer", "title year");

        Table movieStar = new Table("movieStar", "name address gender birthdate",
                "String String Character String", "name");

        Table starsIn = new Table("starsIn", "movieTitle movieYear starName name",
                "String Integer String String", "movieTitle movieYear starName");

        Table movieExec = new Table("movieExec", "certNo name address fee",
                "Integer String String Float", "certNo");

        Table studio = new Table("studio", "name address presNo",
                "String String Integer", "name");

        Table studioB = new Table("studio", "name address presNo dummyStudioB",
                "String String Integer", "name");

        Comparable[] film0 = { "Star_Wars", 1977, 124, "sciFi", "Fox", 12345 };
        Comparable[] film1 = { "Star_Wars_2", 1980, 124, "sciFi", "Fox", 12345 };
        Comparable[] film2 = { "Rocky", 1985, 200, "action", "Universal", 12125 };
        Comparable[] film3 = { "Rambo", 1978, 100, "action", "Universal", 32355 };

        Comparable[] film7 = { "Star_Wars", 1977, 124, "sciFi", "Fox", 12345, "Dummy" };
        Comparable[] film8 = { "Star_Wars_2", 1980, 124, "sciFi", "Fox", 12345, "Dummy" };
        Comparable[] film9 = { "Rocky", 1985, 200, "action", "Universal", 12125, "Dummy" };
        Comparable[] film10 = { "Rambo", 1978, 100, "action", "Universal", 32355, "Dummy" };

        out.println();
        movie.insert(film0);
        movie.insert(film1);
        movie.insert(film2);
        movie.insert(film3);
        movie.print();

        out.println();
        movieB.insert(film7);
        movieB.insert(film8);
        movieB.insert(film9);
        movieB.insert(film10);
        movieB.print();

        Comparable[] film4 = { "Galaxy_Quest", 1999, 104, "comedy", "DreamWorks", 67890 };
        out.println();
        cinema.insert(film2);
        cinema.insert(film3);
        cinema.insert(film4);

        cinema.print();

        Comparable[] star0 = { "Carrie_Fisher", "Hollywood", 'F', "9/9/99" };
        Comparable[] star1 = { "Mark_Hamill", "Brentwood", 'M', "8/8/88" };
        Comparable[] star2 = { "Harrison_Ford", "Beverly_Hills", 'M', "7/7/77" };
        out.println();
        movieStar.insert(star0);
        movieStar.insert(star1);
        movieStar.insert(star2);
        movieStar.print();

        Comparable[] cast0 = { "Star_Wars", 1977, "Carrie_Fisher", "S_Spielberg" };
        Comparable[] cast1 = { "Star_Wars", 1977, "Some_Actor", "Tarantino" };
        Comparable[] cast2 = { "Star_Wars", 1977, "Some_Actor2", "nothing" };

        out.println();
        starsIn.insert(cast0);
        starsIn.insert(cast1);
        starsIn.insert(cast2);

        starsIn.print();

        Comparable[] exec0 = { 9999, "S_Spielberg", "Hollywood", 10000.00 };
        Comparable[] exec1 = { 9998, "Tarantino", "Hollywood", 10000.00 };
        Comparable[] exec2 = { 9996, "G_Lucas", "Hollywood", 10000.00 };

        out.println();
        movieExec.insert(exec0);
        movieExec.insert(exec1);
        movieExec.insert(exec2);

        movieExec.print();

        Comparable[] studio0 = { "Fox", "Los_Angeles", 7777 };
        Comparable[] studio1 = { "Universal", "Universal_City", 8888 };
        Comparable[] studio2 = { "DreamWorks", "Universal_City", 9999 };

        Comparable[] studio3 = { "Fox", "Los_Angeles", 7777, "Dummy" };
        Comparable[] studio4 = { "Universal", "Universal_City", 8888, "null" };
        Comparable[] studio5 = { "DreamWorks", "Universal_City", 9999, "dummy" };
        out.println();
        studio.insert(studio0);
        studio.insert(studio1);
        studio.insert(studio2);
        studio.print();

        out.println();
        studioB.insert(studio3);
        studioB.insert(studio4);
        studioB.insert(studio5);
        studio.print();

        movie.save();
        cinema.save();
        movieStar.save();
        starsIn.save();
        movieExec.save();
        studio.save();

        movieStar.printIndex();

        // --------------------- project: title year

        out.println();
        Table t_project = movie.project("title year genre");
        t_project.print();

        // --------------------- project: title 

        out.println();
        Table t_project2 = movie.project("title");
        t_project2.print();

        // --------------------- select: equals, &&

        out.println();
        Table t_select = movie.select(t -> t[movie.col("title")].equals("Star_Wars") &&
                t[movie.col("year")].equals(1977));
        t_select.print();

        // --------------------- select: <

        out.println();
        Table t_select2 = movie.select(t -> (Integer) t[movie.col("year")] < 1980);
        t_select2.print();

        // --------------------- select: <

        out.println();
        Table t_select3 = movie.select(
                t -> (Integer) t[movie.col("year")] < 1980 && (Integer) t[movie.col("year")] > 1960);
        t_select3.print();

        // --------------------- select: <

        out.println();
        Table t_select4 = movie.select(t -> (Integer) t[movie.col("year")] < 1980
                && t[movie.col("title")].equals("Star_Wars"));
        t_select4.print();

        // --------------------- select: <

        out.println();
        Table t_select5 = movie.select(t -> (Integer) t[movie.col("year")] < 1990
                || t[movie.col("title")].equals("Star_Wars"));
        t_select5.print();

        // --------------------- select: <

        out.println();
        Table t_select6 = movie.select(t -> (Integer) t[movie.col("year")] < 1990
                && !(t[movie.col("title")].equals("Star_Wars")));
        t_select6.print();

        // --------------------- indexed select: key

        out.println();
        Table t_iselect = movieStar.select(new KeyType("Harrison_Ford"));
        t_iselect.print();

        // --------------------- union: movie UNION cinema

        out.println();
        Table t_union = movie.union(cinema);
        t_union.print();

        // --------------------- minus: movie MINUS cinema

        out.println();
        Table t_minus = movie.minus(cinema);
        t_minus.print();

        // --------------------- equi-join: movie JOIN studio ON studioName = name

        out.println();
        Table t_join = movie.join("studioName", "name", studio);
        t_join.print();

        out.println();
        Table t_joinB = movieB.join("studioName dummyMovieB", "name dummyStudioB", studioB);
        t_joinB.print();

        // --------------------- natural join: movie JOIN studio

        out.println();
        // Table t_join2 = movie.join(cinema);
        // t_join2.print();

        out.println();
        // Table t_join3 = movieExec.join(starsIn);
        // t_join3.print();

    } // main

} // MovieDB class
