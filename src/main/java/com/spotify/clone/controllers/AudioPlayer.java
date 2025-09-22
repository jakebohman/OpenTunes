package com.spotify.clone.controllers;

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

import com.spotify.clone.models.Song;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Handles audio playback functionality
 */
public class AudioPlayer {
    private Clip audioClip;
    private Song currentSong;
    private boolean isPlaying;
    private boolean isPaused;
    private long pausePosition;
    private float volume = 0.5f; // 0.0 to 1.0
    private List<AudioPlayerListener> listeners;
    private MediaPlayer mediaPlayerFallback;
    private boolean usingMediaFallback = false;
    
    public AudioPlayer() {
        this.listeners = new ArrayList<>();
        this.isPlaying = false;
        this.isPaused = false;
        this.pausePosition = 0;
    }
    
    /**
     * Interface for audio player event listeners
     */
    public interface AudioPlayerListener {
        void onSongChanged(Song song);
        void onPlayStateChanged(boolean isPlaying);
        void onPositionChanged(long position, long duration);
        void onVolumeChanged(float volume);
        void onError(String error);
        void onSongEnded();
    }
    
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
        try {
            stop();

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            audioClip = AudioSystem.getClip();
            audioClip.open(audioStream);

            usingMediaFallback = false;

            setVolumeInternal(volume);
            audioClip.start();

            currentSong = song;
            isPlaying = true;
            isPaused = false;
            pausePosition = 0;

            notifySongChanged(song);
            notifyPlayStateChanged(true);
            startPositionTracking();
            return true;

        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
            // Try JavaFX MediaPlayer fallback for broader format support (mp3, wav, m4a)
            try {
                stop();
                usingMediaFallback = true;
                Media media = new Media(audioFile.toURI().toString());
                mediaPlayerFallback = new MediaPlayer(media);
                mediaPlayerFallback.setVolume(volume);
                mediaPlayerFallback.setOnReady(() -> {
                    currentSong = song;
                    isPlaying = true;
                    isPaused = false;
                    pausePosition = 0;
                    notifySongChanged(song);
                    notifyPlayStateChanged(true);
                    // Start a small timer to update position via mediaPlayerFallback.getCurrentTime() if desired
                });
                mediaPlayerFallback.setOnEndOfMedia(() -> {
                    isPlaying = false;
                    isPaused = false;
                    notifyPlayStateChanged(false);
                    notifySongEnded();
                });
                mediaPlayerFallback.setOnError(() -> notifyError("MediaPlayer error: " + mediaPlayerFallback.getError().getMessage()));
                Platform.runLater(() -> mediaPlayerFallback.play());
                return true;
            } catch (Exception ex) {
                notifyError("Playback error: " + ex.getMessage());
                return false;
            }
        }
    }
    
    /**
     * Pause current playback
     */
    public void pause() {
        if (usingMediaFallback && mediaPlayerFallback != null && isPlaying && !isPaused) {
            mediaPlayerFallback.pause();
            // JavaFX MediaPlayer reports time in milliseconds
            pausePosition = (long) (mediaPlayerFallback.getCurrentTime().toMillis() * 1000);
            isPaused = true;
            isPlaying = false;
            notifyPlayStateChanged(false);
            return;
        }

        if (audioClip != null && isPlaying && !isPaused) {
            pausePosition = audioClip.getMicrosecondPosition();
            audioClip.stop();
            isPaused = true;
            isPlaying = false;
            notifyPlayStateChanged(false);
        }
    }
    
    /**
     * Resume paused playback
     */
    public void resume() {
        if (usingMediaFallback && mediaPlayerFallback != null && isPaused) {
            final long resumePosUs = pausePosition;
            Platform.runLater(() -> {
                mediaPlayerFallback.seek(javafx.util.Duration.millis(resumePosUs / 1000.0));
                mediaPlayerFallback.play();
                isPaused = false;
                isPlaying = true;
                notifyPlayStateChanged(true);
            });
            return;
        }

        if (audioClip != null && isPaused) {
            audioClip.setMicrosecondPosition(pausePosition);
            audioClip.start();
            isPaused = false;
            isPlaying = true;
            notifyPlayStateChanged(true);
            startPositionTracking();
        }
    }
    
    /**
     * Stop current playback
     */
    public void stop() {
        if (usingMediaFallback) {
            if (mediaPlayerFallback != null) {
                try {
                    Platform.runLater(() -> {
                        mediaPlayerFallback.stop();
                        mediaPlayerFallback.dispose();
                    });
                } catch (Exception ignored) {}
                mediaPlayerFallback = null;
            }
            usingMediaFallback = false;
        }

        if (audioClip != null) {
            audioClip.stop();
            audioClip.close();
            audioClip = null;
        }

        isPlaying = false;
        isPaused = false;
        pausePosition = 0;
        notifyPlayStateChanged(false);
    }
    
    /**
     * Seek to a specific position (in microseconds)
     */
    public void seek(long position) {
        if (usingMediaFallback && mediaPlayerFallback != null) {
            // position is in microseconds
            Platform.runLater(() -> mediaPlayerFallback.seek(javafx.util.Duration.millis(position / 1000.0)));
            pausePosition = position;
            return;
        }

        if (audioClip != null) {
            long duration = audioClip.getMicrosecondLength();
            if (position >= 0 && position <= duration) {
                audioClip.setMicrosecondPosition(position);
                pausePosition = position;
            }
        }
    }
    
    /**
     * Set volume (0.0 to 1.0)
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        setVolumeInternal(this.volume);
        notifyVolumeChanged(this.volume);
    }
    
    private void setVolumeInternal(float volume) {
        if (audioClip != null) {
            try {
                FloatControl volumeControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);
                // Convert 0.0-1.0 to decibel range
                float min = volumeControl.getMinimum();
                float max = volumeControl.getMaximum();
                float gain = min + (max - min) * volume;
                volumeControl.setValue(gain);
            } catch (IllegalArgumentException e) {
                // Volume control not supported
            }
        }
        if (usingMediaFallback && mediaPlayerFallback != null) {
            try {
                Platform.runLater(() -> mediaPlayerFallback.setVolume(volume));
            } catch (Exception ignored) {}
        }
    }
    
    private void startPositionTracking() {
        Thread positionThread = new Thread(() -> {
            while (isPlaying) {
                try {
                    if (usingMediaFallback && mediaPlayerFallback != null) {
                        javafx.util.Duration current = mediaPlayerFallback.getCurrentTime();
                        javafx.util.Duration total = mediaPlayerFallback.getTotalDuration();
                        long posUs = (long) (current.toMillis() * 1000);
                        long durUs = total == null || total.toMillis() == 0 ? 0 : (long) (total.toMillis() * 1000);
                        notifyPositionChanged(posUs, durUs);
                        if (total != null && current.greaterThanOrEqualTo(total.subtract(javafx.util.Duration.millis(100)))) {
                            isPlaying = false;
                            isPaused = false;
                            notifyPlayStateChanged(false);
                            notifySongEnded();
                            break;
                        }
                    } else if (audioClip != null && audioClip.isRunning()) {
                        long position = audioClip.getMicrosecondPosition();
                        long duration = audioClip.getMicrosecondLength();
                        notifyPositionChanged(position, duration);
                        if (position >= duration - 100000) {
                            isPlaying = false;
                            isPaused = false;
                            notifyPlayStateChanged(false);
                            notifySongEnded();
                            break;
                        }
                    }

                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception ignored) {}
            }
        });
        positionThread.setDaemon(true);
        positionThread.start();
    }
    
    // Getters
    public Song getCurrentSong() {
        return currentSong;
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public float getVolume() {
        return volume;
    }
    
    public long getCurrentPosition() {
        if (audioClip != null) {
            return isPaused ? pausePosition : audioClip.getMicrosecondPosition();
        }
        return 0;
    }
    
    public long getDuration() {
        if (audioClip != null) {
            return audioClip.getMicrosecondLength();
        }
        return 0;
    }
    
    // Notification methods
    private void notifySongChanged(Song song) {
        for (AudioPlayerListener listener : listeners) {
            listener.onSongChanged(song);
        }
    }
    
    private void notifyPlayStateChanged(boolean playing) {
        for (AudioPlayerListener listener : listeners) {
            listener.onPlayStateChanged(playing);
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
            } catch (Exception ignored) {
            }
        }
    }

    private void notifyError(String error) {
        for (AudioPlayerListener listener : listeners) {
            listener.onError(error);
        }
    }
}