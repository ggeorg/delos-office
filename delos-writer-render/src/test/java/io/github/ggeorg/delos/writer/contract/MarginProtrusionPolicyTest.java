package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.render.MarginProtrusionPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarginProtrusionPolicyTest {
    private final MarginProtrusionPolicy policy = MarginProtrusionPolicy.DEFAULT;

    @Test
    void detectsLeadingOpeningPunctuation() {
        assertTrue(policy.hasLeadingProtrusion("\"Hello"));
        assertTrue(policy.hasLeadingProtrusion("(Hello"));
        assertFalse(policy.hasLeadingProtrusion("Hello"));
    }

    @Test
    void detectsTrailingClosingPunctuation() {
        assertTrue(policy.hasTrailingProtrusion("Hello."));
        assertTrue(policy.hasTrailingProtrusion("Hello!”"));
        assertTrue(policy.hasTrailingProtrusion("edge-"));
        assertFalse(policy.hasTrailingProtrusion("Hello"));
    }

    @Test
    void ignoresSurroundingWhitespaceWhenScanningEdges() {
        assertEquals(2, policy.firstVisibleIndex("  “Hello"));
        assertEquals(6, policy.lastVisibleIndex("Hello.”  "));
        assertTrue(policy.hasLeadingProtrusion("  “Hello"));
        assertTrue(policy.hasTrailingProtrusion("Hello.”  "));
    }

    @Test
    void detectsConsecutiveEdgePunctuationClusters() {
        MarginProtrusionPolicy.EdgeCluster leading = policy.leadingCluster("  “(Hello");
        MarginProtrusionPolicy.EdgeCluster trailing = policy.trailingCluster("Hello.)”  ");

        assertEquals(2, leading.startInclusive());
        assertEquals(4, leading.endExclusive());
        assertEquals(5, trailing.startInclusive());
        assertEquals(8, trailing.endExclusive());
    }

    @Test
    void usesConservativeFractions() {
        assertEquals(0.35, policy.leadingFraction('“'));
        assertEquals(0.30, policy.trailingFraction('.'));
        assertEquals(0.18, policy.trailingFraction(':'));
        assertEquals(0.0, policy.leadingFraction('A'));
        assertEquals(0.0, policy.trailingFraction('A'));
    }
}
