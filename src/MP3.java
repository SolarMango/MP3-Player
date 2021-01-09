import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

public class MP3 {

    String title;
    String artist;
    String album;
    String path;
    String genre;
    String comments;
    int year;
    ID3v1 id3v1Tag;
    ID3v2 id3v2Tag;

    Mp3File mp3File;

    public MP3(String file) {
        try {
            this.mp3File = new Mp3File(file);

            if(mp3File.hasId3v1Tag()) {
                id3v1Tag = mp3File.getId3v1Tag();
                title = id3v1Tag.getTitle();
                artist = id3v1Tag.getArtist();
                album = id3v1Tag.getAlbum();
                year = Integer.parseInt(id3v1Tag.getYear());
            }

            if(mp3File.hasId3v2Tag()) {
                id3v2Tag = mp3File.getId3v2Tag();
                title = id3v2Tag.getTitle();
                artist = id3v2Tag.getArtist();
                album = id3v2Tag.getAlbum();
                genre = id3v2Tag.getGenreDescription();
                year = Integer.parseInt(id3v2Tag.getYear());
                comments = id3v2Tag.getComment();
                path = file;
            }

        } catch(Exception e) {
            System.out.println("Didn't find MP3 file");
        }
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getComment() {
        return comments;
    }

    public String getPath() {
        return path;
    }

    public int getYear() {
        return year;
    }

    public String getGenre() {
        return genre;
    }
}
