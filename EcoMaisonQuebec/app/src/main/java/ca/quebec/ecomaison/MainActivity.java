package ca.quebec.ecomaison;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {
  LinearLayout root;
  @Override public void onCreate(Bundle b){super.onCreate(b); home();}
  TextView text(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(size);v.setPadding(20,14,20,14);return v;}
  Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
  EditText number(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.LTGRAY);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);return e;}
  void screen(String title){ScrollView sv=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,30,20,40);root.setBackgroundColor(Color.rgb(7,17,31));root.addView(text("⚡ ÉcoMaison Québec",28));root.addView(text(title,22));sv.addView(root);setContentView(sv);}
  void home(){screen("Tableau de bord énergétique");root.addView(text("Surveillez chauffage, eau chaude, appareils, portes, isolation et économies estimées.",16));Button a=button("Configurer ma maison");a.setOnClickListener(v->setup());root.addView(a);Button c=button("Calculateur chauffage");c.setOnClickListener(v->calc());root.addView(c);root.addView(text("Alertes prévues : haute consommation, froid extrême, chaleur extrême, ouverture prolongée, fuite et pertes thermiques.",15));}
  void setup(){screen("Configuration");Spinner tariff=new Spinner(this);String[] t={"Tarif D","Flex D","Tarif DT","Tarif DP","Tarif DM","Tarif DN","Je ne sais pas"};tariff.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,t));root.addView(text("Tarif Hydro-Québec",17));root.addView(tariff);root.addView(text("Repérez votre tarif sous Détail des coûts sur votre facture.",14));Spinner shape=new Spinner(this);String[] s={"Carrée","Rectangulaire","En L","Irrégulière"};shape.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,s));root.addView(text("Forme de la maison",17));root.addView(shape);Button b=button("Retour");b.setOnClickListener(v->home());root.addView(b);}
  void calc(){screen("Pièce et chauffage");EditText l=number("Longueur (pi)"),w=number("Largeur (pi)"),h=number("Hauteur (pi)"),p=number("Chauffage installé (W)");root.addView(l);root.addView(w);root.addView(h);root.addView(p);TextView r=text("Entrez les valeurs.",17);root.addView(r);Button go=button("Calculer");go.setOnClickListener(v->{try{double L=Double.parseDouble(l.getText().toString()),W=Double.parseDouble(w.getText().toString()),H=Double.parseDouble(h.getText().toString()),P=Double.parseDouble(p.getText().toString());double a=L*W,vol=a*H;r.setText(String.format("Superficie : %.1f pi²\nVolume : %.1f pi³\nPuissance : %.0f W\nDensité : %.1f W/pi²",a,vol,P,P/a));}catch(Exception e){r.setText("Vérifiez les valeurs.");}});root.addView(go);Button b=button("Retour");b.setOnClickListener(v->home());root.addView(b);}
}
