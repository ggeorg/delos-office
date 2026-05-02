package io.github.ggeorg.delos.slides.core;

/**
 * Base type for immutable elements placed on a slide.
 */
public sealed interface SlideElement permits TextBoxElement {
    String id();
    double x();
    double y();
    double width();
    double height();
}
