module com.darya.gamedrawandguess {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;


    opens com.darya.gamedrawandguess.ui to javafx.fxml;
    exports com.darya.gamedrawandguess;
}