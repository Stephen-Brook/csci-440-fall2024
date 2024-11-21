package edu.montana.csci.csci440.model;

import edu.montana.csci.csci440.util.DB;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Track extends Model {

    private Long trackId;
    private Long albumId;
    private Long mediaTypeId;
    private Long genreId;
    private String name;
    private Long milliseconds;
    private Long bytes;
    private BigDecimal unitPrice;
    //
    private String artistName;
    private String albumTitle;
    //

    public static final String REDIS_CACHE_KEY = "cs440-tracks-count-cache";

    public Track() {
        mediaTypeId = 1l;
        genreId = 1l;
        milliseconds  = 0l;
        bytes  = 0l;
        unitPrice = new BigDecimal("0");
    }

    public Track(ResultSet results) throws SQLException {
        name = results.getString("Name");
        milliseconds = results.getLong("Milliseconds");
        bytes = results.getLong("Bytes");
        unitPrice = results.getBigDecimal("UnitPrice");
        trackId = results.getLong("TrackId");
        albumId = results.getLong("AlbumId");
        mediaTypeId = results.getLong("MediaTypeId");
        genreId = results.getLong("GenreId");
    }

    public static Track find(long i) {
        try (Connection connect = DB.connect();
             PreparedStatement stmt = connect.prepareStatement(
                     "SELECT tracks.*, albums.Title AS albumTitle, artists.Name AS artistName " +
                             "FROM tracks " +
                             "JOIN albums ON tracks.AlbumId = albums.AlbumId " +
                             "JOIN artists ON albums.ArtistId = artists.ArtistId " +
                             "WHERE tracks.TrackId = ?")) {

            stmt.setLong(1, i);
            ResultSet resultSet = stmt.executeQuery();

            if (resultSet.next()) {
                Track track = new Track(resultSet);
                track.albumTitle = resultSet.getString("albumTitle");
                track.artistName = resultSet.getString("artistName");
                return track;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;  // Return null if no track was found with the given trackId
    }

    public static Long count() {
        try (Jedis jedis = new Jedis()) {
            // Check Redis cache
            String cachedCount = jedis.get(REDIS_CACHE_KEY);
            if (cachedCount != null) {
                return Long.parseLong(cachedCount);
            }

            // If not in cache, query the database
            try (Connection connect = DB.connect();
                 PreparedStatement stmt = connect.prepareStatement("SELECT COUNT(*) AS count FROM tracks")) {

                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    Long count = resultSet.getLong("count");

                    // Cache the result in Redis with a timeout of 1 hour
                    jedis.set(REDIS_CACHE_KEY, count.toString());
                    jedis.expire(REDIS_CACHE_KEY, 3600); // 1 hour expiry

                    return count;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return 0l;
    }

    public boolean create() {
        try (Connection connect = DB.connect();
             PreparedStatement stmt = connect.prepareStatement(
                     "INSERT INTO tracks (Name, AlbumId, MediaTypeId, GenreId, Milliseconds, Bytes, UnitPrice) VALUES (?, ?, ?, ?, ?, ?, ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, this.name);
            stmt.setLong(2, this.albumId);
            stmt.setLong(3, this.mediaTypeId);
            stmt.setLong(4, this.genreId);
            stmt.setLong(5, this.milliseconds);
            stmt.setLong(6, this.bytes);
            stmt.setBigDecimal(7, this.unitPrice);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating track failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    this.trackId = generatedKeys.getLong(1);

                    // Invalidate Redis cache after successful insertion
                    try (Jedis jedis = new Jedis()) {
                        jedis.del(REDIS_CACHE_KEY);
                    }

                    return true;
                } else {
                    throw new SQLException("Creating track failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Album getAlbum() {
        return Album.find(albumId);
    }

    public MediaType getMediaType() {
        return null;
    }
    public Genre getGenre() {
        return null;
    }
    public List<Playlist> getPlaylists(){
        List<Playlist> playlists = new ArrayList<>();

        try (Connection connect = DB.connect();
             PreparedStatement stmt = connect.prepareStatement(
                     "SELECT playlists.* " +
                             "FROM playlists " +
                             "JOIN playlist_track ON playlists.PlaylistId = playlist_track.PlaylistId " +
                             "WHERE playlist_track.TrackId = ? " +
                             "ORDER BY playlists.Name")) {

            stmt.setLong(1, this.trackId);
            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                playlists.add(new Playlist(resultSet));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return playlists;
    }

    public Long getTrackId() {
        return trackId;
    }

    public void setTrackId(Long trackId) {
        this.trackId = trackId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getMilliseconds() {
        return milliseconds;
    }

    public void setMilliseconds(Long milliseconds) {
        this.milliseconds = milliseconds;
    }

    public Long getBytes() {
        return bytes;
    }

    public void setBytes(Long bytes) {
        this.bytes = bytes;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
    }

    public void setAlbum(Album album) {
        albumId = album.getAlbumId();
    }

    public Long getMediaTypeId() {
        return mediaTypeId;
    }

    public void setMediaTypeId(Long mediaTypeId) {
        this.mediaTypeId = mediaTypeId;
    }

    public Long getGenreId() {
        return genreId;
    }

    public void setGenreId(Long genreId) {
        this.genreId = genreId;
    }

    public void setComposer(String composer) {

    }

    public String getArtistName() {
        if (artistName == null) {
            artistName = ""; // Prevent repeated null lookups
            try (Connection connect = DB.connect();
                 PreparedStatement stmt = connect.prepareStatement(
                         "SELECT artists.Name AS artistName " +
                                 "FROM artists " +
                                 "JOIN albums ON artists.ArtistId = albums.ArtistId " +
                                 "WHERE albums.AlbumId = ?")) {

                stmt.setLong(1, this.albumId);
                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    artistName = resultSet.getString("artistName");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return artistName;
    }

    public String getAlbumTitle() {
        if (albumTitle == null) {
            albumTitle = ""; // Prevent repeated null lookups
            try (Connection connect = DB.connect();
                 PreparedStatement stmt = connect.prepareStatement(
                         "SELECT Title FROM albums WHERE AlbumId = ?")) {

                stmt.setLong(1, this.albumId);
                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    albumTitle = resultSet.getString("Title");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return albumTitle;
    }

    public static List<Track> advancedSearch(int page, int count,
                                             String search, Integer artistId, Integer albumId,
                                             Integer maxRuntime, Integer minRuntime) {
        List<Track> result = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();  // Store query parameters here

        StringBuilder query = new StringBuilder("SELECT tracks.* FROM tracks ");
        query.append("JOIN albums ON tracks.AlbumId = albums.AlbumId ");
        query.append("JOIN artists ON albums.ArtistId = artists.ArtistId ");
        query.append("WHERE 1=1 ");  // Base condition to simplify appending "AND" clauses

        // Add filters based on non-null parameters
        if (search != null && !search.isEmpty()) {
            query.append("AND tracks.Name LIKE ? ");
            parameters.add("%" + search + "%");
        }
        if (artistId != null) {
            query.append("AND artists.ArtistId = ? ");
            parameters.add(artistId);
        }
        if (albumId != null) {
            query.append("AND albums.AlbumId = ? ");
            parameters.add(albumId);
        }
        if (maxRuntime != null) {
            query.append("AND tracks.Milliseconds <= ? ");
            parameters.add(maxRuntime);
        }
        if (minRuntime != null) {
            query.append("AND tracks.Milliseconds >= ? ");
            parameters.add(minRuntime);
        }

        query.append("LIMIT ? OFFSET ?");
        parameters.add(count);
        parameters.add((page - 1) * count);

        try (Connection connect = DB.connect();
             PreparedStatement stmt = connect.prepareStatement(query.toString())) {

            // Set all parameters in the PreparedStatement
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }

            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                result.add(new Track(resultSet));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public static List<Track> search(int page, int count, String orderBy, String search) {
        try {
            try(Connection connect = DB.connect();
                PreparedStatement stmt = connect.prepareStatement("SELECT  * FROM tracks WHERE name LIKE ? LIMIT ?")) {
                ArrayList<Track> result = new ArrayList<>();
                stmt.setString(1, "%" + search + "%");
                stmt.setInt(2, count);
                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    result.add(new Track(resultSet));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Track> forAlbum(Long albumId) {
        List<Track> tracks = new ArrayList<>();

        // SQL query to fetch all tracks for a given album ID
        String sql = "SELECT * FROM tracks WHERE AlbumId = ? ORDER BY Name";

        try (Connection connect = DB.connect();
             PreparedStatement stmt = connect.prepareStatement(sql)) {

            // Set the albumId parameter
            stmt.setLong(1, albumId);

            // Execute the query and process the results
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                tracks.add(new Track(resultSet));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching tracks for album ID: " + albumId, e);
        }

        return tracks;
    }

    // Sure would be nice if java supported default parameter values
    public static List<Track> all() {
        return all(0, Integer.MAX_VALUE);
    }

    public static List<Track> all(int page, int count) {
        try {
            try(Connection connect = DB.connect();
                PreparedStatement stmt = connect.prepareStatement("SELECT  * FROM tracks  LIMIT ? OFFSET ?")) {
                ArrayList<Track> result = new ArrayList<>();
                stmt.setInt(1, count);
                stmt.setInt(2, (page - 1) * count);
                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    result.add(new Track(resultSet));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Track> all(int page, int count, String orderBy) {
        // Default to a safe column for ordering if orderBy is null or empty
        String orderColumn = (orderBy != null && !orderBy.trim().isEmpty()) ? orderBy : "TrackId";

        try (Connection connect = DB.connect();
             PreparedStatement stmt = connect.prepareStatement(
                     "SELECT * FROM tracks ORDER BY " + orderColumn + " LIMIT ? OFFSET ?")) {

            ArrayList<Track> result = new ArrayList<>();
            stmt.setInt(1, count);
            stmt.setInt(2, (page - 1) * count);  // Calculate the offset based on page and count

            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                result.add(new Track(resultSet));
            }

            return result;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete() {
        if (trackId == null) {
            throw new IllegalStateException("Track ID is required for deletion");
        }

        try (Connection connect = DB.connect();
             PreparedStatement stmt = connect.prepareStatement("DELETE FROM tracks WHERE TrackId = ?")) {

            stmt.setLong(1, trackId);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                // Invalidate Redis cache after successful deletion
                try (Jedis jedis = new Jedis()) {
                    jedis.del(REDIS_CACHE_KEY);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean verify() {
        _errors.clear();  // Clear any existing errors

        if (getName() == null || getName().trim().isEmpty()) {
            addError("Name is required");
        }

        if (getAlbumId() == null) {
            addError("Album ID is required");
        }

        // Return true if there are no errors, false otherwise
        return !hasErrors();
    }

    public boolean update() {
        if (verify()) {  // Verify required fields before updating
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE tracks SET Name = ?, AlbumId = ?, MediaTypeId = ?, GenreId = ?, Milliseconds = ?, Bytes = ?, UnitPrice = ? WHERE TrackId = ?")) {

                stmt.setString(1, this.getName());
                stmt.setLong(2, this.getAlbumId());
                stmt.setLong(3, this.getMediaTypeId());
                stmt.setLong(4, this.getGenreId());
                stmt.setLong(5, this.getMilliseconds());
                stmt.setLong(6, this.getBytes());
                stmt.setBigDecimal(7, this.getUnitPrice());
                stmt.setLong(8, this.getTrackId());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    // Successfully updated
                    return true;
                } else {
                    // No rows affected, possibly track ID does not exist
                    return false;
                }

            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        } else {
            // Verification failed
            return false;
        }
    }

}