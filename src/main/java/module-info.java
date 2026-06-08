module org.example.modbuddesktopproject {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires com.fazecast.jSerialComm;
    requires static lombok;
    requires java.desktop;

    opens org.example.modbuddesktopproject to javafx.fxml;
    opens org.example.modbuddesktopproject.controller to javafx.fxml;

    exports org.example.modbuddesktopproject;
    exports org.example.modbuddesktopproject.controller;
}