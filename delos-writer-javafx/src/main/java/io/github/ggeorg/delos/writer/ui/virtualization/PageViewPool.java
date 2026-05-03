package io.github.ggeorg.delos.writer.ui.virtualization;

import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.render.PageRenderer;
import io.github.ggeorg.delos.writer.ui.PageView;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.writer.render.fx.JavaFxRenderTextMeasurer;
import io.github.ggeorg.delos.writer.ui.ViewTheme;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Recycles page nodes as the visible page window moves. */
public final class PageViewPool {
    private final ViewTheme theme;
    private final PageRenderer pageRenderer;
    private final RenderTextMeasurer renderTextMeasurer;
    private final Deque<PageView> recycledViews = new ArrayDeque<>();

    public PageViewPool(ViewTheme theme, PageRenderer pageRenderer) {
        this(theme, pageRenderer, new JavaFxRenderTextMeasurer());
    }

    public PageViewPool(ViewTheme theme, PageRenderer pageRenderer, RenderTextMeasurer renderTextMeasurer) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.pageRenderer = Objects.requireNonNull(pageRenderer, "pageRenderer");
        this.renderTextMeasurer = renderTextMeasurer == null ? new JavaFxRenderTextMeasurer() : renderTextMeasurer;
    }

    public PageView acquire(LaidOutPage page) {
        PageView view = recycledViews.pollFirst();
        if (view == null) {
            return new PageView(page, theme, pageRenderer, renderTextMeasurer);
        }
        view.setPage(page);
        return view;
    }

    public void release(PageView view) {
        if (view != null) {
            recycledViews.addLast(view);
        }
    }

    int pooledCount() {
        return recycledViews.size();
    }
}
