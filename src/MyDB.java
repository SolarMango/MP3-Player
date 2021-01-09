import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

// for song id's consider using COUNT to count number of rows and add +1 to increment new song id.

public class MyDB {
    String user = "root";
    String password = "";
    String url = "jdbc:mysql://localhost:3306/mytunes";     // connector - the db engine - location - port - database

    Connection connection;
    Statement statement;

    public void Connect() {
        System.out.println("Connecting to mytunes");

        try {
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to mytunes");

        } catch (SQLException sqlException) {
            Logger.getLogger(MyDB.class.getName()).log(Level.SEVERE, null, sqlException);
        }
    }

    public void addSongtoDB(MP3 newsong) throws SQLException {
        statement = connection.createStatement();
        String sqlInsert = "INSERT INTO `songstable`(`File`, `Title`, `Artist`, `Year`, `Genre`, `Comments`) VALUES ('" + newsong.getPath() + "', '" + newsong.getTitle() + "', '" + newsong.getArtist() + "', " + newsong.getYear() + ", '" + newsong.getGenre() + "', '" + newsong.getComment() + "')";
        System.out.println(sqlInsert);
        statement.executeUpdate(sqlInsert);

        /*ResultSet resultSet = statement.executeQuery("SELECT * FROM `songstable`");
        while (resultSet.next()) {
            System.out.println(resultSet.getString("Title") + " by " + resultSet.getString("Artist") + " , Year: " + resultSet.getString("Year"));
        }*/
    }

    public void deleteSongFromDB(String filepath) throws SQLException {
        statement = connection.createStatement();
        String sqlDelete = "DELETE FROM `songstable` WHERE File = \"" + filepath + "\"";
        System.out.println(sqlDelete);
        statement.executeUpdate(sqlDelete);
    }

    // Playlist Functions
    public void createNewPlaylist(String playlistname) throws SQLException {
        statement = connection.createStatement();
        //String sqlNewPlaylist = "CREATE TABLE " + playlistname + " ( SongID int )";
        String sqlNewPlaylist = "INSERT INTO `playlists`(`playlistName`) VALUES ('" + playlistname + "')";
        System.out.println(sqlNewPlaylist);
        statement.executeUpdate(sqlNewPlaylist);
    }

    public void addSongToPlaylistDB(String playlistname, String songtitle) throws SQLException {
        int playlistID = 0;
        int songID = 0;
        statement = connection.createStatement();

        // Get the inputted playlist's ID from its name
        String sqlSelectPlaylistID = "SELECT * FROM `playlists` WHERE playlistName = \"" + playlistname + "\"";
        System.out.println(sqlSelectPlaylistID);
        ResultSet resultSet = statement.executeQuery(sqlSelectPlaylistID);
        while (resultSet.next()) {
            System.out.println("Playlist ID: " + resultSet.getString("playlistID") + ", Playlist name: " + resultSet.getString("playlistName"));
            playlistID = resultSet.getInt("playlistID");
        }

        // Get the inputted song's ID from its title
        String sqlSelectSongID = "SELECT * FROM `songstable` WHERE Title = \"" + songtitle + "\"";
        System.out.println(sqlSelectSongID);
        ResultSet songIDset = statement.executeQuery(sqlSelectSongID);
        while (songIDset.next()) {
            System.out.println("Song ID: " + songIDset.getString("id") + ", Song name: " + songIDset.getString("Title"));
            songID = songIDset.getInt("id");
        }

        String sqlAddSongToPlaylist = "INSERT INTO `playlistsongs`(`playlistID`, `songID`) VALUES ("+playlistID+", "+songID+")";
        System.out.println(sqlAddSongToPlaylist);
        statement.executeUpdate(sqlAddSongToPlaylist);
    }

    public void deleteSongFromPlaylistDB(String playlistname, String songtitle) throws SQLException {
        int playlistID = 0;
        int songID = 0;
        statement = connection.createStatement();

        // Get the inputted playlist's ID from its name
        String sqlSelectPlaylistID = "SELECT * FROM `playlists` WHERE playlistName = \"" + playlistname + "\"";
        System.out.println(sqlSelectPlaylistID);
        ResultSet resultSet = statement.executeQuery(sqlSelectPlaylistID);
        while (resultSet.next()) {
            System.out.println("Playlist ID: " + resultSet.getString("playlistID") + ", Playlist name: " + resultSet.getString("playlistName"));
            playlistID = resultSet.getInt("playlistID");
        }

        // Get the inputted song's ID from its title
        String sqlSelectSongID = "SELECT * FROM `songstable` WHERE Title = \"" + songtitle + "\"";
        System.out.println(sqlSelectSongID);
        ResultSet songIDset = statement.executeQuery(sqlSelectSongID);
        while (songIDset.next()) {
            System.out.println("Song ID: " + songIDset.getString("id") + ", Song title: " + songIDset.getString("Title"));
            songID = songIDset.getInt("id");
        }

        String sqlDeleteSongFromPlaylist = "DELETE FROM `playlistsongs` WHERE playlistID = "+playlistID+" AND songID = "+songID+"";
        System.out.println(sqlDeleteSongFromPlaylist);
        statement.executeUpdate(sqlDeleteSongFromPlaylist);
    }

    public void deletePlaylistFromDB(String playlistname) throws SQLException {
        statement = connection.createStatement();
        int playlistID = 0;

        // Get the playlist ID
        String sqlSelectPlaylistID = "SELECT * FROM `playlists` WHERE playlistName = \"" + playlistname + "\"";
        System.out.println(sqlSelectPlaylistID);
        ResultSet resultSet = statement.executeQuery(sqlSelectPlaylistID);
        while (resultSet.next()) {
            System.out.println("Playlist ID: " + resultSet.getString("playlistID") + ", Playlist name: " + resultSet.getString("playlistName"));
            playlistID = resultSet.getInt("playlistID");
        }

        // Delete playlist from playlists table
        String sqlDeletePlaylist = "DELETE FROM `playlists` WHERE playlistName = \"" + playlistname + "\"";
        System.out.println(sqlDeletePlaylist);
        statement.executeUpdate(sqlDeletePlaylist);

        // Delete all playlist's songs and ID from playlistsongs table
        String sqlDeleteSongsInPlaylist = "DELETE FROM `playlistsongs` WHERE playlistID = " + playlistID;
        System.out.println(sqlDeleteSongsInPlaylist);
        statement.executeUpdate(sqlDeleteSongsInPlaylist);
    }

    public boolean hasSong(String songName) throws SQLException {       // TODO: FIX SQL IN THIS
        statement = connection.createStatement();
        String exists = "SELECT `Title` FROM `songstable` WHERE Title = \"" + songName + "\"";
        System.out.println("Check if song exists: " + exists);
        ResultSet resultSet = statement.executeQuery(exists);

        boolean songAlreadyExists = resultSet.next();   // if song was found in the database, resultSet will have an entry, else false
        System.out.println("Song already exists? " + songAlreadyExists);
        return songAlreadyExists;
    }

    public void disconnect() throws SQLException {
        connection.close();
    }
}
