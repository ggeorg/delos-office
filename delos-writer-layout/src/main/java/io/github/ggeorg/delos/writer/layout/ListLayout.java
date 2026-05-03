package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.PageStyle;

record ListLayout(boolean enabled, String markerText, double blockX, double contentWidth, double markerX) {
    static ListLayout none(PageStyle pageStyle) {
        return new ListLayout(false, "", pageStyle.marginLeft(), pageStyle.contentWidth(), 0);
    }
}
