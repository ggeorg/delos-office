package io.github.ggeorg.delos.writer.render.fx;

import io.github.ggeorg.delos.render.RenderColor;
import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderImage;
import io.github.ggeorg.delos.render.RenderPath;
import io.github.ggeorg.delos.render.RenderTarget;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import java.io.ByteArrayInputStream;
import java.util.Objects;

/**
 * JavaFX Canvas adapter for the platform-neutral page renderer.
 */
public final class JavaFxRenderTarget implements RenderTarget {
    private final GraphicsContext graphics;

    public JavaFxRenderTarget(GraphicsContext graphics) {
        this.graphics = Objects.requireNonNull(graphics, "graphics");
    }

    @Override
    public void save() {
        graphics.save();
    }

    @Override
    public void restore() {
        graphics.restore();
    }

    @Override
    public void translate(double x, double y) {
        graphics.translate(x, y);
    }

    @Override
    public void clip(double x, double y, double width, double height) {
        graphics.beginPath();
        graphics.rect(x, y, width, height);
        graphics.clip();
    }

    @Override
    public void setGlobalAlpha(double alpha) {
        graphics.setGlobalAlpha(clampUnit(alpha));
    }

    @Override
    public void clearRect(double x, double y, double width, double height) {
        graphics.clearRect(x, y, width, height);
    }

    @Override
    public void setFill(RenderColor color) {
        graphics.setFill(toFxColor(color));
    }

    @Override
    public void setStroke(RenderColor color) {
        graphics.setStroke(toFxColor(color));
    }

    @Override
    public void setLineWidth(double lineWidth) {
        graphics.setLineWidth(lineWidth);
    }

    @Override
    public void setFont(RenderFont font) {
        graphics.setFont(toFxFont(font));
    }

    @Override
    public void fillRect(double x, double y, double width, double height) {
        graphics.fillRect(x, y, width, height);
    }

    @Override
    public void strokeRect(double x, double y, double width, double height) {
        graphics.strokeRect(x, y, width, height);
    }

    @Override
    public void fillRoundRect(double x, double y, double width, double height, double arcWidth, double arcHeight) {
        graphics.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void strokeRoundRect(double x, double y, double width, double height, double arcWidth, double arcHeight) {
        graphics.strokeRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void strokeLine(double startX, double startY, double endX, double endY) {
        graphics.strokeLine(startX, startY, endX, endY);
    }

    @Override
    public void fillText(String text, double x, double y) {
        graphics.fillText(text, x, y);
    }

    @Override
    public void fillPath(RenderPath path) {
        applyPath(path);
        graphics.fill();
    }

    @Override
    public void strokePath(RenderPath path) {
        applyPath(path);
        graphics.stroke();
    }

    @Override
    public boolean drawImage(RenderImage image, double x, double y, double width, double height) {
        Objects.requireNonNull(image, "image");
        try {
            Image fxImage = new Image(new ByteArrayInputStream(image.bytes()));
            if (fxImage.isError()) {
                return false;
            }
            graphics.drawImage(fxImage, x, y, width, height);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void applyPath(RenderPath path) {
        Objects.requireNonNull(path, "path");
        graphics.beginPath();
        for (RenderPath.Command command : path.commands()) {
            switch (command.type()) {
                case MOVE_TO -> graphics.moveTo(command.x1(), command.y1());
                case LINE_TO -> graphics.lineTo(command.x1(), command.y1());
                case QUAD_TO -> graphics.quadraticCurveTo(command.x1(), command.y1(), command.x2(), command.y2());
                case CUBIC_TO -> graphics.bezierCurveTo(command.x1(), command.y1(), command.x2(), command.y2(), command.x3(), command.y3());
                case CLOSE -> graphics.closePath();
            }
        }
    }

    public static Color toFxColor(RenderColor color) {
        Objects.requireNonNull(color, "color");
        return new Color(color.red(), color.green(), color.blue(), color.alpha());
    }

    public static Font toFxFont(RenderFont font) {
        Objects.requireNonNull(font, "font");
        return Font.font(
                font.family(),
                font.bold() ? FontWeight.BOLD : FontWeight.NORMAL,
                font.italic() ? FontPosture.ITALIC : FontPosture.REGULAR,
                font.size()
        );
    }

    private static double clampUnit(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
