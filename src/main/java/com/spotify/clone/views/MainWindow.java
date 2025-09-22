package com.spotify.clone.views;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.spotify.clone.controllers.AudioPlayer;
import com.spotify.clone.models.MusicLibrary;
import com.spotify.clone.models.Playlist;
import com.spotify.clone.models.Song;
import com.spotify.clone.utils.ConfigManager;
import com.spotify.clone.utils.ImportResult;
import com.spotify.clone.utils.MusicImporter;
import com.spotify.clone.utils.MusicLibraryIO;
import com.spotify.clone.utils.PlaylistIO;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Main window for the Spotify clone application
 */
public class MainWindow {
    private Stage primaryStage;
    private MusicLibrary musicLibrary;
    private AudioPlayer audioPlayer;
    
    // UI Components
    private ListView<Song> songListView;
    private ListView<Playlist> playlistListView;
    private Label nowPlayingLabel;
    private Label timeLabel;
    private Slider progressSlider;
    private Slider volumeSlider;
    private Button playPauseButton;
    private Button stopButton;
    private Button previousButton;
    private Button nextButton;
    private TextField searchField;
    
    // Current state
    private Playlist currentPlaylist;
    private int currentSongIndex = -1;
    private boolean isSeeking = false;
    
    public MainWindow(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.musicLibrary = MusicLibrary.getInstance();
        this.audioPlayer = new AudioPlayer();
        // Load persisted library and playlists at startup
        try {
            MusicLibraryIO.loadLibrary(musicLibrary);
            musicLibrary.getAllPlaylists().addAll(PlaylistIO.loadPlaylists());
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Load Error");
            alert.setHeaderText("Failed to load library or playlists");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }

        setupAudioPlayerListeners();
        initializeUI();
    }
    
    private void initializeUI() {
        primaryStage.setTitle("Spotify Clone");
        
        // Create main layout
        BorderPane mainLayout = new BorderPane();
        
        // Create menu bar
        MenuBar menuBar = createMenuBar();
        mainLayout.setTop(menuBar);
        
        // Create center content (split between library and playlists)
        SplitPane centerPane = createCenterPane();
        mainLayout.setCenter(centerPane);
        
        // Create bottom controls
        VBox bottomControls = createBottomControls();
        mainLayout.setBottom(bottomControls);
        
        // Create scene
        Scene scene = new Scene(mainLayout, 1200, 800);
        primaryStage.setScene(scene);
        // On close, persist library and playlists and save config
        primaryStage.setOnCloseRequest(evt -> {
            try {
                MusicLibraryIO.saveLibrary(musicLibrary);
            } catch (IOException e) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Save Error");
                err.setHeaderText("Failed to save music library");
                err.setContentText(e.getMessage());
                err.showAndWait();
            }

            try {
                PlaylistIO.savePlaylists(musicLibrary.getAllPlaylists());
            } catch (IOException e) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Save Error");
                err.setHeaderText("Failed to save playlists");
                err.setContentText(e.getMessage());
                err.showAndWait();
            }

            try {
                ConfigManager.getInstance().saveConfig();
            } catch (IOException e) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Config Save Error");
                err.setHeaderText("Failed to save configuration");
                err.setContentText(e.getMessage());
                err.showAndWait();
            }
        });
        primaryStage.show();
    }
    
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem importItem = new MenuItem("Import Music...");
        importItem.setOnAction(e -> importMusic());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().addAll(importItem, new SeparatorMenuItem(), exitItem);
        
        // Playlist menu
        Menu playlistMenu = new Menu("Playlist");
        MenuItem newPlaylistItem = new MenuItem("New Playlist...");
        newPlaylistItem.setOnAction(e -> createNewPlaylist());
        playlistMenu.getItems().add(newPlaylistItem);
        
        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAbout());
        helpMenu.getItems().add(aboutItem);
        
        menuBar.getMenus().addAll(fileMenu, playlistMenu, helpMenu);
        return menuBar;
    }
    
    private SplitPane createCenterPane() {
        SplitPane splitPane = new SplitPane();
        
        // Left side - Music Library
        VBox libraryPane = createLibraryPane();
        
        // Right side - Playlists
        VBox playlistPane = createPlaylistPane();
        
        splitPane.getItems().addAll(libraryPane, playlistPane);
        splitPane.setDividerPositions(0.7);
        
        return splitPane;
    }
    
    private VBox createLibraryPane() {
        VBox libraryPane = new VBox(10);
        libraryPane.setPadding(new Insets(10));
        
        // Search bar
        searchField = new TextField();
        searchField.setPromptText("Search songs, artists, albums...");
        searchField.textProperty().addListener((obs, oldText, newText) -> performSearch(newText));
        
        // Songs list
        Label songsLabel = new Label("Songs");
        songsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        songListView = new ListView<>();
        songListView.setCellFactory(listView -> new SongListCell());
        songListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Song selectedSong = songListView.getSelectionModel().getSelectedItem();
                if (selectedSong != null) {
                    playSong(selectedSong);
                }
            }
        });
        
        refreshSongList();
        
        libraryPane.getChildren().addAll(searchField, songsLabel, songListView);
        VBox.setVgrow(songListView, Priority.ALWAYS);
        
        return libraryPane;
    }
    
    private VBox createPlaylistPane() {
        VBox playlistPane = new VBox(10);
        playlistPane.setPadding(new Insets(10));
        
        // Playlists header with add button
        HBox playlistHeader = new HBox(10);
        Label playlistLabel = new Label("Playlists");
        playlistLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Button addPlaylistButton = new Button("+");
        addPlaylistButton.setOnAction(e -> createNewPlaylist());
        playlistHeader.getChildren().addAll(playlistLabel, addPlaylistButton);
        
        // Playlists list
        playlistListView = new ListView<>();
        playlistListView.setCellFactory(listView -> {
            PlaylistListCell cell = new PlaylistListCell();
            ContextMenu menu = new ContextMenu();
            MenuItem delete = new MenuItem("Delete Playlist");
            delete.setOnAction(e -> {
                Playlist p = cell.getItem();
                if (p != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Delete Playlist");
                    confirm.setHeaderText("Delete playlist '" + p.getName() + "'");
                    confirm.setContentText("Are you sure? This action cannot be undone.");
                    confirm.showAndWait().ifPresent(bt -> {
                            if (bt.getButtonData().isDefaultButton()) {
                                musicLibrary.removePlaylist(p);
                                try {
                                    PlaylistIO.savePlaylists(musicLibrary.getAllPlaylists());
                                } catch (IOException ex) {
                                    Alert err = new Alert(Alert.AlertType.ERROR);
                                    err.setTitle("Save Error");
                                    err.setHeaderText("Failed to persist playlists");
                                    err.setContentText(ex.getMessage());
                                    err.showAndWait();
                                }
                                refreshPlaylistList();
                                if (currentPlaylist != null && currentPlaylist.equals(p)) {
                                    currentPlaylist = null;
                                    refreshSongList();
                                }
                            }
                    });
                }
            });
            menu.getItems().add(delete);
            cell.setContextMenu(menu);
            return cell;
        });
        playlistListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Playlist selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
                if (selectedPlaylist != null) {
                    loadPlaylist(selectedPlaylist);
                }
            }
        });
        
        refreshPlaylistList();
        
        playlistPane.getChildren().addAll(playlistHeader, playlistListView);
        VBox.setVgrow(playlistListView, Priority.ALWAYS);
        
        return playlistPane;
    }
    
    private VBox createBottomControls() {
        VBox bottomControls = new VBox(5);
        bottomControls.setPadding(new Insets(10));
        bottomControls.setStyle("-fx-background-color: #f0f0f0;");
        
        // Now playing info
        nowPlayingLabel = new Label("No song playing");
        nowPlayingLabel.setStyle("-fx-font-weight: bold;");
        
        // Progress controls
        HBox progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER);
        
        timeLabel = new Label("0:00 / 0:00");
        progressSlider = new Slider(0, 100, 0);
        // Handle clicks and drags: when user presses, stop automatic updates; on release, seek
        progressSlider.setOnMousePressed(e -> isSeeking = true);
        progressSlider.setOnMouseReleased(e -> {
            isSeeking = false;
            seekToPosition();
        });
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // If user is seeking we update only the label; actual seek happens on mouse release
            if (isSeeking) {
                long durationUs = audioPlayer.getDuration();
                long posUs = (long) (durationUs * (newVal.doubleValue() / 100.0));
                timeLabel.setText(formatTime(posUs / 1000000) + " / " + formatTime(durationUs / 1000000));
            }
        });
        
        progressBox.getChildren().addAll(timeLabel, progressSlider);
        HBox.setHgrow(progressSlider, Priority.ALWAYS);
        
        // Playback controls
        HBox controlsBox = new HBox(10);
        controlsBox.setAlignment(Pos.CENTER);
        
        previousButton = new Button("⏮");
        playPauseButton = new Button("▶");
        stopButton = new Button("⏹");
        nextButton = new Button("⏭");
        
        previousButton.setOnAction(e -> playPrevious());
        playPauseButton.setOnAction(e -> togglePlayPause());
        stopButton.setOnAction(e -> stop());
        nextButton.setOnAction(e -> playNext());
        
        controlsBox.getChildren().addAll(previousButton, playPauseButton, stopButton, nextButton);
        
        // Volume control
        HBox volumeBox = new HBox(5);
        volumeBox.setAlignment(Pos.CENTER);
        Label volumeLabel = new Label("🔊");
        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            audioPlayer.setVolume(newVal.floatValue()));
        
        volumeBox.getChildren().addAll(volumeLabel, volumeSlider);
        
        // Combine all controls
        HBox allControls = new HBox(20);
        allControls.setAlignment(Pos.CENTER);
        allControls.getChildren().addAll(controlsBox, volumeBox);
        
        bottomControls.getChildren().addAll(nowPlayingLabel, progressBox, allControls);
        
        return bottomControls;
    }
    
    private void setupAudioPlayerListeners() {
        audioPlayer.addListener(new AudioPlayer.AudioPlayerListener() {
            @Override
            public void onSongChanged(Song song) {
                Platform.runLater(() -> {
                    nowPlayingLabel.setText("Now Playing: " + song.toString());
                });
            }
            
            @Override
            public void onPlayStateChanged(boolean isPlaying) {
                Platform.runLater(() -> {
                    playPauseButton.setText(isPlaying ? "⏸" : "▶");
                });
            }
            
            @Override
            public void onPositionChanged(long position, long duration) {
                Platform.runLater(() -> {
                    if (!isSeeking) {
                        double progress = duration > 0 ? (double) position / duration * 100 : 0;
                        progressSlider.setValue(progress);
                    }
                    
                    String currentTime = formatTime(position / 1000000);
                    String totalTime = formatTime(duration / 1000000);
                    timeLabel.setText(currentTime + " / " + totalTime);
                });
            }
            
            @Override
            public void onVolumeChanged(float volume) {
                Platform.runLater(() -> volumeSlider.setValue(volume));
            }
            
            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Audio Error");
                    alert.setHeaderText("Playback Error");
                    alert.setContentText(error);
                    alert.showAndWait();
                });
            }
            
            @Override
            public void onSongEnded() {
                // Advance to the next track when a song finishes
                Platform.runLater(() -> playNext());
            }
        });
    }
    
    // Event handlers and utility methods would continue here...
    // This is getting quite long, so I'll create separate methods for the remaining functionality
    
    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }
    
    private void refreshSongList() {
        songListView.getItems().clear();
        songListView.getItems().addAll(musicLibrary.getAllSongs());
    }
    
    private void refreshPlaylistList() {
        playlistListView.getItems().clear();
        playlistListView.getItems().addAll(musicLibrary.getAllPlaylists());
    }

    private void configureSongListForPlaylistMode() {
        // Configure cell factory with context menu for removing songs when viewing a playlist
        songListView.setCellFactory(listView -> {
            SongListCell cell = new SongListCell();
            ContextMenu menu = new ContextMenu();
            MenuItem remove = new MenuItem("Remove from playlist");
            remove.setOnAction(e -> {
                Song s = cell.getItem();
                if (currentPlaylist != null && s != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Remove Song");
                    confirm.setHeaderText("Remove '" + s.getTitle() + "' from playlist '" + currentPlaylist.getName() + "'");
                    confirm.setContentText("Are you sure?");
                    confirm.showAndWait().ifPresent(bt -> {
                            if (bt.getButtonData().isDefaultButton()) {
                                currentPlaylist.removeSong(s);
                                try {
                                    PlaylistIO.savePlaylists(musicLibrary.getAllPlaylists());
                                } catch (IOException ex) {
                                    Alert err = new Alert(Alert.AlertType.ERROR);
                                    err.setTitle("Save Error");
                                    err.setHeaderText("Failed to persist playlists");
                                    err.setContentText(ex.getMessage());
                                    err.showAndWait();
                                }
                                loadPlaylist(currentPlaylist);
                            }
                    });
                }
            });
            menu.getItems().add(remove);
            cell.setContextMenu(menu);

            // Drag handlers for reordering
            cell.setOnDragDetected(event -> {
                if (cell.getItem() == null) return;
                Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(songListView.getItems().indexOf(cell.getItem())));
                db.setContent(content);
                event.consume();
            });

            cell.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasString()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                event.consume();
            });

            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString()) {
                    int draggedIndex = Integer.parseInt(db.getString());
                    int thisIndex = cell.getIndex();
                    if (currentPlaylist != null) {
                        currentPlaylist.moveSong(draggedIndex, thisIndex);
                        try {
                            PlaylistIO.savePlaylists(musicLibrary.getAllPlaylists());
                        } catch (IOException ex) {
                            Alert err = new Alert(Alert.AlertType.ERROR);
                            err.setTitle("Save Error");
                            err.setHeaderText("Failed to persist playlists");
                            err.setContentText(ex.getMessage());
                            err.showAndWait();
                        }
                        loadPlaylist(currentPlaylist);
                        success = true;
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });

            return cell;
        });
    }
    
    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            refreshSongList();
        } else {
            List<Song> results = musicLibrary.searchSongs(query.trim());
            songListView.getItems().clear();
            songListView.getItems().addAll(results);
        }
    }
    
    private void playSong(Song song) {
        if (song == null) return;
        audioPlayer.playSong(song);

        // Update current list and index
        List<Song> currentList;
        if (currentPlaylist != null && currentPlaylist.getSongCount() > 0) {
            currentList = currentPlaylist.getSongs();
        } else {
            currentList = songListView.getItems();
        }

        currentSongIndex = currentList.indexOf(song);
        // Select in UI
        songListView.getSelectionModel().select(song);
    }
    
    private void togglePlayPause() {
        if (audioPlayer.isPlaying()) {
            audioPlayer.pause();
        } else if (audioPlayer.isPaused()) {
            audioPlayer.resume();
        }
    }
    
    private void stop() {
        audioPlayer.stop();
    }
    
    private void playNext() {
        List<Song> currentList;
        if (currentPlaylist != null && currentPlaylist.getSongCount() > 0) {
            currentList = currentPlaylist.getSongs();
        } else {
            currentList = songListView.getItems();
        }

        if (currentList == null || currentList.isEmpty()) return;

        int nextIndex = currentSongIndex + 1;
        if (nextIndex < 0 || nextIndex >= currentList.size()) {
            // reached end — stop playback
            audioPlayer.stop();
            return;
        }

        Song nextSong = currentList.get(nextIndex);
        if (nextSong != null) {
            currentSongIndex = nextIndex;
            playSong(nextSong);
        }
    }
    
    private void playPrevious() {
        List<Song> currentList;
        if (currentPlaylist != null && currentPlaylist.getSongCount() > 0) {
            currentList = currentPlaylist.getSongs();
        } else {
            currentList = songListView.getItems();
        }

        if (currentList == null || currentList.isEmpty()) return;

        int prevIndex = currentSongIndex - 1;
        if (prevIndex < 0) {
            // at start — restart current track
            if (currentSongIndex >= 0 && currentSongIndex < currentList.size()) {
                Song current = currentList.get(currentSongIndex);
                audioPlayer.seek(0);
                audioPlayer.playSong(current);
            }
            return;
        }

        Song prevSong = currentList.get(prevIndex);
        if (prevSong != null) {
            currentSongIndex = prevIndex;
            playSong(prevSong);
        }
    }
    
    private void seekToPosition() {
        try {
            double percent = progressSlider.getValue();
            long durationUs = audioPlayer.getDuration();
            if (durationUs <= 0) return;
            long targetUs = (long) (durationUs * (percent / 100.0));
            audioPlayer.seek(targetUs);
        } catch (Exception e) {
            // silent fail — individual audio backends may not support seeking
        }
    }
    
    private void importMusic() {
        try {
            ConfigManager config = ConfigManager.getInstance();
            String lastPath = config.getString("lastImportPath", System.getProperty("user.home"));

            // Offer choice: files or directory
            Alert choice = new Alert(Alert.AlertType.CONFIRMATION);
            choice.setTitle("Import Music");
            choice.setHeaderText("Import music files or folder");
            choice.setContentText("Choose import type:");

            ButtonType filesBtn = new ButtonType("Files");
            ButtonType folderBtn = new ButtonType("Folder");
            ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            choice.getButtonTypes().setAll(filesBtn, folderBtn, cancelBtn);
            ButtonType result = choice.showAndWait().orElse(cancelBtn);

            if (result == filesBtn) {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Import Music Files");
                chooser.setInitialDirectory(new File(lastPath).isDirectory() ? new File(lastPath) : new File(System.getProperty("user.home")));
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(MusicImporter.getFileFilterDescription(), "*.*"));
                List<File> files = chooser.showOpenMultipleDialog(primaryStage);
                if (files != null && !files.isEmpty()) {
                    File[] filesArr = files.toArray(File[]::new);
                    ImportResult importRes = MusicImporter.importAudioFiles(filesArr);
                    if (importRes.successCount() > 0) {
                        config.setString("lastImportPath", files.get(0).getParent());
                        if (!config.saveConfigSafe()) {
                            Alert err = new Alert(Alert.AlertType.ERROR);
                            err.setTitle("Config Save Error");
                            err.setHeaderText("Failed to save import path");
                            err.setContentText("Your import will continue but the last import path could not be saved.");
                            err.showAndWait();
                        }
                        refreshSongList();
                        try {
                            PlaylistIO.savePlaylists(musicLibrary.getAllPlaylists());
                        } catch (Exception ex) {
                            Alert err = new Alert(Alert.AlertType.ERROR);
                            err.setTitle("Save Error");
                            err.setHeaderText("Failed to persist playlists");
                            err.setContentText(ex.getMessage());
                            err.showAndWait();
                        }
                    }

                    // Show summary of results
                    Alert summary = new Alert(Alert.AlertType.INFORMATION);
                    summary.setTitle("Import Summary");
                    summary.setHeaderText("Imported " + importRes.successCount() + " songs, " + importRes.failureCount() + " failures");
                    if (importRes.failureCount() > 0) {
                        StringBuilder detail = new StringBuilder();
                        importRes.getFailures().forEach(f -> detail.append(f.getFile().getName()).append(": ").append(f.getReason()).append("\n"));
                        summary.setContentText(detail.toString());
                    }
                    summary.showAndWait();
                }

            } else if (result == folderBtn) {
                DirectoryChooser dirChooser = new DirectoryChooser();
                dirChooser.setTitle("Import Music Folder");
                dirChooser.setInitialDirectory(new File(lastPath).isDirectory() ? new File(lastPath) : new File(System.getProperty("user.home")));
                File dir = dirChooser.showDialog(primaryStage);
                if (dir != null && dir.isDirectory()) {
                    ImportResult importRes = MusicImporter.importFromDirectoryResult(dir);
                    if (importRes.successCount() > 0) {
                        config.setString("lastImportPath", dir.getAbsolutePath());
                        if (!config.saveConfigSafe()) {
                            Alert err = new Alert(Alert.AlertType.ERROR);
                            err.setTitle("Config Save Error");
                            err.setHeaderText("Failed to save import path");
                            err.setContentText("Your import will continue but the last import path could not be saved.");
                            err.showAndWait();
                        }
                        refreshSongList();
                        try {
                            PlaylistIO.savePlaylists(musicLibrary.getAllPlaylists());
                        } catch (Exception ex) {
                            Alert err = new Alert(Alert.AlertType.ERROR);
                            err.setTitle("Save Error");
                            err.setHeaderText("Failed to persist playlists");
                            err.setContentText(ex.getMessage());
                            err.showAndWait();
                        }
                    }

                    Alert summary = new Alert(Alert.AlertType.INFORMATION);
                    summary.setTitle("Import Summary");
                    summary.setHeaderText("Imported " + importRes.successCount() + " songs, " + importRes.failureCount() + " failures");
                    if (importRes.failureCount() > 0) {
                        StringBuilder detail = new StringBuilder();
                        importRes.getFailures().forEach(f -> detail.append(f.getFile().getName()).append(": ").append(f.getReason()).append("\n"));
                        summary.setContentText(detail.toString());
                    }
                    summary.showAndWait();
                }
            } else {
                // Cancelled
            }

        } catch (Exception e) {
            System.err.println("Import failed: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Import Error");
            alert.setHeaderText("Failed to import music");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void createNewPlaylist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Playlist");
        dialog.setHeaderText("Create a new playlist");
        dialog.setContentText("Playlist name:");

        dialog.showAndWait().ifPresent(name -> {
            if (name == null || name.trim().isEmpty()) return;
            Playlist playlist = new Playlist(name.trim());
            musicLibrary.addPlaylist(playlist);
            refreshPlaylistList();
            // Persist playlists
            try {
                PlaylistIO.savePlaylists(musicLibrary.getAllPlaylists());
            } catch (IOException ex) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Save Error");
                err.setHeaderText("Failed to persist playlists");
                err.setContentText(ex.getMessage());
                err.showAndWait();
            }
        });
    }
    
    private void loadPlaylist(Playlist playlist) {
    if (playlist == null) return;
    this.currentPlaylist = playlist;
    songListView.getItems().clear();
    songListView.getItems().addAll(playlist.getSongs());
    currentSongIndex = -1;
    // Configure song list for playlist actions (remove/reorder)
    configureSongListForPlaylistMode();
    }
    
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Spotify Clone");
        alert.setContentText("A simple music player built with JavaFX\nVersion 1.0");
        alert.showAndWait();
    }
}