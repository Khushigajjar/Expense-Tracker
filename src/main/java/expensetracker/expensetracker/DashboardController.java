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
import javafx.collections.transformation.SortedList;
import javafx.util.Pair;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import javafx.beans.binding.StringBinding;
import java.text.NumberFormat;
import java.util.Locale;

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

            updateUI(income, totalExpenses, categoryMap);


            loadWeeklyExpenseChart();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void updateUI(double income, double total, java.util.Map<String, Double> catMap) {
        incomeLabel.setText(String.format("Income: €%.2f", income));
        totalLabel.setText(String.format("Total: €%.2f", total));
        balanceLabel.setText(String.format("Balance: €%.2f", income - total));

        updateBudgetProgress(income, total);
        categoryPieChart.getData().clear();
        catMap.forEach((k, v) -> categoryPieChart.getData().add(new PieChart.Data(k, v)));

        // Install tooltips on each pie slice so the user sees the exact amount when hovering
        installPieTooltips();

        if (incomeExpenseBarChart != null) {
            incomeExpenseBarChart.getData().clear();
            XYChart.Series<String, Double> series = new XYChart.Series<>();
            series.getData().add(new XYChart.Data<>("Income", income));
            series.getData().add(new XYChart.Data<>("Expenses", total));
            incomeExpenseBarChart.getData().add(series);
        }
        updateSavingsGoal(income, total);
    }


    private void updateBudgetProgress(double income, double totalExpenses) {
        if (income > 0) {
            double ratio = totalExpenses / income;
            budgetProgressBar.setProgress(ratio);

            // Change color to red if over budget
            if (ratio >= 1.0) {
                budgetProgressBar.setStyle("-fx-accent: #e74c3c;");
            } else {
                budgetProgressBar.setStyle("-fx-accent: #3f51b5;");
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
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }


    public void loadWeeklyExpenseChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Weekly Expenses");

        String sql = "SELECT WEEKOFYEAR(date) AS week, SUM(amount) AS total " +
                "FROM expenses " +
                "WHERE user_id = ? " +
                "GROUP BY WEEKOFYEAR(date) " +
                "ORDER BY WEEKOFYEAR(date)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            weeklyExpenseLineChart.getData().clear();

            boolean hasData = false; // to check if query returned rows
            while (rs.next()) {
                hasData = true;
                int weekNumber = rs.getInt("week");
                double total = rs.getDouble("total");
                series.getData().add(new XYChart.Data<>("Week " + weekNumber, total));
            }

            if (hasData) {
                weeklyExpenseLineChart.getData().add(series);
            } else {
                System.out.println("No weekly expense data for this user.");
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
        double goalAmount = 1200.0;
        double currentSavings = income - totalExpenses;
        double progress = (currentSavings > 0) ? (currentSavings / goalAmount) : 0;

        savingsProgressBar.setProgress(Math.min(progress, 1.0));

        savingsLabel.setText(String.format("€%.2f / €%.2f", Math.max(0, currentSavings), goalAmount));

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



    // Adds a tooltip to each PieChart.Data node showing the formatted currency value.
    private void installPieTooltips() {
        if (categoryPieChart == null) return;

        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.getDefault());

        for (PieChart.Data data : categoryPieChart.getData()) {
            // Create a tooltip and bind its text to the pie value so it updates automatically
            Tooltip tooltip = new Tooltip();
            StringBinding binding = new StringBinding() {
                { bind(data.pieValueProperty()); }

                @Override
                protected String computeValue() {
                    return nf.format(data.getPieValue());
                }
            };
            tooltip.textProperty().bind(binding);

            // Show tooltip quickly
            tooltip.setShowDelay(Duration.millis(50));
            Tooltip.install(data.getNode(), tooltip);

            // Simple hover visual feedback
            data.getNode().setOnMouseEntered(e -> data.getNode().setOpacity(0.8));
            data.getNode().setOnMouseExited(e -> data.getNode().setOpacity(1.0));
        }
    }


}

