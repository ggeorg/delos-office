package io.github.ggeorg.delos.writer.layout;

/**
 * Marker interface for positioned page content.
 */
public sealed interface LaidOutBlock permits LaidOutTextBlock, LaidOutAtomicBlock {
}
