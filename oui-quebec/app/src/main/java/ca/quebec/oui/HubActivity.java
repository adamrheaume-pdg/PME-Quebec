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
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class HubActivity extends Activity {
    private static final int BLUE=Color.rgb(0,61,165), DARK=Color.rgb(0,31,91), INK=Color.rgb(20,33,61), MUTED=Color.rgb(82,98,125), SOFT=Color.rgb(244,247,252), WHITE=Color.WHITE;
    private MediaPlayer player;
    private Button playButton;
    private TextView radioStatus;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(DARK);
        getWindow().setNavigationBarColor(DARK);
        build();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private TextView tx(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setLineSpacing(0,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(WHITE);b.setTextSize(14);b.setAllCaps(false);b.setBackground(bg(BLUE,14));b.setPadding(dp(14),0,dp(14),0);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.setMargins(0,dp(6),0,dp(6));b.setLayoutParams(p);return b;}
    private void open(String u){startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));}
    private void card(LinearLayout root,String title,String body,String url){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(15),dp(16),dp(15));c.setBackground(bg(SOFT,16));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(12));c.setLayoutParams(cp);
        c.addView(tx(title,18,DARK,true));TextView d=tx(body,14,INK,false);d.setPadding(0,dp(5),0,dp(7));c.addView(d);Button o=button("Ouvrir la source");o.setOnClickListener(v->open(url));c.addView(o);root.addView(c);
    }

    private void build(){
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setBackgroundColor(WHITE);
        outer.setOnApplyWindowInsetsListener((v,insets)->{int top=0,bottom=0;if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());top=i.top;bottom=i.bottom;}else{top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();}v.setPadding(0,top,0,bottom);return insets;});
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(18),dp(16),dp(18),dp(16));head.setBackgroundColor(DARK);head.addView(tx("⚜  OUI QUÉBEC",27,WHITE,true));head.addView(tx("Centre documentaire • V4.2",13,Color.rgb(210,225,250),false));outer.addView(head);
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(15),dp(16),dp(15),dp(28));
        Button app=button("Ouvrir les données, le Livre bleu et le simulateur");app.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));root.addView(app);
        TextView h=tx("Mouvement et recherche",22,DARK,true);h.setPadding(0,dp(18),0,dp(10));root.addView(h);
        card(root,"Rassemblement pour un Pays Souverain (RPS)","Mouvement de la société civile, sans attaches partisanes selon sa présentation, qui promeut l’indépendance du Québec, la langue française et la connaissance de l’histoire nationale.","https://www.rps.quebec/");
        card(root,"L’Action nationale","Revue publiée par la Ligue d’action nationale. Sa mission se présente comme un carrefour souverainiste de réflexion critique sur les aspirations de la nation québécoise.","https://action-nationale.qc.ca/");
        card(root,"IRAI","Institut de recherche sur l’autodétermination des peuples et les indépendances nationales. Institut indépendant et non partisan consacré à la recherche scientifique, comparative et internationale sur l’autodétermination et l’indépendance.","https://irai.quebec/");
        TextView rh=tx("QUB radio • 99,5 FM Montréal",22,DARK,true);rh.setPadding(0,dp(12),0,dp(5));root.addView(rh);root.addView(tx("Lecteur du flux numérique QUB. La programmation QUB est aussi diffusée simultanément au 99,5 FM Montréal durant certaines plages. Le lecteur Internet peut différer du signal FM hors de ces plages.",14,MUTED,false));
        radioStatus=tx("Prêt à écouter",14,INK,true);radioStatus.setPadding(0,dp(10),0,dp(5));root.addView(radioStatus);
        playButton=button("▶ Écouter QUB en direct");playButton.setOnClickListener(v->toggleRadio());root.addView(playButton);
        Button qub=button("Ouvrir QUB");qub.setOnClickListener(v->open("https://www.qub.ca/accueil/qubradio-accueil"));root.addView(qub);
        TextView note=tx("Sources externes : leur contenu et leur disponibilité demeurent sous la responsabilité de leurs éditeurs. Les contenus militants, éditoriaux, scientifiques et gouvernementaux restent identifiés séparément dans l’application.",12,MUTED,false);note.setPadding(0,dp(18),0,0);root.addView(note);
        scroll.addView(root);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(outer);outer.requestApplyInsets();
    }

    private void toggleRadio(){
        if(player!=null && player.isPlaying()){player.pause();playButton.setText("▶ Reprendre QUB");radioStatus.setText("En pause");return;}
        if(player!=null){player.start();playButton.setText("⏸ Pause");radioStatus.setText("QUB en direct");return;}
        radioStatus.setText("Connexion au direct…");playButton.setEnabled(false);
        player=new MediaPlayer();player.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build());
        try{player.setDataSource("https://playerservices.streamtheworld.com/api/livestream-redirect/QUB_RADIO_SC");player.setOnPreparedListener(p->{playButton.setEnabled(true);p.start();playButton.setText("⏸ Pause");radioStatus.setText("QUB en direct");});player.setOnErrorListener((p,w,e)->{radioStatus.setText("Flux indisponible — utilisez « Ouvrir QUB »");playButton.setEnabled(true);playButton.setText("↻ Réessayer");releasePlayer();return true;});player.prepareAsync();}catch(Exception e){radioStatus.setText("Flux indisponible — utilisez « Ouvrir QUB »");playButton.setEnabled(true);releasePlayer();}
    }
    private void releasePlayer(){if(player!=null){try{player.release();}catch(Exception ignored){}player=null;}}
    @Override protected void onDestroy(){releasePlayer();super.onDestroy();}
}
