module com.darya.gamedrawandguess {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires kotlinx.serialization.core;
    requires kotlinx.serialization.json;

    opens com.darya.gamedrawandguess to javafx.fxml, kotlinx.serialization.json;
    opens com.darya.gamedrawandguess.ui to javafx.fxml;
    exports com.darya.gamedrawandguess;
}