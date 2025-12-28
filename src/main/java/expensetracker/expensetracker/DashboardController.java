package expensetracker.expensetracker;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.time.LocalDate;

public class DashboardController {

    @FXML private TableView<Expense> expenseTable;
    @FXML private PieChart categoryPieChart;
    @FXML private BarChart<String, Double> incomeExpenseBarChart;
    @FXML private TableColumn<Expense, String> dateColumn;
    @FXML private TableColumn<Expense, String> categoryColumn;
    @FXML private TableColumn<Expense, Double> amountColumn;
    @FXML private TableColumn<Expense, String> descriptionColumn;
    @FXML private Label totalLabel;
    @FXML private Label incomeLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label balanceLabel;

    private ObservableList<Expense> expensesList = FXCollections.observableArrayList();
    private int userId;

    public void setUserId(int userId) {
        this.userId = userId;
        loadExpenses();
    }

    @FXML
    protected void initialize() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("€%.2f", amount));
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                }
            }
        });

        setupContextMenu();
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit Expense");
        editItem.setOnAction(event -> onEditExpense());
        MenuItem deleteItem = new MenuItem("Delete Expense");
        deleteItem.setOnAction(event -> onDeleteExpense());
        contextMenu.getItems().addAll(editItem, deleteItem);

        expenseTable.setRowFactory(tv -> {
            TableRow<Expense> row = new TableRow<>();
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );
            return row;
        });
    }

    private void loadExpenses() {
        expensesList.clear();
        double totalExpenses = 0;
        double monthlyTotal = 0;
        double income = getUserIncome();
        LocalDate now = LocalDate.now();
        java.util.Map<String, Double> categoryMap = new java.util.HashMap<>();

        String sql = "SELECT * FROM expenses WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String cat = rs.getString("category");
                double amt = rs.getDouble("amount");
                String date = rs.getString("date");
                String desc = rs.getString("description");

                expensesList.add(new Expense(id, cat, amt, date, desc));
                totalExpenses += amt;


                LocalDate expDate = LocalDate.parse(date);
                if (expDate.getMonth() == now.getMonth() && expDate.getYear() == now.getYear()) {
                    monthlyTotal += amt;
                }

                categoryMap.put(cat, categoryMap.getOrDefault(cat, 0.0) + amt);
            }

            expenseTable.setItems(expensesList);
            updateUI(income, totalExpenses, categoryMap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUI(double income, double total, java.util.Map<String, Double> catMap) {
        incomeLabel.setText(String.format("Income: €%.2f", income));
        totalLabel.setText(String.format("Total: €%.2f", total));
        balanceLabel.setText(String.format("Balance: €%.2f", income - total));


        categoryPieChart.getData().clear();
        catMap.forEach((k, v) -> categoryPieChart.getData().add(new PieChart.Data(k, v)));

        incomeExpenseBarChart.getData().clear();
        XYChart.Series<String, Double> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Income", income));
        series.getData().add(new XYChart.Data<>("Expenses", total));
        incomeExpenseBarChart.getData().add(series);
    }

    @FXML
    protected void onAddExpense() {
        openExpenseWindow(null);
    }

    @FXML
    protected void onEditExpense() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected != null) openExpenseWindow(selected);
    }

    private void openExpenseWindow(Expense expense) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("add_expense.fxml"));
            Parent root = loader.load();
            AddExpenseController controller = loader.getController();
            controller.setUserId(userId);
            if (expense != null) controller.setExpenseData(expense);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();
            stage.setOnHiding(event -> loadExpenses());
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    protected void onDeleteExpense() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this expense?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM expenses WHERE id=?")) {
                stmt.setInt(1, selected.getId());
                stmt.executeUpdate();
                loadExpenses();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private double getUserIncome() {
        double income = 0.0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT total_income FROM user_finance WHERE user_id=?")) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) income = rs.getDouble("total_income");
        } catch (Exception e) { e.printStackTrace(); }
        return income;
    }

    @FXML
    protected void onUpdateIncome() {
        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setHeaderText("Update Monthly Income");
        dialog.showAndWait().ifPresent(amount -> {
            try {
                double val = Double.parseDouble(amount);
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("INSERT INTO user_finance (user_id, total_income) VALUES (?,?) ON DUPLICATE KEY UPDATE total_income=?")) {
                    stmt.setInt(1, userId);
                    stmt.setDouble(2, val);
                    stmt.setDouble(3, val);
                    stmt.executeUpdate();
                    loadExpenses();
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    public void setDisplayName(String name) {
        if (welcomeLabel != null) welcomeLabel.setText("Hello, " + name + "!");
    }

    @FXML
    protected void onLogout(javafx.event.ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("hello-view.fxml"));
            Stage stage = (Stage) expenseTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }
}