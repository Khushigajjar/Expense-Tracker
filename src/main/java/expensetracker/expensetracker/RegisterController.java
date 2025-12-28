package expensetracker.expensetracker;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class RegisterController {
    @FXML private TextField usernameField, emailField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private Label statusLabel;

    @FXML
    protected void onRegisterClick() {
        String user = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String pass = passwordField.getText().trim();

        if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Please fill in all fields.");
            return;
        }

        if (!pass.equals(confirmPasswordField.getText())) {
            statusLabel.setText("Passwords do not match!");
            return;
        }

        String userSql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
        String financeSql = "INSERT INTO user_finance (user_id, total_income) VALUES (LAST_INSERT_ID(), 0.0)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement userStmt = conn.prepareStatement(userSql);
                 PreparedStatement financeStmt = conn.prepareStatement(financeSql)) {

                userStmt.setString(1, user);
                userStmt.setString(2, email);
                userStmt.setString(3, pass);
                userStmt.executeUpdate();

                financeStmt.executeUpdate();

                conn.commit();

                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("Registration successful! You can now login.");

            } catch (Exception e) {
                conn.rollback();
                statusLabel.setText("Registration failed. Email might already exist.");
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("hello-view.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }
}