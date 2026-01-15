package expensetracker.expensetracker;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.Map;

import javafx.collections.transformation.SortedList;
import javafx.util.Pair;

public class DashboardController {

    private ObservableList<Expense> expensesList = FXCollections.observableArrayList();
    private int userId;
    @FXML private LineChart<String, Number> weeklyExpenseLineChart;
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
    @FXML private ProgressBar budgetProgressBar;
    @FXML private Label percentLabel;

    @FXML private TextField searchField;
    @FXML private ProgressBar savingsProgressBar;
    @FXML private Label savingsLabel;
    @FXML private VBox savingsGoalCard;
    @FXML private Label goalNameLabel;


    private String currentGoalName = "New Laptop";
    private double currentGoalAmount = 1200.0;

    private void setupSearchFilter() {
        // 1. Wrap the ObservableList in a FilteredList
        FilteredList<Expense> filteredData = new FilteredList<>(expensesList, p -> true);

        // 2. Set the filter Predicate whenever the filter changes.
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(expense -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();

                if (expense.getDescription().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches description
                } else if (expense.getCategory().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches category
                }
                return false;
            });
        });

        // 3. Wrap the FilteredList in a SortedList so users can still sort columns
        SortedList<Expense> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(expenseTable.comparatorProperty());

        // 4. Add sorted (and filtered) data to the table.
        expenseTable.setItems(sortedData);
    }

    public void setUserId(int userId) {
        this.userId = userId;
        processRecurringExpenses();
        loadExpenses();
    }

    @FXML
    protected void initialize() {

        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));

        setupContextMenu();
        setupSearchFilter();
        // Format amount column
        amountColumn.setCellFactory(column -> new TableCell<>() {
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
        loadWeeklyExpenseChart();
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
                String cat = rs.getString("category").trim();
                if (!cat.isEmpty()) {
                    cat = cat.substring(0, 1).toUpperCase() + cat.substring(1).toLowerCase();
                }
                double amt = rs.getDouble("amount");
                String date = rs.getString("date");
                String desc = rs.getString("description");

                expensesList.add(new Expense(id, cat, amt, date, desc));

                categoryMap.put(cat, categoryMap.getOrDefault(cat, 0.0) + amt);
                totalExpenses += amt;

                LocalDate expDate = LocalDate.parse(date);
                if (expDate.getMonth() == now.getMonth() && expDate.getYear() == now.getYear()) {
                    monthlyTotal += amt;
                }

                categoryMap.put(cat, categoryMap.getOrDefault(cat, 0.0) + amt);
            }

            updateUI(income, totalExpenses, categoryMap);


            loadWeeklyExpenseChart();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void updateUI(double income, double total, Map<String, Double> catMap) {
        double balance = income - total;

        incomeLabel.setText(String.format("Income: €%.2f", income));
        totalLabel.setText(String.format("Total: €%.2f", total));
        balanceLabel.setText(String.format("Balance: €%.2f", balance));

        balanceLabel.setStyle(balance < 0 ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;" : "-fx-text-fill: #2ecc71; -fx-font-weight: bold;");

        updateBudgetProgress(income, total);

        categoryPieChart.getData().clear();
        catMap.forEach((category, amount) -> {
            if (amount > 0) {
                PieChart.Data data = new PieChart.Data(category, amount);
                categoryPieChart.getData().add(data);

                // Add Hover Tooltip Listener
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        Tooltip t = new Tooltip(String.format("%s: €%.2f", data.getName(), data.getPieValue()));
                        Tooltip.install(newNode, t);
                    }
                });
            }
        });

        updateSavingsGoal(income, total);
    }


    private void updateBudgetProgress(double income, double totalExpenses) {
        if (income > 0) {
            double ratio = totalExpenses / income;


            budgetProgressBar.setProgress(Math.min(ratio, 1.0));

            if (ratio >= 1.0) {
                budgetProgressBar.setStyle("-fx-accent: #e74c3c;");
                percentLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else if (ratio >= 0.8) {
                budgetProgressBar.setStyle("-fx-accent: #f39c12;");
                percentLabel.setStyle("-fx-text-fill: #f39c12;");
            } else {
                budgetProgressBar.setStyle("-fx-accent: #3f51b5;");
                percentLabel.setStyle("-fx-text-fill: #3f51b5;");
            }

            percentLabel.setText(String.format("%.0f%%", ratio * 100));
        }
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
        // 1. Get the current income to show as a default in the dialog
        double currentIncome = getUserIncome();

        TextInputDialog dialog = new TextInputDialog(String.valueOf(currentIncome));
        dialog.setTitle("Update Income");
        dialog.setHeaderText("Monthly Income Adjustment");
        dialog.setContentText("Enter your new monthly income (€):");

        dialog.showAndWait().ifPresent(result -> {
            // SQL to update the user_finance table, NOT the expenses table
            String sql = "UPDATE user_finance SET total_income = ? WHERE user_id = ?";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                double newIncome = Double.parseDouble(result);

                stmt.setDouble(1, newIncome);
                stmt.setInt(2, userId);

                int rowsUpdated = stmt.executeUpdate();

                if (rowsUpdated > 0) {
                    // Refresh everything to show the new balance and budget progress
                    loadExpenses();

                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Income updated successfully!");
                    alert.show();
                }

            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter a valid numeric amount.");
                alert.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }


    public void loadWeeklyExpenseChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Expenses");

        // Change: Group by the actual 'date' instead of 'WEEKOFYEAR'
        // We limit it to the last 14 days so the chart doesn't get too crowded
        String sql = "SELECT date, SUM(amount) AS total " +
                "FROM expenses " +
                "WHERE user_id = ? AND date >= DATE_SUB(CURDATE(), INTERVAL 14 DAY) " +
                "GROUP BY date " +
                "ORDER BY date ASC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            weeklyExpenseLineChart.getData().clear();

            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                String dateStr = rs.getString("date"); // The date (e.g., 2026-01-15)
                double total = rs.getDouble("total");

                // Add the date string to the X-axis and total to the Y-axis
                series.getData().add(new XYChart.Data<>(dateStr, total));
            }

            if (hasData) {
                weeklyExpenseLineChart.getData().add(series);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @FXML
    protected void onExportCSV() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
        java.io.File file = chooser.showSaveDialog(expenseTable.getScene().getWindow());

        if (file != null) {
            try (java.io.PrintWriter pw = new java.io.PrintWriter(file)) {
                pw.println("Date,Category,Amount,Description");
                for (Expense e : expensesList) {
                    pw.printf("%s,%s,%.2f,%s%n", e.getDate(), e.getCategory(), e.getAmount(), e.getDescription());
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }





    @FXML
    protected void onAddRecurring() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("add_recurring.fxml"));
            Parent root = loader.load();
            AddRecurringController controller = loader.getController();
            controller.setUserId(userId);

            Stage stage = new Stage();
            stage.setTitle("Add Recurring Expense");
            stage.setScene(new Scene(root));

            stage.setOnHiding(event -> loadExpenses());

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not load the recurring expense window.");
            alert.show();
        }
    }


    private void processRecurringExpenses() {
        LocalDate today = LocalDate.now();
        String checkSql = "SELECT * FROM recurring_expenses WHERE user_id = ? AND (last_processed IS NULL OR MONTH(last_processed) != ? OR YEAR(last_processed) != ?)";
        String insertSql = "INSERT INTO expenses (user_id, category, amount, date, description) VALUES (?, ?, ?, ?, ?)";
        String updateRecSql = "UPDATE recurring_expenses SET last_processed = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, today.getMonthValue());
            stmt.setInt(3, today.getYear());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int billingDay = Math.min(rs.getInt("day_of_month"), today.lengthOfMonth());
                String finalDate = today.withDayOfMonth(billingDay).toString();
                try (PreparedStatement ins = conn.prepareStatement(insertSql);
                     PreparedStatement upd = conn.prepareStatement(updateRecSql)) {

                    ins.setInt(1, userId);
                    ins.setString(2, rs.getString("category"));
                    ins.setDouble(3, rs.getDouble("amount"));
                    ins.setString(4, finalDate);
                    ins.setString(5, "[Recurring] " + rs.getString("description"));
                    ins.executeUpdate();

                    upd.setString(1, today.toString());
                    upd.setInt(2, rs.getInt("id"));
                    upd.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void updateSavingsGoal(double income, double totalExpenses) {
        double currentSavings = income - totalExpenses;
        double progress = (currentSavings > 0) ? (currentSavings / currentGoalAmount) : 0;

        savingsProgressBar.setProgress(Math.min(progress, 1.0));
        savingsLabel.setText(String.format("€%.2f / €%.2f", Math.max(0, currentSavings), currentGoalAmount));

        if (progress >= 1.0) {
            savingsProgressBar.setStyle("-fx-accent: #2ecc71;");
        } else {
            savingsProgressBar.setStyle("-fx-accent: #3f51b5;");
        }
    }


    @FXML
    private void handleGoalClick() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Update Savings Goal");
        dialog.setHeaderText("Set your new goal name and target amount");
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField goalName = new TextField(currentGoalName);
        TextField goalAmount = new TextField(String.valueOf(currentGoalAmount));

        grid.add(new Label("Goal Name:"), 0, 0);
        grid.add(goalName, 1, 0);
        grid.add(new Label("Target Amount (€):"), 0, 1);
        grid.add(goalAmount, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Convert the result to a name-amount pair when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new Pair<>(goalName.getText(), goalAmount.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                this.currentGoalName = result.getKey();
                this.currentGoalAmount = Double.parseDouble(result.getValue());

                // Update the UI labels
                goalNameLabel.setText("Savings Goal: " + currentGoalName);

                // Refresh calculations
                loadExpenses();

            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid amount. Please enter a number.");
                alert.show();
            }
        });
    }


    @FXML
    protected void onSplitExpense() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Split Bill Calculator");
        dialog.setHeaderText("Enter details to deduct your share");

        ButtonType addButtonType = new ButtonType("Deduct My Share", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));

        TextField totalBillField = new TextField();
        totalBillField.setPromptText("Total Bill (€)");

        TextField peopleCountField = new TextField("2");

        // NEW: Custom Description Field
        TextField customDescField = new TextField();
        customDescField.setPromptText("e.g., Pizza with friends");

        Label resultLabel = new Label("Your share: €0.00");
        resultLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #3f51b5;");

        grid.add(new Label("Total Bill:"), 0, 0);
        grid.add(totalBillField, 1, 0);
        grid.add(new Label("People:"), 0, 1);
        grid.add(peopleCountField, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(customDescField, 1, 2);
        grid.add(resultLabel, 0, 3, 2, 1);

        // Math listener
        Runnable calculate = () -> {
            try {
                double total = Double.parseDouble(totalBillField.getText());
                int people = Integer.parseInt(peopleCountField.getText());
                if (people > 0) resultLabel.setText(String.format("Your share: €%.2f", total / people));
            } catch (Exception ignored) {}
        };
        totalBillField.textProperty().addListener((o, old, n) -> calculate.run());
        peopleCountField.textProperty().addListener((o, old, n) -> calculate.run());

        dialog.getDialogPane().setContent(grid);

        // Convert result to a Pair containing (ShareAmount, CustomDescription)
        dialog.setResultConverter(btn -> {
            if (btn == addButtonType) {
                try {
                    double share = Double.parseDouble(totalBillField.getText()) / Integer.parseInt(peopleCountField.getText());
                    return new Pair<>(String.valueOf(share), customDescField.getText());
                } catch (Exception e) { return null; }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            String sql = "INSERT INTO expenses (user_id, category, amount, date, description) VALUES (?, ?, ?, ?, ?)";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                double myShare = Double.parseDouble(result.getKey());
                String userDescription = result.getValue();

                stmt.setInt(1, userId);
                stmt.setString(2, "Split Bill");
                stmt.setDouble(3, myShare);
                stmt.setString(4, java.time.LocalDate.now().toString());
                // Use the written description from the user
                stmt.setString(5, userDescription.isEmpty() ? "Split Bill Share" : userDescription);

                stmt.executeUpdate();
                loadExpenses(); // Refresh table and charts instantly

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    private void openSplitExpenseWindow(double preFilledAmount) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("add_expense.fxml"));
            Parent root = loader.load();

            AddExpenseController controller = loader.getController();
            controller.setUserId(userId);

            // You'll need to add a 'setAmount' method in your AddExpenseController
            controller.setAmount(preFilledAmount);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();
            stage.setOnHiding(event -> loadExpenses());
        } catch (IOException e) { e.printStackTrace(); }
    }



}