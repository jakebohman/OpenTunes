package com.spotify.clone.utils;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import com.spotify.clone.models.Album;
import com.spotify.clone.models.Artist;
import com.spotify.clone.models.MusicLibrary;
import com.spotify.clone.models.Song;

/**
 * Utility class for importing and managing audio files
 */
public class MusicImporter {
    
    private static final String[] SUPPORTED_FORMATS = {
        "mp3", "wav", "flac", "m4a", "ogg"
    };
    
    /**
     * Import a single audio file
     */
    public static Song importAudioFile(File file) {
        if (!isAudioFile(file)) {
            return null;
        }
        
        try {
            // Read audio file metadata
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            AudioHeader header = audioFile.getAudioHeader();
            
            // Extract metadata
            String title = getTagValue(tag, FieldKey.TITLE, file.getName());
            String artistName = getTagValue(tag, FieldKey.ARTIST, "Unknown Artist");
            String albumName = getTagValue(tag, FieldKey.ALBUM, "Unknown Album");
            String genre = getTagValue(tag, FieldKey.GENRE, "");
            String trackNumberStr = getTagValue(tag, FieldKey.TRACK, "0");
            
            int trackNumber = parseTrackNumber(trackNumberStr);
            int durationSeconds = header.getTrackLength();
            
            // Create or get artist
            MusicLibrary library = MusicLibrary.getInstance();
            Artist artist = findOrCreateArtist(artistName, library);
            
            // Create or get album
            Album album = findOrCreateAlbum(albumName, artist, library);
            
            // Create song
            Song song = new Song(title, artist, album, durationSeconds);
            song.setGenre(genre);
            song.setTrackNumber(trackNumber);
            song.setAudioFile(file);
            
            // Add to album
            album.addSong(song);
            
            // Add to library
            library.addSong(song);
            
            return song;
            
        } catch (Exception e) {
            // Attempt to create a basic fallback song; if that fails, bubble up as runtime exception
            try {
                Song fallback = createBasicSong(file);
                if (fallback != null) return fallback;
                throw new RuntimeException("Failed to import and fallback creation returned null for " + file.getName());
            } catch (Exception ex) {
                throw new RuntimeException("Error importing file " + file.getName() + ": " + e.getMessage(), ex);
            }
        }
    }
    
    /**
     * Import multiple audio files and return a detailed ImportResult.
     */
    public static ImportResult importAudioFiles(File[] files) {
        ImportResult result = new ImportResult();

        for (File file : files) {
            if (file.isDirectory()) {
                ImportResult sub = importFromDirectoryResult(file);
                sub.getSuccesses().forEach(result::addSuccess);
                sub.getFailures().forEach(f -> result.addFailure(f.getFile(), f.getReason()));
            } else {
                try {
                    Song song = importAudioFile(file);
                    if (song != null) result.addSuccess(song);
                } catch (Exception e) {
                    result.addFailure(file, e.getMessage());
                }
            }
        }

        return result;
    }

    /**
     * Backwards-compatible helper that returns only the successes as a list of songs.
     */
    public static List<Song> importAudioFilesList(File[] files) {
        return importAudioFiles(files).getSuccesses();
    }
    
    /**
     * Import all audio files from a directory and return a detailed ImportResult.
     */
    public static ImportResult importFromDirectoryResult(File directory) {
        ImportResult result = new ImportResult();

        if (!directory.isDirectory()) {
            return result;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    ImportResult sub = importFromDirectoryResult(file);
                    sub.getSuccesses().forEach(result::addSuccess);
                    sub.getFailures().forEach(f -> result.addFailure(f.getFile(), f.getReason()));
                } else if (isAudioFile(file)) {
                    try {
                        Song song = importAudioFile(file);
                        if (song != null) result.addSuccess(song);
                    } catch (Exception e) {
                        result.addFailure(file, e.getMessage());
                    }
                }
            }
        }

        return result;
    }

    /**
     * Backwards-compatible helper that returns only the successes as a list.
     */
    public static List<Song> importFromDirectoryList(File directory) {
        return importFromDirectoryResult(directory).getSuccesses();
    }
    
    /**
     * Check if file is a supported audio format
     */
    public static boolean isAudioFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        for (String format : SUPPORTED_FORMATS) {
            if (fileName.endsWith("." + format)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get duration of audio file using Java Sound API (fallback)
     */
    private static int getAudioDuration(File file) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = audioInputStream.getFormat();
            long frames = audioInputStream.getFrameLength();
            double durationInSeconds = (frames + 0.0) / format.getFrameRate();
            return (int) durationInSeconds;
        } catch (Exception e) {
            return 0; // Unknown duration
        }
    }
    
    /**
     * Create a basic song when metadata extraction fails
     */
    private static Song createBasicSong(File file) {
        try {
            String fileName = file.getName();
            String title = fileName.substring(0, fileName.lastIndexOf('.'));
            
            MusicLibrary library = MusicLibrary.getInstance();
            Artist artist = findOrCreateArtist("Unknown Artist", library);
            Album album = findOrCreateAlbum("Unknown Album", artist, library);
            
            int duration = getAudioDuration(file);
            
            Song song = new Song(title, artist, album, duration);
            song.setAudioFile(file);
            
            album.addSong(song);
            library.addSong(song);
            
            return song;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create basic song for " + file.getName(), e);
        }
    }
    
    /**
     * Find existing artist or create new one
     */
    private static Artist findOrCreateArtist(String artistName, MusicLibrary library) {
        for (Artist existingArtist : library.getAllArtists()) {
            if (existingArtist.getName().equals(artistName)) {
                return existingArtist;
            }
        }
        
        Artist newArtist = new Artist(artistName);
        library.addArtist(newArtist);
        return newArtist;
    }
    
    /**
     * Find existing album or create new one
     */
    private static Album findOrCreateAlbum(String albumName, Artist artist, MusicLibrary library) {
        for (Album existingAlbum : library.getAllAlbums()) {
            if (existingAlbum.getTitle().equals(albumName) && 
                existingAlbum.getArtist().equals(artist)) {
                return existingAlbum;
            }
        }
        
        Album newAlbum = new Album(albumName, artist, LocalDate.now(), "");
        library.addAlbum(newAlbum);
        artist.addAlbum(newAlbum);
        return newAlbum;
    }
    
    /**
     * Get tag value with fallback
     */
    private static String getTagValue(Tag tag, FieldKey key, String fallback) {
        if (tag == null) {
            return fallback;
        }
        
        try {
            String value = tag.getFirst(key);
            return (value == null || value.trim().isEmpty()) ? fallback : value.trim();
        } catch (Exception e) {
            return fallback;
        }
    }
    
    /**
     * Parse track number from string
     */
    private static int parseTrackNumber(String trackStr) {
        if (trackStr == null || trackStr.trim().isEmpty()) {
            return 0;
        }
        
        try {
            // Handle formats like "01/12" or "1 of 12"
            String[] parts = trackStr.split("[/\\s]+");
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Get list of supported file extensions
     */
    public static String[] getSupportedFormats() {
        return SUPPORTED_FORMATS.clone();
    }
    
    /**
     * Get file filter description for file chooser
     */
    public static String getFileFilterDescription() {
        StringBuilder sb = new StringBuilder("Audio Files (");
        for (int i = 0; i < SUPPORTED_FORMATS.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("*.").append(SUPPORTED_FORMATS[i]);
        }
        sb.append(")");
        return sb.toString();
    }
}