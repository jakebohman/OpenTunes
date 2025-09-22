package com.spotify.clone.views;

import com.spotify.clone.models.Playlist;
import javafx.scene.control.ListCell;

/**
 * Custom list cell for displaying playlists
 */
public class PlaylistListCell extends ListCell<Playlist> {
    
    @Override
    protected void updateItem(Playlist playlist, boolean empty) {
        super.updateItem(playlist, empty);
        
        if (empty || playlist == null) {
            setText(null);
            setGraphic(null);
        } else {
            String displayText = String.format("%s (%d songs, %s)", 
                playlist.getName(),
                playlist.getSongCount(),
                playlist.getFormattedDuration()
            );
            setText(displayText);
        }
    }
}