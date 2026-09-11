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
    private static final int BLUE=Color.rgb(0,61,165), DARK=Color.rgb(0,31,91), INK=Color.rgb(20,33,61), MUTED=Color.rgb(82,98,125), SOFT=Color.rgb(244,247,252), WHITE=Color.WHITE, ORANGE=Color.rgb(194,104,16), GREEN=Color.rgb(18,126,72);
    private final DecimalFormat one=new DecimalFormat("0.0");
    private TextView baseValue, durationValue, resultTotal, resultAnnual, tradeImpact, detail;
    private SeekBar baseSeek, durationSeek;
    private RadioGroup tradeGroup;

    @Override public void onCreate(Bundle b){super.onCreate(b);Locale.setDefault(Locale.CANADA_FRENCH);getWindow().setStatusBarColor(DARK);getWindow().setNavigationBarColor(DARK);build();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private GradientDrawable bordered(int c,int r,int stroke){GradientDrawable g=bg(c,r);g.setStroke(dp(1),stroke);return g;}
    private TextView tx(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);t.setLineSpacing(0,1.10f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(WHITE);b.setTextSize(14);b.setAllCaps(false);b.setBackground(bg(BLUE,14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.setMargins(0,dp(5),0,dp(5));b.setLayoutParams(p);return b;}
    private void open(String u){startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));}
    private void section(LinearLayout root,String title,String sub){TextView h=tx(title,21,DARK,true);h.setPadding(0,dp(18),0,dp(4));root.addView(h);TextView s=tx(sub,13,MUTED,false);s.setPadding(0,0,0,dp(9));root.addView(s);}
    private void card(LinearLayout root,TextView... views){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(15),dp(14),dp(15),dp(14));c.setBackground(bordered(SOFT,16,Color.rgb(216,225,240)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(5),0,dp(8));c.setLayoutParams(p);for(TextView v:views)c.addView(v);root.addView(c);}

    private void build(){
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setBackgroundColor(WHITE);outer.setOnApplyWindowInsetsListener((v,insets)->{if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(i.left,i.top,i.right,i.bottom);}else{v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());}return insets;});
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(18),dp(14),dp(18),dp(14));head.setBackgroundColor(DARK);TextView h=tx("SIMULATEUR DE TRANSITION",24,WHITE,true);h.setGravity(Gravity.CENTER);head.addView(h);TextView hs=tx("Indépendance du Québec • coûts temporaires + commerce avec les États-Unis",13,Color.rgb(210,225,250),false);hs.setGravity(Gravity.CENTER);head.addView(hs);outer.addView(head);

        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(15),dp(14),dp(15),dp(28));
        TextView warn=tx("🟠 SIMULATION — Ce module ne prédit pas le coût réel de l’indépendance. Il permet de tester des hypothèses transparentes. Les montants de transition sont distincts de la dette publique, des dépenses normales d’un État et du partage des actifs fédéraux.",13,ORANGE,true);warn.setPadding(dp(13),dp(11),dp(13),dp(11));warn.setBackground(bordered(Color.rgb(255,249,238),14,Color.rgb(238,205,145)));root.addView(warn);

        section(root,"1. Coût administratif de transition","Hypothèse de base : mise en place ou transfert des systèmes, personnel, frontières, citoyenneté, réglementation, diplomatie, paiements et informatique.");
        baseValue=tx("Hypothèse centrale : 10,0 G$",18,DARK,true);root.addView(baseValue);
        baseSeek=new SeekBar(this);baseSeek.setMax(15);baseSeek.setProgress(5);root.addView(baseSeek);
        root.addView(tx("Plage du simulateur : 5 à 20 G$. Le scénario central utilisé dans l’application est 8–12 G$.",12,MUTED,false));

        section(root,"2. Continuité commerciale avec les États-Unis","Choisis un scénario. Les montants ajoutés ci-dessous sont des marges de prudence illustratives pour comparer les scénarios, pas des prévisions officielles.");
        tradeGroup=new RadioGroup(this);tradeGroup.setOrientation(RadioGroup.VERTICAL);
        RadioButton rapid=radio("Continuité rapide / entente transitoire — +0 G$",100);rapid.setChecked(true);tradeGroup.addView(rapid);
        tradeGroup.addView(radio("Négociation avec perturbations limitées — +2,5 G$",101));
        tradeGroup.addView(radio("Absence temporaire d’entente — +7,5 G$",102));
        root.addView(tradeGroup);
        tradeImpact=tx("Impact commercial illustratif : +0,0 G$",14,GREEN,true);tradeImpact.setPadding(0,dp(7),0,0);root.addView(tradeImpact);

        section(root,"3. Durée de la transition","Répartit l’enveloppe temporaire sur plusieurs années pour voir l’effort budgétaire annuel moyen.");
        durationValue=tx("Durée : 5 ans",18,DARK,true);root.addView(durationValue);
        durationSeek=new SeekBar(this);durationSeek.setMax(4);durationSeek.setProgress(2);root.addView(durationSeek);
        root.addView(tx("Plage : 3 à 7 ans.",12,MUTED,false));

        section(root,"Résultat","Lecture simple de l’enveloppe simulée.");
        resultTotal=tx("Enveloppe temporaire estimée : 10,0 G$",25,DARK,true);resultAnnual=tx("Moyenne : 2,0 G$/an sur 5 ans",18,BLUE,true);detail=tx("",13,INK,false);card(root,resultTotal,resultAnnual,detail);

        section(root,"Répartition illustrative de l’enveloppe","Cette ventilation sert à comprendre où pourraient aller les dépenses de transition. Elle n’est pas un devis gouvernemental.");
        root.addView(tx("• 28 % — systèmes administratifs et numériques\n• 16 % — frontières, citoyenneté et documents\n• 12 % — diplomatie et nouveaux organismes réglementaires\n• 14 % — continuité des paiements, pensions et transferts\n• 30 % — contingence, négociations et adaptation commerciale",13,INK,false));

        section(root,"Ce que le simulateur exclut volontairement","Pour éviter de mélanger des concepts financiers différents.");
        root.addView(tx("• Part éventuelle de la dette fédérale\n• Valeur des actifs fédéraux transférés au Québec\n• Dépenses normales et permanentes d’un État souverain\n• Effets macroéconomiques futurs sur le PIB, les taux d’intérêt ou l’investissement\n• Gains ou économies politiques qui ne sont pas garantis",13,INK,false));

        section(root,"Sources et méthode","Les liens servent à vérifier les ordres de grandeur budgétaires et les travaux existants sur la faisabilité financière.");
        Button irai=button("IRAI — faisabilité économique et financière");irai.setOnClickListener(v->open("https://irai.quebec/publications/la-faisabilite-economique-et-financiere-dun-quebec-souverain/"));root.addView(irai);
        Button budget=button("Budget du Québec 2026–2027");budget.setOnClickListener(v->open("https://www.finances.gouv.qc.ca/Budget_et_mise_a_jour/budget/"));root.addView(budget);
        TextView note=tx("Interprétation : une entente commerciale rapide avec les États-Unis pourrait réduire l’incertitude et certaines frictions de transition, mais son contenu, son calendrier et son effet économique ne peuvent pas être garantis avant une négociation réelle.",12,MUTED,false);note.setPadding(0,dp(12),0,0);root.addView(note);

        baseSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){update();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        durationSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){update();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        tradeGroup.setOnCheckedChangeListener((g,id)->update());
        update();
        scroll.addView(root);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(outer);outer.requestApplyInsets();
    }

    private RadioButton radio(String text,int id){RadioButton r=new RadioButton(this);r.setText(text);r.setTextColor(INK);r.setTextSize(14);r.setId(id);r.setPadding(dp(4),dp(7),dp(4),dp(7));return r;}
    private void update(){
        double base=5.0+baseSeek.getProgress();
        int years=3+durationSeek.getProgress();
        double trade=0.0;int id=tradeGroup.getCheckedRadioButtonId();if(id==101)trade=2.5;else if(id==102)trade=7.5;
        double total=base+trade;double annual=total/years;
        baseValue.setText("Hypothèse de transition : "+one.format(base)+" G$");
        durationValue.setText("Durée : "+years+" ans");
        tradeImpact.setText("Impact commercial illustratif : +"+one.format(trade)+" G$");
        tradeImpact.setTextColor(trade==0?GREEN:ORANGE);
        resultTotal.setText("Enveloppe temporaire simulée : "+one.format(total)+" G$");
        resultAnnual.setText("Moyenne : "+one.format(annual)+" G$/an sur "+years+" ans");
        double admin=total*0.28,borders=total*0.16,diplo=total*0.12,pay=total*0.14,cont=total*0.30;
        detail.setText("Répartition indicative : administration/numérique "+one.format(admin)+" G$ • frontières/citoyenneté "+one.format(borders)+" G$ • diplomatie/réglementation "+one.format(diplo)+" G$ • paiements/pensions "+one.format(pay)+" G$ • contingence/commerce "+one.format(cont)+" G$.");
    }
}
