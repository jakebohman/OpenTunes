package controllers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import models.Song;

/**
 * Handles audio playback functionality, including play, pause, resume, stop, seek, and volume control.
 * Supports both Java Sound API (Clip) and JavaFX MediaPlayer for broader format compatibility.
 */
public class AudioPlayer {

    public enum PlaybackState { STOPPED, PLAYING, PAUSED, BUFFERING }
    private volatile PlaybackState playbackState = PlaybackState.STOPPED; // current playback state
    private Song currentSong; // currently playing song
    private long pausePosition; // position in microseconds where playback was last paused
    private float volume = 0.5f; // 0.0 to 1.0

    private List<AudioPlayerListener> listeners; // registered event listeners for playback events
    private Clip audioClip; // Java Sound Clip for low-latency playback
    private MediaPlayer mediaPlayerFallback; // JavaFX MediaPlayer for broader format support (used if Clip fails)
    private boolean usingMediaFallback = false; // whether currently using MediaPlayer fallback

    /*
     * Constructor
     */
    public AudioPlayer() {
        this.listeners = new ArrayList<>();
        this.playbackState = PlaybackState.STOPPED;
        this.pausePosition = 0;
    }

    /**
     * Interface for audio player event listeners
     */
    public interface AudioPlayerListener {
        void onSongChanged(Song song);
        void onPlayStateChanged(PlaybackState state);
        void onPositionChanged(long position, long duration);
        void onVolumeChanged(float volume);
        void onError(String error);
        void onSongEnded();
    }

    /*
     * Add or remove listeners for audio player events
     */
    public void addListener(AudioPlayerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(AudioPlayerListener listener) {
        listeners.remove(listener);
    }

    /**
     * Load and play a song
     */
    public boolean playSong(Song song) {
        // Validate song and filepath
        if (song == null || song.getFilePath() == null) {
            notifyError("Invalid song or file path");
            return false;
        }

        File audioFile = new File(song.getFilePath());
        if (!audioFile.exists()) {
            notifyError("Audio file not found: " + song.getFilePath());
            return false;
        }

        // Try Java Sound Clip first (fast, low-latency). If it fails for format reasons, try JavaFX MediaPlayer.
        stop();
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            audioClip = AudioSystem.getClip();
            audioClip.open(audioStream);

            usingMediaFallback = false;
            setVolumeInternal(volume);
            audioClip.start();

            markAsPlaying(song);
            return true;
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
            // Fallback to JavaFX MediaPlayer for broader format support
            try {
                usingMediaFallback = true;
                Media media = new Media(audioFile.toURI().toString());
                MediaPlayer mp = new MediaPlayer(media);
                mediaPlayerFallback = mp;
                mp.setVolume(volume);
                mp.setOnReady(() -> {
                    markAsPlaying(song);
                });
                mp.setOnEndOfMedia(() -> {
                    playbackState = PlaybackState.STOPPED;
                    notifyPlayStateChanged(playbackState);
                    notifySongEnded();
                });
                mp.setOnError(() -> notifyError("MediaPlayer error: " + mp.getError().getMessage()));
                Platform.runLater(mp::play);
                return true;
            } catch (Exception ex) {
                // If neither Clip nor MediaPlayer works, report error
                notifyError("Playback error: " + ex.getMessage());
                return false;
            }
        }
    }

    /**
     * Common actions to perform when playback for a song has started.
     */
    private void markAsPlaying(Song song) {
        currentSong = song;
        playbackState = PlaybackState.PLAYING;
        pausePosition = 0;
        notifySongChanged(song);
        notifyPlayStateChanged(playbackState);
        startPositionTracking();
    }

    /**
     * Pause current playback
     */
    public void pause() {
        // If using Clip
        if (audioClip != null && playbackState == PlaybackState.PLAYING) {
            pausePosition = audioClip.getMicrosecondPosition();
            audioClip.stop();
            playbackState = PlaybackState.PAUSED;
            notifyPlayStateChanged(playbackState);
        }
        
        // If using MediaPlayer fallback
        else if (usingMediaFallback && mediaPlayerFallback != null && playbackState == PlaybackState.PLAYING) {
            mediaPlayerFallback.pause();
            pausePosition = (long) (mediaPlayerFallback.getCurrentTime().toMillis() * 1000);
            playbackState = PlaybackState.PAUSED;
            notifyPlayStateChanged(playbackState);
        }
    }

    /**
     * Resume paused playback
     */
    public void resume() {
        // If using Clip
        if (audioClip != null && playbackState == PlaybackState.PAUSED) {
            audioClip.setMicrosecondPosition(pausePosition);
            audioClip.start();
            playbackState = PlaybackState.PLAYING;
            notifyPlayStateChanged(playbackState);
            startPositionTracking();
        }        
        
        // If using MediaPlayer fallback
        else if (usingMediaFallback && mediaPlayerFallback != null && playbackState == PlaybackState.PAUSED) {
            final long resumePosUs = pausePosition;
            Platform.runLater(() -> {
                mediaPlayerFallback.seek(javafx.util.Duration.millis(resumePosUs / 1000.0));
                mediaPlayerFallback.play();
                playbackState = PlaybackState.PLAYING;
                notifyPlayStateChanged(playbackState);
                startPositionTracking();
            });
        }
    }

    /**
     * Stop current playback
     */
    public void stop() {
        // If using Clip
        if (audioClip != null) {
            audioClip.stop();
            audioClip.close();
            audioClip = null;
        }
        
        // If using MediaPlayer fallback
        else if (usingMediaFallback) {
            // capture the current media player so we don't reference the field from the UI thread after nulling it
            final MediaPlayer mp = mediaPlayerFallback;
            mediaPlayerFallback = null;
            usingMediaFallback = false;
            if (mp != null) {
                try {
                    Platform.runLater(() -> {
                        try {
                            mp.stop();
                        } catch (Exception ignored) {}
                        try {
                            mp.dispose();
                        } catch (Exception ignored) {}
                    });
                } catch (Exception ignored) {}
            }
        }

        playbackState = PlaybackState.STOPPED;
        pausePosition = 0;
        notifyPlayStateChanged(playbackState);
    }

    /**
     * Seek to a specific position (in microseconds)
     */
    public void seek(long position) {
        // If using Clip
        if (audioClip != null) {
            long duration = audioClip.getMicrosecondLength();
            if (position >= 0 && position <= duration) {
                audioClip.setMicrosecondPosition(position);
                pausePosition = position;
            }
        }        
        
        // If using MediaPlayer fallback
        else if (usingMediaFallback && mediaPlayerFallback != null) {
            Platform.runLater(() -> mediaPlayerFallback.seek(javafx.util.Duration.millis(position / 1000.0)));
            pausePosition = position;
        }
    }

    /**
     * Public method to set volume (0.0 to 1.0)
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        setVolumeInternal(this.volume);
        notifyVolumeChanged(this.volume);
    }

    /**
     * Internal method to apply volume to the current playback mechanism
     */
    private void setVolumeInternal(float volume) {
        // If using Clip
        if (audioClip != null) {
            try {
                FloatControl volumeControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);
                // Convert 0.0-1.0 to decibel range
                float min = volumeControl.getMinimum();
                float max = volumeControl.getMaximum();
                float gain = min + (max - min) * volume;
                volumeControl.setValue(gain);
            } catch (IllegalArgumentException e) {
                notifyError("Volume control not supported");
            }
        }

        // If using MediaPlayer fallback
        else if (usingMediaFallback && mediaPlayerFallback != null) {
            try {
                Platform.runLater(() -> mediaPlayerFallback.setVolume(volume));
            } catch (Exception ignored) {}
        }
    }

    /*
     * Spawn thread to track playback position and notify listeners
     */
    private void startPositionTracking() {
        Thread positionThread = new Thread(() -> {
            while (playbackState == PlaybackState.PLAYING) {
                try {
                    // If using Clip
                    if (audioClip != null && audioClip.isRunning()) {
                        long position = audioClip.getMicrosecondPosition();
                        long duration = audioClip.getMicrosecondLength();
                        notifyPositionChanged(position, duration);
                        // Check if playback has reached near the end
                        if (position >= duration - 100000) {
                            playbackState = PlaybackState.STOPPED;
                            notifyPlayStateChanged(playbackState);
                            notifySongEnded();
                            break;
                        }
                    }
                    
                    // If using MediaPlayer fallback
                    else if (usingMediaFallback && mediaPlayerFallback != null) {
                        javafx.util.Duration current = mediaPlayerFallback.getCurrentTime();
                        javafx.util.Duration total = mediaPlayerFallback.getTotalDuration();
                        long posUs = (long) (current.toMillis() * 1000);
                        long durUs = total == null || total.toMillis() == 0 ? 0 : (long) (total.toMillis() * 1000);
                        notifyPositionChanged(posUs, durUs);
                        // Check if playback has reached near the end
                        if (total != null && current.greaterThanOrEqualTo(total.subtract(javafx.util.Duration.millis(100)))) {
                            playbackState = PlaybackState.STOPPED;
                            notifyPlayStateChanged(playbackState);
                            notifySongEnded();
                            break;
                        }
                    }

                    // Check again every 100ms until playback stops
                    Thread.sleep(100);
                } catch (InterruptedException e) { break; } catch (Exception ignored) {}
            }
        });
        positionThread.setDaemon(true);
        positionThread.start();
    }

    /*
     * Getters
     */
    public Song getCurrentSong() {
        return currentSong;
    }

    public boolean isPlaying() {
        return playbackState == PlaybackState.PLAYING;
    }

    public boolean isPaused() {
        return playbackState == PlaybackState.PAUSED;
    }

    public PlaybackState getPlaybackState() {
        return playbackState;
    }

    public float getVolume() {
        return volume;
    }

    /*
     * Returns current playback position in microseconds
     */
    public long getCurrentPosition() {
        // If using Clip
        if (audioClip != null) {
            return playbackState == PlaybackState.PAUSED ? pausePosition : audioClip.getMicrosecondPosition();
        }

        // If using MediaPlayer fallback
        else if (usingMediaFallback && mediaPlayerFallback != null) {
            try {
                javafx.util.Duration cur = mediaPlayerFallback.getCurrentTime();
                return (long) (cur.toMillis() * 1000);
            } catch (Exception ignored) {}
        }
        
        return 0;
    }

    /*
     * Returns total duration of the current song in microseconds
     */
    public long getDuration() {
        // If using Clip
        if (audioClip != null) {
            return audioClip.getMicrosecondLength();
        }

        // If using MediaPlayer fallback
        else if (usingMediaFallback && mediaPlayerFallback != null) {
            try {
                javafx.util.Duration total = mediaPlayerFallback.getTotalDuration();
                if (total == null || total.toMillis() <= 0) {
                    return 0;
                }
                return (long) (total.toMillis() * 1000);
            } catch (Exception ignored) {}
        }

        return 0;
    }

    /*
     * Notification methods, used to inform listeners of playback events
     */
    private void notifySongChanged(Song song) {
        for (AudioPlayerListener listener : listeners) {
            listener.onSongChanged(song);
        }
    }

    private void notifyPlayStateChanged(PlaybackState state) {
        for (AudioPlayerListener listener : listeners) {
            try {
                listener.onPlayStateChanged(state);
            } catch (Exception ignored) {}
        }
    }

    private void notifyPositionChanged(long position, long duration) {
        for (AudioPlayerListener listener : listeners) {
            listener.onPositionChanged(position, duration);
        }
    }

    private void notifyVolumeChanged(float volume) {
        for (AudioPlayerListener listener : listeners) {
            listener.onVolumeChanged(volume);
        }
    }

    private void notifySongEnded() {
        for (AudioPlayerListener listener : listeners) {
            try {
                listener.onSongEnded();
            } catch (Exception ignored) {}
        }
    }

    private void notifyError(String error) {
        for (AudioPlayerListener listener : listeners) {
            listener.onError(error);
        }
    }
}