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
import java.util.concurrent.*;
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

    public void pause() throws Exception {
        String ctl = ensureControl();
        soap(ctl, "Pause", "<InstanceID>0</InstanceID>");
        status("Pause.");
    }

    public void play() throws Exception {
        String ctl = ensureControl();
        soap(ctl, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>");
        status("Lecture.");
    }

    public void stopPlayback() throws Exception {
        String ctl = ensureControl();
        try { soap(ctl, "Stop", "<InstanceID>0</InstanceID>"); } finally {
            if (server != null) { server.stop(); server = null; }
        }
        status("Cast direct arrêté.");
    }

    private String ensureControl() throws Exception {
        if (controlUrl == null) controlUrl = findAvTransportControlUrl();
        return controlUrl;
    }

    private String findAvTransportControlUrl() throws Exception {
        List<String> candidates = Arrays.asList(
                "http://" + tvIp + ":38400/MediaRenderer/rendererdevicedesc.xml",
                "http://" + tvIp + ":38400/MediaServer/rendererdevicedesc.xml",
                "http://" + tvIp + ":38400/rendererdevicedesc.xml"
        );
        Exception last = null;
        for (String url : candidates) {
            try {
                String xml = httpGet(url);
                String control = parseControlUrl(xml, "AVTransport");
                if (control != null) return absolutize(url, control);
            } catch (Exception e) { last = e; }
        }
        // Common Hisense/VIDAA fallback.
        String[] fallback = {
                "http://" + tvIp + ":38400/MediaRenderer/AVTransport/control",
                "http://" + tvIp + ":38400/AVTransport/control"
        };
        for (String f : fallback) {
            try {
                soap(f, "GetTransportInfo", "<InstanceID>0</InstanceID>");
                return f;
            } catch (Exception e) { last = e; }
        }
        throw new IOException("Service DLNA/AVTransport introuvable sur la télé" + (last != null ? " : " + last.getMessage() : ""));
    }

    private static String parseControlUrl(String xml, String serviceName) {
        Pattern p = Pattern.compile("<service>\\s*.*?<serviceType>[^<]*" + serviceName + "[^<]*</serviceType>.*?<controlURL>([^<]+)</controlURL>.*?</service>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(xml);
        return m.find() ? m.group(1).trim() : null;
    }

    private static String absolutize(String descriptorUrl, String control) throws Exception {
        URL base = new URL(descriptorUrl);
        if (control.startsWith("http://") || control.startsWith("https://")) return control;
        if (!control.startsWith("/")) control = "/" + control;
        return base.getProtocol() + "://" + base.getHost() + ":" + base.getPort() + control;
    }

    private static String httpGet(String s) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(s).openConnection();
        c.setConnectTimeout(2500); c.setReadTimeout(3500); c.setRequestMethod("GET");
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
        c.setRequestProperty("SOAPAction", "\"" + service + "#" + action + "\"");
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
