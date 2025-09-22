package com.spotify.clone.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a playlist in the music library
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Playlist {
    private String name;
    private String description;
    private List<Song> songs;
    private LocalDateTime createdDate;
    private LocalDateTime lastModified;
    private String coverImageUrl;
    
    public Playlist(String name) {
        this.name = name;
        this.description = "";
        this.songs = new ArrayList<>();
        this.createdDate = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.coverImageUrl = "";
    }

    // No-arg constructor for JSON deserialization
    public Playlist() {
        this.name = "";
        this.description = "";
        this.songs = new ArrayList<>();
        this.createdDate = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.coverImageUrl = "";
    }
    
    public Playlist(String name, String description) {
        this.name = name;
        this.description = description;
        this.songs = new ArrayList<>();
        this.createdDate = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.coverImageUrl = "";
    }
    
    // Getters and Setters
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
    
    public void addSong(Song song) {
        if (song != null && !songs.contains(song)) {
            songs.add(song);
            updateLastModified();
        }
    }
    
    public void addSong(int index, Song song) {
        if (song != null && !songs.contains(song)) {
            if (index >= 0 && index <= songs.size()) {
                songs.add(index, song);
                updateLastModified();
            }
        }
    }
    
    public void removeSong(Song song) {
        if (songs.remove(song)) {
            updateLastModified();
        }
    }
    
    public void removeSong(int index) {
        if (index >= 0 && index < songs.size()) {
            songs.remove(index);
            updateLastModified();
        }
    }
    
    public void moveSong(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < songs.size() && 
            toIndex >= 0 && toIndex < songs.size()) {
            Song song = songs.remove(fromIndex);
            songs.add(toIndex, song);
            updateLastModified();
        }
    }
    
    public void shuffleSongs() {
        Collections.shuffle(songs);
        updateLastModified();
    }
    
    public void clearPlaylist() {
        songs.clear();
        updateLastModified();
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
    
    private void updateLastModified() {
        this.lastModified = LocalDateTime.now();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
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