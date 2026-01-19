package expensetracker.expensetracker;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
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
import javafx.util.Duration;
import javafx.util.Pair;

public class DashboardController {

    private ObservableList<Expense> expensesList = FXCollections.observableArrayList();
    private final FinanceService financeService = new FinanceService();
    private NavigationService navService = new NavigationService();
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

    private ChartService chartService = new ChartService();
    private String currentGoalName = "New Laptop";
    private double currentGoalAmount = 1200.0;

    private void setupSearchFilter() {
        FilteredList<Expense> filteredData = new FilteredList<>(expensesList, p -> true);

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

        SortedList<Expense> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(expenseTable.comparatorProperty());

        expenseTable.setItems(sortedData);
    }

    public void setUserId(int userId) {
        this.userId = userId;
        financeService.processRecurringExpenses(userId);
        loadExpenses();
    }

    @FXML
    protected void initialize() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        setupContextMenu();
        FilterUtility.applySearchFilter(searchField, expenseTable, expensesList);
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

    @FXML
    protected void onEditExpense() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            navService.openExpenseWindow(userId, selected, this::loadExpenses);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select an expense to edit.");
            alert.show();
        }
    }

    private void loadExpenses() {
        ExpenseSummary summary = financeService.getExpenseSummary(userId);
        double income = financeService.getUserIncome(userId);
        expensesList.setAll(summary.getAllExpenses());
        updateUI(income, summary.getTotalAmount(), summary.getCategoryMap());
        loadWeeklyExpenseChart();
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
        double ratio = financeService.getBudgetRatio(totalExpenses, income);
        budgetProgressBar.setProgress(Math.min(ratio, 1.0));
        percentLabel.setText(String.format("%.0f%%", ratio * 100));
        budgetProgressBar.getStyleClass().remove("warning-bar");
        budgetProgressBar.getStyleClass().remove("danger-bar");

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
    }

    @FXML
    protected void onDeleteExpense() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this expense?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                financeService.deleteExpense(selected.getId());
                loadExpenses();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    protected void onUpdateIncome() {
        double currentIncome = financeService.getUserIncome(userId);
        TextInputDialog dialog = new TextInputDialog(String.valueOf(currentIncome));

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-dialog");

        dialog.setTitle("Update Income");
        dialog.setHeaderText("Monthly Income Adjustment");
        dialog.setGraphic(null);

        FadeTransition ft = new FadeTransition(Duration.millis(400), dialogPane);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();

        dialog.showAndWait().ifPresent(result -> {
            try {
                double newIncome = Double.parseDouble(result);
                financeService.updateIncome(userId, newIncome);
                loadExpenses();
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
        Stage stage = (Stage) expenseTable.getScene().getWindow();
        navService.switchScene(stage, "hello-view.fxml");
    }


    public void loadWeeklyExpenseChart() {
        weeklyExpenseLineChart.getData().clear();
        XYChart.Series<String, Number> series = chartService.getWeeklyExpenseSeries(userId);
        if (!series.getData().isEmpty()) {
            weeklyExpenseLineChart.getData().add(series);
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
        navService.openAddRecurringWindow(userId, () -> {
            financeService.processRecurringExpenses(userId);
            loadExpenses();
        });
    }





    private void updateSavingsGoal(double income, double totalExpenses) {
        double progress = financeService.getSavingsProgress(income, totalExpenses, currentGoalAmount);
        double currentSavings = income - totalExpenses;

        savingsProgressBar.setProgress(Math.min(progress, 1.0));
        savingsLabel.setText(String.format("€%.2f / €%.2f", Math.max(0, currentSavings), currentGoalAmount));

        savingsProgressBar.setStyle(progress >= 1.0 ? "-fx-accent: #2ecc71;" : "-fx-accent: #3f51b5;");
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

        DialogPane dialogPane = dialog.getDialogPane();
        String css = getClass().getResource("style.css").toExternalForm();
        dialogPane.getStylesheets().add(css);
        dialogPane.getStyleClass().add("custom-dialog");

        dialog.setHeaderText("Divide your expenses easily");

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField totalBill = new TextField();
        totalBill.setPromptText("Total Bill (€)");
        totalBill.getStyleClass().add("indigo-field"); // Custom CSS class

        TextField peopleCount = new TextField("2");
        peopleCount.getStyleClass().add("indigo-field");

        Label resultLabel = new Label("Your share: €0.00");
        resultLabel.setStyle("-fx-text-fill: #3f51b5; -fx-font-weight: bold; -fx-font-size: 14px;");

        grid.add(new Label("Bill Amount:"), 0, 0);
        grid.add(totalBill, 1, 0);
        grid.add(new Label("Number of People:"), 0, 1);
        grid.add(peopleCount, 1, 1);
        grid.add(resultLabel, 0, 2, 2, 1);

        dialogPane.setContent(grid);

        totalBill.textProperty().addListener((o, old, n) -> calculateShare(totalBill, peopleCount, resultLabel));
        peopleCount.textProperty().addListener((o, old, n) -> calculateShare(totalBill, peopleCount, resultLabel));

        ButtonType addButton = new ButtonType("Add to Expenses", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            double share = Double.parseDouble(totalBill.getText()) / Integer.parseInt(peopleCount.getText());
            try {
                financeService.addSplitExpense(userId, share, "Split Bill Share");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            loadExpenses();
        });
    }

    private void calculateShare(TextField bill, TextField people, Label result) {
        try {
            double total = Double.parseDouble(bill.getText());
            int count = Integer.parseInt(people.getText());
            if (count > 0) result.setText(String.format("Your share: €%.2f", total / count));
        } catch (Exception ignored) {}
    }


    @FXML
    protected void onAddExpense() {
        navService.openExpenseWindow(userId, null, this::loadExpenses);
    }
}