package quebec.maison.universelle;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.wifi.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class WifiAnalyzerActivity extends Activity {
    private WifiManager wm; private LinearLayout body,nav; private TextView title,bandText,summary;
    private int page=0, band=24; private final Handler h=new Handler(Looper.getMainLooper());
    private final Map<String,List<Integer>> history=new LinkedHashMap<>();
    private final Map<String,Deque<Integer>> radarSamples=new LinkedHashMap<>();
    private final Runnable tick=new Runnable(){public void run(){
        if(page==3){refresh();h.postDelayed(this,4000);}
        else if(page==4){refresh();h.postDelayed(this,700);}
    }};

    @Override public void onCreate(Bundle b){super.onCreate(b);wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);buildUi();requestNeeded();refresh();}
    @Override protected void onDestroy(){super.onDestroy();h.removeCallbacksAndMessages(null);}

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(10,18,15));root.setPadding(12,12,12,0);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        Button back=btn("‹");back.setTextSize(30);back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(70,70));
        title=txt("Access Points",22,true);top.addView(title,new LinearLayout.LayoutParams(0,70,1));
        Button help=btn("?");help.setTextSize(22);help.setOnClickListener(v->showHelp());top.addView(help,new LinearLayout.LayoutParams(60,70));
        bandText=txt("2.4\nGHz",18,true);bandText.setGravity(Gravity.CENTER);bandText.setOnClickListener(v->{band=band==24?5:band==5?6:24;refresh();});top.addView(bandText,new LinearLayout.LayoutParams(90,70));root.addView(top);
        summary=txt("Analyse Wi-Fi locale",14,false);summary.setPadding(8,4,8,12);root.addView(summary);
        ScrollView sc=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(0,4,0,8);sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);String[] n={"▰\nAccès","◎\nCanaux","▥\nGraphe","⌁\nTemps","◉\nRadar"};for(int i=0;i<5;i++){final int p=i;Button b1=btn(n[i]);b1.setOnClickListener(v->{page=p;h.removeCallbacks(tick);refresh();if(page==3)h.postDelayed(tick,4000);else if(page==4)h.postDelayed(tick,700);});nav.addView(b1,new LinearLayout.LayoutParams(0,86,1));}root.addView(nav);setContentView(root);
    }
    private TextView txt(String s,int sp,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(Color.WHITE);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setBackgroundColor(Color.TRANSPARENT);b.setAllCaps(false);return b;}

    private void requestNeeded(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.NEARBY_WIFI_DEVICES,Manifest.permission.ACCESS_FINE_LOCATION},40);else if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},40);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==40)refresh();}

    private List<ScanResult> results(){try{wm.startScan();List<ScanResult> all=wm.getScanResults();List<ScanResult> out=new ArrayList<>();for(ScanResult r:all)if(inBand(r.frequency))out.add(r);Collections.sort(out,(a,b)->Integer.compare(b.level,a.level));return out;}catch(Exception e){return new ArrayList<>();}}
    private boolean inBand(int f){return band==24?f>=2400&&f<2500:band==5?f>=4900&&f<5925:f>=5925&&f<7125;}
    private int channel(int f){if(f==2484)return 14;if(f>=2412&&f<=2472)return (f-2407)/5;if(f>=5000&&f<5925)return (f-5000)/5;if(f>=5955)return (f-5950)/5;return 0;}
    private String ssid(ScanResult r){String s=r.SSID;return s==null||s.trim().isEmpty()?"*hidden*":s;}
    private String bandName(){return band==24?"2.4 GHz":band==5?"5 GHz":"6 GHz";}

    private void refresh(){body.removeAllViews();bandText.setText(band==24?"2.4\nGHz":band==5?"5\nGHz":"6\nGHz");String[] t={"Access Points","Channel Rating","Channel Graph","Time Graph","Radar Wi-Fi"};title.setText(t[page]);List<ScanResult> rs=results();summary.setText(bandName()+" • "+rs.size()+" réseau(x) visible(s) • toucher 2.4/5/6 GHz pour changer de bande");if(page==0)showList(rs);else if(page==1)showRating(rs);else if(page==2)showChannelGraph(rs);else if(page==3)showTimeGraph(rs);else showRadar(rs);}

    private void showList(List<ScanResult> rs){if(rs.isEmpty()){body.addView(txt("Aucun point d’accès détecté. Vérifie que la localisation et l’autorisation Appareils à proximité sont permises.",16,false));return;}for(ScanResult r:rs){LinearLayout c=card();String line=ssid(r)+"  ("+r.BSSID+")";TextView a=txt(line,18,true);c.addView(a);int ch=channel(r.frequency);TextView b=txt(r.level+" dBm   CH "+ch+"   "+r.frequency+" MHz",16,true);b.setTextColor(signalColor(r.level));c.addView(b);c.addView(txt("Canal "+ch+" • "+bandName()+" • "+widthText(r.channelWidth)+"\n"+security(r.capabilities),14,false));c.setOnClickListener(v->details(r));body.addView(c);}}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(20,16,20,16);GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(47,47,47));g.setCornerRadius(28);c.setBackground(g);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,12);c.setLayoutParams(lp);return c;}
    private int signalColor(int l){return l>=-60?Color.rgb(0,230,100):l>=-70?Color.rgb(100,220,70):l>=-80?Color.rgb(255,190,0):Color.rgb(190,70,200);}
    private String widthText(int w){switch(w){case ScanResult.CHANNEL_WIDTH_40MHZ:return "40 MHz";case ScanResult.CHANNEL_WIDTH_80MHZ:return "80 MHz";case ScanResult.CHANNEL_WIDTH_160MHZ:return "160 MHz";case ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ:return "80+80 MHz";default:return "20 MHz";}}
    private String security(String c){if(c==null)return "Sécurité inconnue";if(c.contains("WPA3")||c.contains("SAE"))return "WPA3";if(c.contains("WPA2"))return "WPA2";if(c.contains("WPA"))return "WPA";if(c.contains("WEP"))return "WEP";return c.contains("ESS")?"Ouvert / ESS":c;}
    private void details(ScanResult r){new AlertDialog.Builder(this).setTitle(ssid(r)).setMessage("SSID : nom du réseau\nBSSID : "+r.BSSID+"\nSignal : "+r.level+" dBm\nCanal : "+channel(r.frequency)+"\nFréquence : "+r.frequency+" MHz\nLargeur : "+widthText(r.channelWidth)+"\nBande : "+bandName()+"\nSécurité : "+security(r.capabilities)+"\n\nPlus le dBm est près de 0, plus le signal est fort.").setPositiveButton("OK",null).show();}

    private void showHelp(){new AlertDialog.Builder(this).setTitle("Comprendre l’analyse Wi-Fi").setMessage(
        "dBm = puissance reçue. -30 est excellent, -50 très fort, -60 bon, -70 moyen, -80 faible, -90 très faible.\n\n"+
        "SSID = nom du réseau. BSSID = adresse radio unique du point d’accès.\n\n"+
        "Canal = portion de fréquence utilisée. Des réseaux voisins sur le même canal peuvent se gêner.\n\n"+
        "MHz = fréquence radio. 2,4 GHz porte plus loin; 5/6 GHz offrent généralement plus de débit à courte portée.\n\n"+
        "20/40/80/160 MHz = largeur du canal. Plus large peut donner plus de débit mais occupe davantage de spectre.\n\n"+
        "WPA2/WPA3 = protection du réseau. WEP est ancien et faible. *hidden* signifie que le nom du réseau n’est pas diffusé.\n\n"+
        "Radar = représentation RELATIVE fondée sur la puissance du signal et sa stabilité. Android ne fournit pas la direction réelle d’un routeur, donc l’angle affiché sert à séparer visuellement les sources; la distance au centre est l’information utile."
    ).setPositiveButton("Compris",null).show();}

    private void showRating(List<ScanResult> rs){Map<Integer,Integer> scores=new TreeMap<>();int[] candidates=band==24?new int[]{1,6,11}:band==5?new int[]{36,40,44,48,149,153,157,161}:new int[]{5,21,37,53,69,85,101,117,133,149,165,181,197,213,229};for(int ch:candidates)scores.put(ch,100);for(ScanResult r:rs){int c=channel(r.frequency);for(int ch:candidates){int dist=Math.abs(ch-c);int penalty=Math.max(0,45-dist*10);penalty=(int)(penalty*Math.max(.25,(100+r.level)/45.0));scores.put(ch,Math.max(0,scores.get(ch)-penalty));}}List<Map.Entry<Integer,Integer>> e=new ArrayList<>(scores.entrySet());e.sort((a,b)->Integer.compare(b.getValue(),a.getValue()));StringBuilder best=new StringBuilder("Meilleurs canaux : ");for(int i=0;i<Math.min(3,e.size());i++){if(i>0)best.append(", ");best.append(e.get(i).getKey());}TextView x=txt(best.toString(),20,true);x.setTextColor(Color.rgb(50,220,120));body.addView(x);for(Map.Entry<Integer,Integer> q:e){TextView row=txt("Canal "+q.getKey()+"     score "+q.getValue()+"/100",17,false);row.setPadding(8,14,8,14);body.addView(row);}}
    private void showChannelGraph(List<ScanResult> rs){GraphView g=new GraphView(this,false);g.setData(rs,band,history);body.addView(g,new LinearLayout.LayoutParams(-1,780));body.addView(txt("Plus le signal est près de -30 dBm, plus il est fort. Les réseaux qui se chevauchent utilisent des canaux voisins.",14,false));}
    private void showTimeGraph(List<ScanResult> rs){for(ScanResult r:rs){List<Integer> l=history.computeIfAbsent(r.BSSID,k->new ArrayList<>());l.add(r.level);if(l.size()>30)l.remove(0);}GraphView g=new GraphView(this,true);g.setData(rs,band,history);body.addView(g,new LinearLayout.LayoutParams(-1,780));body.addView(txt("Historique du signal actualisé environ toutes les 4 secondes. Android peut limiter la fréquence réelle des balayages Wi-Fi.",14,false));}

    private void showRadar(List<ScanResult> rs){
        for(ScanResult r:rs){Deque<Integer>d=radarSamples.computeIfAbsent(r.BSSID,k->new ArrayDeque<>());d.addLast(r.level);while(d.size()>8)d.removeFirst();}
        RadarView rv=new RadarView(this);rv.setData(rs,radarSamples);body.addView(rv,new LinearLayout.LayoutParams(-1,820));
        TextView note=txt("RADAR PASSIF • rafraîchissement visuel ~0,7 s\nLes mesures sont lissées sur plusieurs lectures rapprochées. Android peut réutiliser des résultats en cache ou limiter les scans. Le rayon indique la proximité radio relative; l’angle n’est pas une direction physique.",13,false);note.setTextColor(Color.rgb(100,255,170));note.setPadding(8,10,8,10);body.addView(note);
    }

    static class RadarView extends View {
        private List<ScanResult> rs=new ArrayList<>(); private Map<String,Deque<Integer>> samples=new LinkedHashMap<>();
        private Paint p=new Paint(1); private float sweep=0; private Handler anim=new Handler(Looper.getMainLooper());
        private Runnable spin=new Runnable(){public void run(){sweep=(sweep+5)%360;invalidate();anim.postDelayed(this,45);}};
        RadarView(Context c){super(c);p.setTypeface(Typeface.create(Typeface.MONOSPACE,Typeface.BOLD));anim.post(spin);}
        void setData(List<ScanResult> r,Map<String,Deque<Integer>> s){rs=new ArrayList<>(r);samples=s;invalidate();}
        @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();anim.removeCallbacksAndMessages(null);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);int w=getWidth(),hh=getHeight();float cx=w/2f,cy=hh/2f;float R=Math.min(w,hh)*0.43f;c.drawColor(Color.rgb(4,13,9));
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.rgb(0,75,40));for(int i=1;i<=4;i++)c.drawCircle(cx,cy,R*i/4f,p);c.drawLine(cx-R,cy,cx+R,cy,p);c.drawLine(cx,cy-R,cx,cy+R,p);
            for(int a=0;a<360;a+=30){double q=Math.toRadians(a);c.drawLine(cx,cy,cx+(float)Math.cos(q)*R,cy+(float)Math.sin(q)*R,p);}
            double sw=Math.toRadians(sweep);p.setStrokeWidth(5);p.setColor(Color.rgb(0,255,120));c.drawLine(cx,cy,cx+(float)Math.cos(sw)*R,cy+(float)Math.sin(sw)*R,p);
            p.setStyle(Paint.Style.FILL);p.setTextSize(22);p.setColor(Color.rgb(0,255,130));c.drawText("WIFI QUÉBEC • PASSIVE RADAR",18,30,p);p.setTextSize(17);c.drawText("CENTRE = signal plus fort",18,55,p);
            for(ScanResult r:rs){int avg=average(samples.get(r.BSSID),r.level);float norm=Math.max(0f,Math.min(1f,(-avg-30)/65f));float rad=40+norm*(R-55);int hsh=(r.BSSID==null?r.SSID:r.BSSID).hashCode();float ang=(Math.abs(hsh)%360);double a=Math.toRadians(ang);float x=cx+(float)Math.cos(a)*rad,y=cy+(float)Math.sin(a)*rad;int col=avg>=-60?Color.rgb(0,255,120):avg>=-75?Color.rgb(255,210,0):Color.rgb(220,70,220);p.setColor(col);c.drawCircle(x,y,9,p);p.setTextSize(16);String name=(r.SSID==null||r.SSID.isEmpty()?"hidden":r.SSID);if(name.length()>13)name=name.substring(0,13);c.drawText(name+" "+avg+"dBm",x+12,y-6,p);}
        }
        private int average(Deque<Integer>d,int fallback){if(d==null||d.isEmpty())return fallback;int s=0;for(int v:d)s+=v;return Math.round(s/(float)d.size());}
    }

    static class GraphView extends View {List<ScanResult> rs=new ArrayList<>();Map<String,List<Integer>> hist;int band;boolean time;Paint p=new Paint(1);GraphView(Context c,boolean t){super(c);time=t;p.setTextSize(26);}void setData(List<ScanResult> r,int b,Map<String,List<Integer>> h){rs=r;band=b;hist=h;invalidate();}
        protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(Color.rgb(23,23,23));int w=getWidth(),h=getHeight();p.setStrokeWidth(2);p.setColor(Color.DKGRAY);for(int db=-30;db>=-90;db-=10){float y=y(db,h);c.drawLine(55,y,w-15,y,p);p.setColor(Color.LTGRAY);c.drawText(String.valueOf(db),5,y+8,p);p.setColor(Color.DKGRAY);}int[] cols={Color.CYAN,Color.GREEN,Color.MAGENTA,Color.YELLOW,Color.RED,Color.BLUE,Color.rgb(255,120,0)};if(time){int k=0;for(Map.Entry<String,List<Integer>> e:hist.entrySet()){List<Integer> v=e.getValue();if(v.size()<2)continue;p.setColor(cols[k++%cols.length]);p.setStrokeWidth(4);Path path=new Path();for(int i=0;i<v.size();i++){float x=70+(w-100)*(i/(float)Math.max(1,v.size()-1));float yy=y(v.get(i),h);if(i==0)path.moveTo(x,yy);else path.lineTo(x,yy);}c.drawPath(path,p);}}else{int k=0;for(ScanResult r:rs){int ch=chan(r.frequency);float cx=xFor(ch,w,band);float half=band==24?55:38;p.setColor(cols[k++%cols.length]);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);Path q=new Path();float yy=y(r.level,h);q.moveTo(cx-half,h-55);q.quadTo(cx-half/2,yy,cx,yy);q.quadTo(cx+half/2,yy,cx+half,h-55);c.drawPath(q,p);p.setStyle(Paint.Style.FILL);c.drawText((r.SSID==null||r.SSID.isEmpty()?"*hidden*":r.SSID)+" "+ch,cx-half,yy-8,p);}}}
        private float y(int db,int h){return 45+(-30-db)*(h-100)/65f;}private static int chan(int f){if(f==2484)return 14;if(f>=2412&&f<=2472)return (f-2407)/5;if(f>=5000&&f<5925)return (f-5000)/5;if(f>=5955)return (f-5950)/5;return 0;}private float xFor(int ch,int w,int b){float min=b==24?1:b==5?32:1,max=b==24?14:b==5?177:233;return 65+(w-100)*(ch-min)/(max-min);}}
}
