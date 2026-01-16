package expensetracker.expensetracker;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.collections.ObservableList;

public class FilterUtility {
    public static void applySearchFilter(TextField searchField, TableView<Expense> table, ObservableList<Expense> data) {
        FilteredList<Expense> filteredData = new FilteredList<>(data, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(expense -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lowerFilter = newVal.toLowerCase();
                return expense.getDescription().toLowerCase().contains(lowerFilter) ||
                        expense.getCategory().toLowerCase().contains(lowerFilter);
            });
        });

        SortedList<Expense> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);
    }
}