package io.github.ggeorg.delos.document;

final class DelosPackageXml {
    private DelosPackageXml() {
    }

    static String escapeText(String value) {
        String text = value == null ? "" : value;
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    static String escapeAttribute(String value) {
        return escapeText(value).replace("\"", "&quot;");
    }
}
