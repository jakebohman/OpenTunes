# Spotify Clone - Java Music Player

A barebones clone of Spotify/iTunes built using Java and JavaFX. This application provides basic music library management and playback functionality.

## Features

- **Music Library Management**: Import and organize songs, albums, and artists
- **Audio Playback**: Play, pause, stop, and seek through audio files
- **Playlists**: Create and manage custom playlists
- **Search**: Search through your music library
- **Metadata Support**: Automatically extract song information from audio files
- **File Format Support**: MP3, WAV, FLAC, M4A, OGG

## Project Structure

```
src/main/java/com/spotify/clone/
├── SpotifyCloneApp.java          # Main application entry point
├── controllers/
│   └── AudioPlayer.java          # Audio playback controller
├── models/
│   ├── Artist.java              # Artist data model
│   ├── Album.java               # Album data model
│   ├── Song.java                # Song data model
│   ├── Playlist.java            # Playlist data model
│   └── MusicLibrary.java        # Central library management
├── utils/
│   └── MusicImporter.java       # Audio file import utilities
└── views/
    ├── MainWindow.java          # Main application window
    ├── SongListCell.java        # Custom song display cell
    └── PlaylistListCell.java    # Custom playlist display cell
```

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- JavaFX (included as dependency)

## Dependencies

- **JavaFX**: Modern UI framework
- **JAudioTagger**: Audio metadata extraction
- **Jackson**: JSON processing for playlist persistence

## Setup and Installation

1. **Clone or download the project**
2. **Navigate to the project directory**:
   ```bash
   cd "Java Spotify Clone"
   ```

3. **Install dependencies**:
   ```bash
   mvn clean install
   ```

4. **Run the application**:
   ```bash
   mvn javafx:run
   ```

## Usage

### Importing Music
1. Use **File → Import Music...** to select audio files or folders
2. The application will automatically extract metadata (title, artist, album, etc.)
3. Songs will appear in the main library view

### Playing Music
- **Double-click** a song to play it
- Use the playback controls at the bottom:
  - ⏮ Previous
  - ▶/⏸ Play/Pause
  - ⏹ Stop
  - ⏭ Next
- Adjust volume using the volume slider
- Click on the progress bar to seek to a specific position

### Managing Playlists
1. Click the **+** button next to "Playlists"
2. Enter a playlist name
3. Drag songs from the library to the playlist (future enhancement)
4. Double-click a playlist to view its contents

### Searching
- Use the search bar at the top to find songs by title, artist, or album
- Results update in real-time as you type

## Building from Source

### Compile the project:
```bash
mvn compile
```

### Create executable JAR:
```bash
mvn package
```

### Run the JAR:
```bash
java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml -jar target/spotify-clone-1.0-SNAPSHOT.jar
```

## Troubleshooting

### Common Issues

1. **JavaFX not found**: Make sure JavaFX is properly installed or use the Maven JavaFX plugin
2. **Audio files not playing**: Ensure audio files are in supported formats (MP3, WAV, etc.)
3. **Metadata not extracted**: Install JAudioTagger dependency via Maven

### Supported Audio Formats

- **MP3**: Most common format
- **WAV**: Uncompressed audio
- **FLAC**: Lossless compression
- **M4A**: Apple format
- **OGG**: Open source format

## Future Enhancements

- Drag and drop playlist management
- Equalizer
- Crossfade between songs
- Internet radio support
- Album artwork display
- Advanced search filters
- Export/import playlists
- Keyboard shortcuts
- Dark theme

## Technical Details

### Architecture
- **MVC Pattern**: Separation of models, views, and controllers
- **Singleton Pattern**: MusicLibrary uses singleton for global access
- **Observer Pattern**: AudioPlayer uses listeners for UI updates

### Audio Processing
- Uses Java Sound API for basic audio playback
- JAudioTagger for metadata extraction
- Supports seeking and volume control

### UI Framework
- JavaFX for modern, responsive UI
- Custom list cells for song and playlist display
- Split pane layout for library and playlist management

## Contributing

This is a educational project demonstrating basic music player functionality. Feel free to extend it with additional features or improvements.

## License

This project is for educational purposes. Please respect copyright laws when using with copyrighted audio content.