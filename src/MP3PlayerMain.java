import java.sql.SQLException;

public class MP3PlayerMain {
    public static void main(String [] args) throws SQLException {
        MP3PlayerGUI mp3PlayerGUI = new MP3PlayerGUI();
        mp3PlayerGUI.runPlayer();
        //mp3PlayerGUI.addNewSong(new Object[] {"C:\\\\Users\\\\danal\\\\Music\\\\WideOpen.mp3","Wide Open", "The Chemical Brothers", 2015});
    }
}
