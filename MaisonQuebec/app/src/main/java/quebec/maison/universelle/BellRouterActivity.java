package quebec.maison.universelle;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

/**
 * Contrôle local de la Borne Giga via l'API interne Sagemcom XMO.
 * Protocole adapté du projet MIT python-sagemcom-api (Mick Vleeshouwer, 2021).
 */
public class BellRouterActivity extends Activity {
    private TextView status, log;
    private EditText password;
    private Button go;
    private String targetIp="", targetMac="", gateway="192.168.2.1";
    private boolean block=true;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        Intent i=getIntent();
        if(i.getStringExtra("target_ip")!=null)targetIp=i.getStringExtra("target_ip");
        if(i.getStringExtra("target_mac")!=null)targetMac=i.getStringExtra("target_mac");
        if(i.getStringExtra("gateway")!=null&&!i.getStringExtra("gateway").isEmpty())gateway=i.getStringExtra("gateway");
        block=i.getBooleanExtra("block",true);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        final int base=dp(16);
        root.setPadding(base,base,base,base);
        scroll.addView(root,new ScrollView.LayoutParams(-1,-2));
        setContentView(scroll);

        // Android 15+ applique le edge-to-edge par défaut : on respecte les barres système.
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            int top=insets.getSystemWindowInsetTop();
            int bottom=insets.getSystemWindowInsetBottom();
            int left=insets.getSystemWindowInsetLeft();
            int right=insets.getSystemWindowInsetRight();
            root.setPadding(base+left,base+top,base+right,base+bottom);
            return insets;
        });
        scroll.requestApplyInsets();

        TextView title=tv("⚜ Borne Giga — contrôle direct",24,true);
        title.setTextColor(Color.rgb(0,63,151));
        root.addView(title);

        status=tv(targetIp.isEmpty()?"Test de connexion à la borne":(block?"Blocage":"Déblocage")+" de "+targetIp+(targetMac.isEmpty()?"":" • "+targetMac),15,true);
        status.setPadding(0,dp(10),0,dp(10));
        root.addView(status);

        password=new EditText(this);
        password.setHint("Mot de passe administrateur de la borne");
        password.setSingleLine(true);
        password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        root.addView(password,new LinearLayout.LayoutParams(-1,-2));

        go=new Button(this);
        go.setText(targetIp.isEmpty()?"TESTER L’API LOCALE":(block?"BLOQUER MAINTENANT":"DÉBLOQUER MAINTENANT"));
        go.setOnClickListener(v->runDirect());
        root.addView(go,new LinearLayout.LayoutParams(-1,-2));

        Button bell=new Button(this);
        bell.setText("OUVRIR L’INTERFACE BELL LOCALE");
        bell.setOnClickListener(v->openRouter());
        root.addView(bell,new LinearLayout.LayoutParams(-1,-2));

        log=tv("API locale XMO • /cgi/json-req • SHA-512 • compte admin.\nLe mot de passe est utilisé uniquement pour communiquer avec la borne sur ton réseau local.",13,false);
        log.setPadding(0,dp(12),0,dp(12));
        log.setTextIsSelectable(true);
        root.addView(log,new LinearLayout.LayoutParams(-1,-2));
    }

    private void runDirect(){
        final String pwd=password.getText().toString();
        if(pwd.isEmpty()){
            Toast.makeText(this,"Entre le mot de passe administrateur de la borne.",Toast.LENGTH_SHORT).show();
            password.requestFocus();
            return;
        }
        hideKeyboard();
        go.setEnabled(false);
        status.setText("Connexion directe à "+gateway+"…");
        log.setText("Authentification XMO SHA-512 en cours…");

        worker.execute(()->{
            XmoClient c=null;
            try{
                c=new XmoClient(gateway,"admin",pwd);
                c.login();

                Object info=null;
                try{info=c.getValue("Device/DeviceInfo");}catch(Exception ignored){}

                if(targetIp.isEmpty()&&targetMac.isEmpty()){
                    final String text="API XMO connectée avec succès.\n"+friendlyDeviceInfo(info);
                    safeUi(()->{
                        status.setText("✓ Connexion réussie");
                        log.setText(text);
                        go.setEnabled(true);
                    });
                    return;
                }

                Object hosts=c.getValue("Device/Hosts/Hosts");
                JSONObject host=findHost(hosts,targetMac,targetIp);
                if(host==null)throw new Exception("Appareil introuvable dans la liste officielle de la borne. Vérifie son IP/MAC puis relance un scan.");

                String uid=valueCI(host,"uid");
                String phys=valueCI(host,"PhysAddress");
                String ip=valueCI(host,"IPAddress");
                String name=firstNonEmpty(valueCI(host,"UserFriendlyName"),valueCI(host,"HostName"),valueCI(host,"Layer2Interface"),"Appareil");

                String basePath;
                if(uid!=null&&!uid.isEmpty())basePath="Device/Hosts/Hosts/Host[@uid='"+uid+"']";
                else if(phys!=null&&!phys.isEmpty())basePath="Device/Hosts/Hosts/Host[PhysAddress='"+phys+"']";
                else throw new Exception("Appareil trouvé, mais la borne n’a fourni ni UID ni MAC exploitable.");

                String blacklistPath=basePath+"/BlacklistEnable";
                String schedulePath=basePath+"/BlacklistedAccordingToSchedule";
                String runtimePath=basePath+"/Blacklisted";

                // Active le mécanisme global seulement s'il est désactivé. On ne le coupe jamais au déblocage,
                // afin de ne pas casser les règles des autres appareils.
                if(block){
                    try{
                        Object global=c.getValue("Device/Managers/NetworkLan/AccessControlEnable");
                        if(!asBoolean(global)){
                            c.setValue("Device/Managers/NetworkLan/AccessControlEnable","true");
                            sleep(250);
                        }
                    }catch(Exception ignored){}
                }

                c.setValue(blacklistPath,block?"true":"false");
                if(!block){
                    try{c.setValue(schedulePath,"false");}catch(Exception ignored){}
                }

                Object configured=null,runtime=null;
                boolean confirmed=false;
                for(int attempt=0;attempt<5;attempt++){
                    sleep(attempt==0?250:600);
                    configured=c.getValue(blacklistPath);
                    try{runtime=c.getValue(runtimePath);}catch(Exception ignored){}
                    if(asBoolean(configured)==block){
                        confirmed=true;
                        // Le champ Blacklisted peut être retardé ou absent selon le firmware.
                        if(runtime==null||asBoolean(runtime)==block||!block)break;
                    }
                }

                final Object fConfigured=configured;
                final Object fRuntime=runtime;
                final boolean ok=confirmed;
                final String summary="Appareil : "+name+"\nMAC : "+safe(phys)+"\nIP : "+safe(ip)+"\nÉtat configuré (BlacklistEnable) : "+booleanLabel(fConfigured)+"\nÉtat appliqué (Blacklisted) : "+(fRuntime==null?"non exposé par ce firmware":booleanLabel(fRuntime));

                safeUi(()->{
                    if(ok){
                        status.setText(block?"✓ BLOCAGE CONFIRMÉ PAR LA BORNE":"✓ DÉBLOCAGE CONFIRMÉ PAR LA BORNE");
                    }else{
                        status.setText("Commande envoyée, mais la borne n’a pas confirmé l’état demandé");
                    }
                    log.setText(summary);
                    go.setEnabled(true);
                });
            }catch(Exception e){
                final String m=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
                safeUi(()->{
                    status.setText("Échec du contrôle direct");
                    log.setText(m+"\n\nAucun faux succès n’est affiché : WiFi Québec exige une réponse de la borne avant de confirmer.");
                    go.setEnabled(true);
                });
            }finally{
                if(c!=null)try{c.logout();}catch(Exception ignored){}
            }
        });
    }

    private void hideKeyboard(){
        try{
            View v=getCurrentFocus();
            if(v==null)v=password;
            InputMethodManager imm=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(imm!=null)imm.hideSoftInputFromWindow(v.getWindowToken(),0);
            password.clearFocus();
        }catch(Exception ignored){}
    }

    private void openRouter(){
        try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("http://"+gateway+"/")));}
        catch(Exception e){Toast.makeText(this,"Impossible d’ouvrir l’interface locale.",Toast.LENGTH_SHORT).show();}
    }

    private void safeUi(Runnable r){if(!isFinishing()&&!isDestroyed())runOnUiThread(r);}
    private void sleep(long ms){try{Thread.sleep(ms);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private static String safe(String s){return s==null||s.isEmpty()?"non disponible":s;}
    private static String firstNonEmpty(String... values){for(String s:values)if(s!=null&&!s.trim().isEmpty())return s;return "";}
    private static String booleanLabel(Object o){return asBoolean(o)?"BLOQUÉ":"AUTORISÉ";}

    private static boolean asBoolean(Object o){
        if(o==null||o==JSONObject.NULL)return false;
        if(o instanceof Boolean)return (Boolean)o;
        if(o instanceof Number)return ((Number)o).intValue()!=0;
        if(o instanceof JSONObject){
            JSONObject j=(JSONObject)o;
            Iterator<String> it=j.keys();
            while(it.hasNext()){
                String k=it.next();
                if(k.equalsIgnoreCase("value"))return asBoolean(j.opt(k));
            }
        }
        String s=String.valueOf(o).trim().toLowerCase(Locale.ROOT);
        return s.equals("true")||s.equals("1")||s.equals("yes")||s.equals("on");
    }

    private static String friendlyDeviceInfo(Object info){
        if(info==null)return "Borne détectée, détails du modèle non disponibles.";
        JSONObject j=findFirstObjectContaining(info,"ModelName","ModelNumber","ProductClass");
        if(j==null&&info instanceof JSONObject)j=(JSONObject)info;
        if(j==null)return "Borne détectée.";
        String manufacturer=firstNonEmpty(valueCI(j,"Manufacturer"),"Sagemcom");
        String model=firstNonEmpty(valueCI(j,"ModelName"),valueCI(j,"ModelNumber"),valueCI(j,"ProductClass"));
        String sw=firstNonEmpty(valueCI(j,"SoftwareVersion"),valueCI(j,"ExternalFirmwareVersion"),valueCI(j,"InternalFirmwareVersion"));
        StringBuilder b=new StringBuilder("Borne détectée : ").append(manufacturer);
        if(!model.isEmpty())b.append(" ").append(model);
        if(!sw.isEmpty())b.append("\nFirmware : ").append(sw);
        return b.toString();
    }

    private static JSONObject findFirstObjectContaining(Object node,String... keys){
        if(node==null||node==JSONObject.NULL)return null;
        if(node instanceof JSONObject){
            JSONObject o=(JSONObject)node;
            for(String wanted:keys)if(valueCI(o,wanted)!=null)return o;
            Iterator<String> it=o.keys();
            while(it.hasNext()){
                JSONObject f=findFirstObjectContaining(o.opt(it.next()),keys);
                if(f!=null)return f;
            }
        }else if(node instanceof JSONArray){
            JSONArray a=(JSONArray)node;
            for(int i=0;i<a.length();i++){
                JSONObject f=findFirstObjectContaining(a.opt(i),keys);
                if(f!=null)return f;
            }
        }
        return null;
    }

    private static JSONObject findHost(Object node,String mac,String ip){
        if(node==null||node==JSONObject.NULL)return null;
        try{
            if(node instanceof JSONObject){
                JSONObject o=(JSONObject)node;
                String pm=valueCI(o,"PhysAddress"), pip=valueCI(o,"IPAddress");
                if((mac!=null&&!mac.isEmpty()&&sameMac(pm,mac))||(ip!=null&&!ip.isEmpty()&&ip.equalsIgnoreCase(pip)))return o;
                Iterator<String> it=o.keys();
                while(it.hasNext()){
                    JSONObject f=findHost(o.opt(it.next()),mac,ip);
                    if(f!=null)return f;
                }
            }else if(node instanceof JSONArray){
                JSONArray a=(JSONArray)node;
                for(int j=0;j<a.length();j++){
                    JSONObject f=findHost(a.opt(j),mac,ip);
                    if(f!=null)return f;
                }
            }
        }catch(Exception ignored){}
        return null;
    }

    private static boolean sameMac(String a,String b){
        if(a==null||b==null)return false;
        return a.replace("-",":").equalsIgnoreCase(b.replace("-",":"));
    }

    private static String valueCI(JSONObject o,String wanted){
        if(o==null)return null;
        Iterator<String> it=o.keys();
        while(it.hasNext()){
            String k=it.next();
            if(k.replace("_","").equalsIgnoreCase(wanted.replace("_",""))){
                Object v=o.opt(k);
                return v==null||v==JSONObject.NULL?null:String.valueOf(v);
            }
        }
        return null;
    }

    private TextView tv(String s,int sp,boolean bold){
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(Color.rgb(16,33,58));
        if(bold)t.setTypeface(null,1);
        return t;
    }

    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}

    static class XmoClient{
        private static final String ENDPOINT="/cgi/json-req";
        private final String host,user,passwordHash;
        private final SecureRandom rnd=new SecureRandom();
        private int requestId=-1,sessionId=0;
        private String serverNonce="";

        XmoClient(String h,String u,String p)throws Exception{
            host=h;user=u;passwordHash=sha512(p);
        }

        void login()throws Exception{
            JSONObject so=new JSONObject();
            so.put("nss",new JSONArray().put(new JSONObject().put("name","gtw").put("uri","http://sagemcom.com/gateway-data")));
            so.put("language","ident");
            so.put("context-flags",new JSONObject().put("get-content-name",true).put("local-time",true));
            so.put("capability-depth",2);
            so.put("capability-flags",new JSONObject().put("name",true).put("default-value",false).put("restriction",true).put("description",false));
            so.put("time-format","ISO_8601");
            so.put("write-only-string","_XMO_WRITE_ONLY_");
            so.put("undefined-write-only-string","_XMO_UNDEFINED_WRITE_ONLY_");

            JSONObject a=new JSONObject().put("id",0).put("method","logIn")
                .put("parameters",new JSONObject().put("user",user).put("persistent",true).put("session-options",so));
            JSONObject r=request(new JSONArray().put(a),true);
            JSONObject p=parameters(r,0);
            if(p==null)throw new Exception("Réponse de connexion invalide.");
            sessionId=p.optInt("id",0);
            serverNonce=p.optString("nonce","");
            if(sessionId==0||serverNonce.isEmpty())throw new Exception("Authentification refusée par la borne.");
        }

        void logout()throws Exception{
            if(sessionId==0)return;
            JSONObject a=new JSONObject().put("id",0).put("method","logOut");
            try{request(new JSONArray().put(a),false);}finally{sessionId=0;serverNonce="";}
        }

        Object getValue(String xpath)throws Exception{
            JSONObject a=new JSONObject().put("id",0).put("method","getValue")
                .put("xpath",quoteXpath(xpath)).put("options",new JSONObject());
            JSONObject r=request(new JSONArray().put(a),false);
            ensureActionOk(r,0,xpath);
            JSONObject p=parameters(r,0);
            if(p==null||!p.has("value"))throw new Exception("Aucune valeur retournée pour "+xpath);
            return p.opt("value");
        }

        void setValue(String xpath,String value)throws Exception{
            JSONObject a=new JSONObject().put("id",0).put("method","setValue")
                .put("xpath",quoteXpath(xpath))
                .put("parameters",new JSONObject().put("value",String.valueOf(value)))
                .put("options",new JSONObject());
            JSONObject r=request(new JSONArray().put(a),false);
            ensureActionOk(r,0,xpath);
        }

        private JSONObject request(JSONArray actions,boolean priority)throws Exception{
            requestId++;
            int cnonce=rnd.nextInt(500000);
            String cred=sha512(user+":"+serverNonce+":"+passwordHash);
            String auth=sha512(cred+":"+requestId+":"+cnonce+":JSON:"+ENDPOINT);
            JSONObject req=new JSONObject().put("id",requestId).put("session-id",sessionId).put("priority",priority)
                .put("actions",actions).put("cnonce",cnonce).put("auth-key",auth);
            String payload=new JSONObject().put("request",req).toString();
            byte[] data=("req="+URLEncoder.encode(payload,"UTF-8")).getBytes(StandardCharsets.UTF_8);

            HttpURLConnection c=(HttpURLConnection)new URL("http://"+host+ENDPOINT).openConnection();
            c.setConnectTimeout(7000);
            c.setReadTimeout(10000);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
            c.setRequestProperty("User-Agent","Python_Sagemcom WiFiQuebec/1.7");
            c.setFixedLengthStreamingMode(data.length);
            try(OutputStream os=c.getOutputStream()){os.write(data);}
            int code=c.getResponseCode();
            InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream();
            String body=read(is);
            c.disconnect();
            if(code!=200)throw new Exception("HTTP "+code+" de la borne : "+trimForLog(body));

            JSONObject r=new JSONObject(body);
            JSONObject reply=r.optJSONObject("reply");
            JSONObject err=reply==null?null:reply.optJSONObject("error");
            String d=err==null?"":err.optString("description","");
            if(!(d.equals("XMO_REQUEST_NO_ERR")||d.equals("Ok")||d.isEmpty())){
                throw new Exception("Erreur XMO : "+d);
            }
            return r;
        }

        private static void ensureActionOk(JSONObject r,int index,String xpath)throws Exception{
            try{
                JSONObject reply=r.getJSONObject("reply");
                JSONArray actions=reply.optJSONArray("actions");
                if(actions==null||index>=actions.length())return;
                JSONObject action=actions.optJSONObject(index);
                if(action==null)return;
                JSONObject err=action.optJSONObject("error");
                if(err==null)return;
                String d=err.optString("description","");
                if(d.isEmpty()||d.equals("XMO_NO_ERR")||d.equals("XMO_REQUEST_NO_ERR")||d.equalsIgnoreCase("Ok"))return;
                throw new Exception("Erreur XMO sur "+xpath+" : "+d);
            }catch(JSONException ignored){}
        }

        private static JSONObject parameters(JSONObject r,int i){
            try{return r.getJSONObject("reply").getJSONArray("actions").getJSONObject(i).getJSONArray("callbacks").getJSONObject(0).getJSONObject("parameters");}
            catch(Exception e){return null;}
        }

        private static String read(InputStream is)throws IOException{
            if(is==null)return "";
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            byte[] b=new byte[4096];
            int n;
            while((n=is.read(b))>0)out.write(b,0,n);
            return out.toString("UTF-8");
        }

        private static String sha512(String s)throws Exception{
            MessageDigest md=MessageDigest.getInstance("SHA-512");
            byte[] d=md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder x=new StringBuilder();
            for(byte b:d)x.append(String.format(Locale.US,"%02x",b&255));
            return x.toString();
        }

        private static String quoteXpath(String s){
            String safe="/=[]'@\"";
            StringBuilder out=new StringBuilder();
            for(byte b:s.getBytes(StandardCharsets.UTF_8)){
                int c=b&255;char ch=(char)c;
                if((c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9')||safe.indexOf(ch)>=0)out.append(ch);
                else out.append(String.format(Locale.US,"%%%02X",c));
            }
            return out.toString();
        }

        private static String trimForLog(String s){return s==null?"":(s.length()>300?s.substring(0,300)+"…":s);}
    }
}
