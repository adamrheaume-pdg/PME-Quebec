package ca.quebec.oui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class HistoryActivity extends Activity {
    private static final int BLUE=Color.rgb(0,61,165), DARK=Color.rgb(0,31,91), INK=Color.rgb(20,33,61), MUTED=Color.rgb(82,98,125), SOFT=Color.rgb(244,247,252), WHITE=Color.WHITE;
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView tx(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setLineSpacing(0,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(DARK);getWindow().setNavigationBarColor(DARK);build();}
    private void event(LinearLayout r,String date,String title,String body,String tag){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(13),dp(16),dp(14));c.setBackgroundColor(SOFT);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(9));c.setLayoutParams(p);
        c.addView(tx(date+" • "+tag,13,BLUE,true));c.addView(tx(title,18,DARK,true));TextView b=tx(body,14,INK,false);b.setPadding(0,dp(5),0,0);c.addView(b);r.addView(c);
    }
    private void build(){
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setBackgroundColor(WHITE);outer.setOnApplyWindowInsetsListener((v,i)->{if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets x=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(0,x.top,0,x.bottom);}return i;});
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setGravity(Gravity.CENTER);head.setPadding(dp(16),dp(16),dp(16),dp(16));head.setBackgroundColor(DARK);head.addView(tx("⚜ HISTOIRE POLITIQUE DU QUÉBEC",23,WHITE,true));head.addView(tx("Ligne du temps • 1534 à aujourd’hui",14,Color.rgb(210,225,250),false));outer.addView(head);
        ScrollView s=new ScrollView(this);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(14),dp(15),dp(14),dp(28));r.addView(tx("Repères, conflits et points de friction",21,DARK,true));TextView intro=tx("Chronologie documentaire. Elle distingue les guerres militaires, crises politiques, conflits constitutionnels, linguistiques et référendaires. Les rapports entre Français, Britanniques, Canadiens, Canadiens français, Québécois et Canada changent selon les époques : les catégories ne sont pas interchangeables.",14,MUTED,false);intro.setPadding(0,dp(6),0,dp(14));r.addView(intro);
        event(r,"1534","Voyage de Jacques Cartier","Prise de possession symbolique du territoire au nom du roi de France; début d’une longue période d’exploration et de présence française, sur des territoires déjà habités par des peuples autochtones.","NOUVELLE-FRANCE");
        event(r,"1608","Fondation de Québec","Samuel de Champlain établit Québec. La colonie française se développe dans un réseau complexe d’alliances, de commerce et de conflits avec les nations autochtones et les puissances européennes.","NOUVELLE-FRANCE");
        event(r,"1629–1632","Occupation anglaise de Québec","Les frères Kirke prennent Québec en 1629; la colonie revient à la France en 1632 par le traité de Saint-Germain-en-Laye.","FRANCE–ANGLETERRE");
        event(r,"1689–1760","Guerres impériales en Amérique du Nord","Série de guerres franco-britanniques auxquelles participent colonies et nations autochtones. Le contrôle de l’Amérique du Nord est un enjeu impérial majeur.","CONFLITS MILITAIRES");
        event(r,"1755","Déportation des Acadiens","Les autorités britanniques commencent la déportation massive des Acadiens dans le contexte de la rivalité franco-britannique.","GRAND DÉRANGEMENT");
        event(r,"1759","Bataille des plaines d’Abraham","Victoire britannique devant Québec durant la guerre de Sept Ans. Québec capitule peu après.","CONQUÊTE");
        event(r,"1760","Capitulation de Montréal","Fin du contrôle militaire français de la colonie; la Nouvelle-France passe sous administration militaire britannique.","CONQUÊTE");
        event(r,"1763","Traité de Paris et Proclamation royale","La France cède le Canada à la Grande-Bretagne. Un nouveau régime colonial britannique s’installe.","RÉGIME BRITANNIQUE");
        event(r,"1774","Acte de Québec","Le droit civil français et la liberté de religion catholique sont notamment reconnus, tandis que la colonie demeure sans assemblée élue.","CONSTITUTION");
        event(r,"1791–1792","Haut-Canada et Bas-Canada","L’Acte constitutionnel divise la Province de Québec et instaure des chambres d’assemblée élues. Le Bas-Canada reste toutefois une colonie royale où l’exécutif n’est pas responsable devant l’Assemblée.","PARLEMENTARISME");
        event(r,"1834","92 Résolutions","La Chambre du Bas-Canada formule une longue liste de griefs et de demandes de réforme, notamment sur le contrôle de l’exécutif et des finances.","CRISE POLITIQUE");
        event(r,"1837–1838","Rébellions des Patriotes","Après le rejet des principales revendications par Londres, l’impasse politique dégénère en affrontements armés. Les rébellions sont écrasées par les forces britanniques; la Constitution de 1791 est suspendue.","AFFRONTEMENTS");
        event(r,"1840–1841","Acte d’Union","Le Haut et le Bas-Canada sont réunis dans la Province du Canada. L’Union devient un important point de friction politique et national chez les Canadiens français.","UNION");
        event(r,"1848","Gouvernement responsable","L’exécutif colonial devient politiquement responsable devant la majorité de la Chambre, étape majeure du parlementarisme.","DÉMOCRATIE");
        event(r,"1867","Confédération canadienne","L’Acte de l’Amérique du Nord britannique crée le Canada fédéral. Le Québec devient une province avec des compétences constitutionnelles propres.","FÉDÉRATION");
        event(r,"1885","Affaire Louis Riel","L’exécution de Louis Riel provoque une forte crise politique et nationale au Québec et exacerbe les tensions entre visions du Canada.","FRICTION NATIONALE");
        event(r,"1917","Crise de la conscription","La conscription durant la Première Guerre mondiale rencontre une forte opposition au Québec et creuse le fossé politique entre une partie du Canada français et du Canada anglais.","CONSCRIPTION");
        event(r,"1942–1944","Deuxième crise de la conscription","Le plébiscite de 1942 et la conscription outre-mer ravivent les tensions linguistiques et nationales.","CONSCRIPTION");
        event(r,"1960–1966","Révolution tranquille","Modernisation rapide de l’État québécois, laïcisation, nationalisation de l’électricité et affirmation accrue de l’État du Québec. Le débat sur le statut politique du Québec prend une nouvelle ampleur.","ÉTAT QUÉBÉCOIS");
        event(r,"1969","Loi sur les langues officielles du Canada","Le bilinguisme officiel fédéral devient un élément central de la politique linguistique canadienne, alors que le Québec développe parallèlement sa propre politique du français.","LANGUE");
        event(r,"1970","Crise d’Octobre","Enlèvements par le FLQ, meurtre de Pierre Laporte et recours fédéral à la Loi sur les mesures de guerre. Épisode majeur de tension politique et de sécurité publique.","CRISE");
        event(r,"1974–1977","Français, langue officielle et Charte de la langue française","La loi 22 fait du français la langue officielle du Québec en 1974; la loi 101 de 1977 établit un cadre linguistique beaucoup plus vaste.","LANGUE");
        event(r,"1980","Premier référendum","Le gouvernement du Parti Québécois demande un mandat pour négocier la souveraineté-association. Le Non l’emporte avec environ 59,6 %.","RÉFÉRENDUM");
        event(r,"1981–1982","Rapatriement constitutionnel","Le Canada rapatrie sa Constitution et adopte la Charte canadienne des droits et libertés. Le gouvernement du Québec ne consent pas à l’entente constitutionnelle de 1981; cette absence de consentement demeure un point majeur de friction politique.","CONSTITUTION");
        event(r,"1987–1990","Accord du lac Meech","Tentative de faire adhérer politiquement le Québec à l’ordre constitutionnel de 1982, notamment avec la reconnaissance de la société distincte. L’accord échoue en 1990.","CONSTITUTION");
        event(r,"1992","Accord de Charlottetown","Nouvelle proposition de réforme constitutionnelle rejetée par référendum au Québec et dans l’ensemble du Canada.","CONSTITUTION");
        event(r,"1995","Deuxième référendum","Le Non obtient 50,58 % et le Oui 49,42 %. Le résultat très serré marque durablement les relations Québec–Canada.","RÉFÉRENDUM");
        event(r,"1998–2000","Renvoi sur la sécession et Loi sur la clarté","La Cour suprême conclut qu’un vote clair sur une question claire créerait une obligation de négocier, sans droit de sécession unilatérale en droit canadien. Le Parlement fédéral adopte ensuite la Loi sur la clarté.","DROIT CONSTITUTIONNEL");
        event(r,"2006","Reconnaissance de la nation québécoise","La Chambre des communes adopte une motion reconnaissant que les Québécois forment une nation au sein d’un Canada uni.","RECONNAISSANCE");
        event(r,"2019–2022","Laïcité et langue : nouveaux conflits judiciaires et politiques","La loi 21 sur la laïcité et la loi 96 sur le français ravivent les débats sur les compétences du Québec, les droits, la clause dérogatoire et les rapports avec le gouvernement fédéral.","FRICTIONS");
        event(r,"2026","Le débat sur l’avenir politique continue","Indépendance, fédéralisme, langue, immigration, fiscalité, environnement, pouvoirs constitutionnels et place du Québec au Canada demeurent des sujets de compétition électorale et de débat public.","AUJOURD’HUI");
        TextView src=tx("Sources de référence à intégrer/consulter : Assemblée nationale du Québec (chronologie parlementaire et patrimoine), textes constitutionnels, Bibliothèque de l’Assemblée nationale et documents gouvernementaux. Les événements contemporains doivent être mis à jour avec leurs sources officielles.",12,MUTED,false);src.setPadding(0,dp(10),0,0);r.addView(src);s.addView(r);outer.addView(s,new LinearLayout.LayoutParams(-1,0,1));setContentView(outer);outer.requestApplyInsets();
    }
}
