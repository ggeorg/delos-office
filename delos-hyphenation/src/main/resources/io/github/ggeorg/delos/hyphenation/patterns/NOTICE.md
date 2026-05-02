# Delos Hyphenation Pattern Resources

Delos' hyphenation engine is implemented in Java in the `delos-hyphenation`
module. Language pattern data is imported from the TeX hyphenation ecosystem.

## Imported resources

### `en-US.tex`

Source file:

```text
hyph-utf8/tex/generic/hyph-utf8/patterns/tex/hyph-en-us.tex
```

Upstream project:

```text
https://github.com/hyphenation/tex-hyphen
```

Upstream package:

```text
hyph-utf8
```

The upstream file header states that it is part of the `hyph-utf8` package and
contains this licence text:

```text
Copying and distribution of this file, with or without modification,
are permitted in any medium without royalty provided the copyright
notice and this notice are preserved.
```

When importing additional languages, copy only the needed UTF-8 TeX pattern
file, preserve its original header, and add a short entry here with source path,
language tag, and licence text.
