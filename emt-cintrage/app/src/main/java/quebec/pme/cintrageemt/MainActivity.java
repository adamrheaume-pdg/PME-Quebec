package quebec.pme.cintrageemt;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private Spinner operationSpinner, sizeSpinner, benderSpinner, angleSpinner;
    private EditText inputA, inputB;
    private TextView labelA, labelB, result, sizeNote;

    private static final String[] OPERATIONS = {
            "90° (stub-up)",
            "Dos-à-dos 90°",
            "Offset",
            "Dos d'âne 3 points"
    };

    private static final String[] SIZES = {"1/2 po", "3/4 po", "1 po", "1-1/4 po", "1-1/2 po", "2 po"};
    private static final String[] BENDERS = {
            "Klein manuel (1/2 à 1-1/4)",
            "Greenlee 1818 mécanique (3/4 à 2)"
    };
    private static final String[] ANGLES = {"10°", "22.5°", "30°", "45°", "60°"};

    private final Map<String, Double> kleinTakeUp = new HashMap<>();
    private final Map<String, Double> greenleeTakeUp = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        kleinTakeUp.put("1/2 po", 5.0);
        kleinTakeUp.put("3/4 po", 6.0);
        kleinTakeUp.put("1 po", 8.0);
        kleinTakeUp.put("1-1/4 po", 11.0);

        // EMT deduct values from the Greenlee 1818 mechanical bender chart.
        greenleeTakeUp.put("3/4 po", 8.6875);
        greenleeTakeUp.put("1 po", 10.25);
        greenleeTakeUp.put("1-1/4 po", 12.625);
        greenleeTakeUp.put("1-1/2 po", 12.9375);
        greenleeTakeUp.put("2 po", 15.0);

        buildUi();
        updateFields();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        root.setBackgroundColor(Color.rgb(245, 249, 255));
        scroll.addView(root);

        TextView header = new TextView(this);
        header.setText("CINTRAGE EMT QUÉBEC");
        header.setTextSize(25);
        header.setTextColor(Color.WHITE);
        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(12), dp(18), dp(12), dp(18));
        header.setBackgroundColor(Color.rgb(0, 91, 187));
        root.addView(header, matchWrap());

        TextView sub = new TextView(this);
        sub.setText("Calculateur hors ligne • EMT 1/2 po à 2 po");
        sub.setTextSize(15);
        sub.setTextColor(Color.rgb(35, 55, 75));
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(12), 0, dp(12));
        root.addView(sub, matchWrap());

        operationSpinner = addSpinner(root, "Type de cintrage", OPERATIONS);
        sizeSpinner = addSpinner(root, "Grosseur EMT", SIZES);
        benderSpinner = addSpinner(root, "Profil de cintreuse", BENDERS);
        angleSpinner = addSpinner(root, "Angle", ANGLES);

        sizeNote = new TextView(this);
        sizeNote.setTextSize(13);
        sizeNote.setTextColor(Color.rgb(85, 85, 85));
        sizeNote.setPadding(dp(6), dp(4), dp(6), dp(10));
        root.addView(sizeNote, matchWrap());

        labelA = fieldLabel(root, "Mesure A");
        inputA = field(root, "Ex.: 12 3/8");
        labelB = fieldLabel(root, "Mesure B");
        inputB = field(root, "Ex.: 20");

        TextView hint = new TextView(this);
        hint.setText("Entrée acceptée : 12, 12.5, 12 3/8 ou 3/8 (en pouces).");
        hint.setTextSize(12);
        hint.setTextColor(Color.DKGRAY);
        hint.setPadding(dp(4), dp(4), dp(4), dp(10));
        root.addView(hint, matchWrap());

        Button calculate = new Button(this);
        calculate.setText("CALCULER LES MARQUES");
        calculate.setTextSize(16);
        calculate.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        calculate.setTextColor(Color.WHITE);
        calculate.setBackgroundColor(Color.rgb(0, 91, 187));
        calculate.setPadding(dp(8), dp(12), dp(8), dp(12));
        calculate.setOnClickListener(v -> calculate());
        LinearLayout.LayoutParams bp = matchWrap();
        bp.setMargins(0, dp(8), 0, dp(12));
        root.addView(calculate, bp);

        result = new TextView(this);
        result.setTextSize(16);
        result.setTextColor(Color.rgb(20, 35, 50));
        result.setPadding(dp(16), dp(16), dp(16), dp(16));
        result.setBackgroundColor(Color.WHITE);
        root.addView(result, matchWrap());

        TextView safety = new TextView(this);
        safety.setText("Référence de calcul seulement. Utiliser la bonne cintreuse pour la grosseur du conduit et vérifier les repères inscrits sur l'outil du fabricant. Ne jamais travailler sur une canalisation sous tension.");
        safety.setTextSize(12);
        safety.setTextColor(Color.rgb(90, 90, 90));
        safety.setPadding(dp(4), dp(18), dp(4), dp(6));
        root.addView(safety, matchWrap());

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { updateFields(); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        operationSpinner.setOnItemSelectedListener(listener);
        sizeSpinner.setOnItemSelectedListener(listener);
        benderSpinner.setOnItemSelectedListener(listener);

        setContentView(scroll);
    }

    private void updateFields() {
        if (operationSpinner == null) return;
        String op = (String) operationSpinner.getSelectedItem();
        String size = (String) sizeSpinner.getSelectedItem();
        String bender = (String) benderSpinner.getSelectedItem();

        boolean needsAngle = op.equals("Offset") || op.equals("Dos d'âne 3 points");
        angleSpinner.setVisibility(needsAngle ? View.VISIBLE : View.GONE);

        if (op.equals("90° (stub-up)")) {
            labelA.setText("Hauteur finale voulue du 90° (po)");
            labelB.setText("Mesure B (non utilisée)");
            inputB.setVisibility(View.GONE);
            labelB.setVisibility(View.GONE);
        } else if (op.equals("Dos-à-dos 90°")) {
            labelA.setText("Distance entre les deux surfaces / dos de 90° (po)");
            labelB.setText("Hauteur voulue du premier 90° (po)");
            inputB.setVisibility(View.VISIBLE);
            labelB.setVisibility(View.VISIBLE);
        } else if (op.equals("Offset")) {
            labelA.setText("Hauteur de l'offset (po)");
            labelB.setText("Distance du bout du conduit à l'obstacle (po)");
            inputB.setVisibility(View.VISIBLE);
            labelB.setVisibility(View.VISIBLE);
        } else {
            labelA.setText("Hauteur de l'obstacle / dos d'âne (po)");
            labelB.setText("Distance du bout au centre de l'obstacle (po)");
            inputB.setVisibility(View.VISIBLE);
            labelB.setVisibility(View.VISIBLE);
        }

        boolean supported = getTakeUp(size, bender) != null;
        sizeNote.setText(supported
                ? "Profil compatible pour le 90° : déduction connue pour " + size + "."
                : "Attention : ce profil ne contient pas de déduction 90° fabricant pour " + size + ". Choisis l'autre profil ou vérifie la valeur inscrite sur ta cintreuse.");
    }

    private void calculate() {
        try {
            String op = (String) operationSpinner.getSelectedItem();
            String size = (String) sizeSpinner.getSelectedItem();
            String bender = (String) benderSpinner.getSelectedItem();
            double a = parseInches(inputA.getText().toString());
            double b = inputB.getVisibility() == View.VISIBLE ? parseInches(inputB.getText().toString()) : 0.0;

            String out;
            if (op.equals("90° (stub-up)")) {
                Double takeUp = getTakeUp(size, bender);
                if (takeUp == null) {
                    out = "Aucune déduction 90° confirmée dans ce profil pour " + size + ".\n\nUtilise la valeur 'stub/take-up/deduct' indiquée sur la cintreuse du fabricant.";
                } else if (a <= takeUp) {
                    out = "La hauteur demandée est trop courte pour cette déduction.\n\nDéduction du profil : " + fmt(takeUp) + " po.";
                } else {
                    double mark = a - takeUp;
                    out = "90° — " + size + "\n\n" +
                            "Hauteur voulue : " + fmt(a) + " po\n" +
                            "Déduction : " + fmt(takeUp) + " po\n" +
                            "MARQUE À PARTIR DU BOUT : " + fmt(mark) + " po\n\n" +
                            "Repère de cintreuse : FLÈCHE sur la marque.\n" +
                            "Formule : hauteur voulue − déduction.";
                }
            } else if (op.equals("Dos-à-dos 90°")) {
                Double takeUp = getTakeUp(size, bender);
                String first90 = "";
                if (takeUp != null && b > 0 && b > takeUp) {
                    first90 = "Premier 90° : marque à " + fmt(b - takeUp) + " po du bout (flèche).\n\n";
                }
                out = "DOS-À-DOS 90° — " + size + "\n\n" + first90 +
                        "Après le premier 90°, mesure " + fmt(a) + " po depuis le DOS du premier coude.\n" +
                        "Fais la 2e marque à cet endroit.\n\n" +
                        "Repère de cintreuse pour le deuxième 90° : ÉTOILE sur la 2e marque.";
            } else if (op.equals("Offset")) {
                double angle = selectedAngle();
                double multiplier = multiplier(angle);
                double shrinkPerInch = shrink(angle);
                double totalShrink = a * shrinkPerInch;
                double firstMark = b + totalShrink;
                double spacing = a * multiplier;
                double secondMark = firstMark + spacing;
                out = "OFFSET " + trimAngle(angle) + " — " + size + "\n\n" +
                        "Hauteur offset : " + fmt(a) + " po\n" +
                        "Rétrécissement total : " + fmt(totalShrink) + " po\n" +
                        "1re MARQUE : " + fmt(firstMark) + " po du bout\n" +
                        "Distance entre marques : " + fmt(spacing) + " po\n" +
                        "2e MARQUE : " + fmt(secondMark) + " po du bout\n\n" +
                        "Repère : FLÈCHE sur chaque marque.\n" +
                        "Multiplier : " + nice(multiplier) + " • shrink/po : " + fmt(shrinkPerInch) + " po.";
            } else {
                double angle = selectedAngle();
                if (angle != 45.0 && angle != 60.0) {
                    out = "Pour le dos d'âne 3 points, sélectionne 45° ou 60° comme angle central.";
                } else {
                    double shrinkPer = angle == 45.0 ? 0.1875 : 0.25;
                    double sideMultiplier = angle == 45.0 ? 2.5 : 2.0;
                    double totalShrink = a * shrinkPer;
                    double center = b + totalShrink;
                    double sideDistance = a * sideMultiplier;
                    double first = center - sideDistance;
                    double third = center + sideDistance;
                    double returnAngle = angle / 2.0;
                    out = "DOS D'ÂNE 3 POINTS — centre " + trimAngle(angle) + "\n\n" +
                            "Hauteur obstacle : " + fmt(a) + " po\n" +
                            "Rétrécissement : " + fmt(totalShrink) + " po\n" +
                            "MARQUE CENTRALE : " + fmt(center) + " po du bout\n" +
                            "Distance de chaque côté : " + fmt(sideDistance) + " po\n" +
                            "1re MARQUE : " + fmt(first) + " po\n" +
                            "3e MARQUE : " + fmt(third) + " po\n\n" +
                            "Angles : " + trimAngle(returnAngle) + " / " + trimAngle(angle) + " / " + trimAngle(returnAngle) + ".\n" +
                            "Repère : encoche/centre de coude sur la marque centrale; flèche sur les deux marques extérieures.";
                }
            }
            result.setText(out);
        } catch (Exception e) {
            result.setText("Entre des mesures valides en pouces. Exemples : 12, 12.5, 12 3/8 ou 3/8.");
        }
    }

    private Double getTakeUp(String size, String bender) {
        return bender.startsWith("Klein") ? kleinTakeUp.get(size) : greenleeTakeUp.get(size);
    }

    private double selectedAngle() {
        String s = (String) angleSpinner.getSelectedItem();
        return Double.parseDouble(s.replace("°", ""));
    }

    private static double multiplier(double angle) {
        if (angle == 10.0) return 6.0;
        if (angle == 22.5) return 2.6;
        if (angle == 30.0) return 2.0;
        if (angle == 45.0) return 1.4;
        return 1.2;
    }

    private static double shrink(double angle) {
        if (angle == 10.0) return 0.0625;
        if (angle == 22.5) return 0.1875;
        if (angle == 30.0) return 0.25;
        if (angle == 45.0) return 0.375;
        return 0.5;
    }

    private static double parseInches(String text) {
        String s = text.trim().replace(',', '.');
        if (s.isEmpty()) throw new IllegalArgumentException();
        if (s.contains(" ")) {
            String[] p = s.split("\\s+");
            double whole = Double.parseDouble(p[0]);
            if (p.length > 1 && p[1].contains("/")) return whole + parseFraction(p[1]);
            return whole;
        }
        if (s.contains("/")) return parseFraction(s);
        return Double.parseDouble(s);
    }

    private static double parseFraction(String s) {
        String[] f = s.split("/");
        if (f.length != 2) throw new IllegalArgumentException();
        return Double.parseDouble(f[0]) / Double.parseDouble(f[1]);
    }

    private static String fmt(double value) {
        boolean neg = value < 0;
        value = Math.abs(value);
        int whole = (int) Math.floor(value + 1e-9);
        int sixteenth = (int) Math.round((value - whole) * 16.0);
        if (sixteenth == 16) { whole++; sixteenth = 0; }
        if (sixteenth == 0) return (neg ? "-" : "") + whole;
        int gcd = gcd(sixteenth, 16);
        int n = sixteenth / gcd;
        int d = 16 / gcd;
        String frac = n + "/" + d;
        if (whole == 0) return (neg ? "-" : "") + frac;
        return (neg ? "-" : "") + whole + " " + frac;
    }

    private static int gcd(int a, int b) {
        while (b != 0) { int t = a % b; a = b; b = t; }
        return a;
    }

    private static String nice(double d) {
        return String.format(Locale.CANADA_FRENCH, "%.3g", d);
    }

    private static String trimAngle(double d) {
        if (d == Math.rint(d)) return ((int) d) + "°";
        return nice(d) + "°";
    }

    private Spinner addSpinner(LinearLayout root, String title, String[] items) {
        fieldLabel(root, title);
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(adapter);
        spinner.setBackgroundColor(Color.WHITE);
        spinner.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams p = matchWrap();
        p.setMargins(0, 0, 0, dp(10));
        root.addView(spinner, p);
        return spinner;
    }

    private TextView fieldLabel(LinearLayout root, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tv.setTextColor(Color.rgb(0, 59, 122));
        tv.setPadding(dp(2), dp(6), dp(2), dp(4));
        root.addView(tv, matchWrap());
        return tv;
    }

    private EditText field(LinearLayout root, String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(18);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setSingleLine(true);
        e.setBackgroundColor(Color.WHITE);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams p = matchWrap();
        p.setMargins(0, 0, 0, dp(8));
        root.addView(e, p);
        return e;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
