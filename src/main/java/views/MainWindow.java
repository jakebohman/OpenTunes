package views;

import java.io.File;
import java.io.IOException;
import java.util.List;

import controllers.AudioPlayer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
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
import javafx.scene.image.Image;
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
import models.Album;
import models.Artist;
import models.MusicLibrary;
import models.Playlist;
import models.Song;
import utils.ConfigManager;
import utils.ImportResult;
import utils.MusicImporter;
import utils.MusicLibraryIO;
import utils.PlaylistIO;

/**
 * Main window for the Spotify clone application
 */
public class MainWindow {

    private Stage primaryStage; // primary stage
    private MusicLibrary musicLibrary; // the music library
    private MusicPlayerController controller; // playback controller

    // UI Components
    private ListView<Song> songListView; // list of songs
    private ListView<Playlist> playlistListView; // list of playlists
    private Label nowPlayingLabel; // label showing current song
    private Label timeLabel; // label showing current time / total duration
    private Slider progressSlider; // slider for song progress
    private Slider volumeSlider; // slider for volume
    private Label volumeLabel; // label for volume icon
    private volatile boolean adjustingVolume = false; // true when programmatically updating volume slider to avoid feedback loops
    private Button playPauseButton; // play/pause button
    private Button stopButton; // stop button
    private Button previousButton; // previous button
    private Button nextButton; // next button
    private Button loopButton; // loop button
    private TextField searchField; // search field

    // Placeholder ID used for the 'View Full Library' top list item
    private static final String LIBRARY_PLACEHOLDER_ID = "__VIEW_LIBRARY__";

    // Current state
    private Playlist currentPlaylist; // currently loaded playlist (null = library mode)
    private boolean isSeeking = false; // true if user is currently dragging the progress slider
    private boolean loopCurrent = false; // true if current song should loop when finished

    /*
     * Constructor
     */
    public MainWindow(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.musicLibrary = MusicLibrary.getInstance();
        this.controller = new MusicPlayerController(this.musicLibrary);
        // Load persisted library and playlists at startup
        try {
            MusicLibraryIO.loadLibrary(musicLibrary);
            musicLibrary.getAllPlaylists().addAll(PlaylistIO.loadPlaylists());
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Load Error");
            alert.setHeaderText("Failed to load library or playlists");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }

        setupAudioPlayerListeners();
        initializeUI();
    }

    /*
     * Initialize the main UI components and layout
     */
    private void initializeUI() {
        primaryStage.setTitle("OpenTunes");

        // Create main layout
        BorderPane mainLayout = new BorderPane();

        // Create menu bar
        MenuBar menuBar = createMenuBar();
        mainLayout.setTop(menuBar);

        // Create center content (split between library and playlists)
        SplitPane centerPane = createCenterPane();
        mainLayout.setCenter(centerPane);

        // After UI lists are created, inform controller about current list (library mode)
        controller.setCurrentList(songListView.getItems());

        // Create bottom controls
        VBox bottomControls = createBottomControls();
        mainLayout.setBottom(bottomControls);

        // Create scene
        Scene scene = new Scene(mainLayout, 1200, 800);

        // Load window icon
        try {
            Image icon = new Image(getClass().getResourceAsStream("/note_icon.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception ignored) {
        }

        // Load css
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception ignored) {
        }
        primaryStage.setScene(scene);
        // Scene-level accelerators (playback controls)
        try {
            scene.getAccelerators().put(
                    javafx.scene.input.KeyCombination.keyCombination("CTRL+SPACE"),
                    () -> togglePlayPause());

            scene.getAccelerators().put(
                    javafx.scene.input.KeyCombination.keyCombination("CTRL+RIGHT"),
                    () -> playNext());

            scene.getAccelerators().put(
                    javafx.scene.input.KeyCombination.keyCombination("CTRL+LEFT"),
                    () -> playPrevious());
        } catch (Exception ignored) {
        }
        // On close, stop playback threads, persist library and playlists and save config
        primaryStage.setOnCloseRequest(evt -> {
            // Ensure audio/background threads are stopped cleanly
            if (controller != null) {
                try {
                    controller.shutdown();
                } catch (Exception e) {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Shutdown Error");
                    err.setHeaderText("Failed to stop audio player");
                    err.setContentText(e.getMessage());
                    err.showAndWait();
                }
            }

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

    /*
     * Create the top menu bar with menus and items
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("menu-bar");

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem importItem = new MenuItem("Import Music...");
        importItem.setOnAction(e -> importMusic());
        importItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("CTRL+I"));
        importItem.setId("menu-import");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());
        exitItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("CTRL+Q"));
        fileMenu.getItems().addAll(importItem, new SeparatorMenuItem(), exitItem);

        // Playlist menu
        Menu playlistMenu = new Menu("Playlist");
        MenuItem newPlaylistItem = new MenuItem("New Playlist...");
        newPlaylistItem.setOnAction(e -> createNewPlaylist());
        newPlaylistItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("CTRL+N"));
        newPlaylistItem.setId("menu-new-playlist");
        playlistMenu.getItems().add(newPlaylistItem);

        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAbout());
        aboutItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("F1"));
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, playlistMenu, helpMenu);
        return menuBar;
    }

    /*
     * Create the center pane with library and playlists split
     */
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

    /*
     * Create the library pane with search and song list
     */
    private VBox createLibraryPane() {
        VBox libraryPane = new VBox(10);
        libraryPane.setPadding(new Insets(10));

        // Search bar
        searchField = new TextField();
        searchField.setPromptText("Search songs, artists, albums...");
        searchField.setAccessibleText("Search songs, artists, albums");
        searchField.textProperty().addListener((obs, oldText, newText) -> performSearch(newText));

        // Songs list
        Label songsLabel = new Label("Songs");
        songsLabel.getStyleClass().add("section-label");
        songsLabel.setLabelFor(songListView);

        songListView = new ListView<>();
        // Use the shared/common cell factory so both library and playlist modes have identical menus
        setCommonSongCellFactory();
        songListView.setId("song-list");
        songListView.setAccessibleText("Songs list");
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

    /*
     * Create the playlist pane with playlist list and controls
     */
    private VBox createPlaylistPane() {
        VBox playlistPane = new VBox(10);
        playlistPane.setPadding(new Insets(10));

        // Playlists header with add button
        HBox playlistHeader = new HBox(6);
        Label playlistLabel = new Label("Playlists");
        playlistLabel.getStyleClass().add("section-label");
        playlistLabel.setLabelFor(playlistListView);
        Button addPlaylistButton = new Button("+");
        addPlaylistButton.setOnAction(e -> createNewPlaylist());
        addPlaylistButton.getStyleClass().add("add-playlist-button");

        // Slightly raise the '+' button to align with the "Playlists" label
        addPlaylistButton.setTranslateY(-4);
        playlistHeader.getChildren().addAll(playlistLabel, addPlaylistButton);

        // Playlists list
        playlistListView = new ListView<>();
        playlistListView.setCellFactory(listView -> {
            PlaylistListCell cell = new PlaylistListCell();

            // Update context menu depending on which playlist is set on the cell.
            cell.itemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) {
                    cell.setContextMenu(null);
                    return;
                }

                // If this is the library placeholder, don't provide delete option
                if (LIBRARY_PLACEHOLDER_ID.equals(newVal.getName())) {
                    cell.setContextMenu(null);
                    return;
                }

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
            });

            return cell;
        });
        playlistListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Playlist selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
                if (selectedPlaylist != null) {
                    if (LIBRARY_PLACEHOLDER_ID.equals(selectedPlaylist.getName())) {
                        selectLibrary();
                    } else {
                        loadPlaylist(selectedPlaylist);
                    }
                }
            }
        });

        refreshPlaylistList();

        playlistListView.setId("playlist-list");
        playlistListView.setAccessibleText("Playlists list");

        // Add header and the playlist list (the top item represents the library)
        playlistPane.getChildren().addAll(playlistHeader, playlistListView);
        VBox.setVgrow(playlistListView, Priority.ALWAYS);

        return playlistPane;
    }

    /*
     * Create the bottom controls with playback buttons, progress, and volume
     */
    private VBox createBottomControls() {
        VBox bottomControls = new VBox(5);
        bottomControls.setPadding(new Insets(10));
        bottomControls.getStyleClass().add("bottom-controls");

        // Now playing info
        nowPlayingLabel = new Label("No song playing");
        nowPlayingLabel.getStyleClass().add("now-playing-label");
        nowPlayingLabel.getStyleClass().add("now-playing-label");
        nowPlayingLabel.setId("now-playing");
        nowPlayingLabel.setAccessibleText("Now playing label");

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
                long durationUs = controller.getDuration();
                long posUs = (long) (durationUs * (newVal.doubleValue() / 100.0));
                timeLabel.setText(formatTime(posUs / 1000000) + " / " + formatTime(durationUs / 1000000));
            }
            // update visual fill for the slider (left-of-thumb)
            setSliderFill(progressSlider, newVal.doubleValue());
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
        loopButton = new Button("🔁");
        previousButton.getStyleClass().add("controls-button");
        playPauseButton.getStyleClass().add("controls-button");
        stopButton.getStyleClass().add("controls-button");
        nextButton.getStyleClass().add("controls-button");

        // Accessibility and accelerators
        playPauseButton.setId("btn-play-pause");
        playPauseButton.setAccessibleText("Play or pause");

        // Keep play/pause visually consistent with other control buttons
        playPauseButton.setPrefWidth(36.0);
        previousButton.setId("btn-previous");
        previousButton.setAccessibleText("Previous track");
        nextButton.setId("btn-next");
        nextButton.setAccessibleText("Next track");
        stopButton.setId("btn-stop");
        stopButton.setAccessibleText("Stop playback");

        // Keyboard shortcuts for playback (Ctrl+Space = Play/Pause, Ctrl+Right = Next, Ctrl+Left = Previous)
        playPauseButton.getScene(); // ensure scene accessibility for accelerators later

        previousButton.setOnAction(e -> playPrevious());
        playPauseButton.setOnAction(e -> togglePlayPause());
        stopButton.setOnAction(e -> stop());
        nextButton.setOnAction(e -> playNext());

        // Make loop visually consistent and set a smaller width
        loopButton.getStyleClass().add("controls-button");
        loopButton.setPrefWidth(36.0);
        loopButton.setId("btn-loop");

        loopButton.setOnAction(e -> {
            loopCurrent = !loopCurrent;
            // visually indicate state
            loopButton.getStyleClass().removeAll("loop-on");
            if (loopCurrent) {
                loopButton.getStyleClass().add("loop-on");
            }
        });

        // Ensure stop button is small and matches loop
        stopButton.getStyleClass().add("controls-button");
        stopButton.setPrefWidth(36.0);

        // Order: previous, play/pause, next, loop, stop (loop/stop to the right of forward)
        controlsBox.getChildren().addAll(previousButton, playPauseButton, nextButton, loopButton, stopButton);

        // Volume control
        HBox volumeBox = new HBox(5);
        volumeBox.setAlignment(Pos.CENTER);
        this.volumeLabel = new Label("🔊");
        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Ignore changes that originate from programmatic updates
            if (adjustingVolume) {
                return;
            }
            float v = newVal.floatValue();
            controller.setVolume(v);
            // update icon: muted when exactly zero
            if (v <= 0.0001f) {
                volumeLabel.setText("🔇");
            } else {
                volumeLabel.setText("🔊");
            }
        });
        volumeSlider.getStyleClass().add("volume-slider");
        volumeSlider.setId("volume-slider");
        volumeSlider.setAccessibleText("Volume");

        // Update visual fill for volume slider (value is 0.0-1.0)
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> setSliderFill(volumeSlider, newVal.doubleValue() * 100.0));
        setSliderFill(progressSlider, progressSlider.getValue());
        setSliderFill(volumeSlider, volumeSlider.getValue() * 100.0);

        volumeBox.getChildren().addAll(volumeLabel, volumeSlider);

        // Combine all controls
        HBox allControls = new HBox(20);
        allControls.setAlignment(Pos.CENTER);
        allControls.getChildren().addAll(controlsBox, volumeBox);

        bottomControls.getChildren().addAll(nowPlayingLabel, progressBox, allControls);

        return bottomControls;
    }

    /*
     * Setup listeners for audio player events
     */
    private void setupAudioPlayerListeners() {
        controller.addListener(new AudioPlayer.AudioPlayerListener() {
            @Override
            public void onSongChanged(Song song) {
                Platform.runLater(() -> {
                    nowPlayingLabel.setText("Now Playing: " + song.toString());
                });
            }

            @Override
            public void onPlayStateChanged(controllers.AudioPlayer.PlaybackState state) {
                Platform.runLater(() -> {
                    playPauseButton.setText(state == controllers.AudioPlayer.PlaybackState.PLAYING ? "⏸" : "▶");
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
                Platform.runLater(() -> {
                    adjustingVolume = true;
                    try {
                        volumeSlider.setValue(volume);
                        // also update icon when programmatically changing
                        if (volume <= 0.0001f) {
                            volumeLabel.setText("🔇");
                        } else {
                            volumeLabel.setText("🔊");
                        }
                    } finally {
                        adjustingVolume = false;
                    }
                });
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
                // Advance to the next track when a song finishes (or loop if enabled)
                Platform.runLater(() -> {
                    if (loopCurrent) {
                        // restart the same song
                        List<Song> currentList;
                        if (currentPlaylist != null && currentPlaylist.getSongCount() > 0) {
                            currentList = currentPlaylist.getSongs();
                        } else {
                            currentList = songListView.getItems();
                        }
                        int idx = controller.getCurrentSongIndex();
                        if (idx >= 0 && idx < currentList.size()) {
                            Song s = currentList.get(idx);
                            if (s != null) {
                                controller.seek(0);
                                controller.playSong(s);
                            }
                        }
                    } else {
                        playNext();
                    }
                });
            }
        });
    }

    /*
     * Format time in seconds to M:SS format
     */
    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }

    /*
     * Refresh the song list to show all songs in the library
     */
    private void refreshSongList() {
        songListView.getItems().clear();
        songListView.getItems().addAll(musicLibrary.getAllSongs());
        // Keep controller in sync with the currently-shown list
        if (controller != null) {
            controller.setCurrentList(songListView.getItems());
        }
    }

    /*
     * Refresh the playlist list to show all playlists, with the library placeholder on top
     */
    private void refreshPlaylistList() {
        playlistListView.getItems().clear();
        // Add the library placeholder as the first item
        Playlist placeholder = new Playlist(LIBRARY_PLACEHOLDER_ID);
        playlistListView.getItems().add(placeholder);

        // Add all user playlists after the placeholder
        for (Playlist p : musicLibrary.getAllPlaylists()) {
            // Skip any playlists that accidentally have the reserved placeholder id
            if (p != null && !LIBRARY_PLACEHOLDER_ID.equals(p.getName())) {
                playlistListView.getItems().add(p);
            }
        }
    }

    /**
     * Set visual fill for a slider: fills left side up to value percent using
     * accent color
     */
    private void setSliderFill(javafx.scene.control.Slider slider, double percent) {
        percent = Math.max(0.0, Math.min(100.0, percent));
        final double p = percent;
        final String color = "#5ac8fa"; // light blue (iTunes-like)
        final String style = String.format("-fx-background-color: linear-gradient(to right, %s %s%%, transparent %s%%);", color, p, p);

        try {
            // Try to apply style to the track node (so only the inside of the progress bar is filled)
            javafx.application.Platform.runLater(() -> {
                try {
                    javafx.scene.Node track = slider.lookup(".track");
                    if (track != null) {
                        track.setStyle(style);
                        return;
                    }
                    // Fallback: try the bar element
                    javafx.scene.Node bar = slider.lookup(".track .bar");
                    if (bar != null) {
                        bar.setStyle(style);
                        return;
                    }
                    // Fallback to applying style on slider itself
                    slider.setStyle(style);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    /**
     * Show the full library (deselect any playlist)
     */
    private void selectLibrary() {
        this.currentPlaylist = null;
        // Refresh song list to show entire library
        refreshSongList();
        // Clear playlist selection
        // Select the placeholder top item to indicate library is active
        if (!playlistListView.getItems().isEmpty()) {
            playlistListView.getSelectionModel().select(0);
        }

        // Configure song list for library mode (shared factory)
        setCommonSongCellFactory();
        if (controller != null) {
            controller.setCurrentList(songListView.getItems());
        }
    }

    /*
     * Configure the song list for playlist mode (drag-and-drop reordering, playlist-specific menu)
     */
    private void configureSongListForPlaylistMode() {
        // Use the shared/common cell factory so playlist mode shows the same menu as library mode
        setCommonSongCellFactory();
        if (controller != null && currentPlaylist != null) {
            controller.setCurrentPlaylist(currentPlaylist);
        }
    }

    /**
     * Install the single shared song cell factory (library-style) on the
     * songListView. This ensures library and playlist modes have identical
     * right-click menus.
     */
    private void setCommonSongCellFactory() {
        songListView.setCellFactory(listView -> {
            SongListCell cell = new SongListCell();
            ContextMenu menu = new ContextMenu();

            MenuItem edit = new MenuItem("Edit Metadata");
            edit.setOnAction(e -> {
                Song s = cell.getItem();
                if (s != null) {
                    editSongMetadata(s);
                }
            });

            MenuItem addTo = new MenuItem("Add to Playlist...");
            addTo.setOnAction(e -> {
                Song s = cell.getItem();
                if (s != null) {
                    addSongToPlaylist(s);
                }
            });

            MenuItem remove = new MenuItem("Remove from playlist...");
            remove.setOnAction(e -> {
                Song s = cell.getItem();
                if (s == null) {
                    return;
                }

                // Only show playlists that actually contain this song
                List<Playlist> allPlaylists = musicLibrary.getAllPlaylists();
                if (allPlaylists == null || allPlaylists.isEmpty()) {
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("No Playlists");
                    info.setHeaderText("No playlists available");
                    info.setContentText("There are no playlists to remove songs from.");
                    info.showAndWait();
                    return;
                }
                List<Playlist> containing = new java.util.ArrayList<>();
                for (Playlist p : allPlaylists) {
                    if (p.getSongs().contains(s)) {
                        containing.add(p);
                    }
                }
                // If no playlists contain this song, inform the user
                if (containing.isEmpty()) {
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Not Found");
                    info.setHeaderText("Song not in any playlist");
                    info.setContentText("This song isn't present in any playlist.");
                    info.showAndWait();
                    return;
                }
                // If only one playlist contains this song, skip the choice dialog and confirm removal
                ChoiceDialog<Playlist> dialog = new ChoiceDialog<>(containing.contains(currentPlaylist) ? currentPlaylist : containing.get(0), containing);
                dialog.setTitle("Remove from Playlist");
                dialog.setHeaderText("Choose a playlist to remove the song from");
                dialog.setContentText("Playlist:");
                dialog.showAndWait().ifPresent(chosen -> {
                    if (chosen == null) {
                        return;
                    }
                    if (!chosen.getSongs().contains(s)) {
                        Alert info = new Alert(Alert.AlertType.INFORMATION);
                        info.setTitle("Not Found");
                        info.setHeaderText("Song not in playlist");
                        info.setContentText("The selected playlist '" + chosen.getName() + "' does not contain this song.");
                        info.showAndWait();
                        return;
                    }
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Remove Song");
                    confirm.setHeaderText("Remove '" + s.getTitle() + "' from playlist '" + chosen.getName() + "'");
                    confirm.setContentText("Are you sure?");
                    confirm.showAndWait().ifPresent(bt -> {
                        if (bt.getButtonData().isDefaultButton()) {
                            chosen.removeSong(s);
                            try {
                                PlaylistIO.savePlaylists(musicLibrary.getAllPlaylists());
                            } catch (IOException ex) {
                                Alert err = new Alert(Alert.AlertType.ERROR);
                                err.setTitle("Save Error");
                                err.setHeaderText("Failed to persist playlists");
                                err.setContentText(ex.getMessage());
                                err.showAndWait();
                            }
                            // If the user is viewing the playlist we removed from, reload it
                            if (currentPlaylist != null && currentPlaylist.equals(chosen)) {
                                loadPlaylist(currentPlaylist);
                            }
                            refreshPlaylistList();
                        }
                    });
                });
            });

            menu.getItems().addAll(edit, addTo, remove);
            cell.setContextMenu(menu);

            // If a playlist is active, enable drag-and-drop reordering (playlist-only behavior)
            if (currentPlaylist != null) {
                cell.setOnDragDetected(event -> {
                    if (cell.getItem() == null) {
                        return;
                    }
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
            }

            return cell;
        });
    }

    /*
     * Perform a search in the music library and update the song list
     */
    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            refreshSongList();
        } else {
            List<Song> results = musicLibrary.searchSongs(query.trim());
            songListView.getItems().clear();
            songListView.getItems().addAll(results);
        }
    }

    /*
     * Play the specified song and update UI accordingly
     */
    private void playSong(Song song) {
        if (song == null) {
            return;
        }
        controller.playSong(song);
        // Select in UI
        songListView.getSelectionModel().select(song);
    }

    /*
     * Edit metadata of the specified song via a dialog
     */
    private void editSongMetadata(Song song) {
        if (song == null) {
            return;
        }
        // Create a single dialog with all editable fields
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Song Metadata");
        dialog.setHeaderText("Edit metadata for: " + song.getTitle());

        // Buttons
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, cancelBtn);

        // Fields
        TextField titleField = new TextField(song.getTitle());
        TextField artistField = new TextField(song.getArtist() != null ? song.getArtist().getName() : "");
        TextField albumField = new TextField(song.getAlbum() != null ? song.getAlbum().getTitle() : "");
        TextField genreField = new TextField(song.getGenre() != null ? song.getGenre() : "");
        TextField trackField = new TextField(song.getTrackNumber() > 0 ? String.valueOf(song.getTrackNumber()) : "");
        TextField pathField = new TextField(song.getFilePath() != null ? song.getFilePath() : "");

        // Layout
        VBox content = new VBox(8);
        content.getChildren().addAll(
                new Label("Title:"), titleField,
                new Label("Artist:"), artistField,
                new Label("Album:"), albumField,
                new Label("Genre:"), genreField,
                new Label("Track number:"), trackField,
                new Label("File path:"), pathField
        );
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> btn);
        dialog.showAndWait().ifPresent(result -> {
            if (result == saveBtn) {
                String newTitle = titleField.getText() != null ? titleField.getText().trim() : "";
                String newArtist = artistField.getText() != null ? artistField.getText().trim() : "";
                String newAlbum = albumField.getText() != null ? albumField.getText().trim() : "";
                String newGenre = genreField.getText() != null ? genreField.getText().trim() : "";
                String newTrack = trackField.getText() != null ? trackField.getText().trim() : "";
                String newPath = pathField.getText() != null ? pathField.getText().trim() : "";

                if (!newTitle.isEmpty()) {
                    song.setTitle(newTitle);
                }

                // Artist: find or create
                Artist artistObj = song.getArtist();
                if (!newArtist.isEmpty()) {
                    Artist found = null;
                    for (Artist a : musicLibrary.getAllArtists()) {
                        if (a.getName().equals(newArtist)) {
                            found = a;
                            break;
                        }
                    }
                    if (found == null) {
                        found = new Artist(newArtist);
                        musicLibrary.addArtist(found);
                    }
                    artistObj = found;
                    song.setArtist(found);
                }

                // Album: find or create and associate with artist
                if (!newAlbum.isEmpty()) {
                    Album found = null;
                    for (Album al : musicLibrary.getAllAlbums()) {
                        if (al.getTitle().equals(newAlbum) && al.getArtist().equals(artistObj)) {
                            found = al;
                            break;
                        }
                    }
                    if (found == null) {
                        found = new Album(newAlbum, artistObj);
                        musicLibrary.addAlbum(found);
                        if (artistObj != null) {
                            artistObj.addAlbum(found);
                        }
                    }
                    song.setAlbum(found);
                    found.addSong(song);
                }

                if (!newGenre.isEmpty()) {
                    song.setGenre(newGenre);
                }
                if (!newTrack.isEmpty()) {
                    try {
                        song.setTrackNumber(Integer.parseInt(newTrack));
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (!newPath.isEmpty()) {
                    song.setFilePath(newPath);
                }

                try {
                    MusicLibraryIO.saveLibrary(musicLibrary);
                } catch (IOException e) {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Save Error");
                    err.setHeaderText("Failed to save library");
                    err.setContentText(e.getMessage());
                    err.showAndWait();
                }

                refreshSongList();
            }
        });
    }

    /*
     * Add the specified song to a playlist (existing or new)
     */
    private void addSongToPlaylist(Song song) {
        if (song == null) {
            return;
        }
        List<Playlist> playlists = musicLibrary.getAllPlaylists();
        if (playlists == null || playlists.isEmpty()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("No Playlists");
            info.setHeaderText("No playlists available");
            info.setContentText("There are no playlists to add songs to. Create a playlist first.");
            info.showAndWait();
            return;
        }

        ChoiceDialog<Playlist> dialog = new ChoiceDialog<>(null, playlists);
        dialog.setTitle("Add to Playlist");
        dialog.setHeaderText("Choose a playlist to add the song to");
        dialog.setContentText("Playlist:");
        // Provide an option to create new playlist if none or user wants
        dialog.showAndWait().ifPresent(chosen -> {
            if (chosen == null) {
                // Prompt for new playlist name
                TextInputDialog create = new TextInputDialog();
                create.setTitle("New Playlist");
                create.setHeaderText("Create a new playlist");
                create.setContentText("Playlist name:");
                create.showAndWait().ifPresent(name -> {
                    if (name != null && !name.trim().isEmpty()) {
                        Playlist p = new Playlist(name.trim());
                        p.addSong(song);
                        musicLibrary.addPlaylist(p);
                        try {
                            PlaylistIO.savePlaylists(musicLibrary.getAllPlaylists());
                        } catch (IOException ex) {
                        }
                        refreshPlaylistList();
                    }
                });
            } else {
                if (!chosen.getSongs().contains(song)) {
                    chosen.addSong(song);
                    try {
                        PlaylistIO.savePlaylists(musicLibrary.getAllPlaylists());
                    } catch (IOException ex) {
                    }
                    refreshPlaylistList();
                }
            }
        });
    }

    /*
     * Toggle play/pause state of the audio player
     */
    private void togglePlayPause() {
        controller.togglePlayPause();
    }

    /*
     * Stop playback and reset UI state
     */
    private void stop() {
        controller.stop();
        // Reset UI state
        songListView.getSelectionModel().clearSelection();
        progressSlider.setValue(0);
        nowPlayingLabel.setText("No song playing");
    }

    /*
     * Play the next song in the current list (playlist or library)
     */
    private void playNext() {
        controller.playNext();
        Song s = controller.getCurrentSong();
        if (s != null) {
            songListView.getSelectionModel().select(s);
        }
    }

    /*
     * Play the previous song in the current list (playlist or library). If at the start, restart the current track.
     */
    private void playPrevious() {
        controller.playPrevious();
        Song s = controller.getCurrentSong();
        if (s != null) {
            songListView.getSelectionModel().select(s);
        }
    }

    /*
     * Seek to the position indicated by the progress slider
     */
    private void seekToPosition() {
        try {
            double percent = progressSlider.getValue();
            long durationUs = controller.getDuration();
            if (durationUs <= 0) {
                return;
            }
            long targetUs = (long) (durationUs * (percent / 100.0));
            controller.seek(targetUs);
        } catch (Exception e) {
            // silent fail — individual audio backends may not support seeking
        }
    }

    /*
     * Import music files or a folder, updating the library and UI accordingly
     */
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
                        } catch (IOException ex) {
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
                        } catch (IOException ex) {
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

    /*
     * Create a new playlist via a dialog and update UI accordingly
     */
    private void createNewPlaylist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Playlist");
        dialog.setHeaderText("Create a new playlist");
        dialog.setContentText("Playlist name:");

        dialog.showAndWait().ifPresent(name -> {
            if (name == null || name.trim().isEmpty()) {
                return;
            }
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

    /*
     * Load and display the specified playlist in the song list
     */
    private void loadPlaylist(Playlist playlist) {
        if (playlist == null) {
            return;
        }
        this.currentPlaylist = playlist;
        songListView.getItems().clear();
        songListView.getItems().addAll(playlist.getSongs());
        if (controller != null) {
            controller.setCurrentPlaylist(playlist);
        }
        // Configure song list for playlist actions (remove/reorder)
        configureSongListForPlaylistMode();
    }

    /*
     * Show the About dialog with application information
     */
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("OpenTunes");
        alert.setContentText("A simple music player built with JavaFX\nVersion 1.0");
        alert.showAndWait();
    }
}