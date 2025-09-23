import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import views.MainWindow;

/**
 * Main application class for the Spotify Clone
 */
public class OpenTunesApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            @SuppressWarnings("unused")
            MainWindow mainWindow = new MainWindow(primaryStage);
        } catch (Exception e) {
            // Show a user-friendly popup with an error message
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Startup Error");
            alert.setHeaderText("Application failed to start ):");
            alert.setContentText(e.getMessage() != null ? e.getMessage() : "An unexpected error occurred during startup.");
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
