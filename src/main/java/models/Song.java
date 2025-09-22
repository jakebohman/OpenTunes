package models;

import java.io.File;
import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a song in the music library
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Song {

    private String title;
    private Artist artist;
    private Album album;
    private int durationSeconds;
    private String genre;
    private int trackNumber;
    private LocalDate dateAdded;
    private File audioFile;
    private String filePath;
    private long fileSizeBytes;

    public Song(String title, Artist artist) {
        this.title = title;
        this.artist = artist;
        this.durationSeconds = 0;
        this.genre = "";
        this.trackNumber = 0;
        this.dateAdded = LocalDate.now();
        this.fileSizeBytes = 0;
    }

    // No-arg constructor for JSON deserialization
    public Song() {
        this.title = "";
        this.artist = null;
        this.album = null;
        this.durationSeconds = 0;
        this.genre = "";
        this.trackNumber = 0;
        this.dateAdded = LocalDate.now();
        this.fileSizeBytes = 0;
    }

    public Song(String title, Artist artist, Album album, int durationSeconds) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.durationSeconds = durationSeconds;
        this.genre = "";
        this.trackNumber = 0;
        this.dateAdded = LocalDate.now();
        this.fileSizeBytes = 0;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public LocalDate getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDate dateAdded) {
        this.dateAdded = dateAdded;
    }

    public File getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(File audioFile) {
        this.audioFile = audioFile;
        this.filePath = audioFile != null ? audioFile.getAbsolutePath() : null;
        this.fileSizeBytes = audioFile != null ? audioFile.length() : 0;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
        if (filePath != null) {
            this.audioFile = new File(filePath);
            this.fileSizeBytes = audioFile.exists() ? audioFile.length() : 0;
        }
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getFormattedDuration() {
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public String getFormattedFileSize() {
        if (fileSizeBytes < 1024) {
            return fileSizeBytes + " B";
        } else if (fileSizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", fileSizeBytes / 1024.0);
        } else {
            return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024.0));
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Song song = (Song) obj;
        return Objects.equals(title, song.title)
                && Objects.equals(artist, song.artist)
                && Objects.equals(album, song.album);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, artist, album);
    }

    @Override
    public String toString() {
        return title + " by " + artist.getName();
    }
}
