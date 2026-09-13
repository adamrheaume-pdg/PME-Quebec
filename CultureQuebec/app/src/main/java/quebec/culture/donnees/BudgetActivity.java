package quebec.culture.donnees;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.text.NumberFormat;
import java.util.*;

public class BudgetActivity extends Activity {
    private SharedPreferences prefs;
    private Spinner legalType;
    private EditText technology, subcontracting, consulting, training, labour, administration, contingency, otherIneligible, privateContribution, otherPublicAid;
    private TextView result;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(0,40,77));
        getWindow().setNavigationBarColor(Color.rgb(0,40,77));
        prefs=getSharedPreferences("standardisation_budget",MODE_PRIVATE);
        setContentView(build());
    }

    private View build(){
        ScrollView s=new ScrollView(this);s.setFillViewport(true);
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(18),dp(18),dp(18),dp(30));r.setBackgroundColor(Color.rgb(245,247,250));
        s.addView(r,new ScrollView.LayoutParams(-1,-1));applyInsets(r);
        r.addView(text("Budget du projet",28,Color.rgb(0,59,113),true));
        r.addView(text("Calculateur de travail pour le Programme de soutien à la standardisation des données. Il aide à vérifier les plafonds et contributions, sans remplacer le budget XLSM officiel ni une validation du Ministère.",14,Color.rgb(35,55,75),false),mb());

        LinearLayout type=panel();type.addView(text("Statut du demandeur",20,Color.rgb(21,37,54),true));
        String[] types={"OBNL ou coopérative","Entreprise privée à but lucratif"};
        legalType=new Spinner(this);legalType.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,types));legalType.setSelection(prefs.getInt("legalType",0));type.addView(legalType,new LinearLayout.LayoutParams(-1,dp(54)));r.addView(type,mb());

        LinearLayout costs=panel();costs.addView(text("Dépenses du projet",20,Color.rgb(21,37,54),true));
        technology=moneyField(costs,"Conception, développement, équipement, technologie ou outils","technology");
        subcontracting=moneyField(costs,"Sous-traitance et honoraires","subcontracting");
        consulting=moneyField(costs,"Études et expertises-conseils","consulting");
        training=moneyField(costs,"Formation du personnel — plafond indicatif 20 %","training");
        labour=moneyField(costs,"Main-d’œuvre et avantages sociaux","labour");
        administration=moneyField(costs,"Administration — plafond indicatif 5 %","administration");
        contingency=moneyField(costs,"Contingence — plafond indicatif 10 %","contingency");
        otherIneligible=moneyField(costs,"Dépenses non admissibles / hors projet","otherIneligible");
        r.addView(costs,mb());

        LinearLayout financing=panel();financing.addView(text("Financement",20,Color.rgb(21,37,54),true));
        privateContribution=moneyField(financing,"Contribution privée / investissement du demandeur","privateContribution");
        otherPublicAid=moneyField(financing,"Autres aides publiques et crédits d’impôt","otherPublicAid");
        r.addView(financing,mb());

        LinearLayout calc=panel();calc.addView(text("Analyse budgétaire",20,Color.rgb(21,37,54),true));
        result=text("",14,Color.rgb(35,55,75),false);calc.addView(result);
        Button calculate=primary("Calculer et vérifier le budget");calculate.setOnClickListener(v->calculate(true));calc.addView(calculate);
        Button save=secondary("Enregistrer le budget");save.setOnClickListener(v->{save();calculate(false);Toast.makeText(this,"Budget enregistré localement",Toast.LENGTH_SHORT).show();});calc.addView(save);
        Button share=secondary("Partager le résumé budgétaire");share.setOnClickListener(v->share());calc.addView(share);
        r.addView(calc,mb());

        calculate(false);
        return s;
    }

    private EditText moneyField(LinearLayout p,String label,String key){
        p.addView(text(label,13,Color.rgb(21,37,54),true));
        EditText e=new EditText(this);e.setText(prefs.getString(key,""));e.setHint("0,00 $");e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);e.setSingleLine(true);e.setTextSize(15);e.setBackgroundColor(Color.WHITE);e.setPadding(dp(10),dp(9),dp(10),dp(9));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(10);p.addView(e,lp);return e;
    }

    private double value(EditText e){
        try{return Math.max(0,Double.parseDouble(e.getText().toString().trim().replace(" ","").replace(',','.')));}catch(Exception x){return 0;}
    }

    private String calculate(boolean saveFirst){
        if(saveFirst)save();
        double tech=value(technology),sub=value(subcontracting),consult=value(consulting),train=value(training),lab=value(labour),admin=value(administration),cont=value(contingency),ineligible=value(otherIneligible);
        double eligible=tech+sub+consult+train+lab+admin+cont;
        boolean privateCorp=legalType.getSelectedItemPosition()==1;
        double aidRate=privateCorp?0.65:0.90;
        double publicCap=privateCorp?0.75:0.90;
        double contributionMin=privateCorp?0.25:0.10;
        double investmentMin=privateCorp?0.10:0.05;
        double maxGrant=Math.min(50000.0,eligible*aidRate);
        double minContribution=eligible*contributionMin;
        double minInvestment=eligible*investmentMin;
        double totalPublic=value(otherPublicAid)+maxGrant;
        double publicPct=eligible<=0?0:totalPublic/eligible;
        List<String> warnings=new ArrayList<>();
        if(eligible>0 && train>eligible*0.20+0.01)warnings.add("Formation supérieure à 20 % du total admissible saisi.");
        if(eligible>0 && admin>eligible*0.05+0.01)warnings.add("Administration supérieure à 5 % du total admissible saisi.");
        if(eligible>0 && cont>eligible*0.10+0.01)warnings.add("Contingence supérieure à 10 % du total admissible saisi.");
        if(value(privateContribution)+0.01<minContribution)warnings.add("Contribution du demandeur sous le minimum calculé.");
        if(value(privateContribution)+0.01<minInvestment)warnings.add("Investissement minimal du demandeur non atteint selon les montants saisis.");
        if(publicPct>publicCap+0.0001)warnings.add("Le cumul estimé des aides publiques dépasse le plafond applicable.");
        if(ineligible>0)warnings.add("Les dépenses non admissibles doivent être financées hors du calcul de la subvention.");

        NumberFormat nf=NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH);
        StringBuilder b=new StringBuilder();
        b.append("Dépenses admissibles saisies : ").append(nf.format(eligible)).append('\n');
        b.append("Dépenses non admissibles saisies : ").append(nf.format(ineligible)).append('\n');
        b.append("Aide maximale théorique selon le taux : ").append(nf.format(maxGrant)).append(" (plafond absolu 50 000 $)\n");
        b.append("Contribution minimale du demandeur : ").append(nf.format(minContribution)).append('\n');
        b.append("Investissement minimal à prévoir : ").append(nf.format(minInvestment)).append('\n');
        b.append("Cumul public estimé avec aide maximale : ").append(String.format(Locale.CANADA_FRENCH,"%.1f %%",publicPct*100)).append(" / plafond ").append(String.format(Locale.CANADA_FRENCH,"%.0f %%",publicCap*100)).append('\n');
        if(warnings.isEmpty())b.append("\n✓ Aucun dépassement simple détecté avec les montants saisis.");
        else{b.append("\nÀ vérifier :");for(String w:warnings)b.append("\n• ").append(w);}
        b.append("\n\nCalcul indicatif : le gabarit budgétaire officiel et les règles du Ministère demeurent la référence.");
        result.setText(b.toString());
        return b.toString();
    }

    private void save(){
        prefs.edit().putInt("legalType",legalType.getSelectedItemPosition())
            .putString("technology",technology.getText().toString()).putString("subcontracting",subcontracting.getText().toString())
            .putString("consulting",consulting.getText().toString()).putString("training",training.getText().toString())
            .putString("labour",labour.getText().toString()).putString("administration",administration.getText().toString())
            .putString("contingency",contingency.getText().toString()).putString("otherIneligible",otherIneligible.getText().toString())
            .putString("privateContribution",privateContribution.getText().toString()).putString("otherPublicAid",otherPublicAid.getText().toString()).apply();
    }

    private void share(){save();String body="Culture du Québec — Résumé budgétaire de travail\n\n"+calculate(false);Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,body);startActivity(Intent.createChooser(i,"Partager le budget"));}

    private void applyInsets(View v){v.setOnApplyWindowInsetsListener((view,in)->{android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars());view.setPadding(dp(18),dp(18)+bars.top,dp(18),dp(30)+bars.bottom);return in;});v.requestApplyInsets();}
    private LinearLayout panel(){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(16),dp(16),dp(16),dp(16));p.setBackgroundResource(R.drawable.panel);return p;}
    private TextView text(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);t.setLineSpacing(0,1.12f);return t;}
    private Button primary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setAllCaps(false);b.setBackgroundResource(R.drawable.button_primary);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(10);b.setLayoutParams(lp);return b;}
    private Button secondary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.rgb(0,59,113));b.setTextSize(14);b.setAllCaps(false);b.setBackgroundResource(R.drawable.button_secondary);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);b.setLayoutParams(lp);return b;}
    private LinearLayout.LayoutParams mb(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
