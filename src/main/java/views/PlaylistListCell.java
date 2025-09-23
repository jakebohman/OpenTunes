package views;

import javafx.scene.control.ListCell;
import models.Playlist;

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
            // Special-case the reserved placeholder ID used by the UI to show
            // the full library as the first list item.
            if ("__VIEW_LIBRARY__".equals(playlist.getName())) {
                setText("Library");
                // Make the library placeholder visually distinct (bold)
                setStyle("-fx-font-weight: bold;");
            } else {
                setStyle(null);
                String displayText = String.format("%s (%d songs, %s)", 
                    playlist.getName(),
                    playlist.getSongCount(),
                    playlist.getFormattedDuration()
                );
                setText(displayText);
            }
        }
    }
}