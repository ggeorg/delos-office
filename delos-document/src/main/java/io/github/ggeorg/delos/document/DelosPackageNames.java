package io.github.ggeorg.delos.document;

/**
 * Stable entry names used by native Delos ZIP packages.
 *
 * <p>This is the suite-level package contract. Writer, Sheet, Presentation, Drawing,
 * and future Delos applications may use the same physical package layout while
 * owning their own content schema and root media type.</p>
 */
public final class DelosPackageNames {
    public static final String MIMETYPE = "mimetype";
    public static final String CONTENT_XML = "content.xml";
    public static final String STYLES_XML = "styles.xml";
    public static final String SETTINGS_XML = "settings.xml";
    public static final String META_XML = "meta.xml";
    public static final String MANIFEST_XML = "META-INF/manifest.xml";
    public static final String MEDIA_DIR = "media/";

    private DelosPackageNames() {
    }
}
