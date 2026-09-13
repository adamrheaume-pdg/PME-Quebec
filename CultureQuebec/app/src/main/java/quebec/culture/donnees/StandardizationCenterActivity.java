package quebec.culture.donnees;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

public class StandardizationCenterActivity extends Activity {
    private LinearLayout root;
    private SharedPreferences prefs;
    private Spinner standard;
    private EditText organisation, responsable, sources, quality, sharing, retention, target, schedule;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(0,40,77));
        getWindow().setNavigationBarColor(Color.rgb(0,40,77));
        prefs=getSharedPreferences("standardisation_plan",MODE_PRIVATE);
        setContentView(build());
    }

    private View build(){
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(18),dp(18),dp(30)); root.setBackgroundColor(Color.rgb(245,247,250));
        scroll.addView(root,new ScrollView.LayoutParams(-1,-1)); applyInsets(root);

        root.addView(text("Centre de standardisation",29,Color.rgb(0,59,113),true));
        root.addView(text("Mise à niveau concrète des bases de données culturelles selon les 5 normes sectorielles reconnues par le ministère de la Culture et des Communications.",15,Color.rgb(35,55,75),false),mb());

        LinearLayout warning=panel();
        warning.addView(text("Important",18,Color.rgb(21,37,54),true));
        warning.addView(text("Cet outil aide à préparer, analyser, normaliser et documenter des données. Il ne constitue pas une certification officielle du Ministère et ne garantit pas l’admissibilité d’une demande de subvention.",14,Color.rgb(120,70,15),false));
        root.addView(warning,mb());

        addNorms();
        addMetrics();
        addWorkflow();
        addPlan();
        addEligibility();
        addReporting();
        return scroll;
    }

    private void addNorms(){
        LinearLayout p=panel(); p.addView(text("1 · Choisir la norme sectorielle",21,Color.rgb(21,37,54),true));
        p.addView(text("Le projet doit reposer sur une des cinq normes reconnues. Le secteur choisi devient la référence de travail pour l’import, le diagnostic et l’export.",14,Color.DKGRAY,false));
        String[] labels={"MétaMusique","Arts de la scène","Cinéma","Livre — pour les éditeurs","Exposition dans un contexte muséal"};
        standard=new Spinner(this); standard.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
        int saved=prefs.getInt("standard",0); standard.setSelection(Math.max(0,Math.min(saved,labels.length-1))); p.addView(standard,new LinearLayout.LayoutParams(-1,dp(54)));
        Button guide=secondary("Voir les champs et le guide de cette norme"); guide.setOnClickListener(v->{String[] codes={"METAMUSIQUE","SCENE","CINEMA","LIVRE","MUSEE"};Intent i=new Intent(this,SectorGuideActivity.class);i.putExtra("sector",codes[standard.getSelectedItemPosition()]);startActivity(i);}); p.addView(guide);
        root.addView(p,mb());
    }

    private void addMetrics(){
        List<DocumentStore.Item> docs=DocumentStore.list(this); int rows=0,empty=0,dup=0,score=0; for(DocumentStore.Item d:docs){rows+=d.rows;empty+=d.emptyCells;dup+=d.duplicates;score+=d.score;}
        int avg=docs.isEmpty()?0:score/docs.size();
        LinearLayout p=panel(); p.addView(text("2 · Mesures de qualité et preuves",21,Color.rgb(21,37,54),true));
        p.addView(text("Documents analysés : "+docs.size()+"\nDonnées / fiches : "+rows+"\nQualité moyenne actuelle : "+(docs.isEmpty()?"—":avg+" %")+"\nCellules vides à réviser : "+empty+"\nDoublons détectés : "+dup,15,Color.rgb(21,37,54),false));
        if(!docs.isEmpty()){DataBarView bar=new DataBarView(this);bar.setValues(Math.max(0,avg),Math.max(0,100-avg),Math.min(20,empty+dup));p.addView(bar,new LinearLayout.LayoutParams(-1,dp(44)));}
        p.addView(text("Ces indicateurs servent de point de départ. Pour une reddition de comptes solide, l’organisation devra aussi conserver le nombre total de données adaptées, le nombre de nouvelles données ajoutées et le pourcentage réellement adapté à la norme choisie.",13,Color.rgb(20,108,67),false));
        Button docsB=secondary("Retourner aux catalogues et diagnostics"); docsB.setOnClickListener(v->finish()); p.addView(docsB);
        root.addView(p,mb());
    }

    private void addWorkflow(){
        LinearLayout p=panel();p.addView(text("3 · Parcours de mise à niveau",21,Color.rgb(21,37,54),true));
        p.addView(text("IMPORTER → CARTOGRAPHIER LES CHAMPS → DIAGNOSTIQUER → NORMALISER → VALIDER HUMAINEMENT → EXPORTER → PARTAGER → MESURER",14,Color.rgb(0,59,113),true));
        p.addView(text("L’application doit servir à une mise à niveau réelle d’une base de données existante et non à un projet seulement exploratoire. Les transformations doivent demeurer traçables et les données d’origine doivent pouvoir être distinguées des données normalisées.",14,Color.DKGRAY,false));
        root.addView(p,mb());
    }

    private void addPlan(){
        LinearLayout p=panel();p.addView(text("4 · Plan de gestion des données descriptives",21,Color.rgb(21,37,54),true));
        p.addView(text("Conservez ici les éléments essentiels du plan de gestion. Les informations sont enregistrées localement sur l’appareil.",14,Color.DKGRAY,false));
        organisation=field(p,"Organisation","organisation");
        responsable=field(p,"Responsable des données","responsable");
        sources=field(p,"Sources et provenance des données","sources");
        quality=field(p,"Processus de contrôle de qualité","quality");
        sharing=field(p,"Modalités de partage / données ouvertes","sharing");
        retention=field(p,"Conservation, sauvegardes et versions","retention");
        target=field(p,"Cible mesurable : nombre ou % de données à adapter","target");
        schedule=field(p,"Échéancier de réalisation (maximum 12 mois)","schedule");
        Button save=primary("Enregistrer le plan de gestion");save.setOnClickListener(v->savePlan());p.addView(save);
        root.addView(p,mb());
    }

    private void addEligibility(){
        LinearLayout p=panel();p.addView(text("5 · Vérification avant dépôt",21,Color.rgb(21,37,54),true));
        String[] checks={
            "Organisation légalement constituée depuis plus de 12 mois et siège au Québec",
            "Projet basé sur une des 5 normes reconnues",
            "Mise à niveau concrète d’une base de données, pas seulement exploration",
            "Calendrier de réalisation de 12 mois maximum",
            "Devis externe ou démonstration claire des capacités internes",
            "Description de projet complète selon le gabarit demandé",
            "Budget détaillé et réaliste",
            "Équipe et sous-traitants documentés avec expérience et compétences",
            "Plan de gestion des données descriptives",
            "États financiers et renseignements corporatifs requis",
            "Dépenses prévues après l’annonce de subvention lorsque les règles l’exigent",
            "Développement Web/API non majoritaire dans les dépenses admissibles"
        };
        for(String c:checks){CheckBox cb=new CheckBox(this);cb.setText(c);cb.setTextColor(Color.rgb(35,55,75));cb.setTextSize(14);p.addView(cb);}
        Button budget=primary("Ouvrir le calculateur budgétaire");budget.setOnClickListener(v->startActivity(new Intent(this,BudgetActivity.class)));p.addView(budget);
        p.addView(text("Le calculateur vérifie les plafonds simples de formation, administration et contingence, le taux d’aide, le cumul public et la contribution minimale. Le gabarit XLSM officiel reste la référence pour le dépôt.",12,Color.DKGRAY,false));
        root.addView(p,mb());
    }

    private void addReporting(){
        LinearLayout p=panel();p.addView(text("6 · Reddition de comptes et données ouvertes",21,Color.rgb(21,37,54),true));
        p.addView(text("À conserver pendant le projet : dépenses et écarts, activités réalisées, problèmes rencontrés, nombre total de données adaptées, nouvelles données ajoutées, pourcentage adapté, modalités de partage et partenaires ayant accédé aux données.",14,Color.DKGRAY,false));
        p.addView(text("Objectif recommandé : prévoir un export de données descriptives ouvertes sur une base régulière lorsque les droits et licences le permettent. Le programme prévoit un bonus de sélection pour les projets qui déposent régulièrement leurs données descriptives en données ouvertes.",13,Color.rgb(20,108,67),false));
        Button evidence=primary("Ouvrir le suivi des preuves et résultats");evidence.setOnClickListener(v->startActivity(new Intent(this,ProjectEvidenceActivity.class)));p.addView(evidence);
        Button share=secondary("Préparer un résumé du projet");share.setOnClickListener(v->shareSummary());p.addView(share);
        root.addView(p,mb());
    }

    private EditText field(LinearLayout p,String label,String key){p.addView(text(label,13,Color.rgb(21,37,54),true));EditText e=new EditText(this);e.setText(prefs.getString(key,""));e.setHint(label);e.setMinLines(2);e.setGravity(Gravity.TOP|Gravity.START);e.setTextSize(14);e.setBackgroundColor(Color.WHITE);e.setPadding(dp(10),dp(9),dp(10),dp(9));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(10);p.addView(e,lp);return e;}

    private void savePlan(){prefs.edit().putInt("standard",standard.getSelectedItemPosition()).putString("organisation",organisation.getText().toString()).putString("responsable",responsable.getText().toString()).putString("sources",sources.getText().toString()).putString("quality",quality.getText().toString()).putString("sharing",sharing.getText().toString()).putString("retention",retention.getText().toString()).putString("target",target.getText().toString()).putString("schedule",schedule.getText().toString()).apply();Toast.makeText(this,"Plan de gestion enregistré localement",Toast.LENGTH_SHORT).show();}

    private void shareSummary(){savePlan();String s="Culture du Québec — Résumé de mise à niveau\n\nNorme : "+standard.getSelectedItem()+"\nOrganisation : "+organisation.getText()+"\nResponsable : "+responsable.getText()+"\nSources : "+sources.getText()+"\nContrôle qualité : "+quality.getText()+"\nPartage : "+sharing.getText()+"\nConservation : "+retention.getText()+"\nCible : "+target.getText()+"\nÉchéancier : "+schedule.getText()+"\n\nDocument de travail — ne constitue pas une certification du MCC.";Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,s);startActivity(Intent.createChooser(i,"Partager le résumé"));}

    private void applyInsets(View v){v.setOnApplyWindowInsetsListener((view,in)->{android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars());view.setPadding(dp(18),dp(18)+bars.top,dp(18),dp(30)+bars.bottom);return in;});v.requestApplyInsets();}
    private LinearLayout panel(){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(16),dp(16),dp(16),dp(16));p.setBackgroundResource(R.drawable.panel);return p;}
    private TextView text(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);t.setLineSpacing(0,1.12f);return t;}
    private Button primary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setAllCaps(false);b.setBackgroundResource(R.drawable.button_primary);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(10);b.setLayoutParams(lp);return b;}
    private Button secondary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.rgb(0,59,113));b.setTextSize(14);b.setAllCaps(false);b.setBackgroundResource(R.drawable.button_secondary);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);b.setLayoutParams(lp);return b;}
    private LinearLayout.LayoutParams mb(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
