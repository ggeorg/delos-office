package io.github.ggeorg.delos.base.app;

import io.github.ggeorg.delos.javafx.DelosStylesheets;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class DelosBaseApp extends Application {
    @Override
    public void start(Stage stage) {
        BaseMainWindow root = new BaseMainWindow(stage);
        Scene scene = new Scene(root, 1280, 840);
        DelosStylesheets.addTo(scene);
        scene.getStylesheets().add(DelosBaseApp.class
                .getResource("/io/github/ggeorg/delos/base/app/delos-base.css")
                .toExternalForm());

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
