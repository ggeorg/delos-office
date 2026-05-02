package io.github.ggeorg.delos.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RenderPathTest {
    @Test
    void builderCreatesImmutableCommandList() {
        RenderPath.Builder builder = RenderPath.builder()
                .moveTo(1.0, 2.0)
                .lineTo(3.0, 4.0);

        RenderPath path = builder.build();
        builder.lineTo(5.0, 6.0);

        assertEquals(2, path.commands().size());
        assertEquals(RenderPath.CommandType.MOVE_TO, path.commands().get(0).type());
        assertEquals(RenderPath.CommandType.LINE_TO, path.commands().get(1).type());
    }

    @Test
    void emptyPathIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> RenderPath.builder().build());
    }
}
