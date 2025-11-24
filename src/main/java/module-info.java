module expensetracker.expensetracker {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.sql;


    opens expensetracker.expensetracker to javafx.fxml;
    exports expensetracker.expensetracker;
}