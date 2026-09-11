package ca.quebec.oui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Locale;

public class TransitionActivity extends Activity {
    private static final int BLUE=Color.rgb(0,61,165), DARK=Color.rgb(0,31,91), INK=Color.rgb(20,33,61), MUTED=Color.rgb(82,98,125), SOFT=Color.rgb(244,247,252), WHITE=Color.WHITE, ORANGE=Color.rgb(194,104,16), GREEN=Color.rgb(18,126,72), RED=Color.rgb(176,46,46);
    private static final double FEDERAL_DEBT_2627=1399.3; // G$, projection officielle fédérale 2026-2027
    private static final double POP_SHARE_BASE=21.7;       // %, Budget Québec 2026-2027
    private final DecimalFormat one=new DecimalFormat("0.0");
    private final DecimalFormat two=new DecimalFormat("0.00");

    private TextView transitionValue, yearsValue, transitionResult, transitionAnnual, transitionDetail;
    private TextView debtShareValue, debtResult, debtInterestValue, debtInterestResult;
    private TextView gdpShockValue, rateShockValue, macroRevenueResult, macroInterestResult, macroFiveYearResult;
    private TextView savingsValue, budgetBalanceResult, verdict;
    private SeekBar transitionSeek, yearsSeek, debtShareSeek, debtInterestSeek, gdpShockSeek, rateShockSeek, savingsSeek;
    private RadioGroup tradeGroup;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        Locale.setDefault(Locale.CANADA_FRENCH);
        getWindow().setStatusBarColor(DARK);getWindow().setNavigationBarColor(DARK);
        build();
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private GradientDrawable bordered(int c,int r,int stroke){GradientDrawable g=bg(c,r);g.setStroke(dp(1),stroke);return g;}
    private TextView tx(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);t.setLineSpacing(0,1.10f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(WHITE);b.setTextSize(14);b.setAllCaps(false);b.setBackground(bg(BLUE,14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.setMargins(0,dp(5),0,dp(5));b.setLayoutParams(p);return b;}
    private void open(String u){startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));}
    private void section(LinearLayout root,String title,String sub){TextView h=tx(title,21,DARK,true);h.setPadding(0,dp(18),0,dp(4));root.addView(h);TextView s=tx(sub,13,MUTED,false);s.setPadding(0,0,0,dp(9));root.addView(s);}
    private void card(LinearLayout root,TextView... views){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(15),dp(14),dp(15),dp(14));c.setBackground(bordered(SOFT,16,Color.rgb(216,225,240)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(5),0,dp(8));c.setLayoutParams(p);for(TextView v:views){v.setPadding(0,dp(3),0,dp(3));c.addView(v);}root.addView(c);}
    private RadioButton radio(String text,int id){RadioButton r=new RadioButton(this);r.setText(text);r.setTextColor(INK);r.setTextSize(14);r.setId(id);r.setPadding(dp(4),dp(7),dp(4),dp(7));return r;}
    private SeekBar seek(int max,int progress){SeekBar s=new SeekBar(this);s.setMax(max);s.setProgress(progress);return s;}

    private void build(){
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setBackgroundColor(WHITE);
        outer.setOnApplyWindowInsetsListener((v,insets)->{if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(i.left,i.top,i.right,i.bottom);}else{v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());}return insets;});
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(18),dp(14),dp(18),dp(14));head.setBackgroundColor(DARK);
        TextView h=tx("SIMULATEUR ÉCONOMIQUE — QUÉBEC INDÉPENDANT",23,WHITE,true);h.setGravity(Gravity.CENTER);head.addView(h);
        TextView hs=tx("Transition • dette • actifs • budget annuel • PIB • taux • commerce",13,Color.rgb(210,225,250),false);hs.setGravity(Gravity.CENTER);head.addView(hs);outer.addView(head);

        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(15),dp(14),dp(15),dp(28));
        TextView warn=tx("🟠 MODÈLE DE SCÉNARIOS — Les données officielles servent d’ancrage. Les résultats liés à une éventuelle indépendance sont des calculs ou des hypothèses, pas des prévisions officielles. Chaque bloc est volontairement séparé pour éviter le double comptage.",13,ORANGE,true);warn.setPadding(dp(13),dp(11),dp(13),dp(11));warn.setBackground(bordered(Color.rgb(255,249,238),14,Color.rgb(238,205,145)));root.addView(warn);

        section(root,"A. Coût temporaire de transition","Fourchette de travail destinée à tester la mise en place et le transfert des systèmes, personnel, fiscalité, frontières, citoyenneté, diplomatie, réglementation et informatique.");
        transitionValue=tx("Hypothèse de transition : 10,0 G$",18,DARK,true);root.addView(transitionValue);
        transitionSeek=seek(15,5);root.addView(transitionSeek);
        root.addView(tx("Plage : 5 à 20 G$. Ce n’est pas un chiffre gouvernemental établi. Le scénario central de travail demeure 8–12 G$.",12,MUTED,false));
        yearsValue=tx("Durée : 5 ans",17,DARK,true);yearsValue.setPadding(0,dp(8),0,0);root.addView(yearsValue);
        yearsSeek=seek(4,2);root.addView(yearsSeek);

        section(root,"B. Commerce Québec–États-Unis","Le scénario commercial ne crée plus artificiellement un montant fixe en dollars. Il sert plutôt à cadrer le niveau de risque macroéconomique à tester.");
        tradeGroup=new RadioGroup(this);tradeGroup.setOrientation(RadioGroup.VERTICAL);
        RadioButton rapid=radio("Continuité rapide / accord transitoire — risque commercial faible",100);rapid.setChecked(true);tradeGroup.addView(rapid);
        tradeGroup.addView(radio("Négociation avec frictions limitées — risque intermédiaire",101));
        tradeGroup.addView(radio("Perturbation commerciale importante — scénario de stress",102));root.addView(tradeGroup);
        root.addView(tx("Une entente avec Washington n’est pas présumée. Le module teste uniquement les conséquences possibles de différents niveaux de continuité commerciale.",12,MUTED,false));

        section(root,"C. Dette fédérale : partage hypothétique","Point d’ancrage : la Mise à jour économique fédérale du printemps 2026 projette une dette fédérale de 1 399,3 G$ en 2026-2027. Le Budget du Québec utilise une part de population de 21,7 %. Aucun mécanisme de partage n’est prédéterminé.");
        debtShareValue=tx("Clé de partage simulée : 21,7 %",18,DARK,true);root.addView(debtShareValue);
        debtShareSeek=seek(100,67);root.addView(debtShareSeek); // 15,0 à 25,0 %
        debtResult=tx("",18,BLUE,true);card(root,debtResult,tx("Important : la « dette fédérale » (déficit accumulé) est déjà un concept NET des actifs financiers et non financiers du gouvernement fédéral. Soustraire ensuite une part proportionnelle de tous les actifs une deuxième fois serait un double comptage.",12,INK,false));

        section(root,"D. Coût d’intérêt de la dette attribuée","Calcul mécanique seulement : applique un taux moyen hypothétique à la dette fédérale attribuée. Il ne prédit ni la cote de crédit ni le taux auquel un nouvel État emprunterait réellement.");
        debtInterestValue=tx("Taux moyen simulé : 4,0 %",18,DARK,true);root.addView(debtInterestValue);
        debtInterestSeek=seek(40,20);root.addView(debtInterestSeek); // 2,0 à 6,0
        debtInterestResult=tx("",17,BLUE,true);card(root,debtInterestResult);

        section(root,"E. Stress macroéconomique","Sensibilités officielles du Québec utilisées comme repères : 1 point de PIB nominal ≈ 1,1 G$ de revenus autonomes; +1 point de taux ≈ +654 M$ de service de dette la première année et ≈ +1,8 G$ à la cinquième année. Ces sensibilités sont celles du budget actuel, pas un modèle complet d’indépendance.");
        gdpShockValue=tx("Écart de PIB nominal vs scénario de référence : 0,0 %",17,DARK,true);root.addView(gdpShockValue);
        gdpShockSeek=seek(70,0);root.addView(gdpShockSeek); // 0 à -7,0
        rateShockValue=tx("Prime de taux / choc de taux : +0,0 point",17,DARK,true);rateShockValue.setPadding(0,dp(8),0,0);root.addView(rateShockValue);
        rateShockSeek=seek(30,0);root.addView(rateShockSeek); // 0 à +3,0
        macroRevenueResult=tx("",16,BLUE,true);macroInterestResult=tx("",15,INK,true);macroFiveYearResult=tx("",15,INK,true);card(root,macroRevenueResult,macroInterestResult,macroFiveYearResult);

        section(root,"F. Économies de chevauchements","Hypothèse volontairement ajustable. Aucune économie n’est garantie : un service fédéral supprimé doit souvent être remplacé par une fonction québécoise. Le gain net dépendrait de l’organisation choisie.");
        savingsValue=tx("Économies nettes simulées : 0,0 G$/an",17,DARK,true);root.addView(savingsValue);
        savingsSeek=seek(70,0);root.addView(savingsSeek); // 0 à 7,0 G
        root.addView(tx("Plage de stress-test : 0 à 7 G$/an. 0 G$ = scénario prudent; les valeurs supérieures sont des hypothèses, pas des faits établis.",12,MUTED,false));

        section(root,"G. Tableau de bord consolidé","On additionne uniquement les éléments qui sont compatibles. La dette attribuée reste un stock; le coût de transition est temporaire; les intérêts, les chocs de revenus et les économies sont des flux annuels.");
        transitionResult=tx("",23,DARK,true);transitionAnnual=tx("",17,BLUE,true);transitionDetail=tx("",13,INK,false);budgetBalanceResult=tx("",18,DARK,true);verdict=tx("",14,INK,true);card(root,transitionResult,transitionAnnual,transitionDetail,budgetBalanceResult,verdict);

        section(root,"H. Ce qui est traité séparément — pas ignoré","Ces éléments ne doivent pas être mélangés au coût temporaire de transition.");
        root.addView(tx("• Part négociée de la dette fédérale\n• Actifs fédéraux : déjà intégrés dans la notion de dette fédérale nette utilisée ici lorsqu’on applique une clé proportionnelle; les actifs localisés ou non proportionnels nécessiteraient une négociation distincte\n• Dépenses permanentes d’un État souverain\n• Recettes fiscales fédérales qui deviendraient potentiellement québécoises\n• Péréquation, transferts fédéraux et programmes fédéraux qui cesseraient ou seraient remplacés\n• Régime monétaire et bancaire\n• Effets sur l’investissement, la productivité, la migration et les chaînes d’approvisionnement\n• Partage des régimes de retraite et autres engagements\n• Coûts juridiques et calendrier réel de négociation",13,INK,false));

        section(root,"I. Repères factuels","Les calculs ci-dessus utilisent des données publiques récentes quand une donnée officielle existe.");
        root.addView(tx("🟢 OFFICIEL — Dette fédérale projetée 2026-2027 : 1 399,3 G$.\n🟢 OFFICIEL — Part de la population québécoise utilisée au Budget 2026-2027 : 21,7 %.\n🟢 OFFICIEL — Sensibilité Québec : 1 pt de PIB nominal ≈ 1,1 G$ de revenus autonomes.\n🟢 OFFICIEL — Sensibilité Québec : +1 pt de taux ≈ +654 M$ de service de dette la première année; ≈ +1,8 G$ à la cinquième année.\n🟠 HYPOTHÈSE — Coût de transition 5–20 G$.\n🟠 HYPOTHÈSE — Économies de chevauchements 0–7 G$/an.\n🟠 HYPOTHÈSE — Chocs de PIB et de taux choisis par l’utilisateur.",13,INK,false));

        section(root,"Sources","Documents officiels et étude de référence. L’IRAI est présenté comme source d’analyse, pas comme donnée gouvernementale.");
        Button fed=button("Canada — Mise à jour économique du printemps 2026");fed.setOnClickListener(v->open("https://budget.canada.ca/update-miseajour/2026/report-rapport/anx1-fr.html"));root.addView(fed);
        Button qc=button("Québec — Budget 2026-2027, information additionnelle");qc.setOnClickListener(v->open("https://www.finances.gouv.qc.ca/Budget_and_update/budget/documents/AUTEN_SupplementaryInfoMarch2026.pdf"));root.addView(qc);
        Button annual=button("Canada — Rapport financier annuel 2024-2025");annual.setOnClickListener(v->open("https://www.canada.ca/fr/ministere-finances/services/publications/rapport-financier-annuel/2025.html"));root.addView(annual);
        Button irai=button("IRAI — Nicolas Marceau, faisabilité financière (2023)");irai.setOnClickListener(v->open("https://irai.quebec/publications/la-faisabilite-economique-et-financiere-dun-quebec-souverain/"));root.addView(irai);

        SeekBar.OnSeekBarChangeListener listener=new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){update();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}};
        transitionSeek.setOnSeekBarChangeListener(listener);yearsSeek.setOnSeekBarChangeListener(listener);debtShareSeek.setOnSeekBarChangeListener(listener);debtInterestSeek.setOnSeekBarChangeListener(listener);gdpShockSeek.setOnSeekBarChangeListener(listener);rateShockSeek.setOnSeekBarChangeListener(listener);savingsSeek.setOnSeekBarChangeListener(listener);
        tradeGroup.setOnCheckedChangeListener((g,id)->applyTradePreset(id));
        update();
        scroll.addView(root);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(outer);outer.requestApplyInsets();
    }

    private void applyTradePreset(int id){
        if(id==100){if(gdpShockSeek.getProgress()>10)gdpShockSeek.setProgress(5);}
        else if(id==101){if(gdpShockSeek.getProgress()<10||gdpShockSeek.getProgress()>35)gdpShockSeek.setProgress(15);}
        else if(id==102){if(gdpShockSeek.getProgress()<30)gdpShockSeek.setProgress(40);}
        update();
    }

    private void update(){
        double transition=5.0+transitionSeek.getProgress();
        int years=3+yearsSeek.getProgress();
        double transitionAnnualValue=transition/years;
        double share=(150.0+debtShareSeek.getProgress())/10.0;
        double debt=FEDERAL_DEBT_2627*share/100.0;
        double debtRate=(20.0+debtInterestSeek.getProgress())/10.0;
        double debtInterest=debt*debtRate/100.0;
        double gdpShock=-(gdpShockSeek.getProgress()/10.0);
        double rateShock=rateShockSeek.getProgress()/10.0;
        double revenuePressure=(-gdpShock)*1.1;
        double rateYear1=rateShock*0.654;
        double rateYear5=rateShock*1.8;
        double savings=savingsSeek.getProgress()/10.0;
        double annualStress=revenuePressure+rateYear1;
        double illustrativeAnnualNet=transitionAnnualValue+annualStress-savings;

        transitionValue.setText("Hypothèse de transition : "+one.format(transition)+" G$");
        yearsValue.setText("Durée : "+years+" ans");
        debtShareValue.setText("Clé de partage simulée : "+one.format(share)+" %"+(Math.abs(share-POP_SHARE_BASE)<0.06?"  • repère démographique actuel":""));
        debtResult.setText("Part théorique de dette fédérale : "+one.format(debt)+" G$\nCalcul : 1 399,3 G$ × "+one.format(share)+" %.");
        debtInterestValue.setText("Taux moyen simulé : "+one.format(debtRate)+" %");
        debtInterestResult.setText("Intérêt mécanique sur cette part : ≈ "+one.format(debtInterest)+" G$/an\nCe calcul est séparé du coût de transition et ne tient pas compte d’un échéancier de refinancement.");
        gdpShockValue.setText("Écart de PIB nominal vs scénario de référence : "+one.format(gdpShock)+" %");
        rateShockValue.setText("Prime de taux / choc de taux : +"+one.format(rateShock)+" point"+(rateShock>1.0?"s":""));
        macroRevenueResult.setText("Pression illustrative sur les revenus autonomes : −"+one.format(revenuePressure)+" G$/an");
        macroInterestResult.setText("Effet taux — repère officiel actuel, année 1 : +"+one.format(rateYear1)+" G$/an");
        macroFiveYearResult.setText("Effet taux — repère officiel actuel, année 5 : jusqu’à +"+one.format(rateYear5)+" G$/an");
        savingsValue.setText("Économies nettes simulées : "+one.format(savings)+" G$/an");

        transitionResult.setText("Transition : "+one.format(transition)+" G$ sur "+years+" ans");
        transitionAnnual.setText("Effort temporaire moyen : "+one.format(transitionAnnualValue)+" G$/an");
        transitionDetail.setText("Dette fédérale théorique attribuée : "+one.format(debt)+" G$ (stock) • intérêt mécanique à "+one.format(debtRate)+" % : "+one.format(debtInterest)+" G$/an • choc de revenus : −"+one.format(revenuePressure)+" G$/an • choc de taux année 1 : +"+one.format(rateYear1)+" G$/an • économies simulées : "+one.format(savings)+" G$/an.");
        budgetBalanceResult.setText("Pression annuelle combinée testée* : "+one.format(illustrativeAnnualNet)+" G$/an");
        budgetBalanceResult.setTextColor(illustrativeAnnualNet<=2?GREEN:(illustrativeAnnualNet<=6?ORANGE:RED));
        String assessment=illustrativeAnnualNet<=2?"faible à modérée":(illustrativeAnnualNet<=6?"importante":"très importante");
        verdict.setText("Lecture : pression "+assessment+" dans CE scénario. *Cette mesure n’est pas un déficit prévu : elle additionne l’effort temporaire moyen, les sensibilités macroéconomiques choisies et retranche les économies hypothétiques. Elle n’inclut pas les nouvelles recettes fiscales fédérales récupérées ni les dépenses fédérales reprises.");
    }
}
