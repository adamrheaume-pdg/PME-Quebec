from pathlib import Path

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

old='private void notifyBest(Station s,String fuel){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;Notification n=new Notification.Builder(this,CHANNEL_ID).setSmallIcon(R.drawable.ic_launcher).setContentTitle("Essence Québec • meilleur prix").setContentText(String.format(Locale.CANADA_FRENCH,"%.1f ¢/L chez %s • %.1f km",s.price,s.name,s.distance)).setAutoCancel(true).build();((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(7,n);}'
new='''private void notifyBest(Station s,String fuel){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        Uri mapsUri=Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+s.lat+","+s.lng);
        Intent mapsIntent=new Intent(Intent.ACTION_VIEW,mapsUri);
        mapsIntent.setPackage("com.google.android.apps.maps");
        if(mapsIntent.resolveActivity(getPackageManager())==null){mapsIntent.setPackage(null);}
        mapsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent go=PendingIntent.getActivity(this,7007,mapsIntent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(this,CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Essence Québec • meilleur prix")
            .setContentText(String.format(Locale.CANADA_FRENCH,"%.1f ¢/L chez %s • %.1f km",s.price,s.name,s.distance))
            .setContentIntent(go)
            .setAutoCancel(true)
            .build();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(7,n);
    }'''
if old not in s: raise SystemExit('notifyBest original introuvable')
s=s.replace(old,new,1)

required=['setContentIntent(go)','com.google.android.apps.maps','destination="+s.lat+","+s.lng','PendingIntent.FLAG_IMMUTABLE']
missing=[x for x in required if x not in s]
if missing: raise SystemExit('notification Maps incomplète: '+repr(missing))

p.write_text(s,encoding='utf-8')
print('Notifications: toucher ouvre la station exacte dans Google Maps')
