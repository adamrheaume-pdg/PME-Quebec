package quebec.culture.donnees;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private static final int OPEN_FILE = 1001;
    private static final int SAVE_FILE = 1002;

    private Spinner sectorSpinner;
    private TextView fileText, statsText, warningsText, previewText;
    private Button normalizeButton, exportButton;
    private List<List<String>> importedRows;
    private DataNormalizer.Report report;
    private String importedName = "catalogue";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(0, 40, 77));
        getWindow().setNavigationBarColor(Color.rgb(0, 40, 77));
        setContentView(buildUi());
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        root.setBackgroundColor(Color.rgb(245, 247, 250));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        TextView title = text("Culture du Québec", 30, Color.rgb(0, 59, 113), true);
        root.addView(title);
        TextView subtitle = text("Données Culture Québec · Normalisateur de catalogues culturels", 16, Color.rgb(21, 37, 54), false);
        subtitle.setPadding(0, dp(4), 0, dp(16));
        root.addView(subtitle);

        LinearLayout program = panel();
        program.addView(text("Programme québécois 2026", 18, Color.rgb(0, 59, 113), true));
        program.addView(text("Prépare et nettoie les données descriptives en vue d'une mise à niveau sectorielle. Cette application n'est pas une application officielle du gouvernement du Québec.", 14, Color.rgb(92, 107, 122), false));
        Button official = secondaryButton("Voir le programme officiel");
        official.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.quebec.ca/culture/aide-financiere/aide-aux-projets-appel/soutien-standardisation-donnees/appel-projets-soutien-standardisation-donnees"))));
        program.addView(official);
        root.addView(program, marginBottom(14));

        LinearLayout sectorPanel = panel();
        sectorPanel.addView(text("1. Choisir la norme sectorielle", 18, Color.rgb(21,37,54), true));
        sectorSpinner = new Spinner(this);
        ArrayAdapter<DataNormalizer.Sector> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, DataNormalizer.Sector.values());
        sectorSpinner.setAdapter(adapter);
        sectorPanel.addView(sectorSpinner, new LinearLayout.LayoutParams(-1, dp(54)));
        sectorPanel.addView(text("Profils disponibles : MétaMusique, Arts de la scène, Cinéma, Livre et Exposition muséale.", 13, Color.rgb(92,107,122), false));
        root.addView(sectorPanel, marginBottom(14));

        LinearLayout importPanel = panel();
        importPanel.addView(text("2. Importer un catalogue", 18, Color.rgb(21,37,54), true));
        fileText = text("Aucun fichier sélectionné", 14, Color.rgb(92,107,122), false);
        fileText.setPadding(0, dp(6), 0, dp(8));
        importPanel.addView(fileText);
        Button importButton = primaryButton("Importer CSV / Excel (.xlsx)");
        importButton.setOnClickListener(v -> openFile());
        importPanel.addView(importButton);
        normalizeButton = secondaryButton("Analyser et normaliser");
        normalizeButton.setEnabled(false);
        normalizeButton.setOnClickListener(v -> normalize());
        importPanel.addView(normalizeButton);
        root.addView(importPanel, marginBottom(14));

        LinearLayout reportPanel = panel();
        reportPanel.addView(text("3. Diagnostic", 18, Color.rgb(21,37,54), true));
        statsText = text("Importe un fichier pour commencer.", 15, Color.rgb(21,37,54), false);
        reportPanel.addView(statsText);
        warningsText = text("", 14, Color.rgb(154,103,0), false);
        warningsText.setPadding(0, dp(8), 0, 0);
        reportPanel.addView(warningsText);
        root.addView(reportPanel, marginBottom(14));

        LinearLayout previewPanel = panel();
        previewPanel.addView(text("4. Aperçu normalisé", 18, Color.rgb(21,37,54), true));
        previewText = text("Les 8 premières lignes apparaîtront ici.", 12, Color.rgb(92,107,122), false);
        previewText.setTypeface(android.graphics.Typeface.MONOSPACE);
        previewText.setTextIsSelectable(true);
        previewPanel.addView(previewText);
        exportButton = primaryButton("Exporter le CSV propre");
        exportButton.setEnabled(false);
        exportButton.setOnClickListener(v -> saveFile());
        previewPanel.addView(exportButton);
        root.addView(previewPanel, marginBottom(14));

        LinearLayout privacy = panel();
        privacy.addView(text("Confidentialité", 18, Color.rgb(21,37,54), true));
        privacy.addView(text("La normalisation est effectuée localement sur l'appareil. Aucun catalogue n'est téléversé par cette version.", 14, Color.rgb(20,108,67), false));
        root.addView(privacy);
        return scroll;
    }

    private void openFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/plain"});
        startActivityForResult(i, OPEN_FILE);
    }

    private void normalize() {
        if (importedRows == null) return;
        DataNormalizer.Sector sector = (DataNormalizer.Sector) sectorSpinner.getSelectedItem();
        report = DataNormalizer.normalize(importedRows, sector);
        statsText.setText("Lignes : " + report.rows + "\nColonnes : " + report.columns + "\nCellules normalisées : " + report.changedCells + "\nCellules vides : " + report.emptyCells + "\nDoublons détectés : " + report.duplicateRows);
        warningsText.setText(report.warnings.isEmpty() ? "Aucun avertissement structurel de base." : "• " + String.join("\n• ", report.warnings));
        previewText.setText(makePreview(report.cleaned, 8));
        exportButton.setEnabled(!report.cleaned.isEmpty());
        getPreferences(MODE_PRIVATE).edit().putLong("last_run", System.currentTimeMillis()).apply();
    }

    private void saveFile() {
        if (report == null) return;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/csv");
        String base = importedName.replaceAll("(?i)\\.(csv|xlsx)$", "");
        i.putExtra(Intent.EXTRA_TITLE, base + "-normalise.csv");
        startActivityForResult(i, SAVE_FILE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == OPEN_FILE) {
            importedName = displayName(uri);
            try {
                importedRows = SpreadsheetReader.read(getContentResolver(), uri, importedName);
                fileText.setText(importedName + " · " + Math.max(0, importedRows.size() - 1) + " ligne(s) de données détectée(s)");
                normalizeButton.setEnabled(true);
                statsText.setText("Fichier chargé. Appuie sur « Analyser et normaliser ».");
                warningsText.setText("");
                previewText.setText(makePreview(importedRows, 5));
                exportButton.setEnabled(false);
            } catch (Exception e) {
                importedRows = null;
                normalizeButton.setEnabled(false);
                new AlertDialog.Builder(this).setTitle("Import impossible").setMessage(e.getMessage()).setPositiveButton("OK", null).show();
            }
        } else if (requestCode == SAVE_FILE) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new Exception("Impossible de créer le fichier.");
                out.write(SpreadsheetReader.toCsv(report.cleaned).getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, "CSV normalisé exporté.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                new AlertDialog.Builder(this).setTitle("Export impossible").setMessage(e.getMessage()).setPositiveButton("OK", null).show();
            }
        }
    }

    private String displayName(Uri uri) {
        String name = "catalogue";
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) name = c.getString(0);
        } catch (Exception ignored) {}
        return name == null ? "catalogue" : name;
    }

    private String makePreview(List<List<String>> rows, int maxRows) {
        StringBuilder b = new StringBuilder();
        for (int r = 0; r < Math.min(rows.size(), maxRows); r++) {
            List<String> row = rows.get(r);
            for (int c = 0; c < Math.min(row.size(), 6); c++) {
                if (c > 0) b.append("  |  ");
                String v = row.get(c);
                if (v.length() > 26) v = v.substring(0, 23) + "…";
                b.append(v);
            }
            if (row.size() > 6) b.append("  | …");
            b.append('\n');
        }
        if (rows.size() > maxRows) b.append("…");
        return b.toString();
    }

    private LinearLayout panel() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(16), dp(16), dp(16), dp(16));
        p.setBackgroundResource(quebec.culture.donnees.R.drawable.panel);
        return p;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        t.setLineSpacing(0, 1.12f);
        return t;
    }

    private Button primaryButton(String label) {
        Button b = new Button(this);
        b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(15); b.setAllCaps(false);
        b.setBackgroundResource(quebec.culture.donnees.R.drawable.button_primary);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(10); b.setLayoutParams(lp);
        return b;
    }

    private Button secondaryButton(String label) {
        Button b = new Button(this);
        b.setText(label); b.setTextColor(Color.rgb(0,59,113)); b.setTextSize(15); b.setAllCaps(false);
        b.setBackgroundResource(quebec.culture.donnees.R.drawable.button_secondary);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(10); b.setLayoutParams(lp);
        return b;
    }

    private LinearLayout.LayoutParams marginBottom(int dp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(dp); return p;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
