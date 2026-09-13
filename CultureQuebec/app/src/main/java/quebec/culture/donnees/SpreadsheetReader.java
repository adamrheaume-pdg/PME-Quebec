package quebec.culture.donnees;

import android.content.ContentResolver;
import android.net.Uri;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class SpreadsheetReader {
    private SpreadsheetReader() {}

    public static List<List<String>> read(ContentResolver resolver, Uri uri, String name) throws Exception {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) throw new IOException("Impossible d'ouvrir le fichier.");
            if (lower.endsWith(".xlsx")) return readXlsx(in);
            return readCsv(in);
        }
    }

    public static List<List<String>> readCsv(InputStream in) throws IOException {
        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        char sep = guessSeparator(text);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < text.length() && text.charAt(i + 1) == '"') { cell.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == sep && !quoted) {
                row.add(cell.toString()); cell.setLength(0);
            } else if ((c == '\n' || c == '\r') && !quoted) {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(cell.toString()); cell.setLength(0);
                if (!row.isEmpty()) rows.add(row);
                row = new ArrayList<>();
            } else cell.append(c);
        }
        if (cell.length() > 0 || !row.isEmpty()) { row.add(cell.toString()); rows.add(row); }
        return rows;
    }

    private static char guessSeparator(String text) {
        int end = text.indexOf('\n');
        String first = end >= 0 ? text.substring(0, end) : text;
        long commas = first.chars().filter(c -> c == ',').count();
        long semis = first.chars().filter(c -> c == ';').count();
        long tabs = first.chars().filter(c -> c == '\t').count();
        return tabs > commas && tabs > semis ? '\t' : (semis > commas ? ';' : ',');
    }

    public static List<List<String>> readXlsx(InputStream in) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zin = new ZipInputStream(in)) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                String n = e.getName();
                if (n.equals("xl/sharedStrings.xml") || n.equals("xl/worksheets/sheet1.xml")) {
                    entries.put(n, zin.readAllBytes());
                }
            }
        }
        byte[] sheet = entries.get("xl/worksheets/sheet1.xml");
        if (sheet == null) throw new IOException("Aucune première feuille Excel détectée.");
        List<String> shared = entries.containsKey("xl/sharedStrings.xml") ? parseSharedStrings(entries.get("xl/sharedStrings.xml")) : List.of();
        return parseSheet(sheet, shared);
    }

    private static List<String> parseSharedStrings(byte[] xml) throws Exception {
        List<String> values = new ArrayList<>();
        XmlPullParser p = newParser(xml);
        StringBuilder current = null;
        int event;
        while ((event = p.next()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && p.getName().equals("si")) current = new StringBuilder();
            else if (event == XmlPullParser.START_TAG && p.getName().equals("t") && current != null) current.append(p.nextText());
            else if (event == XmlPullParser.END_TAG && p.getName().equals("si") && current != null) { values.add(current.toString()); current = null; }
        }
        return values;
    }

    private static List<List<String>> parseSheet(byte[] xml, List<String> shared) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        XmlPullParser p = newParser(xml);
        List<String> row = null;
        String type = null;
        int cellIndex = 0;
        int event;
        while ((event = p.next()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && p.getName().equals("row")) { row = new ArrayList<>(); cellIndex = 0; }
            else if (event == XmlPullParser.START_TAG && p.getName().equals("c") && row != null) {
                String ref = p.getAttributeValue(null, "r");
                type = p.getAttributeValue(null, "t");
                int target = ref == null ? cellIndex : columnIndex(ref);
                while (row.size() < target) row.add("");
                cellIndex = target;
            } else if (event == XmlPullParser.START_TAG && (p.getName().equals("v") || p.getName().equals("t")) && row != null) {
                String v = p.nextText();
                if ("s".equals(type)) {
                    try { int idx = Integer.parseInt(v); v = idx >= 0 && idx < shared.size() ? shared.get(idx) : v; } catch (NumberFormatException ignored) {}
                }
                while (row.size() <= cellIndex) row.add("");
                row.set(cellIndex, v);
            } else if (event == XmlPullParser.END_TAG && p.getName().equals("c")) cellIndex++;
            else if (event == XmlPullParser.END_TAG && p.getName().equals("row") && row != null) { rows.add(row); row = null; }
        }
        return rows;
    }

    private static XmlPullParser newParser(byte[] xml) throws Exception {
        XmlPullParserFactory f = XmlPullParserFactory.newInstance();
        f.setNamespaceAware(false);
        XmlPullParser p = f.newPullParser();
        p.setInput(new ByteArrayInputStream(xml), "UTF-8");
        return p;
    }

    private static int columnIndex(String ref) {
        int n = 0;
        for (int i = 0; i < ref.length(); i++) {
            char c = ref.charAt(i);
            if (!Character.isLetter(c)) break;
            n = n * 26 + (Character.toUpperCase(c) - 'A' + 1);
        }
        return Math.max(0, n - 1);
    }

    public static String toCsv(List<List<String>> rows) {
        StringBuilder out = new StringBuilder("\uFEFF");
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) out.append(',');
                String value = row.get(i) == null ? "" : row.get(i);
                boolean quote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
                if (quote) out.append('"').append(value.replace("\"", "\"\"")).append('"');
                else out.append(value);
            }
            out.append("\r\n");
        }
        return out.toString();
    }
}
