module expensetracker.frontend {
    requires javafx.controls;
    requires javafx.fxml;


    opens expensetracker.frontend to javafx.fxml;
    exports expensetracker.frontend;
}