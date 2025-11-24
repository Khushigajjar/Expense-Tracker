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
import java.sql.SQLException;

public class HelloController {

    @FXML
    private TextField emailField;

    @FXML
    private TextField passwordField;

    @FXML
    private Label errorLabel;

    // Called when login button is clicked
    @FXML
    protected void onLoginButtonClick() throws SQLException {
        String email = emailField.getText();
        String password = passwordField.getText();

        String query = "SELECT * FROM users WHERE email = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            stmt.setString(2, password);

            java.sql.ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                errorLabel.setText("Login successful!");
                // Dashboard
                try{
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("dashboard.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) emailField.getScene().getWindow();
                    stage.setScene(new Scene(root));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                }
            }
            else {
                errorLabel.setText("Invalid credentials!");
            }

        }
        catch(Exception e) {
            e.printStackTrace();
            errorLabel.setText("Database error!");
        }
    }
}