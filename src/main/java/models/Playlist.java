package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a playlist in the music library
 */
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unknown properties during JSON deserialization
public class Playlist {

    private String name; // Playlist name, must be unique
    private String description; // Optional description
    private List<Song> songs; // Songs in the playlist
    private LocalDateTime createdDate; // Date the playlist was created
    private LocalDateTime lastModified; // Date the playlist was last modified
    private String coverImageUrl; // URL to the playlist cover image

    /*
     * Constructor with name only
     */
    public Playlist(String name) {
        this.name = name;
        this.description = "";
        this.songs = new ArrayList<>();
        this.createdDate = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.coverImageUrl = "";
    }

    /*
     * Default constructor for JSON deserialization
     */
    public Playlist() {
        this.name = "";
        this.description = "";
        this.songs = new ArrayList<>();
        this.createdDate = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.coverImageUrl = "";
    }

    /*
     * Constructor with name and description
     */
    public Playlist(String name, String description) {
        this.name = name;
        this.description = description;
        this.songs = new ArrayList<>();
        this.createdDate = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.coverImageUrl = "";
    }

    /*
     * Add song to playlist if not already present
     */
    public void addSong(Song song) {
        if (song != null && !songs.contains(song)) {
            songs.add(song);
            updateLastModified();
        }
    }

    /*
     * Add song at specific index if not already present
     */
    public void addSong(int index, Song song) {
        if (song != null && !songs.contains(song)) {
            if (index >= 0 && index <= songs.size()) {
                songs.add(index, song);
                updateLastModified();
            }
        }
    }

    /*
     * Remove song from playlist by object
     */
    public void removeSong(Song song) {
        if (songs.remove(song)) {
            updateLastModified();
        }
    }

    /*
     * Remove song from playlist by index
     */
    public void removeSong(int index) {
        if (index >= 0 && index < songs.size()) {
            songs.remove(index);
            updateLastModified();
        }
    }

    /*
     * Move song from one index to another within the playlist
     */
    public void moveSong(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < songs.size()
                && toIndex >= 0 && toIndex < songs.size()) {
            Song song = songs.remove(fromIndex);
            songs.add(toIndex, song);
            updateLastModified();
        }
    }

    /*
     * Shuffle the songs in the playlist randomly
     */
    public void shuffleSongs() {
        Collections.shuffle(songs);
        updateLastModified();
    }

    /*
     * Clear all songs from the playlist
     */
    public void clearPlaylist() {
        songs.clear();
        updateLastModified();
    }

    /*
     * Getters and setters
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        updateLastModified();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        updateLastModified();
    }

    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
        updateLastModified();
    }

    public int getSongCount() {
        return songs.size();
    }

    public int getTotalDurationSeconds() {
        return songs.stream()
                .mapToInt(Song::getDurationSeconds)
                .sum();
    }

    /*
     * Get formatted duration as H:MM:SS or M:SS
     */
    public String getFormattedDuration() {
        int totalSeconds = getTotalDurationSeconds();
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    /*
     * Update the last modified timestamp to current time
     */
    private void updateLastModified() {
        this.lastModified = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Playlist playlist = (Playlist) obj;
        return Objects.equals(name, playlist.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name + " (" + getSongCount() + " songs)";
    }
}