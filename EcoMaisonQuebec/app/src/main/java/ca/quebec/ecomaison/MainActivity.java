package ca.quebec.ecomaison;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.widget.*;
import java.util.Locale;
import java.util.Calendar;

public class MainActivity extends Activity {
  private LinearLayout root;
  private SharedPreferences prefs;
  private final int BG=Color.rgb(7,17,31);
  private final int PANEL=Color.rgb(13,29,49);
  private final int PINK=Color.rgb(255,45,170);
  private final int GREEN=Color.rgb(57,255,20);

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    try{
      prefs=getSharedPreferences("ecomaison",MODE_PRIVATE);
      Window w=getWindow();
      w.setStatusBarColor(BG);
      w.setNavigationBarColor(BG);
      home();
    }catch(Throwable t){showCrashFallback(t);}
  }

  private void showCrashFallback(Throwable t){
    LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(30,30,30,30);box.setBackgroundColor(BG);
    TextView title=new TextView(this);title.setText("ÉcoMaison Québec — mode récupération");title.setTextColor(PINK);title.setTextSize(24);box.addView(title);
    TextView msg=new TextView(this);msg.setText("L’application a intercepté une erreur au démarrage. Réinitialisez les données locales puis relancez.\n\n"+t.getClass().getSimpleName());msg.setTextColor(GREEN);msg.setTextSize(16);box.addView(msg);
    Button reset=new Button(this);reset.setText("Réinitialiser les données locales");reset.setOnClickListener(v->{getSharedPreferences("ecomaison",MODE_PRIVATE).edit().clear().apply();recreate();});box.addView(reset);
    setContentView(box);
  }

  private TextView fixed(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(PINK);v.setTextSize(size);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setShadowLayer(7f,0,0,PINK);v.setPadding(10,10,10,10);return v;}
  private TextView value(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(GREEN);v.setTextSize(size);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(10,8,10,8);return v;}
  private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(PINK);b.setTextSize(17);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackgroundColor(PANEL);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,7,0,7);b.setLayoutParams(lp);return b;}
  private EditText field(String hint,boolean number){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(GREEN);e.setHintTextColor(Color.rgb(40,180,40));e.setTextSize(17);e.setBackgroundColor(PANEL);e.setPadding(16,14,16,14);e.setInputType(number?(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED):(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,5,0,5);e.setLayoutParams(lp);return e;}
  private CheckBox check(String s){CheckBox c=new CheckBox(this);c.setText(s);c.setTextColor(GREEN);c.setTextSize(16);return c;}
  private Spinner spinner(String[] items){Spinner s=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,items){@Override public View getView(int p,View v,android.view.ViewGroup parent){TextView t=(TextView)super.getView(p,v,parent);t.setTextColor(GREEN);t.setTextSize(17);t.setBackgroundColor(PANEL);t.setPadding(16,14,16,14);return t;}@Override public View getDropDownView(int p,View v,android.view.ViewGroup parent){TextView t=(TextView)super.getDropDownView(p,v,parent);t.setTextColor(GREEN);t.setTextSize(17);t.setBackgroundColor(PANEL);t.setPadding(20,18,20,18);return t;}};s.setAdapter(a);return s;}
  private double num(EditText e){return Double.parseDouble(e.getText().toString().trim().replace(',','.'));}
  private int indexOf(String[] a,String x){for(int i=0;i<a.length;i++)if(a[i].equals(x))return i;return 0;}

  private void screen(String title){
    ScrollView sv=new ScrollView(this);sv.setFillViewport(true);sv.setBackgroundColor(BG);
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,20,20,50);root.setBackgroundColor(BG);
    root.addView(fixed("⚡ ÉcoMaison Québec",27));root.addView(fixed(title,22));sv.addView(root);setContentView(sv);
  }

  private void home(){
    screen("Tableau de bord énergétique");
    root.addView(value("ÉcoScore : "+ecoScore()+" / 100",23));
    root.addView(fixed("Tarif : "+prefs.getString("tariff","Non configuré")+"  •  Pièces : "+prefs.getInt("roomSaved",0),14));
    root.addView(fixed("Surveillez chauffage, eau chaude, appareils, portes, isolation et économies estimées.",15));
    Button a=button("🏠 Configurer ma maison");a.setOnClickListener(v->setup());root.addView(a);
    Button b=button("🌡 Pièces et chauffage");b.setOnClickListener(v->rooms());root.addView(b);
    Button c=button("⚡ Appareils, eau chaude et coût");c.setOnClickListener(v->usage());root.addView(c);
    Button d=button("🚪 Portes, isolation et alertes");d.setOnClickListener(v->alerts());root.addView(d);
    Button e=button("💰 Efforts et économies");e.setOnClickListener(v->efforts());root.addView(e);
    root.addView(fixed("Rappels : baissez le chauffage d’environ 2 °C si confortable, éteignez l’éclairage inutile, limitez les douches, et déplacez les gros usages hors pointe lorsque votre tarif le rend avantageux.",13));
  }

  private int ecoScore(){int s=30;if(!prefs.getString("tariff","").isEmpty())s+=15;if(prefs.getBoolean("electric",false))s+=10;if(prefs.getInt("roomSaved",0)>0)s+=15;if(prefs.getBoolean("showerOk",false))s+=10;if(prefs.getBoolean("doorOk",false))s+=5;s+=Math.min(15,prefs.getInt("efforts",0));return Math.min(100,s);}

  private void setup(){
    screen("Configuration de la maison");
    String[] tariffs={"Tarif D","Flex D","Tarif DT","Tarif DP","Tarif DM","Tarif DN","Je ne sais pas"};
    String[] shapes={"Carrée","Rectangulaire","En L","Irrégulière"};
    root.addView(fixed("Tarif Hydro-Québec",17));Spinner tariff=spinner(tariffs);tariff.setSelection(indexOf(tariffs,prefs.getString("tariff","Tarif D")));root.addView(tariff);
    root.addView(fixed("Repérez-le sous « Détail des coûts » sur votre facture.",13));
    root.addView(fixed("Forme de la maison",17));Spinner shape=spinner(shapes);shape.setSelection(indexOf(shapes,prefs.getString("shape","Rectangulaire")));root.addView(shape);
    EditText count=field("Nombre total de pièces",true);count.setText(String.valueOf(prefs.getInt("expectedRooms",6)));root.addView(count);
    CheckBox electric=check("Chauffage principal électrique");electric.setChecked(prefs.getBoolean("electric",true));root.addView(electric);
    Button save=button("Enregistrer");save.setOnClickListener(v->{int n=6;try{n=Math.max(1,(int)num(count));}catch(Exception ignored){}prefs.edit().putString("tariff",tariffs[tariff.getSelectedItemPosition()]).putString("shape",shapes[shape.getSelectedItemPosition()]).putInt("expectedRooms",n).putBoolean("electric",electric.isChecked()).apply();Toast.makeText(this,"Configuration enregistrée",Toast.LENGTH_SHORT).show();home();});root.addView(save);
    Button back=button("Retour");back.setOnClickListener(v->home());root.addView(back);
  }

  private void rooms(){
    screen("Pièces et chauffage");
    EditText name=field("Nom de la pièce",false),l=field("Longueur (pi)",true),w=field("Largeur (pi)",true),h=field("Hauteur (pi)",true),p=field("Chauffage installé (W)",true);root.addView(name);root.addView(l);root.addView(w);root.addView(h);root.addView(p);
    String[] iso={"Bonne isolation","Isolation moyenne","Isolation faible"};Spinner ins=spinner(iso);root.addView(fixed("Isolation estimée",15));root.addView(ins);
    TextView out=value("Entrez les dimensions.",16);root.addView(out);
    Button calc=button("Calculer");calc.setOnClickListener(v->{try{double L=num(l),W=num(w),H=num(h),P=num(p);double area=L*W,vol=area*H;double factor=ins.getSelectedItemPosition()==0?0.85:ins.getSelectedItemPosition()==2?1.25:1.0;double preliminary=area*10.0*(H/8.0)*factor;out.setText(String.format(Locale.CANADA,"Superficie : %.1f pi²\nVolume : %.1f pi³\nInstallé : %.0f W\nDensité : %.1f W/pi²\nRepère préliminaire : %.0f W",area,vol,P,P/area,preliminary));}catch(Exception x){out.setText("Vérifiez les valeurs.");}});root.addView(calc);
    Button save=button("Ajouter cette pièce");save.setOnClickListener(v->{try{double L=num(l),W=num(w),H=num(h),P=num(p);String n=name.getText().toString().trim();if(n.isEmpty())n="Pièce "+(prefs.getInt("roomSaved",0)+1);String old=prefs.getString("rooms","");String line=n+" — "+String.format(Locale.CANADA,"%.1f × %.1f × %.1f pi — %.0f W",L,W,H,P);prefs.edit().putString("rooms",old+(old.isEmpty()?"":"\n")+line).putInt("roomSaved",prefs.getInt("roomSaved",0)+1).apply();rooms();}catch(Exception x){Toast.makeText(this,"Valeurs invalides",Toast.LENGTH_SHORT).show();}});root.addView(save);
    root.addView(fixed("Pièces enregistrées",16));String saved=prefs.getString("rooms","");root.addView(value(saved.isEmpty()?"Aucune":saved,14));
    root.addView(fixed("Le repère de puissance est une estimation de dépistage, pas une validation du Code de construction ou du Code de l’électricité. Une charge réelle doit tenir compte des murs, fenêtres, infiltration, climat et caractéristiques du bâtiment.",12));
    Button back=button("Retour");back.setOnClickListener(v->home());root.addView(back);
  }

  private double rate(String tariff,double kwh,boolean peak){int m=Calendar.getInstance().get(Calendar.MONTH)+1;boolean winter=m==12||m<=3;if("Flex D".equals(tariff)){if(winter&&peak)return 0.46463;if(winter)return kwh<=40?0.04886:0.09103;}return kwh<=40?0.07065:0.11142;}

  private void usage(){
    screen("Appareils, eau chaude et coût");
    String[] apps={"Sécheuse — 4000 W","Lave-vaisselle — 1200 W","Laveuse — 500 W","Chauffe-eau — 4500 W","Éclairage — 100 W","Autre"};Spinner ap=spinner(apps);root.addView(ap);
    EditText watts=field("Puissance réelle W (optionnelle)",true),mins=field("Durée (minutes)",true),cycles=field("Nombre d’utilisations",true);cycles.setText("1");root.addView(watts);root.addView(mins);root.addView(cycles);CheckBox peak=check("Événement de pointe Flex D");root.addView(peak);
    TextView out=value("Calcul basé sur le tarif enregistré.",15);root.addView(out);
    Button calc=button("Calculer kWh et coût");calc.setOnClickListener(v->{try{double[] defs={4000,1200,500,4500,100,0};double P=watts.getText().toString().trim().isEmpty()?defs[ap.getSelectedItemPosition()]:num(watts);double kwh=(P/1000.0)*(num(mins)/60.0)*num(cycles);String t=prefs.getString("tariff","Tarif D");double cost=kwh*rate(t,kwh,peak.isChecked());out.setText(String.format(Locale.CANADA,"Énergie : %.2f kWh\nTarif : %s\nCoût énergie estimé : %.2f $",kwh,t,cost));prefs.edit().putFloat("lastKwh",(float)kwh).putFloat("lastCost",(float)cost).apply();}catch(Exception x){out.setText("Vérifiez les valeurs.");}});root.addView(calc);
    root.addView(fixed("Douche / eau chaude",17));EditText shower=field("Durée moyenne d’une douche (min)",true);shower.setText(String.valueOf(prefs.getInt("shower",15)));root.addView(shower);Button save=button("Enregistrer la durée");save.setOnClickListener(v->{try{int m=(int)num(shower);prefs.edit().putInt("shower",m).putBoolean("showerOk",m<=15).apply();Toast.makeText(this,m<=15?"Objectif 15 min atteint":"Durée enregistrée",Toast.LENGTH_SHORT).show();}catch(Exception ignored){}});root.addView(save);
    root.addView(fixed("Les puissances proposées sont des valeurs typiques. Utilisez la plaque signalétique de l’appareil pour une estimation plus précise.",12));Button back=button("Retour");back.setOnClickListener(v->home());root.addView(back);
  }

  private void alerts(){
    screen("Portes, isolation et alertes");EditText outT=field("Température extérieure °C",true),inT=field("Température intérieure °C",true),door=field("Porte ouverte depuis (min)",true);root.addView(outT);root.addView(inT);root.addView(door);CheckBox leak=check("Humidité ou infiltration d’eau observée");root.addView(leak);CheckBox draft=check("Courant d’air / ouverture froide observé");root.addView(draft);TextView out=value("Entrez les observations.",15);root.addView(out);
    Button check=button("Analyser");check.setOnClickListener(v->{StringBuilder s=new StringBuilder();try{double o=num(outT),i=num(inT),d=num(door);if(o<=-20)s.append("⚠ Froid extrême : surveillez chauffage et ouvertures.\n");if(o>=30)s.append("⚠ Chaleur élevée : réduisez les gains de chaleur et surveillez la température intérieure.\n");if(d>=3)s.append("⚠ Porte ouverte longtemps : perte de chaleur/climatisation possible.\n");if(Math.abs(i-o)>35)s.append("Écart intérieur/extérieur important : inspectez les zones froides et infiltrations.\n");prefs.edit().putBoolean("doorOk",d<3).apply();}catch(Exception x){s.append("Températures ou durée incomplètes.\n");}if(leak.isChecked())s.append("💧 Infiltration possible : localisez la source et surveillez l’humidité.\n");if(draft.isChecked())s.append("🏠 Étanchéité à vérifier autour des portes, fenêtres ou autres ouvertures.\n");if(s.length()==0)s.append("Aucune alerte selon les données entrées.");out.setText(s.toString());});root.addView(check);Button back=button("Retour");back.setOnClickListener(v->home());root.addView(back);
  }

  private void efforts(){
    screen("Efforts et économies");int e=prefs.getInt("efforts",0);float cost=prefs.getFloat("lastCost",0f);root.addView(value("Efforts enregistrés : "+e,20));root.addView(value(String.format(Locale.CANADA,"Dernier coût calculé : %.2f $",cost),16));String[] actions={"Thermostat diminué de 2 °C","Lumières éteintes","Douche ≤ 15 min","Sécheuse/lave-vaisselle reporté hors pointe","Porte refermée rapidement"};Spinner act=spinner(actions);root.addView(act);Button add=button("Ajouter cet effort");add.setOnClickListener(v->{prefs.edit().putInt("efforts",prefs.getInt("efforts",0)+1).apply();efforts();});root.addView(add);root.addView(fixed("Les économies affichées dans cette version sont des estimations. Une mesure réelle nécessite les données du compteur ou des capteurs compatibles.",12));Button back=button("Retour");back.setOnClickListener(v->home());root.addView(back);
  }
}
