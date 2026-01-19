package expensetracker.expensetracker;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

public class ChartService {

    public ObservableList<PieChart.Data> getCategoryPieData(Map<String, Double> catMap) {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        catMap.forEach((category, amount) -> {
            if (amount > 0) {
                PieChart.Data data = new PieChart.Data(category, amount);
                pieChartData.add(data);
            }
        });
        return pieChartData;
    }

    public XYChart.Series<String, Number> getWeeklyExpenseSeries(int userId) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Expenses");

        String sql = "SELECT date, SUM(amount) AS total FROM expenses " +
                "WHERE user_id = ? AND date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
                "GROUP BY date ORDER BY date ASC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                series.getData().add(new XYChart.Data<>(rs.getString("date"), rs.getDouble("total")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return series;
    }
}