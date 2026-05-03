module io.github.ggeorg.delos.writer.app {
    requires io.github.ggeorg.delos.document;
    requires io.github.ggeorg.delos.javafx;
    requires io.github.ggeorg.delos.render;
    requires io.github.ggeorg.delos.writer.core;
    requires io.github.ggeorg.delos.writer.layout;
    requires io.github.ggeorg.delos.writer.javafx;
    requires io.github.ggeorg.delos.writer.pdf;
    requires io.github.ggeorg.delos.writer.print;
    requires javafx.controls;
    requires javafx.graphics;
    requires java.desktop;

    exports io.github.ggeorg.delos.writer.app;
}
