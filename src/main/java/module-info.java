module expensetracker.expensetracker {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens expensetracker.expensetracker to javafx.fxml;
    exports expensetracker.expensetracker;
}