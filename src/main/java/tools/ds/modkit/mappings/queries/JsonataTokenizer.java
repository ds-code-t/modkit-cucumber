package tools.ds.modkit.mappings.queries;

import java.util.ArrayList;
import java.util.List;

public final class JsonataTokenizer {

    /**
     * Tokenizes a Jsonata-like path/expression into step tokens.
     *
     * Examples
     * --------
     * "$.top.ArrayNode[5].*.Obj4.aNode[11,1..5,7,8].**.obj4"
     * -> ["$.", "top", "ArrayNode", "[5]", "*", "Obj4", "aNode", "[11,1..5,7,8]", "**", "obj4"]
     *
     * Rules
     * -----
     * 1) Top-level '.' splits steps (but not inside (), [], {} or string literals).
     * 2) The initial root is kept as "$." (if present) or "$" (if no dot follows).
     * 3) Square-bracket segments are one token, including filters/ranges/unions: "[...]" .
     * 4) Curly/object segments are one token: "{...}" .
     * 5) Function calls stay attached to the identifier: "map(...)" is one token step.
     * 6) Wildcards and specials:
     *      "*"   -> step token
     *      "**"  -> step token (recursive descent)
     *      "^" or "^<digits>" -> step token (parent steps)
     * 7) Whitespace outside of groupings is ignored; inside tokens it’s preserved.
     * 8) Quoted strings inside any grouping are handled with escapes.
     *
     * Notes
     * -----
     * - This is intentionally path-step oriented. It will happily carry along any
     *   advanced/obscure Jsonata syntax inside a bracket/object/function token without
     *   trying to interpret it.
     * - That makes it well-suited for your “apply tokens step-by-step” executor: each
     *   step is atomic from the path-walking perspective.
     */
    public static List<String> tokenize(String expr) {
        ArrayList<String> tokens = new ArrayList<>();
        if (expr == null) return tokens;

        String s = expr.trim();
        int n = s.length();
        int i = 0;

        // Handle root: "$." or "$"
        if (i < n && s.charAt(i) == '$') {
            if (i + 1 < n && s.charAt(i + 1) == '.') {
                tokens.add("$."); // match the user’s desired root token
                i += 2;
            } else {
                tokens.add("$");
                i += 1;
                // If the next char is a dot, consume it as a separator
                if (i < n && s.charAt(i) == '.') i++;
            }
            // Skip any incidental whitespace after root
            while (i < n && isWs(s.charAt(i))) i++;
        }

        StringBuilder buf = new StringBuilder();

        while (i < n) {
            char c = s.charAt(i);

            // Skip whitespace between steps (but not inside groups)
            if (isWs(c)) { i++; continue; }

            // Top-level separators / special path tokens
            if (c == '.') {
                // End of a name or function step
                flush(buf, tokens);
                i++;
                continue;
            }

            // Double-star (recursive descent) or single-star (wildcard)
            if (c == '*') {
                flush(buf, tokens);
                if (i + 1 < n && s.charAt(i + 1) == '*') {
                    tokens.add("**");
                    i += 2;
                } else {
                    tokens.add("*");
                    i += 1;
                }
                continue;
            }

            // Parent operator ^ or ^<digits>
            if (c == '^') {
                flush(buf, tokens);
                int j = i + 1;
                while (j < n && Character.isDigit(s.charAt(j))) j++;
                tokens.add(s.substring(i, j)); // "^" or "^123"
                i = j;
                continue;
            }

            // Square brackets => one atomic token [ ...balanced... ]
            if (c == '[') {
                flush(buf, tokens);
                int end = scanBalanced(s, i, '[', ']');
                tokens.add(s.substring(i, end + 1));
                i = end + 1;
                continue;
            }

            // Object constructor or block => one atomic token { ...balanced... }
            if (c == '{') {
                flush(buf, tokens);
                int end = scanBalanced(s, i, '{', '}');
                tokens.add(s.substring(i, end + 1));
                i = end + 1;
                continue;
            }

            // Parentheses: attach to current identifier to form e.g. "map(...)" as one step
            if (c == '(') {
                // If there's no identifier yet, start one so "(...)" becomes its own token
                if (buf.isEmpty()) buf.append("");
                int end = scanBalanced(s, i, '(', ')');
                buf.append(s, i, end + 1);
                i = end + 1;
                continue;
            }

            // Composition operator "~>" — treat as a step boundary, keep as its own token
            if (c == '~' && i + 1 < n && s.charAt(i + 1) == '>') {
                flush(buf, tokens);
                tokens.add("~>");
                i += 2;
                continue;
            }

            // Otherwise, collect a bare identifier / symbol run until a top-level delimiter
            // (., *, ^, [, {, (, ~>) while *not* inside any grouping).
            int j = i;
            while (j < n) {
                char cj = s.charAt(j);
                if (cj == '.' || cj == '*' || cj == '^' || cj == '[' || cj == '{' || cj == '(' ||
                        (cj == '~' && j + 1 < n && s.charAt(j + 1) == '>') ||
                        isWs(cj)) break;
                j++;
            }
            if (j > i) {
                buf.append(s, i, j);
                i = j;
                continue;
            }

            // Fallback (shouldn’t happen often): consume the single char
            buf.append(c);
            i++;
        }

        flush(buf, tokens);
        return tokens;
    }

    private static boolean isWs(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    private static void flush(StringBuilder buf, List<String> out) {
        if (buf.length() > 0) {
            out.add(buf.toString());
            buf.setLength(0);
        }
    }

    /**
     * Scan forward from 'start' (which must point at 'open') to the matching 'close',
     * honoring nested (), [], {} and quoted strings with escapes.
     *
     * Returns the index of the *matching close*.
     * Throws IllegalArgumentException if no match is found.
     */
    private static int scanBalanced(String s, int start, char open, char close) {
        int n = s.length();
        if (start >= n || s.charAt(start) != open) {
            throw new IllegalArgumentException("scanBalanced: expected " + open + " at " + start);
        }

        int i = start;
        int depthSquare = 0, depthCurly = 0, depthParen = 0;

        // Initialize the depth for the opening kind we started on
        if (open == '[') depthSquare = 1;
        else if (open == '{') depthCurly = 1;
        else if (open == '(') depthParen = 1;

        boolean inString = false;
        char quote = 0;
        boolean escape = false;

        for (i = start + 1; i < n; i++) {
            char c = s.charAt(i);

            // Inside a quoted string? (respect escapes)
            if (inString) {
                if (escape) {
                    escape = false; // skip this char
                    continue;
                }
                if (c == '\\') {
                    escape = true;
                    continue;
                }
                if (c == quote) {
                    inString = false;
                    quote = 0;
                }
                continue;
            }

            // Not inside a string — opening a new string?
            if (c == '"' || c == '\'' || c == '`') {
                inString = true;
                quote = c;
                continue;
            }

            // Track all three kinds of brackets so we don't confuse ']' inside a '{...}' etc.
            if (c == '[') depthSquare++;
            else if (c == ']') depthSquare--;
            else if (c == '{') depthCurly++;
            else if (c == '}') depthCurly--;
            else if (c == '(') depthParen++;
            else if (c == ')') depthParen--;

            // Matched our target closing *only* when the other depths are not negative
            if (c == close) {
                boolean matched =
                        (close == ']' && depthSquare == 0) ||
                                (close == '}' && depthCurly == 0) ||
                                (close == ')' && depthParen == 0);
                if (matched) return i;
            }

            // Sanity: depths must not dip below zero for non-targets
            if (depthSquare < 0 || depthCurly < 0 || depthParen < 0) {
                throw new IllegalArgumentException("Unbalanced brackets near index " + i);
            }
        }

        throw new IllegalArgumentException("Unbalanced '" + open + "' starting at " + start);
    }

    // --- quick demo ---
    public static void main(String[] args) {
        String ex = "$.a.b[ x in [1..5] and $foo(bar[2]) ].($.z).%.%.{ \"k\": $.q }.**.c";
        System.out.println(tokenize(ex));

    }
}
