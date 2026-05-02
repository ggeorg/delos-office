package io.github.ggeorg.delos.writer.ui.control;

/**
 * Presentation mode for the writer document workspace.
 *
 * <p>Only {@link #CONTINUOUS} is implemented in the first control refactor.
 * The remaining modes name future layout policies without pretending they are
 * complete.</p>
 */
public enum PageViewMode {
    CONTINUOUS,
    SINGLE_PAGE,
    TWO_PAGE,
    BOOK
}
