package tools.ds.modkit.mappings;

import java.util.*;
import java.util.regex.*;

public final class KeyParser {

    public enum Kind { LIST, MAP, SINGLE }

    public static record KeyParse(String base, Kind kind, int[] intList) {}

    // base  | optional "as-LIST/MAP" | optional "#<ints and/or ranges>"
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "^\\s*(.*?)" +                                  // (1) base (always)
                    "(?:\\s+(?i:as-(LIST|MAP)))?" +                 // (2) kind (optional)
                    "(?:\\s*#\\s*(" +                               // (3) numbers (optional)
                    "\\d+(?:\\s*-\\s*\\d+)?(?:\\s*,\\s*\\d+(?:\\s*-\\s*\\d+)?)*" +
                    "))?\\s*$"
    );

    public static KeyParse parseKey(String input) {
        if (input == null) return null;
        Matcher m = KEY_PATTERN.matcher(input.strip());
        if (!m.matches()) return null;

        String base = m.group(1).strip();

        Kind explicit = null;
        String g2 = m.group(2); // LIST or MAP if present
        if (g2 != null) explicit = Kind.valueOf(g2.toUpperCase(Locale.ROOT));

        int[] ints = parseInts(m.group(3)); // preserve order & duplicates

        // Default [-1] only when no ints and no explicit kind
        if ((ints == null || ints.length == 0) && explicit == null) {
            ints = new int[]{ -1 };
        }

        Kind kind = (explicit != null)
                ? explicit
                : (ints.length > 1 ? Kind.LIST : Kind.SINGLE);

        return new KeyParse(base, kind, ints);
    }

    private static int[] parseInts(String nums) {
        if (nums == null || nums.isBlank()) return new int[0];
        List<Integer> result = new ArrayList<>();
        for (String part : nums.split(",")) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            int dash = t.indexOf('-');
            if (dash >= 0) {
                int a = Integer.parseInt(t.substring(0, dash).trim());
                int b = Integer.parseInt(t.substring(dash + 1).trim());
                int lo = Math.min(a, b), hi = Math.max(a, b);
                for (int v = lo; v <= hi; v++) result.add(v);
            } else {
                result.add(Integer.parseInt(t));
            }
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    private KeyParser() {} // no instances
}
