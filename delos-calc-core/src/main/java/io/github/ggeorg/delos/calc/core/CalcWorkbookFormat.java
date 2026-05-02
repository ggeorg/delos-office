package io.github.ggeorg.delos.calc.core;

import io.github.ggeorg.delos.document.DocumentFormat;
import io.github.ggeorg.delos.document.DocumentType;
import org.w3c.dom.Element;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Native Calc workbook format used by the Delos suite shell.
 */
public final class CalcWorkbookFormat implements DocumentFormat<Workbook> {
    public static final String FORMAT_VERSION = "1";
    public static final DocumentType TYPE = new DocumentType(
            "calc",
            "Delos Spreadsheet",
            ".dcalc",
            "application/vnd.delos.calc+xml"
    );

    @Override
    public DocumentType type() {
        return TYPE;
    }

    @Override
    public Workbook createBlank(String title) {
        String normalized = title == null || title.isBlank() ? "Untitled Spreadsheet" : title.trim();
        return Workbook.blank().withTitle(normalized);
    }

    @Override
    public Workbook read(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");
        try (inputStream) {
            org.w3c.dom.Document xml = newDocumentBuilder().parse(new InputSource(inputStream));
            Element root = xml.getDocumentElement();
            if (root == null || !"delos-workbook".equals(root.getTagName())) {
                throw new IllegalArgumentException("Unsupported Delos workbook root element");
            }
            String version = root.getAttribute("version");
            if (!FORMAT_VERSION.equals(version)) {
                throw new IllegalArgumentException("Unsupported Delos workbook format version: " + version);
            }

            String title = defaultString(root.getAttribute("title"), "Untitled Spreadsheet");
            List<Sheet> sheets = new ArrayList<>();
            for (Element sheetElement : childElements(root, "sheet")) {
                sheets.add(parseSheet(sheetElement));
            }
            if (sheets.isEmpty()) {
                sheets.add(Sheet.named("Sheet1"));
            }
            return new Workbook(title, sheets);
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("Failed to read Delos workbook", exception);
        }
    }

    @Override
    public void write(Workbook workbook, OutputStream outputStream) throws IOException {
        Objects.requireNonNull(workbook, "workbook");
        Objects.requireNonNull(outputStream, "outputStream");
        try {
            org.w3c.dom.Document xml = newDocumentBuilder().newDocument();
            Element root = xml.createElement("delos-workbook");
            root.setAttribute("version", FORMAT_VERSION);
            root.setAttribute("title", workbook.title());
            xml.appendChild(root);

            for (Sheet sheet : workbook.sheets()) {
                root.appendChild(createSheet(xml, sheet));
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(xml), new StreamResult(outputStream));
        } catch (Exception exception) {
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Failed to write Delos workbook", exception);
        }
    }

    private static Element createSheet(org.w3c.dom.Document xml, Sheet sheet) {
        Element sheetElement = xml.createElement("sheet");
        sheetElement.setAttribute("name", sheet.name());
        for (Cell cell : sheet.cells()) {
            Element cellElement = xml.createElement("cell");
            cellElement.setAttribute("address", cell.address().toA1());
            cellElement.setAttribute("type", cell.content().type().name());
            cellElement.setTextContent(cell.content().text());
            sheetElement.appendChild(cellElement);
        }
        return sheetElement;
    }

    private static Sheet parseSheet(Element sheetElement) {
        Sheet sheet = Sheet.named(defaultString(sheetElement.getAttribute("name"), "Sheet1"));
        for (Element cellElement : childElements(sheetElement, "cell")) {
            CellAddress address = CellAddress.parse(requiredAttribute(cellElement, "address"));
            CellContent.Type type = CellContent.Type.valueOf(requiredAttribute(cellElement, "type"));
            String text = cellElement.getTextContent();
            sheet = sheet.withCell(address, new CellContent(type, text));
        }
        return sheet;
    }

    private static DocumentBuilder newDocumentBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder();
    }

    private static List<Element> childElements(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        List<Element> result = new ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            if (nodes.item(index).getParentNode() == parent && nodes.item(index) instanceof Element element) {
                result.add(element);
            }
        }
        return result;
    }

    private static String requiredAttribute(Element element, String attribute) {
        String value = element.getAttribute(attribute);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required attribute: " + attribute);
        }
        return value;
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
