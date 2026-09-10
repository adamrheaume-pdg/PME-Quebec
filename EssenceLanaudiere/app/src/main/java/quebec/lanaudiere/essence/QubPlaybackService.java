package quebec.lanaudiere.essence;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;

public class QubPlaybackService extends Service {
    public static final String ACTION_PLAY="quebec.lanaudiere.essence.action.QUB_PLAY";
    public static final String ACTION_STOP="quebec.lanaudiere.essence.action.QUB_STOP";
    private static final String CHANNEL_ID="qub_radio_playback";
    private static final int NOTIFICATION_ID=995;
    private static final String STREAM_URL="https://playerservices.streamtheworld.com/api/livestream-redirect/QUB_RADIOAAC.aac?dist=essencequebec";
    private static volatile boolean active=false;
    private MediaPlayer player;

    public static boolean isActive(){ return active; }

    @Override public void onCreate(){
        super.onCreate();
        NotificationManager nm=getSystemService(NotificationManager.class);
        NotificationChannel ch=new NotificationChannel(CHANNEL_ID,"QUB radio",NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Lecture QUB radio en arrière-plan");
        ch.setSound(null,null);
        nm.createNotificationChannel(ch);
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String action=intent==null?ACTION_PLAY:intent.getAction();
        if(ACTION_STOP.equals(action)){
            stopPlayback();
            return START_NOT_STICKY;
        }
        active=true;
        startForeground(NOTIFICATION_ID,buildNotification("Connexion au direct…"));
        if(player==null) startPlayback();
        return START_NOT_STICKY;
    }

    private void startPlayback(){
        try{
            player=new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
            player.setDataSource(STREAM_URL);
            player.setOnPreparedListener(mp->{
                mp.start();
                NotificationManager nm=getSystemService(NotificationManager.class);
                nm.notify(NOTIFICATION_ID,buildNotification("En direct • lecture en arrière-plan"));
            });
            player.setOnErrorListener((mp,what,extra)->{
                stopPlayback();
                return true;
            });
            player.prepareAsync();
        }catch(Exception e){
            stopPlayback();
        }
    }

    private Notification buildNotification(String status){
        Intent open=new Intent(this,MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi=PendingIntent.getActivity(this,21,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        Intent stop=new Intent(this,QubPlaybackService.class).setAction(ACTION_STOP);
        PendingIntent stopPi=PendingIntent.getService(this,22,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this,CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("QUB radio • EN DIRECT")
                .setContentText(status)
                .setContentIntent(openPi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(new Notification.Action.Builder(null,"ARRÊTER",stopPi).build())
                .build();
    }

    private void stopPlayback(){
        active=false;
        if(player!=null){
            try{ if(player.isPlaying()) player.stop(); }catch(Exception ignored){}
            player.reset();
            player.release();
            player=null;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override public void onDestroy(){
        if(player!=null){
            try{ player.release(); }catch(Exception ignored){}
            player=null;
        }
        active=false;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent){ return null; }
}
