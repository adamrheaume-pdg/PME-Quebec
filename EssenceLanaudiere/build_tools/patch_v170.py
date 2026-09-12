from pathlib import Path

p=Path("EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java")
s=p.read_text(encoding="utf-8")

s=s.replace(
    'private static final int REQ_LOCATION=101, REQ_NOTIFICATIONS=102;',
    'private static final int REQ_LOCATION=101, REQ_NOTIFICATIONS=102, REQ_PROFILE_PHOTO=103;',
    1
)

old='''        Button fav=toolButton("★  SUIS TES STATIONS FAVORITES"); fav.setOnClickListener(v->showFavorites()); grid.addView(fav); page.addView(grid);'''
new='''        Button fav=toolButton("★  SUIS TES STATIONS FAVORITES"); fav.setOnClickListener(v->showFavorites()); grid.addView(fav);
        Button profile=toolButton("👤  MON PROFIL ESSENCE QUÉBEC"); profile.setOnClickListener(v->showPilotProfile()); grid.addView(profile);
        Button offline=toolButton("⬇  MODE HORS LIGNE"); offline.setOnClickListener(v->showOfflineManager()); grid.addView(offline);
        page.addView(grid);'''
if old in s:
    s=s.replace(old,new,1)

old='''        status=text("Prix récents • données ouvertes • vérifie le prix à la pompe",12,false,Color.rgb(220,228,238)); status.setPadding(0,dp(8),0,dp(10)); page.addView(status);'''
new='''        status=text(eqIsOnline()?"Prix récents • données ouvertes • vérifie le prix à la pompe":"MODE HORS LIGNE • dernières données enregistrées",12,true,eqIsOnline()?Color.rgb(220,228,238):Color.rgb(255,210,70)); status.setPadding(0,dp(8),0,dp(10)); page.addView(status);'''
if old in s:
    s=s.replace(old,new,1)

if 'cacheStations(s);' not in s:
    s=s.replace('lastStations=new ArrayList<>(s);', 'lastStations=new ArrayList<>(s); cacheStations(s);')

old_hours='''    private void showStationHours(Station s){
        new AlertDialog.Builder(this).setTitle("Horaires • "+s.name)
            .setMessage("Les heures d'ouverture ne sont pas disponibles dans la source actuelle pour cette station. Aucune heure n'est inventée.")
            .setPositiveButton("FERMER",null).show();
    }'''
new_hours=r'''    private void showStationHours(Station station){
        final String key=eqGooglePlacesKey();
        if(key.isEmpty()){
            new AlertDialog.Builder(this)
                .setTitle("Horaires • "+station.name)
                .setMessage("Google Places est prêt dans Essence Québec, mais aucune clé Google Places API n'est configurée pour cette compilation. Vous pouvez quand même ouvrir la fiche Google Maps de cette station.")
                .setPositiveButton("GOOGLE MAPS",(d,w)->{
                    String q=station.name+" "+displayStationAddress(station);
                    Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/search/?api=1&query="+Uri.encode(q)));
                    startActivity(i);
                }).setNegativeButton("FERMER",null).show();
            return;
        }
        Toast.makeText(this,"Chargement des horaires Google…",Toast.LENGTH_SHORT).show();
        new Thread(()->{
            String result=eqFetchGoogleHours(station,key);
            runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Horaires • "+station.name)
                .setMessage(result).setPositiveButton("FERMER",null).show());
        }).start();
    }

    private String eqGooglePlacesKey(){
        try{
            android.content.pm.ApplicationInfo ai=getPackageManager().getApplicationInfo(getPackageName(),PackageManager.GET_META_DATA);
            if(ai.metaData==null)return "";
            String k=ai.metaData.getString("com.google.android.geo.API_KEY","");
            return k==null?"":k.trim();
        }catch(Exception e){return "";}
    }

    private String eqFetchGoogleHours(Station station,String key){
        HttpURLConnection c=null;
        try{
            URL u=new URL("https://places.googleapis.com/v1/places:searchText");
            c=(HttpURLConnection)u.openConnection();
            c.setConnectTimeout(8000); c.setReadTimeout(9000); c.setRequestMethod("POST"); c.setDoOutput(true);
            c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
            c.setRequestProperty("X-Goog-Api-Key",key);
            c.setRequestProperty("X-Goog-FieldMask","places.displayName,places.formattedAddress,places.currentOpeningHours,places.regularOpeningHours");
            JSONObject body=new JSONObject();
            body.put("textQuery",station.name+" "+displayStationAddress(station));
            JSONObject bias=new JSONObject(); JSONObject circle=new JSONObject(); JSONObject center=new JSONObject();
            center.put("latitude",station.lat); center.put("longitude",station.lng); circle.put("center",center); circle.put("radius",1500.0); bias.put("circle",circle); body.put("locationBias",bias);
            try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
            int code=c.getResponseCode();
            InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
            StringBuilder sb=new StringBuilder(); try(BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=br.readLine())!=null)sb.append(line);}
            if(code<200||code>=300)return "Google Places n'a pas pu fournir les horaires pour le moment.";
            JSONObject root=new JSONObject(sb.toString()); JSONArray places=root.optJSONArray("places");
            if(places==null||places.length()==0)return "Aucun horaire Google correspondant à cette station.";
            JSONObject place=places.optJSONObject(0);
            JSONObject hours=place.optJSONObject("currentOpeningHours");
            if(hours==null)hours=place.optJSONObject("regularOpeningHours");
            if(hours==null)return "Horaire non disponible sur Google pour cette station.";
            StringBuilder out=new StringBuilder();
            if(hours.has("openNow"))out.append(hours.optBoolean("openNow")?"OUVERT MAINTENANT":"FERMÉ MAINTENANT").append("\n\n");
            JSONArray days=hours.optJSONArray("weekdayDescriptions");
            if(days!=null)for(int i=0;i<days.length();i++)out.append(days.optString(i)).append(i==days.length()-1?"":"\n");
            if(out.length()==0)return "Horaire non disponible sur Google pour cette station.";
            return out.toString();
        }catch(Exception e){
            return "Impossible de charger les horaires Google. Vérifiez la connexion Internet.";
        }finally{if(c!=null)c.disconnect();}
    }'''
if old_hours in s:
    s=s.replace(old_hours,new_hours,1)

if 'private void eqCycleAlertText' not in s:
    marker='    private void showTrendDialog(){'
    methods=r'''
    private void eqCycleAlertText(TextView v){
        final int[] colors={Color.rgb(44,235,135),Color.rgb(255,215,65),Color.WHITE};
        final Handler h=new Handler(Looper.getMainLooper()); final int[] i={0}; final Runnable[] r=new Runnable[1];
        r[0]=()->{if(!v.isAttachedToWindow())return; v.setTextColor(colors[i[0]%colors.length]); i[0]++; h.postDelayed(r[0],1800);};
        v.post(r[0]);
    }

'''
    if marker in s:
        s=s.replace(marker,methods+marker,1)

s=s.replace(
    'TextView addr=text(displayStationAddress(x),14,false,ink);',
    'TextView addr=text(displayStationAddress(x),14,false,ink); if(isHighest)eqCycleAlertText(addr);'
)
s=s.replace(
    'TextView nm=text(x.name,19,true,ink);',
    'TextView nm=text(x.name,19,true,ink); if(isHighest)eqCycleAlertText(nm);'
)

if 'private void showPilotProfile()' not in s:
    marker='    private View fakeSearchBar(){'
    methods=r'''
    private SharedPreferences eqProfilePrefs(){return getSharedPreferences("eq_profile",MODE_PRIVATE);}

    private String eqInstallId(){
        SharedPreferences p=eqProfilePrefs(); String id=p.getString("install_id","");
        if(id.isEmpty()){id="EQ-"+UUID.randomUUID().toString().toUpperCase(Locale.CANADA).substring(0,13);p.edit().putString("install_id",id).apply();}
        return id;
    }

    private void showPilotProfile(){
        SharedPreferences p=eqProfilePrefs();
        if(p.getString("contact","").trim().isEmpty()){showAccountSetup();return;}
        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); sc.setBackgroundColor(Color.rgb(5,12,28));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(28),dp(24),dp(28),dp(36));sc.addView(root);
        TextView back=text("‹",44,true,Color.WHITE);back.setOnClickListener(v->showMain());root.addView(back,new LinearLayout.LayoutParams(-1,dp(54)));

        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView avatar=new ImageView(this);avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);avatar.setBackground(round(Color.rgb(20,28,50),60));
        File f=new File(new File(getFilesDir(),"profile"),"avatar.jpg");
        if(f.exists()){android.graphics.Bitmap b=android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath());if(b!=null)avatar.setImageBitmap(b);}else avatar.setImageResource(R.drawable.essence_quebec_logo);
        avatar.setOnClickListener(v->pickProfilePhoto());
        head.addView(avatar,new LinearLayout.LayoutParams(dp(86),dp(86)));
        LinearLayout ht=new LinearLayout(this);ht.setOrientation(LinearLayout.VERTICAL);ht.setPadding(dp(16),0,0,0);
        String display=p.getString("name","").trim();if(display.isEmpty())display="Pilote Québec";
        ht.addView(text(display,28,true,Color.WHITE));
        ht.addView(text("Identifiant "+eqInstallId(),12,false,Color.rgb(180,195,220)));
        ht.addView(text(p.getString("contact",""),13,false,Color.rgb(180,195,220)));
        head.addView(ht,new LinearLayout.LayoutParams(0,-2,1));root.addView(head);

        int xp=p.getInt("xp",0);int level=1+(xp/1000);int current=xp%1000;
        LinearLayout levelCard=eqProfileCard();levelCard.addView(text("Niveau "+level+"     "+current+" / 1 000 XP",16,true,Color.WHITE));
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setProgress(current);levelCard.addView(bar,new LinearLayout.LayoutParams(-1,dp(16)));root.addView(levelCard);

        double savings=Double.longBitsToDouble(p.getLong("savings_total",Double.doubleToLongBits(0)));
        LinearLayout save=eqProfileCard();save.addView(text("ÉCONOMIE AUJOURD'HUI",16,true,Color.WHITE));
        LinearLayout sr=new LinearLayout(this);sr.setGravity(Gravity.CENTER_VERTICAL);
        sr.addView(text(String.format(Locale.CANADA_FRENCH,"%.2f $",savings),38,true,Color.rgb(40,235,140)),new LinearLayout.LayoutParams(0,-2,1));
        ImageView money=new ImageView(this);money.setImageResource(R.drawable.money_bundle);money.setScaleType(ImageView.ScaleType.CENTER_INSIDE);sr.addView(money,new LinearLayout.LayoutParams(dp(70),dp(50)));save.addView(sr);
        root.addView(save);

        int trips=p.getInt("trip_count",0);int favs=eqFavoriteCount();double litres=Double.longBitsToDouble(p.getLong("litres_total",Double.doubleToLongBits(0)));
        LinearLayout stats=eqProfileCard();stats.addView(text("TES STATS",20,true,Color.WHITE));
        stats.addView(text("⛽ "+trips+" trajets/pleins suivis   ★ "+favs+" favoris\n"+String.format(Locale.CANADA_FRENCH,"%.1f L estimés • %.2f $ économisés",litres,savings),16,true,Color.rgb(215,225,240)));
        root.addView(stats);

        Button photo=primaryButton("AJOUTER / MODIFIER LA PHOTO");photo.setOnClickListener(v->pickProfilePhoto());root.addView(photo,new LinearLayout.LayoutParams(-1,dp(56)));
        Button edit=ghostButton("MODIFIER MON PROFIL");edit.setOnClickListener(v->showAccountSetup());root.addView(edit,new LinearLayout.LayoutParams(-1,dp(56)));
        Button trip=primaryButton("AJOUTER UN TRAJET");trip.setOnClickListener(v->showAddTrip());root.addView(trip,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView local=text(eqIsOnline()?"Synchronisation réseau disponible • données principales conservées localement":"MODE HORS LIGNE • profil, trajets, favoris et statistiques disponibles",13,true,eqIsOnline()?Color.rgb(90,220,170):Color.rgb(255,210,70));local.setPadding(0,dp(20),0,0);root.addView(local);
        setContentView(sc);
    }

    private LinearLayout eqProfileCard(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(round(Color.rgb(12,24,49),18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(16);c.setLayoutParams(lp);return c;}

    private void showAccountSetup(){
        SharedPreferences p=eqProfilePrefs(); LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(22),dp(8),dp(22),0);
        EditText name=new EditText(this);name.setHint("Nom ou pseudonyme");name.setText(p.getString("name",""));box.addView(name);
        EditText contact=new EditText(this);contact.setHint("Courriel ou numéro de téléphone");contact.setText(p.getString("contact",""));box.addView(contact);
        EditText cons=new EditText(this);cons.setHint("Consommation L/100 km");cons.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);double cv=Double.longBitsToDouble(p.getLong("consumption",Double.doubleToLongBits(8.5)));cons.setText(String.format(Locale.CANADA_FRENCH,"%.1f",cv));box.addView(cons);
        new AlertDialog.Builder(this).setTitle("Compte Essence Québec").setView(box).setMessage("Aucun mot de passe. L'application crée un identifiant Essence Québec unique pour cet appareil.")
            .setPositiveButton("ENREGISTRER",(d,w)->{
                String c=contact.getText().toString().trim();if(c.isEmpty()){Toast.makeText(this,"Entre un courriel ou un numéro de téléphone.",Toast.LENGTH_LONG).show();return;}
                double v=8.5;try{v=Double.parseDouble(cons.getText().toString().replace(',','.'));}catch(Exception ignored){}
                p.edit().putString("name",name.getText().toString().trim()).putString("contact",c).putLong("consumption",Double.doubleToLongBits(Math.max(1.0,v))).apply();eqInstallId();showPilotProfile();
            }).setNegativeButton("ANNULER",null).show();
    }

    private void pickProfilePhoto(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,REQ_PROFILE_PHOTO);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQ_PROFILE_PHOTO&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri u=data.getData();
            try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
            File dir=new File(getFilesDir(),"profile");dir.mkdirs();File out=new File(dir,"avatar.jpg");
            try(InputStream in=getContentResolver().openInputStream(u);OutputStream os=new FileOutputStream(out)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)os.write(buf,0,n);}
            catch(Exception e){Toast.makeText(this,"Impossible d'enregistrer cette photo.",Toast.LENGTH_LONG).show();}
            showPilotProfile();
        }
    }

    private int eqFavoriteCount(){
        int n=0;for(Map.Entry<String,?> e:getSharedPreferences("eq_favorites",MODE_PRIVATE).getAll().entrySet())if(Boolean.TRUE.equals(e.getValue()))n++;return n;
    }

    private void showAddTrip(){
        SharedPreferences p=eqProfilePrefs();LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(22),0,dp(22),0);
        EditText km=new EditText(this);km.setHint("Distance du trajet (km)");km.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);box.addView(km);
        new AlertDialog.Builder(this).setTitle("Ajouter un trajet").setView(box).setPositiveButton("AJOUTER",(d,w)->{
            double distance=0;try{distance=Double.parseDouble(km.getText().toString().replace(',','.'));}catch(Exception ignored){}
            if(distance<=0){Toast.makeText(this,"Distance invalide.",Toast.LENGTH_SHORT).show();return;}
            double cons=Double.longBitsToDouble(p.getLong("consumption",Double.doubleToLongBits(8.5)));double litres=distance*cons/100.0;double savings=0;
            if(lastStations!=null&&lastStations.size()>1){double lo=Double.MAX_VALUE,hi=0;for(Station st:lastStations){lo=Math.min(lo,st.price);hi=Math.max(hi,st.price);}if(lo<Double.MAX_VALUE&&hi>lo)savings=litres*((hi-lo)/100.0);}
            int trips=p.getInt("trip_count",0)+1;int xp=p.getInt("xp",0)+25;double ts=Double.longBitsToDouble(p.getLong("savings_total",Double.doubleToLongBits(0)))+savings;double tl=Double.longBitsToDouble(p.getLong("litres_total",Double.doubleToLongBits(0)))+litres;
            JSONArray arr;try{arr=new JSONArray(p.getString("trips","[]"));}catch(Exception e){arr=new JSONArray();}
            JSONObject o=new JSONObject();try{o.put("time",System.currentTimeMillis());o.put("km",distance);o.put("litres",litres);o.put("savings",savings);arr.put(o);}catch(Exception ignored){}
            p.edit().putInt("trip_count",trips).putInt("xp",xp).putLong("savings_total",Double.doubleToLongBits(ts)).putLong("litres_total",Double.doubleToLongBits(tl)).putString("trips",arr.toString()).apply();
            Toast.makeText(this,String.format(Locale.CANADA_FRENCH,"%.1f L estimés • %.2f $ d'économie potentielle",litres,savings),Toast.LENGTH_LONG).show();showPilotProfile();
        }).setNegativeButton("ANNULER",null).show();
    }

    private boolean eqIsOnline(){
        try{
            android.net.ConnectivityManager cm=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            if(cm==null)return false;android.net.Network n=cm.getActiveNetwork();if(n==null)return false;android.net.NetworkCapabilities cp=cm.getNetworkCapabilities(n);
            return cp!=null&&cp.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }catch(Exception e){return false;}
    }

    private void cacheStations(List<Station> stations){
        try{
            JSONArray a=new JSONArray();for(Station st:stations){JSONObject o=new JSONObject();o.put("name",st.name);o.put("address",st.address);o.put("city",st.city);o.put("lat",st.lat);o.put("lng",st.lng);o.put("price",st.price);o.put("distance",st.distance);a.put(o);}
            getSharedPreferences("eq_offline",MODE_PRIVATE).edit().putString("stations",a.toString()).putLong("updated",System.currentTimeMillis()).apply();
        }catch(Exception ignored){}
    }

    private void showOfflineManager(){
        SharedPreferences p=getSharedPreferences("eq_offline",MODE_PRIVATE);long t=p.getLong("updated",0);String raw=p.getString("stations","[]");int n=0;try{n=new JSONArray(raw).length();}catch(Exception ignored){}
        String when=t==0?"jamais":android.text.format.DateFormat.format("yyyy-MM-dd HH:mm",t).toString();
        new AlertDialog.Builder(this).setTitle("Mode hors ligne")
            .setMessage("Essence Québec conserve localement le profil, la photo, les favoris, les trajets et les dernières stations consultées.\n\nStations mises en cache : "+n+"\nDernière mise à jour : "+when+"\n\nLe GPS continue de fonctionner sans Internet. Les prix et données Google/MTMD restent ceux de la dernière synchronisation jusqu'au retour du réseau.")
            .setPositiveButton("OK",null)
            .setNeutralButton("VIDER LE CACHE",(d,w)->getSharedPreferences("eq_offline",MODE_PRIVATE).edit().clear().apply()).show();
    }

'''
    if marker not in s:
        raise SystemExit("profile insertion marker missing")
    s=s.replace(marker,methods+marker,1)

required=[
    "MON PROFIL ESSENCE QUÉBEC",
    "MODE HORS LIGNE",
    "showPilotProfile",
    "eqFetchGoogleHours",
    "REQ_PROFILE_PHOTO"
]
missing=[x for x in required if x not in s]
if missing: raise SystemExit("v170 features missing: "+repr(missing))

p.write_text(s,encoding="utf-8")
print("Essence Quebec 1.7.0 patch applied")
