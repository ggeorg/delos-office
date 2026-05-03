package io.github.ggeorg.delos.javafx.icon;

import java.util.Objects;

/**
 * Stable Delos icon names.
 * <p>
 * Application code should depend on this enum, not on a concrete Ikonli icon
 * pack. The current pack is Material Design Icons because its heavier desktop
 * glyphs match the Pages-inspired Delos chrome better than Feather/FA in the
 * Writer toolbar.
 */
public enum DelosIconId {
    NEW("mdi-file-plus"),
    OPEN("mdi-folder-open"),
    SAVE("mdi-content-save"),
    SAVE_AS("mdi-content-save-all"),
    PRINT("mdi-printer"),
    EXPORT("mdi-file-export"),
    SHARE("mdi-share-variant"),
    LEFT_SIDEBAR("mdi-view-quilt"),
    PAGE_LAYOUT("mdi-file-document"),
    COMMENT("mdi-comment"),
    UNDO("mdi-undo"),
    REDO("mdi-redo"),
    CUT("mdi-content-cut"),
    COPY("mdi-content-copy"),
    PASTE("mdi-content-paste"),
    DELETE("mdi-delete"),
    CLEAR("mdi-backspace"),
    SELECT_ALL("mdi-checkbox-marked"),
    FIND("mdi-magnify"),
    SETTINGS("mdi-settings"),
    HELP("mdi-help-circle"),
    ABOUT("mdi-information"),
    ZOOM_IN("mdi-magnify-plus"),
    ZOOM_OUT("mdi-magnify-minus"),
    ZOOM_FIT("mdi-arrow-expand-all"),
    COMMAND_PALETTE("mdi-console"),
    INSPECTOR("mdi-brush"),
    SIDEBAR_CONTENT("mdi-palette"),
    FORMAT("mdi-format-paint"),
    IMAGE("mdi-image"),
    TABLE("mdi-table"),
    FORMULA("mdi-function"),
    PAGE_BREAK("mdi-file-document"),
    BOLD("mdi-format-bold"),
    ITALIC("mdi-format-italic"),
    UNDERLINE("mdi-format-underline"),
    STRIKETHROUGH("mdi-format-strikethrough"),
    TEXT_COLOR("mdi-format-color-text"),
    ALIGN_LEFT("mdi-format-align-left"),
    ALIGN_CENTER("mdi-format-align-center"),
    ALIGN_RIGHT("mdi-format-align-right"),
    ALIGN_JUSTIFY("mdi-format-align-justify"),
    BULLETED_LIST("mdi-format-list-bulleted"),
    NUMBERED_LIST("mdi-format-list-numbers"),
    DECREASE_INDENT("mdi-format-indent-decrease"),
    INCREASE_INDENT("mdi-format-indent-increase"),
    LINE_SPACING("mdi-format-line-spacing"),
    RULER("mdi-ruler"),
    FUNCTION("mdi-function"),
    ROW("mdi-view-sequential"),
    COLUMN("mdi-view-column"),
    SHEET("mdi-file-table"),
    CHART("mdi-chart-bar"),
    SORT("mdi-sort-alphabetical"),
    FILTER("mdi-filter"),
    RECALCULATE("mdi-sync"),
    SLIDE("mdi-monitor"),
    TEXT_BOX("mdi-format-text"),
    SHAPE("mdi-shape"),
    THEME("mdi-tune"),
    PLAY("mdi-play"),
    DATABASE("mdi-database"),
    QUERY("mdi-magnify"),
    FORM("mdi-format-list-checks"),
    REPORT("mdi-file-chart"),
    RELATIONSHIPS("mdi-graphql"),
    SQL("mdi-console");

    private final String iconLiteral;

    DelosIconId(String iconLiteral) {
        this.iconLiteral = Objects.requireNonNull(iconLiteral, "iconLiteral");
    }

    public String iconLiteral() {
        return iconLiteral;
    }
}
