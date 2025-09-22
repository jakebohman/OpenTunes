package views;

import models.Song;

import javafx.scene.control.ListCell;

/**
 * Custom list cell for displaying songs
 */
public class SongListCell extends ListCell<Song> {
    
    @Override
    protected void updateItem(Song song, boolean empty) {
        super.updateItem(song, empty);
        
        if (empty || song == null) {
            setText(null);
            setGraphic(null);
        } else {
            String displayText = String.format("%s - %s (%s)", 
                song.getTitle(),
                song.getArtist().getName(),
                song.getFormattedDuration()
            );
            setText(displayText);
        }
    }
}