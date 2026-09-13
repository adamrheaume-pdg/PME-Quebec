package quebec.culture.donnees;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

public class ProjectEvidenceActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout root;
    private EditText totalData, adaptedData, newData, partners, accessCount, activities, problems, expenses, deviations, tech, sustainability, openFrequency;
    private TextView metrics;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(0,40,77));
        getWindow().setNavigationBarColor(Color.rgb(0,40,77));
        prefs=getSharedPreferences("project_evidence",MODE_PRIVATE);
        setContentView(build());
    }

    private View build(){
        ScrollView s=new ScrollView(this);s.setFillViewport(true);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(18),dp(18),dp(30));root.setBackgroundColor(Color.rgb(245,247,250));
        s.addView(root,new ScrollView.LayoutParams(-1,-1));applyInsets(root);
        root.addView(text("Preuves et reddition de comptes",28,Color.rgb(0,59,113),true));
        root.addView(text("Suivi mesurable du projet de standardisation : résultats, qualité, données ouvertes, activités, dépenses et difficultés. Les valeurs restent enregistrées localement.",14,Color.rgb(35,55,75),false),mb());

        addAutoMetrics();
        addResults();
        addOpenData();
        addProjectLog();
        addEvaluationSupport();
        addActions();
        return s;
    }

    private void addAutoMetrics(){
        List<DocumentStore.Item> docs=DocumentStore.list(this);int rows=0,empty=0,dup=0,changed=0,score=0;
        for(DocumentStore.Item d:docs){rows+=d.rows;empty+=d.emptyCells;dup+=d.duplicates;changed+=d.changedCells;score+=d.score;}
        int avg=docs.isEmpty()?0:score/docs.size();
        LinearLayout p=panel();p.addView(text("Mesures automatiques de l’application",20,Color.rgb(21,37,54),true));
        p.addView(text("Catalogues analysés : "+docs.size()+"\nFiches analysées : "+rows+"\nQualité moyenne : "+(docs.isEmpty()?"—":avg+" %")+"\nCorrections automatiques : "+changed+"\nCellules vides : "+empty+"\nDoublons détectés : "+dup,15,Color.rgb(35,55,75),false));
        p.addView(text("Ces mesures sont calculées à partir des catalogues présents dans l’application. Elles complètent, sans remplacer, les chiffres officiels que l’organisation doit conserver pour sa reddition de comptes.",12,Color.DKGRAY,false));
        root.addView(p,mb());
    }

    private void addResults(){
        LinearLayout p=panel();p.addView(text("Résultats mesurables",20,Color.rgb(21,37,54),true));
        totalData=field(p,"Nombre total de données détenues","totalData",false);
        adaptedData=field(p,"Nombre de données adaptées à la norme","adaptedData",false);
        newData=field(p,"Nouvelles données adaptées ajoutées depuis le projet","newData",false);
        partners=field(p,"Nombre de partenaires / clients ayant accès","partners",false);
        accessCount=field(p,"Nombre d’accès ou de réutilisations connus","accessCount",false);
        metrics=text("",15,Color.rgb(0,59,113),true);p.addView(metrics);
        Button calc=secondary("Calculer le pourcentage adapté");calc.setOnClickListener(v->refreshMetrics());p.addView(calc);
        refreshMetrics();root.addView(p,mb());
    }

    private void addOpenData(){
        LinearLayout p=panel();p.addView(text("Données ouvertes et partage régulier",20,Color.rgb(21,37,54),true));
        openFrequency=field(p,"Fréquence prévue de publication (ex. mensuelle)","openFrequency",false);
        p.addView(text("Inscrivez une fréquence seulement si l’organisation possède les droits nécessaires pour publier les données. L’application ne publie rien automatiquement et n’envoie aucune donnée sans action de l’utilisateur.",13,Color.DKGRAY,false));
        root.addView(p,mb());
    }

    private void addProjectLog(){
        LinearLayout p=panel();p.addView(text("Journal du projet",20,Color.rgb(21,37,54),true));
        activities=field(p,"Activités réalisées et livrables","activities",true);
        problems=field(p,"Problèmes et défis rencontrés","problems",true);
        expenses=field(p,"Utilisation de la subvention / dépenses réalisées","expenses",true);
        deviations=field(p,"Écarts au budget ou au calendrier et justification","deviations",true);
        root.addView(p,mb());
    }

    private void addEvaluationSupport(){
        LinearLayout p=panel();p.addView(text("Arguments pour l’analyse du projet",20,Color.rgb(21,37,54),true));
        tech=field(p,"Approche technologique : interopérabilité, traçabilité, formats, sécurité","tech",true);
        sustainability=field(p,"Développement durable : durée de vie, réutilisation, réduction des doublons, sobriété numérique","sustainability",true);
        p.addView(text("Cette section aide à documenter deux critères d’analyse du programme. Le contenu doit refléter les pratiques réelles du projet et ne doit pas être présenté comme une validation du Ministère.",13,Color.DKGRAY,false));
        root.addView(p,mb());
    }

    private void addActions(){
        LinearLayout p=panel();p.addView(text("Sauvegarde et rapport",20,Color.rgb(21,37,54),true));
        Button save=primary("Enregistrer les preuves");save.setOnClickListener(v->save());p.addView(save);
        Button share=secondary("Partager le rapport de suivi");share.setOnClickListener(v->share());p.addView(share);
        root.addView(p,mb());
    }

    private EditText field(LinearLayout p,String label,String key,boolean multiline){
        p.addView(text(label,13,Color.rgb(21,37,54),true));EditText e=new EditText(this);e.setText(prefs.getString(key,""));e.setHint(label);e.setTextSize(14);e.setBackgroundColor(Color.WHITE);e.setPadding(dp(10),dp(9),dp(10),dp(9));
        if(multiline){e.setMinLines(3);e.setGravity(Gravity.TOP|Gravity.START);}else{e.setSingleLine(true);}
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(10);p.addView(e,lp);return e;
    }

    private int integer(EditText e){try{return Math.max(0,Integer.parseInt(e.getText().toString().trim()));}catch(Exception x){return 0;}}
    private void refreshMetrics(){int total=integer(totalData),adapted=integer(adaptedData);int pct=total<=0?0:(int)Math.round(Math.min(100.0,adapted*100.0/total));metrics.setText(total<=0?"Pourcentage adapté : —":"Pourcentage adapté : "+pct+" % ("+adapted+" / "+total+")");}

    private void save(){prefs.edit()
        .putString("totalData",totalData.getText().toString()).putString("adaptedData",adaptedData.getText().toString()).putString("newData",newData.getText().toString())
        .putString("partners",partners.getText().toString()).putString("accessCount",accessCount.getText().toString()).putString("openFrequency",openFrequency.getText().toString())
        .putString("activities",activities.getText().toString()).putString("problems",problems.getText().toString()).putString("expenses",expenses.getText().toString())
        .putString("deviations",deviations.getText().toString()).putString("tech",tech.getText().toString()).putString("sustainability",sustainability.getText().toString()).apply();
        refreshMetrics();Toast.makeText(this,"Preuves enregistrées localement",Toast.LENGTH_SHORT).show();
    }

    private void share(){save();int total=integer(totalData),adapted=integer(adaptedData);String pct=total<=0?"non calculé":Math.round(Math.min(100.0,adapted*100.0/total))+" %";
        String report="Culture du Québec — Rapport de suivi du projet\n\nDONNÉES\nTotal détenu : "+totalData.getText()+"\nAdaptées à la norme : "+adaptedData.getText()+"\nPourcentage adapté : "+pct+"\nNouvelles données adaptées : "+newData.getText()+"\nPartenaires/clients : "+partners.getText()+"\nAccès/réutilisations : "+accessCount.getText()+"\nPublication ouverte prévue : "+openFrequency.getText()+"\n\nACTIVITÉS\n"+activities.getText()+"\n\nPROBLÈMES ET DÉFIS\n"+problems.getText()+"\n\nDÉPENSES\n"+expenses.getText()+"\n\nÉCARTS ET JUSTIFICATIONS\n"+deviations.getText()+"\n\nAPPROCHE TECHNOLOGIQUE\n"+tech.getText()+"\n\nDÉVELOPPEMENT DURABLE\n"+sustainability.getText()+"\n\nDocument de travail généré par Culture du Québec. Ne constitue pas une certification officielle.";
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,report);startActivity(Intent.createChooser(i,"Partager le rapport"));
    }

    private void applyInsets(View v){v.setOnApplyWindowInsetsListener((view,in)->{android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars());view.setPadding(dp(18),dp(18)+bars.top,dp(18),dp(30)+bars.bottom);return in;});v.requestApplyInsets();}
    private LinearLayout panel(){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(16),dp(16),dp(16),dp(16));p.setBackgroundResource(R.drawable.panel);return p;}
    private TextView text(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);t.setLineSpacing(0,1.12f);return t;}
    private Button primary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setAllCaps(false);b.setBackgroundResource(R.drawable.button_primary);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(10);b.setLayoutParams(lp);return b;}
    private Button secondary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.rgb(0,59,113));b.setTextSize(14);b.setAllCaps(false);b.setBackgroundResource(R.drawable.button_secondary);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);b.setLayoutParams(lp);return b;}
    private LinearLayout.LayoutParams mb(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
