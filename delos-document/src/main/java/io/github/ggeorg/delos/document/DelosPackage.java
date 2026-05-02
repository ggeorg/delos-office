package io.github.ggeorg.delos.document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A native Delos ZIP package in memory.
 *
 * <p>The package container is generic. Its {@code rootMediaType} tells the suite
 * which document family owns the content schema, for example Writer, Sheet,
 * Presentation, or Drawing. This mirrors the OpenDocument family: common package
 * discipline, concrete document-family MIME types.</p>
 */
public final class DelosPackage {
    private final String rootMediaType;
    private final Map<String, DelosPackagePart> partsByPath;

    public DelosPackage(String rootMediaType, List<DelosPackagePart> parts) {
        this.rootMediaType = requireNonBlank(rootMediaType, "rootMediaType");
        Objects.requireNonNull(parts, "parts");

        Map<String, DelosPackagePart> ordered = new LinkedHashMap<>();
        for (DelosPackagePart part : parts) {
            DelosPackagePart checked = Objects.requireNonNull(part, "part");
            if (ordered.putIfAbsent(checked.path(), checked) != null) {
                throw new IllegalArgumentException("duplicate package part: " + checked.path());
            }
        }

        requirePart(ordered, DelosPackageNames.CONTENT_XML);
        requirePart(ordered, DelosPackageNames.STYLES_XML);
        requirePart(ordered, DelosPackageNames.SETTINGS_XML);
        requirePart(ordered, DelosPackageNames.META_XML);
        requirePart(ordered, DelosPackageNames.MEDIA_DIR);
        this.partsByPath = Map.copyOf(ordered);
    }

    public static DelosPackage of(String rootMediaType, List<DelosPackagePart> parts) {
        return new DelosPackage(rootMediaType, parts);
    }

    public String rootMediaType() {
        return rootMediaType;
    }

    public List<DelosPackagePart> parts() {
        return partsByPath.values().stream()
                .sorted(Comparator.comparing(DelosPackagePart::path))
                .toList();
    }

    public Optional<DelosPackagePart> part(String path) {
        Objects.requireNonNull(path, "path");
        return Optional.ofNullable(partsByPath.get(path.replace('\\', '/')));
    }

    public DelosPackagePart requirePart(String path) {
        return part(path).orElseThrow(() -> new IllegalArgumentException("missing package part: " + path));
    }

    public DelosPackage withGeneratedManifest() {
        List<DelosPackagePart> parts = new ArrayList<>(partsByPath.values().stream()
                .filter(part -> !DelosPackageNames.MANIFEST_XML.equals(part.path()))
                .toList());
        parts.add(DelosPackagePart.xml(DelosPackageNames.MANIFEST_XML, DelosPackageManifest.fromPackage(this).toXml()));
        return new DelosPackage(rootMediaType, parts);
    }

    private static void requirePart(Map<String, DelosPackagePart> parts, String path) {
        if (!parts.containsKey(path)) {
            throw new IllegalArgumentException("missing required Delos package part: " + path);
        }
    }

    private static String requireNonBlank(String value, String name) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
