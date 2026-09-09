package quebec.maison.universelle;

import android.content.*;
import java.net.*;

public class AutomationReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent){
        final String ip=intent.getStringExtra("ip");
        final String protocol=intent.getStringExtra("protocol");
        final String name=intent.getStringExtra("name");
        final boolean on=intent.getBooleanExtra("on",false);
        if(ip==null||protocol==null)return;
        new Thread(()->{
            DomotiqueActivity.Device d=new DomotiqueActivity.Device(ip,name==null?ip:name,"",protocol,"",true);
            DomotiqueActivity.send(d,on);
        }).start();
    }
}
