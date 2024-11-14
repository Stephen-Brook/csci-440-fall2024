package edu.montana.csci.csci440.model;

import edu.montana.csci.csci440.util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Playlist extends Model {

    Long playlistId;
    String name;

    public Playlist() {
    }

    public Playlist(ResultSet results) throws SQLException {
        name = results.getString("Name");
        playlistId = results.getLong("PlaylistId");
    }


    public List<Track> getTracks(){
        // TODO implement, order by track name
        //composer may be artist
//        return Collections.emptyList();
        List<Track> tracks = new ArrayList<>();

        try (Connection connect = DB.connect();
             PreparedStatement stmt = connect.prepareStatement(
                     "SELECT tracks.* " +
                             "FROM tracks " +
                             "JOIN playlist_track ON tracks.TrackId = playlist_track.TrackId " +
                             "WHERE playlist_track.PlaylistId = ? " +
                             "ORDER BY tracks.Name")) {

            stmt.setLong(1, playlistId);
            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                tracks.add(new Track(resultSet));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return tracks;
    }

    public Long getPlaylistId() {
        return playlistId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static List<Playlist> all() {
        return all(0, Integer.MAX_VALUE);
    }

    public static List<Playlist> all(int page, int count) {
        try {
            try(Connection connect = DB.connect();
                PreparedStatement stmt = connect.prepareStatement("SELECT  * FROM playlists  LIMIT ? OFFSET ?")) {
                ArrayList<Playlist> result = new ArrayList<>();
                stmt.setInt(1, count);
                stmt.setInt(2, (page - 1) * count);
                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    result.add(new Playlist(resultSet));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Playlist find(long playlistId) {
        try (Connection connect = DB.connect();
             PreparedStatement stmt = connect.prepareStatement("SELECT * FROM playlists WHERE PlaylistId = ?")) {

            stmt.setLong(1, playlistId);
            ResultSet resultSet = stmt.executeQuery();

            if (resultSet.next()) {
                return new Playlist(resultSet);  // Constructor initializes a Track from a ResultSet row
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;  // Return null if no track was found with the given trackId
    }

}
