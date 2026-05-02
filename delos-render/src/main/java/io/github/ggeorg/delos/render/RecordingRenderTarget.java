package io.github.ggeorg.delos.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Test render target that records drawing calls without JavaFX.
 */
public final class RecordingRenderTarget implements RenderTarget {
    private final List<DrawCall> calls = new ArrayList<>();
    private final Deque<RenderState> savedStates = new ArrayDeque<>();
    private RenderColor fill = RenderColor.rgb(0, 0, 0);
    private RenderColor stroke = RenderColor.rgb(0, 0, 0);
    private RenderFont font = new RenderFont("System", 12.0, false, false);
    private double lineWidth = 1.0;
    private double globalAlpha = 1.0;

    public List<DrawCall> calls() {
        return List.copyOf(calls);
    }

    public boolean containsText(String text) {
        return calls.stream().anyMatch(call -> call.kind() == DrawKind.FILL_TEXT && Objects.equals(call.text(), text));
    }

    public boolean containsTextAt(String text, double x, double y, double tolerance) {
        return calls.stream().anyMatch(call ->
                call.kind() == DrawKind.FILL_TEXT
                        && Objects.equals(call.text(), text)
                        && Math.abs(call.x() - x) <= tolerance
                        && Math.abs(call.y() - y) <= tolerance
        );
    }

    public long count(DrawKind kind) {
        return calls.stream().filter(call -> call.kind() == kind).count();
    }

    public List<DrawCall> callsOf(DrawKind kind) {
        return calls.stream().filter(call -> call.kind() == kind).toList();
    }

    @Override
    public void save() {
        savedStates.push(currentState());
        calls.add(DrawCall.state(DrawKind.SAVE, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void restore() {
        if (!savedStates.isEmpty()) {
            applyState(savedStates.pop());
        }
        calls.add(DrawCall.state(DrawKind.RESTORE, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void translate(double x, double y) {
        calls.add(DrawCall.point(DrawKind.TRANSLATE, x, y, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void clip(double x, double y, double width, double height) {
        calls.add(DrawCall.rect(DrawKind.CLIP, x, y, width, height, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void setGlobalAlpha(double alpha) {
        globalAlpha = clampUnit(alpha);
        calls.add(DrawCall.state(DrawKind.SET_GLOBAL_ALPHA, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void clearRect(double x, double y, double width, double height) {
        calls.add(DrawCall.rect(DrawKind.CLEAR_RECT, x, y, width, height, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void setFill(RenderColor color) {
        fill = Objects.requireNonNull(color, "color");
        calls.add(DrawCall.state(DrawKind.SET_FILL, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void setStroke(RenderColor color) {
        stroke = Objects.requireNonNull(color, "color");
        calls.add(DrawCall.state(DrawKind.SET_STROKE, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
        calls.add(DrawCall.state(DrawKind.SET_LINE_WIDTH, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void setFont(RenderFont font) {
        this.font = Objects.requireNonNull(font, "font");
        calls.add(DrawCall.state(DrawKind.SET_FONT, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void fillRect(double x, double y, double width, double height) {
        calls.add(DrawCall.rect(DrawKind.FILL_RECT, x, y, width, height, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void strokeRect(double x, double y, double width, double height) {
        calls.add(DrawCall.rect(DrawKind.STROKE_RECT, x, y, width, height, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void fillRoundRect(double x, double y, double width, double height, double arcWidth, double arcHeight) {
        calls.add(DrawCall.roundRect(DrawKind.FILL_ROUND_RECT, x, y, width, height, arcWidth, arcHeight, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void strokeRoundRect(double x, double y, double width, double height, double arcWidth, double arcHeight) {
        calls.add(DrawCall.roundRect(DrawKind.STROKE_ROUND_RECT, x, y, width, height, arcWidth, arcHeight, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void strokeLine(double startX, double startY, double endX, double endY) {
        calls.add(DrawCall.line(DrawKind.STROKE_LINE, startX, startY, endX, endY, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void fillText(String text, double x, double y) {
        calls.add(DrawCall.text(DrawKind.FILL_TEXT, text, x, y, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void fillPath(RenderPath path) {
        calls.add(DrawCall.path(DrawKind.FILL_PATH, path, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public void strokePath(RenderPath path) {
        calls.add(DrawCall.path(DrawKind.STROKE_PATH, path, fill, stroke, font, lineWidth, globalAlpha));
    }

    @Override
    public boolean drawImage(RenderImage image, double x, double y, double width, double height) {
        calls.add(DrawCall.image(image, x, y, width, height, fill, stroke, font, lineWidth, globalAlpha));
        return true;
    }

    private RenderState currentState() {
        return new RenderState(fill, stroke, font, lineWidth, globalAlpha);
    }

    private void applyState(RenderState state) {
        fill = state.fill();
        stroke = state.stroke();
        font = state.font();
        lineWidth = state.lineWidth();
        globalAlpha = state.globalAlpha();
    }

    private static double clampUnit(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    public enum DrawKind {
        SAVE,
        RESTORE,
        TRANSLATE,
        CLIP,
        SET_GLOBAL_ALPHA,
        CLEAR_RECT,
        SET_FILL,
        SET_STROKE,
        SET_LINE_WIDTH,
        SET_FONT,
        FILL_RECT,
        STROKE_RECT,
        FILL_ROUND_RECT,
        STROKE_ROUND_RECT,
        STROKE_LINE,
        FILL_TEXT,
        FILL_PATH,
        STROKE_PATH,
        DRAW_IMAGE
    }

    private record RenderState(
            RenderColor fill,
            RenderColor stroke,
            RenderFont font,
            double lineWidth,
            double globalAlpha
    ) { }

    public record DrawCall(
            DrawKind kind,
            String text,
            RenderImage image,
            RenderPath path,
            double x,
            double y,
            double width,
            double height,
            double endX,
            double endY,
            double arcWidth,
            double arcHeight,
            RenderColor fill,
            RenderColor stroke,
            RenderFont font,
            double lineWidth,
            double globalAlpha
    ) {
        static DrawCall state(DrawKind kind, RenderColor fill, RenderColor stroke, RenderFont font, double lineWidth, double globalAlpha) {
            return new DrawCall(kind, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, fill, stroke, font, lineWidth, globalAlpha);
        }

        static DrawCall point(DrawKind kind, double x, double y,
                              RenderColor fill, RenderColor stroke, RenderFont font, double lineWidth, double globalAlpha) {
            return new DrawCall(kind, null, null, null, x, y, 0, 0, 0, 0, 0, 0, fill, stroke, font, lineWidth, globalAlpha);
        }

        static DrawCall rect(DrawKind kind, double x, double y, double width, double height,
                             RenderColor fill, RenderColor stroke, RenderFont font, double lineWidth, double globalAlpha) {
            return new DrawCall(kind, null, null, null, x, y, width, height, 0, 0, 0, 0, fill, stroke, font, lineWidth, globalAlpha);
        }

        static DrawCall roundRect(DrawKind kind, double x, double y, double width, double height,
                                  double arcWidth, double arcHeight,
                                  RenderColor fill, RenderColor stroke, RenderFont font, double lineWidth, double globalAlpha) {
            return new DrawCall(kind, null, null, null, x, y, width, height, 0, 0, arcWidth, arcHeight, fill, stroke, font, lineWidth, globalAlpha);
        }

        static DrawCall line(DrawKind kind, double startX, double startY, double endX, double endY,
                             RenderColor fill, RenderColor stroke, RenderFont font, double lineWidth, double globalAlpha) {
            return new DrawCall(kind, null, null, null, startX, startY, 0, 0, endX, endY, 0, 0, fill, stroke, font, lineWidth, globalAlpha);
        }

        static DrawCall text(DrawKind kind, String text, double x, double y,
                             RenderColor fill, RenderColor stroke, RenderFont font, double lineWidth, double globalAlpha) {
            return new DrawCall(kind, text, null, null, x, y, 0, 0, 0, 0, 0, 0, fill, stroke, font, lineWidth, globalAlpha);
        }

        static DrawCall path(DrawKind kind, RenderPath path,
                             RenderColor fill, RenderColor stroke, RenderFont font, double lineWidth, double globalAlpha) {
            return new DrawCall(kind, null, null, Objects.requireNonNull(path, "path"), 0, 0, 0, 0, 0, 0, 0, 0, fill, stroke, font, lineWidth, globalAlpha);
        }

        static DrawCall image(RenderImage image, double x, double y, double width, double height,
                              RenderColor fill, RenderColor stroke, RenderFont font, double lineWidth, double globalAlpha) {
            return new DrawCall(DrawKind.DRAW_IMAGE, null, image, null, x, y, width, height, 0, 0, 0, 0, fill, stroke, font, lineWidth, globalAlpha);
        }
    }
}
