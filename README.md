# OpenTunes - Java Music Player

A lightweight music player and library manager implemented with JavaFX. This project is a learning/example app that provides a small subset of features you expect from a desktop music player.

## What it does
- Music library management: import songs, group by artists/albums
- Basic audio playback (play / pause / stop / seek)
- Playlists: create, reorder, add/remove songs
- Metadata extraction using `jaudiotagger`
- JSON persistence for playlists and library snapshot using Jackson
- Supported audio formats (depends on platform + JavaFX): MP3, WAV, FLAC, M4A, OGG

## Prerequisites
- Java Development Kit (JDK) 11 installed and `JAVA_HOME` configured. The project is compiled for Java 11 (see `pom.xml`).
- Apache Maven 3.6+ (to build and run via the `javafx-maven-plugin`).
- A platform with working JavaFX media support (the project depends on JavaFX 17.0.2 via Maven).

If you plan to run the app from an IDE, make sure the IDE is configured to use JDK 11 and that Maven dependencies are resolved.

## Key libraries used
- JavaFX (controls, fxml, media) — `org.openjfx:javafx-*:17.0.2` (configured in `pom.xml`)
- jaudiotagger — `net.jthink:jaudiotagger` (metadata extraction)
- Jackson — `com.fasterxml.jackson` (JSON serialization for playlists/library)

## Build and run
From the project root you can build and run the app using Maven. On Windows PowerShell run:

```powershell
mvn clean javafx:run
```

This uses the `javafx-maven-plugin` configured in `pom.xml` and should start the JavaFX application. If you only want to build the jar without launching the UI:

```powershell
mvn clean package
```

To run the test suite:

```powershell
mvn test
```

## Where data is stored
- Playlists are saved/loaded from the project root file `playlists.json` by default.
- The library snapshot is saved/loaded from the project root file `music-library.json` by default.
- Settings saved/loaded from the project root file `open-tunes.properties` by default.

## License & Disclaimer
This project uses the MIT license.