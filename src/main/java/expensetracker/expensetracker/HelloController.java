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

        System.out.println("Attempting login with email: " + email + ", password: " + password);

        String query = "SELECT * FROM users WHERE email = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {

            System.out.println("Database connection established: " + (conn != null));

            try (java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, email);
                stmt.setString(2, password);

                System.out.println("PreparedStatement created, executing query...");

                java.sql.ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    int userId = rs.getInt("id");
                    System.out.println("Login successful! User ID: " + userId);
                    errorLabel.setText("Login successful!");

                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("dashboard.fxml"));
                        Parent root = loader.load();

                        DashboardController dashboardController = loader.getController();
                        dashboardController.setUserId(userId);
                        Stage stage = (Stage) emailField.getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.show();

                        System.out.println("Switched to Dashboard.");

                    } catch (IOException e) {
                        e.printStackTrace();
                        errorLabel.setText("Error loading dashboard file.");
                        System.out.println("Check if 'dashboard.fxml' is in the correct folder: " + getClass().getResource("dashboard.fxml"));
                    }

                } else {
                    System.out.println("No matching user found for email/password.");
                    errorLabel.setText("Invalid credentials!");
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Database error!");
        }
    }

    @FXML
    protected void onAddExpenseButtonClick() {
        System.out.println("Add Expense button clicked!"); // debug
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("add_expense.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) errorLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

            System.out.println("Switched to Add Expense screen.");
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Error loading Add Expense screen.");
        }
    }


}

