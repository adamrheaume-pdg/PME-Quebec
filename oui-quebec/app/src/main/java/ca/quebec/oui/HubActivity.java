package ca.quebec.oui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class HubActivity extends Activity {
    private static final int BLUE=Color.rgb(0,61,165), BLUE2=Color.rgb(0,81,196), DARK=Color.rgb(0,31,91), INK=Color.rgb(20,33,61), MUTED=Color.rgb(82,98,125), SOFT=Color.rgb(244,247,252), WHITE=Color.WHITE, GREEN=Color.rgb(18,126,72), ORANGE=Color.rgb(194,104,16), PURPLE=Color.rgb(111,70,160);
    private MediaPlayer player; private Button playButton; private TextView radioStatus;

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(DARK);getWindow().setNavigationBarColor(DARK);build();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable bordered(int color,int radius,int strokeColor){GradientDrawable g=bg(color,radius);g.setStroke(dp(1),strokeColor);return g;}
    private TextView tx(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setLineSpacing(0,1.10f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(WHITE);b.setTextSize(15);b.setAllCaps(false);b.setBackground(bg(BLUE,14));b.setPadding(dp(14),0,dp(14),0);b.setMinHeight(dp(54));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(54));p.setMargins(0,dp(6),0,dp(6));b.setLayoutParams(p);return b;}
    private void open(String u){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}
    private ImageView logo(int sizeDp){ImageView i=new ImageView(this);i.setImageResource(R.drawable.oui_logo);i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);i.setContentDescription("Logo OUI Québec");i.setAdjustViewBounds(true);i.setBackgroundColor(Color.TRANSPARENT);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(sizeDp),dp(sizeDp));p.gravity=Gravity.CENTER_HORIZONTAL;i.setLayoutParams(p);return i;}
    private TextView chip(String text,int color){TextView t=tx(text,11,color,true);t.setGravity(Gravity.CENTER);t.setPadding(dp(10),dp(7),dp(10),dp(7));t.setBackground(bordered(Color.WHITE,14,color));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(3),0,dp(3),0);t.setLayoutParams(p);return t;}
    private void sectionTitle(LinearLayout root,String title,String subtitle){TextView h=tx(title,22,DARK,true);h.setPadding(0,dp(20),0,dp(4));root.addView(h);TextView s=tx(subtitle,13,MUTED,false);s.setPadding(0,0,0,dp(10));root.addView(s);}
    private void card(LinearLayout root,String title,String body,String url){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(15),dp(16),dp(15));c.setBackground(bg(SOFT,16));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(12));c.setLayoutParams(cp);c.addView(tx(title,18,DARK,true));TextView d=tx(body,14,INK,false);d.setPadding(0,dp(5),0,dp(7));c.addView(d);Button o=button("Ouvrir le site / programme");o.setOnClickListener(v->open(url));c.addView(o);root.addView(c);}
    private void infoCard(LinearLayout root,String title,String body){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(bordered(Color.rgb(248,250,255),16,Color.rgb(214,225,244)));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(8),0,dp(10));c.setLayoutParams(cp);c.addView(tx(title,17,DARK,true));TextView b=tx(body,13,INK,false);b.setPadding(0,dp(5),0,0);c.addView(b);root.addView(c);}

    private void build(){
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setBackgroundColor(WHITE);outer.setOnApplyWindowInsetsListener((v,insets)->{int top=0,bottom=0,left=0,right=0;if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());top=i.top;bottom=i.bottom;left=i.left;right=i.right;}else{top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();}v.setPadding(left,top,right,bottom);return insets;});

        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setGravity(Gravity.CENTER_HORIZONTAL);head.setPadding(dp(18),dp(10),dp(18),dp(16));head.setBackgroundColor(DARK);head.addView(logo(96));TextView title=tx("OUI QUÉBEC",28,WHITE,true);title.setGravity(Gravity.CENTER);title.setPadding(0,dp(3),0,0);head.addView(title);TextView version=tx("Centre documentaire • V4.7",13,Color.rgb(210,225,250),false);version.setGravity(Gravity.CENTER);head.addView(version);outer.addView(head);

        ScrollView scroll=new ScrollView(this);scroll.setClipToPadding(false);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(15),dp(14),dp(15),dp(30));

        infoCard(root,"Repères de fiabilité","L’application sépare les faits officiels, les calculs dérivés, les estimations, les propositions politiques et les documents historiques. Les sources partisanes sont identifiées comme telles.");
        LinearLayout chips=new LinearLayout(this);chips.setOrientation(LinearLayout.HORIZONTAL);chips.setPadding(0,0,0,dp(8));chips.addView(chip("● OFFICIEL",GREEN));chips.addView(chip("● CALCUL",BLUE2));chips.addView(chip("● ESTIMATION",ORANGE));root.addView(chips);

        sectionTitle(root,"Navigation rapide","Accès direct aux principaux modules de l’application.");
        Button app=button("📊 Données, Livre bleu et simulateur");app.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));root.addView(app);
        Button library=button("📚 Bibliothèque — indépendance du Québec");library.setOnClickListener(v->startActivity(new Intent(this,LiteratureActivity.class)));root.addView(library);
        Button history=button("🕰 Histoire politique — 1534 à aujourd’hui");history.setOnClickListener(v->startActivity(new Intent(this,HistoryActivity.class)));root.addView(history);

        sectionTitle(root,"Partis politiques indépendantistes","Liens directs vers les partis et leurs positions ou programmes. Cette section présente les sources partisanes comme telles.");
        card(root,"Parti Québécois","Parti indépendantiste. Sa section officielle sur l’indépendance présente son projet de Québec pays, ses arguments et ses documents politiques.","https://pq.org/independance/");
        card(root,"Québec solidaire","Parti qui inscrit l’indépendance du Québec dans son projet politique. Accès à sa plateforme électorale et à ses propositions officielles.","https://quebecsolidaire.net/");
        card(root,"Climat Québec","Parti indépendantiste dont la plateforme présente son projet politique et ses orientations pour le Québec.","https://climat.quebec/");
        card(root,"Archives des programmes — Assemblée nationale","Guide non partisan de la Bibliothèque de l’Assemblée nationale recensant les programmes et slogans politiques québécois, y compris des formations indépendantistes historiques.","https://www.bibliotheque.assnat.qc.ca/guides/fr/3748-programmes-et-slogans-politiques");

        sectionTitle(root,"Mouvement, recherche et idées","Organisations, revue et institut de recherche liés au débat sur l’indépendance.");
        card(root,"Rassemblement pour un Pays Souverain (RPS)","Mouvement de la société civile qui promeut l’indépendance du Québec, la langue française et la connaissance de l’histoire nationale.","https://www.rps.quebec/");
        card(root,"L’Action nationale","Revue publiée par la Ligue d’action nationale. Carrefour souverainiste de réflexion critique sur les aspirations de la nation québécoise.","https://action-nationale.qc.ca/");
        card(root,"IRAI","Institut indépendant et non partisan consacré à la recherche scientifique, comparative et internationale sur l’autodétermination et l’indépendance.","https://irai.quebec/");

        sectionTitle(root,"QUB radio • 99,5 FM Montréal","Lecteur intégré du flux numérique QUB. La programmation QUB est aussi diffusée au 99,5 FM Montréal durant certaines plages.");
        radioStatus=tx("Prêt à écouter",14,INK,true);radioStatus.setPadding(0,dp(4),0,dp(4));root.addView(radioStatus);playButton=button("▶ Écouter QUB en direct");playButton.setOnClickListener(v->toggleRadio());root.addView(playButton);Button my=button("🌐 Ouvrir QUB sur myTuner");my.setOnClickListener(v->open("https://mytuner-radio.com/fr/radio/qub-radio-458512/"));root.addView(my);

        infoCard(root,"Méthode documentaire","Les contenus militants, partisans, éditoriaux, scientifiques, historiques et gouvernementaux sont séparés. La chronologie distingue conflit militaire, affrontement armé, crise politique, conflit constitutionnel et friction linguistique. Une estimation n’est jamais présentée comme une donnée officielle.");
        TextView footer=tx("OUI Québec • V4.7 • Base stable : V4.6.1",11,MUTED,true);footer.setGravity(Gravity.CENTER);footer.setPadding(0,dp(8),0,dp(8));root.addView(footer);

        scroll.addView(root);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(outer);outer.requestApplyInsets();
    }

    private void toggleRadio(){if(player!=null&&player.isPlaying()){player.pause();playButton.setText("▶ Reprendre QUB");radioStatus.setText("En pause");return;}if(player!=null){player.start();playButton.setText("⏸ Pause");radioStatus.setText("QUB en direct");return;}radioStatus.setText("Connexion au direct…");playButton.setEnabled(false);player=new MediaPlayer();player.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build());try{player.setDataSource("https://playerservices.streamtheworld.com/api/livestream-redirect/QUB_RADIO_SC");player.setOnPreparedListener(p->{playButton.setEnabled(true);p.start();playButton.setText("⏸ Pause");radioStatus.setText("QUB en direct");});player.setOnErrorListener((p,w,e)->{radioStatus.setText("Flux temporairement indisponible");playButton.setEnabled(true);playButton.setText("↻ Réessayer");releasePlayer();return true;});player.prepareAsync();}catch(Exception e){radioStatus.setText("Flux temporairement indisponible");playButton.setEnabled(true);playButton.setText("↻ Réessayer");releasePlayer();}}
    private void releasePlayer(){if(player!=null){try{player.release();}catch(Exception ignored){}player=null;}}
    @Override protected void onDestroy(){releasePlayer();super.onDestroy();}
}
