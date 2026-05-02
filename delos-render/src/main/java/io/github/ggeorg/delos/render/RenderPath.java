package io.github.ggeorg.delos.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable platform-neutral vector path.
 *
 * <p>Coordinates use the same page-local coordinate system as {@link RenderTarget}:
 * origin at top-left and y increasing downward. Render targets translate this
 * into their native coordinate space.</p>
 */
public final class RenderPath {
    private final List<Command> commands;

    private RenderPath(List<Command> commands) {
        if (commands.isEmpty()) {
            throw new IllegalArgumentException("path must contain at least one command");
        }
        this.commands = List.copyOf(commands);
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Command> commands() {
        return commands;
    }

    public enum CommandType {
        MOVE_TO,
        LINE_TO,
        QUAD_TO,
        CUBIC_TO,
        CLOSE
    }

    public record Command(
            CommandType type,
            double x1,
            double y1,
            double x2,
            double y2,
            double x3,
            double y3
    ) {
        public Command {
            Objects.requireNonNull(type, "type");
        }

        public static Command moveTo(double x, double y) {
            return new Command(CommandType.MOVE_TO, x, y, 0.0, 0.0, 0.0, 0.0);
        }

        public static Command lineTo(double x, double y) {
            return new Command(CommandType.LINE_TO, x, y, 0.0, 0.0, 0.0, 0.0);
        }

        public static Command quadTo(double controlX, double controlY, double x, double y) {
            return new Command(CommandType.QUAD_TO, controlX, controlY, x, y, 0.0, 0.0);
        }

        public static Command cubicTo(double control1X, double control1Y, double control2X, double control2Y, double x, double y) {
            return new Command(CommandType.CUBIC_TO, control1X, control1Y, control2X, control2Y, x, y);
        }

        public static Command close() {
            return new Command(CommandType.CLOSE, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public static final class Builder {
        private final List<Command> commands = new ArrayList<>();

        private Builder() {
        }

        public Builder moveTo(double x, double y) {
            commands.add(Command.moveTo(x, y));
            return this;
        }

        public Builder lineTo(double x, double y) {
            commands.add(Command.lineTo(x, y));
            return this;
        }

        public Builder quadTo(double controlX, double controlY, double x, double y) {
            commands.add(Command.quadTo(controlX, controlY, x, y));
            return this;
        }

        public Builder cubicTo(double control1X, double control1Y, double control2X, double control2Y, double x, double y) {
            commands.add(Command.cubicTo(control1X, control1Y, control2X, control2Y, x, y));
            return this;
        }

        public Builder close() {
            commands.add(Command.close());
            return this;
        }

        public RenderPath build() {
            return new RenderPath(commands);
        }
    }
}
