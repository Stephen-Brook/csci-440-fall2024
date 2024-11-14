package edu.montana.csci.csci440.homeworks.hwk5;

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

public class Homework5 extends DBTest {

    @Test
    /*
     * Select tracks that have been sold more than once (> 1)
     *
     * Select the albums that have tracks that have been sold more than once (> 1)
     *   NOTE: This is NOT the same as albums whose tracks have been sold more than once!
     *         An album could have had three tracks, each sold once, and should not be included
     *         in this result.  It should only include the albums of the tracks found in the first
     *         query.
     * */
    public void selectPopularTracksAndTheirAlbums() throws SQLException {

        // HINT: join to invoice items and do a group by/having to get the right answer
        List<Map<String, Object>> tracks = exec("SELECT t.TrackId" +
                " FROM tracks t" +
                " JOIN invoice_items ii ON t.TrackId = ii.TrackId" +
                " GROUP BY t.trackId" +
                " HAVING COUNT(ii.InvoiceId) > 1");
        assertEquals(256, tracks.size());

        // HINT: join to tracks and invoice items and do a group by/having to get the right answer
        //       note: you will need to use the DISTINCT operator to get the right result!
        List<Map<String, Object>> albums = exec("SELECT DISTINCT a.AlbumId" +
                " FROM albums a" +
                " JOIN tracks t on a.AlbumId = t.AlbumId" +
                " JOIN invoice_items ii ON t.TrackId = ii.TrackId" +
                " GROUP BY t.TrackId" +
                " HAVING COUNT(ii.InvoiceId) > 1");
        assertEquals(166, albums.size());
    }

    @Test
    /*
     * Select customers emails who are assigned to Jane Peacock as a Rep and
     * who have purchased something from the 'Rock' Genre
     *
     * Please use an IN clause and a sub-select to generate customer IDs satisfying the criteria
     * */
    public void selectCustomersMeetingCriteria() throws SQLException {
        // HINT: join to invoice items and do a group by/having to get the right answer
        List<Map<String, Object>> tracks = exec("SELECT c.Email" +
                " FROM customers c" +
                " WHERE c.SupportRepId = (" +
                " SELECT EmployeeID" +
                " FROM employees" +
                " WHERE FirstName = 'Jane' AND LastName = 'Peacock'" +
                ")" +
                " AND c.CustomerID IN (" +
                " SELECT DISTINCT i.CustomerId" +
                " FROM invoices i" +
                " JOIN invoice_items ii ON i.InvoiceId = ii.InvoiceId" +
                " JOIN tracks t ON ii.TrackId = t.TrackId" +
                " JOIN genres g ON t.GenreId = g.GenreId" +
                " WHERE g.Name = 'Rock'" +
                " )" );
        assertEquals(21, tracks.size());
    }
}
