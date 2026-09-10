package ca.quebec.oui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PollsActivity extends Activity {
    private static final int BLUE=Color.rgb(0,61,165), DARK=Color.rgb(0,31,91), INK=Color.rgb(20,33,61), MUTED=Color.rgb(82,98,125), SOFT=Color.rgb(244,247,252), WHITE=Color.WHITE, RED=Color.rgb(174,52,52), GOLD=Color.rgb(176,118,8);
    private static final String PREFS="polls_cache_v49";
    private static final String QC125="https://qc125.com/sondages-souv.htm";
    private LinearLayout pollBox, mediaBox;
    private TextView pollStatus, mediaStatus, latestSummary;
    private Button refresh;
    private SharedPreferences prefs;
    private int pendingLoads=0;

    private static final int[] YEARS={1976,1977,1978,1979,1980,1981,1982,1983,1984,1985,1988,1989,1990,1991,1992,1993,1994,1995,1996,1997,1998,1999,2000,2001,2002,2003,2004,2005,2006,2007,2008,2009,2010,2011,2012,2013,2014,2016,2017,2018,2020,2021,2022,2025,2026};
    private static final float[] YES={12f,16f,14f,30f,40.4f,34.7f,41f,38f,32f,34f,31.5f,33f,42f,48f,47.2f,46f,41f,49.4f,47.2f,48.6f,40.3f,43.9f,33.5f,39.5f,33f,41.3f,41.6f,44.6f,41f,37f,37f,34f,39.9f,37f,34f,33f,37f,37f,36f,37f,36f,36f,40.5f,35f,27f};

    @Override public void onCreate(Bundle b){super.onCreate(b);Locale.setDefault(Locale.CANADA_FRENCH);getWindow().setStatusBarColor(DARK);getWindow().setNavigationBarColor(DARK);prefs=getSharedPreferences(PREFS,MODE_PRIVATE);build();showCachedPolls();showCachedMedia();loadAll();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private GradientDrawable border(int c,int r,int stroke){GradientDrawable g=bg(c,r);g.setStroke(dp(1),stroke);return g;}
    private TextView tx(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);t.setLineSpacing(0,1.10f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(WHITE);b.setTextSize(14);b.setAllCaps(false);b.setBackground(bg(BLUE,14));b.setPadding(dp(12),0,dp(12),0);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.setMargins(0,dp(5),0,dp(5));b.setLayoutParams(p);return b;}
    private void open(String u){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}
    private void heading(LinearLayout r,String title,String sub){TextView h=tx(title,22,DARK,true);h.setPadding(0,dp(20),0,dp(4));r.addView(h);TextView s=tx(sub,13,MUTED,false);s.setPadding(0,0,0,dp(9));r.addView(s);}
    private void applyInsets(LinearLayout outer){outer.setOnApplyWindowInsetsListener((v,insets)->{int l=0,t=0,r=0,b=0;if(Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());l=i.left;t=i.top;r=i.right;b=i.bottom;}else{t=insets.getSystemWindowInsetTop();b=insets.getSystemWindowInsetBottom();}v.setPadding(l,t,r,b);return insets;});}

    private void build(){
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setBackgroundColor(WHITE);applyInsets(outer);
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setGravity(Gravity.CENTER);head.setPadding(dp(18),dp(15),dp(18),dp(15));head.setBackgroundColor(DARK);
        TextView h=tx("SONDAGES • INDÉPENDANCE",24,WHITE,true);h.setGravity(Gravity.CENTER);head.addView(h);TextView s=tx("Historique depuis 1976 • données récentes • médias en direct",13,Color.rgb(210,225,250),false);s.setGravity(Gravity.CENTER);head.addView(s);outer.addView(head);
        ScrollView scroll=new ScrollView(this);scroll.setClipToPadding(false);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(15),dp(14),dp(15),dp(32));
        latestSummary=tx("Chargement des données récentes…",16,DARK,true);latestSummary.setPadding(dp(14),dp(13),dp(14),dp(13));latestSummary.setBackground(border(Color.rgb(247,250,255),15,Color.rgb(205,220,243)));root.addView(latestSummary);
        refresh=button("↻ Actualiser toutes les données");refresh.setOnClickListener(v->loadAll());root.addView(refresh);
        heading(root,"Évolution historique","Vue de tendance. Les formulations ont changé avec le temps; les points ne constituent donc pas une série parfaitement homogène.");
        root.addView(new TrendView(),new LinearLayout.LayoutParams(-1,dp(285)));
        TextView warn=tx("1980 (40,4 %) et 1995 (49,4 %) sont des résultats référendaires, pas des sondages. Les autres points sont des repères de sondages historiques compilés. Les questions peuvent porter sur la souveraineté-association, la souveraineté ou l’indépendance pure, avec ou sans répartition des indécis.",12,MUTED,false);warn.setPadding(0,dp(8),0,dp(8));root.addView(warn);
        Button hist=button("Voir la série source Qc125 / 338Canada");hist.setOnClickListener(v->open(QC125));root.addView(hist);
        heading(root,"Sondages récents — source actualisée","Téléchargement direct de la table publique Qc125 à l’ouverture. Les valeurs sont affichées comme publiées avant répartition des indécis.");
        pollStatus=tx("Connexion à la source…",13,INK,true);pollStatus.setPadding(0,dp(3),0,dp(5));root.addView(pollStatus);pollBox=new LinearLayout(this);pollBox.setOrientation(LinearLayout.VERTICAL);root.addView(pollBox);
        heading(root,"Veille des médias québécois","Recherche automatique des articles récents liés aux sondages, à la souveraineté ou à l’indépendance. Un article de presse n’est pas lui-même une maison de sondage.");
        mediaStatus=tx("Recherche des articles récents…",13,INK,true);mediaStatus.setPadding(0,dp(3),0,dp(5));root.addView(mediaStatus);mediaBox=new LinearLayout(this);mediaBox.setOrientation(LinearLayout.VERTICAL);root.addView(mediaBox);
        TextView method=tx("MÉTHODE • Les données structurées proviennent de la table publique Qc125. La veille médias utilise Google Actualités et filtre des médias québécois reconnus. Une valeur détectée automatiquement dans un titre est signalée comme telle et doit être vérifiée dans l’article. En cas de panne réseau, l’application conserve le dernier résultat obtenu sur l’appareil.",12,MUTED,false);method.setPadding(dp(13),dp(13),dp(13),dp(13));method.setBackground(bg(SOFT,14));LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);mp.setMargins(0,dp(18),0,dp(8));method.setLayoutParams(mp);root.addView(method);
        TextView foot=tx("OUI Québec • Sondages V4.9 • mise à jour à la demande + cache hors ligne",11,MUTED,true);foot.setGravity(Gravity.CENTER);foot.setPadding(0,dp(8),0,dp(8));root.addView(foot);
        scroll.addView(root);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(outer);outer.requestApplyInsets();
    }

    private synchronized void loadAll(){if(pendingLoads>0)return;pendingLoads=2;refresh.setEnabled(false);refresh.setText("Actualisation en cours…");pollStatus.setText("Actualisation Qc125…");mediaStatus.setText("Actualisation médias…");loadPolls();loadMedia();}
    private synchronized void doneOne(){pendingLoads=Math.max(0,pendingLoads-1);if(pendingLoads==0)runOnUiThread(()->{refresh.setEnabled(true);refresh.setText("↻ Actualiser toutes les données");});}
    private String download(String url) throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setInstanceFollowRedirects(true);c.setRequestProperty("User-Agent","Mozilla/5.0 OUI-Quebec/4.9 Android");c.setRequestProperty("Accept-Language","fr-CA,fr;q=0.9");StringBuilder sb=new StringBuilder();try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8))){String line;while((line=br.readLine())!=null)sb.append(line).append('\n');}finally{c.disconnect();}return sb.toString();}
    private String clean(String s){return Html.fromHtml(s,Html.FROM_HTML_MODE_LEGACY).toString().replace('\u00a0',' ').replaceAll("\\s+"," ").trim();}

    private void loadPolls(){new Thread(()->{List<Poll> list=new ArrayList<>();try{String html=download(QC125);Pattern tr=Pattern.compile("(?is)<tr[^>]*>(.*?)</tr>"),td=Pattern.compile("(?is)<td[^>]*>(.*?)</td>");Matcher rows=tr.matcher(html);while(rows.find()&&list.size()<20){Matcher cells=td.matcher(rows.group(1));List<String> vals=new ArrayList<>();while(cells.find())vals.add(clean(cells.group(1)));if(vals.size()<5)continue;Matcher meta=Pattern.compile("(.+?)\\s+(20\\d{2}-\\d{2}-\\d{2}).*?n\\s*[=  ]\\s*([0-9 , ]+)").matcher(vals.get(0));if(!meta.find())continue;List<Integer> nums=new ArrayList<>();for(int i=1;i<vals.size();i++){String v=vals.get(i).replace("%","").trim();if(v.matches("\\d{1,3}")){int n=Integer.parseInt(v);if(n>=0&&n<=100)nums.add(n);}}if(nums.size()>=3){int no=nums.get(0),yes=nums.get(1),und=nums.get(2);String sample=meta.group(3).replaceAll("[^0-9]","");list.add(new Poll(meta.group(2),meta.group(1).trim(),yes,no,und,sample));}}if(list.isEmpty())throw new Exception("table non reconnue");savePolls(list);}catch(Exception ignored){}final List<Poll> out=list;runOnUiThread(()->{if(!out.isEmpty())showPolls(out,false);else{List<Poll> cached=readPolls();if(!cached.isEmpty()){showPolls(cached,true);pollStatus.setText("Mode hors ligne • dernière copie enregistrée");}else pollStatus.setText("Source temporairement indisponible. Réessaie plus tard.");}doneOne();});}).start();}
    private void showPolls(List<Poll> list,boolean cached){pollBox.removeAllViews();if(list.isEmpty())return;Poll p0=list.get(0);latestSummary.setText("Dernier sondage structuré : "+p0.yes+" % OUI • "+p0.no+" % NON • "+p0.und+" % indécis\n"+p0.firm+" • "+p0.date+" • n="+p0.sample);String stamp=prefs.getString("poll_stamp","");pollStatus.setText((cached?"Copie locale":"Source Qc125 actualisée")+(stamp.isEmpty()?"":" • "+stamp));int max=Math.min(12,list.size());for(int i=0;i<max;i++){Poll p=list.get(i);LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(11),dp(14),dp(11));c.setBackground(bg(SOFT,14));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(4),0,dp(4));c.setLayoutParams(cp);c.addView(tx(p.date+" • "+p.firm+(p.sample.isEmpty()?"":" • n="+p.sample),12,MUTED,true));TextView score=tx("OUI "+p.yes+" %     NON "+p.no+" %     INDÉCIS "+p.und+" %",15,INK,true);score.setPadding(0,dp(4),0,0);c.addView(score);c.setOnClickListener(v->open(QC125));pollBox.addView(c);}}
    private void savePolls(List<Poll> list){StringBuilder s=new StringBuilder();for(Poll p:list)s.append(p.date).append('\t').append(p.firm.replace("\t"," ")).append('\t').append(p.yes).append('\t').append(p.no).append('\t').append(p.und).append('\t').append(p.sample).append('\n');prefs.edit().putString("polls",s.toString()).putString("poll_stamp",now()).apply();}
    private List<Poll> readPolls(){List<Poll> out=new ArrayList<>();for(String row:prefs.getString("polls","").split("\\n")){String[] a=row.split("\\t",-1);if(a.length>=6)try{out.add(new Poll(a[0],a[1],Integer.parseInt(a[2]),Integer.parseInt(a[3]),Integer.parseInt(a[4]),a[5]));}catch(Exception ignored){}}return out;}
    private void showCachedPolls(){List<Poll> c=readPolls();if(!c.isEmpty())showPolls(c,true);}

    private boolean isQuebecMedia(String source){if(source==null)return false;String s=source.toLowerCase(Locale.CANADA_FRENCH);String[] allowed={"journal de québec","journal de montreal","journal de montréal","tva nouvelles","qub","la presse","le devoir","radio-canada","noovo","98.5","le soleil","l'actualité","l’actualité","24 heures","métro"};for(String a:allowed)if(s.contains(a))return true;return false;}
    private void loadMedia(){new Thread(()->{List<Article> found=new ArrayList<>();try{String q=URLEncoder.encode("(sondage souveraineté Québec) OR (sondage indépendance Québec) OR (référendum souveraineté Québec)",StandardCharsets.UTF_8.name());HttpURLConnection c=(HttpURLConnection)new URL("https://news.google.com/rss/search?q="+q+"&hl=fr-CA&gl=CA&ceid=CA:fr").openConnection();c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setRequestProperty("User-Agent","OUI-Quebec/4.9");try(InputStream in=c.getInputStream()){XmlPullParser p=XmlPullParserFactory.newInstance().newPullParser();p.setInput(in,"UTF-8");String title=null,link=null,date=null,source=null,tag=null;boolean item=false;int ev=p.getEventType();while(ev!=XmlPullParser.END_DOCUMENT){if(ev==XmlPullParser.START_TAG){tag=p.getName();if("item".equals(tag)){item=true;title=link=date=source=null;}}else if(ev==XmlPullParser.TEXT&&item&&tag!=null){String v=p.getText();if("title".equals(tag))title=v;else if("link".equals(tag))link=v;else if("pubDate".equals(tag))date=v;else if("source".equals(tag))source=v;}else if(ev==XmlPullParser.END_TAG){if("item".equals(p.getName())){if(isQuebecMedia(source)&&title!=null&&link!=null)found.add(new Article(clean(title),link,date,source));item=false;}tag=null;}ev=p.next();}}finally{c.disconnect();}if(!found.isEmpty())saveMedia(found);}catch(Exception ignored){}final List<Article> out=found;runOnUiThread(()->{if(!out.isEmpty())showMedia(out,false);else{List<Article> cached=readMedia();if(!cached.isEmpty()){showMedia(cached,true);mediaStatus.setText("Mode hors ligne • derniers articles enregistrés");}else mediaStatus.setText("Aucun article compatible trouvé pour le moment.");}doneOne();});}).start();}
    private void showMedia(List<Article> list,boolean cached){mediaBox.removeAllViews();String stamp=prefs.getString("media_stamp","");mediaStatus.setText((cached?"Copie locale":"Veille médias actualisée")+" • "+Math.min(12,list.size())+" article(s)"+(stamp.isEmpty()?"":" • "+stamp));int max=Math.min(12,list.size());for(int i=0;i<max;i++){Article a=list.get(i);LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(11),dp(14),dp(11));c.setBackground(border(Color.rgb(249,251,255),14,Color.rgb(220,229,243)));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(4),0,dp(4));c.setLayoutParams(cp);c.addView(tx(a.source+(a.date==null||a.date.isEmpty()?"":" • "+a.date),12,MUTED,true));c.addView(tx(a.title,15,INK,true));Float pct=extractPct(a.title);if(pct!=null){TextView d=tx("Valeur détectée dans le titre : "+String.format(Locale.CANADA_FRENCH,"%.1f %%",pct)+" • vérifier le contexte dans l’article",12,GOLD,true);d.setPadding(0,dp(4),0,0);c.addView(d);}c.setOnClickListener(v->open(a.link));mediaBox.addView(c);}}
    private void saveMedia(List<Article> list){StringBuilder s=new StringBuilder();int max=Math.min(20,list.size());for(int i=0;i<max;i++){Article a=list.get(i);s.append(a.source.replace("\t"," ")).append('\t').append((a.date==null?"":a.date).replace("\t"," ")).append('\t').append(a.title.replace("\t"," ").replace("\n"," ")).append('\t').append(a.link).append('\n');}prefs.edit().putString("media",s.toString()).putString("media_stamp",now()).apply();}
    private List<Article> readMedia(){List<Article> out=new ArrayList<>();for(String row:prefs.getString("media","").split("\\n")){String[] a=row.split("\\t",4);if(a.length==4)out.add(new Article(a[2],a[3],a[1],a[0]));}return out;}
    private void showCachedMedia(){List<Article> c=readMedia();if(!c.isEmpty())showMedia(c,true);}
    private Float extractPct(String s){if(s==null)return null;Matcher m=Pattern.compile("(?i)(?:oui|appui|souverainet[ée]|ind[ée]pendance)[^0-9]{0,35}(\\d{2}(?:[.,]\\d)?)\\s*%").matcher(s);if(m.find())try{return Float.parseFloat(m.group(1).replace(',','.'));}catch(Exception ignored){}return null;}
    private String now(){return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT,Locale.CANADA_FRENCH).format(new Date());}
    private static class Poll{String date,firm,sample;int yes,no,und;Poll(String d,String f,int y,int n,int u,String s){date=d;firm=f;yes=y;no=n;und=u;sample=s;}}
    private static class Article{String title,link,date,source;Article(String t,String l,String d,String s){title=t;link=l;date=d;source=s;}}

    private class TrendView extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);TrendView(){super(PollsActivity.this);setBackground(bg(SOFT,14));setContentDescription("Graphique de tendance de l'appui à l'indépendance depuis 1976");}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float L=dp(42),R=getWidth()-dp(12),T=dp(18),B=getHeight()-dp(34);p.setTypeface(Typeface.DEFAULT);p.setTextSize(dp(10));p.setStrokeWidth(dp(1));for(int y=0;y<=60;y+=10){float py=B-(B-T)*y/60f;p.setColor(Color.rgb(215,224,238));c.drawLine(L,py,R,py,p);p.setColor(MUTED);c.drawText(y+"%",dp(7),py+dp(4),p);}float fifty=B-(B-T)*50f/60f;p.setColor(Color.rgb(160,160,160));c.drawLine(L,fifty,R,fifty,p);int[] ticks={1976,1980,1990,1995,2000,2010,2020,2026};for(int yr:ticks){float x=L+(R-L)*(yr-1976f)/50f;p.setColor(MUTED);c.drawText(String.valueOf(yr),x-dp(11),B+dp(20),p);}Path path=new Path();for(int i=0;i<YEARS.length;i++){float x=L+(R-L)*(YEARS[i]-1976f)/50f;float y=B-(B-T)*YES[i]/60f;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(BLUE);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);for(int i=0;i<YEARS.length;i++){float x=L+(R-L)*(YEARS[i]-1976f)/50f;float y=B-(B-T)*YES[i]/60f;p.setColor((YEARS[i]==1980||YEARS[i]==1995)?RED:BLUE);c.drawCircle(x,y,(YEARS[i]==1980||YEARS[i]==1995)?dp(5):dp(2),p);}p.setColor(RED);p.setTextSize(dp(10));float x80=L+(R-L)*4f/50f,y80=B-(B-T)*40.4f/60f;c.drawText("1980",x80+dp(4),y80-dp(5),p);float x95=L+(R-L)*19f/50f,y95=B-(B-T)*49.4f/60f;c.drawText("1995",x95+dp(4),y95-dp(5),p);}
    }
}
