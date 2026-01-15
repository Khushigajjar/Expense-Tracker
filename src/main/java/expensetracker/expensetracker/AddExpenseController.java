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

    private int userId = 1;

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


        String sql;
        boolean isEdit = (editingExpenseId != -1);

        if (isEdit) {
            sql = "UPDATE expenses SET category=?, amount=?, date=?, description=? WHERE id=? AND user_id=?";
        } else {
            sql = "INSERT INTO expenses (category, amount, date, description, user_id) VALUES (?, ?, ?, ?, ?)";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (isEdit) {
                stmt.setString(1, category);
                stmt.setDouble(2, amount);
                stmt.setString(3, date);
                stmt.setString(4, description);
                stmt.setInt(5, editingExpenseId);
                stmt.setInt(6, userId);
            } else {
                stmt.setString(1, category);
                stmt.setDouble(2, amount);
                stmt.setString(3, date);
                stmt.setString(4, description);
                stmt.setInt(5, userId);
            }

            stmt.executeUpdate();

            categoryField.getScene().getWindow().hide();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private int editingExpenseId = -1;

    public void setExpenseData(Expense expense) {
        this.editingExpenseId = expense.getId();
        categoryField.setText(expense.getCategory());
        amountField.setText(String.valueOf(expense.getAmount()));
        descriptionField.setText(expense.getDescription());
        datePicker.setValue(java.time.LocalDate.parse(expense.getDate()));
    }

    public void setAmount(double amount) {
        amountField.setText(String.format("%.2f", amount));
    }
}