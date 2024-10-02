package edu.montana.csci.csci440.model;

import edu.montana.csci.csci440.util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Artist extends Model {

    Long artistId;
    String name;

    public Artist() {
    }

    private Artist(ResultSet results) throws SQLException {
        name = results.getString("Name");
        artistId = results.getLong("ArtistId");
    }

    public List<Album> getAlbums(){
        return Album.getForArtist(artistId);
    }

    public Long getArtistId() {
        return artistId;
    }

    public void setArtistId(Long id) {
        this.artistId = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static List<Artist> all() {
        return all(0, Integer.MAX_VALUE);
    }

    public static List<Artist> all(int page, int count) {
        try {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT * FROM artists LIMIT ? OFFSET ?")) {
                stmt.setInt(1, count);
                stmt.setInt(2, (page - 1) * count);
                ResultSet resultSet = stmt.executeQuery();
                ArrayList<Artist> artists = new ArrayList<>();
                while (resultSet.next()) {
                    artists.add(new Artist(resultSet));
                }
                return artists;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Artist find(long i) {
        try {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT * FROM artists WHERE ArtistId = ?")) {
                stmt.setLong(1, i);
                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    return new Artist(resultSet);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean  update() {
        if (verify()) {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE artists SET name = ? WHERE artistID=?")) {
                stmt.setString(1, this.getName());
                stmt.setString(2, this.artistId.toString());
                stmt.executeUpdate();
                return true;
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean verify() {
        _errors.clear();
        if (name == null || name.isEmpty() || "".equals(name)){
            addError("Artist name is null or empty!");
        }
        return !hasErrors();
    }

    @Override
    public boolean create() {
        if (verify()) {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO artists (Name) VALUES (?)")) {
                stmt.setString(1, this.getName());
                stmt.executeUpdate();
                artistId = DB.getLastID(conn);
                return true;
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        } else {
            return false;
        }
    }

}
