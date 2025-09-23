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
 * Simple JSON-backed persistence for playlists. Store playlists in a JSON file within the user's home directory.
 */
public class PlaylistIO {

    private static final ObjectMapper MAPPER = createMapper(); // Jackson ObjectMapper with JavaTimeModule registered
    // Persist playlists alongside the project file `music-library.json` in the working directory
    private static final String PLAYLIST_FILE = "playlists.json";

    /*
     * Load playlists from the JSON file, returning an empty list if the file doesn't exist or is unreadable
     */
    public static List<Playlist> loadPlaylists() throws IOException {
        File file = new File(PLAYLIST_FILE);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        return MAPPER.readValue(file, new TypeReference<List<Playlist>>() {
        });
    }

    /*
     * Save the given list of playlists to the JSON file, creating directories as needed
     */
    public static void savePlaylists(List<Playlist> playlists) throws IOException {
        File file = new File(PLAYLIST_FILE);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, playlists);
    }

    /*
     * Create and configure the Jackson ObjectMapper for JSON serialization/deserialization
     */
    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
