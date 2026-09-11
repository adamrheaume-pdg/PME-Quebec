package quebec.social360;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;
import java.util.Locale;

public class QuebecGateActivity extends Activity {
    private static final int RC_LOCATION=1001;
    private TextView status;

    @Override public void onCreate(Bundle b){super.onCreate(b); showGate();}

    private void showGate(){
        LinearLayout root=Ui.col(this); root.setBackgroundColor(Ui.BG); root.setPadding(Ui.dp(this,22),Ui.dp(this,34),Ui.dp(this,22),Ui.dp(this,20));
        TextView flag=Ui.text(this,"⚜",56,Ui.TEXT,true);flag.setGravity(Gravity.CENTER);root.addView(flag);
        TextView title=Ui.text(this,"Social Québec",30,Ui.TEXT,true);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView sub=Ui.text(this,"Le réseau social réservé au Québec",16,Ui.MUTED,false);sub.setGravity(Gravity.CENTER);root.addView(sub);
        root.addView(Ui.space(this,28));
        status=Ui.text(this,"Pour accéder à Social Québec, confirme ta présence au Québec. La position exacte n’est pas conservée.",16,Ui.TEXT,false);root.addView(status);
        root.addView(Ui.space(this,16));
        Button verify=Ui.button(this,"Vérifier ma présence au Québec"); verify.setOnClickListener(v->verify()); root.addView(verify);
        root.addView(Ui.space(this,14));
        root.addView(Ui.text(this,"Version 1 : validation GPS + pays de l’appareil. Une validation IP serveur sera ajoutée au backend public.",13,Ui.MUTED,false));
        setContentView(root);
    }

    private void verify(){
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},RC_LOCATION);return;}
        LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE);
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            status.setText("Active la localisation pour terminer la vérification."); startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); return;
        }
        status.setText("Vérification GPS…");
        try {
            lm.getCurrentLocation(LocationManager.GPS_PROVIDER,null,getMainExecutor(),loc->{
                if(loc==null){status.setText("Position indisponible. Réessaie avec le GPS activé.");return;}
                boolean inQc = roughQuebecBounds(loc.getLatitude(), loc.getLongitude());
                boolean localeOk = Locale.getDefault().getCountry().equalsIgnoreCase("CA");
                if(inQc && localeOk){
                    getSharedPreferences("gate",MODE_PRIVATE).edit().putBoolean("gps_ok",true).apply();
                    status.setText("Présence au Québec confirmée.");
                    startActivity(new Intent(this,MainActivity.class)); finish();
                } else status.setText("Accès refusé : la présence au Québec n’a pas pu être confirmée.");
            });
        } catch(SecurityException e){status.setText("Permission de localisation requise.");}
    }

    private boolean roughQuebecBounds(double lat,double lon){return lat>=44.9 && lat<=62.7 && lon>=-79.8 && lon<=-57.0;}

    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==RC_LOCATION && g.length>0 && g[0]==PackageManager.PERMISSION_GRANTED)verify();else status.setText("La localisation est nécessaire pour l’accès Québec seulement.");}
}
