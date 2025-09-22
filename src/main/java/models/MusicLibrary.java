package models;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Central management class for the music library
 */
public class MusicLibrary {

    private List<Song> songs;
    private List<Artist> artists;
    private List<Album> albums;
    private List<Playlist> playlists;
    private static MusicLibrary instance;

    private MusicLibrary() {
        this.songs = new ArrayList<>();
        this.artists = new ArrayList<>();
        this.albums = new ArrayList<>();
        this.playlists = new ArrayList<>();
    }

    public static synchronized MusicLibrary getInstance() {
        if (instance == null) {
            instance = new MusicLibrary();
        }
        return instance;
    }

    // Song Management
    public void addSong(Song song) {
        if (song == null) {
            return;
        }

        // If a song with the same file path already exists, treat as duplicate
        String path = song.getFilePath();
        if (path != null && !path.isEmpty()) {
            for (Song s : songs) {
                if (path.equals(s.getFilePath())) {
                    return; // duplicate by filepath
                }
            }
        } else {
            // fallback to equality check if no file path
            if (songs.contains(song)) {
                return;
            }
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

    public List<Song> getAllSongs() {
        return new ArrayList<>(songs);
    }

    // Artist Management
    public void addArtist(Artist artist) {
        if (artist != null && !artists.contains(artist)) {
            artists.add(artist);
        }
    }

    public List<Artist> getAllArtists() {
        return new ArrayList<>(artists);
    }

    // Album Management
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

    public List<Album> getAllAlbums() {
        return new ArrayList<>(albums);
    }

    // Playlist Management
    public void addPlaylist(Playlist playlist) {
        if (playlist != null && !playlists.contains(playlist)) {
            playlists.add(playlist);
        }
    }

    public void removePlaylist(Playlist playlist) {
        playlists.remove(playlist);
    }

    public List<Playlist> getAllPlaylists() {
        return new ArrayList<>(playlists);
    }

    // Search Functions
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

    // Filter Functions
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

    // Statistics
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

    // Utility methods
    public void clearLibrary() {
        songs.clear();
        artists.clear();
        albums.clear();
        playlists.clear();
    }

    public List<String> getAllGenres() {
        return songs.stream()
                .map(Song::getGenre)
                .filter(genre -> !genre.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
