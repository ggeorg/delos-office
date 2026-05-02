package io.github.ggeorg.delos.writer.render;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative LaTeX-source preview for formula placeholders.
 *
 * <p>This is deliberately not a math renderer. The canonical formula remains the
 * source stored in the document model; this class only makes common LaTeX input
 * more readable in editor/PDF placeholders without adding an external dependency.</p>
 */
final class FormulaDisplayText {
    private static final Pattern SIMPLE_FRAC = Pattern.compile("\\\\frac\\{([^{}]+)}\\{([^{}]+)}");
    private static final Pattern SIMPLE_SQRT = Pattern.compile("\\\\sqrt\\{([^{}]+)}");
    private static final Pattern BRACED_SUPERSCRIPT = Pattern.compile("\\^\\{([^{}]+)}");
    private static final Pattern BRACED_SUBSCRIPT = Pattern.compile("_\\{([^{}]+)}");
    private static final Pattern SINGLE_SUPERSCRIPT = Pattern.compile("\\^([A-Za-z0-9+\\-=()])");
    private static final Pattern SINGLE_SUBSCRIPT = Pattern.compile("_([A-Za-z0-9+\\-=()])");

    private static final Map<String, String> SYMBOLS = Map.ofEntries(
            Map.entry("\\alpha", "α"),
            Map.entry("\\beta", "β"),
            Map.entry("\\gamma", "γ"),
            Map.entry("\\delta", "δ"),
            Map.entry("\\epsilon", "ε"),
            Map.entry("\\theta", "θ"),
            Map.entry("\\lambda", "λ"),
            Map.entry("\\mu", "μ"),
            Map.entry("\\pi", "π"),
            Map.entry("\\rho", "ρ"),
            Map.entry("\\sigma", "σ"),
            Map.entry("\\phi", "φ"),
            Map.entry("\\omega", "ω"),
            Map.entry("\\Delta", "Δ"),
            Map.entry("\\Omega", "Ω"),
            Map.entry("\\times", "×"),
            Map.entry("\\cdot", "·"),
            Map.entry("\\div", "÷"),
            Map.entry("\\pm", "±"),
            Map.entry("\\leq", "≤"),
            Map.entry("\\geq", "≥"),
            Map.entry("\\neq", "≠"),
            Map.entry("\\approx", "≈"),
            Map.entry("\\infty", "∞"),
            Map.entry("\\rightarrow", "→"),
            Map.entry("\\leftarrow", "←")
    );

    private FormulaDisplayText() {
    }

    static String preview(String source) {
        String value = normalize(source);
        if (value.isEmpty()) {
            return "Formula";
        }

        value = stripMathDelimiters(value);
        value = replaceSimpleGroups(value, SIMPLE_FRAC, "(%s)/(%s)");
        value = replaceSimpleGroups(value, SIMPLE_SQRT, "√(%s)");
        value = replaceScript(value, BRACED_SUPERSCRIPT, true);
        value = replaceScript(value, BRACED_SUBSCRIPT, false);
        value = replaceScript(value, SINGLE_SUPERSCRIPT, true);
        value = replaceScript(value, SINGLE_SUBSCRIPT, false);
        value = replaceSymbols(value);
        value = value.replace("\\left", "")
                .replace("\\right", "")
                .replace("\\,", " ")
                .replace("\\;", " ")
                .replace("\\ ", " ")
                .replace("{", "")
                .replace("}", "");
        return normalize(value);
    }

    static String compactSource(String source) {
        String value = normalize(source);
        return value.isEmpty() ? "Formula source" : value;
    }

    static String abbreviate(String value, int maxLength) {
        String normalized = normalize(value);
        if (maxLength <= 1 || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - 1) + "…";
    }

    private static String normalize(String source) {
        return source == null ? "" : source.strip().replace('\n', ' ').replaceAll("\\s+", " ");
    }

    private static String stripMathDelimiters(String value) {
        String stripped = value;
        if (stripped.startsWith("$$") && stripped.endsWith("$$") && stripped.length() >= 4) {
            stripped = stripped.substring(2, stripped.length() - 2).strip();
        }
        if (stripped.startsWith("$") && stripped.endsWith("$") && stripped.length() >= 2) {
            stripped = stripped.substring(1, stripped.length() - 1).strip();
        }
        if (stripped.startsWith("\\(") && stripped.endsWith("\\)") && stripped.length() >= 4) {
            stripped = stripped.substring(2, stripped.length() - 2).strip();
        }
        if (stripped.startsWith("\\[") && stripped.endsWith("\\]") && stripped.length() >= 4) {
            stripped = stripped.substring(2, stripped.length() - 2).strip();
        }
        return stripped;
    }

    private static String replaceSymbols(String value) {
        String result = value;
        for (Map.Entry<String, String> entry : SYMBOLS.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static String replaceSimpleGroups(String value, Pattern pattern, String replacementFormat) {
        Matcher matcher = pattern.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String replacement = matcher.groupCount() == 2
                    ? replacementFormat.formatted(matcher.group(1), matcher.group(2))
                    : replacementFormat.formatted(matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceScript(String value, Pattern pattern, boolean superscript) {
        Matcher matcher = pattern.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(toScript(matcher.group(1), superscript)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String toScript(String value, boolean superscript) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char replacement = superscript ? superscript(value.charAt(i)) : subscript(value.charAt(i));
            result.append(replacement == 0 ? value.charAt(i) : replacement);
        }
        return result.toString();
    }

    private static char superscript(char ch) {
        return switch (ch) {
            case '0' -> '⁰';
            case '1' -> '¹';
            case '2' -> '²';
            case '3' -> '³';
            case '4' -> '⁴';
            case '5' -> '⁵';
            case '6' -> '⁶';
            case '7' -> '⁷';
            case '8' -> '⁸';
            case '9' -> '⁹';
            case '+' -> '⁺';
            case '-' -> '⁻';
            case '=' -> '⁼';
            case '(' -> '⁽';
            case ')' -> '⁾';
            case 'n' -> 'ⁿ';
            case 'i' -> 'ⁱ';
            default -> 0;
        };
    }

    private static char subscript(char ch) {
        return switch (ch) {
            case '0' -> '₀';
            case '1' -> '₁';
            case '2' -> '₂';
            case '3' -> '₃';
            case '4' -> '₄';
            case '5' -> '₅';
            case '6' -> '₆';
            case '7' -> '₇';
            case '8' -> '₈';
            case '9' -> '₉';
            case '+' -> '₊';
            case '-' -> '₋';
            case '=' -> '₌';
            case '(' -> '₍';
            case ')' -> '₎';
            case 'a' -> 'ₐ';
            case 'e' -> 'ₑ';
            case 'h' -> 'ₕ';
            case 'i' -> 'ᵢ';
            case 'j' -> 'ⱼ';
            case 'k' -> 'ₖ';
            case 'l' -> 'ₗ';
            case 'm' -> 'ₘ';
            case 'n' -> 'ₙ';
            case 'o' -> 'ₒ';
            case 'p' -> 'ₚ';
            case 'r' -> 'ᵣ';
            case 's' -> 'ₛ';
            case 't' -> 'ₜ';
            case 'u' -> 'ᵤ';
            case 'v' -> 'ᵥ';
            case 'x' -> 'ₓ';
            default -> 0;
        };
    }
}
