package ca.quebec.ecomaison;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.*;

public class MainActivity extends Activity {
  LinearLayout root;
  final int BG=Color.rgb(7,17,31);
  final int PINK=Color.rgb(255,45,170);
  final int GREEN=Color.rgb(57,255,20);

  @Override public void onCreate(Bundle b){super.onCreate(b);configureSystemBars();home();}

  void configureSystemBars(){
    Window w=getWindow();
    w.setStatusBarColor(BG); w.setNavigationBarColor(BG);
    if(android.os.Build.VERSION.SDK_INT>=30){
      w.setDecorFitsSystemWindows(true);
      WindowInsetsController c=w.getInsetsController();
      if(c!=null)c.setSystemBarsAppearance(0,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
    }
  }

  TextView text(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(PINK);v.setTextSize(size);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setShadowLayer(10f,0,0,PINK);v.setPadding(20,14,20,14);return v;}
  Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.rgb(20,20,20));b.setTextSize(17);b.setPadding(16,12,16,12);return b;}
  EditText number(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(GREEN);e.setHintTextColor(Color.rgb(40,190,35));e.setTextSize(18);e.setTypeface(Typeface.DEFAULT,Typeface.BOLD);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);return e;}
  ArrayAdapter<String> neonAdapter(String[] items){return new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,items){@Override public View getView(int p,View v,android.view.ViewGroup parent){TextView t=(TextView)super.getView(p,v,parent);t.setTextColor(GREEN);t.setTextSize(18);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setBackgroundColor(BG);t.setPadding(20,18,20,18);return t;}@Override public View getDropDownView(int p,View v,android.view.ViewGroup parent){TextView t=(TextView)super.getDropDownView(p,v,parent);t.setTextColor(GREEN);t.setTextSize(18);t.setBackgroundColor(Color.rgb(12,28,48));t.setPadding(24,22,24,22);return t;}};}

  void screen(String title){ScrollView sv=new ScrollView(this);sv.setFillViewport(true);sv.setBackgroundColor(BG);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,24,20,48);root.setBackgroundColor(BG);root.addView(text("⚡ ÉcoMaison Québec",28));root.addView(text(title,22));sv.addView(root);setContentView(sv);}

  void home(){screen("Tableau de bord énergétique");root.addView(text("Surveillez chauffage, eau chaude, appareils, portes, isolation et économies estimées.",16));Button a=button("Configurer ma maison");a.setOnClickListener(v->setup());root.addView(a);Button c=button("Calculateur chauffage");c.setOnClickListener(v->calc());root.addView(c);root.addView(text("Alertes prévues : haute consommation, froid extrême, chaleur extrême, ouverture prolongée, fuite et pertes thermiques.",15));}

  void setup(){screen("Configuration");Spinner tariff=new Spinner(this);String[] t={"Tarif D","Flex D","Tarif DT","Tarif DP","Tarif DM","Tarif DN","Je ne sais pas"};tariff.setAdapter(neonAdapter(t));root.addView(text("Tarif Hydro-Québec",17));root.addView(tariff);root.addView(text("Repérez votre tarif sous Détail des coûts sur votre facture.",14));Spinner shape=new Spinner(this);String[] s={"Carrée","Rectangulaire","En L","Irrégulière"};shape.setAdapter(neonAdapter(s));root.addView(text("Forme de la maison",17));root.addView(shape);Button b=button("Retour");b.setOnClickListener(v->home());root.addView(b);}

  void calc(){screen("Pièce et chauffage");EditText l=number("Longueur (pi)"),w=number("Largeur (pi)"),h=number("Hauteur (pi)"),p=number("Chauffage installé (W)");root.addView(l);root.addView(w);root.addView(h);root.addView(p);TextView r=text("Entrez les valeurs.",17);root.addView(r);Button go=button("Calculer");go.setOnClickListener(v->{try{double L=Double.parseDouble(l.getText().toString()),W=Double.parseDouble(w.getText().toString()),H=Double.parseDouble(h.getText().toString()),P=Double.parseDouble(p.getText().toString());double a=L*W,vol=a*H;r.setText(String.format("Superficie : %.1f pi²\nVolume : %.1f pi³\nPuissance : %.0f W\nDensité : %.1f W/pi²",a,vol,P,P/a));}catch(Exception e){r.setText("Vérifiez les valeurs.");}});root.addView(go);Button b=button("Retour");b.setOnClickListener(v->home());root.addView(b);}
}
