import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerException;

public class Playlist {

    BasicPlayer player = new BasicPlayer();     // initialize the player
    MyDB playlistdatabase = new MyDB();
    MP3 mp3file;
    String nameOfPlaylist;

    // Frame, Table, Scrollpane, Panel
    JFrame mainWindow = new JFrame();
    JTable songTable;
    DefaultTableModel tableModel = new DefaultTableModel();
    JScrollPane scrollPane;
    JPanel mainPanel = new JPanel();               // initialize main panel, hold the scrollpane and buttons

    // Menus
    JMenuBar menuBar = new JMenuBar();
    JMenu menuFile = new JMenu("File");
    //JMenuItem barPlaySong = new JMenuItem("Open");
    JMenuItem barAddSongToPlaylist = new JMenuItem("Add Song to Playlist");
    //JMenuItem barAddPlaylist = new JMenuItem("Add Playlist");
    JMenuItem barExit = new JMenuItem("Exit");

    JPopupMenu popupMenu = new JPopupMenu();
    JPopupMenu playlistPopupMenu = new JPopupMenu();
    //JMenuItem rightClickAdd = new JMenuItem("Add Song");
    JMenuItem rightClickDelete = new JMenuItem("Delete Song from Playlist");


    // Buttons
    JButton playButton, pauseButton, stopButton, skipPrevButton, skipNextButton;

    // ActionListener
    PlayActionListener playActionListener = new PlayActionListener();
    PauseActionListener pauseActionListener = new PauseActionListener();
    StopActionListener stopActionListener = new StopActionListener();
    PreviousActionListener previousActionListener = new PreviousActionListener();
    SkipActionListener skipActionListener = new SkipActionListener();
    OpenFileListener openFileActionListener = new OpenFileListener();
    AddSongActionListener addSongActionListener = new AddSongActionListener();
    DeleteSongActionListener deleteSongActionListener = new DeleteSongActionListener();
    ExitPlaylist exitPlaylist = new ExitPlaylist();

    // Drop Target
    MyDropTarget dropTarget = new MyDropTarget();

    //Playlist
    DefaultMutableTreeNode parent = new DefaultMutableTreeNode();
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Library");
    DefaultMutableTreeNode playlist = new DefaultMutableTreeNode("Playlist");

    //Slider
    JSlider volumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);

    int currentSelectedRow;
    boolean pausedFlag;
    File currentfile;

    MP3PlayerGUI mp3GUI;

    public Playlist(String playlistName, MP3PlayerGUI mainPlayer) throws SQLException {
        mp3GUI = mainPlayer;

        System.out.println("Playlist window connecting...");
        playlistdatabase.Connect();
        nameOfPlaylist = playlistName;

        mainWindow.setJMenuBar(menuBar);
        menuBar.add(menuFile);
        menuFile.add(barAddSongToPlaylist);
        barAddSongToPlaylist.addActionListener(addSongActionListener);
        menuFile.add(barExit);
        barExit.addActionListener(exitPlaylist);


        // Right click menu setup
        //popupMenu.add(rightClickAdd);
        //rightClickAdd.addActionListener(addSongActionListener);
        popupMenu.add(rightClickDelete);
        rightClickDelete.addActionListener(deleteSongActionListener);

        //Slider Setup
        Hashtable<Integer, JLabel> volumeLabels = new Hashtable<>();
        volumeLabels.put(volumeSlider.getMinimum(), new JLabel("-"));
        volumeLabels.put(volumeSlider.getMaximum() / 2, new JLabel("Volume"));
        volumeLabels.put(volumeSlider.getMaximum(), new JLabel("+"));
        volumeSlider.setLabelTable(volumeLabels);
        volumeSlider.setPaintLabels(true);

        // Song Table setup
        songTable = new JTable(tableModel);       // adds DefaultTableModel to JTable
        tableModel.addColumn("File");
        tableModel.addColumn("Title");
        tableModel.addColumn("Artist");
        tableModel.addColumn("Year");
        tableModel.addColumn("Genre");
        tableModel.addColumn("Comments");

        songTable.setDragEnabled(true);
        songTable.setDropMode(DropMode.INSERT_ROWS);
        songTable.setFillsViewportHeight(true);
        TableTransferHandler tableTransferHandler = new TableTransferHandler();
        songTable.setTransferHandler(tableTransferHandler);

        updateTable(); // initial update

        TableColumn fileColumn = songTable.getColumnModel().getColumn(0);   // gets leftmost column (file column)
        fileColumn.setPreferredWidth(250);
        TableColumn yearColumn = songTable.getColumnModel().getColumn(3);   // gets rightmost column (year column)
        yearColumn.setPreferredWidth(50);

        scrollPane = new JScrollPane(songTable);    // make the scroll pane with the song table
        scrollPane.setPreferredSize(new Dimension(650, 350));

        // ********** Setup for clicking table rows + right click menu *********** //
        MouseListener mouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                currentSelectedRow = songTable.getSelectedRow();
                System.out.println("Selected row: " + currentSelectedRow);

                if (SwingUtilities.isRightMouseButton(e)) {
                    //popupMenu.add(rightClickAdd);
                    popupMenu.add(rightClickDelete);
                }
            }
        };
        songTable.addMouseListener(mouseListener);  // add mouse listener to table

        // Slider Change Listener
        volumeSlider.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e){
                double volume =  ((JSlider) e.getSource()).getValue();
                try {
                    player.setGain(volume/100);
                } catch (BasicPlayerException basicPlayerException) {
                    basicPlayerException.printStackTrace();
                }
            }
        });

        mainPanel.setLayout(new FlowLayout());  // set panel layout to add left-to-right
        mainPanel.setDropTarget(dropTarget);

        scrollPane.setComponentPopupMenu(popupMenu);
        songTable.setComponentPopupMenu(popupMenu);

        // ********** Button stuff ********** //
        playButton = new JButton("Play");
        playButton.setPreferredSize(new Dimension(70, 30));
        playButton.addActionListener(playActionListener);
        pauseButton = new JButton("Pause");
        pauseButton.setPreferredSize(new Dimension(70, 30));
        pauseButton.addActionListener(pauseActionListener);
        stopButton = new JButton("Stop");
        stopButton.setPreferredSize(new Dimension(70, 30));
        stopButton.addActionListener(stopActionListener);
        skipNextButton = new JButton("Next");
        skipNextButton.setPreferredSize(new Dimension(70, 30));
        skipNextButton.addActionListener(skipActionListener);
        skipPrevButton = new JButton("Prev");
        skipPrevButton.setPreferredSize(new Dimension(70, 30));
        skipPrevButton.addActionListener(previousActionListener);

        // ********** Main window stuff ********** //
        mainWindow.setSize(700, 500); // set main window size
        mainWindow.setTitle("Playlist: " + playlistName);
        mainPanel.add(scrollPane);
        mainPanel.add(playButton);
        mainPanel.add(pauseButton);
        mainPanel.add(stopButton);
        mainPanel.add(skipNextButton);
        mainPanel.add(skipPrevButton);
        mainPanel.add(volumeSlider);
        mainWindow.add(mainPanel);

    }

    public void runPlayer() {
        mainWindow.setLocation(200, 200);
        mainWindow.setVisible(true);    // sets window visible
    }

    public void addNewSongToLibrary(MP3 newsong) throws SQLException {
        playlistdatabase.addSongtoDB(newsong);
        updateTable();
    }

    public void addNewSongToPlaylist(MP3 newsong) throws SQLException {
        playlistdatabase.addSongToPlaylistDB(nameOfPlaylist, newsong.getTitle());
        updateTable();
    }

    public void deleteSong(int rowToDelete) throws SQLException {
        //tableModel.removeRow(currentSelectedRow);
        String filepath = (String) songTable.getValueAt(currentSelectedRow, 0);
        //database.deleteSongFromPlaylistDB(filepath);
        updateTable();
    }

    public void updateTable() throws SQLException {     // make this delete the current table
        tableModel.setRowCount(0);                                              // remove all entries in the table
        Statement startingstatement = playlistdatabase.connection.createStatement();
        ResultSet resultSet = startingstatement.executeQuery("SELECT * FROM `songstable`");
        int playlistID = 0;
        ArrayList<Integer> songIDs = new ArrayList<>();

        // Get the playlist ID
        String sqlSelectPlaylistID = "SELECT * FROM `playlists` WHERE playlistName = \"" + nameOfPlaylist + "\"";
        System.out.println("Playlist window: " + sqlSelectPlaylistID);
        resultSet = startingstatement.executeQuery(sqlSelectPlaylistID);
        while (resultSet.next()) {
            System.out.println("Playlist ID: " + resultSet.getString("playlistID") + ", Playlist name: " + resultSet.getString("playlistName"));
            playlistID = resultSet.getInt("playlistID");
        }

        // Get the song ID's in that playlist, add them all to the arraylist
        String sqlSelectSongID = "SELECT * FROM `playlistsongs` WHERE playlistID = " + playlistID + "";
        System.out.println("Playlist window: " + sqlSelectSongID);
        ResultSet songIDset = startingstatement.executeQuery(sqlSelectSongID);
        while (songIDset.next()) {
            System.out.println("Playlist ID: " + songIDset.getInt("playlistID") + ", Song ID: " + songIDset.getInt("songID"));
            songIDs.add(songIDset.getInt("songID"));
        }

        // Use the song ID's to get song info from songstable
        String sqlSelectFromLibrary;
        for (int songid : songIDs) {
            sqlSelectFromLibrary = "SELECT * FROM `songstable` WHERE id = " + songid + "";
            System.out.println("Playlist window: " + sqlSelectFromLibrary);
            resultSet = startingstatement.executeQuery(sqlSelectFromLibrary);

            while (resultSet.next()) {
                tableModel.insertRow(tableModel.getRowCount(), new Object[]{resultSet.getString("File"), resultSet.getString("Title"), resultSet.getString("Artist"), resultSet.getInt("Year"), resultSet.getString("Genre"), resultSet.getString("Comments")});
            }
        }

        System.out.println("Playlist table updated.");
    }

    public void closeTable() {
        mainWindow.setVisible(false);
        mainWindow.dispose();
    }

    public JTable getTable() {
        return songTable;
    }


    // *********** Action Listeners *********** //
    // Button Listeners
    class PlayActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (pausedFlag == true) {
                    player.resume();
                } else {
                    pausedFlag = false;
                    String filepath = (String) songTable.getValueAt(currentSelectedRow, 0); // gets the filepath from the clicked row (only seems to work in try block?)
                    currentfile = new File(filepath);
                    player.open(currentfile);
                    player.play();
                    System.out.println(filepath);
                }
            } catch (BasicPlayerException basicPlayerException) {
                System.out.println("BasicPlayer exception.");
                //Logger.getLogger(StreamPlayerGUI.class.getName()).log(Level.SEVERE, null, basicPlayerException);
            }
        }
    }

    class PauseActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                pausedFlag = true;
                player.pause();
            } catch (BasicPlayerException basicPlayerException) {
                System.out.println("BasicPlayer exception.");
                //Logger.getLogger(StreamPlayerGUI.class.getName()).log(Level.SEVERE, null, basicPlayerException);
            }
        }
    }

    class StopActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                pausedFlag = false; // pausedFlag is false in order to play song from beginning when play button is pressed.

                player.pause();
            } catch (BasicPlayerException basicPlayerException) {
                System.out.println("BasicPlayer exception.");
                //Logger.getLogger(StreamPlayerGUI.class.getName()).log(Level.SEVERE, null, basicPlayerException);
            }
        }
    }

    class PreviousActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                songTable.clearSelection();
                if (currentSelectedRow == 0){
                    System.out.println("Reached the beginning, going to the end!");
                    songTable.changeSelection(songTable.getRowCount() - 1,0,true,false);
                    currentSelectedRow = songTable.getRowCount() - 1;
                }
                else{
                    songTable.changeSelection(currentSelectedRow - 1,0,true,false);
                    currentSelectedRow -= 1;
                }

                System.out.println("The current selected row is: " + currentSelectedRow);
                String filepath = (String) songTable.getValueAt(currentSelectedRow, 0);
                currentfile = new File(filepath);
                player.open(currentfile);
                player.play();
            } catch (BasicPlayerException basicPlayerException) {
                System.out.println("BasicPlayer exception.");
                //Logger.getLogger(StreamPlayerGUI.class.getName()).log(Level.SEVERE, null, basicPlayerException);
            }
        }
    }

    class SkipActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                songTable.clearSelection();
                if (currentSelectedRow == songTable.getRowCount() - 1){
                    System.out.println("Reached the end, going to the beginning!");
                    songTable.changeSelection(0,0,true,false);
                    currentSelectedRow = 0;
                }
                else{
                    songTable.changeSelection(currentSelectedRow + 1,0,true,false);
                    currentSelectedRow += 1;
                }

                System.out.println("The current selected row is: " + currentSelectedRow);
                String filepath = (String) songTable.getValueAt(currentSelectedRow, 0);
                currentfile = new File(filepath);
                player.open(currentfile);
                player.play();
            } catch (BasicPlayerException basicPlayerException) {
                System.out.println("BasicPlayer exception.");
                //Logger.getLogger(StreamPlayerGUI.class.getName()).log(Level.SEVERE, null, basicPlayerException);
            }
        }
    }

    // Open File Explorer Listener
    class OpenFileListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("C:\\Users\\danal\\Music\\"));
            Component parent = null;
            int returnVal = chooser.showOpenDialog(parent);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    String filelocation = chooser.getSelectedFile().getAbsolutePath();
                    System.out.println(filelocation);
                    File file = new File(chooser.getSelectedFile().getAbsolutePath());
                    player.open(file);
                    player.play();
                } catch (BasicPlayerException ex) {
                    System.out.println("BasicPlayer exception");
                }
            }
        }
    }

    // Add Song to Playlist Listener
    class AddSongActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("C:\\Users\\danal\\Music\\"));
            Component parent = null;
            int returnVal = chooser.showOpenDialog(parent);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String filelocation = chooser.getSelectedFile().getAbsolutePath();
                System.out.println(filelocation);

                mp3file = new MP3(filelocation);
                System.out.println("New song info: " + mp3file.getPath() + ", " + mp3file.getTitle() + ", " + mp3file.getAlbum() + ", " + mp3file.getArtist());
            }
            try {
                if (!playlistdatabase.hasSong(mp3file.getTitle())) {
                    addNewSongToLibrary(mp3file);
                    // update the main window
                    mp3GUI.updateTable("Library");
                }
                addNewSongToPlaylist(mp3file);
                //playlistdatabase.addSongToPlaylistDB(nameOfPlaylist, mp3file.getTitle());
            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            }
        }
    }

    // Delete Song from Playlist Listener
    class DeleteSongActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {

            try {
                String songtitle = (String) songTable.getValueAt(currentSelectedRow, 1);
                playlistdatabase.deleteSongFromPlaylistDB(nameOfPlaylist, songtitle);
                updateTable();
            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            }
        }
    }

    // Exit Playlist Window Listener
    class ExitPlaylist implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            mainWindow.dispatchEvent(new WindowEvent(mainWindow, WindowEvent.WINDOW_CLOSING));
        }
    }

    // Drop Target Class
    class MyDropTarget extends DropTarget {
        public void drop(DropTargetDropEvent evt) {
            try {
                evt.acceptDrop(DnDConstants.ACTION_COPY);

                java.util.List result = new ArrayList();
                result = (java.util.List) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                String str = "";
                for (Object o : result) {
                    str += (o.toString());
                    System.out.println(str);
                    mp3file = new MP3(str);
                    if (!playlistdatabase.hasSong(mp3file.getTitle())) {
                        addNewSongToLibrary(mp3file);
                        // update the main window
                        mp3GUI.updateTable("Library");
                    }
                    addNewSongToPlaylist(mp3file);
                    str = "";
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // Transfer Handler Class
    public class TableTransferHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {     // checks if transfer is supported, we only support drops
            if (!support.isDrop()) {
                return false;
            }
            if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return false;
            }
            return true;
        }

        @Override
        public boolean importData(TransferSupport support) {    // merge transferhandler and droptarget classes?
            if (!canImport(support)) {
                return false;
            }

            JTable table = (JTable) support.getComponent();
            DefaultTableModel tableModel = (DefaultTableModel) table.getModel();

            JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation();

            int row = dropLocation.getRow();
            int col = dropLocation.getColumn();
            String data = "";

            try {
                data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                System.out.println(data);
                String[] dataarray = data.split("/");
                for (String s : dataarray) {
                    //System.out.println(s);
                    playlistdatabase.addSongToPlaylistDB(nameOfPlaylist, s);    // s is the song title, gotten from the transfer data
                }
                updateTable();
            } catch (UnsupportedFlavorException e) {
                e.printStackTrace();
                return false;
            } catch (IOException ioException) {
                ioException.printStackTrace();
                return false;
            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            }

            //tableModel.setValueAt(data, row, col);

            return true;
        }
    }
}
