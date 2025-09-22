package com.spotify.clone.models;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MusicLibraryTest {

    private MusicLibrary lib;

    @BeforeEach
    public void setup() {
        lib = MusicLibrary.getInstance();
        lib.clearLibrary();
    }

    @Test
    public void addAndRemoveSong_updatesCountsAndCollections() {
        Artist artist = new Artist("Sample Artist");
        Song song = new Song("Sample", artist);
        song.setDurationSeconds(120);

        lib.addSong(song);
        assertEquals(1, lib.getTotalSongCount());
        assertEquals(1, lib.getTotalArtistCount());
        assertTrue(lib.getAllSongs().contains(song));

        // Remove and verify cleanup
        lib.removeSong(song);
        assertEquals(0, lib.getTotalSongCount());
        assertEquals(0, lib.getTotalArtistCount());
    }

    @Test
    public void searchSongs_findsByTitleAndArtist() {
        Artist artist = new Artist("SearchArtist");
        Song s1 = new Song("Hello World", artist);
        Song s2 = new Song("Other", new Artist("Another"));
        lib.addSong(s1);
        lib.addSong(s2);

        List<Song> res = lib.searchSongs("hello");
        assertEquals(1, res.size());
        assertEquals(s1, res.get(0));
    }

    @Test
    public void getTotalDurationSeconds_sumsDurations() {
        Artist a = new Artist("A");
        Song s1 = new Song("S1", a);
        s1.setDurationSeconds(30);
        Song s2 = new Song("S2", a);
        s2.setDurationSeconds(90);
        lib.addSong(s1);
        lib.addSong(s2);

        assertEquals(120, lib.getTotalDurationSeconds());
    }
}
