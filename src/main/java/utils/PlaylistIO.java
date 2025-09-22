package utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import models.Playlist;

/**
 * Simple JSON-backed persistence for playlists.
 */
public class PlaylistIO {

    private static final ObjectMapper MAPPER = createMapper();
    private static final String DEFAULT_DIR = System.getProperty("user.home") + File.separator + ".spotify-clone";
    private static final String PLAYLIST_FILE = DEFAULT_DIR + File.separator + "playlists.json";

    public static List<Playlist> loadPlaylists() throws IOException {
        Path dir = Path.of(DEFAULT_DIR);
        if (!Files.exists(dir)) {
            return new ArrayList<>();
        }
        File file = new File(PLAYLIST_FILE);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        return MAPPER.readValue(file, new TypeReference<List<Playlist>>() {
        });
    }

    public static void savePlaylists(List<Playlist> playlists) throws IOException {
        Path dir = Path.of(DEFAULT_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        File file = new File(PLAYLIST_FILE);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, playlists);
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
