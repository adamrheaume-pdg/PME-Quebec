package quebec.culture.donnees;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

public class ApplicationChecklistActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout root, checklist;
    private TextView readiness;
    private final List<CheckBox> boxes=new ArrayList<>();
    private final String[] labels={
        "Profil di@pason à jour",
        "Aide-mémoire signé",
        "Description du projet selon le gabarit",
        "Budget détaillé selon le gabarit XLSM",
        "Lettres d’engagement des partenaires, s’il y a lieu",
        "Présentation de l’équipe et des sous-traitants",
        "Calendrier de réalisation de 12 mois maximum",
        "Plan de gestion des données descriptives",
        "Soumissions de fournisseurs, s’il y a lieu",
        "Conditions d’octroi de l’aide financière signées",
        "États financiers les plus récents approuvés et signés",
        "Liste à jour du conseil d’administration",
        "Renseignements sur actionnariat et contrôle, si entreprise privée",
        "Norme sectorielle choisie et documentée",
        "Base de données existante identifiée",
        "Cartographie des champs réalisée",
        "Objectifs mesurables définis",
        "Capacité interne ou devis externe démontré",
        "Budget conforme aux plafonds applicables",
        "Plan de reddition de comptes préparé"
    };

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(0,40,77));
        getWindow().setNavigationBarColor(Color.rgb(0,40,77));
        prefs=getSharedPreferences("application_checklist",MODE_PRIVATE);
        setContentView(build());
    }

    private View build(){
        ScrollView s=new ScrollView(this);s.setFillViewport(true);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(18),dp(18),dp(30));root.setBackgroundColor(Color.rgb(245,247,250));
        s.addView(root,new ScrollView.LayoutParams(-1,-1));applyInsets(root);
        root.addView(text("Dossier de demande",28,Color.rgb(0,59,113),true));
        root.addView(text("Liste de contrôle locale pour préparer un dossier complet. Elle n’envoie rien à di@pason et ne remplace pas les gabarits officiels.",14,Color.rgb(35,55,75),false),mb());

        LinearLayout status=panel();status.addView(text("État de préparation",20,Color.rgb(21,37,54),true));
        readiness=text("",24,Color.rgb(0,59,113),true);status.addView(readiness);root.addView(status,mb());

        checklist=panel();checklist.addView(text("Pièces et validations",20,Color.rgb(21,37,54),true));
        for(int i=0;i<labels.length;i++){
            final int index=i;
            CheckBox cb=new CheckBox(this);cb.setText(labels[i]);cb.setTextSize(14);cb.setTextColor(Color.rgb(35,55,75));cb.setChecked(prefs.getBoolean("c"+i,false));
            cb.setOnCheckedChangeListener((button,checked)->{prefs.edit().putBoolean("c"+index,checked).apply();refresh();});
            boxes.add(cb);checklist.addView(cb);
        }
        root.addView(checklist,mb());

        LinearLayout actions=panel();actions.addView(text("Actions",20,Color.rgb(21,37,54),true));
        Button share=primary("Partager l’état du dossier");share.setOnClickListener(v->share());actions.addView(share);
        Button reset=secondary("Réinitialiser la liste");reset.setOnClickListener(v->confirmReset());actions.addView(reset);root.addView(actions,mb());
        refresh();return s;
    }

    private void refresh(){
        int done=0;for(CheckBox b:boxes)if(b.isChecked())done++;
        int pct=boxes.isEmpty()?0:(int)Math.round(done*100.0/boxes.size());
        readiness.setText(done+" / "+boxes.size()+" éléments · "+pct+" %");
        readiness.setTextColor(pct>=90?Color.rgb(20,108,67):(pct>=60?Color.rgb(154,103,0):Color.rgb(170,40,40)));
    }

    private void share(){
        StringBuilder b=new StringBuilder("Culture du Québec — État du dossier de demande\n\n");
        int done=0;for(int i=0;i<boxes.size();i++){boolean ok=boxes.get(i).isChecked();if(ok)done++;b.append(ok?"✓ ":"☐ ").append(labels[i]).append('\n');}
        int pct=boxes.isEmpty()?0:(int)Math.round(done*100.0/boxes.size());b.append("\nPréparation : ").append(pct).append(" %\n\nListe de travail interne — les exigences et gabarits officiels demeurent la référence.");
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,b.toString());startActivity(Intent.createChooser(i,"Partager l’état du dossier"));
    }

    private void confirmReset(){new AlertDialog.Builder(this).setTitle("Réinitialiser?").setMessage("Toutes les cases seront décochées.").setNegativeButton("Annuler",null).setPositiveButton("Réinitialiser",(d,w)->{SharedPreferences.Editor e=prefs.edit().clear();e.apply();for(CheckBox b:boxes)b.setChecked(false);refresh();}).show();}

    private void applyInsets(View v){v.setOnApplyWindowInsetsListener((view,in)->{android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars());view.setPadding(dp(18),dp(18)+bars.top,dp(18),dp(30)+bars.bottom);return in;});v.requestApplyInsets();}
    private LinearLayout panel(){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(16),dp(16),dp(16),dp(16));p.setBackgroundResource(R.drawable.panel);return p;}
    private TextView text(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);t.setLineSpacing(0,1.12f);return t;}
    private Button primary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setAllCaps(false);b.setBackgroundResource(R.drawable.button_primary);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(10);b.setLayoutParams(lp);return b;}
    private Button secondary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.rgb(0,59,113));b.setTextSize(14);b.setAllCaps(false);b.setBackgroundResource(R.drawable.button_secondary);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);b.setLayoutParams(lp);return b;}
    private LinearLayout.LayoutParams mb(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
