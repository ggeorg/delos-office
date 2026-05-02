package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.layout.KnuthPlassLineBreaker;
import io.github.ggeorg.delos.writer.layout.KnuthPlassTypes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnuthPlassLineBreakerTest {
    private final KnuthPlassLineBreaker breaker = new KnuthPlassLineBreaker();

    @Test
    void choosesBalancedBreakpointsOverPoorEarlyBreaks() {
        List<KnuthPlassTypes.Item> items = List.of(
                box("alpha", 40, 0),
                glue(10, 5, 3, 5),
                box("beta", 20, 6),
                glue(10, 5, 3, 10),
                box("gamma", 20, 11),
                glue(10, 5, 3, 16),
                box("delta", 40, 17),
                forcedEnd(22)
        );

        assertIterableEquals(List.of(3, 7), breaker.computeBreakpointIndices(items, 70));
    }

    @Test
    void honorsForcedBreakPenaltiesAsMandatoryLineEnds() {
        List<KnuthPlassTypes.Item> items = List.of(
                box("alpha", 50, 0),
                forcedEnd(5),
                box("beta", 50, 6),
                forcedEnd(10)
        );

        assertIterableEquals(List.of(1, 3), breaker.computeBreakpointIndices(items, 500));
    }

    @Test
    void positivePenaltyDiscouragesAnOtherwiseLegalEarlyBreak() {
        List<KnuthPlassTypes.Item> items = List.of(
                box("alpha", 30, 0),
                new KnuthPlassTypes.Penalty("", 5, 0, 500, false),
                box("beta", 30, 6),
                glue(10, 5, 3, 10),
                box("gamma", 30, 11),
                forcedEnd(16)
        );

        assertIterableEquals(List.of(3, 5), breaker.computeBreakpointIndices(items, 70));
    }

    @Test
    void returnedBreakpointsCarryStableLineNumbersAndFiniteDemerits() {
        List<KnuthPlassTypes.Item> items = List.of(
                box("one", 35, 0),
                glue(10, 4, 2, 3),
                box("two", 35, 4),
                glue(10, 4, 2, 7),
                box("three", 35, 8),
                forcedEnd(13)
        );

        List<KnuthPlassTypes.Breakpoint> breakpoints = breaker.computeBreakpoints(items, 80);

        assertEquals(2, breakpoints.size());
        assertEquals(1, breakpoints.get(0).lineNumber());
        assertEquals(2, breakpoints.get(1).lineNumber());
        assertTrue(Double.isFinite(breakpoints.get(0).demerits()));
        assertTrue(Double.isFinite(breakpoints.get(1).demerits()));
    }


    @Test
    void consecutiveFlaggedBreaksAccrueExtraDemerits() {
        List<KnuthPlassTypes.Item> items = List.of(
                box("alpha", 20, 0),
                new KnuthPlassTypes.Penalty("-", 5, 5, 50, true),
                box("beta", 20, 6),
                new KnuthPlassTypes.Penalty("-", 10, 5, 50, true),
                box("gamma", 20, 11),
                forcedEnd(16)
        );

        List<KnuthPlassTypes.Breakpoint> breakpoints = breaker.computeBreakpoints(items, 25);

        assertEquals(3, breakpoints.size());
        assertTrue(breakpoints.get(1).demerits() - breakpoints.get(0).demerits() > 3000.0,
                "expected consecutive flagged hyphen breaks to accrue an additional policy demerit");
    }

    private static KnuthPlassTypes.Box box(String text, double width, int startOffset) {
        return new KnuthPlassTypes.Box(text, CharacterStyle.PLAIN, startOffset, startOffset + text.length(), width);
    }

    private static KnuthPlassTypes.Glue glue(double width, double stretch, double shrink, int startOffset) {
        return new KnuthPlassTypes.Glue(" ", CharacterStyle.PLAIN, startOffset, startOffset + 1, width, stretch, shrink);
    }

    private static KnuthPlassTypes.Penalty forcedEnd(int offset) {
        return new KnuthPlassTypes.Penalty("", offset, 0, KnuthPlassTypes.FORCED_BREAK_PENALTY, false);
    }
}
