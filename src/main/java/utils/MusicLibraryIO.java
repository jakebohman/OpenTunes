package utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import models.Album;
import models.Artist;
import models.MusicLibrary;
import models.Playlist;
import models.Song;

/**
 * Persistence helper for the MusicLibrary. Saves/loads a simple snapshot object
 * to JSON.
 */
public class MusicLibraryIO {

    private static final String LIBRARY_FILE = "music-library.json";
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Snapshot DTO used for serialization
    public static class Snapshot {

        private List<Song> songs;
        private List<Artist> artists;
        private List<Album> albums;
        private List<Playlist> playlists;

        public Snapshot() {
        }

        public Snapshot(List<Song> songs, List<Artist> artists, List<Album> albums, List<Playlist> playlists) {
            this.songs = songs;
            this.artists = artists;
            this.albums = albums;
            this.playlists = playlists;
        }

        public List<Song> getSongs() {
            return songs;
        }

        public void setSongs(List<Song> songs) {
            this.songs = songs;
        }

        public List<Artist> getArtists() {
            return artists;
        }

        public void setArtists(List<Artist> artists) {
            this.artists = artists;
        }

        public List<Album> getAlbums() {
            return albums;
        }

        public void setAlbums(List<Album> albums) {
            this.albums = albums;
        }

        public List<Playlist> getPlaylists() {
            return playlists;
        }

        public void setPlaylists(List<Playlist> playlists) {
            this.playlists = playlists;
        }
    }

    public static void saveLibrary(MusicLibrary lib) throws IOException {
        saveLibrary(lib, new File(LIBRARY_FILE));
    }

    public static void saveLibrary(MusicLibrary lib, File outFile) throws IOException {
        Snapshot snap = new Snapshot(lib.getAllSongs(), lib.getAllArtists(), lib.getAllAlbums(), lib.getAllPlaylists());
        mapper.writerWithDefaultPrettyPrinter().writeValue(outFile, snap);
    }

    public static void loadLibrary(MusicLibrary lib) throws IOException {
        loadLibrary(lib, new File(LIBRARY_FILE));
    }

    public static void loadLibrary(MusicLibrary lib, File inFile) throws IOException {
        if (!inFile.exists()) {
            return;
        }
        Snapshot snap = mapper.readValue(inFile, Snapshot.class);
        // Replace current library content with the snapshot
        lib.clearLibrary();
        if (snap.getSongs() != null) {
            for (Song s : snap.getSongs()) {
                lib.addSong(s);
            }
        }
        if (snap.getPlaylists() != null) {
            for (Playlist p : snap.getPlaylists()) {
                lib.addPlaylist(p);
            }
        }
    }
}
