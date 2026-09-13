package ca.quebec.ecomaison;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.*;
import java.util.Locale;

public class MainActivity extends Activity {
  LinearLayout root;
  ScrollView scroll;
  SharedPreferences prefs;
  final int BG=Color.rgb(7,17,31);
  final int PANEL=Color.rgb(13,29,49);
  final int PINK=Color.rgb(255,45,170);
  final int GREEN=Color.rgb(57,255,20);
  final int MUTED=Color.rgb(210,190,210);

  @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("ecomaison",MODE_PRIVATE);configureSystemBars();home();}

  void configureSystemBars(){
    Window w=getWindow();
    w.setStatusBarColor(BG); w.setNavigationBarColor(BG);
    if(android.os.Build.VERSION.SDK_INT>=30){
      w.setDecorFitsSystemWindows(false);
      WindowInsetsController c=w.getInsetsController();
      if(c!=null)c.setSystemBarsAppearance(0,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
    }
  }

  TextView fixed(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(PINK);v.setTextSize(size);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setShadowLayer(8f,0,0,PINK);v.setPadding(18,12,18,12);return v;}
  TextView value(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(GREEN);v.setTextSize(size);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(18,10,18,10);return v;}
  Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(PINK);b.setTextSize(17);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackgroundColor(PANEL);b.setPadding(18,18,18,18);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,8,0,8);b.setLayoutParams(lp);return b;}
  EditText field(String hint,boolean numeric){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(GREEN);e.setHintTextColor(Color.rgb(35,170,40));e.setTextSize(18);e.setTypeface(Typeface.DEFAULT,Typeface.BOLD);e.setBackgroundColor(PANEL);e.setPadding(18,18,18,18);if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);else e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,5,0,5);e.setLayoutParams(lp);return e;}
  CheckBox check(String s){CheckBox c=new CheckBox(this);c.setText(s);c.setTextColor(GREEN);c.setTextSize(17);c.setPadding(12,10,12,10);return c;}
  ArrayAdapter<String> neonAdapter(String[] items){return new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,items){@Override public View getView(int p,View v,android.view.ViewGroup parent){TextView t=(TextView)super.getView(p,v,parent);t.setTextColor(GREEN);t.setTextSize(18);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setBackgroundColor(PANEL);t.setPadding(20,18,20,18);return t;}@Override public View getDropDownView(int p,View v,android.view.ViewGroup parent){TextView t=(TextView)super.getDropDownView(p,v,parent);t.setTextColor(GREEN);t.setTextSize(18);t.setBackgroundColor(Color.rgb(12,34,52));t.setPadding(24,22,24,22);return t;}};}
  Spinner spinner(String[] a){Spinner s=new Spinner(this);s.setAdapter(neonAdapter(a));return s;}
  int indexOf(String[] a,String s){for(int i=0;i<a.length;i++)if(a[i].equals(s))return i;return 0;}
  double num(EditText e){return Double.parseDouble(e.getText().toString().trim().replace(',','.'));}
  void gap(){Space x=new Space(this);x.setLayoutParams(new LinearLayout.LayoutParams(1,12));root.addView(x);}

  void screen(String title){
    scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);scroll.setClipToPadding(false);
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,16,20,48);root.setBackgroundColor(BG);
    root.addView(fixed("⚡ ÉcoMaison Québec",28));root.addView(fixed(title,22));scroll.addView(root);setContentView(scroll);
    scroll.setOnApplyWindowInsetsListener((v,insets)->{
      int top=0,bottom=0;
      if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());top=i.top;bottom=i.bottom;}else{top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();}
      v.setPadding(0,top,0,bottom);return insets;
    });scroll.requestApplyInsets();
  }

  void home(){
    screen("Tableau de bord énergétique");
    String tariff=prefs.getString("tariff","Non configuré");int roomCount=prefs.getInt("roomSaved",0);int score=ecoScore();
    root.addView(value("ÉcoScore : "+score+" / 100",24));
    root.addView(fixed("Tarif : "+tariff+"   •   Pièces enregistrées : "+roomCount,15));
    root.addView(fixed("Surveillez chauffage, eau chaude, appareils, portes, isolation et économies estimées.",16));
    Button a=button("🏠 Configurer ma maison");a.setOnClickListener(v->setup());root.addView(a);
    Button r=button("🌡 Pièces et chauffage");r.setOnClickListener(v->rooms());root.addView(r);
    Button u=button("⚡ Appareils, eau chaude et coût");u.setOnClickListener(v->usage());root.addView(u);
    Button al=button("🚪 Alertes, portes et enveloppe");al.setOnClickListener(v->alerts());root.addView(al);
    Button e=button("💰 Mes efforts et économies");e.setOnClickListener(v->efforts());root.addView(e);
    root.addView(fixed("Conseil : réduisez les thermostats d’environ 2 °C lorsque c’est confortable, éteignez l’éclairage inutile et reportez les gros appareils hors pointe lorsque votre tarif le rend avantageux.",14));
  }

  int ecoScore(){int s=35;if(!prefs.getString("tariff","").isEmpty())s+=15;if(prefs.getBoolean("electric",false))s+=10;if(prefs.getInt("roomSaved",0)>0)s+=15;if(prefs.getBoolean("showerOk",false))s+=10;if(prefs.getBoolean("doorOk",false))s+=5;s+=Math.min(10,prefs.getInt("efforts",0));return Math.min(100,s);}

  void setup(){
    screen("Configuration de la maison");
    final String[] tariffs={"Tarif D","Flex D","Tarif DT","Tarif DP","Tarif DM","Tarif DN","Je ne sais pas"};
    final String[] shapes={"Carrée","Rectangulaire","En L","Irrégulière"};
    root.addView(fixed("Tarif Hydro-Québec",17));Spinner tariff=spinner(tariffs);tariff.setSelection(indexOf(tariffs,prefs.getString("tariff","Tarif D")));root.addView(tariff);
    root.addView(fixed("Repérez votre tarif sous « Détail des coûts » sur votre facture.",14));
    root.addView(fixed("Forme de la maison",17));Spinner shape=spinner(shapes);shape.setSelection(indexOf(shapes,prefs.getString("shape","Rectangulaire")));root.addView(shape);
    EditText expected=field("Nombre total de pièces",true);expected.setText(String.valueOf(prefs.getInt("expectedRooms",6)));root.addView(expected);
    CheckBox electric=check("Chauffage principal électrique");electric.setChecked(prefs.getBoolean("electric",true));root.addView(electric);
    root.addView(fixed("L’application est conçue d’abord pour les maisons chauffées à l’électricité. Les calculs de puissance sont des estimations préliminaires, pas une attestation de conformité au Code.",13));
    Button save=button("Enregistrer la configuration");save.setOnClickListener(v->{int n=6;try{n=(int)num(expected);}catch(Exception ignored){}prefs.edit().putString("tariff",tariffs[tariff.getSelectedItemPosition()]).putString("shape",shapes[shape.getSelectedItemPosition()]).putInt("expectedRooms",Math.max(1,n)).putBoolean("electric",electric.isChecked()).apply();Toast.makeText(this,"Configuration enregistrée",Toast.LENGTH_SHORT).show();home();});root.addView(save);
    Button b=button("Retour");b.setOnClickListener(v->home());root.addView(b);
  }

  void rooms(){
    screen("Pièces et chauffage");
    EditText name=field("Nom de la pièce",false),l=field("Longueur (pi)",true),w=field("Largeur (pi)",true),h=field("Hauteur (pi)",true),p=field("Chauffage installé (W)",true);
    root.addView(name);root.addView(l);root.addView(w);root.addView(h);root.addView(p);
    String[] iso={"Isolation bonne","Isolation moyenne","Isolation faible"};Spinner insulation=spinner(iso);root.addView(fixed("Isolation estimée",15));root.addView(insulation);
    TextView r=value("Entrez les dimensions.",16);root.addView(r);
    Button calc=button("Calculer la pièce");calc.setOnClickListener(v->{try{double L=num(l),W=num(w),H=num(h),P=num(p),area=L*W,vol=area*H;double factor=insulation.getSelectedItemPosition()==0?0.85:insulation.getSelectedItemPosition()==2?1.25:1.0;double estimated=area*10.0*(H/8.0)*factor;double density=P/area;String state=P<estimated*0.85?"Puissance possiblement faible":P>estimated*1.35?"Puissance possiblement élevée":"Puissance près de l’estimation";r.setText(String.format(Locale.CANADA,"Superficie : %.1f pi²\nVolume : %.1f pi³\nInstallé : %.0f W (%.1f W/pi²)\nEstimation préliminaire : %.0f W\n%s",area,vol,P,density,estimated,state));}catch(Exception ex){r.setText("Vérifiez toutes les valeurs numériques.");}});root.addView(calc);
    Button save=button("Ajouter cette pièce à la maison");save.setOnClickListener(v->{try{String n=name.getText().toString().trim();if(n.isEmpty())n="Pièce "+(prefs.getInt("roomSaved",0)+1);double L=num(l),W=num(w),H=num(h),P=num(p);String old=prefs.getString("rooms","");String line=n+" — "+String.format(Locale.CANADA,"%.1f × %.1f × %.1f pi — %.0f W",L,W,H,P);prefs.edit().putString("rooms",old+(old.isEmpty()?"":"\n")+line).putInt("roomSaved",prefs.getInt("roomSaved",0)+1).apply();Toast.makeText(this,"Pièce enregistrée",Toast.LENGTH_SHORT).show();rooms();}catch(Exception ex){Toast.makeText(this,"Valeurs invalides",Toast.LENGTH_SHORT).show();}});root.addView(save);
    String saved=prefs.getString("rooms","");root.addView(fixed("Pièces enregistrées",17));root.addView(value(saved.isEmpty()?"Aucune pièce pour le moment.":saved,15));
    Button clear=button("Effacer toutes les pièces");clear.setOnClickListener(v->{prefs.edit().remove("rooms").putInt("roomSaved",0).apply();rooms();});root.addView(clear);
    root.addView(fixed("Méthode actuelle : repère simplifié de 10 W/pi² à plafond de 8 pi, ajusté selon hauteur et isolation. Une vraie charge de chauffage doit considérer fenêtres, murs extérieurs, étanchéité, climat et règles applicables.",12));
    Button b=button("Retour");b.setOnClickListener(v->home());root.addView(b);
  }

  double energyRate(String tariff,double dailyKwh,boolean flexPeak){
    boolean winter=true;
    int m=java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)+1;winter=(m==12||m<=3);
    if("Flex D".equals(tariff)){
      if(winter&&flexPeak)return 0.46463;
      if(winter)return dailyKwh<=40?0.04886:0.09103;
      return dailyKwh<=40?0.07065:0.11142;
    }
    return dailyKwh<=40?0.07065:0.11142;
  }

  void usage(){
    screen("Appareils, eau chaude et coût");
    String[] appliances={"Sécheuse — 4000 W","Lave-vaisselle — 1200 W","Laveuse — 500 W","Chauffe-eau — 4500 W","Éclairage — 100 W","Autre — entrer la puissance"};Spinner ap=spinner(appliances);root.addView(ap);
    EditText watts=field("Puissance réelle (W) — optionnelle",true),minutes=field("Durée d’utilisation (minutes)",true),cycles=field("Nombre d’utilisations",true);cycles.setText("1");root.addView(watts);root.addView(minutes);root.addView(cycles);
    CheckBox peak=check("Je calcule pendant un événement de pointe Flex D");root.addView(peak);
    TextView out=value("Le coût utilise le tarif enregistré.",16);root.addView(out);
    Button calc=button("Calculer énergie et coût");calc.setOnClickListener(v->{try{double[] defaults={4000,1200,500,4500,100,0};double P=watts.getText().toString().trim().isEmpty()?defaults[ap.getSelectedItemPosition()]:num(watts);double min=num(minutes),cy=num(cycles);double kwh=(P/1000.0)*(min/60.0)*cy;String tariff=prefs.getString("tariff","Tarif D");double rate=energyRate(tariff,kwh,peak.isChecked());double cost=kwh*rate;String note=(tariff.equals("Tarif D")||tariff.equals("Flex D"))?"":"Tarif spécial : coût affiché avec repère D seulement; la facturation exacte peut inclure d’autres composantes.";out.setText(String.format(Locale.CANADA,"Énergie : %.2f kWh\nTarif : %s\nCoût énergie estimé : %.2f $\n%s",kwh,tariff,cost,note));prefs.edit().putFloat("lastKwh",(float)kwh).putFloat("lastCost",(float)cost).apply();}catch(Exception ex){out.setText("Vérifiez durée, puissance et nombre d’utilisations.");}});root.addView(calc);
    gap();root.addView(fixed("Douche / eau chaude",18));EditText shower=field("Durée moyenne d’une douche (minutes)",true);shower.setText(String.valueOf(prefs.getInt("shower",15)));root.addView(shower);TextView sh=value("Objectif suggéré dans l’application : 15 minutes ou moins.",15);root.addView(sh);Button saveSh=button("Enregistrer ma durée de douche");saveSh.setOnClickListener(v->{try{int m=(int)num(shower);boolean ok=m<=15;prefs.edit().putInt("shower",m).putBoolean("showerOk",ok).apply();sh.setText(ok?"✓ Objectif atteint : douche de 15 minutes ou moins.":"Durée supérieure à l’objectif. Réduire progressivement peut diminuer eau chaude et électricité.");}catch(Exception ignored){}});root.addView(saveSh);
    root.addView(fixed("Les puissances préremplies sont des valeurs typiques pour faciliter une première estimation. Entrez la puissance de la plaque signalétique de votre appareil pour plus de précision.",12));
    Button b=button("Retour");b.setOnClickListener(v->home());root.addView(b);
  }

  void alerts(){
    screen("Alertes et enveloppe de la maison");
    EditText outside=field("Température extérieure (°C)",true),inside=field("Température intérieure (°C)",true),door=field("Porte extérieure ouverte (minutes)",true);root.addView(outside);root.addView(inside);root.addView(door);
    TextView r=value("Entrez les conditions actuelles.",16);root.addView(r);
    Button go=button("Analyser");go.setOnClickListener(v->{try{double o=num(outside),i=num(inside),d=num(door);StringBuilder s=new StringBuilder();if(o<=-20)s.append("❄ ALERTE FROID EXTRÊME : surveillez chauffage, tuyauterie et ouvertures.\n");if(o>=30)s.append("☀ ALERTE CHALEUR EXTRÊME : limitez les gains de chaleur et surveillez la température intérieure.\n");if(d>5)s.append("🚪 Porte ouverte depuis plus de 5 minutes : perte de chaleur/climatisation possible.\n");double delta=Math.abs(i-o);if(delta>=30)s.append("🏠 Écart intérieur/extérieur élevé : les défauts d’étanchéité deviennent particulièrement coûteux.\n");if(s.length()==0)s.append("Aucune alerte selon les seuils locaux saisis.");r.setText(s.toString());prefs.edit().putBoolean("doorOk",d<=5).apply();}catch(Exception ex){r.setText("Vérifiez les trois valeurs.");}});root.addView(go);
    root.addView(fixed("Points à inspecter : contours de fenêtres et portes, trappe d’entretoit, prises sur murs extérieurs, jonctions mur/plancher, sous-sol, traces d’humidité près du chauffe-eau, laveuse, lave-vaisselle et fondations.",14));
    root.addView(fixed("Une future version pourra recevoir des capteurs d’ouverture, température, humidité, fuite et caméra thermique. Une alerte de capteur ne remplacera pas une inspection professionnelle.",12));
    Button b=button("Retour");b.setOnClickListener(v->home());root.addView(b);
  }

  void efforts(){
    screen("Mes efforts et économies");int efforts=prefs.getInt("efforts",0);float k=prefs.getFloat("lastKwh",0),c=prefs.getFloat("lastCost",0);root.addView(value("Efforts enregistrés : "+efforts+"\nDernier calcul : "+String.format(Locale.CANADA,"%.2f kWh — %.2f $",k,c)+"\nÉcoScore actuel : "+ecoScore()+" / 100",20));
    String[] labels={"Thermostat réduit d’environ 2 °C","Douche de 15 min ou moins","Lumières éteintes après usage","Gros appareil reporté hors pointe"};for(String label:labels){Button x=button("✓ "+label);x.setOnClickListener(v->{prefs.edit().putInt("efforts",prefs.getInt("efforts",0)+1).apply();Toast.makeText(this,"Effort ajouté",Toast.LENGTH_SHORT).show();efforts();});root.addView(x);}root.addView(fixed("Le compteur d’efforts mesure vos habitudes. Les économies en dollars ne sont affichées que lorsqu’elles proviennent d’un calcul d’énergie; l’application évite de transformer un geste en montant arbitraire.",13));Button reset=button("Réinitialiser le compteur d’efforts");reset.setOnClickListener(v->{prefs.edit().putInt("efforts",0).apply();efforts();});root.addView(reset);Button b=button("Retour");b.setOnClickListener(v->home());root.addView(b);
  }
}
