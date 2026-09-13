package com.pmequebec.francaispme;

import android.app.*;
import android.content.*;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "francaispme_echeances";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(channelId, "Échéances de conformité", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(ch);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(context, channelId) : new Notification.Builder(context);
        b.setContentTitle("FrancaisPME Québec")
         .setContentText(title == null ? "Une échéance de conformité approche." : title)
         .setSmallIcon(android.R.drawable.ic_dialog_info)
         .setAutoCancel(true);
        nm.notify((int)(System.currentTimeMillis() % 100000), b.build());
    }
}
