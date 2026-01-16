package expensetracker.expensetracker;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.util.Duration;

public class NavigationService {
    private void applyStyle(Scene scene) {
        String css = getClass().getResource("style.css").toExternalForm();
        scene.getStylesheets().add(css);
    }
    private void playPopupAnimation(Parent root) {
        FadeTransition fade = new FadeTransition(Duration.millis(400), root);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(400), root);
        scale.setFromX(0.8);
        scale.setFromY(0.8);
        scale.setToX(1.0);
        scale.setToY(1.0);

        new ParallelTransition(fade, scale).play();
    }

    public void openExpenseWindow(int userId, Expense expense, Runnable onClose) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("add_expense.fxml"));
            Parent root = loader.load();
            playPopupAnimation(root);

            AddExpenseController controller = loader.getController();
            controller.setUserId(userId);
            if (expense != null) controller.setExpenseData(expense);

            Stage stage = new Stage();
            Scene scene = new Scene(root);
            applyStyle(scene);

            stage.setScene(scene);
            stage.setOnHiding(event -> onClose.run());
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void openAddRecurringWindow(int userId, Runnable onClose) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("add_recurring.fxml"));
            Parent root = loader.load();
            playPopupAnimation(root);

            AddRecurringController controller = loader.getController();
            controller.setUserId(userId);

            Stage stage = new Stage();
            Scene scene = new Scene(root);
            applyStyle(scene);

            stage.setTitle("Add Recurring Expense");
            stage.setScene(scene);
            stage.setOnHiding(event -> onClose.run());
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void switchScene(Stage stage, String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            applyStyle(scene);

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}