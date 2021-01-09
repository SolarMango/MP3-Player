import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.tree.DefaultMutableTreeNode;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerException;

public class MP3PlayerGUI {
    // Player, Database, MP3tags
    BasicPlayer player = new BasicPlayer();     // initialize the player
    MyDB maindatabase = new MyDB();
    MP3 mp3file;

    // Frame, Table, Scrollpane, Panel
    JFrame mainWindow = new JFrame("MP3 Player by Danny and Michael");
    JTable songTable;
    DefaultTableModel maintableModel = new DefaultTableModel();
    JScrollPane scrollPane;
    JPanel mainPanel = new JPanel();               // initialize main panel, hold the scrollpane and buttons
    JScrollPane treeView;

    // Library/Playlist Tree
    JTree tree;
    DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    DefaultMutableTreeNode library = new DefaultMutableTreeNode("Library");
    DefaultMutableTreeNode playlistNode = new DefaultMutableTreeNode("Playlist");

    // Bar Menu
    JMenuBar menuBar = new JMenuBar();
    JMenu menuFile = new JMenu("File");
    JMenuItem barPlaySong = new JMenuItem("Play Song not in Library");
    JMenuItem barExit = new JMenuItem("Exit");
    JMenuItem barAddSong = new JMenuItem("Add Song to Library");
    JMenuItem barDeleteSong = new JMenuItem("Delete Song from Library");
    JMenuItem barAddPlaylist = new JMenuItem("Create Playlist");

    JMenu addToPlaylist = new JMenu("Add To Playlist"); // This is the menu that we have when we right click on the main table and add to a certain playlist.

    // Right-click Menu
    JPopupMenu popupMenu = new JPopupMenu();
    JMenuItem rightClickAdd = new JMenuItem("Add Song to Library");
    JMenuItem rightClickDelete = new JMenuItem("Delete Song from Library");

    JPopupMenu playlistPopupMenu = new JPopupMenu();
    JMenuItem rightClickOpenPlaylist = new JMenuItem("Open in New Window");
    JMenuItem rightClickAddPlaylist = new JMenuItem("Add Playlist");
    JMenuItem rightClickDeletePlaylist = new JMenuItem("Delete Playlist");

    ArrayList<JMenuItem> addSongToPlaylistMenuItemsList = new ArrayList<>();
    int playlistMenuItemsIndex = 0;
    HashMap<String, Playlist> mapOfPlaylists = new HashMap <String, Playlist> ();

    // Buttons
    JButton playButton, pauseButton, stopButton, skipPrevButton, skipNextButton;

    // ActionListeners
    PlayActionListener playActionListener = new PlayActionListener();
    PauseActionListener pauseActionListener = new PauseActionListener();
    StopActionListener stopActionListener = new StopActionListener();
    PreviousActionListener previousActionListener = new PreviousActionListener();
    SkipActionListener skipActionListener = new SkipActionListener();

    OpenFileListener openFileActionListener = new OpenFileListener();
    ExitActionListener exitActionListener = new ExitActionListener();
    AddSongActionListener addSongActionListener = new AddSongActionListener();
    DeleteSongActionListener deleteSongActionListener = new DeleteSongActionListener();

    AddPlaylistActionListener addPlaylistActionListener = new AddPlaylistActionListener();
    DeletePlaylistActionListener deletePlaylistActionListener = new DeletePlaylistActionListener();
    OpenPlaylistActionListener openPlaylistWindow = new OpenPlaylistActionListener();
    AddSongToPlaylistFromRightClickActionListener addSongToPlaylistFromRightClickActionListener = new AddSongToPlaylistFromRightClickActionListener();


    // Drop Target
    MyDropTarget dropTarget = new MyDropTarget();

    // Volume Slider
    JSlider volumeSlider = new JSlider(JSlider.HORIZONTAL,0,100,50);

    int currentSelectedRow;
    int[] selectedRows;
    boolean pausedFlag;
    File currentfile;
    String currentOpenedTable = "Library";

    MP3PlayerGUI myPlayer = this;

    public MP3PlayerGUI() throws SQLException {     // throws because of database.connection.createStatement()

        maindatabase.Connect();

        // MenuBar setup
        mainWindow.setJMenuBar(menuBar);
        menuBar.add(menuFile);
        menuFile.add(barPlaySong);
        barPlaySong.addActionListener(openFileActionListener);
        menuFile.add(barAddSong);
        barAddSong.addActionListener(addSongActionListener);
        menuFile.add(barDeleteSong);
        barDeleteSong.addActionListener(deleteSongActionListener);
        menuFile.add(barAddPlaylist);
        barAddPlaylist.addActionListener(addPlaylistActionListener);
        menuFile.add(barExit);
        barExit.addActionListener(exitActionListener);

        // Right click menu setup
        popupMenu.add(rightClickAdd);
        rightClickAdd.addActionListener(addSongActionListener);
        popupMenu.add(rightClickDelete);
        rightClickDelete.addActionListener(deleteSongActionListener);
        popupMenu.add(addToPlaylist);

        playlistPopupMenu.add(rightClickOpenPlaylist);
        rightClickOpenPlaylist.addActionListener(openPlaylistWindow);
        //playlistPopupMenu.add(rightClickAddPlaylist);
        //rightClickAddPlaylist.addActionListener(addPlaylistActionListener);
        playlistPopupMenu.add(rightClickDeletePlaylist);
        rightClickDeletePlaylist.addActionListener(deletePlaylistActionListener);

        // Song Table setup
        songTable = new JTable(maintableModel);       // adds DefaultTableModel to JTable
        maintableModel.addColumn("File");
        maintableModel.addColumn("Title");
        maintableModel.addColumn("Artist");
        maintableModel.addColumn("Year");
        maintableModel.addColumn("Genre");
        maintableModel.addColumn("Comments");

        songTable.setDragEnabled(true);
        songTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        TableTransferHandler tableTransferHandler = new TableTransferHandler();
        songTable.setTransferHandler(tableTransferHandler);

        // Tree setup
        root.add(library);
        root.add(playlistNode);

        tree = new JTree(root);
        tree.setRootVisible(false);

        treeView = new JScrollPane(tree);
        treeView.setPreferredSize(new Dimension(120, 350));

        // Slider setup
        Hashtable<Integer, JLabel> volumeLabels = new Hashtable<> ();
        volumeLabels.put(volumeSlider.getMinimum(), new JLabel("-"));
        volumeLabels.put(volumeSlider.getMaximum()/2, new JLabel("Volume"));
        volumeLabels.put(volumeSlider.getMaximum(), new JLabel("+"));
        volumeSlider.setLabelTable(volumeLabels);
        volumeSlider.setPaintLabels(true);

        // Initial song table and tree update from database
        updateTable("Library");
        updatePlaylistTree();

        // set certain column sizes
        TableColumn fileColumn = songTable.getColumnModel().getColumn(0);   // gets leftmost column (file column)
        fileColumn.setPreferredWidth(250);
        TableColumn yearColumn = songTable.getColumnModel().getColumn(3);   // gets rightmost column (year column)
        yearColumn.setPreferredWidth(50);

        scrollPane = new JScrollPane(songTable);    // make the scroll pane with the song table
        scrollPane.setPreferredSize(new Dimension(650, 350));

        // ********** Setup for clicking table rows + right click menu ********** //
        MouseListener mouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                currentSelectedRow = songTable.getSelectedRow();
                System.out.println("Selected row: " + currentSelectedRow);

                selectedRows = songTable.getSelectedRows();
                System.out.print("Rows selected: ");
                for (int i : selectedRows) {
                    System.out.print(i + " ");
                }
                System.out.println();

                if(SwingUtilities.isRightMouseButton(e)) {
                    popupMenu.add(rightClickAdd);
                    popupMenu.add(rightClickDelete);
                }
            }
        };
        songTable.addMouseListener(mouseListener);  // add mouse listener to table

        // ********** Mouse Listener for the tree ********** //
        MouseListener playlistMouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode();
                try {
                    node = (DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent();
                } catch (NullPointerException nullPointerException) {
                    System.out.println("Selected tree node is null.");
                }

                if(e.getClickCount() >= 2){
                    if (node.getUserObject().toString().equals("Playlist")){}
                    else if (node.getUserObject().toString().equals("Library")) {
                        System.out.println("Playlist selected: Library");
                        currentOpenedTable = "Library";
                        try {
                            updateTable("Library");
                        } catch (SQLException sqlException) {
                            sqlException.printStackTrace();
                        }
                    }
                    else {
                        String name = node.getUserObject().toString();
                        System.out.println("Playlist selected: " + name);
                        currentOpenedTable = name;
                        try {
                            updateTable(name);
                        } catch (SQLException sqlException) {
                            sqlException.printStackTrace();
                        }
                    }
                }
                if(SwingUtilities.isRightMouseButton(e)) {
                    playlistPopupMenu.add(rightClickOpenPlaylist);
                    playlistPopupMenu.add(rightClickDeletePlaylist);
                }
            }
        };
        tree.addMouseListener(playlistMouseListener);

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
        songTable.setDropTarget(dropTarget);
        songTable.setFillsViewportHeight(true);
        scrollPane.setComponentPopupMenu(popupMenu);
        songTable.setComponentPopupMenu(popupMenu);
        tree.setComponentPopupMenu(playlistPopupMenu);

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
        mainWindow.setSize(820, 500);   // set main window size
        mainPanel.add(treeView);
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
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // set to exit program on close
        mainWindow.setVisible(true);    // sets window visible
    }

    public void addNewSongToLibrary(MP3 newsong) throws SQLException {
        maindatabase.addSongtoDB(newsong);
        updateTable("Library");
    }

    public void addNewSongToPlaylist(String nameofplaylist, MP3 newsong) throws SQLException {
        maindatabase.addSongToPlaylistDB(nameofplaylist, newsong.getTitle());
        updateTable(nameofplaylist);
    }

    public void deleteSong(int rowToDelete) throws SQLException {
        // Delete song from playlists first
        String songName = (String) songTable.getValueAt(currentSelectedRow, 1);
        for (int i = 0; i < playlistNode.getChildCount(); i++) {
            maindatabase.deleteSongFromPlaylistDB(playlistNode.getChildAt(i).toString(), songName);
        }

        // Delete song from songstable
        String filepath = (String) songTable.getValueAt(currentSelectedRow, 0);
        maindatabase.deleteSongFromDB(filepath);

        updateTable("Library");
    }

    public void updateTable(String databaseSource) throws SQLException {
        maintableModel.setRowCount(0);                                              // remove all entries in the table
        Statement startingstatement = maindatabase.connection.createStatement();
        ResultSet resultSet = startingstatement.executeQuery("SELECT * FROM `songstable`");
        int playlistID = 0;
        ArrayList<Integer> songIDs = new ArrayList<>();

        if (databaseSource.equals("Library")) {
            resultSet = startingstatement.executeQuery("SELECT * FROM `songstable`");
            while (resultSet.next()) {
                maintableModel.insertRow(maintableModel.getRowCount(), new Object[] {resultSet.getString("File"), resultSet.getString("Title"), resultSet.getString("Artist"), resultSet.getInt("Year"), resultSet.getString("Genre"), resultSet.getString("Comments")});
            }
        }
        else {
            // Get the playlist ID
            String sqlSelectPlaylistID = "SELECT * FROM `playlists` WHERE playlistName = \"" + databaseSource + "\"";
            System.out.println(sqlSelectPlaylistID);
            resultSet = startingstatement.executeQuery(sqlSelectPlaylistID);
            while (resultSet.next()) {
                System.out.println("Playlist ID: " + resultSet.getString("playlistID") + ", Playlist name: " + resultSet.getString("playlistName"));
                playlistID = resultSet.getInt("playlistID");
            }

            // Get the song ID's in that playlist, add them all to the arraylist
            String sqlSelectSongID = "SELECT * FROM `playlistsongs` WHERE playlistID = " + playlistID + "";
            System.out.println(sqlSelectSongID);
            ResultSet songIDset = startingstatement.executeQuery(sqlSelectSongID);
            while (songIDset.next()) {
                System.out.println("Playlist ID: " + songIDset.getInt("playlistID") + ", Song ID: " + songIDset.getInt("songID"));
                songIDs.add(songIDset.getInt("songID"));
            }

            // Use the song ID's to get song info from songstable
            String sqlSelectFromLibrary;
            for (int songid : songIDs) {
                sqlSelectFromLibrary = "SELECT * FROM `songstable` WHERE id = "+songid+"";
                System.out.println(sqlSelectFromLibrary);
                resultSet = startingstatement.executeQuery(sqlSelectFromLibrary);

                while (resultSet.next()) {
                    maintableModel.insertRow(maintableModel.getRowCount(), new Object[] {resultSet.getString("File"), resultSet.getString("Title"), resultSet.getString("Artist"), resultSet.getInt("Year"), resultSet.getString("Genre"), resultSet.getString("Comments")});
                }
            }
        }

        System.out.println("Table updated.");
    }

    public void updatePlaylistTree() throws SQLException {      // Updates the playlist tree with all saved playlists in database.
        Statement treestatement = maindatabase.connection.createStatement();
        ResultSet resultSet = treestatement.executeQuery("SELECT * FROM `playlists`");

        while (resultSet.next()) {
            playlistNode.add(new DefaultMutableTreeNode(resultSet.getString("playlistName")));
            JMenuItem popupAddToPlaylistItem = new JMenuItem(resultSet.getString("playlistName"));
            popupAddToPlaylistItem.addActionListener(addSongToPlaylistFromRightClickActionListener);
            addToPlaylist.add(popupAddToPlaylistItem);

            mapOfPlaylists.put(resultSet.getString("playlistName"), new Playlist(resultSet.getString("playlistName"), myPlayer));
        }
    }


    // *************** Action Listeners *************** //

    // Button Listeners
    class PlayActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (pausedFlag == true) {
                    player.resume();
                }
                else {
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
               // Logger.getLogger(StreamPlayerGUI.class.getName()).log(Level.SEVERE, null, basicPlayerException);
            }
        }
    }

    // Exit Program Listener
    class ExitActionListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                maindatabase.disconnect();
            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            }
            System.out.println("Exiting");
            System.exit(0);
        }
    }

    // Open File Explorer Listener
    class OpenFileListener implements ActionListener{

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

    // Add Song to Library Listener
    class AddSongActionListener implements ActionListener{

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
                addNewSongToLibrary(mp3file);
            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            }
        }
    }

    // Delete Song from Library Listener
    class DeleteSongActionListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {

            try {
                deleteSong(currentSelectedRow);
                // TODO: delete from playlist window when deleted from library
            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            }
        }
    }


    // ********** Playlist Action Listeners ********** //

    // Add Playlist Listener
    class AddPlaylistActionListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            String playlistName = JOptionPane.showInputDialog("Playlist name:");
            try {
                if (playlistName.equals("null") == false) {   // if user presses cancel in dialog box, name is null so dont create playlist
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(playlistName);
                    playlistNode.add(childNode);

                    // Stuff to add to addsongtoplaylist popup menu and update playlist tree
                   /* addSongToPlaylistMenuItemsList.add(new JMenuItem(playlistName));    // add new menuitem to arraylist, increment index, add the latest menuitem
                    playlistMenuItemsIndex++;
                    addToPlaylist.add(addSongToPlaylistMenuItemsList.get(playlistMenuItemsIndex - 1));
                    addSongToPlaylistMenuItemsList.get(playlistMenuItemsIndex - 1).addActionListener(addSongToPlaylistActionListener);  // add action listener to menuitem*/

                    // Stuff to add Playlist to map for getting playlist names
                    mapOfPlaylists.put(playlistName, new Playlist(playlistName, myPlayer));   //maybe do new JMenuItem mapOfPlaylists.put(playlistName, new JMenuItem(playlistName));
                    JMenuItem popupAddSongToPlaylistItem = new JMenuItem(playlistName);
                    popupAddSongToPlaylistItem.addActionListener(addSongToPlaylistFromRightClickActionListener);
                    addToPlaylist.add(popupAddSongToPlaylistItem);

                    maindatabase.createNewPlaylist(playlistName);
                    updateTable(playlistName);
                }
            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            } catch (NullPointerException nullPointerException) {
                System.out.println("Playlist creation has been canceled.");
            }
            //scrollPane.setViewportView(mapOfPlaylists.get(playlistName).getTable());    // create a new table, update from database

            tree.updateUI();
            tree.expandRow(1);
            tree.setSelectionRow(playlistNode.getChildCount() + 1);
        }
    }

    // Delete Playlist Listener
    class DeletePlaylistActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            int deleteconfirm = JOptionPane.showConfirmDialog(null,"Are you sure you want to delete this playlist?");
            if (deleteconfirm == 0) {   // if user clicks yes, try to delete
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent();
                if (node.getUserObject().toString().equals("Playlist") || node.getUserObject().toString().equals("Library")) {
                    System.out.println("Can't Delete " + node.getUserObject().toString());
                }
                else {
                    mapOfPlaylists.get(node.getUserObject().toString()).closeTable();   // close playlist window if open
                    playlistNode.remove(node);
                    addToPlaylist.removeAll();

                    for (int i = 0; i < playlistNode.getChildCount(); i++ ){
                        JMenuItem item = new JMenuItem((playlistNode.getChildAt(i).toString()));
                        item.addActionListener(addSongToPlaylistFromRightClickActionListener);
                        addToPlaylist.add(item);
                    }

                    try {
                        maindatabase.deletePlaylistFromDB(node.getUserObject().toString());
                        updateTable("Library");
                    } catch (SQLException sqlException) {
                        sqlException.printStackTrace();
                    }

                    System.out.println("Deleting Playlist " + node.getUserObject().toString());
                    tree.updateUI();
                    tree.setSelectionRow(0);
                }
            }
            else {
                System.out.println("User clicked no or cancel.");
            }
        }
    }

    // Add Song to Playlist from Right Clicking library table Listener
    class AddSongToPlaylistFromRightClickActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String playlistname = e.getActionCommand();
            System.out.println(playlistname +" Called AddSongToPlaylistActionListener.");
            try {
                maindatabase.addSongToPlaylistDB(playlistname, (String) songTable.getValueAt(currentSelectedRow, 1));
                mapOfPlaylists.get(playlistname).updateTable();
            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            }
        }
    }

    // Open Separate Playlist Window Listener
    class OpenPlaylistActionListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                //scrollPane.setViewportView(songTable);
                updateTable("Library");
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent();
                String playlistName = node.getUserObject().toString();
                //Playlist playlistWindow = new Playlist(playlistName);
                Playlist playlistWindow = mapOfPlaylists.get(playlistName);
                playlistWindow.runPlayer();
                tree.setSelectionRow(0);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }


    // ********** Drop Target + Transfer Handler Classes ********** //

    // Drop Target Class
    class MyDropTarget extends DropTarget {
        public void drop(DropTargetDropEvent evt) {
            try {
                evt.acceptDrop(DnDConstants.ACTION_COPY);

                if (currentOpenedTable == "Library") {
                    System.out.println("Dropping to Library");
                    java.util.List result = new ArrayList();
                    result = (java.util.List) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                    String str = "";
                    for(Object o : result) {
                        str += (o.toString());
                        System.out.println(str);
                        mp3file = new MP3(str);
                        addNewSongToLibrary(mp3file);
                        str = "";
                    }
                }
                else {
                    System.out.println("Dropping to playlist " + currentOpenedTable);
                    java.util.List result = new ArrayList();
                    result = (java.util.List) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                    String str = "";
                    for(Object o : result) {
                        str += (o.toString());
                        System.out.println(str);
                        mp3file = new MP3(str);
                        if (!maindatabase.hasSong(mp3file.getTitle())) {
                            addNewSongToLibrary(mp3file);
                        }
                        addNewSongToPlaylist(currentOpenedTable, mp3file);
                        str = "";
                    }
                }

            }
            catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    // Transfer Handler Class
    public class TableTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            JTable table = (JTable) c;
            /*int row = table.getSelectedRow();
            int col = table.getSelectedColumn();
            System.out.println(row+ " " +col);*/

            String rowsData = "";
            for (int row : selectedRows) {
                rowsData = rowsData + table.getModel().getValueAt(row, 1) + "/";
            }
            //rowsData = (String) table.getModel().getValueAt(row,1);
            StringSelection transferable = new StringSelection(rowsData);
            //table.getModel().setValueAt(null,row,col);
            return transferable;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            System.out.println("Song was copied from the library.");
        }
    }
}
