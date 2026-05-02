module io.github.ggeorg.delos.writer.core {
    requires java.xml;
    requires transitive io.github.ggeorg.delos.document;

    exports io.github.ggeorg.delos.writer.document;
    exports io.github.ggeorg.delos.writer.session;
    exports io.github.ggeorg.delos.writer.io;
    exports io.github.ggeorg.delos.writer.editor;
}
