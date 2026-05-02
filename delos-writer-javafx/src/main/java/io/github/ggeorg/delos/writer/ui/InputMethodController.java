package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.editor.EditorInteractionModel;
import io.github.ggeorg.delos.writer.layout.HitTestResult;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.InputMethodTextRun;

import java.text.Normalizer;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Handles JavaFX input-method events for pre-edit/composition text.
 *
 * <p>The important RichTextFX lesson is that JavaFX input composition expects a
 * real text range to exist in the editable control. Earlier Delos H35 tried to
 * keep composed text only as an overlay. That is elegant, but it breaks some
 * platform keyboards/dead-key flows, including Greek tonos on macOS. This
 * controller therefore follows the RichTextFX shape: keep track of the current
 * composed range, replace that range on each input-method event, and only then
 * place the caret inside the composed range when requested by the platform.</p>
 */
public final class InputMethodController {
    private final EditorInteractionModel interactionModel;
    private final DocumentViewportEditController editController;
    private final BiFunction<Double, Double, HitTestResult> hitTestFinder;
    private final Supplier<Bounds> caretBoundsInContentSupplier;
    private final Node coordinateNode;

    /** Start of the currently inserted composition/pre-edit text. */
    private TextPosition imStart;

    /** UTF-16 length of the currently inserted composition/pre-edit text. */
    private int imLength;

    public InputMethodController(
            EditorInteractionModel interactionModel,
            DocumentViewportEditController editController,
            BiFunction<Double, Double, HitTestResult> hitTestFinder,
            Supplier<Bounds> caretBoundsInContentSupplier,
            Node coordinateNode
    ) {
        this.interactionModel = Objects.requireNonNull(interactionModel, "interactionModel");
        this.editController = Objects.requireNonNull(editController, "editController");
        this.hitTestFinder = Objects.requireNonNull(hitTestFinder, "hitTestFinder");
        this.caretBoundsInContentSupplier = Objects.requireNonNull(caretBoundsInContentSupplier, "caretBoundsInContentSupplier");
        this.coordinateNode = Objects.requireNonNull(coordinateNode, "coordinateNode");
    }

    public void handleInputMethodTextChanged(InputMethodEvent event) {
        Objects.requireNonNull(event, "event");

        String committed = sanitize(event.getCommitted());
        String composed = composedText(event.getComposed());

        TextPosition replacementStart = prepareReplacementStart();
        if (replacementStart == null) {
            event.consume();
            return;
        }

        String replacement = committed + composed;
        if (!replacement.isEmpty() || imLength != 0 || hasSelection()) {
            editController.replaceSelection(replacement, composed.isEmpty() ? "IME Commit Text" : "IME Compose Text");
        }

        TextPosition composedStart = advance(replacementStart, committed);
        imStart = composed.isEmpty() ? null : composedStart;
        imLength = composed.length();

        if (imLength != 0) {
            int caretInComposition = Math.max(0, Math.min(event.getCaretPosition(), imLength));
            interactionModel.setCaret(advance(composedStart, composed.substring(0, caretInComposition)));
        } else {
            imStart = null;
        }

        event.consume();
    }

    public boolean isComposing() {
        return imStart != null && imLength > 0;
    }

    public InputMethodRequests requests() {
        return new Requests();
    }

    public void clearCompositionRange() {
        imStart = null;
        imLength = 0;
    }

    private TextPosition prepareReplacementStart() {
        if (isComposing()) {
            TextPosition end = advance(imStart, imLength);
            interactionModel.setSelection(imStart, end);
            return imStart;
        }

        SelectionRange selection = interactionModel.selectionRange();
        if (selection != null && !selection.isCollapsed()) {
            return selection.start();
        }
        return interactionModel.caretPosition();
    }

    private boolean hasSelection() {
        SelectionRange selection = interactionModel.selectionRange();
        return selection != null && !selection.isCollapsed();
    }

    private TextPosition advance(TextPosition start, int utf16Length) {
        return new TextPosition(start.paragraphIndex(), start.offset() + Math.max(0, utf16Length));
    }

    private TextPosition advance(TextPosition start, String insertedText) {
        if (insertedText == null || insertedText.isEmpty()) {
            return start;
        }

        String normalized = insertedText.replace("\r\n", "\n").replace('\r', '\n');
        int paragraph = start.paragraphIndex();
        int offset = start.offset();
        int lineStart = 0;
        for (int i = 0; i < normalized.length(); i++) {
            if (normalized.charAt(i) == '\n') {
                paragraph++;
                lineStart = i + 1;
                offset = 0;
            }
        }
        if (lineStart == 0) {
            return new TextPosition(paragraph, offset + normalized.length());
        }
        return new TextPosition(paragraph, normalized.length() - lineStart);
    }

    private String composedText(List<InputMethodTextRun> runs) {
        if (runs == null || runs.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (InputMethodTextRun run : runs) {
            if (run != null && run.getText() != null) {
                out.append(run.getText());
            }
        }
        return sanitize(out.toString());
    }

    private String selectedText() {
        return editController.selectedText();
    }

    private String sanitize(String text) {
        return text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    private final class Requests implements InputMethodRequests {
        @Override
        public Point2D getTextLocation(int offset) {
            Bounds caretBounds = caretBoundsInContentSupplier.get();
            if (caretBounds == null) {
                Point2D origin = coordinateNode.localToScreen(0.0, 0.0);
                return origin == null ? new Point2D(0.0, 0.0) : origin;
            }
            double x = caretBounds.getMaxX() - 5.0;
            double y = caretBounds.getMinY() + caretBounds.getHeight();
            Point2D screen = coordinateNode.localToScreen(x, y);
            return screen == null ? new Point2D(0.0, 0.0) : screen;
        }

        @Override
        public int getLocationOffset(int x, int y) {
            Point2D local = coordinateNode.screenToLocal(x, y);
            if (local == null) {
                return 0;
            }
            Point2D scene = coordinateNode.localToScene(local);
            if (scene == null) {
                return 0;
            }
            HitTestResult hit = hitTestFinder.apply(scene.getX(), scene.getY());
            if (hit == null || hit.position() == null || interactionModel.caretPosition() == null) {
                return 0;
            }
            return Math.max(0, hit.position().offset() - interactionModel.caretPosition().offset());
        }

        @Override
        public void cancelLatestCommittedText() {
            editController.undo();
        }

        @Override
        public String getSelectedText() {
            return selectedText();
        }
    }
}
