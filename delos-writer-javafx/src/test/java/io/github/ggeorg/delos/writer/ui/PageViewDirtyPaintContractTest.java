package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.render.RenderTarget;
import io.github.ggeorg.delos.writer.contract.JavaFxTestSupport;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.render.PageRenderContext;
import io.github.ggeorg.delos.writer.render.PageRenderState;
import io.github.ggeorg.delos.writer.render.PageRenderer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PageViewDirtyPaintContractTest extends JavaFxTestSupport {
    @Test
    void coalescesPageAndRenderStateChangesUntilLayoutPass() {
        onFxThread(() -> {
            CountingPageRenderer renderer = new CountingPageRenderer();
            PageView view = new PageView(page(0), ViewTheme.defaultTheme(), renderer);

            view.resize(600.0, 800.0);
            view.layoutChildren();
            assertEquals(1, renderer.paintCount());

            view.setPage(page(1));
            view.setRenderState(new PageRenderState(null, null, true));
            assertEquals(1, renderer.paintCount());

            view.layoutChildren();
            assertEquals(2, renderer.paintCount());

            view.layoutChildren();
            assertEquals(2, renderer.paintCount());
        });
    }

    private static LaidOutPage page(int index) {
        return new LaidOutPage(index, 500.0, 700.0, List.of());
    }

    private static final class CountingPageRenderer implements PageRenderer {
        private final AtomicInteger paintCount = new AtomicInteger();

        @Override
        public void renderPage(RenderTarget target, PageRenderContext context) {
            paintCount.incrementAndGet();
        }

        int paintCount() {
            return paintCount.get();
        }
    }
}
