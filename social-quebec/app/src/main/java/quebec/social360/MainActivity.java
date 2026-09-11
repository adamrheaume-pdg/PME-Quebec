package quebec.social360;

import android.app.*;
import android.os.Bundle;
import android.widget.*;

public class MainActivity extends Activity {
    private LinearLayout content;
    @Override public void onCreate(Bundle b){super.onCreate(b); buildShell(); showHome();}

    private void buildShell(){
        LinearLayout root=Ui.col(this); root.setBackgroundColor(Ui.BG);
        LinearLayout top=Ui.row(this); top.setPadding(Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,12),Ui.dp(this,6));
        TextView brand=Ui.text(this,"⚜ Social Québec",22,Ui.TEXT,true); top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        Button search=Ui.button(this,"⌕"); search.setOnClickListener(v->showDiscover()); top.addView(search,new LinearLayout.LayoutParams(Ui.dp(this,58),Ui.dp(this,48))); root.addView(top);
        content=Ui.col(this); ScrollView scroll=new ScrollView(this); scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=Ui.row(this); String[] labels={"Accueil","Découvrir","Créer","Messages","Profil"};
        for(String s:labels){Button b=Ui.button(this,s); b.setOnClickListener(v->navigate(((Button)v).getText().toString())); nav.addView(b,new LinearLayout.LayoutParams(0,Ui.dp(this,58),1));}
        root.addView(nav); setContentView(root);
    }
    private void navigate(String s){switch(s){case"Accueil":showHome();break;case"Découvrir":showDiscover();break;case"Créer":showCreate();break;case"Messages":showMessages();break;default:showProfile();}}
    private void clear(String title){content.removeAllViews();content.addView(Ui.text(this,title,26,Ui.TEXT,true));}
    private void pillRow(String...xs){HorizontalScrollView hsv=new HorizontalScrollView(this);LinearLayout r=Ui.row(this);for(String x:xs){Button b=Ui.button(this,x);r.addView(b,new LinearLayout.LayoutParams(-2,Ui.dp(this,48)));}hsv.addView(r);content.addView(hsv);}
    private void card(String title,String body,String actions){LinearLayout c=Ui.col(this);c.setPadding(Ui.dp(this,8),Ui.dp(this,8),Ui.dp(this,8),Ui.dp(this,8));c.setBackgroundColor(Ui.SURFACE);c.addView(Ui.text(this,title,18,Ui.TEXT,true));c.addView(Ui.text(this,body,15,Ui.MUTED,false));c.addView(Ui.text(this,actions,14,Ui.TEXT,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,12),Ui.dp(this,8));content.addView(c,p);}

    private void showHome(){clear("Pour toi");pillRow("Pour toi","Abonnements","Chronologique","Local","Québec","Amis");card("Stories","○ Léa   ○ Malik   ○ Québec auto   ○ Hockey","Voir tout →");card("Vidéo courte · Lanaudière","Découvre un nouveau sentier près de chez toi. Format vertical, remix, duo, réponses vidéo et sous-titres.","♡ 12,4 k   💬 812   ↗ 1,9 k   + Suivre");card("Publication · Montréal","Fil texte/photo avec citations, fils, sondages et Notes communautaires.","♡ J’aime   💬 Répondre   ↻ Repartager   ⚑");card("LIVE maintenant","Salon public : musique locale • 1 248 personnes","Rejoindre le LIVE");}
    private void showDiscover(){clear("Découvrir");pillRow("Tendances","Vidéos","Communautés","LIVE","Local","Créateurs");card("Recherche universelle","Cherche des personnes, sujets, vidéos, transcriptions, communautés, événements et lieux publics.","⌕ Ex. « rénovation cuisine Québec »");card("Radar Québec","Tendances par région sans exposer la position exacte des utilisateurs.","Montréal · Québec · Estrie · Lanaudière · Mauricie · Outaouais…");card("Communautés","Salons texte, forums, audio, événements, sondages et modération.","Explorer les communautés");}
    private void showCreate(){clear("Créer");pillRow("Texte","Photo","Story","Vidéo","Short","LIVE","Salon","Sondage");card("Studio de création","Caméra, montage, filtres, musique autorisée, sous-titres, brouillons, confidentialité et planification.","＋ Nouveau contenu");card("Assistant IA","Titre, résumé, chapitres, transcription, traduction et découpage de longs contenus en clips.","✦ Ouvrir l’assistant");}
    private void showMessages(){clear("Messages");pillRow("Privés","Groupes","Demandes","Appels");card("Mélanie","On se rejoint au salon à 19 h?","✓✓ il y a 2 min");card("Groupe · Hockey Québec","12 nouveaux messages","Ouvrir");card("Appels et salons","Audio/vidéo, partage d’écran et salons persistants.","Démarrer un salon");}
    private void showProfile(){clear("Profil");card("@toi · Québec vérifié ⚜","Abonnés 12,8 k   •   Abonnements 486","Modifier le profil");pillRow("Publications","Vidéos","Shorts","LIVE","Stories","Communautés");card("Contrôle du fil","Choisis ce que tu veux suivre chez chaque créateur : textes, vidéos, LIVE ou tout.","Gérer mes préférences");card("Confidentialité","Position exacte non affichée. Contrôle des messages, mentions, visibilité, blocages et export/suppression des données.","Paramètres");}
}
