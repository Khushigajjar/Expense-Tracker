package expensetracker.expensetracker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.util.HashMap;
import java.time.LocalDate;
import java.util.Map;

public class FinanceService {

    public double getUserIncome(int userId) {
        double income = 0.0;
        String sql = "SELECT total_income FROM user_finance WHERE user_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) income = rs.getDouble("total_income");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return income;
    }


    public void deleteExpense(int expenseId) throws Exception {
        String sql = "DELETE FROM expenses WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, expenseId);
            stmt.executeUpdate();
        }
    }


    public void processRecurringExpenses(int userId) {
        LocalDate today = LocalDate.now();
        // Logic to find expenses not yet processed for this month/year
        String checkSql = "SELECT * FROM recurring_expenses WHERE user_id = ? AND (last_processed IS NULL OR MONTH(last_processed) != ? OR YEAR(last_processed) != ?)";
        String insertSql = "INSERT INTO expenses (user_id, category, amount, date, description) VALUES (?, ?, ?, ?, ?)";
        String updateRecSql = "UPDATE recurring_expenses SET last_processed = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Use a transaction for safety
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, userId);
                checkStmt.setInt(2, today.getMonthValue());
                checkStmt.setInt(3, today.getYear());
                ResultSet rs = checkStmt.executeQuery();

                while (rs.next()) {
                    int billingDay = Math.min(rs.getInt("day_of_month"), today.lengthOfMonth());
                    String finalDate = today.withDayOfMonth(billingDay).toString();

                    // Insert into main expenses table
                    try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                        ins.setInt(1, userId);
                        ins.setString(2, rs.getString("category"));
                        ins.setDouble(3, rs.getDouble("amount"));
                        ins.setString(4, finalDate);
                        ins.setString(5, "[Recurring] " + rs.getString("description"));
                        ins.executeUpdate();
                    }

                    // Mark as processed
                    try (PreparedStatement upd = conn.prepareStatement(updateRecSql)) {
                        upd.setString(1, today.toString());
                        upd.setInt(2, rs.getInt("id"));
                        upd.executeUpdate();
                    }
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void addSplitExpense(int userId, double share, String description) throws Exception {
        String sql = "INSERT INTO expenses (user_id, category, amount, date, description) VALUES (?, 'Split Bill', ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setDouble(2, share);
            stmt.setString(3, java.time.LocalDate.now().toString());
            stmt.setString(4, description.isEmpty() ? "Split Bill Share" : description);
            stmt.executeUpdate();
        }
    }

    public void updateIncome(int userId, double newIncome) throws Exception {
        String sql = "UPDATE user_finance SET total_income = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, newIncome);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public ExpenseSummary getExpenseSummary(int userId) {
        List<Expense> expenses = new ArrayList<>();
        Map<String, Double> categoryMap = new HashMap<>();
        double total = 0;

        String sql = "SELECT * FROM expenses WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String cat = rs.getString("category").trim();
                if (!cat.isEmpty()) {
                    cat = cat.substring(0, 1).toUpperCase() + cat.substring(1).toLowerCase();
                }
                double amt = rs.getDouble("amount");

                Expense expense = new Expense(
                        rs.getInt("id"), cat, amt, rs.getString("date"), rs.getString("description")
                );

                expenses.add(expense);
                categoryMap.put(cat, categoryMap.getOrDefault(cat, 0.0) + amt);
                total += amt;
            }
        } catch (Exception e) { e.printStackTrace(); }

        return new ExpenseSummary(expenses, categoryMap, total);
    }


    public double getBudgetRatio(double totalExpenses, double income) {
        if (income <= 0) return 0.0;
        return totalExpenses / income;
    }

    public double getSavingsProgress(double income, double totalExpenses, double goalAmount) {
        double currentSavings = income - totalExpenses;
        if (goalAmount <= 0 || currentSavings <= 0) return 0.0;
        return currentSavings / goalAmount;
    }

}