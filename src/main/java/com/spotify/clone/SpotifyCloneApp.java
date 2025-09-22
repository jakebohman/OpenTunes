package com.spotify.clone;

import com.spotify.clone.views.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main application class for the Spotify Clone
 */
public class SpotifyCloneApp extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            MainWindow mainWindow = new MainWindow(primaryStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}