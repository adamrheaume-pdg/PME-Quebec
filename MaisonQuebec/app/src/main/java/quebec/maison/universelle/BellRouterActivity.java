package quebec.maison.universelle;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.text.InputType;
import android.view.*;
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
    private String targetIp="", targetMac="", gateway="192.168.2.1";
    private boolean block=true;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        Intent i=getIntent();
        if(i.getStringExtra("target_ip")!=null)targetIp=i.getStringExtra("target_ip");
        if(i.getStringExtra("target_mac")!=null)targetMac=i.getStringExtra("target_mac");
        if(i.getStringExtra("gateway")!=null&&!i.getStringExtra("gateway").isEmpty())gateway=i.getStringExtra("gateway");
        block=i.getBooleanExtra("block",true);

        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(18,18,18,18);root.setBackgroundColor(Color.WHITE);
        TextView title=tv("⚜ Borne Giga — contrôle direct",24,true);title.setTextColor(Color.rgb(0,63,151));root.addView(title);
        status=tv((targetIp.isEmpty()?"Test de connexion":(block?"Blocage":"Déblocage")+" de "+targetIp+(targetMac.isEmpty()?"":" • "+targetMac)),15,true);status.setPadding(0,10,0,10);root.addView(status);

        password=new EditText(this);password.setHint("Mot de passe administrateur de la borne");password.setSingleLine(true);password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);root.addView(password,new LinearLayout.LayoutParams(-1,-2));

        Button go=new Button(this);go.setText(targetIp.isEmpty()?"Tester l’API locale":(block?"BLOQUER MAINTENANT":"DÉBLOQUER MAINTENANT"));go.setOnClickListener(v->runDirect());root.addView(go,new LinearLayout.LayoutParams(-1,-2));
        Button bell=new Button(this);bell.setText("Ouvrir l’interface Bell locale");bell.setOnClickListener(v->openRouter());root.addView(bell,new LinearLayout.LayoutParams(-1,-2));

        log=tv("Méthode : API locale XMO /cgi/json-req • SHA-512 • compte admin.\nAucun mot de passe n’est envoyé ailleurs que vers ta borne locale.",13,false);log.setPadding(0,12,0,0);root.addView(log,new LinearLayout.LayoutParams(-1,-2));
        setContentView(root);
    }

    private void runDirect(){
        final String pwd=password.getText().toString();
        if(pwd.isEmpty()){Toast.makeText(this,"Entre le mot de passe administrateur de la borne.",Toast.LENGTH_SHORT).show();return;}
        status.setText("Connexion directe à "+gateway+"…");log.setText("Authentification SHA-512 en cours…");
        worker.execute(()->{
            try{
                XmoClient c=new XmoClient(gateway,"admin",pwd);
                c.login();
                Object info=null;try{info=c.getValue("Device/DeviceInfo");}catch(Exception ignored){}
                if(targetIp.isEmpty()&&targetMac.isEmpty()){
                    final String text="API XMO connectée avec succès."+(info==null?"":"\nBorne détectée : "+shortJson(info));
                    runOnUiThread(()->{status.setText("✓ Connexion réussie");log.setText(text);});return;
                }
                Object hosts=c.getValue("Device/Hosts/Hosts");
                JSONObject host=findHost(hosts,targetMac,targetIp);
                if(host==null)throw new Exception("Appareil introuvable dans Device/Hosts/Hosts. MAC/IP cible : "+(targetMac.isEmpty()?targetIp:targetMac));
                String uid=valueCI(host,"uid");
                String phys=valueCI(host,"PhysAddress");
                String ip=valueCI(host,"IPAddress");
                String xpath;
                if(uid!=null&&!uid.isEmpty())xpath="Device/Hosts/Hosts/Host[@uid='"+uid+"']/BlacklistEnable";
                else if(phys!=null&&!phys.isEmpty())xpath="Device/Hosts/Hosts/Host[PhysAddress='"+phys+"']/BlacklistEnable";
                else throw new Exception("L’appareil a été trouvé, mais la borne n’a fourni ni UID ni MAC exploitable.");

                try{c.setValue("Device/Managers/NetworkLan/AccessControlEnable","true");}catch(Exception ignored){}
                c.setValue(xpath,block?"true":"false");
                if(!block){try{c.setValue(xpath.replace("/BlacklistEnable","/BlacklistedAccordingToSchedule"),"false");}catch(Exception ignored){}}
                Object verify=c.getValue(xpath);
                boolean ok=String.valueOf(verify).toLowerCase(Locale.ROOT).contains(block?"true":"false");
                final String result="Cible Borne Giga : "+(phys==null?"":phys)+(ip==null?"":" • "+ip)+"\nXPath : "+xpath+"\nRéponse de vérification : "+String.valueOf(verify);
                runOnUiThread(()->{status.setText(ok?(block?"✓ BLOCAGE CONFIRMÉ PAR LA BORNE":"✓ DÉBLOCAGE CONFIRMÉ PAR LA BORNE"):"Commande envoyée — vérification non concluante");log.setText(result);});
            }catch(Exception e){
                final String m=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
                runOnUiThread(()->{status.setText("Échec du contrôle direct");log.setText(m+"\n\nCette version n’affiche pas un faux succès : il faut une confirmation retournée par l’API de la borne.");});
            }
        });
    }

    private void openRouter(){try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("http://"+gateway+"/")));}catch(Exception ignored){}}

    private static JSONObject findHost(Object node,String mac,String ip){
        if(node==null||node==JSONObject.NULL)return null;
        try{
            if(node instanceof JSONObject){
                JSONObject o=(JSONObject)node;
                String pm=valueCI(o,"PhysAddress"), pip=valueCI(o,"IPAddress");
                if((!mac.isEmpty()&&sameMac(pm,mac))||(!ip.isEmpty()&&ip.equalsIgnoreCase(pip)))return o;
                Iterator<String> it=o.keys();while(it.hasNext()){JSONObject f=findHost(o.opt(it.next()),mac,ip);if(f!=null)return f;}
            }else if(node instanceof JSONArray){JSONArray a=(JSONArray)node;for(int j=0;j<a.length();j++){JSONObject f=findHost(a.opt(j),mac,ip);if(f!=null)return f;}}
        }catch(Exception ignored){}
        return null;
    }
    private static boolean sameMac(String a,String b){if(a==null||b==null)return false;return a.replace("-",":").equalsIgnoreCase(b.replace("-",":"));}
    private static String valueCI(JSONObject o,String wanted){Iterator<String> it=o.keys();while(it.hasNext()){String k=it.next();if(k.replace("_","").equalsIgnoreCase(wanted.replace("_",""))){Object v=o.opt(k);return v==null||v==JSONObject.NULL?null:String.valueOf(v);}}return null;}
    private static String shortJson(Object o){String s=String.valueOf(o);return s.length()>500?s.substring(0,500)+"…":s;}
    private TextView tv(String s,int sp,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(Color.rgb(16,33,58));if(bold)t.setTypeface(null,1);return t;}
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}

    static class XmoClient{
        private static final String ENDPOINT="/cgi/json-req";
        private final String host,user,password,passwordHash;
        private final SecureRandom rnd=new SecureRandom();
        private int requestId=-1,sessionId=0;
        private String serverNonce="";
        XmoClient(String h,String u,String p)throws Exception{host=h;user=u;password=p;passwordHash=sha512(p);}

        void login()throws Exception{
            JSONObject so=new JSONObject();so.put("nss",new JSONArray().put(new JSONObject().put("name","gtw").put("uri","http://sagemcom.com/gateway-data")));so.put("language","ident");so.put("context-flags",new JSONObject().put("get-content-name",true).put("local-time",true));so.put("capability-depth",2);so.put("capability-flags",new JSONObject().put("name",true).put("default-value",false).put("restriction",true).put("description",false));so.put("time-format","ISO_8601");so.put("write-only-string","_XMO_WRITE_ONLY_");so.put("undefined-write-only-string","_XMO_UNDEFINED_WRITE_ONLY_");
            JSONObject a=new JSONObject().put("id",0).put("method","logIn").put("parameters",new JSONObject().put("user",user).put("persistent",true).put("session-options",so));
            JSONObject r=request(new JSONArray().put(a),true);
            JSONObject p=parameters(r,0);if(p==null)throw new Exception("Réponse de connexion invalide.");
            sessionId=p.optInt("id",0);serverNonce=p.optString("nonce","");
            if(sessionId==0||serverNonce.isEmpty())throw new Exception("Authentification refusée par la borne.");
        }
        Object getValue(String xpath)throws Exception{
            JSONObject a=new JSONObject().put("id",0).put("method","getValue").put("xpath",quoteXpath(xpath)).put("options",new JSONObject());
            JSONObject p=parameters(request(new JSONArray().put(a),false),0);if(p==null||!p.has("value"))throw new Exception("Aucune valeur retournée pour "+xpath);return p.opt("value");
        }
        void setValue(String xpath,String value)throws Exception{
            JSONObject a=new JSONObject().put("id",0).put("method","setValue").put("xpath",quoteXpath(xpath)).put("parameters",new JSONObject().put("value",value)).put("options",new JSONObject());
            request(new JSONArray().put(a),false);
        }
        private JSONObject request(JSONArray actions,boolean priority)throws Exception{
            requestId++;int cnonce=rnd.nextInt(500000);
            String cred=sha512(user+":"+serverNonce+":"+passwordHash);
            String auth=sha512(cred+":"+requestId+":"+cnonce+":JSON:"+ENDPOINT);
            JSONObject req=new JSONObject().put("id",requestId).put("session-id",sessionId).put("priority",priority).put("actions",actions).put("cnonce",cnonce).put("auth-key",auth);
            String payload=new JSONObject().put("request",req).toString();
            byte[] data=("req="+URLEncoder.encode(payload,"UTF-8")).getBytes(StandardCharsets.UTF_8);
            HttpURLConnection c=(HttpURLConnection)new URL("http://"+host+ENDPOINT).openConnection();c.setConnectTimeout(7000);c.setReadTimeout(10000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");c.setRequestProperty("User-Agent","Python_Sagemcom WiFiQuebec/1.6");c.setFixedLengthStreamingMode(data.length);try(OutputStream os=c.getOutputStream()){os.write(data);}int code=c.getResponseCode();InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream();String body=read(is);c.disconnect();if(code!=200)throw new Exception("HTTP "+code+" de la borne : "+body);
            JSONObject r=new JSONObject(body);JSONObject err=r.optJSONObject("reply")==null?null:r.optJSONObject("reply").optJSONObject("error");String d=err==null?"":err.optString("description","");if(!(d.equals("XMO_REQUEST_NO_ERR")||d.equals("Ok")||d.isEmpty())){
                JSONArray aa=r.optJSONObject("reply")==null?null:r.optJSONObject("reply").optJSONArray("actions");String ad="";if(aa!=null&&aa.length()>0&&aa.optJSONObject(0)!=null&&aa.optJSONObject(0).optJSONObject("error")!=null)ad=aa.optJSONObject(0).optJSONObject("error").optString("description","");throw new Exception("Erreur XMO : "+(ad.isEmpty()?d:ad));
            }return r;
        }
        private static JSONObject parameters(JSONObject r,int i){try{return r.getJSONObject("reply").getJSONArray("actions").getJSONObject(i).getJSONArray("callbacks").getJSONObject(0).getJSONObject("parameters");}catch(Exception e){return null;}}
        private static String read(InputStream is)throws IOException{if(is==null)return "";ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=is.read(b))>0)out.write(b,0,n);return out.toString("UTF-8");}
        private static String sha512(String s)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-512");byte[] d=md.digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte b:d)x.append(String.format(Locale.US,"%02x",b&255));return x.toString();}
        private static String quoteXpath(String s){String safe="/=[]'@\"-._~";StringBuilder out=new StringBuilder();for(byte b:s.getBytes(StandardCharsets.UTF_8)){int c=b&255;char ch=(char)c;if((c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9')||safe.indexOf(ch)>=0)out.append(ch);else out.append(String.format(Locale.US,"%%%02X",c));}return out.toString();}
    }
}
