package views;

import java.io.IOException;
import java.util.List;

import controllers.AudioPlayer;
import models.MusicLibrary;
import models.Playlist;
import models.Song;
import utils.PlaylistIO;

/**
 * Controller that manages playback state and coordinates with the AudioPlayer.
 * This keeps playback logic separate from the JavaFX UI in MainWindow.
 */
public class MusicPlayerController {

    private final MusicLibrary musicLibrary;
    private final AudioPlayer audioPlayer;

    // Playback state
    private Playlist currentPlaylist; // null = library / arbitrary list mode
    private List<Song> currentList; // reference to the current list shown in the UI
    private int currentSongIndex = -1;

    public MusicPlayerController(MusicLibrary musicLibrary) {
        this.musicLibrary = musicLibrary;
        this.audioPlayer = new AudioPlayer();
    }

    // Listener plumbing (UI will register a listener to receive events)
    public void addListener(AudioPlayer.AudioPlayerListener listener) {
        audioPlayer.addListener(listener);
    }

    // Playback controls (delegates to AudioPlayer and maintains index state)
    public void playSong(Song song) {
        if (song == null) return;
        audioPlayer.playSong(song);

        if (currentList != null) {
            currentSongIndex = currentList.indexOf(song);
        } else {
            currentSongIndex = -1;
        }
    }

    public void togglePlayPause() {
        if (audioPlayer.isPlaying()) {
            audioPlayer.pause();
        } else if (audioPlayer.isPaused()) {
            audioPlayer.resume();
        }
    }

    public void stop() {
        audioPlayer.stop();
        currentSongIndex = -1;
    }

    public void playNext() {
        List<Song> list = currentList;
        if ((currentPlaylist != null && currentPlaylist.getSongCount() > 0)) {
            list = currentPlaylist.getSongs();
        }

        if (list == null || list.isEmpty()) return;

        int nextIndex = currentSongIndex >= 0 ? currentSongIndex + 1 : 0;
        if (nextIndex < 0) nextIndex = 0;
        if (nextIndex < 0 || nextIndex >= list.size()) {
            audioPlayer.stop();
            return;
        }

        Song next = list.get(nextIndex);
        if (next != null) {
            currentSongIndex = nextIndex;
            playSong(next);
        }
    }

    public void playPrevious() {
        List<Song> list = currentList;
        if ((currentPlaylist != null && currentPlaylist.getSongCount() > 0)) {
            list = currentPlaylist.getSongs();
        }

        if (list == null || list.isEmpty()) return;

        int prevIndex = currentSongIndex >= 0 ? currentSongIndex - 1 : 0;
        if (prevIndex < 0) {
            // restart current track
            if (currentSongIndex >= 0 && currentSongIndex < list.size()) {
                Song current = list.get(currentSongIndex);
                audioPlayer.seek(0);
                audioPlayer.playSong(current);
            }
            return;
        }

        Song prev = list.get(prevIndex);
        if (prev != null) {
            currentSongIndex = prevIndex;
            playSong(prev);
        }
    }

    public void seek(long targetUs) {
        try {
            audioPlayer.seek(targetUs);
        } catch (Exception ignored) {
        }
    }

    public long getDuration() {
        return audioPlayer.getDuration();
    }

    public void setVolume(float v) {
        audioPlayer.setVolume(v);
    }

    public boolean isPlaying() {
        return audioPlayer.isPlaying();
    }

    public boolean isPaused() {
        return audioPlayer.isPaused();
    }

    public Song getCurrentSong() {
        if (currentList != null && currentSongIndex >= 0 && currentSongIndex < currentList.size()) {
            return currentList.get(currentSongIndex);
        }
        if (currentPlaylist != null && currentPlaylist.getSongCount() > 0 && currentSongIndex >= 0 && currentSongIndex < currentPlaylist.getSongCount()) {
            return currentPlaylist.getSongs().get(currentSongIndex);
        }
        return null;
    }

    public int getCurrentSongIndex() {
        return currentSongIndex;
    }

    public Playlist getCurrentPlaylist() {
        return currentPlaylist;
    }

    public void setCurrentPlaylist(Playlist p) {
        this.currentPlaylist = p;
        if (p != null) {
            this.currentList = p.getSongs();
        }
        this.currentSongIndex = -1;
    }

    public void setCurrentList(List<Song> list) {
        this.currentList = list;
        this.currentPlaylist = null;
        this.currentSongIndex = -1;
    }

    /**
     * Move a song within the currently-set playlist (returns true on success).
     */
    public boolean moveSongInCurrentPlaylist(int from, int to) {
        if (currentPlaylist == null) return false;
        currentPlaylist.moveSong(from, to);
        try {
            PlaylistIO.savePlaylists(musicLibrary.getAllPlaylists());
        } catch (IOException ex) {
            // caller/UI can surface errors if desired
        }
        // After moving, refresh indices
        this.currentSongIndex = -1;
        return true;
    }

    /**
     * Shutdown playback and release resources. Call this when the application is exiting
     * to ensure any background playback threads are stopped.
     */
    public void shutdown() {
        try {
            audioPlayer.stop();
        } catch (Exception ignored) {
        }
    }
}
