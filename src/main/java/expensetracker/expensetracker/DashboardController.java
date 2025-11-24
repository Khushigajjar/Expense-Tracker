package expensetracker.expensetracker;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

public class DashboardController {

    @FXML
    private TableView<Expense> expenseTable;

    @FXML
    private TableColumn<Expense, String> dateColumn;

    @FXML
    private TableColumn<Expense, String> categoryColumn;

    @FXML
    private TableColumn<Expense, Double> amountColumn;

    @FXML
    private TableColumn<Expense, String> descriptionColumn;

    @FXML
    private Label totalLabel;

    private ObservableList<Expense> expensesList = FXCollections.observableArrayList();


    private int userId;

    public void setUserId(int userId) {
        this.userId = userId;
        loadExpenses(); // load expenses for this user when set
    }

    @FXML
    protected void initialize() {
        // Set up table columns
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Load expenses from DB
        loadExpenses();
    }

    private void loadExpenses() {
        expensesList.clear();
        double total = 0;

        String sql = "SELECT * FROM expenses WHERE user_id = ?"; // Only for this user

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId); // Bind userId
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Expense exp = new Expense(
                        rs.getInt("id"),
                        rs.getString("category"),
                        rs.getDouble("amount"),
                        rs.getString("date"),
                        rs.getString("description")
                );
                expensesList.add(exp);
                total += rs.getDouble("amount");
            }

            expenseTable.setItems(expensesList);
            totalLabel.setText(String.valueOf(total));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onAddExpense() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("add_expense.fxml"));
            Parent root = loader.load();

            AddExpenseController controller = loader.getController();
            controller.setUserId(userId); // Pass logged-in user ID

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Add Expense");
            stage.show();

            // Refresh dashboard after closing add expense window
            stage.setOnHiding(event -> loadExpenses());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
