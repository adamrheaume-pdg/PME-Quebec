package ca.quebec.oui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BLUE=Color.rgb(0,61,165), DARK=Color.rgb(0,31,91), INK=Color.rgb(11,23,57), MUTED=Color.rgb(83,98,122), SOFT=Color.rgb(243,246,251), OFFICIAL=Color.rgb(23,114,69), ESTIMATE=Color.rgb(184,92,0);
    private LinearLayout content;
    private final DecimalFormat one=new DecimalFormat("0.0");

    @Override public void onCreate(Bundle b){super.onCreate(b);Locale.setDefault(Locale.CANADA_FRENCH);shell();dashboard();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView tx(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);t.setLineSpacing(0,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button nav(String s,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setOnClickListener(l);return b;}

    private void shell(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.WHITE);
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(18),dp(18),dp(18),dp(14));head.setBackgroundColor(DARK);
        head.addView(tx("⚜  OUI QUÉBEC",26,Color.WHITE,true));head.addView(tx("Données publiques • Québec–Canada • Simulation d'un Québec pays",13,Color.rgb(210,225,255),false));root.addView(head);
        HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout n=new LinearLayout(this);n.setBackgroundColor(BLUE);n.setPadding(dp(6),dp(6),dp(6),dp(6));
        n.addView(nav("Tableau",v->dashboard()));n.addView(nav("Simulation",v->simulation()));n.addView(nav("Québec–Canada",v->quebecCanada()));n.addView(nav("Dédoublements",v->duplicates()));n.addView(nav("Sources",v->sources()));hs.addView(n);root.addView(hs);
        ScrollView sv=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(16),dp(16),dp(30));sv.addView(content);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private void reset(String h,String s){content.removeAllViews();content.addView(tx(h,25,INK,true));TextView sub=tx(s,14,MUTED,false);sub.setPadding(0,dp(4),0,dp(14));content.addView(sub);}
    private void card(String title,String big,String detail,boolean official){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(14),dp(12),dp(14),dp(12));b.setBackgroundColor(SOFT);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));b.setLayoutParams(lp);b.addView(tx(title,14,MUTED,true));b.addView(tx(big,27,INK,true));b.addView(tx(detail,13,MUTED,false));TextView tag=tx(official?"● DONNÉE OFFICIELLE":"● ESTIMATION / SIMULATION",11,official?OFFICIAL:ESTIMATE,true);tag.setPadding(0,dp(6),0,0);b.addView(tag);content.addView(b);}
    private void note(String s){TextView t=tx("● "+s,12,ESTIMATE,true);t.setPadding(0,dp(5),0,dp(8));content.addView(t);}

    private void dashboard(){reset("Québec en chiffres","Base V1 : données officielles 2026-2027 disponibles au 9 septembre 2026.");card("Revenus du Québec","166,492 G$","134,361 G$ de revenus autonomes + 32,131 G$ de transferts fédéraux.",true);card("Dépenses du Québec","170,757 G$","160,489 G$ de dépenses de portefeuilles + 10,268 G$ de service de la dette.",true);card("Solde budgétaire","−8,612 G$","Après versements au Fonds des générations; 1,3 % du PIB.",true);card("Principaux transferts fédéraux","30,256 G$","Santé 12,457 G$ • social 3,892 G$ • péréquation 13,907 G$.",true);card("Revenus fédéraux – Canada","529,6 G$","Projection 2026-2027 de la Mise à jour économique du printemps 2026.",true);note("Les scénarios d'indépendance sont toujours séparés des données officielles.");}

    private void simulation(){
        reset("Si le Québec devenait un pays","Simulation basée sur les comptes 2026-2027. Les trois paramètres ci-dessous sont modifiables et ne sont pas des prévisions officielles.");
        final double own=134.361, expenses=170.757;
        TextView a=tx("Revenus fédéraux attribuables au Québec : 90,0 G$",15,INK,true);content.addView(a);SeekBar fa=new SeekBar(this);fa.setMax(800);fa.setProgress(400);content.addView(fa);
        TextView b=tx("Nouvelles fonctions d'un État : 65,0 G$",15,INK,true);b.setPadding(0,dp(12),0,0);content.addView(b);SeekBar fn=new SeekBar(this);fn.setMax(600);fn.setProgress(350);content.addView(fn);
        TextView c=tx("Économies de dédoublements : 4,0 G$",15,INK,true);c.setPadding(0,dp(12),0,0);content.addView(c);SeekBar sa=new SeekBar(this);sa.setMax(150);sa.setProgress(40);content.addView(sa);
        TextView r=tx("",26,INK,true);r.setPadding(0,dp(18),0,dp(6));content.addView(r);TextView d=tx("",14,MUTED,false);content.addView(d);
        Runnable up=()->{double fed=50+fa.getProgress()/10.0, funcs=30+fn.getProgress()/10.0, save=sa.getProgress()/10.0, bal=own+fed-expenses-funcs+save;a.setText("Revenus fédéraux attribuables au Québec : "+one.format(fed)+" G$");b.setText("Nouvelles fonctions d'un État : "+one.format(funcs)+" G$");c.setText("Économies de dédoublements : "+one.format(save)+" G$");r.setText("Solde simulé : "+(bal>=0?"+":"")+one.format(bal)+" G$");d.setText("Calcul : 134,361 + "+one.format(fed)+" − 170,757 − "+one.format(funcs)+" + "+one.format(save)+". Les transferts fédéraux actuels ne sont pas comptés comme revenus d'un Québec indépendant.");};
        SeekBar.OnSeekBarChangeListener l=new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){up.run();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}};fa.setOnSeekBarChangeListener(l);fn.setOnSeekBarChangeListener(l);sa.setOnSeekBarChangeListener(l);up.run();note("Orange = hypothèse. Le but est de permettre plusieurs scénarios plutôt que de présenter un chiffre partisan comme certain.");
    }

    private void quebecCanada(){reset("Rapport Québec–Canada","Les transferts ne représentent pas à eux seuls le bilan fiscal Québec–Ottawa.");card("Santé","12,457 G$","Transfert canadien en matière de santé, 2026-2027.",true);card("Programmes sociaux","3,892 G$","Transfert canadien en matière de programmes sociaux, 2026-2027.",true);card("Péréquation","13,907 G$","Montant prévu pour 2026-2027.",true);card("Principaux transferts – total","30,256 G$","Le budget du Québec comptabilise 32,131 G$ de transferts fédéraux au total; le périmètre est plus large.",true);note("La prochaine base détaillée devra aussi ventiler impôts, TPS, assurance-emploi et dépenses fédérales effectuées au Québec selon les sources disponibles.");}

    private void duplicate(String area,String now,String status,String detail){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(14),dp(12),dp(14),dp(12));b.setBackgroundColor(SOFT);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));b.setLayoutParams(lp);b.addView(tx(area,18,INK,true));b.addView(tx(now,13,MUTED,false));TextView st=tx(status,13,ESTIMATE,true);st.setPadding(0,dp(5),0,dp(3));b.addView(st);b.addView(tx(detail,13,MUTED,false));content.addView(b);}
    private void duplicates(){reset("Dédoublements institutionnels","Chaque fonction est classée comme à conserver, transférer, fusionner ou potentiellement réduire.");duplicate("Fiscalité","Revenu Québec + Agence du revenu du Canada","Fusion / transfert potentiel","Administration fiscale, systèmes et services à comparer poste par poste.");duplicate("Immigration","Québec + IRCC","Transfert + nouvelles responsabilités","Frontières et citoyenneté deviendraient des fonctions nationales.");duplicate("Environnement","Québec + Environnement Canada","Chevauchement partiel","Normes, inspection, données et réglementation à redistribuer.");duplicate("Développement économique","Québec + organismes fédéraux","Fusion partielle","Programmes régionaux et soutien aux entreprises à comparer.");duplicate("Statistiques","ISQ + Statistique Canada","Coopération / transfert","Un pays doit conserver une capacité statistique nationale complète.");duplicate("Affaires intergouvernementales","Structures Québec–Ottawa","Potentiellement réductible","Certaines fonctions liées aux relations fédérales-provinciales changeraient de nature.");note("Un ministère semblable ne signifie pas automatiquement que 100 % de sa dépense peut disparaître.");}

    private void source(String title,String detail,String url){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(14),dp(12),dp(14),dp(12));b.setBackgroundColor(SOFT);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));b.setLayoutParams(lp);b.addView(tx(title,17,INK,true));b.addView(tx(detail,13,MUTED,false));Button o=new Button(this);o.setText("Ouvrir la source officielle");o.setAllCaps(false);o.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))));b.addView(o);content.addView(b);}
    private void sources(){reset("Sources officielles","Chaque donnée de la V1 renvoie à la source gouvernementale originale.");source("Budget du Québec 2026-2027","Ministère des Finances du Québec","https://www.finances.gouv.qc.ca/Budget_et_mise_a_jour/budget/");source("Plan budgétaire 2026-2027","Revenus, dépenses, déficit et cadre financier","https://www.finances.gouv.qc.ca/Budget_et_mise_a_jour/budget/documents/Budget2627_PlanBudgetaire.pdf");source("Principaux transferts fédéraux","Finances Canada – Québec 2026-2027","https://www.canada.ca/fr/ministere-finances/programmes/transferts-federaux/principaux-transferts-federaux.html");source("Mise à jour économique du printemps 2026","Projections fédérales","https://budget.canada.ca/update-miseajour/2026/report-rapport/anx1-fr.html");source("Rapport financier annuel 2024-2025","Comptes fédéraux audités","https://www.canada.ca/fr/ministere-finances/services/publications/rapport-financier-annuel/2025.html");TextView s=tx("Base intégrée : 9 septembre 2026",12,MUTED,true);s.setPadding(0,dp(12),0,0);content.addView(s);}
}
