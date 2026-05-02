package io.github.ggeorg.delos.writer.io;

import io.github.ggeorg.delos.writer.document.Alignment;
import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.FormulaSourceFormat;
import io.github.ggeorg.delos.writer.document.HorizontalRuleBlock;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.ListMarkerKind;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.ParagraphListStyle;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import io.github.ggeorg.delos.writer.document.Story;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCell;
import io.github.ggeorg.delos.writer.document.TableCellStyle;
import io.github.ggeorg.delos.writer.document.TableColumnSpec;
import io.github.ggeorg.delos.writer.document.TableRow;
import io.github.ggeorg.delos.writer.document.TableStyle;
import io.github.ggeorg.delos.writer.document.TextRun;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Minimal Writer XML serializer used for the {@code content.xml} part inside a
 * native {@code .dlw} package.
 */
public final class DocumentSerializer {
    public static final String FORMAT_VERSION = "1";

    public String toXml(Document document) {
        Objects.requireNonNull(document, "document");
        try {
            var xml = newXmlDocument();
            Element root = xml.createElement("delos-document");
            root.setAttribute("version", FORMAT_VERSION);
            xml.appendChild(root);

            appendTextElement(xml, root, "title", document.title());
            root.appendChild(createPageStyle(xml, document.pageStyle()));
            root.appendChild(createStoryContainer(xml, "blocks", document.body()));

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(xml), new StreamResult(writer));
            return writer.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize Delos document", exception);
        }
    }

    public Document fromXml(String xml) {
        Objects.requireNonNull(xml, "xml");
        return read(new StringReader(xml));
    }

    public void write(Document document, OutputStream outputStream) throws IOException {
        Objects.requireNonNull(outputStream, "outputStream");
        outputStream.write(toXml(document).getBytes(StandardCharsets.UTF_8));
    }

    public Document read(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");
        try (inputStream) {
            return read(new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid Delos document", exception);
        }
    }

    private Document read(Reader reader) {
        try {
            org.w3c.dom.Document xml = newDocumentBuilder().parse(new InputSource(reader));
            Element root = xml.getDocumentElement();
            if (root == null || !"delos-document".equals(root.getTagName())) {
                throw new IllegalArgumentException("Unsupported Delos document root element");
            }

            String version = root.getAttribute("version");
            if (!FORMAT_VERSION.equals(version)) {
                throw new IllegalArgumentException("Unsupported Delos format version: " + version);
            }

            String title = childText(root, "title").orElse("Untitled");
            PageStyle pageStyle = parsePageStyle(requiredChild(root, "page-style"));
            Story body = parseBodyStory(root);
            return Document.fromBlocks(title, pageStyle, body.blocks());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse Delos document", exception);
        }
    }

    private static Element createPageStyle(org.w3c.dom.Document xml, PageStyle pageStyle) {
        Element page = xml.createElement("page-style");
        page.setAttribute("width", formatDouble(pageStyle.width()));
        page.setAttribute("height", formatDouble(pageStyle.height()));
        page.setAttribute("margin-top", formatDouble(pageStyle.marginTop()));
        page.setAttribute("margin-right", formatDouble(pageStyle.marginRight()));
        page.setAttribute("margin-bottom", formatDouble(pageStyle.marginBottom()));
        page.setAttribute("margin-left", formatDouble(pageStyle.marginLeft()));
        return page;
    }

    private static Element createStoryContainer(org.w3c.dom.Document xml, String tagName, Story story) {
        Element storyElement = xml.createElement(tagName);
        for (Block block : story.blocks()) {
            storyElement.appendChild(createBlock(xml, block));
        }
        return storyElement;
    }

    private static Element createBlock(org.w3c.dom.Document xml, Block block) {
        if (block instanceof ParagraphBlock paragraphBlock) {
            return createParagraph(xml, paragraphBlock.paragraph());
        }
        if (block instanceof ImageBlock imageBlock) {
            Element image = xml.createElement("image");
            image.setAttribute("source", imageBlock.source());
            image.setAttribute("width", formatDouble(imageBlock.width()));
            image.setAttribute("height", formatDouble(imageBlock.height()));
            setOptionalAttribute(image, "alt", imageBlock.altText());
            return image;
        }
        if (block instanceof HorizontalRuleBlock) {
            return xml.createElement("horizontal-rule");
        }
        if (block instanceof FormulaBlock formulaBlock) {
            Element formula = xml.createElement("formula");
            formula.setAttribute("source-format", formulaBlock.sourceFormat().xmlValue());
            setOptionalAttribute(formula, "alt-text", formulaBlock.altText());
            formula.setTextContent(formulaBlock.source());
            return formula;
        }
        if (block instanceof TableBlock tableBlock) {
            return createTable(xml, tableBlock);
        }
        throw new IllegalArgumentException("Unsupported block type: " + block.getClass().getName());
    }

    private static Element createTable(org.w3c.dom.Document xml, TableBlock tableBlock) {
        Element table = xml.createElement("table");
        if (tableBlock.headerRowCount() > 0) {
            table.setAttribute("header-rows", Integer.toString(tableBlock.headerRowCount()));
        }
        table.setAttribute("width-fraction", formatDouble(tableBlock.style().widthFraction()));
        table.setAttribute("cell-padding", formatDouble(tableBlock.style().cellPadding()));
        table.setAttribute("borders-enabled", Boolean.toString(tableBlock.style().bordersEnabled()));
        Element columnsElement = xml.createElement("columns");
        for (TableColumnSpec column : tableBlock.columns()) {
            Element columnElement = xml.createElement("column");
            columnElement.setAttribute("width-weight", formatDouble(column.widthWeight()));
            columnsElement.appendChild(columnElement);
        }
        table.appendChild(columnsElement);
        for (TableRow row : tableBlock.rows()) {
            Element rowElement = xml.createElement("row");
            for (TableCell cell : row.cells()) {
                Element cellElement = xml.createElement("cell");
                setOptionalAttribute(cellElement, "background-color", cell.style().backgroundColor());
                cellElement.appendChild(createStoryContainer(xml, "story", cell.content()));
                rowElement.appendChild(cellElement);
            }
            table.appendChild(rowElement);
        }
        return table;
    }

    private static Element createParagraph(org.w3c.dom.Document xml, Paragraph paragraph) {
        Element paragraphElement = xml.createElement("paragraph");
        ParagraphStyle style = paragraph.style();
        paragraphElement.setAttribute("alignment", style.alignment().name());
        paragraphElement.setAttribute("first-line-indent", formatDouble(style.firstLineIndent()));
        paragraphElement.setAttribute("spacing-before", formatDouble(style.spacingBefore()));
        paragraphElement.setAttribute("spacing-after", formatDouble(style.spacingAfter()));
        paragraphElement.setAttribute("line-spacing-multiplier", formatDouble(style.lineSpacingMultiplier()));
        if (style.listStyle().enabled()) {
            paragraphElement.setAttribute("list-kind", style.listStyle().kind().name());
            paragraphElement.setAttribute("list-level", Integer.toString(style.listStyle().level()));
            paragraphElement.setAttribute("list-start", Integer.toString(style.listStyle().start()));
        }

        for (TextRun run : paragraph.runs()) {
            paragraphElement.appendChild(createRun(xml, run));
        }
        return paragraphElement;
    }

    private static Element createRun(org.w3c.dom.Document xml, TextRun run) {
        Element runElement = xml.createElement("run");
        CharacterStyle style = run.style();
        runElement.setAttribute("bold", Boolean.toString(style.bold()));
        runElement.setAttribute("italic", Boolean.toString(style.italic()));
        runElement.setAttribute("underline", Boolean.toString(style.underline()));
        runElement.setAttribute("strikethrough", Boolean.toString(style.strikethrough()));
        setOptionalAttribute(runElement, "font-family", style.fontFamily());
        if (style.fontSize() != null) {
            runElement.setAttribute("font-size", formatDouble(style.fontSize()));
        }
        setOptionalAttribute(runElement, "color", style.color());
        setOptionalAttribute(runElement, "link-href", style.linkHref());
        runElement.setTextContent(run.text());
        return runElement;
    }

    private static PageStyle parsePageStyle(Element page) {
        return new PageStyle(
                parseRequiredDouble(page, "width"),
                parseRequiredDouble(page, "height"),
                parseRequiredDouble(page, "margin-top"),
                parseRequiredDouble(page, "margin-right"),
                parseRequiredDouble(page, "margin-bottom"),
                parseRequiredDouble(page, "margin-left")
        );
    }

    private static Story parseBodyStory(Element root) {
        List<Element> blockContainers = childElements(root, "blocks");
        if (!blockContainers.isEmpty()) {
            return parseStoryContainer(blockContainers.getFirst());
        }

        Element paragraphsElement = requiredChild(root, "paragraphs");
        List<Block> blocks = new ArrayList<>();
        for (Element paragraphElement : childElements(paragraphsElement, "paragraph")) {
            blocks.add(ParagraphBlock.of(parseParagraph(paragraphElement)));
        }
        if (blocks.isEmpty()) {
            blocks.add(ParagraphBlock.of(Paragraph.of("")));
        }
        return Story.ofBlocks(blocks);
    }

    private static Story parseStoryContainer(Element storyElement) {
        return Story.ofBlocks(parseBlockChildren(storyElement));
    }

    private static List<Block> parseBlockChildren(Element blocksElement) {
        List<Block> blocks = new ArrayList<>();
        NodeList nodeList = blocksElement.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element element) {
                blocks.add(parseBlock(element));
            }
        }
        if (blocks.isEmpty()) {
            blocks.add(ParagraphBlock.of(Paragraph.of("")));
        }
        return blocks;
    }

    private static Block parseBlock(Element element) {
        return switch (element.getTagName()) {
            case "paragraph" -> ParagraphBlock.of(parseParagraph(element));
            case "image" -> new ImageBlock(
                    element.getAttribute("source"),
                    optionalDouble(element, "width", 0.0),
                    optionalDouble(element, "height", 0.0),
                    Objects.requireNonNullElse(element.getAttribute("alt"), "")
            );
            case "horizontal-rule" -> new HorizontalRuleBlock();
            case "formula" -> new FormulaBlock(
                    FormulaSourceFormat.fromXmlValue(element.getAttribute("source-format")),
                    element.getTextContent(),
                    Objects.requireNonNullElse(element.getAttribute("alt-text"), "")
            );
            case "table" -> parseTable(element);
            default -> throw new IllegalArgumentException("Unsupported block element: " + element.getTagName());
        };
    }

    private static TableBlock parseTable(Element tableElement) {
        List<TableColumnSpec> columns = parseTableColumns(tableElement);
        int headerRowCount = optionalInt(tableElement, "header-rows", 0);
        TableStyle tableStyle = new TableStyle(
                optionalDouble(tableElement, "width-fraction", 1.0),
                optionalDouble(tableElement, "cell-padding", 5.0),
                optionalBoolean(tableElement, "borders-enabled", true)
        );
        List<TableRow> rows = new ArrayList<>();
        for (Element rowElement : childElements(tableElement, "row")) {
            List<TableCell> cells = new ArrayList<>();
            for (Element cellElement : childElements(rowElement, "cell")) {
                TableCellStyle style = new TableCellStyle(optionalAttribute(cellElement, "background-color"));
                cells.add(new TableCell(parseCellStory(cellElement), style));
            }
            rows.add(new TableRow(cells));
        }
        return columns.isEmpty()
                ? new TableBlock(rows).withHeaderRowCount(headerRowCount).withStyle(tableStyle)
                : new TableBlock(rows, columns, headerRowCount, tableStyle);
    }

    private static List<TableColumnSpec> parseTableColumns(Element tableElement) {
        List<Element> columnsContainers = childElements(tableElement, "columns");
        if (columnsContainers.isEmpty()) {
            return List.of();
        }
        List<TableColumnSpec> columns = new ArrayList<>();
        for (Element columnElement : childElements(columnsContainers.getFirst(), "column")) {
            columns.add(new TableColumnSpec(parseRequiredDouble(columnElement, "width-weight")));
        }
        return columns;
    }

    private static Story parseCellStory(Element cellElement) {
        List<Element> storyElements = childElements(cellElement, "story");
        if (!storyElements.isEmpty()) {
            return parseStoryContainer(storyElements.getFirst());
        }

        // Compatibility with v73-v79 content.xml, where cell block children were
        // written directly under <cell> instead of inside a nested <story>.
        return parseStoryContainer(cellElement);
    }

    private static Paragraph parseParagraph(Element paragraphElement) {
        ParagraphStyle style = new ParagraphStyle(
                Alignment.valueOf(paragraphElement.getAttribute("alignment")),
                parseRequiredDouble(paragraphElement, "first-line-indent"),
                parseRequiredDouble(paragraphElement, "spacing-before"),
                parseRequiredDouble(paragraphElement, "spacing-after"),
                parseRequiredDouble(paragraphElement, "line-spacing-multiplier"),
                parseListStyle(paragraphElement)
        );

        List<TextRun> runs = new ArrayList<>();
        for (Element runElement : childElements(paragraphElement, "run")) {
            CharacterStyle characterStyle = new CharacterStyle(
                    Boolean.parseBoolean(runElement.getAttribute("bold")),
                    Boolean.parseBoolean(runElement.getAttribute("italic")),
                    Boolean.parseBoolean(runElement.getAttribute("underline")),
                    Boolean.parseBoolean(runElement.getAttribute("strikethrough")),
                    optionalAttribute(runElement, "font-family"),
                    optionalDouble(runElement, "font-size"),
                    optionalAttribute(runElement, "color"),
                    optionalAttribute(runElement, "link-href")
            );
            runs.add(new TextRun(runElement.getTextContent(), characterStyle));
        }

        return new Paragraph(style, runs);
    }

    private static ParagraphListStyle parseListStyle(Element paragraphElement) {
        String kindValue = paragraphElement.getAttribute("list-kind");
        if (kindValue == null || kindValue.isBlank()) {
            return ParagraphListStyle.none();
        }
        ListMarkerKind kind = ListMarkerKind.valueOf(kindValue);
        if (kind == ListMarkerKind.NONE) {
            return ParagraphListStyle.none();
        }
        int level = optionalInt(paragraphElement, "list-level", 0);
        int start = optionalInt(paragraphElement, "list-start", 1);
        return new ParagraphListStyle(kind, level, start);
    }

    private static int optionalInt(Element element, String attributeName, int fallback) {
        String value = element.getAttribute(attributeName);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value);
    }

    private static double optionalDouble(Element element, String attributeName, double fallback) {
        String raw = element.getAttribute(attributeName);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Double.parseDouble(raw);
    }

    private static boolean optionalBoolean(Element element, String attributeName, boolean fallback) {
        String raw = element.getAttribute(attributeName);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(raw);
    }

    private static org.w3c.dom.Document newXmlDocument() throws Exception {
        return newDocumentBuilder().newDocument();
    }

    private static DocumentBuilder newDocumentBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);
        factory.setXIncludeAware(false);
        return factory.newDocumentBuilder();
    }

    private static void appendTextElement(org.w3c.dom.Document xml, Element parent, String tagName, String value) {
        Element element = xml.createElement(tagName);
        element.setTextContent(value);
        parent.appendChild(element);
    }

    private static void setOptionalAttribute(Element element, String name, String value) {
        if (value != null && !value.isBlank()) {
            element.setAttribute(name, value);
        }
    }

    private static String formatDouble(double value) {
        return Double.toString(value);
    }

    private static double parseRequiredDouble(Element element, String attributeName) {
        String raw = element.getAttribute(attributeName);
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Missing attribute '" + attributeName + "' on <" + element.getTagName() + ">");
        }
        return Double.parseDouble(raw);
    }

    private static Double optionalDouble(Element element, String attributeName) {
        String raw = element.getAttribute(attributeName);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Double.parseDouble(raw);
    }

    private static String optionalAttribute(Element element, String attributeName) {
        String raw = element.getAttribute(attributeName);
        return raw == null || raw.isBlank() ? null : raw;
    }

    private static java.util.Optional<String> childText(Element parent, String tagName) {
        List<Element> children = childElements(parent, tagName);
        if (children.isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(children.getFirst().getTextContent());
    }

    private static Element requiredChild(Element parent, String tagName) {
        List<Element> children = childElements(parent, tagName);
        if (children.isEmpty()) {
            throw new IllegalArgumentException("Missing <" + tagName + "> under <" + parent.getTagName() + ">");
        }
        return children.getFirst();
    }

    private static List<Element> childElements(Element parent, String tagName) {
        NodeList nodeList = parent.getChildNodes();
        List<Element> elements = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element element && tagName.equals(element.getTagName())) {
                elements.add(element);
            }
        }
        return elements;
    }
}
