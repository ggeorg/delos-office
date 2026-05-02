module io.github.ggeorg.delos.writer.javafx {
    requires io.github.ggeorg.delos.javafx;
    requires io.github.ggeorg.delos.writer.core;
    requires io.github.ggeorg.delos.writer.layout;
    requires io.github.ggeorg.delos.writer.render;
    requires io.github.ggeorg.delos.render;
    requires javafx.controls;
    requires javafx.graphics;

    exports io.github.ggeorg.delos.writer.ui;
    exports io.github.ggeorg.delos.writer.ui.command;
    exports io.github.ggeorg.delos.writer.ui.control;
    exports io.github.ggeorg.delos.writer.ui.ruler;
}
