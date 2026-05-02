package io.github.ggeorg.delos.calc.app;

import io.github.ggeorg.delos.javafx.DelosStylesheets;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class DelosCalcApp extends Application {
    @Override
    public void start(Stage stage) {
        CalcMainWindow root = new CalcMainWindow(stage);
        Scene scene = new Scene(root, 1280, 840);
        DelosStylesheets.addTo(scene);
        scene.getStylesheets().add(DelosCalcApp.class.getResource("/io/github/ggeorg/delos/calc/app/delos-calc.css").toExternalForm());

        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            if (!root.requestClose()) {
                event.consume();
            }
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
