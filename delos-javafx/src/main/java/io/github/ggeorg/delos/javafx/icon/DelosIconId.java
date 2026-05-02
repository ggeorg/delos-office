package io.github.ggeorg.delos.javafx.icon;

import java.util.Objects;

/**
 * Stable Delos icon names.
 * <p>
 * Application code should depend on this enum, not on a concrete Ikonli icon
 * pack. The current pack is Feather because it gives Delos a quiet,
 * Lucide-like line-icon style while staying easy to use from JavaFX.
 */
public enum DelosIconId {
    NEW("fth-file-plus"),
    OPEN("fth-folder"),
    SAVE("fth-save"),
    SAVE_AS("fth-save"),
    PRINT("fth-printer"),
    EXPORT("fth-upload"),
    UNDO("fth-rotate-ccw"),
    REDO("fth-rotate-cw"),
    CUT("fth-scissors"),
    COPY("fth-copy"),
    PASTE("fth-clipboard"),
    DELETE("fth-trash-2"),
    CLEAR("fth-delete"),
    SELECT_ALL("fth-check-square"),
    FIND("fth-search"),
    SETTINGS("fth-settings"),
    HELP("fth-help-circle"),
    ABOUT("fth-info"),
    ZOOM_IN("fth-zoom-in"),
    ZOOM_OUT("fth-zoom-out"),
    ZOOM_FIT("fth-maximize"),
    COMMAND_PALETTE("fth-command"),
    INSPECTOR("fth-sidebar"),
    IMAGE("fth-image"),
    TABLE("fth-grid"),
    FORMULA("fth-hash"),
    PAGE_BREAK("fth-file"),
    BOLD("fth-bold"),
    ITALIC("fth-italic"),
    UNDERLINE("fth-underline"),
    STRIKETHROUGH("fth-type"),
    TEXT_COLOR("fth-droplet"),
    ALIGN_LEFT("fth-align-left"),
    ALIGN_CENTER("fth-align-center"),
    ALIGN_RIGHT("fth-align-right"),
    ALIGN_JUSTIFY("fth-align-justify"),
    BULLETED_LIST("fth-list"),
    NUMBERED_LIST("fth-list"),
    DECREASE_INDENT("fth-arrow-left"),
    INCREASE_INDENT("fth-arrow-right"),
    LINE_SPACING("fth-move"),
    RULER("fth-sidebar"),
    FUNCTION("fth-hash"),
    ROW("fth-more-horizontal"),
    COLUMN("fth-more-vertical"),
    SHEET("fth-file-text"),
    CHART("fth-bar-chart-2"),
    SORT("fth-arrow-up"),
    FILTER("fth-filter"),
    RECALCULATE("fth-refresh-cw"),
    SLIDE("fth-monitor"),
    TEXT_BOX("fth-type"),
    SHAPE("fth-square"),
    THEME("fth-sliders"),
    PLAY("fth-play"),
    DATABASE("fth-database"),
    QUERY("fth-search"),
    FORM("fth-layout"),
    REPORT("fth-file-text"),
    RELATIONSHIPS("fth-git-branch"),
    SQL("fth-terminal");

    private final String iconLiteral;

    DelosIconId(String iconLiteral) {
        this.iconLiteral = Objects.requireNonNull(iconLiteral, "iconLiteral");
    }

    public String iconLiteral() {
        return iconLiteral;
    }
}
