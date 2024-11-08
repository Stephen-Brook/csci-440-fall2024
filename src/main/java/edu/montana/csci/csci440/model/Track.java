package edu.montana.csci.csci440.model;

import edu.montana.csci.csci440.util.DB;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.math.BigInteger;
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
//    private String albumTitle;
//    private String artistName;

    public static final String REDIS_CACHE_KEY = "cs440-tracks-count-cache";

    public Track() {
        mediaTypeId = 1l;
        genreId = 1l;
        milliseconds  = 0l;
        bytes  = 0l;
        unitPrice = new BigDecimal("0");
    }

    private Track(ResultSet results) throws SQLException {
        name = results.getString("Name");
        milliseconds = results.getLong("Milliseconds");
        bytes = results.getLong("Bytes");
        unitPrice = results.getBigDecimal("UnitPrice");
        trackId = results.getLong("TrackId");
        albumId = results.getLong("AlbumId");
        mediaTypeId = results.getLong("MediaTypeId");
        genreId = results.getLong("GenreId");
        //get new fields
    }

    public static Track find(long i) {
        try (Connection connect = DB.connect();
             PreparedStatement stmt = connect.prepareStatement("SELECT * FROM tracks WHERE TrackId = ?")) {

            stmt.setLong(1, i);
            ResultSet resultSet = stmt.executeQuery();

            if (resultSet.next()) {
                return new Track(resultSet);  // Constructor initializes a Track from a ResultSet row
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;  // Return null if no track was found with the given trackId
    }

    public static Long count() {

        // write query to count number of tracks
        // check the redis cache
        // if there is a value there return it
        // else issue the query
        //   save the count to redis
        //   return the count


        // TODO - also invalidate cache

        return 0l;
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
        return Collections.emptyList();
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

    public String getArtistName() {
        // TODO implement more efficiently
        //  hint: cache on this model object
        // introduce a field for artist name
        return getAlbum().getArtist().getName();
    }

    public String getAlbumTitle() {
        // TODO implement more efficiently
        //  hint: cache on this model object
        return getAlbum().getTitle();
    }

    public static List<Track> advancedSearch(int page, int count,
                                             String search, Integer artistId, Integer albumId,
                                             Integer maxRuntime, Integer minRuntime) {
        try {
            String sql =
                    "SELECT  * FROM tracks " +
                    "WHERE name LIKE ? ";
            ArrayList<Object> args = new ArrayList<>();

            if(artistId != null){
                sql += "AND artistId = ?";
                args.add(artistId);
            }
            if(albumId != null){
                sql += "AND albumId = ?";
                args.add(albumId);
            }
            if(maxRuntime != null){
                sql += "AND maxRuntime = ?";
                args.add(maxRuntime);
            }
            if(minRuntime != null){
                sql += "AND minRuntime = ?";
                args.add(minRuntime);
            }
            sql += " LIMIT ? OFFSET ?";
            args.add(count);
            args.add((page - 1) * count);

            try(Connection connect = DB.connect();
                PreparedStatement stmt = connect.prepareStatement(sql)) {
                ArrayList<Track> result = new ArrayList<>();
                stmt.setString(1, "%" + search + "%");

                for(int i = 0; i < args.size(); i++){
                    Object arg = args.get(i);
                    stmt.setObject(i + 2, arg);
                }

//                //TODO add additional fields to search
//                stmt.setLong(2, (long) maxRuntime);
//                stmt.setLong(3, (long) minRuntime);
//                stmt.setLong(4, artistId);
//                stmt.setLong(5, albumId);
//                stmt.setLong(6, count);
                stmt.setLong(7, (page - 1) * count);
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
        return Collections.emptyList();
    }

    // Sure would be nice if java supported default parameter values
    public static List<Track> all() {
        return all(0, Integer.MAX_VALUE);
    }

    public static List<Track> all(int page, int count) {
        return all(page, count, "TrackId");
    }

    public static List<Track> all(int page, int count, String orderBy) {
        try {
            try(Connection connect = DB.connect();
                PreparedStatement stmt = connect.prepareStatement("SELECT  * FROM tracks LIMIT ? OFFSET ?")) {
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
        }    }

//    public boolean create() {
//        if (verify()) {
//            try (Connection conn = DB.connect();
//                 PreparedStatement stmt = conn.prepareStatement(
//                         "INSERT INTO Tracks (Name, Milliseconds, Bytes, UnitPrice, TrackId, AlbumId, MediaTypeId, GenreId) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
//                stmt.setString(1, this.getName());
//                stmt.setLong(2, this.getMilliseconds());
//                stmt.setLong(3, this.getBytes());
//                stmt.setBigDecimal(4, this.getUnitPrice());
//                stmt.setLong(5, this.getTrackId());
//                stmt.setLong(6, this.getAlbumId());
//                stmt.setLong(7, this.getMediaTypeId());
//                stmt.setLong(8, this.getGenreId());
//
//                stmt.executeUpdate();
//                this.trackId = DB.getLastID(conn);
//                return true;
//            } catch (SQLException sqlException) {
//                throw new RuntimeException(sqlException);
//            }
//        }
//        return false;
//    }

    /*
    public boolean equals(Object o){
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        if(!super.equals(o)){
            return false;
        }
        Track track = (Track) o;
        return Objects.equals(trackId, track.trackId) && Objects.equals(albumId, track.albumId) && Objects.equals(mediaTypeId, track.mediaTypeId) && Objects.equals(genreId, track.genreId) && Objects.equals(name, track.name) && Objects.equals(milliseconds, track.milliseconds) && Objects.equals(bytes, track.bytes) && Objects.equals(unitPrice, track.unitPrice);
    }

    public int hashCode(){
        return Objects.hash(super.hashCode(), trackId, albumId, mediaTypeId, genreId, name, milliseconds, bytes, unitPrice);
    }
    */

}
