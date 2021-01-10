#MP3 Player - "MyPlayer" 

Created by Danny Alvarez and Michael Anderson

This is a simple MP3 player created in Java. It features a library that stores local MP3 file info, and 
supports the creation of playlists where the user can add songs from the library into playlists of their choosing. 

You can also drag and drop MP3 files into the player to add them to the library or a playlist, and you can drag and drop songs
from inside the library to a playlist window to add the song to the playlist.

Uses Apache HTTP Server and MySQL to host and manage a database, where library and playlist info are stored. 

##Libraries used:
 * jlGui : A Java music player, more specifically using BasicPlayer3.0 (http://www.javazoom.net/jlgui/jlgui.html)
 * mysql-connector-java-8.0.21 : MySQL Connector/J, a JDBC driver for MySQL. (https://dev.mysql.com/doc/connector-j/8.0/en/)
 * mp3agic-0.9.2 : A java library for reading mp3 files and reading/manipulating the ID3 tags. (https://github.com/mpatric/mp3agic)

###TODO:
 * Add a progress bar.
 * Fix drag and drop fuctionality when dragging table entry from library window to a playlist window when dragging anywhere inside the table.
