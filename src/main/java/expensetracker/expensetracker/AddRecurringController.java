package expensetracker.expensetracker;

import expensetracker.expensetracker.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class AddRecurringController {
    @FXML private TextField descField, amountField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Spinner<Integer> daySpinner;
    private int userId;

    @FXML
    public void initialize() {
        categoryComboBox.setItems(FXCollections.observableArrayList("Subscription", "Rent", "Utilities", "Insurance", "Other"));
        daySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 31, 1));
    }

    public void setUserId(int userId) { this.userId = userId; }

    @FXML
    private void onSave() {
        String sql = "INSERT INTO recurring_expenses (user_id, category, amount, description, day_of_month) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, categoryComboBox.getValue());
            stmt.setDouble(3, Double.parseDouble(amountField.getText()));
            stmt.setString(4, descField.getText());
            stmt.setInt(5, daySpinner.getValue());

            stmt.executeUpdate();
            onCancel(); // Close window
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onCancel() {
        ((Stage) descField.getScene().getWindow()).close();
    }
}