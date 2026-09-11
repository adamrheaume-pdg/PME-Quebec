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
import android.widget.ScrollView;
import android.widget.TextView;

public class CivilSocietyActivity extends Activity {
    private static final int BLUE=Color.rgb(0,61,165), DARK=Color.rgb(0,31,91), INK=Color.rgb(20,33,61), MUTED=Color.rgb(82,98,125), SOFT=Color.rgb(244,247,252), WHITE=Color.WHITE, GREEN=Color.rgb(18,126,72), ORANGE=Color.rgb(194,104,16);

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(DARK);getWindow().setNavigationBarColor(DARK);build();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable bordered(int color,int radius,int stroke){GradientDrawable g=bg(color,radius);g.setStroke(dp(1),stroke);return g;}
    private TextView tx(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setLineSpacing(0,1.10f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(WHITE);b.setTextSize(14);b.setAllCaps(false);b.setBackground(bg(BLUE,14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.setMargins(0,dp(6),0,0);b.setLayoutParams(p);return b;}
    private void open(String u){startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));}
    private void section(LinearLayout root,String title,String subtitle){TextView h=tx(title,22,DARK,true);h.setPadding(0,dp(20),0,dp(4));root.addView(h);TextView s=tx(subtitle,13,MUTED,false);s.setPadding(0,0,0,dp(10));root.addView(s);}
    private void card(LinearLayout root,String title,String tag,String body,String url){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(15),dp(14),dp(15),dp(14));c.setBackground(bordered(SOFT,16,Color.rgb(214,225,244)));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(11));c.setLayoutParams(cp);c.addView(tx(title,18,DARK,true));TextView chip=tx(tag,11,GREEN,true);chip.setPadding(0,dp(3),0,dp(4));c.addView(chip);TextView d=tx(body,13,INK,false);c.addView(d);if(url!=null&&!url.isEmpty()){Button o=button("Ouvrir la source officielle");o.setOnClickListener(v->open(url));c.addView(o);}root.addView(c);}

    private void build(){
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setBackgroundColor(WHITE);outer.setOnApplyWindowInsetsListener((v,insets)->{if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(i.left,i.top,i.right,i.bottom);}else{v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());}return insets;});
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setGravity(Gravity.CENTER);head.setPadding(dp(18),dp(16),dp(18),dp(16));head.setBackgroundColor(DARK);head.addView(tx("SOCIÉTÉ CIVILE INDÉPENDANTISTE",23,WHITE,true));TextView sub=tx("Organisations, médias, recherche, mobilisation et financement",13,Color.rgb(210,225,250),false);sub.setGravity(Gravity.CENTER);head.addView(sub);outer.addView(head);

        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(15),dp(14),dp(15),dp(28));
        TextView note=tx("Repère : cette page recense des acteurs vérifiables de la société civile liés à l’indépendance, à la souveraineté ou à la mobilisation nationale. Ils n’ont pas tous le même rôle, la même ligne politique ni le même degré d’engagement partisan. Le ‘camp du OUI’ est un terme collectif, pas une organisation permanente unique.",13,ORANGE,true);note.setPadding(dp(13),dp(11),dp(13),dp(11));note.setBackground(bordered(Color.rgb(255,249,238),14,Color.rgb(238,205,145)));root.addView(note);

        section(root,"Coordination et mobilisation","Acteurs qui rassemblent, mobilisent ou structurent directement le mouvement.");
        card(root,"OUI Québec — Organisations unies pour l’indépendance","ORGANISME-PARAPLUIE","Organisation citoyenne non partisane qui se présente comme organisme-parapluie du mouvement indépendantiste organisé. Elle regroupe des tables régionales, des comités thématiques et des représentants d’organisations et de mouvements sociaux.","https://www.ouiquebec.org/les-oui-cest-quoi");
        card(root,"Rassemblement pour un Pays Souverain (RPS)","MOUVEMENT CITOYEN","Mouvement de la société civile fondé en 2000, sans attache partisane déclarée, qui travaille explicitement à la réalisation de l’indépendance du Québec et à la défense du français.","https://www.rps.quebec/le-rps");
        card(root,"Société Saint-Jean-Baptiste de Montréal (SSJB)","ORGANISATION NATIONALE","Organisation historique active dans la défense de la langue, de l’histoire nationale, des Patriotes et des enjeux liés à l’affirmation nationale du Québec.","https://ssjb.com/");
        card(root,"Mouvement national des Québécoises et Québécois (MNQ)","RÉSEAU NATIONAL","Mouvement issu de la société civile qui fédère 19 sociétés membres au Québec et agit sur l’identité, la langue, l’histoire, la culture et le patrimoine. Son histoire comprend une participation active aux campagnes souverainistes et référendaires.","https://mnq.quebec/");
        card(root,"Action Souveraine","PLATEFORME STRATÉGIQUE","Plateforme de conseil stratégique et d’innovation civique qui accompagne des initiatives souverainistes, produit des outils d’action et favorise la coordination entre acteurs.","https://actionsouveraine.org/");

        section(root,"Calendrier, événements et ‘camp du OUI’","Outils de convergence et espaces où les initiatives se retrouvent.");
        card(root,"action.quebec","CALENDRIER COLLECTIF","Calendrier collectif qui centralise les activités indépendantistes au Québec afin de rendre les événements plus accessibles et d’encourager la participation citoyenne.","https://action.quebec/");
        card(root,"Camp du OUI","ÉCOSYSTÈME — PAS UNE ORGANISATION UNIQUE","Expression qui désigne collectivement les personnes, groupes, organismes, médias, syndicats, chercheurs et partis qui appuient le OUI dans un contexte référendaire ou indépendantiste. Pour le mouvement organisé actuel, OUI Québec joue un rôle important de convergence.","https://www.ouiquebec.org/");

        section(root,"Idées, recherche et argumentaires","Organismes qui produisent des analyses, publications ou débats intellectuels.");
        card(root,"Intellectuels pour la souveraineté (IPSO)","RÉFLEXION ET DÉBAT","Organisation fondée en 1995 qui vise explicitement à promouvoir la souveraineté et l’indépendance par des interventions publiques, des débats et une réflexion critique indépendante des partis.","https://www.ipso.quebec/");
        card(root,"IRAI","RECHERCHE","Institut de recherche indépendant et non partisan consacré à l’autodétermination des peuples et aux indépendances nationales, avec des travaux comparatifs, juridiques et économiques.","https://irai.quebec/");
        card(root,"L’Action nationale","REVUE D’IDÉES","Revue publiée par la Ligue d’action nationale qui constitue depuis longtemps un lieu de réflexion sur la nation québécoise, la langue, les institutions et le projet souverainiste.","https://action-nationale.qc.ca/");

        section(root,"Médias, relève et diffusion","Projets qui rejoignent le public, les jeunes ou les personnes moins familières avec le débat.");
        card(root,"Génération OUI","MÉDIA / RELÈVE","Projet de contenu indépendantiste cité par OUI Québec parmi les organisations de son écosystème. Il diffuse des entrevues et discussions sur l’indépendance et le projet de pays.","https://www.ouiquebec.org/equipe/");

        section(root,"Financement citoyen","Acteurs qui soutiennent matériellement des initiatives non partisanes.");
        card(root,"Fonds indépendantiste du Québec (FIDQ)","FINANCEMENT DE PROJETS","Organisme indépendant des partis qui finance des projets citoyens ou d’organismes de la société civile visant à informer la population sur l’indépendance du Québec et à en faire la promotion.","https://fidq.quebec/");

        section(root,"Mouvements sociaux liés au réseau","Acteurs importants du débat national, sans les présenter automatiquement comme organisations exclusivement indépendantistes.");
        card(root,"FTQ et CSN","MOUVEMENTS SOCIAUX","OUI Québec identifie la FTQ et la CSN parmi les mouvements sociaux présents dans son réseau de représentation. Leur rôle et leurs positions peuvent varier selon les périodes et les instances; ils ne sont donc pas classés ici comme organismes voués uniquement à l’indépendance.","https://www.ouiquebec.org/equipe/");

        TextView footer=tx("OUI Québec • Société civile • Sources officielles ou institutionnelles privilégiées",11,MUTED,true);footer.setGravity(Gravity.CENTER);footer.setPadding(0,dp(12),0,dp(8));root.addView(footer);
        scroll.addView(root);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(outer);outer.requestApplyInsets();
    }
}
