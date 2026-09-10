package quebec.cast.hisense;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class DirectDlnaCaster {
    public interface Listener { void onStatus(String message); }

    private final Context context;
    private final String tvIp;
    private final Listener listener;
    private volatile MediaServer server;
    private volatile String controlUrl;

    public DirectDlnaCaster(Context context, String tvIp, Listener listener) {
        this.context = context.getApplicationContext();
        this.tvIp = tvIp;
        this.listener = listener;
    }

    private void status(String s) { if (listener != null) listener.onStatus(s); }

    public void cast(Uri uri) throws Exception {
        ContentResolver cr = context.getContentResolver();
        String mime = cr.getType(uri);
        if (mime == null) mime = "application/octet-stream";
        String name = queryName(cr, uri);
        if (name == null || name.isEmpty()) name = "media";

        if (server != null) server.stop();
        server = new MediaServer(context, uri, mime, name);
        server.start();

        String localIp = getLocalIpForTv(tvIp);
        String mediaUrl = "http://" + localIp + ":" + server.getPort() + "/media";
        String ctl = findAvTransportControlUrl();
        controlUrl = ctl;

        status("Envoi direct vers la Hisense…");
        String metadata = buildDidl(mediaUrl, mime, name);
        soap(ctl, "SetAVTransportURI",
                "<InstanceID>0</InstanceID><CurrentURI>" + xml(mediaUrl) + "</CurrentURI><CurrentURIMetaData>" + xml(metadata) + "</CurrentURIMetaData>");
        Thread.sleep(250);
        soap(ctl, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>");
        status("Lecture directe sur la Hisense — sans Google Cast.");
    }

    public void pause() throws Exception { soap(ensureControl(), "Pause", "<InstanceID>0</InstanceID>"); status("Pause."); }
    public void play() throws Exception { soap(ensureControl(), "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>"); status("Lecture."); }
    public void stopPlayback() throws Exception {
        String ctl = ensureControl();
        try { soap(ctl, "Stop", "<InstanceID>0</InstanceID>"); }
        finally { if (server != null) { server.stop(); server = null; } }
        status("Cast direct arrêté.");
    }

    private String ensureControl() throws Exception {
        if (controlUrl == null) controlUrl = findAvTransportControlUrl();
        return controlUrl;
    }

    private String findAvTransportControlUrl() throws Exception {
        // 1) Méthode correcte UPnP : demander au téléviseur son URL LOCATION via SSDP.
        List<String> locations = discoverLocationsBySsdp();
        Exception last = null;
        for (String location : locations) {
            try {
                String desc = httpGet(location);
                String control = parseControlUrl(desc, "AVTransport");
                if (control != null) {
                    String abs = absolutize(location, control);
                    soap(abs, "GetTransportInfo", "<InstanceID>0</InstanceID>");
                    status("Récepteur DLNA trouvé automatiquement.");
                    return abs;
                }
            } catch (Exception e) { last = e; }
        }

        // 2) Certains VIDAA n'annoncent le renderer que via plusieurs ST.
        String[] sts = {
                "urn:schemas-upnp-org:device:MediaRenderer:1",
                "urn:schemas-upnp-org:service:AVTransport:1",
                "ssdp:all"
        };
        for (String st : sts) {
            try {
                String loc = discoverSingleLocation(st);
                if (loc != null) {
                    String desc = httpGet(loc);
                    String control = parseControlUrl(desc, "AVTransport");
                    if (control != null) {
                        String abs = absolutize(loc, control);
                        soap(abs, "GetTransportInfo", "<InstanceID>0</InstanceID>");
                        return abs;
                    }
                }
            } catch (Exception e) { last = e; }
        }

        throw new IOException("Aucun récepteur DLNA/AVTransport n’est annoncé par la Hisense sur le réseau local" +
                (last != null && last.getMessage() != null ? " : " + last.getMessage() : ""));
    }

    private List<String> discoverLocationsBySsdp() throws Exception {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String[] sts = {
                "urn:schemas-upnp-org:device:MediaRenderer:1",
                "urn:schemas-upnp-org:service:AVTransport:1",
                "ssdp:all"
        };
        for (String st : sts) {
            out.addAll(discoverLocations(st, 1800));
        }
        return new ArrayList<>(out);
    }

    private String discoverSingleLocation(String st) throws Exception {
        List<String> a = discoverLocations(st, 2200);
        return a.isEmpty() ? null : a.get(0);
    }

    private List<String> discoverLocations(String st, int timeoutMs) throws Exception {
        List<String> out = new ArrayList<>();
        DatagramSocket sock = new DatagramSocket();
        try {
            sock.setReuseAddress(true);
            sock.setSoTimeout(350);
            String req = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 2\r\n" +
                    "ST: " + st + "\r\n\r\n";
            byte[] data = req.getBytes(StandardCharsets.ISO_8859_1);
            InetAddress group = InetAddress.getByName("239.255.255.250");
            sock.send(new DatagramPacket(data, data.length, group, 1900));
            long end = System.currentTimeMillis() + timeoutMs;
            byte[] buf = new byte[8192];
            while (System.currentTimeMillis() < end) {
                try {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    sock.receive(p);
                    if (!tvIp.equals(p.getAddress().getHostAddress())) continue;
                    String resp = new String(p.getData(), 0, p.getLength(), StandardCharsets.ISO_8859_1);
                    Matcher m = Pattern.compile("(?im)^LOCATION\\s*:\\s*(.+?)\\s*$").matcher(resp);
                    if (m.find()) {
                        String loc = m.group(1).trim();
                        if (!out.contains(loc)) out.add(loc);
                    }
                } catch (SocketTimeoutException ignored) {}
            }
        } finally { sock.close(); }
        return out;
    }

    private static String parseControlUrl(String xml, String serviceName) {
        Pattern p = Pattern.compile("<service>\\s*.*?<serviceType>[^<]*" + serviceName + "[^<]*</serviceType>.*?<controlURL>([^<]+)</controlURL>.*?</service>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(xml);
        return m.find() ? m.group(1).trim() : null;
    }

    private static String absolutize(String descriptorUrl, String control) throws Exception {
        URL base = new URL(descriptorUrl);
        if (control.startsWith("http://") || control.startsWith("https://")) return control;
        URI resolved = base.toURI().resolve(control);
        return resolved.toString();
    }

    private static String httpGet(String s) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(s).openConnection();
        c.setConnectTimeout(3000); c.setReadTimeout(4000); c.setRequestMethod("GET");
        int code = c.getResponseCode();
        if (code < 200 || code >= 300) throw new IOException("HTTP " + code);
        try (InputStream in = c.getInputStream()) { return readAll(in); }
        finally { c.disconnect(); }
    }

    private static String soap(String controlUrl, String action, String inner) throws Exception {
        String service = "urn:schemas-upnp-org:service:AVTransport:1";
        String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body><u:" + action + " xmlns:u=\"" + service + "\">" + inner + "</u:" + action + "></s:Body></s:Envelope>";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection c = (HttpURLConnection)new URL(controlUrl).openConnection();
        c.setConnectTimeout(3000); c.setReadTimeout(5000); c.setRequestMethod("POST"); c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
        c.setRequestProperty("SOAPACTION", "\"" + service + "#" + action + "\"");
        c.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream out = c.getOutputStream()) { out.write(bytes); }
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String result = in == null ? "" : readAll(in);
        c.disconnect();
        if (code < 200 || code >= 300) throw new IOException("DLNA " + action + " refusé (HTTP " + code + ") " + result);
        return result;
    }

    private static String buildDidl(String url, String mime, String title) {
        String cls = mime.startsWith("image/") ? "object.item.imageItem.photo" : mime.startsWith("audio/") ? "object.item.audioItem.musicTrack" : "object.item.videoItem";
        return "<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\">" +
                "<item id=\"0\" parentID=\"0\" restricted=\"1\"><dc:title>" + xml(title) + "</dc:title><upnp:class>" + cls + "</upnp:class>" +
                "<res protocolInfo=\"http-get:*:" + xml(mime) + ":*\">" + xml(url) + "</res></item></DIDL-Lite>";
    }

    private static String xml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String queryName(ContentResolver cr, Uri uri) {
        try (Cursor c = cr.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {}
        return uri.getLastPathSegment();
    }

    private static String getLocalIpForTv(String tvIp) throws Exception {
        try (DatagramSocket s = new DatagramSocket()) {
            s.connect(InetAddress.getByName(tvIp), 9);
            InetAddress a = s.getLocalAddress();
            if (a != null && !a.isAnyLocalAddress()) return a.getHostAddress();
        }
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            NetworkInterface ni = en.nextElement();
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress a = addrs.nextElement();
                if (a instanceof Inet4Address && !a.isLoopbackAddress() && a.isSiteLocalAddress()) return a.getHostAddress();
            }
        }
        throw new IOException("Adresse Wi-Fi locale introuvable");
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[8192]; int n;
        while ((n = in.read(b)) >= 0) out.write(b, 0, n);
        return out.toString("UTF-8");
    }

    private static class MediaServer {
        private final Context context; private final Uri uri; private final String mime; private final String name;
        private volatile boolean running; private ServerSocket ss; private Thread thread;
        MediaServer(Context c, Uri u, String m, String n) { context=c; uri=u; mime=m; name=n; }
        int getPort() { return ss == null ? 0 : ss.getLocalPort(); }
        void start() throws Exception {
            ss = new ServerSocket(0); running = true;
            thread = new Thread(() -> { while (running) { try { Socket s = ss.accept(); handle(s); } catch (Exception e) { if (running) e.printStackTrace(); } } }, "HisenseMediaServer");
            thread.setDaemon(true); thread.start();
        }
        void stop() { running=false; try { if (ss != null) ss.close(); } catch(Exception ignored){} }
        private long length() {
            try (android.os.ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r")) { return pfd == null ? -1 : pfd.getStatSize(); }
            catch(Exception e){ return -1; }
        }
        private void handle(Socket sock) {
            try (Socket s = sock; BufferedInputStream in = new BufferedInputStream(s.getInputStream()); OutputStream out = new BufferedOutputStream(s.getOutputStream())) {
                s.setSoTimeout(10000);
                String first = readLine(in); if (first == null) return;
                String range = null; String line;
                while ((line = readLine(in)) != null && !line.isEmpty()) if (line.toLowerCase(Locale.US).startsWith("range:")) range = line.substring(6).trim();
                boolean head = first.startsWith("HEAD ");
                long total = length(); long start=0, end=total > 0 ? total-1 : -1; boolean partial=false;
                if (range != null && range.startsWith("bytes=") && total > 0) {
                    String[] p = range.substring(6).split("-",2); start = Long.parseLong(p[0].trim()); if (p.length>1 && !p[1].trim().isEmpty()) end = Math.min(end, Long.parseLong(p[1].trim())); partial=true;
                }
                long contentLen = total > 0 ? (end-start+1) : -1;
                StringBuilder h = new StringBuilder(); h.append(partial ? "HTTP/1.1 206 Partial Content\r\n" : "HTTP/1.1 200 OK\r\n");
                h.append("Content-Type: ").append(mime).append("\r\nAccept-Ranges: bytes\r\nConnection: close\r\n");
                if (contentLen >= 0) h.append("Content-Length: ").append(contentLen).append("\r\n");
                if (partial) h.append("Content-Range: bytes ").append(start).append('-').append(end).append('/').append(total).append("\r\n");
                h.append("Content-Disposition: inline; filename=\"").append(name.replace("\"", "")).append("\"\r\n\r\n");
                out.write(h.toString().getBytes(StandardCharsets.UTF_8)); out.flush(); if (head) return;
                try (android.os.ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r"); FileInputStream fin = new FileInputStream(pfd.getFileDescriptor())) {
                    if (start > 0) fin.getChannel().position(start);
                    byte[] buf = new byte[64*1024]; long remain = contentLen; int n;
                    while (running && (remain != 0) && (n = fin.read(buf, 0, remain > 0 ? (int)Math.min(buf.length, remain) : buf.length)) > 0) { out.write(buf,0,n); if (remain > 0) remain -= n; }
                    out.flush();
                }
            } catch(Exception ignored) {}
        }
        private static String readLine(InputStream in) throws IOException {
            ByteArrayOutputStream b = new ByteArrayOutputStream(); int prev=-1, cur;
            while ((cur=in.read()) != -1) { if (prev=='\r' && cur=='\n') { byte[] a=b.toByteArray(); return new String(a,0,Math.max(0,a.length-1),StandardCharsets.ISO_8859_1); } b.write(cur); prev=cur; }
            return b.size()==0 ? null : b.toString("ISO-8859-1");
        }
    }
}
