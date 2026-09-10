package ca.quebec.oui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PollsActivity extends Activity {
    private static final int BLUE=Color.rgb(0,61,165), DARK=Color.rgb(0,31,91), INK=Color.rgb(20,33,61), MUTED=Color.rgb(82,98,125), SOFT=Color.rgb(244,247,252), WHITE=Color.WHITE;
    private LinearLayout liveBox;
    private TextView liveStatus;

    private static final int[] YEARS={1976,1977,1978,1979,1980,1981,1982,1983,1984,1985,1988,1989,1990,1991,1992,1993,1994,1995,1996,1997,1998,1999,2000,2001,2002,2003,2004,2005,2006,2007,2008,2009,2010,2011,2012,2013,2014,2016,2017,2018,2020,2021,2022,2025,2026};
    private static final float[] YES={12f,16f,14f,30f,40.4f,34.7f,41f,38f,32f,34f,31.5f,33f,42f,48f,47.2f,46f,41f,49.4f,47.2f,48.6f,40.3f,43.9f,33.5f,39.5f,33f,41.3f,41.6f,44.6f,41f,37f,37f,34f,39.9f,37f,34f,33f,37f,37f,36f,37f,36f,36f,40.5f,35f,27f};
    private static final String[] NOTE={
        "Maurice Pinard","Sorecom","INCI","CROP","Résultat référendum 1980","CROP","CROP","CROP","CROP","Sorecom","CROP","Gallup","Gallup","CROP","CROP","CROP","CROP","Résultat référendum 1995","Léger","Léger","Léger","Léger","CROP","Léger","Léger","CROP","CROP","CROP","Léger","Léger","CROP","Angus Reid","Léger","Léger","CROP (nov.)","Léger","Léger","CROP/Léger repères","Léger","Léger","Léger","Mainstreet repère","GROP/Léger — indépendance pure","Léger — oct. 2025","Léger — 5 sept. 2026"
    };

    private static final String[][] RECENT_2026={
        {"4 jan.","Spark Advocacy","24","61","15"},{"10 jan.","Pallas Data","35","54","10"},{"12 jan.","Ipsos","26","55","20"},{"24 jan.","Innovative Research","31","58","11"},{"28 jan.","Léger","29","62","9"},{"4 févr.","Angus Reid","27","63","10"},{"22 févr.","Pallas Data","32","60","8"},{"1 mars","Léger","26","65","9"},{"14 avr.","Pallas Data","31","63","6"},{"19 avr.","Léger","32","59","9"},{"27 avr.","Liaison Strategies","36","59","5"},{"8 mai","Pallas Data","31","59","10"},{"9 mai","Synopsis Recherche","28","63","9"},{"15 mai","Mainstreet Research","21","70","9"},{"17 mai","Léger","29","62","9"},{"7 juin","Synopsis Recherche","27","65","8"},{"11 juin","Pallas Data","33","56","11"},{"14 juin","Léger","29","63","8"},{"2 août","Pallas Data","29","59","12"},{"8 août","Léger","28","63","9"},{"8 août","Synopsis Recherche","28","62","10"},{"25 août","Synopsis Recherche","29","63","8"},{"5 sept.","Léger","27","62","11"}
    };

    @Override public void onCreate(Bundle b){super.onCreate(b);Locale.setDefault(Locale.CANADA_FRENCH);build();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private TextView tx(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);t.setLineSpacing(0,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(WHITE);b.setTextSize(14);b.setAllCaps(false);b.setBackground(bg(BLUE,14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.setMargins(0,dp(5),0,dp(5));b.setLayoutParams(p);return b;}
    private void open(String u){startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));}

    private void build(){
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setBackgroundColor(WHITE);outer.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(i.left,i.top,i.right,i.bottom);return insets;});
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(18),dp(14),dp(18),dp(14));head.setBackgroundColor(DARK);TextView h=tx("SONDAGES • INDÉPENDANCE",24,WHITE,true);h.setGravity(Gravity.CENTER);head.addView(h);TextView s=tx("Historique depuis 1976 + veille des médias québécois",13,Color.rgb(210,225,250),false);s.setGravity(Gravity.CENTER);head.addView(s);outer.addView(head);
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(15),dp(16),dp(15),dp(28));
        root.addView(tx("Évolution historique",22,DARK,true));TextView warning=tx("Attention : les questions posées ne sont pas identiques d’une époque à l’autre (souveraineté-association, question référendaire de 1995, indépendance pure, indécis répartis ou non). Le graphique sert à visualiser la tendance, pas à prétendre que toutes les vagues sont parfaitement comparables.",13,MUTED,false);warning.setPadding(0,dp(5),0,dp(10));root.addView(warning);
        TrendView chart=new TrendView();root.addView(chart,new LinearLayout.LayoutParams(-1,dp(270)));
        TextView legend=tx("Repères : 1980 = 40,4 % (résultat référendaire) • 1995 = 49,4 % (résultat référendaire) • 2022 = 40,5 % GROP/Léger, question d’indépendance pure • oct. 2025 = 35 % Léger • 5 sept. 2026 = 27 % Léger (11 % indécis).",12,MUTED,false);legend.setPadding(0,dp(8),0,dp(12));root.addView(legend);
        Button sources=button("Sources et séries détaillées");sources.setOnClickListener(v->open("https://qc125.com/sondages-souv.htm"));root.addView(sources);

        TextView rtitle=tx("Sondages 2026 — vagues publiées",22,DARK,true);rtitle.setPadding(0,dp(20),0,dp(6));root.addView(rtitle);root.addView(tx("Oui / Non / Indécis. Les méthodes, tailles d’échantillon et formulations peuvent varier selon les firmes.",13,MUTED,false));
        for(String[] r:RECENT_2026){TextView row=tx(r[0]+"  •  "+r[1]+"\nOUI "+r[2]+" %   NON "+r[3]+" %   INDÉCIS "+r[4]+" %",14,INK,true);row.setPadding(dp(14),dp(11),dp(14),dp(11));row.setBackground(bg(SOFT,14));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.setMargins(0,dp(4),0,dp(4));row.setLayoutParams(rp);root.addView(row);}

        TextView ltitle=tx("Veille médias du Québec — en direct",22,DARK,true);ltitle.setPadding(0,dp(22),0,dp(5));root.addView(ltitle);root.addView(tx("La veille recherche les nouveaux articles qui parlent de sondages sur la souveraineté ou l’indépendance dans les principaux médias québécois. Appuie sur Actualiser pour refaire la recherche au moment où tu consultes l’application.",13,MUTED,false));
        liveStatus=tx("Aucune actualisation effectuée.",13,INK,true);liveStatus.setPadding(0,dp(10),0,dp(4));root.addView(liveStatus);Button refresh=button("↻ Actualiser les sondages dans les médias");refresh.setOnClickListener(v->loadLive());root.addView(refresh);liveBox=new LinearLayout(this);liveBox.setOrientation(LinearLayout.VERTICAL);root.addView(liveBox);
        TextView src=tx("Sources de référence intégrées : Qc125/338Canada pour les séries récentes, archives et compilations historiques renvoyant notamment à Léger, CROP, Gallup, Angus Reid, Mainstreet, Pallas, Synopsis, Ipsos et BAnQ. La veille médias est une recherche d’articles, pas une maison de sondage.",12,MUTED,false);src.setPadding(0,dp(18),0,0);root.addView(src);
        scroll.addView(root);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(outer);outer.requestApplyInsets();
    }

    private boolean isQuebecMedia(String source){if(source==null)return false;String s=source.toLowerCase(Locale.CANADA_FRENCH);String[] allowed={"journal de québec","journal de montreal","journal de montréal","tva nouvelles","qub","la presse","le devoir","radio-canada","noovo","98.5","le soleil","l'actualité","l’actualité","24 heures","métro"};for(String a:allowed)if(s.contains(a))return true;return false;}
    private void loadLive(){liveStatus.setText("Recherche en cours…");liveBox.removeAllViews();new Thread(()->{List<Article> found=new ArrayList<>();try{String q=URLEncoder.encode("sondage souveraineté Québec OR sondage indépendance Québec", StandardCharsets.UTF_8.name());URL u=new URL("https://news.google.com/rss/search?q="+q+"&hl=fr-CA&gl=CA&ceid=CA:fr");HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(9000);c.setReadTimeout(9000);c.setRequestProperty("User-Agent","OUI-Quebec/4.8");try(InputStream in=c.getInputStream()){XmlPullParser p=XmlPullParserFactory.newInstance().newPullParser();p.setInput(in,"UTF-8");String title=null,link=null,date=null,source=null,tag=null;boolean item=false;int ev=p.getEventType();while(ev!=XmlPullParser.END_DOCUMENT){if(ev==XmlPullParser.START_TAG){tag=p.getName();if("item".equals(tag)){item=true;title=link=date=source=null;}}else if(ev==XmlPullParser.TEXT&&item&&tag!=null){String v=p.getText();if("title".equals(tag))title=v;else if("link".equals(tag))link=v;else if("pubDate".equals(tag))date=v;else if("source".equals(tag))source=v;}else if(ev==XmlPullParser.END_TAG){if("item".equals(p.getName())){if(isQuebecMedia(source)&&title!=null&&link!=null)found.add(new Article(title,link,date,source));item=false;}tag=null;}ev=p.next();}}}catch(Exception ignored){}
            runOnUiThread(()->showLive(found));}).start();}
    private void showLive(List<Article> a){liveBox.removeAllViews();if(a.isEmpty()){liveStatus.setText("Aucun nouvel article compatible trouvé pour le moment.");return;}liveStatus.setText(a.size()+" article(s) trouvé(s) dans les médias québécois");int max=Math.min(12,a.size());for(int i=0;i<max;i++){Article x=a.get(i);LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(13),dp(11),dp(13),dp(11));c.setBackground(bg(SOFT,14));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(5),0,dp(5));c.setLayoutParams(cp);c.addView(tx(x.source+(x.date==null?"":" • "+x.date),12,MUTED,true));c.addView(tx(Html.fromHtml(x.title,Html.FROM_HTML_MODE_LEGACY).toString(),15,INK,true));Float pct=extractYes(x.title);if(pct!=null)c.addView(tx("Pourcentage d’appui détecté dans le titre : "+String.format(Locale.CANADA_FRENCH,"%.1f %%",pct)+" — à vérifier dans l’article.",12,BLUE,true));c.setOnClickListener(v->open(x.link));liveBox.addView(c);}}
    private Float extractYes(String s){if(s==null)return null;Pattern[] ps={Pattern.compile("(?i)(?:oui|appui|souverainet[ée]|ind[ée]pendance)[^0-9]{0,30}(\\d{2}(?:[.,]\\d)?)\\s*%"),Pattern.compile("(?i)(\\d{2}(?:[.,]\\d)?)\\s*%[^a-zA-ZÀ-ÿ]{0,10}(?:pour|d['’]appui)[^.]{0,25}(?:souverainet[ée]|ind[ée]pendance)")};for(Pattern p:ps){Matcher m=p.matcher(s);if(m.find()){try{return Float.parseFloat(m.group(1).replace(',','.'));}catch(Exception ignored){}}}return null;}
    private static class Article{String title,link,date,source;Article(String t,String l,String d,String s){title=t;link=l;date=d;source=s;}}

    private class TrendView extends View{
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);TrendView(){super(PollsActivity.this);setBackground(bg(SOFT,14));setPadding(dp(8),dp(8),dp(8),dp(8));}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float L=dp(38),R=getWidth()-dp(10),T=dp(16),B=getHeight()-dp(30);p.setTypeface(Typeface.DEFAULT);p.setTextSize(dp(10));p.setStrokeWidth(dp(1));for(int y=0;y<=60;y+=10){float py=B-(B-T)*y/60f;p.setColor(Color.rgb(215,224,238));c.drawLine(L,py,R,py,p);p.setColor(MUTED);c.drawText(y+"%",dp(5),py+dp(4),p);}int[] ticks={1976,1980,1990,1995,2000,2010,2020,2026};for(int yr:ticks){float x=L+(R-L)*(yr-1976f)/(2026f-1976f);p.setColor(MUTED);c.drawText(String.valueOf(yr),x-dp(10),B+dp(18),p);}Path path=new Path();for(int i=0;i<YEARS.length;i++){float x=L+(R-L)*(YEARS[i]-1976f)/(2026f-1976f);float y=B-(B-T)*YES[i]/60f;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(BLUE);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);for(int i=0;i<YEARS.length;i++){float x=L+(R-L)*(YEARS[i]-1976f)/(2026f-1976f);float y=B-(B-T)*YES[i]/60f;p.setColor((YEARS[i]==1980||YEARS[i]==1995)?Color.rgb(194,104,16):BLUE);c.drawCircle(x,y,dp(3),p);} }
    }
}
