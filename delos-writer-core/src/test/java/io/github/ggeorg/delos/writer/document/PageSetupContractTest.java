package io.github.ggeorg.delos.writer.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PageSetupContractTest {
    @Test
    void a4DefaultKeepsExistingWriterGeometryStable() {
        PageStyle style = PageStyle.a4Default();

        assertEquals(595.0, style.width());
        assertEquals(842.0, style.height());
        assertEquals(68.0, style.marginTop());
        assertEquals(72.0, style.marginRight());
        assertEquals(72.0, style.marginBottom());
        assertEquals(72.0, style.marginLeft());
        assertEquals(451.0, style.contentWidth());
        assertEquals(702.0, style.contentHeight());
        assertTrue(style.isPortrait());
        assertFalse(style.isLandscape());
    }

    @Test
    void pageSetupCreatesLandscapeLetterWithoutRawMagicNumbersAtCallSite() {
        PageStyle style = PageSetup.letterDefault()
                .withOrientation(PageOrientation.LANDSCAPE)
                .toPageStyle();

        assertEquals(792.0, style.width());
        assertEquals(612.0, style.height());
        assertEquals(72.0, style.marginTop());
        assertEquals(72.0, style.marginRight());
        assertEquals(648.0, style.contentWidth());
        assertEquals(468.0, style.contentHeight());
        assertTrue(style.isLandscape());
    }

    @Test
    void pageStyleCanChangeMarginsAndOrientationWithoutChangingTheProductionModel() {
        PageStyle style = PageStyle.of(PageSize.A4, PageOrientation.PORTRAIT, PageMargins.oneInch())
                .landscape()
                .withMargins(new PageMargins(36.0, 48.0, 36.0, 48.0));

        assertEquals(842.0, style.width());
        assertEquals(595.0, style.height());
        assertEquals(new PageMargins(36.0, 48.0, 36.0, 48.0), style.margins());
        assertEquals(746.0, style.contentWidth());
        assertEquals(523.0, style.contentHeight());
    }

    @Test
    void invalidPageGeometryFailsEarlyBeforePdfOrPrintProduction() {
        assertThrows(IllegalArgumentException.class, () -> new PageSize("Bad", 0.0, 842.0));
        assertThrows(IllegalArgumentException.class, () -> new PageMargins(72.0, -1.0, 72.0, 72.0));
        assertThrows(IllegalArgumentException.class, () -> new PageStyle(100.0, 100.0, 10.0, 60.0, 10.0, 60.0));
        assertThrows(IllegalArgumentException.class, () -> new PageStyle(100.0, 100.0, 60.0, 10.0, 60.0, 10.0));
    }
    @Test
    void documentCanReplacePageStyleWithoutChangingBodyOrMedia() {
        Document original = Document.blank();
        PageStyle letterLandscape = PageSetup.letterDefault()
                .withOrientation(PageOrientation.LANDSCAPE)
                .toPageStyle();

        Document changed = original.withPageStyle(letterLandscape);

        assertEquals(letterLandscape, changed.pageStyle());
        assertEquals(original.title(), changed.title());
        assertEquals(original.blocks(), changed.blocks());
        assertEquals(original.mediaItems(), changed.mediaItems());
    }

}
