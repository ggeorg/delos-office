package io.github.ggeorg.delos.writer.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure planning helper that splits an edge run into normal and protruded pieces.
 */
public final class ProtrusionRenderPlanner {
    private final MarginProtrusionPolicy policy;

    public ProtrusionRenderPlanner(MarginProtrusionPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public List<Piece> plan(String text, boolean leadingEdgeRun, boolean trailingEdgeRun) {
        String safeText = Objects.requireNonNullElse(text, "");
        if (safeText.isEmpty()) {
            return List.of();
        }

        MarginProtrusionPolicy.EdgeCluster leading = leadingEdgeRun
                ? policy.leadingCluster(safeText)
                : MarginProtrusionPolicy.EdgeCluster.empty();
        MarginProtrusionPolicy.EdgeCluster trailing = trailingEdgeRun
                ? policy.trailingCluster(safeText)
                : MarginProtrusionPolicy.EdgeCluster.empty();

        if (!leading.isEmpty() && !trailing.isEmpty() && trailing.startInclusive() < leading.endExclusive()) {
            trailing = MarginProtrusionPolicy.EdgeCluster.empty();
        }

        List<Piece> pieces = new ArrayList<>();
        int cursor = 0;

        if (!leading.isEmpty()) {
            appendNormal(pieces, safeText.substring(cursor, leading.startInclusive()));
            for (int i = leading.startInclusive(); i < leading.endExclusive(); i++) {
                char ch = safeText.charAt(i);
                pieces.add(new Piece(String.valueOf(ch), -policy.leadingFraction(ch)));
            }
            cursor = leading.endExclusive();
        }

        int trailingStart = trailing.isEmpty() ? safeText.length() : trailing.startInclusive();
        appendNormal(pieces, safeText.substring(cursor, trailingStart));
        cursor = trailingStart;

        if (!trailing.isEmpty()) {
            for (int i = trailing.startInclusive(); i < trailing.endExclusive(); i++) {
                char ch = safeText.charAt(i);
                pieces.add(new Piece(String.valueOf(ch), policy.trailingFraction(ch)));
            }
            cursor = trailing.endExclusive();
        }

        appendNormal(pieces, safeText.substring(cursor));
        return pieces;
    }

    private static void appendNormal(List<Piece> pieces, String text) {
        if (!text.isEmpty()) {
            pieces.add(new Piece(text, 0.0));
        }
    }

    public record Piece(String text, double shiftFraction) {
        public Piece {
            text = Objects.requireNonNullElse(text, "");
        }

        public boolean protruded() {
            return shiftFraction != 0.0;
        }
    }
}
