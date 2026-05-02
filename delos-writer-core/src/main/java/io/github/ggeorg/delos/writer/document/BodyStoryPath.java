package io.github.ggeorg.delos.writer.document;

/**
 * Story path for the main document body.
 */
public record BodyStoryPath() implements StoryPath {
    public static final BodyStoryPath INSTANCE = new BodyStoryPath();

    @Override
    public boolean isBody() {
        return true;
    }

    @Override
    public String toString() {
        return "body";
    }
}
