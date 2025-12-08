package expensetracker.expensetracker;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class AddExpenseController {

    @FXML
    private TextField categoryField;

    @FXML
    private TextField amountField;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField descriptionField;

    @FXML
    private Button saveButton;

    private int userId=1;

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @FXML
    protected void onSaveExpense() {
        String category = categoryField.getText();
        String amountText = amountField.getText();
        String description = descriptionField.getText();

        if (category.isEmpty() || amountText.isEmpty() || datePicker.getValue() == null) {
            System.out.println("Please fill in all required fields!");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount!");
            return;
        }

        String date = datePicker.getValue().toString();

        String sql = "INSERT INTO expenses (user_id, category, amount, date, description) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, category);
            stmt.setDouble(3, amount);
            stmt.setString(4, date);
            stmt.setString(5, description);

            stmt.executeUpdate();
            saveButton.getScene().getWindow().hide();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
