package expensetracker.expensetracker;

public class Expense {

    private int id;
    private String category;
    private double amount;
    private String date;
    private String description;

    public Expense(int id, String category, double amount, String date, String description) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.description = description;
    }

    // Getters and setters
    public int getId() { return id; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public String getDate() { return date; }
    public String getDescription() { return description; }

    public void setId(int id) { this.id = id; }
    public void setCategory(String category) { this.category = category; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setDate(String date) { this.date = date; }
    public void setDescription(String description) { this.description = description; }
}
