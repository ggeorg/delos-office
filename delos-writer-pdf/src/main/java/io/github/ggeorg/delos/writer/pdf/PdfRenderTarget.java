package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderColor;
import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderImage;
import io.github.ggeorg.delos.render.RenderPath;
import io.github.ggeorg.delos.render.RenderTarget;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * PDFBox {@link PDPageContentStream} adapter for the Delos RenderTarget API.
 *
 * <p>Coordinates are accepted in Delos page-local units: origin at top-left,
 * y increasing downward, values in points. They are converted to PDF's
 * bottom-left coordinate system at the adapter boundary.</p>
 */
public final class PdfRenderTarget implements RenderTarget, AutoCloseable {
    private final PDDocument document;
    private final PDPageContentStream content;
    private final double pageHeight;
    private final PdfFontResolver fonts;
    private final Deque<State> states = new ArrayDeque<>();
    private State state = State.initial();
    private boolean textOpen;

    public PdfRenderTarget(PDDocument document, PDPageContentStream content, double pageHeight, PdfFontResolver fonts) {
        this.document = Objects.requireNonNull(document, "document");
        this.content = Objects.requireNonNull(content, "content");
        this.pageHeight = pageHeight;
        this.fonts = Objects.requireNonNull(fonts, "fonts");
    }

    @Override
    public void save() {
        io(() -> {
            closeOpenTextObject();
            states.push(state);
            content.saveGraphicsState();
        });
    }

    @Override
    public void restore() {
        io(() -> {
            closeOpenTextObject();
            content.restoreGraphicsState();
            if (!states.isEmpty()) {
                state = states.pop();
            }
        });
    }

    @Override
    public void translate(double x, double y) {
        state = state.translate(x, y);
    }

    @Override
    public void clip(double x, double y, double width, double height) {
        io(() -> {
            closeOpenTextObject();
            Rect rect = rect(x, y, width, height);
            content.addRect((float) rect.x(), (float) rect.y(), (float) rect.width(), (float) rect.height());
            content.clip();
        });
    }

    @Override
    public void setGlobalAlpha(double alpha) {
        state = state.withAlpha(clampUnit(alpha));
        io(() -> {
            closeOpenTextObject();
            PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
            graphicsState.setNonStrokingAlphaConstant((float) state.globalAlpha());
            graphicsState.setStrokingAlphaConstant((float) state.globalAlpha());
            content.setGraphicsStateParameters(graphicsState);
        });
    }

    @Override
    public void clearRect(double x, double y, double width, double height) {
        // PDF content streams are append-only. Final PDF export does not use
        // clear operations; a white fill can be added by the caller if needed.
    }

    @Override
    public void setFill(RenderColor color) {
        state = state.withFill(Objects.requireNonNull(color, "color"));
        io(() -> {
            closeOpenTextObject();
            content.setNonStrokingColor(
                    (float) state.fill().red(),
                    (float) state.fill().green(),
                    (float) state.fill().blue()
            );
        });
    }

    @Override
    public void setStroke(RenderColor color) {
        state = state.withStroke(Objects.requireNonNull(color, "color"));
        io(() -> {
            closeOpenTextObject();
            content.setStrokingColor(
                    (float) state.stroke().red(),
                    (float) state.stroke().green(),
                    (float) state.stroke().blue()
            );
        });
    }

    @Override
    public void setLineWidth(double lineWidth) {
        double safeLineWidth = Double.isFinite(lineWidth) && lineWidth > 0.0 ? lineWidth : 1.0;
        state = state.withLineWidth(safeLineWidth);
        io(() -> {
            closeOpenTextObject();
            content.setLineWidth((float) safeLineWidth);
        });
    }

    @Override
    public void setFont(RenderFont font) {
        state = state.withFont(fonts.resolve(Objects.requireNonNull(font, "font")));
    }

    @Override
    public void fillRect(double x, double y, double width, double height) {
        io(() -> {
            closeOpenTextObject();
            Rect rect = rect(x, y, width, height);
            content.addRect((float) rect.x(), (float) rect.y(), (float) rect.width(), (float) rect.height());
            content.fill();
        });
    }

    @Override
    public void strokeRect(double x, double y, double width, double height) {
        io(() -> {
            closeOpenTextObject();
            Rect rect = rect(x, y, width, height);
            content.addRect((float) rect.x(), (float) rect.y(), (float) rect.width(), (float) rect.height());
            content.stroke();
        });
    }

    @Override
    public void fillRoundRect(double x, double y, double width, double height, double arcWidth, double arcHeight) {
        fillRect(x, y, width, height);
    }

    @Override
    public void strokeRoundRect(double x, double y, double width, double height, double arcWidth, double arcHeight) {
        strokeRect(x, y, width, height);
    }

    @Override
    public void strokeLine(double startX, double startY, double endX, double endY) {
        io(() -> {
            closeOpenTextObject();
            Point start = point(startX, startY);
            Point end = point(endX, endY);
            content.moveTo((float) start.x(), (float) start.y());
            content.lineTo((float) end.x(), (float) end.y());
            content.stroke();
        });
    }

    @Override
    public void fillText(String text, double x, double y) {
        String safeText = PdfTextSanitizer.sanitize(text);
        if (safeText.isEmpty()) {
            return;
        }
        io(() -> {
            Point point = point(x, y);
            closeOpenTextObject();
            try {
                content.beginText();
                textOpen = true;
                content.setFont(fonts.fontFor(state.font(), safeText), (float) state.font().size());
                content.newLineAtOffset((float) point.x(), (float) point.y());
                content.showText(safeText);
            } finally {
                closeOpenTextObject();
            }
        });
    }

    @Override
    public void fillPath(RenderPath path) {
        io(() -> {
            closeOpenTextObject();
            applyPath(path);
            content.fill();
        });
    }

    @Override
    public void strokePath(RenderPath path) {
        io(() -> {
            closeOpenTextObject();
            applyPath(path);
            content.stroke();
        });
    }

    @Override
    public boolean drawImage(RenderImage image, double x, double y, double width, double height) {
        Objects.requireNonNull(image, "image");
        if (!image.isRasterImage() || width <= 0.0 || height <= 0.0) {
            return false;
        }
        try {
            closeOpenTextObject();
            PDImageXObject pdfImage = PDImageXObject.createFromByteArray(document, image.bytes(), image.source());
            Rect rect = rect(x, y, width, height);
            content.drawImage(pdfImage, (float) rect.x(), (float) rect.y(), (float) rect.width(), (float) rect.height());
            return true;
        } catch (IOException | RuntimeException ex) {
            return false;
        }
    }

    private void applyPath(RenderPath path) throws IOException {
        Objects.requireNonNull(path, "path");
        Point current = null;
        Point subpathStart = null;
        for (RenderPath.Command command : path.commands()) {
            switch (command.type()) {
                case MOVE_TO -> {
                    Point point = point(command.x1(), command.y1());
                    content.moveTo((float) point.x(), (float) point.y());
                    current = point;
                    subpathStart = point;
                }
                case LINE_TO -> {
                    Point point = point(command.x1(), command.y1());
                    content.lineTo((float) point.x(), (float) point.y());
                    current = point;
                }
                case QUAD_TO -> {
                    if (current == null) {
                        throw new IOException("Quadratic path command requires a current point");
                    }
                    Point control = point(command.x1(), command.y1());
                    Point end = point(command.x2(), command.y2());
                    Point c1 = new Point(
                            current.x() + (2.0 / 3.0) * (control.x() - current.x()),
                            current.y() + (2.0 / 3.0) * (control.y() - current.y())
                    );
                    Point c2 = new Point(
                            end.x() + (2.0 / 3.0) * (control.x() - end.x()),
                            end.y() + (2.0 / 3.0) * (control.y() - end.y())
                    );
                    content.curveTo(
                            (float) c1.x(), (float) c1.y(),
                            (float) c2.x(), (float) c2.y(),
                            (float) end.x(), (float) end.y()
                    );
                    current = end;
                }
                case CUBIC_TO -> {
                    Point c1 = point(command.x1(), command.y1());
                    Point c2 = point(command.x2(), command.y2());
                    Point end = point(command.x3(), command.y3());
                    content.curveTo(
                            (float) c1.x(), (float) c1.y(),
                            (float) c2.x(), (float) c2.y(),
                            (float) end.x(), (float) end.y()
                    );
                    current = end;
                }
                case CLOSE -> {
                    content.closePath();
                    current = subpathStart;
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        closeOpenTextObject();
        content.close();
    }

    private void closeOpenTextObject() throws IOException {
        if (textOpen) {
            content.endText();
            textOpen = false;
        }
    }

    private Point point(double x, double y) {
        double px = state.translateX() + x;
        double py = pageHeight - (state.translateY() + y);
        return new Point(px, py);
    }

    private Rect rect(double x, double y, double width, double height) {
        double px = state.translateX() + x;
        double py = pageHeight - (state.translateY() + y + height);
        return new Rect(px, py, width, height);
    }

    private static double clampUnit(double value) {
        if (!Double.isFinite(value)) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static void io(IoRunnable runnable) {
        try {
            runnable.run();
        } catch (IOException ex) {
            throw new PdfRenderException(ex);
        }
    }

    @FunctionalInterface
    private interface IoRunnable {
        void run() throws IOException;
    }

    private record Point(double x, double y) { }

    private record Rect(double x, double y, double width, double height) { }

    private record State(
            RenderColor fill,
            RenderColor stroke,
            RenderFont font,
            double lineWidth,
            double globalAlpha,
            double translateX,
            double translateY
    ) {
        static State initial() {
            return new State(
                    RenderColor.rgb(0, 0, 0),
                    RenderColor.rgb(0, 0, 0),
                    new RenderFont("Helvetica", 12.0, false, false),
                    1.0,
                    1.0,
                    0.0,
                    0.0
            );
        }

        State withFill(RenderColor value) {
            return new State(value, stroke, font, lineWidth, globalAlpha, translateX, translateY);
        }

        State withStroke(RenderColor value) {
            return new State(fill, value, font, lineWidth, globalAlpha, translateX, translateY);
        }

        State withFont(RenderFont value) {
            return new State(fill, stroke, value, lineWidth, globalAlpha, translateX, translateY);
        }

        State withLineWidth(double value) {
            return new State(fill, stroke, font, value, globalAlpha, translateX, translateY);
        }

        State withAlpha(double value) {
            return new State(fill, stroke, font, lineWidth, value, translateX, translateY);
        }

        State translate(double x, double y) {
            return new State(fill, stroke, font, lineWidth, globalAlpha, translateX + x, translateY + y);
        }
    }
}
