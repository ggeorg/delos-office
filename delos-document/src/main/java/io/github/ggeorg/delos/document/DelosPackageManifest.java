package io.github.ggeorg.delos.document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Manifest metadata for the native Delos package.
 */
public final class DelosPackageManifest {
    private final Map<String, Entry> entriesByPath;

    public DelosPackageManifest(List<Entry> entries) {
        Objects.requireNonNull(entries, "entries");
        Map<String, Entry> ordered = new LinkedHashMap<>();
        for (Entry entry : entries) {
            Entry checked = Objects.requireNonNull(entry, "entry");
            if (ordered.putIfAbsent(checked.path(), checked) != null) {
                throw new IllegalArgumentException("duplicate manifest entry: " + checked.path());
            }
        }
        this.entriesByPath = Map.copyOf(ordered);
    }

    public static DelosPackageManifest fromPackage(DelosPackage delosPackage) {
        Objects.requireNonNull(delosPackage, "delosPackage");
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry("/", delosPackage.rootMediaType()));
        delosPackage.parts().stream()
                .filter(part -> !DelosPackageNames.MANIFEST_XML.equals(part.path()))
                .map(part -> new Entry(part.path(), part.mediaType()))
                .forEach(entries::add);
        return new DelosPackageManifest(entries);
    }

    public List<Entry> entries() {
        return entriesByPath.values().stream()
                .sorted(Comparator.comparing(Entry::path))
                .toList();
    }

    public Optional<Entry> find(String path) {
        return Optional.ofNullable(entriesByPath.get(path));
    }

    public String toXml() {
        StringBuilder xml = new StringBuilder(512);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<manifest xmlns=\"urn:delos:package:manifest:1\">\n");
        for (Entry entry : entries()) {
            xml.append("  <file-entry full-path=\"")
                    .append(DelosPackageXml.escapeAttribute(entry.path()))
                    .append("\" media-type=\"")
                    .append(DelosPackageXml.escapeAttribute(entry.mediaType()))
                    .append("\"/>\n");
        }
        xml.append("</manifest>\n");
        return xml.toString();
    }

    public record Entry(String path, String mediaType) {
        public Entry {
            path = Objects.requireNonNullElse(path, "").trim();
            mediaType = Objects.requireNonNullElse(mediaType, "").trim();
            if (path.isEmpty()) {
                throw new IllegalArgumentException("manifest entry path must not be blank");
            }
        }
    }
}
