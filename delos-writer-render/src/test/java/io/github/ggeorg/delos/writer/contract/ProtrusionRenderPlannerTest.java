package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.render.MarginProtrusionPolicy;
import io.github.ggeorg.delos.writer.render.ProtrusionRenderPlanner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtrusionRenderPlannerTest {
    private final ProtrusionRenderPlanner planner = new ProtrusionRenderPlanner(MarginProtrusionPolicy.DEFAULT);

    @Test
    void plansWhitespacePaddedLeadingPunctuationWithinAnEdgeRun() {
        List<ProtrusionRenderPlanner.Piece> pieces = planner.plan("  “Hello", true, false);

        assertEquals(List.of("  ", "“", "Hello"), pieces.stream().map(ProtrusionRenderPlanner.Piece::text).toList());
        assertEquals(0.0, pieces.get(0).shiftFraction());
        assertTrue(pieces.get(1).shiftFraction() < 0);
        assertEquals(0.0, pieces.get(2).shiftFraction());
    }

    @Test
    void plansTrailingPunctuationClustersAtTheLineEdge() {
        List<ProtrusionRenderPlanner.Piece> pieces = planner.plan("Hello.)”  ", false, true);

        assertEquals(List.of("Hello", ".", ")", "”", "  "), pieces.stream().map(ProtrusionRenderPlanner.Piece::text).toList());
        assertTrue(pieces.get(1).shiftFraction() > 0);
        assertTrue(pieces.get(2).shiftFraction() > 0);
        assertTrue(pieces.get(3).shiftFraction() > 0);
        assertEquals(0.0, pieces.get(4).shiftFraction());
    }

    @Test
    void leavesInteriorPunctuationUntouchedForNonEdgeRuns() {
        List<ProtrusionRenderPlanner.Piece> pieces = planner.plan("  “Hello.”  ", false, false);

        assertEquals(1, pieces.size());
        assertEquals("  “Hello.”  ", pieces.get(0).text());
        assertFalse(pieces.get(0).protruded());
    }
}
