package com.spotify.clone.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents an album in the music library
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Album {
    private String title;
    private Artist artist;
    private LocalDate releaseDate;
    private String genre;
    private List<Song> songs;
    private String coverImageUrl;
    
    public Album(String title, Artist artist) {
        this.title = title;
        this.artist = artist;
        this.songs = new ArrayList<>();
        this.releaseDate = LocalDate.now();
        this.genre = "";
        this.coverImageUrl = "";
    }

    // No-arg constructor for JSON deserialization
    public Album() {
        this.title = "";
        this.artist = null;
        this.songs = new ArrayList<>();
        this.releaseDate = LocalDate.now();
        this.genre = "";
        this.coverImageUrl = "";
    }
    
    public Album(String title, Artist artist, LocalDate releaseDate, String genre) {
        this.title = title;
        this.artist = artist;
        this.releaseDate = releaseDate;
        this.genre = genre;
        this.songs = new ArrayList<>();
        this.coverImageUrl = "";
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
    
    public LocalDate getReleaseDate() {
        return releaseDate;
    }
    
    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }
    
    public String getGenre() {
        return genre;
    }
    
    public void setGenre(String genre) {
        this.genre = genre;
    }
    
    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }
    
    public void addSong(Song song) {
        if (!songs.contains(song)) {
            songs.add(song);
            song.setAlbum(this);
        }
    }
    
    public void removeSong(Song song) {
        songs.remove(song);
    }
    
    public String getCoverImageUrl() {
        return coverImageUrl;
    }
    
    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }
    
    public int getSongCount() {
        return songs.size();
    }
    
    public int getTotalDurationSeconds() {
        return songs.stream()
                   .mapToInt(Song::getDurationSeconds)
                   .sum();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Album album = (Album) obj;
        return Objects.equals(title, album.title) && 
               Objects.equals(artist, album.artist);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(title, artist);
    }
    
    @Override
    public String toString() {
        return title + " by " + artist.getName();
    }
}