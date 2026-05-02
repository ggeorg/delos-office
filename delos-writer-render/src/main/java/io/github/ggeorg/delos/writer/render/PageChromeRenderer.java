package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderTarget;
import io.github.ggeorg.delos.render.RenderTheme;

/**
 * Paints the non-document chrome for a page: workspace background,
 * page shadow, page fill, and page border.
 */
final class PageChromeRenderer {
    void paintWorkspace(RenderTarget target, RenderTheme theme, double width, double height) {
        target.clearRect(0, 0, width, height);
        target.setFill(theme.workspaceBackground());
        target.fillRect(0, 0, width, height);
    }

    void paintPageShell(RenderTarget target, RenderTheme theme, double x, double y, double width, double height) {
        target.setFill(theme.pageShadow());
        target.fillRoundRect(x + theme.pageShadowOffsetX(), y + theme.pageShadowOffsetY(), width, height,
                theme.pageCornerRadius(), theme.pageCornerRadius());

        target.setFill(theme.pageBackground());
        target.fillRoundRect(x, y, width, height, theme.pageCornerRadius(), theme.pageCornerRadius());

        target.setStroke(theme.pageBorder());
        target.strokeRoundRect(x, y, width, height, theme.pageCornerRadius(), theme.pageCornerRadius());
    }
}
