package ca.quebec.oui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class LiteratureActivity extends Activity {
    private static final int BLUE=Color.rgb(0,61,165), DARK=Color.rgb(0,31,91), NAVY=Color.rgb(8,22,55), INK=Color.rgb(20,33,61), MUTED=Color.rgb(82,98,125), SOFT=Color.rgb(244,247,252), BORDER=Color.rgb(220,228,241), WHITE=Color.WHITE, PURPLE=Color.rgb(111,70,160);
    private LinearLayout results;

    private static class Book {
        final String title, author, year, isbn, note, buy;
        Book(String title,String author,String year,String isbn,String note,String buy){this.title=title;this.author=author;this.year=year;this.isbn=isbn;this.note=note;this.buy=buy;}
    }

    private static final Book[] BOOKS = {
        new Book("Option Québec","René Lévesque","1968 / éd. 2025","9782892954814","Texte fondateur de la souveraineté-association et de l’option politique portée par René Lévesque.","https://editionstypo.groupelivre.com/products/option-quebec-format-poche"),
        new Book("Pourquoi je suis séparatiste","Marcel Chaput","1961 / rééd. 2007","9782894062715","Un des textes fondateurs du mouvement indépendantiste moderne.","https://distributionhmh.com/livre/pourquoi-je-suis-separatiste/"),
        new Book("Le colonialisme au Québec","André d’Allemagne","1966 / rééd. 2000","9782922494433","Essai majeur du RIN sur la dépendance politique, économique et culturelle du Québec.","https://luxediteur.com/catalogue/le-colonialisme-au-quebec/"),
        new Book("Égalité ou indépendance","Daniel Johnson","1965 / nouv. éd. 2024","9782981913760","Essai historique sur les rapports Québec–Canada et l’alternative entre égalité politique et indépendance.","https://www.leslibraires.ca/recherche/?q=9782981913760"),
        new Book("La souveraineté en héritage","Jacques Beauchemin","2015","9782764624012","Réflexion sociologique et politique sur la transmission du projet souverainiste.","https://www.editionsboreal.qc.ca/catalogue/livres/souverainete-heritage-2449.html"),
        new Book("Droit à l’indépendance","Frédéric Bérard et Stéphane Beaulac","2015","9782892619423","Analyse juridique de l’autodétermination, de la sécession et des référendums.","https://editionsxyz.com/livre/droit-a-l-independance"),
        new Book("Une histoire du RIN","Claude Cardinal","2015","9782896496105","Histoire détaillée du Rassemblement pour l’indépendance nationale, de 1960 à 1968.","https://www.leslibraires.ca/livres/une-histoire-du-rin-claude-cardinal-9782896496105.html"),
        new Book("Le souverainisme de province","Simon-Pierre Savard-Tremblay","2014","9782764623596","Analyse critique de l’évolution du mouvement souverainiste québécois.","https://www.librairiemartin.com/Livres-francophones/Le-souverainisme-de-province/9782764623596/3091/Savard-Tremblay-Simon-Pierre-Histoire-politique-et-societe"),
        new Book("Le projet Ambition Québec : s’organiser pour l’indépendance","Catherine Fournier","2019","9782897941215","Propositions pour renouveler l’organisation du mouvement indépendantiste sur une base plus citoyenne.","https://editionssommetoute.com/livre/projet-ambition-quebec-le/"),
        new Book("Un peuple libre : indépendance, laïcité et inclusion","Benoit Renaud","2020","9782897196219","Essai liant indépendance, inclusion, démocratie et question sociale.","https://ecosociete.org/livres/un-peuple-libre"),
        new Book("Le Livre qui fait dire OUI","Collectif","2015–2026","9782923365596","Ouvrage collectif de vulgarisation sur différents enjeux liés à l’indépendance; plusieurs éditions existent.","https://www.lequebecois.org/boutique/le-livre-qui-fait-dire-oui-3-edition-2026/"),
        new Book("Cessons d’être des colonisés!","J.-Maurice Arbour","2015","9782763727639","Essai sur les rapports de pouvoir entre Québec et Canada, catalogué par BAnQ sous le sujet souveraineté.","https://www.leslibraires.ca/recherche/?q=9782763727639"),
        new Book("Tous unis : ensemble, choisir un Québec souverain","Léo-Paul Provencher","2022","9782982114401","Ouvrage consacré explicitement au choix d’un Québec souverain.","https://www.leslibraires.ca/recherche/?q=9782982114401")
    };

    @Override public void onCreate(Bundle b){super.onCreate(b);Locale.setDefault(Locale.CANADA_FRENCH);build();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable bordered(int color,int stroke){GradientDrawable g=bg(color,16);g.setStroke(dp(1),stroke);return g;}
    private TextView tx(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);t.setLineSpacing(0,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(WHITE);b.setTextSize(13);b.setAllCaps(false);b.setBackground(bg(BLUE,12));b.setPadding(dp(12),0,dp(12),0);return b;}
    private void open(String u){startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));}

    private void build(){
        getWindow().setStatusBarColor(DARK);getWindow().setNavigationBarColor(DARK);
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setBackgroundColor(WHITE);
        outer.setOnApplyWindowInsetsListener((v,insets)->{int top=0,bottom=0;if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());top=i.top;bottom=i.bottom;}else{top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();}v.setPadding(0,top,0,bottom);return insets;});
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setGravity(Gravity.CENTER_HORIZONTAL);head.setPadding(dp(16),dp(12),dp(16),dp(14));head.setBackgroundColor(DARK);
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.oui_logo);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);logo.setContentDescription("Logo OUI Québec");head.addView(logo,new LinearLayout.LayoutParams(dp(72),dp(72)));
        TextView title=tx("Bibliothèque de l’indépendance",24,WHITE,true);title.setGravity(Gravity.CENTER);head.addView(title);
        TextView sub=tx("Ouvrages, couvertures et accès d’achat",13,Color.rgb(210,225,250),false);sub.setGravity(Gravity.CENTER);head.addView(sub);outer.addView(head);

        ScrollView scroll=new ScrollView(this);scroll.setClipToPadding(false);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(15),dp(15),dp(15),dp(30));
        TextView note=tx("Cette section rassemble des ouvrages de référence sur l’indépendance, la souveraineté, le RIN, le droit à l’autodétermination et les débats Québec–Canada. Aucun catalogue unique ne garantit de recenser absolument tous les livres publiés; BAnQ demeure la référence bibliographique nationale pour poursuivre l’indexation.",13,MUTED,false);note.setPadding(dp(12),dp(10),dp(12),dp(10));note.setBackground(bordered(SOFT,BORDER));root.addView(note);
        EditText search=new EditText(this);search.setHint("Rechercher un titre, auteur, année ou ISBN");search.setSingleLine(true);search.setTextColor(INK);search.setHintTextColor(MUTED);search.setBackground(bordered(WHITE,BORDER));search.setPadding(dp(12),0,dp(12),0);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(52));sp.setMargins(0,dp(12),0,dp(10));root.addView(search,sp);
        results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);root.addView(results);
        Button banq=button("Consulter la Bibliographie du Québec — BAnQ");LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(50));bp.setMargins(0,dp(4),0,dp(8));banq.setLayoutParams(bp);banq.setOnClickListener(v->open("https://www.banq.qc.ca/plateformes-numeriques/bibliographie-du-quebec/"));root.addView(banq);
        TextView legal=tx("Les images de couverture sont chargées depuis un service bibliographique externe à partir de l’ISBN. Les boutons d’achat ouvrent la page du libraire ou de l’éditeur; prix et disponibilité peuvent changer.",11,MUTED,false);legal.setPadding(0,dp(8),0,0);root.addView(legal);
        Runnable render=()->render(search.getText().toString());render.run();
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){render.run();}public void afterTextChanged(Editable e){}});
        scroll.addView(root);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(outer);outer.requestApplyInsets();
    }

    private void render(String q){
        results.removeAllViews();String n=q.trim().toLowerCase(Locale.CANADA_FRENCH);int count=0;
        for(Book b:BOOKS){String hay=(b.title+" "+b.author+" "+b.year+" "+b.isbn+" "+b.note).toLowerCase(Locale.CANADA_FRENCH);if(n.length()==0||hay.contains(n)){addBook(b);count++;}}
        if(count==0){TextView t=tx("Aucun résultat dans le catalogue local. Essaie un autre mot-clé ou consulte BAnQ.",14,MUTED,true);t.setPadding(0,dp(12),0,dp(12));results.addView(t);}
    }

    private void addBook(Book b){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);card.setPadding(dp(12),dp(12),dp(12),dp(12));card.setBackground(bordered(SOFT,BORDER));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(11));card.setLayoutParams(cp);
        ImageView cover=new ImageView(this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setContentDescription("Couverture de "+b.title);cover.setBackgroundColor(Color.rgb(230,235,243));card.addView(cover,new LinearLayout.LayoutParams(dp(92),dp(138)));loadCover(b.isbn,cover);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,-2,1);ip.setMargins(dp(12),0,0,0);card.addView(info,ip);
        info.addView(tx(b.title,17,NAVY,true));TextView a=tx(b.author+" • "+b.year,12,PURPLE,true);a.setPadding(0,dp(3),0,dp(4));info.addView(a);info.addView(tx(b.note,12,INK,false));TextView isbn=tx("ISBN : "+b.isbn,11,MUTED,false);isbn.setPadding(0,dp(5),0,dp(7));info.addView(isbn);
        Button buy=button("Acheter / disponibilité");info.addView(buy,new LinearLayout.LayoutParams(-1,dp(44)));buy.setOnClickListener(v->open(b.buy));
        results.addView(card);
    }

    private void loadCover(String isbn,ImageView target){
        new Thread(()->{HttpURLConnection c=null;try{URL u=new URL("https://covers.openlibrary.org/isbn/"+isbn+"-M.jpg?default=false");c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(5000);c.setReadTimeout(7000);c.setInstanceFollowRedirects(true);if(c.getResponseCode()>=200&&c.getResponseCode()<300){Bitmap bmp=BitmapFactory.decodeStream(c.getInputStream());if(bmp!=null)runOnUiThread(()->target.setImageBitmap(bmp));}}catch(Exception ignored){}finally{if(c!=null)c.disconnect();}}).start();
    }
}
