package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents an artist in the music library
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Artist {

    private String name;
    private String biography;
    private List<Album> albums;
    private String imageUrl;

    public Artist(String name) {
        this.name = name;
        this.albums = new ArrayList<>();
        this.biography = "";
        this.imageUrl = "";
    }

    // No-arg constructor for JSON deserialization
    public Artist() {
        this.name = "";
        this.albums = new ArrayList<>();
        this.biography = "";
        this.imageUrl = "";
    }

    public Artist(String name, String biography, String imageUrl) {
        this.name = name;
        this.biography = biography;
        this.imageUrl = imageUrl;
        this.albums = new ArrayList<>();
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    @JsonIgnore
    public List<Album> getAlbums() {
        return new ArrayList<>(albums);
    }

    public void addAlbum(Album album) {
        if (!albums.contains(album)) {
            albums.add(album);
        }
    }

    public void removeAlbum(Album album) {
        albums.remove(album);
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getAlbumCount() {
        return albums.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Artist artist = (Artist) obj;
        return Objects.equals(name, artist.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
