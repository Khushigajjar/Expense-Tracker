package expensetracker.expensetracker;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

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

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM expenses")) {

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
        // TODO: Open a popup or scene to add new expense
        System.out.println("Add Expense button clicked!");
    }
}
