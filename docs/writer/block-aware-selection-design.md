# Delos Writer Block-Aware Selection Design

Version: v117.20.2
Status: design + explicit mixed-block deletion implementation

## Problem

The current Writer editing model has two different addressing systems:

- `SelectionRange` uses paragraph projection space: `(paragraphIndex, offset)`.
- Rich blocks such as images, formulas, horizontal rules, and tables live in `Document.blocks()` space.

This is safe for pure text editing, but it is incomplete for visual selections that cross or target non-paragraph blocks.

Example document:

```text
Paragraph A
[image]
Paragraph B
```

A paragraph-projection selection from paragraph 0 into paragraph 1 can visually appear to cross the image, but the current `StoryEditor` only replaces paragraph content. The image is preserved because it is not represented by the paragraph projection.

This behavior is now pinned by tests in v117.20.1 so future work changes it deliberately instead of accidentally.

## Current baseline semantics

### Text selection

`SelectionRange` remains a text-story selection.

It means:

```text
anchor paragraph index + offset
focus paragraph index + offset
```

It does **not** mean top-level block index.

Current behavior:

- Editing a `SelectionRange` changes paragraph content only.
- Non-paragraph blocks interleaved between selected paragraphs are preserved.
- A mixed visual selection is therefore not fully modeled yet.

### Whole block selection

`BlockSelection` exists for selecting a whole top-level non-text block.

It means:

```text
blockIndex in Document.blocks()
```

Current behavior:

- `DocumentEditor.removeBlock(document, blockIndex, ...)` removes non-paragraph blocks.
- Removing a paragraph through `removeBlock` is intentionally ignored today, because paragraph deletion is handled through text selection/editing.

### Table cell selection

`TableCellSelection` is separate and addresses a table cell as:

```text
table block index + row index + column index
```

It is not the same as a document-body `SelectionRange`.

## Target selection model

Delos Writer should eventually support a discriminated selection model:

```text
WriterSelection
├── TextSelection
│   └── story path + anchor/focus CaretPosition
├── BlockSelection
│   └── top-level block index
├── TableCellSelection
│   └── table block index + row/column range
└── MixedBlockSelection
    └── ordered spans across paragraphs and top-level blocks
```

The important rule is that selections must carry their coordinate space explicitly.

A future mixed selection should not be represented as a paragraph-only `SelectionRange`.

## Deletion semantics to implement next

### Text-only selection

```text
Before: Paragraph A | Paragraph B
Select: text within paragraphs
Delete: remove selected text and merge paragraph fragments
```

This remains the current `SelectionRange` path.

### Atomic block selection

```text
Before: Paragraph A | [image] | Paragraph B
Select: image block
Delete: remove image block only
Caret: nearest text position after the removed block
Undo: restores image block and media reference
```

This mostly exists through `DocumentEditor.removeBlock`, but it needs to be surfaced consistently through the UI selection model.

### Mixed block selection

```text
Before: Paragraph A | [image] | Paragraph B
Select: from middle of Paragraph A through image into Paragraph B
Delete: remove selected text and selected image
Result: merged paragraph text only, image deleted
```

This is not implemented yet.

The future implementation should:

- identify all top-level blocks between the selection boundaries,
- preserve unselected prefix text,
- preserve unselected suffix text,
- delete selected atomic/table blocks,
- merge surviving paragraph fragments,
- preserve undo/redo as one edit command,
- keep table-cell story selections separate from body mixed selections.

## Non-goals for v117.20.1

v117.20.1 does **not** implement mixed deletion.

It only:

- documents the expected model,
- pins today’s paragraph-only behavior,
- pins current block-selection deletion behavior,
- prevents accidental future confusion between paragraph projection space and block index space.

## v117.20.2 implementation

The visual editor now uses an explicit mixed-block replacement path for body selections:

```java
DocumentEditor.replaceIncludingInterleavedBlocks(...)
```

This path still accepts the current body `SelectionRange`, but the method name makes the semantics explicit: all top-level blocks between the selected paragraph boundary blocks are treated as selected content.

Implemented behavior:

- preserve blocks before the start paragraph block,
- preserve the unselected prefix of the start paragraph,
- delete all blocks between the selected paragraph boundary blocks, including images, formulas, horizontal rules, and tables,
- preserve the unselected suffix of the end paragraph,
- merge paragraph fragments for single-line replacement,
- preserve paragraph-only `DocumentEditor.replace(...)` for callers that intentionally want paragraph-projection behavior.

The sealed `WriterSelection` hierarchy remains deferred. Nintendo rule: no empty framework shell before another real consumer needs it.
