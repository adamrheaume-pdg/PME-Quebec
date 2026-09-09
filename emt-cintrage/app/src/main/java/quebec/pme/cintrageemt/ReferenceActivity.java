package quebec.pme.cintrageemt;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ReferenceActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(28));
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
        sub.setText("Guide théorique • EMT 1/2 po à 2 po • Références fabricants");
        sub.setTextSize(15);
        sub.setTextColor(Color.rgb(35, 55, 75));
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(12), 0, dp(14));
        root.addView(sub, matchWrap());

        addSection(root, "FORMES DE CINTRAGE",
                "• 90° / stub-up\n• Dos-à-dos 90°\n• Offset\n• Dos d'âne / selle 3 points\n• Selle 4 points\n• Coudes parallèles\n\nCette section sert à reconnaître la géométrie et le vocabulaire de chaque type de cintrage.");

        addSection(root, "REPÈRES DE CINTREUSE",
                "FLÈCHE — repère de référence présent sur plusieurs cintreuses manuelles.\n\nÉTOILE — repère présent sur certains modèles pour certains retours et dos-à-dos.\n\nENCHE / CENTRE — repère du centre du coude sur certains modèles.\n\nGRADUATIONS D'ANGLE — indiquent les angles prévus par le fabricant.\n\nLa signification exacte des symboles varie selon la marque et le modèle.");

        addManufacturer(root, "GREENLEE",
                "Familles courantes : SITE-RITE manuelles, 1818 mécaniques et 555 électriques. Les capacités EMT dépendent du modèle, du sabot et des accessoires.\n\nUtiliser la charte officielle correspondant exactement au numéro de modèle et au sabot installé.",
                "https://www.greenlee.com/ca/en/bending");

        addManufacturer(root, "KLEIN TOOLS",
                "Cintreuses manuelles EMT courantes pour plusieurs petites et moyennes grosseurs. Klein publie des guides sur les symboles moulés, les types de cintrage et les tableaux propres à ses modèles.\n\nToujours vérifier la fiche de la tête de cintreuse exacte.",
                "https://www.kleintools.com/catalog/conduit-benders");

        addManufacturer(root, "IDEAL",
                "Cintreuses manuelles en aluminium ou fonte ductile pour plusieurs grosseurs EMT. Les repères et données sont propres à la tête utilisée.\n\nRéférence : fiche technique du diamètre et documentation officielle du fabricant.",
                "https://www.idealind.com/ca/en/category/product.html/conduit-benders.html");

        addManufacturer(root, "GARDNER BENDER",
                "Fabricant de cintreuses manuelles et d'outillage pour conduit. Plusieurs modèles couvrent les diamètres EMT courants.\n\nNe pas transposer une charte d'une autre marque ou d'un autre modèle.",
                "https://www.gardnerbender.com/en/products/benders");

        addManufacturer(root, "MILWAUKEE",
                "Milwaukee offre de l'équipement de cintrage motorisé selon les marchés et les modèles. Les capacités et repères sont propres à la machine, aux sabots et aux accessoires installés.\n\nRéférence : documentation officielle de la machine.",
                "https://www.milwaukeetool.ca/");

        addSection(root, "INDEX DES GROSSEURS EMT",
                "1/2 po\n3/4 po\n1 po\n1-1/4 po\n1-1/2 po\n2 po\n\nUne même cintreuse ne couvre pas nécessairement toute cette plage.");

        addSection(root, "LECTURE D'UNE CHARTE FABRICANT",
                "• Identifier la marque et le numéro exact de la cintreuse.\n• Identifier la grosseur EMT.\n• Sur une machine, identifier aussi le sabot installé.\n• Ouvrir la documentation officielle correspondante.\n• Comparer uniquement des données prévues pour ce modèle précis.\n\nL'APK sert d'index théorique vers les chartes officielles.");

        addSection(root, "NOTE",
                "Les déductions, rayons, gains, repères et capacités ne sont pas universels. Ils dépendent de la marque, du modèle, de la grosseur du conduit et parfois du sabot. Toujours utiliser la charte officielle correspondant à l'outil réel.");

        setContentView(scroll);
    }

    private void addManufacturer(LinearLayout root, String title, String body, String url) {
        addSection(root, title, body);
        Button button = new Button(this);
        button.setText("Documentation " + title);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackgroundColor(Color.rgb(0, 91, 187));
        button.setPadding(dp(8), dp(10), dp(8), dp(10));
        button.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ignored) { }
        });
        LinearLayout.LayoutParams bp = matchWrap();
        bp.setMargins(0, 0, 0, dp(14));
        root.addView(button, bp);
    }

    private void addSection(LinearLayout root, String title, String body) {
        TextView card = new TextView(this);
        card.setText(title + "\n\n" + body);
        card.setTextSize(15);
        card.setTextColor(Color.rgb(24, 38, 52));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundColor(Color.WHITE);
        card.setLineSpacing(0, 1.08f);
        LinearLayout.LayoutParams cp = matchWrap();
        cp.setMargins(0, 0, 0, dp(12));
        root.addView(card, cp);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
