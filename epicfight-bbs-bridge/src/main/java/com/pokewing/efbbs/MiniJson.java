package com.pokewing.efbbs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny dependency-free JSON reader/writer, used ONLY by {@link OfflineConverter}
 * so the standalone tool can run with nothing but efbbs.jar on the classpath
 * (no Gson, no Minecraft, no Forge). Parses into Map/List/String/Double/Boolean/
 * null and writes those back out.
 */
public final class MiniJson {
    private final String s;
    private int i;

    private MiniJson(String s) { this.s = s; }

    public static Object parse(String text) {
        MiniJson p = new MiniJson(text);
        p.ws();
        Object v = p.value();
        p.ws();
        return v;
    }

    private Object value() {
        char c = s.charAt(i);
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': i += 4; return Boolean.TRUE;   // true
            case 'f': i += 5; return Boolean.FALSE;  // false
            case 'n': i += 4; return null;           // null
            default:  return number();
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++; // {
        ws();
        if (s.charAt(i) == '}') { i++; return m; }
        while (true) {
            ws();
            String key = string();
            ws();
            i++; // :
            ws();
            m.put(key, value());
            ws();
            char c = s.charAt(i++);
            if (c == '}') break;
            // c == ',' -> continue
        }
        return m;
    }

    private List<Object> array() {
        List<Object> a = new ArrayList<>();
        i++; // [
        ws();
        if (s.charAt(i) == ']') { i++; return a; }
        while (true) {
            ws();
            a.add(value());
            ws();
            char c = s.charAt(i++);
            if (c == ']') break;
            // c == ',' -> continue
        }
        return a;
    }

    private String string() {
        StringBuilder sb = new StringBuilder();
        i++; // opening quote
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') break;
            if (c == '\\') {
                char e = s.charAt(i++);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    default: sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Double number() {
        int start = i;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E'
                    || (c >= '0' && c <= '9')) i++;
            else break;
        }
        return Double.parseDouble(s.substring(start, i));
    }

    private void ws() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') i++;
            else break;
        }
    }

    // --- writing -----------------------------------------------------------

    public static String write(Object o) {
        StringBuilder sb = new StringBuilder();
        write(o, sb, 0);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void write(Object o, StringBuilder sb, int indent) {
        if (o == null) { sb.append("null"); return; }
        if (o instanceof String str) { sb.append('"').append(escape(str)).append('"'); return; }
        if (o instanceof Boolean) { sb.append(o); return; }
        if (o instanceof Number num) { sb.append(plainNumber(num.doubleValue())); return; }
        String pad = "  ".repeat(indent), pad2 = "  ".repeat(indent + 1);
        if (o instanceof Map<?, ?> m) {
            if (m.isEmpty()) { sb.append("{}"); return; }
            sb.append("{\n");
            int n = 0;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                sb.append(pad2).append('"').append(escape(String.valueOf(e.getKey()))).append("\": ");
                write(e.getValue(), sb, indent + 1);
                if (++n < m.size()) sb.append(',');
                sb.append('\n');
            }
            sb.append(pad).append('}');
            return;
        }
        if (o instanceof List<?> a) {
            // Numeric arrays (like [x,y,z]) stay on one line for readability.
            boolean simple = a.stream().allMatch(x -> x instanceof Number || x instanceof String || x instanceof Boolean);
            if (simple) {
                sb.append('[');
                for (int k = 0; k < a.size(); k++) {
                    if (k > 0) sb.append(", ");
                    write(a.get(k), sb, indent);
                }
                sb.append(']');
            } else {
                sb.append("[\n");
                for (int k = 0; k < a.size(); k++) {
                    sb.append(pad2);
                    write(a.get(k), sb, indent + 1);
                    if (k + 1 < a.size()) sb.append(',');
                    sb.append('\n');
                }
                sb.append(pad).append(']');
            }
            return;
        }
        sb.append('"').append(escape(o.toString())).append('"');
    }

    /** Format a double as plain decimal (no scientific notation), integers without ".0". */
    private static String plainNumber(double d) {
        if (d == 0.0) return "0"; // also collapses -0.0
        if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
            return Long.toString((long) d);
        }
        return java.math.BigDecimal.valueOf(d).toPlainString();
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\t': sb.append("\\t"); break;
                case '\r': sb.append("\\r"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
