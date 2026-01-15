package expensetracker.expensetracker;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;

public class HelloController {

    @FXML
    private TextField emailField;

    @FXML
    private TextField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    protected void onLoginButtonClick() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        String query = "SELECT * FROM users WHERE email = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            stmt.setString(2, password);

            java.sql.ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String userName = rs.getString("username");

                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("dashboard.fxml"));
                    Parent root = loader.load();

                    // 1. Pass data to the controller
                    DashboardController dashboardController = loader.getController();
                    dashboardController.setUserId(userId);
                    dashboardController.setDisplayName(userName);

                    // 2. Get the current Stage (Window)
                    Stage stage = (Stage) emailField.getScene().getWindow();

                    // 3. Create the scene with preferred dimensions (acts as a minimum)
                    Scene scene = new Scene(root, 1200, 800);

                    // 4. Update Stage properties
                    stage.setTitle("Expense Tracker - " + userName);
                    stage.setScene(scene);

                    // Ensure the window is allowed to grow
                    stage.setResizable(true);

                    // Force maximize to fill the screen
                    stage.setMaximized(true);

                    // Final reveal
                    stage.show();

                } catch (IOException e) {
                    e.printStackTrace();
                    errorLabel.setText("Error loading dashboard file.");
                }

            } else {
                errorLabel.setText("Invalid credentials!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Database error!");
        }
    }

    @FXML
    protected void onAddExpenseButtonClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("add_expense.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) errorLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Error loading Add Expense screen.");
        }
    }


    @FXML
    protected void onRegisterClick() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("register.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Register - Expense Tracker");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}