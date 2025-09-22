package utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import models.Artist;
import models.Playlist;
import models.Song;

public class PlaylistIOTest {

    private static final Path TEST_DIR = Path.of(System.getProperty("java.io.tmpdir"), "spotify-clone-test");
    private static final Path PLAYLIST_FILE = TEST_DIR.resolve("playlists.json");

    @AfterEach
    public void cleanup() throws Exception {
        // Attempt to remove the playlists.json from the user's home .spotify-clone
        Path userDir = Path.of(System.getProperty("user.home"), ".spotify-clone", "playlists.json");
        if (Files.exists(userDir)) {
            Files.deleteIfExists(userDir);
        }
    }

    @Test
    public void saveAndLoadPlaylists_roundTrip() throws Exception {
        // create a playlist
        Artist a = new Artist("Test Artist");
        Song s = new Song("Test Song", a);
        Playlist p = new Playlist("UnitTest Playlist");
        p.addSong(s);

        // Save playlists (will throw on failure)
        PlaylistIO.savePlaylists(List.of(p));

        List<Playlist> loaded = PlaylistIO.loadPlaylists();
        assertNotNull(loaded);
        assertTrue(loaded.stream().anyMatch(pl -> "UnitTest Playlist".equals(pl.getName())), "Loaded playlists should contain the saved playlist");
    }
}
