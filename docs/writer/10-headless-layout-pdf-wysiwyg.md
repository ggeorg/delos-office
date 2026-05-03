# Baseline v1 — Delos Headless Layout & PDF WYSIWYG Design Spec

## 1. Purpose

Delos must support both a desktop Writer experience and a commercial reporting/PDF automation engine. Therefore, PDF output must not depend on JavaFX screenshots, JavaFX node geometry, or desktop-only rendering behavior.

The goal is:

```text
One document model.
One headless layout engine.
Many renderers.
```

Renderers include:

```text
JavaFX preview
PDF export
Print
Future Delos Report Server
Future CLI/report engine
```

The immediate problem is that JavaFX preview and PDF export can disagree on text wrapping, line positions, page breaks, fonts, and margins if the PDF path uses typography assumptions that differ from the editor preview.

This spec defines the architecture to make Delos documents WYSIWYG for Delos-native documents and Delos-native report templates.

---

## 2. Business constraint

Delos Works is not only a desktop office suite. The commercial direction is document automation, reporting, PDF generation, and server-side document infrastructure.

Therefore:

```text
PDF generation must be headless.
Report generation must be headless.
The layout engine must not require JavaFX.
The server product must not depend on JavaFX.
```

JavaFX is a renderer and editor shell, not the source of truth.

---

## 3. Non-negotiable architecture rule

Correct architecture:

```text
Document model
  ↓
Headless layout engine
  ↓
LaidOutDocument
  ↓
JavaFX renderer
PDF renderer
Print renderer
Report Server renderer
```

Incorrect architecture:

```text
JavaFX nodes
  ↓
screenshot / node snapshot
  ↓
PDF
```

Also incorrect:

```text
Document model
  ↓
JavaFX layout guesses one result
PDF exporter guesses another result
```

The PDF exporter may run layout, but it must run the same headless layout policy as the JavaFX preview.

---

## 4. Source of truth

### 4.1 Document model owns document settings

The following belong to the document model:

```text
page size
orientation
margins
paragraph style
character style
table style
image properties
list metadata
```

Current storage target:

```text
Document.pageStyle()
```

for:

```text
paper size
orientation
top/right/bottom/left margins
```

These settings must serialize with the document.

### 4.2 App preferences own UI settings only

The following do not belong in the document model:

```text
sidebar width
last selected inspector tab
preferred UI units
window size
zoom level
recent files
```

These belong in app preferences later.

### 4.3 Units

Internal document/layout/PDF units:

```text
points
72 pt = 1 inch
```

UI may display:

```text
cm
mm
in
pt
```

But the model stores canonical points.

---

## 5. WYSIWYG definition for Delos

For Delos-native documents, WYSIWYG means:

```text
same page size
same margins
same text wrapping
same line count
same paragraph positions
same table positions
same image positions
same page breaks
same PDF output from desktop and server
```

It does not initially mean:

```text
perfect Microsoft Word compatibility
perfect Apple Pages compatibility
perfect LibreOffice compatibility
pixel-perfect cross-platform system-font matching
full DOCX layout compatibility
advanced OpenType shaping parity
```

The initial target is:

```text
Delos-to-Delos parity.
```

---

## 6. Layout pipeline

### 6.1 Canonical pipeline

```text
Document
  ↓
PaginatingDocumentLayoutEngine
  ↓
LaidOutDocument
  ↓
Render target
```

The layout engine is responsible for:

```text
page geometry
margin box calculation
paragraph layout
line breaking
line box positions
block positions
table layout
image layout
page breaking
list indentation
```

Render targets are responsible for drawing only.

### 6.2 Renderer responsibilities

A renderer may:

```text
draw text run at x/y
draw underline/strikethrough
draw image at x/y/w/h
draw table background
draw table borders
draw page background
draw selection/caret overlays if interactive renderer
```

A renderer must not:

```text
invent page breaks
re-wrap paragraphs privately
choose unrelated default fonts
change margins
change line spacing
change table widths
change paragraph y positions
```

---

## 7. Layout theme/profile

Delos needs one shared layout policy used by both JavaFX preview and PDF export.

Recommended concept:

```java
LayoutTheme.defaultTheme()
```

or:

```java
WriterLayoutProfile.printPreview()
```

This shared profile owns:

```text
default body font family
default body font size
line height multiplier
paragraph spacing defaults
list indentation defaults
table defaults
image defaults
unit conversion policy
font fallback policy
```

### 7.1 Immediate default typography

Initial conservative defaults:

```text
body font family: Serif
body font size: current existing default
line height behavior: current existing default, but defined once through layout/theme + measurer
```

The exact numbers should preserve existing behavior as much as possible, but they must be defined once in the shared layout layer.

### 7.2 Avoid PDF-only defaults

PDF code must not secretly decide:

```text
Helvetica for all body text
font size × 1.2
baseline = font size × 0.8
```

unless those values come from the shared layout policy or the actual PDF font metrics.

---

## 8. Font strategy

### 8.1 Short-term font mapping

PDF has built-in base fonts. JavaFX has platform fonts. To avoid bundled font complexity in the first stabilization patch, use logical font families.

Proposed mapping:

```text
Delos logical Serif      → PDF Times family
Delos logical Sans Serif → PDF Helvetica family
Delos logical Monospace  → PDF Courier family
```

Style mapping:

```text
regular     → regular
bold        → bold
italic      → italic
bold italic → bold italic
```

### 8.2 Medium-term font registry

Introduce a font registry later:

```text
logical font name
available desktop font
available PDF font
embedding policy
fallback chain
```

### 8.3 Long-term production PDF fonts

For enterprise/reporting correctness, Delos will eventually need:

```text
embedded fonts
font subsetting
consistent server font bundles
PDF/A-compatible font handling
better shaping support
```

Not in v117.10.

---

## 9. JavaFX preview role

JavaFX preview must consume the same `LaidOutDocument` geometry as PDF.

JavaFX may add interactive overlays:

```text
caret
selection
hover states
resize handles later
spellcheck underline later
comments later
```

But these are editor overlays, not layout truth.

JavaFX must not be required for:

```text
PDF export
batch report generation
server rendering
CLI rendering
```

---

## 10. PDF renderer role

PDF renderer consumes `LaidOutDocument` and draws fixed pages.

It uses:

```text
page width/height from laid-out pages
block x/y from layout
line x/y/baseline from layout
run style from model/layout
image boxes from layout
table boxes from layout
```

It must not use private page setup or private margin values.

---

## 11. Report Server role

Future Delos Report Server should use the same headless engine:

```text
Template document
  + JSON/business data
  ↓
Merged Delos document
  ↓
Headless layout engine
  ↓
PDF renderer
```

No JavaFX dependency.

This is essential for Docker/Kubernetes/server deployment.

---

## 12. Comparison with external tools

### 12.1 Microsoft Word

Word achieves strong DOCX-to-PDF fidelity by using Word’s own document layout/rendering pipeline for fixed-format export.

Lesson for Delos:

```text
Do not use two unrelated layout engines.
```

### 12.2 LibreOffice

LibreOffice can run headless and export documents to PDF using its own core document model/layout/export filters.

Lesson for Delos:

```text
Headless document layout is realistic and commercially useful.
```

### 12.3 HTML to PDF

HTML-to-PDF systems usually rely on Chromium or another browser engine.

Lesson for Delos:

```text
WYSIWYG comes from using the same layout engine that understands the document model.
```

### 12.4 Carbone-style reporting

Carbone-style systems commonly merge data into existing office templates and then rely on office/conversion infrastructure for PDF.

Lesson for Delos:

```text
Document automation is the market.
But Delos can differentiate by owning the editor + template model + headless PDF engine.
```

---

## 13. Immediate stabilization: v117.10

### Goal

Make JavaFX preview and PDF export use the same headless layout assumptions.

### Scope

```text
1. Keep Document.pageStyle as the source of truth.
2. Keep layout in delos-writer-layout, independent from JavaFX.
3. Use LayoutTheme.defaultTheme as the shared starting point for PDF defaults.
4. Remove the PDF default that forced body text to Helvetica.
5. Map logical Serif to PDF Times, Sans/System to PDF Helvetica, Monospace to PDF Courier.
6. Use PDF font descriptor metrics for PDF line height/baseline instead of naked constants.
7. Keep PDF renderer drawing laid-out geometry rather than making private layout decisions.
8. Add/adjust contract tests around default PDF fonts and PDF font metrics.
```

### Out of scope

```text
No JavaFX screenshots to PDF.
No JavaFX dependency in PDF/server path.
No full font embedding yet.
No HarfBuzz/shaping engine yet.
No DOCX-perfect compatibility.
No Pages/Word feature parity.
No new output framework module.
```

---

## 14. Contract tests

### 14.1 Page style parity test

Same document should produce same page geometry in preview and PDF layout paths:

```text
same page width
same page height
same margin box
same page count
```

### 14.2 Paragraph wrapping parity test

Same document with several paragraphs should produce:

```text
same paragraph block count
same line count per paragraph
same page breaks
same block y positions within tolerance
```

### 14.3 Table parity test

Same table document should produce:

```text
same table width
same row count
same row heights
same cell padding
same border state
```

### 14.4 Font mapping test

Logical font styles should map consistently:

```text
Serif regular/bold/italic/bold italic
Sans Serif regular/bold/italic/bold italic
Monospace regular/bold/italic/bold italic
```

### 14.5 No JavaFX dependency test

PDF module must not require JavaFX modules.

Guard against accidental dependency on:

```text
javafx.controls
javafx.graphics
javafx.fxml
```

---

## 15. Suggested module boundaries

### delos-document

Owns generic document primitives if applicable.

### delos-writer-core

Owns Writer document model:

```text
Document
PageStyle
ParagraphStyle
CharacterStyle
TableStyle
Image properties
List metadata
serialization
commands/model mutation
```

### delos-writer-layout

Owns headless layout:

```text
LayoutTheme / LayoutProfile
PaginatingDocumentLayoutEngine
TextMeasurer abstraction
LaidOutDocument
LaidOutPage
LaidOutBlock
LaidOutLine
LaidOutRun
```

### delos-writer-javafx

Owns JavaFX renderer/editor:

```text
JavaFxTextMeasurer
DocumentViewport
PageView
caret/selection overlays
interactive editing
```

### delos-writer-pdf

Owns PDF rendering/export:

```text
PdfRenderTextMeasurer
PdfWriterExporter
PdfRenderTarget
font mapping
PDF page drawing
```

Must remain headless.

### delos-writer-print

Owns printing integration.

---

## 16. Open design questions

### 16.1 Should the canonical text measurer be PDF-like or JavaFX-like?

Options:

```text
A. JavaFX preview measures text with JavaFX, PDF measures text with PDFBox.
B. Both use a common metrics abstraction with logical approximations.
C. Both use a real shared font/shaping library later.
```

Recommended now:

```text
Use shared layout policy and reduce drift.
Do not promise perfect glyph parity until embedded fonts/shared shaping exist.
```

### 16.2 Should JavaFX preview use PDF-compatible fonts?

Possibly for print preview mode.

Example:

```text
Serif → platform serif for editing
Serif → PDF Times equivalent for export
```

But this can still drift. Long-term, bundled/embedded fonts may be necessary.

### 16.3 Should Delos have two preview modes?

Potential future:

```text
Editing preview: native desktop feel
Print preview: strict output layout
```

For now, avoid this complexity. The normal page view should be close to output.

---

## 17. Stop condition for current cycle

After v117.10:

```text
Run full mvn clean test.
Verify document scrollbar behavior.
Verify page setup editability.
Verify context-sensitive inspector behavior.
Verify PDF page size/margins/line count against JavaFX preview.
```

Then continue only with:

```text
v118 — UX Polish Pass
v119 — Viewport/caret anchoring
STOP / review checkpoint
```

No new feature branch before review.

---

## 18. Final architecture statement

Delos should not be architected as:

```text
A JavaFX app that can export PDFs.
```

It should be architected as:

```text
A headless document engine with a JavaFX editor and PDF/report renderers.
```

That is the architecture that supports both:

```text
Delos Writer Community
Delos Reports
Delos Report Server
Delos Document Engine
```

and it is the only realistic path to WYSIWYG output without making the commercial server product depend on desktop UI technology.
