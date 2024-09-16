//9/16/24
package edu.montana.csci.csci440.homeworks.hwk1;

import edu.montana.csci.csci440.DBTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class Homework1 extends DBTest {

    @Test
    /*
     * Write a query in the string below that returns all artists that have an 'A' in their name
     */
    void selectArtistsWhoseNameHasAnAInIt(){
        String query = """
          SELECT * FROM artists
          WHERE  name LIKE '%a%'
          """;
        List<Map<String, Object>> results = exec(query);
        assertEquals(211, results.size());
    }

    @Test
    /*
     * Write a query in the string below that returns all artists that have more than one album using an IN
     */
    void selectAllArtistsWithMoreThanOneAlbum(){
        String query = """
          SELECT * FROM artists WHERE ArtistId IN(
            SELECT ArtistID
            FROM albums
            GROUP BY ArtistID
            HAVING COUNT(AlbumID) > 1
          )
          """;
        List<Map<String, Object>> results = exec(query);
        assertEquals(56, results.size());
        assertEquals("AC/DC", results.get(0).get("Name"));
    }

    @Test
        /*
         * Write a query in the string below that returns all tracks longer than six minutes along with the
         * album and artist name
         */
    void selectTheTrackAndAlbumAndArtistForAllTracksLongerThanSixMinutes() {
        String query = """
                SELECT tracks.Name as TrackName, albums.Title as AlbumTitle, artists.Name as ArtistsName
                FROM tracks
                JOIN albums ON tracks.AlbumID = albums.AlbumID
                JOIN artists ON albums.ArtistID = artists.ArtistID
                WHERE tracks.Milliseconds > 6 * 60000
                """;
        List<Map<String, Object>> results = exec(query);

        assertEquals(623, results.size());
    }

}
