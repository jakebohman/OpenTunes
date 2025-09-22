package controllers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import models.Song;

public class AudioPlayerTest {

    @Test
    public void initialState() {
        AudioPlayer p = new AudioPlayer();
        assertFalse(p.isPlaying(), "Should not be playing initially");
        assertFalse(p.isPaused(), "Should not be paused initially");
        assertEquals(0.5f, p.getVolume(), 0.0001f, "Default volume should be 0.5");
    }

    @Test
    public void volumeClamping() {
        AudioPlayer p = new AudioPlayer();
        p.setVolume(2.0f);
        assertTrue(p.getVolume() <= 1.0f && p.getVolume() >= 0.0f, "Volume should be clamped to [0,1]");
        p.setVolume(-1.0f);
        assertTrue(p.getVolume() <= 1.0f && p.getVolume() >= 0.0f, "Volume should be clamped to [0,1]");
    }

    @Test
    public void stopPauseResumeNoMedia() {
        AudioPlayer p = new AudioPlayer();
        // calling stop/pause/resume with no media should not throw
        assertDoesNotThrow(() -> p.stop());
        assertDoesNotThrow(() -> p.pause());
        assertDoesNotThrow(() -> p.resume());
    }

    @Test
    public void playSongInvalidPath() {
        AudioPlayer p = new AudioPlayer();
        Song s = new Song();
        s.setFilePath("/non/existent/file.mp3");
        boolean ok = p.playSong(s);
        assertFalse(ok, "Playing a missing file should return false");
    }
}
