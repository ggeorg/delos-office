package io.github.ggeorg.delos.base.core;

public enum DatabaseObjectKind {
    TABLE("Tables"),
    QUERY("Queries"),
    FORM("Forms"),
    REPORT("Reports"),
    RELATIONSHIP("Relationships");

    private final String displayName;

    DatabaseObjectKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
