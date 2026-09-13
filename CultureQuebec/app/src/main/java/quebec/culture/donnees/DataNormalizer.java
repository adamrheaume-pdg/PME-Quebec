package quebec.culture.donnees;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class DataNormalizer {
    public enum Sector {
        METAMUSIQUE("MétaMusique", new String[]{"titre", "artiste", "album", "genre", "date", "identifiant", "langue"}),
        SCENE("Arts de la scène", new String[]{"titre", "artiste", "discipline", "lieu", "ville", "date", "langue"}),
        CINEMA("Cinéma", new String[]{"titre", "realisateur", "annee", "genre", "langue", "pays", "identifiant"}),
        LIVRE("Livre — éditeurs", new String[]{"titre", "auteur", "editeur", "isbn", "date", "langue", "categorie"}),
        MUSEE("Exposition muséale", new String[]{"titre", "institution", "lieu", "ville", "date_debut", "date_fin", "categorie"});

        public final String label;
        public final String[] suggestedFields;
        Sector(String label, String[] suggestedFields) {
            this.label = label;
            this.suggestedFields = suggestedFields;
        }
        @Override public String toString() { return label; }
    }

    public static final class Report {
        public int rows;
        public int columns;
        public int emptyCells;
        public int changedCells;
        public int duplicateRows;
        public final List<String> warnings = new ArrayList<>();
        public final List<List<String>> cleaned = new ArrayList<>();
    }

    private static final Set<String> DATE_HEADERS = Set.of("date", "date_debut", "date_fin", "publication", "sortie");
    private static final Set<String> CITY_HEADERS = Set.of("ville", "municipalite", "municipalité");
    private static final Set<String> LANG_HEADERS = Set.of("langue", "language");

    private DataNormalizer() {}

    public static Report normalize(List<List<String>> input, Sector sector) {
        Report r = new Report();
        if (input == null || input.isEmpty()) {
            r.warnings.add("Le fichier ne contient aucune ligne exploitable.");
            return r;
        }

        List<String> originalHeader = input.get(0);
        List<String> header = new ArrayList<>();
        Set<String> seenHeaders = new HashSet<>();
        for (int i = 0; i < originalHeader.size(); i++) {
            String h = normalizeHeader(originalHeader.get(i));
            if (h.isEmpty()) h = "champ_" + (i + 1);
            String base = h;
            int n = 2;
            while (!seenHeaders.add(h)) h = base + "_" + n++;
            header.add(h);
        }
        r.columns = header.size();
        r.cleaned.add(header);

        Set<String> expected = new LinkedHashSet<>(Arrays.asList(sector.suggestedFields));
        for (String field : expected) {
            if (!header.contains(field)) r.warnings.add("Champ sectoriel suggéré absent : " + field);
        }

        Set<String> signatures = new HashSet<>();
        for (int rowIndex = 1; rowIndex < input.size(); rowIndex++) {
            List<String> row = input.get(rowIndex);
            List<String> out = new ArrayList<>();
            for (int col = 0; col < header.size(); col++) {
                String value = col < row.size() ? row.get(col) : "";
                String before = value == null ? "" : value;
                String after = normalizeValue(header.get(col), before);
                if (!before.equals(after)) r.changedCells++;
                if (after.isBlank()) r.emptyCells++;
                out.add(after);
            }
            if (out.stream().allMatch(String::isBlank)) continue;
            String sig = String.join("\u001f", out).toLowerCase(Locale.CANADA_FRENCH);
            if (!signatures.add(sig)) r.duplicateRows++;
            r.cleaned.add(out);
            r.rows++;
        }

        if (r.duplicateRows > 0) r.warnings.add(r.duplicateRows + " ligne(s) dupliquée(s) détectée(s). Elles sont conservées pour validation humaine.");
        if (r.emptyCells > 0) r.warnings.add(r.emptyCells + " cellule(s) vide(s) à réviser.");
        return r;
    }

    public static String normalizeHeader(String value) {
        if (value == null) return "";
        String s = value.trim().toLowerCase(Locale.CANADA_FRENCH);
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.replace('&', ' ').replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        Map<String,String> aliases = new HashMap<>();
        aliases.put("nom_artiste", "artiste");
        aliases.put("artist", "artiste");
        aliases.put("author", "auteur");
        aliases.put("publisher", "editeur");
        aliases.put("realisateur", "realisateur");
        aliases.put("municipalite", "ville");
        aliases.put("city", "ville");
        aliases.put("title", "titre");
        aliases.put("category", "categorie");
        aliases.put("language", "langue");
        return aliases.getOrDefault(s, s);
    }

    public static String normalizeValue(String header, String value) {
        String s = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (s.isEmpty()) return "";
        if (DATE_HEADERS.contains(header)) return normalizeDate(s);
        if (CITY_HEADERS.contains(header)) return titleCase(s);
        if (LANG_HEADERS.contains(header)) return normalizeLanguage(s);
        if (header.contains("isbn")) return s.replaceAll("[^0-9Xx]", "").toUpperCase(Locale.CANADA_FRENCH);
        if (header.equals("annee")) return s.replaceAll("[^0-9]", "");
        if (header.equals("province") && s.equalsIgnoreCase("qc")) return "Québec";
        return s;
    }

    private static String normalizeLanguage(String s) {
        String x = s.toLowerCase(Locale.CANADA_FRENCH);
        if (Set.of("fr", "fra", "fre", "français", "francais").contains(x)) return "fr";
        if (Set.of("en", "eng", "anglais", "english").contains(x)) return "en";
        return x;
    }

    private static String normalizeDate(String s) {
        List<DateTimeFormatter> formats = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("d-M-uuuu"),
                DateTimeFormatter.ofPattern("uuuu/M/d"));
        for (DateTimeFormatter f : formats) {
            try { return LocalDate.parse(s, f).toString(); }
            catch (DateTimeParseException ignored) {}
        }
        return s;
    }

    private static String titleCase(String input) {
        String[] words = input.toLowerCase(Locale.CANADA_FRENCH).split(" ");
        StringBuilder b = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return b.toString();
    }
}
