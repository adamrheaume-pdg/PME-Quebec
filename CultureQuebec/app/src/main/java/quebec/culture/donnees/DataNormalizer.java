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
        Sector(String label, String[] suggestedFields){this.label=label;this.suggestedFields=suggestedFields;}
        @Override public String toString(){return label;}
    }

    public static final class ColumnMapping {
        public String sourceHeader="";
        public String normalizedHeader="";
        public String targetField="";
        public String sampleBefore="";
        public String sampleAfter="";
        public String status="";
        public String reason="";
    }

    public static final class Report {
        public int rows, columns, emptyCells, changedCells, duplicateRows;
        public final List<String> warnings=new ArrayList<>();
        public final List<List<String>> cleaned=new ArrayList<>();
        public final List<ColumnMapping> mappings=new ArrayList<>();
    }

    private static final Set<String> DATE_HEADERS=Set.of("date","date_debut","date_fin","publication","sortie");
    private static final Set<String> CITY_HEADERS=Set.of("ville","municipalite","municipalité");
    private static final Set<String> LANG_HEADERS=Set.of("langue","language");
    private DataNormalizer(){}

    public static Report normalize(List<List<String>> input,Sector sector){
        Report r=new Report();
        if(input==null||input.isEmpty()){r.warnings.add("Le fichier ne contient aucune ligne exploitable.");return r;}

        List<String> originalHeader=input.get(0);
        List<String> header=new ArrayList<>();
        Set<String> seenHeaders=new HashSet<>();
        for(int i=0;i<originalHeader.size();i++){
            String h=normalizeHeader(originalHeader.get(i));
            if(h.isEmpty())h="champ_"+(i+1);
            String base=h;int n=2;while(!seenHeaders.add(h))h=base+"_"+n++;
            header.add(h);
            ColumnMapping m=new ColumnMapping();
            m.sourceHeader=originalHeader.get(i)==null?"":originalHeader.get(i).trim();
            m.normalizedHeader=h;
            m.targetField=matchTarget(h,sector);
            if(m.targetField.equals(h)){
                m.status="CONFORME";
                m.reason="Correspondance directe avec un champ du profil sectoriel choisi.";
            }else if(!m.targetField.isEmpty()){
                m.status="À VALIDER";
                m.reason="Correspondance proposée automatiquement; une validation humaine est recommandée.";
            }else{
                m.status="HORS PROFIL";
                m.reason="Aucune correspondance fiable trouvée dans le profil sectoriel sélectionné.";
            }
            r.mappings.add(m);
        }
        r.columns=header.size();
        r.cleaned.add(header);

        Set<String> expected=new LinkedHashSet<>(Arrays.asList(sector.suggestedFields));
        for(String field:expected){
            boolean found=false;
            for(ColumnMapping m:r.mappings)if(field.equals(m.targetField)){found=true;break;}
            if(!found)r.warnings.add("Champ sectoriel suggéré absent : "+field);
        }

        Set<String> signatures=new HashSet<>();
        for(int rowIndex=1;rowIndex<input.size();rowIndex++){
            List<String> row=input.get(rowIndex);List<String> out=new ArrayList<>();
            for(int col=0;col<header.size();col++){
                String value=col<row.size()?row.get(col):"";String before=value==null?"":value;String after=normalizeValue(header.get(col),before);
                if(!before.equals(after))r.changedCells++;if(after.isBlank())r.emptyCells++;out.add(after);
                ColumnMapping m=r.mappings.get(col);
                if(m.sampleBefore.isEmpty()&&!before.isBlank())m.sampleBefore=before;
                if(m.sampleAfter.isEmpty()&&!after.isBlank())m.sampleAfter=after;
            }
            if(out.stream().allMatch(String::isBlank))continue;
            String sig=String.join("\u001f",out).toLowerCase(Locale.CANADA_FRENCH);if(!signatures.add(sig))r.duplicateRows++;
            r.cleaned.add(out);r.rows++;
        }
        if(r.duplicateRows>0)r.warnings.add(r.duplicateRows+" ligne(s) dupliquée(s) détectée(s). Elles sont conservées pour validation humaine.");
        if(r.emptyCells>0)r.warnings.add(r.emptyCells+" cellule(s) vide(s) à réviser.");
        return r;
    }

    private static String matchTarget(String h,Sector sector){
        for(String f:sector.suggestedFields)if(f.equals(h))return f;
        Map<String,String> broad=new LinkedHashMap<>();
        broad.put("nom","titre");broad.put("nom_oeuvre","titre");broad.put("oeuvre","titre");broad.put("creator","artiste");broad.put("interprete","artiste");
        broad.put("director","realisateur");broad.put("cinéaste","realisateur");broad.put("auteure","auteur");broad.put("maison_edition","editeur");
        broad.put("municipality","ville");broad.put("location","lieu");broad.put("venue","lieu");broad.put("type","categorie");broad.put("category","categorie");
        broad.put("release_date","date");broad.put("publication_date","date");broad.put("start_date","date_debut");broad.put("end_date","date_fin");broad.put("year","annee");
        String proposed=broad.getOrDefault(h,"");
        if(!proposed.isEmpty()&&Arrays.asList(sector.suggestedFields).contains(proposed))return proposed;
        return "";
    }

    public static String normalizeHeader(String value){
        if(value==null)return "";String s=value.trim().toLowerCase(Locale.CANADA_FRENCH);
        s=Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}+","");
        s=s.replace('&',' ').replaceAll("[^a-z0-9]+","_").replaceAll("^_+|_+$","");
        Map<String,String> aliases=new HashMap<>();
        aliases.put("nom_artiste","artiste");aliases.put("artist","artiste");aliases.put("author","auteur");aliases.put("publisher","editeur");
        aliases.put("realisateur","realisateur");aliases.put("municipalite","ville");aliases.put("city","ville");aliases.put("title","titre");
        aliases.put("category","categorie");aliases.put("language","langue");return aliases.getOrDefault(s,s);
    }

    public static String normalizeValue(String header,String value){
        String s=value==null?"":value.trim().replaceAll("\\s+"," ");if(s.isEmpty())return "";
        if(DATE_HEADERS.contains(header))return normalizeDate(s);if(CITY_HEADERS.contains(header))return titleCase(s);if(LANG_HEADERS.contains(header))return normalizeLanguage(s);
        if(header.contains("isbn"))return s.replaceAll("[^0-9Xx]","").toUpperCase(Locale.CANADA_FRENCH);if(header.equals("annee"))return s.replaceAll("[^0-9]","");
        if(header.equals("province")&&s.equalsIgnoreCase("qc"))return "Québec";return s;
    }
    private static String normalizeLanguage(String s){String x=s.toLowerCase(Locale.CANADA_FRENCH);if(Set.of("fr","fra","fre","français","francais").contains(x))return "fr";if(Set.of("en","eng","anglais","english").contains(x))return "en";return x;}
    private static String normalizeDate(String s){List<DateTimeFormatter> formats=List.of(DateTimeFormatter.ISO_LOCAL_DATE,DateTimeFormatter.ofPattern("d/M/uuuu"),DateTimeFormatter.ofPattern("d-M-uuuu"),DateTimeFormatter.ofPattern("uuuu/M/d"));for(DateTimeFormatter f:formats){try{return LocalDate.parse(s,f).toString();}catch(DateTimeParseException ignored){}}return s;}
    private static String titleCase(String input){String[] words=input.toLowerCase(Locale.CANADA_FRENCH).split(" ");StringBuilder b=new StringBuilder();for(String w:words){if(w.isEmpty())continue;if(b.length()>0)b.append(' ');b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));}return b.toString();}
}
