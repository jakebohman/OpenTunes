package models;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Central management class for the music library
 */
public class MusicLibrary {

    private List<Song> songs; // All songs in the library
    private List<Artist> artists; // All artists in the library
    private List<Album> albums; // All albums in the library
    private List<Playlist> playlists; // All playlists in the library
    private static MusicLibrary instance; // Singleton instance

    /*
     * Private constructor for singleton pattern, restricts the class to a single instance
     */
    private MusicLibrary() {
        this.songs = new ArrayList<>();
        this.artists = new ArrayList<>();
        this.albums = new ArrayList<>();
        this.playlists = new ArrayList<>();
    }

    /*
     * Get the singleton instance of the music library
     */
    public static synchronized MusicLibrary getInstance() {
        if (instance == null) {
            instance = new MusicLibrary();
        }
        return instance;
    }

    /*
     * Add a song to the library, ensuring no duplicates by file path
     * Also adds associated artist and album if they do not already exist
     */
    public void addSong(Song song) {
        if (song == null) {
            return;
        }

        // If a song with the same file path already exists, treat as duplicate
        String path = song.getFilePath();
        if (path != null && !path.isEmpty()) {
            for (Song s : songs) {
                if (path.equals(s.getFilePath())) {
                    return;
                }
            }
        }

        // Avoid adding exact duplicate song objects
        if (songs.contains(song)) {
            return;
        }

        songs.add(song);

        // Add artist if not exists
        Artist artist = song.getArtist();
        if (artist != null && !artists.contains(artist)) {
            artists.add(artist);
        }

        // Add album if not exists
        Album album = song.getAlbum();
        if (album != null && !albums.contains(album)) {
            albums.add(album);
            if (artist != null) {
                artist.addAlbum(album);
            }
        }
    }

    /*
     * Remove a song from the library, also removing it from any playlists
     * and cleaning up empty albums or artists if necessary
     */
    public void removeSong(Song song) {
        songs.remove(song);

        // Remove from all playlists
        for (Playlist playlist : playlists) {
            playlist.removeSong(song);
        }

        // Remove album if it becomes empty
        Album album = song.getAlbum();
        if (album != null) {
            album.removeSong(song);
            if (album.getSongCount() == 0) {
                albums.remove(album);
                if (song.getArtist() != null) {
                    song.getArtist().removeAlbum(album);
                }
            }
        }

        // Remove artist if they have no more songs
        Artist artist = song.getArtist();
        if (artist != null && getSongsByArtist(artist).isEmpty()) {
            artists.remove(artist);
        }
    }

    /*
     * Get a list of all songs in the library
     */
    public List<Song> getAllSongs() {
        return new ArrayList<>(songs);
    }

    /*
     * Add an artist to the library if not already present
     */
    public void addArtist(Artist artist) {
        if (artist != null && !artists.contains(artist)) {
            artists.add(artist);
        }
    }

    /*
     * Get a list of all artists in the library
     */
    public List<Artist> getAllArtists() {
        return new ArrayList<>(artists);
    }

    /*
     * Add an album to the library, ensuring no duplicates
     * Also adds the associated artist if not already present
     */
    public void addAlbum(Album album) {
        if (album != null && !albums.contains(album)) {
            albums.add(album);

            // Add artist if not exists
            Artist artist = album.getArtist();
            if (artist != null && !artists.contains(artist)) {
                artists.add(artist);
            }
        }
    }

    /*
     * Get a list of all albums in the library
     */
    public List<Album> getAllAlbums() {
        return new ArrayList<>(albums);
    }

    /*
     * Add a playlist to the library if not already present
     */
    public void addPlaylist(Playlist playlist) {
        if (playlist != null && !playlists.contains(playlist)) {
            playlists.add(playlist);
        }
    }

    /*
     * Remove a playlist from the library
     */
    public void removePlaylist(Playlist playlist) {
        playlists.remove(playlist);
    }

    /*
     * Get a list of all playlists in the library
     */
    public List<Playlist> getAllPlaylists() {
        return new ArrayList<>(playlists);
    }

    /*
     * Search functions for songs, artists, albums, and playlists by name or relevant fields
     */
    public List<Song> searchSongs(String query) {
        String lowerQuery = query.toLowerCase();
        return songs.stream()
                .filter(song -> song.getTitle().toLowerCase().contains(lowerQuery)
                || song.getArtist().getName().toLowerCase().contains(lowerQuery)
                || (song.getAlbum() != null
                && song.getAlbum().getTitle().toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());
    }

    public List<Artist> searchArtists(String query) {
        String lowerQuery = query.toLowerCase();
        return artists.stream()
                .filter(artist -> artist.getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    public List<Album> searchAlbums(String query) {
        String lowerQuery = query.toLowerCase();
        return albums.stream()
                .filter(album -> album.getTitle().toLowerCase().contains(lowerQuery)
                || album.getArtist().getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    public List<Playlist> searchPlaylists(String query) {
        String lowerQuery = query.toLowerCase();
        return playlists.stream()
                .filter(playlist -> playlist.getName().toLowerCase().contains(lowerQuery)
                || playlist.getDescription().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    /*
     * Get songs by specific artist, album, or genre
     */
    public List<Song> getSongsByArtist(Artist artist) {
        return songs.stream()
                .filter(song -> song.getArtist().equals(artist))
                .collect(Collectors.toList());
    }

    public List<Song> getSongsByAlbum(Album album) {
        return songs.stream()
                .filter(song -> album.equals(song.getAlbum()))
                .collect(Collectors.toList());
    }

    public List<Song> getSongsByGenre(String genre) {
        return songs.stream()
                .filter(song -> genre.equalsIgnoreCase(song.getGenre()))
                .collect(Collectors.toList());
    }

    public List<Album> getAlbumsByArtist(Artist artist) {
        return albums.stream()
                .filter(album -> album.getArtist().equals(artist))
                .collect(Collectors.toList());
    }

    /*
     * Get aggregate statistics about the library (total songs, artists, albums, playlists, total duration)
     */
    public int getTotalSongCount() {
        return songs.size();
    }

    public int getTotalArtistCount() {
        return artists.size();
    }

    public int getTotalAlbumCount() {
        return albums.size();
    }

    public int getTotalPlaylistCount() {
        return playlists.size();
    }

    public int getTotalDurationSeconds() {
        return songs.stream()
                .mapToInt(Song::getDurationSeconds)
                .sum();
    }

    /*
     * Clear the entire library
     */
    public void clearLibrary() {
        songs.clear();
        artists.clear();
        albums.clear();
        playlists.clear();
    }

    /*
     * Get a list of all unique genres in the library
     */
    public List<String> getAllGenres() {
        return songs.stream()
                .map(Song::getGenre)
                .filter(genre -> !genre.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}