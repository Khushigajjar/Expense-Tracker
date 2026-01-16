package expensetracker.expensetracker;

import java.util.List;
import java.util.Map;

public class ExpenseSummary {
    private final List<Expense> allExpenses;
    private final Map<String, Double> categoryMap;
    private final double totalAmount;

    public ExpenseSummary(List<Expense> allExpenses, Map<String, Double> categoryMap, double totalAmount) {
        this.allExpenses = allExpenses;
        this.categoryMap = categoryMap;
        this.totalAmount = totalAmount;
    }

    public List<Expense> getAllExpenses() { return allExpenses; }
    public Map<String, Double> getCategoryMap() { return categoryMap; }
    public double getTotalAmount() { return totalAmount; }
}