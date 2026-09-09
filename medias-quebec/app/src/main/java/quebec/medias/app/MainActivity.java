package quebec.medias.app;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.Html;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private WebView webView;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final String[] KEYWORDS = new String[]{
            "francophone", "québec", "quebec", "québécois", "quebecois",
            "indépendantiste", "independantiste", "indépendance", "independance",
            "souverainiste", "séparatiste", "separatiste", "séparatistes", "separatistes",
            "pays", "nationaliste", "parti québécois", "parti quebecois",
            "paul st-pierre plamondon", "pspp"
    };

    static class FeedSpec {
        final String name;
        final String url;
        final String section;

        FeedSpec(String name, String url, String section) {
            this.name = name;
            this.url = url;
            this.section = section;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " MediasQuebec/1.0");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new Bridge(), "Android");
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        String url = webView.getUrl();
        if (url != null && !url.startsWith("file:///android_asset/")) {
            webView.loadUrl("file:///android_asset/index.html");
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private class Bridge {
        @JavascriptInterface
        public void loadSection(String section) {
            String safe = section == null ? "QUEBEC" : section.toUpperCase(Locale.CANADA_FRENCH);
            executor.execute(() -> loadSectionWorker(safe));
        }

        @JavascriptInterface
        public void openArticle(String url) {
            if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) return;
            runOnUiThread(() -> webView.loadUrl(url));
        }

        @JavascriptInterface
        public String getKeywords() {
            JSONArray arr = new JSONArray();
            for (String keyword : KEYWORDS) arr.put(keyword);
            return arr.toString();
        }
    }

    private void loadSectionWorker(String section) {
        String cached = readCache(section);
        if (cached != null && !cached.isEmpty()) {
            sendToJs(section, cached, isOnline() ? "cache" : "offline");
        }

        if (!isOnline()) {
            if (cached == null || cached.isEmpty()) sendToJs(section, "[]", "offline-empty");
            return;
        }

        try {
            JSONArray items;
            if ("LIVE".equals(section)) {
                items = liveItems();
            } else {
                items = fetchSection(section);
            }
            writeCache(section, items.toString());
            sendToJs(section, items.toString(), "online");
        } catch (Exception e) {
            if (cached == null || cached.isEmpty()) sendToJs(section, "[]", "error");
        }
    }

    private JSONArray fetchSection(String section) {
        List<FeedSpec> feeds = feedsFor(section);
        JSONArray result = new JSONArray();
        Set<String> seen = new HashSet<>();

        for (FeedSpec feed : feeds) {
            try {
                List<JSONObject> parsed = fetchFeed(feed);
                for (JSONObject item : parsed) {
                    String title = item.optString("title");
                    String link = item.optString("link");
                    String key = (link.isEmpty() ? title : link).trim().toLowerCase(Locale.ROOT);
                    if (key.isEmpty() || seen.contains(key)) continue;

                    if ("VEILLE".equals(section)) {
                        String haystack = (title + " " + item.optString("description")).toLowerCase(Locale.CANADA_FRENCH);
                        JSONArray matches = keywordMatches(haystack);
                        if (matches.length() == 0) continue;
                        item.put("matches", matches);
                    }

                    seen.add(key);
                    result.put(item);
                    if (result.length() >= 160) return result;
                }
            } catch (Exception ignored) {
                // Un flux peut être temporairement indisponible; les autres continuent.
            }
        }
        return result;
    }

    private JSONArray keywordMatches(String text) {
        JSONArray arr = new JSONArray();
        Set<String> added = new HashSet<>();
        for (String keyword : KEYWORDS) {
            if (text.contains(keyword.toLowerCase(Locale.CANADA_FRENCH))) {
                String display = keyword;
                if (added.add(display)) arr.put(display);
            }
        }
        return arr;
    }

    private List<FeedSpec> feedsFor(String section) {
        List<FeedSpec> f = new ArrayList<>();

        if ("QUEBEC".equals(section)) {
            f.add(new FeedSpec("Journal de Montréal — Dernières nouvelles", "https://www.journaldemontreal.com/accueil/rss.xml", "Québec"));
            f.add(new FeedSpec("Journal de Montréal — Politique", "https://www.journaldemontreal.com/actualite/politique/rss.xml", "Québec"));
            f.add(new FeedSpec("Journal de Québec — Dernières nouvelles", "https://www.journaldequebec.com/accueil/rss.xml", "Québec"));
            f.add(new FeedSpec("Journal de Québec — Politique", "https://www.journaldequebec.com/actualite/politique/rss.xml", "Québec"));
            f.add(new FeedSpec("Le Devoir — Manchettes", "https://www.ledevoir.com/rss/manchettes.xml", "Québec"));
            f.add(new FeedSpec("Radio-Canada — Actualités", "https://ici.radio-canada.ca/rss/4159", "Québec"));
            f.add(new FeedSpec("Google Actualités — Québec", gnews("Québec OR québécois OR politique Québec", "fr-CA", "CA", "CA:fr"), "Québec"));
            f.add(new FeedSpec("Google Actualités — médias québécois", gnews("Québec (Radio-Canada OR TVA OR Le Devoir OR La Presse OR Journal de Montréal OR Journal de Québec OR Noovo)", "fr-CA", "CA", "CA:fr"), "Québec"));
        } else if ("CANADA".equals(section)) {
            f.add(new FeedSpec("CBC — Top Stories", "https://www.cbc.ca/cmlink/rss-topstories", "Canada"));
            f.add(new FeedSpec("CBC — Canada", "https://www.cbc.ca/cmlink/rss-canada", "Canada"));
            f.add(new FeedSpec("Radio-Canada — Canada", gnews("Canada politique fédérale francophone", "fr-CA", "CA", "CA:fr"), "Canada"));
            f.add(new FeedSpec("Google Actualités — Canada", gnews("Canada politics OR Ottawa OR federal", "en-CA", "CA", "CA:en"), "Canada"));
        } else if ("USA".equals(section)) {
            f.add(new FeedSpec("New York Times — U.S.", "https://rss.nytimes.com/services/xml/rss/nyt/US.xml", "USA"));
            f.add(new FeedSpec("New York Times — Politics", "https://rss.nytimes.com/services/xml/rss/nyt/Politics.xml", "USA"));
            f.add(new FeedSpec("NPR — News", "https://feeds.npr.org/1001/rss.xml", "USA"));
            f.add(new FeedSpec("Google News — United States", gnews("United States politics", "en-US", "US", "US:en"), "USA"));
        } else if ("FRANCE".equals(section)) {
            f.add(new FeedSpec("Franceinfo — Titres", "https://www.francetvinfo.fr/titres.rss", "France"));
            f.add(new FeedSpec("RFI — France", "https://www.rfi.fr/fr/rss", "France"));
            f.add(new FeedSpec("Le Monde — Actualité", "https://www.lemonde.fr/actualite/rss_full.xml", "France"));
            f.add(new FeedSpec("Le Monde — France", "https://www.lemonde.fr/france/rss_full.xml", "France"));
            f.add(new FeedSpec("Le Monde — Politique", "https://www.lemonde.fr/politique/rss_full.xml", "France"));
            f.add(new FeedSpec("Google Actualités — France", gnews("France politique actualité", "fr", "FR", "FR:fr"), "France"));
        } else if ("POLITIQUE".equals(section)) {
            f.add(new FeedSpec("JDM — Politique", "https://www.journaldemontreal.com/actualite/politique/rss.xml", "Politique"));
            f.add(new FeedSpec("JDQ — Politique", "https://www.journaldequebec.com/actualite/politique/rss.xml", "Politique"));
            f.add(new FeedSpec("Le Devoir — Politique québécoise", gnews("politique Québec Parti québécois CAQ PLQ QS", "fr-CA", "CA", "CA:fr"), "Politique"));
            f.add(new FeedSpec("Politique Québec — Tous médias", gnews("politique Québec", "fr-CA", "CA", "CA:fr"), "Politique"));
        } else if ("VEILLE".equals(section)) {
            String q = "francophone OR Québec OR québécois OR indépendantiste OR indépendance OR souverainiste OR séparatiste OR nationaliste OR \"Parti québécois\" OR \"Paul St-Pierre Plamondon\" OR PSPP";
            f.add(new FeedSpec("Veille Québec", gnews(q, "fr-CA", "CA", "CA:fr"), "Veille"));
            f.add(new FeedSpec("JDM — Politique", "https://www.journaldemontreal.com/actualite/politique/rss.xml", "Veille"));
            f.add(new FeedSpec("JDQ — Politique", "https://www.journaldequebec.com/actualite/politique/rss.xml", "Veille"));
            f.add(new FeedSpec("Le Devoir — Manchettes", "https://www.ledevoir.com/rss/manchettes.xml", "Veille"));
        } else {
            return feedsFor("QUEBEC");
        }

        return f;
    }

    private String gnews(String query, String hl, String gl, String ceid) {
        try {
            return "https://news.google.com/rss/search?q=" + URLEncoder.encode(query, "UTF-8")
                    + "&hl=" + URLEncoder.encode(hl, "UTF-8")
                    + "&gl=" + URLEncoder.encode(gl, "UTF-8")
                    + "&ceid=" + URLEncoder.encode(ceid, "UTF-8");
        } catch (Exception e) {
            return "https://news.google.com/rss";
        }
    }

    private List<JSONObject> fetchFeed(FeedSpec feed) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(feed.url).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) MediasQuebec/1.0 RSSReader");
        conn.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*");

        int code = conn.getResponseCode();
        if (code < 200 || code >= 400) throw new Exception("HTTP " + code);

        try (InputStream in = conn.getInputStream()) {
            return parseXml(in, feed);
        } finally {
            conn.disconnect();
        }
    }

    private List<JSONObject> parseXml(InputStream in, FeedSpec feed) throws Exception {
        List<JSONObject> out = new ArrayList<>();
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(false);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(in, null);

        boolean inside = false;
        String title = "", link = "", description = "", date = "", source = feed.name;

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if (tag == null) { event = parser.next(); continue; }
                tag = tag.toLowerCase(Locale.ROOT);

                if (tag.equals("item") || tag.equals("entry")) {
                    inside = true;
                    title = link = description = date = "";
                    source = feed.name;
                } else if (inside) {
                    if (tag.equals("title")) {
                        title = safeNextText(parser);
                    } else if (tag.equals("link")) {
                        String href = parser.getAttributeValue(null, "href");
                        if (href != null && !href.isEmpty()) link = href;
                        else link = safeNextText(parser);
                    } else if (tag.equals("description") || tag.equals("summary") || tag.equals("content") || tag.equals("content:encoded")) {
                        if (description.isEmpty()) description = safeNextText(parser);
                    } else if (tag.equals("pubdate") || tag.equals("published") || tag.equals("updated") || tag.equals("dc:date")) {
                        if (date.isEmpty()) date = safeNextText(parser);
                    } else if (tag.equals("source")) {
                        String s = safeNextText(parser);
                        if (!s.isEmpty()) source = s;
                    }
                }
            } else if (event == XmlPullParser.END_TAG) {
                String tag = parser.getName();
                if (inside && tag != null && (tag.equalsIgnoreCase("item") || tag.equalsIgnoreCase("entry"))) {
                    inside = false;
                    if (!title.trim().isEmpty()) {
                        JSONObject obj = new JSONObject();
                        obj.put("title", clean(title));
                        obj.put("link", link == null ? "" : link.trim());
                        obj.put("description", clean(description));
                        obj.put("date", clean(date));
                        obj.put("source", clean(source));
                        obj.put("section", feed.section);
                        out.add(obj);
                        if (out.size() >= 45) break;
                    }
                }
            }
            event = parser.next();
        }
        return out;
    }

    private String safeNextText(XmlPullParser parser) {
        try {
            return parser.nextText();
        } catch (Exception e) {
            return "";
        }
    }

    private String clean(String value) {
        if (value == null) return "";
        String decoded;
        try {
            decoded = Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString();
        } catch (Exception e) {
            decoded = value.replaceAll("<[^>]*>", " ");
        }
        return decoded.replaceAll("\\s+", " ").trim();
    }

    private JSONArray liveItems() {
        JSONArray arr = new JSONArray();
        addLive(arr, "ICI RDI / Radio-Canada — En direct", "https://ici.radio-canada.ca/info", "Québec / Canada");
        addLive(arr, "TVA Nouvelles — En direct", "https://www.tvanouvelles.ca/", "Québec");
        addLive(arr, "Noovo Info — Actualités", "https://www.noovo.info/", "Québec");
        addLive(arr, "Le Devoir — Actualités", "https://www.ledevoir.com/", "Québec");
        addLive(arr, "CBC News — Live / Canada", "https://www.cbc.ca/news", "Canada");
        addLive(arr, "Franceinfo — Direct", "https://www.francetvinfo.fr/en-direct/", "France");
        addLive(arr, "Le Monde — En direct", "https://www.lemonde.fr/en-direct/", "France");
        addLive(arr, "NPR — News", "https://www.npr.org/sections/news/", "USA");
        return arr;
    }

    private void addLive(JSONArray arr, String title, String link, String section) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("title", title);
            obj.put("link", link);
            obj.put("description", "Ouvrir la couverture en direct ou la page d'actualité dans Médias Québec. Une connexion Internet est requise pour le direct.");
            obj.put("date", "");
            obj.put("source", "En direct");
            obj.put("section", section);
            obj.put("live", true);
            arr.put(obj);
        } catch (Exception ignored) { }
    }

    private boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
        } catch (Exception e) {
            return false;
        }
    }

    private File cacheFile(String section) {
        return new File(getFilesDir(), "rss_" + section.toLowerCase(Locale.ROOT) + ".json");
    }

    private void writeCache(String section, String data) {
        try (FileOutputStream out = new FileOutputStream(cacheFile(section))) {
            out.write(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) { }
    }

    private String readCache(String section) {
        File file = cacheFile(section);
        if (!file.exists()) return null;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void sendToJs(String section, String json, String mode) {
        String script = "window.onNativeData(" + JSONObject.quote(section) + "," + json + "," + JSONObject.quote(mode) + ");";
        runOnUiThread(() -> webView.evaluateJavascript(script, null));
    }
}
