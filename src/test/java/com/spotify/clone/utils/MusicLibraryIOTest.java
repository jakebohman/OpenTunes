package com.spotify.clone.utils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.spotify.clone.models.MusicLibrary;
import com.spotify.clone.models.Song;

public class MusicLibraryIOTest {

    @Test
    public void testSaveAndLoadLibrary() throws Exception {
        MusicLibrary lib = MusicLibrary.getInstance();
        // Use a clean slate
        lib.clearLibrary();

        Song s = new Song("Test Song", null);
        lib.addSong(s);

        File tmp = File.createTempFile("musiclib", ".json");
        tmp.deleteOnExit();

        MusicLibraryIO.saveLibrary(lib, tmp);

        // Clear and reload
        lib.clearLibrary();
        assertEquals(0, lib.getTotalSongCount());

        MusicLibraryIO.loadLibrary(lib, tmp);
        assertEquals(1, lib.getTotalSongCount());
    }
}
