package expensetracker.expensetracker;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;

public class HelloController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    protected void onLoginButtonClick() {

        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.equals("admin@example.com") && password.equals("admin123")) {
            errorLabel.setText("Login successful!");
        } else {
            errorLabel.setText("Invalid credentials!");
        }
    }
}
