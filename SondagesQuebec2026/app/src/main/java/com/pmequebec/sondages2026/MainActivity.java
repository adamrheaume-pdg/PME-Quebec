package com.pmequebec.sondages2026;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private final List<Poll> polls = new ArrayList<>();
    private LinearLayout content;
    private TextView status;
    private TrendView trendView;

    private static final int BLUE = Color.rgb(0,31,151);
    private static final int NAVY = Color.rgb(0,19,67);
    private static final int LIGHT = Color.rgb(245,248,255);
    private static final int BORDER = Color.rgb(218,226,243);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPolls();
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(LIGHT);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(14), dp(14), dp(30));
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("SONDAGES QUÉBEC 2026", 26, Color.WHITE, true);
        title.setPadding(dp(16), dp(16), dp(16), dp(6));
        title.setBackgroundColor(BLUE);
        content.addView(title, matchWrap());

        TextView subtitle = text("Intentions de vote • toutes les vagues 2026 intégrées • lecture simple et neutre", 14, Color.WHITE, false);
        subtitle.setPadding(dp(16), 0, dp(16), dp(14));
        subtitle.setBackgroundColor(BLUE);
        content.addView(subtitle, matchWrap());

        status = text("● Données locales prêtes — dernière vague intégrée : 6 septembre 2026", 13, Color.rgb(24,120,74), true);
        status.setPadding(0, dp(12), 0, dp(8));
        content.addView(status);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setWeightSum(2f);
        Button refresh = button("ACTUALISER");
        Button live = button("FLUX DIRECT");
        actions.addView(refresh, weighted());
        actions.addView(live, weighted());
        content.addView(actions, matchWrap());

        refresh.setOnClickListener(v -> {
            String now = new SimpleDateFormat("HH:mm:ss", Locale.CANADA_FRENCH).format(new Date());
            status.setText("● Tableau recalculé à " + now + " — ouvrir Flux direct pour les publications les plus récentes");
            addSummary(true);
        });
        live.setOnClickListener(v -> openLive());

        addSummary(false);

        TextView note = text("MÉTHODE", 18, NAVY, true);
        note.setPadding(0, dp(18), 0, dp(6));
        content.addView(note);
        TextView methodology = text("La moyenne affichée est une moyenne simple des 5 derniers sondages de population générale. Elle n'est ni une projection de sièges ni une moyenne pondérée. Les sondages ont des modes, dates et marges d'incertitude différents.", 13, Color.DKGRAY, false);
        content.addView(methodology);

        TextView sources = text("Sources publiques de vérification : Vote-Scope, Qc125, Le Québec Vote, Léger, Pallas Data et pages des firmes. Le bouton Flux direct ouvre les agrégateurs publics dans l'application.", 12, Color.GRAY, false);
        sources.setPadding(0, dp(8), 0, 0);
        content.addView(sources);

        setContentView(scroll);
    }

    private void addSummary(boolean replace) {
        if (replace) {
            while (content.getChildCount() > 5) content.removeViewAt(5);
        }

        TextView h1 = text("Moyenne simple — 5 derniers sondages", 20, NAVY, true);
        h1.setPadding(0, dp(18), 0, dp(8));
        content.addView(h1);

        int start = Math.max(0, polls.size() - 5);
        float[] avg = new float[5];
        for (int i=start; i<polls.size(); i++) {
            Poll p = polls.get(i);
            avg[0]+=p.pq; avg[1]+=p.caq; avg[2]+=p.plq; avg[3]+=p.pcq; avg[4]+=p.qs;
        }
        int count = polls.size() - start;
        String[] names = {"PQ","CAQ","PLQ","PCQ","QS"};
        int[] colors = {Color.rgb(18,88,190), Color.rgb(0,130,150), Color.rgb(210,38,52), Color.rgb(92,46,145), Color.rgb(241,124,0)};

        LinearLayout cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.HORIZONTAL);
        for (int i=0;i<5;i++) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(4), dp(10), dp(4), dp(10));
            TextView n = text(names[i], 12, colors[i], true);
            TextView v = text(String.format(Locale.CANADA_FRENCH,"%.1f%%", avg[i]/count), 21, NAVY, true);
            card.addView(n); card.addView(v);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(2),0,dp(2),0);
            card.setBackgroundColor(Color.WHITE);
            cards.addView(card, lp);
        }
        content.addView(cards, matchWrap());

        TextView h2 = text("Évolution des intentions de vote", 20, NAVY, true);
        h2.setPadding(0, dp(18), 0, dp(6));
        content.addView(h2);

        trendView = new TrendView(this, polls);
        content.addView(trendView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(250)));

        TextView firmsTitle = text("Maisons de sondage présentes en 2026", 20, NAVY, true);
        firmsTitle.setPadding(0, dp(18), 0, dp(6));
        content.addView(firmsTitle);
        Map<String,Integer> firms = new LinkedHashMap<>();
        for (Poll p: polls) firms.put(p.firm, firms.getOrDefault(p.firm,0)+1);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String,Integer> e: firms.entrySet()) {
            if (sb.length()>0) sb.append("  •  ");
            sb.append(e.getKey()).append(" (").append(e.getValue()).append(")");
        }
        content.addView(text(sb.toString(), 13, Color.DKGRAY, false));

        TextView h3 = text("Tous les sondages 2026", 20, NAVY, true);
        h3.setPadding(0, dp(18), 0, dp(6));
        content.addView(h3);
        content.addView(makeTable());
    }

    private View makeTable() {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        TableLayout table = new TableLayout(this);
        table.setStretchAllColumns(false);
        table.setBackgroundColor(Color.WHITE);

        TableRow head = new TableRow(this);
        String[] headers = {"Date","Firme","n","PQ","CAQ","PLQ","PCQ","QS"};
        for (String h: headers) head.addView(cell(h,true));
        table.addView(head);

        for (int i=polls.size()-1;i>=0;i--) {
            Poll p = polls.get(i);
            TableRow row = new TableRow(this);
            String[] vals = {p.date,p.firm,String.valueOf(p.n),p.pq+"%",p.caq+"%",p.plq+"%",p.pcq+"%",p.qs+"%"};
            for (String v: vals) row.addView(cell(v,false));
            table.addView(row);
        }
        hsv.addView(table);
        return hsv;
    }

    private TextView cell(String s, boolean header) {
        TextView t = text(s, header?13:12, header?Color.WHITE:Color.rgb(35,43,60), header);
        t.setPadding(dp(9), dp(10), dp(9), dp(10));
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setMinWidth(dp(header && s.equals("Firme") ? 145 : 70));
        t.setBackgroundColor(header?NAVY:Color.WHITE);
        return t;
    }

    private void openLive() {
        Dialog d = new Dialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10),dp(8),dp(10),dp(8));
        TextView ttl = text("Flux direct — Vote-Scope", 17, Color.WHITE, true);
        Button close = button("FERMER");
        bar.setBackgroundColor(BLUE);
        bar.addView(ttl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        bar.addView(close, new LinearLayout.LayoutParams(dp(100), ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(bar);

        WebView web = new WebView(this);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        web.setWebViewClient(new WebViewClient());
        web.loadUrl("https://vote-scope.com/fr/canada/quebec/sondages/");
        root.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        d.setContentView(root);
        if (d.getWindow()!=null) d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        close.setOnClickListener(v -> d.dismiss());
        d.setOnShowListener(x -> { if (d.getWindow()!=null) d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT); });
        d.show();
    }

    private void loadPolls() {
        polls.clear();
        add("10 janv.","Pallas Data",1128,34,11,24,16,11);
        add("24 janv.","Innovative Research",600,31,14,26,18,6);
        add("28 janv.","Léger",1000,32,17,26,14,7);
        add("22 févr.","Pallas Data",1075,30,14,27,16,10);
        add("1 mars","Léger",1041,31,13,30,15,9);
        add("21 mars","Léger",1003,33,9,33,15,9);
        add("4 avr.","Léger",1036,32,13,33,12,8);
        add("14 avr.","Pallas Data",956,29,14,32,14,11);
        add("19 avr.","Léger",1030,31,17,28,14,8);
        add("27 avr.","Liaison Strategies",1000,32,16,32,11,7);
        add("8 mai","Pallas Data",1176,29,19,28,14,8);
        add("9 mai","Synopsis Recherche",1000,30,18,30,13,8);
        add("15 mai","Mainstreet Research",1468,23,24,32,13,6);
        add("17 mai","Léger",1027,30,22,28,11,8);
        add("7 juin","Synopsis Recherche",1000,31,21,25,12,11);
        add("11 juin","Pallas Data",1099,29,20,25,14,11);
        add("14 juin","Léger",1014,30,21,27,13,8);
        add("2 août","Pallas Data",1121,31,19,27,14,9);
        add("8 août","Synopsis Recherche",1040,30,20,26,15,9);
        add("8 août","Léger",998,30,20,24,13,11);
        add("23 août","Léger",1010,30,23,23,16,7);
        add("25 août","Synopsis Recherche",1009,28,27,20,14,10);
        add("29 août","Pallas Data",1108,29,24,22,16,9);
        add("30 août","Léger",1008,29,24,22,15,10);
        add("5 sept.","Pallas Data",1100,29,24,20,15,11);
        add("6 sept.","Léger",1008,29,23,22,15,10);
    }

    private void add(String d,String f,int n,int pq,int caq,int plq,int pcq,int qs){ polls.add(new Poll(d,f,n,pq,caq,plq,pcq,qs)); }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(12); b.setAllCaps(false); b.setBackgroundColor(BLUE);
        return b;
    }
    private LinearLayout.LayoutParams weighted(){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1f); p.setMargins(dp(2),0,dp(2),0); return p; }
    private LinearLayout.LayoutParams matchWrap(){ return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private int dp(int v){ return Math.round(v*getResources().getDisplayMetrics().density); }

    static class Poll {
        final String date, firm; final int n,pq,caq,plq,pcq,qs;
        Poll(String date,String firm,int n,int pq,int caq,int plq,int pcq,int qs){this.date=date;this.firm=firm;this.n=n;this.pq=pq;this.caq=caq;this.plq=plq;this.pcq=pcq;this.qs=qs;}
    }

    static class TrendView extends View {
        private final List<Poll> data;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] colors = {Color.rgb(18,88,190), Color.rgb(0,130,150), Color.rgb(210,38,52), Color.rgb(92,46,145), Color.rgb(241,124,0)};
        private final String[] names = {"PQ","CAQ","PLQ","PCQ","QS"};
        TrendView(Activity c,List<Poll> data){ super(c); this.data=data; setBackgroundColor(Color.WHITE); }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(), h=getHeight(); float l=46, r=12, t=22, b=38;
            paint.setStrokeWidth(1); paint.setColor(Color.rgb(220,226,238));
            for(int y=10;y<=40;y+=10){ float py=t+(40-y)*(h-t-b)/40f; c.drawLine(l,py,w-r,py,paint); paint.setColor(Color.GRAY); paint.setTextSize(22); c.drawText(y+"%",5,py+7,paint); paint.setColor(Color.rgb(220,226,238)); }
            for(int s=0;s<5;s++){
                paint.setColor(colors[s]); paint.setStrokeWidth(4); paint.setStyle(Paint.Style.STROKE); Path path=new Path();
                for(int i=0;i<data.size();i++){ Poll p=data.get(i); int val=s==0?p.pq:s==1?p.caq:s==2?p.plq:s==3?p.pcq:p.qs; float x=l+i*(w-l-r)/(data.size()-1f); float y=t+(40-val)*(h-t-b)/40f; if(i==0)path.moveTo(x,y); else path.lineTo(x,y); }
                c.drawPath(path,paint); paint.setStyle(Paint.Style.FILL); paint.setTextSize(22); c.drawText(names[s], l+s*58, h-10, paint);
            }
        }
    }
}
