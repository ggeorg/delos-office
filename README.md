# Delos Office

Delos is an engine-first Java/JavaFX office-suite foundation.

## Modules

```text
delos-render          rendering abstraction
delos-document        shared document registry, file I/O, and native package container
delos-writer-core     pure Writer document model, editing engine, and Writer format schema
delos-writer-layout   pure Writer pagination, layout, caret, hit testing
delos-writer-render   Writer rendering adapters
delos-writer-pdf      Writer PDF export backend
delos-calc-core       pure Calc workbook/sheet/cell model
delos-slides-core     pure Slides deck/slide/element model
delos-base-core       pure Base database project/object model
delos-javafx          shared JavaFX command and zoom infrastructure
delos-writer-javafx   reusable Writer JavaFX component
delos-calc-javafx     reusable Calc JavaFX component
delos-slides-javafx   reusable Slides JavaFX component
delos-base-javafx     reusable Base JavaFX component
delos-writer-app      standalone Writer application
delos-calc-app        standalone Calc application
delos-slides-app      standalone Slides application
delos-base-app        standalone Base application
```

Writer, Calc, Slides, and Base are sibling applications. They share infrastructure, but they do not live inside one combined `MainWindow`.

## Native Delos package v1

The native package layer is generic and lives in `delos-document`.

Inspired by ODF, Delos uses one common ZIP package discipline while each application owns its own document-family MIME type and content schema.

```text
mimetype
content.xml
styles.xml
settings.xml
meta.xml
META-INF/manifest.xml
media/
```

That means:

```text
delos-document    = package/container mechanics
WriterDocumentFormat = Writer MIME type + Writer content.xml schema
future Calc          = its own MIME type + its own content.xml schema
future Slides        = its own MIME type + its own content.xml schema
future Base          = its own MIME type + its own content.xml schema
```

Writer is the first real consumer of the package container. The shared module does not contain Writer-only DTOs such as paragraphs or text runs.

## Build

```bash
mvn clean test
```

## Run Writer

```bash
mvn -pl delos-writer-app -am javafx:run
```

## Run Calc

```bash
mvn -pl delos-calc-app -am javafx:run
```


## Run Slides

```bash
mvn -pl delos-slides-app -am javafx:run
```

## Run Base

```bash
mvn -pl delos-base-app -am javafx:run
```
