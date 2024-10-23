package edu.montana.csci.csci440.homeworks.hwk3;

import edu.montana.csci.csci440.DBTest;
import edu.montana.csci.csci440.model.Track;
import edu.montana.csci.csci440.util.DB;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Homework3 extends DBTest {

    @Test
    /*
     * Create a view tracksPlus to display the artist, song title, album, and genre for all tracks.
     */
    public void createTracksPlusView(){
        //TODO fill this in
        executeDDL("CREATE VIEW tracksPlus AS " +
                "SELECT t.TrackId, t.Name as TrackName, ar.Name as ArtistName, " +
                "al.Title as AlbumTitle, g.Name as GenreName " +
                "FROM tracks t " +
                "JOIN albums al ON t.AlbumId = al.AlbumId " +
                "JOIN artists ar ON al.ArtistId = ar.ArtistId " +
                "JOIN genres g ON t.GenreId = g.GenreId");

        List<Map<String, Object>> results = exec("SELECT * FROM tracksPlus ORDER BY TrackId");
        assertEquals(3503, results.size());
        assertEquals("Rock", results.get(0).get("GenreName"));
        assertEquals("AC/DC", results.get(0).get("ArtistName"));
        assertEquals("For Those About To Rock We Salute You", results.get(0).get("AlbumTitle"));
    }

    @Test
    /*
     * Create a table grammy_infos to track grammy information for an artist.  The table should include
     * a reference to the artist, the album (if the grammy was for an album) and the song (if the grammy was
     * for a song).  There should be a string column indicating if the artist was nominated or won.  Finally,
     * there should be a reference to the grammy_category table
     *
     * Create a table grammy_category
     */
    public void createGrammyInfoTable(){
        //TODO fill these in
        executeDDL("CREATE TABLE grammy_categories (" +
                "GrammyCategoryId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Name TEXT NOT NULL)");

        executeDDL("CREATE TABLE grammy_infos (" +
                "GrammyInfoId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ArtistId INTEGER NOT NULL, " +
                "AlbumId INTEGER, " +
                "TrackId INTEGER, " +
                "GrammyCategoryId INTEGER NOT NULL, " +
                "Status TEXT NOT NULL, " +
                "FOREIGN KEY(ArtistId) REFERENCES artists(ArtistId), " +
                "FOREIGN KEY(AlbumId) REFERENCES albums(AlbumId), " +
                "FOREIGN KEY(TrackId) REFERENCES tracks(TrackId), " +
                "FOREIGN KEY(GrammyCategoryId) REFERENCES grammy_categories(GrammyCategoryId))");

        // TEST CODE
        executeUpdate("INSERT INTO grammy_categories(Name) VALUES ('Greatest Ever');");
        Object categoryId = exec("SELECT GrammyCategoryId FROM grammy_categories").get(0).get("GrammyCategoryId");

        executeUpdate("INSERT INTO grammy_infos(ArtistId, AlbumId, TrackId, GrammyCategoryId, Status) VALUES (1, 1, 1, " + categoryId + ",'Won');");

        List<Map<String, Object>> results = exec("SELECT * FROM grammy_infos");
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).get("ArtistId"));
        assertEquals(1, results.get(0).get("AlbumId"));
        assertEquals(1, results.get(0).get("TrackId"));
        assertEquals(1, results.get(0).get("GrammyCategoryId"));
    }

    @Test
    /*
     * Bulk insert five categories of your choosing in the genres table
     */
    public void bulkInsertGenres(){
        Integer before = (Integer) exec("SELECT COUNT(*) as COUNT FROM genres").get(0).get("COUNT");

        //TODO fill this in
        executeUpdate("INSERT INTO genres(Name) VALUES " +
                "('blues'), " +
                "('jazz'), " +
                "('country'), " +
                "('house'), " +
                "('rap')");

        Integer after = (Integer) exec("SELECT COUNT(*) as COUNT FROM genres").get(0).get("COUNT");
        assertEquals(before + 5, after);
    }


}
