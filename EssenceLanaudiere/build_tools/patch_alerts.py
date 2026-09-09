from pathlib import Path

p = Path("EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java")
s = p.read_text(encoding="utf-8")

# Corrige l'affichage d'adresse pour ne pas répéter la ville deux fois.
s = s.replace('TextView addr=text(x.address+"\\n"+x.city,14,false,ink);',
              'TextView addr=text(displayStationAddress(x),14,false,ink);')

# Ajoute le calcul du prix le plus élevé de la zone et l'historique local des prix.
needle = 'Station cheap=Collections.min(s,Comparator.comparingDouble(x->x.price)); hero.setText(String.format(Locale.CANADA_FRENCH,"🏆 %.1f ¢/L • %s • %.1f km",cheap.price,cheap.name,cheap.distance)); notifyBest(cheap,fuel);'
replacement = '''Station cheap=Collections.min(s,Comparator.comparingDouble(x->x.price));
        Station expensive=Collections.max(s,Comparator.comparingDouble(x->x.price));
        hero.setText(String.format(Locale.CANADA_FRENCH,"🏆 %.1f ¢/L • %s • %.1f km",cheap.price,cheap.name,cheap.distance)); notifyBest(cheap,fuel);'''
if needle in s:
    s = s.replace(needle, replacement, 1)

# Transforme chaque carte station en alerte visuelle selon son statut.
needle2 = 'int rank=1; for(Station x:s){LinearLayout c=cardStation(); c.setOnClickListener(v->openMap(x));'
replacement2 = '''int rank=1; for(Station x:s){
            LinearLayout c=cardStation(); c.setOnClickListener(v->openMap(x));
            Double previous=previousStationPrice(x);
            boolean dropped=previous!=null && x.price < previous-0.049;
            boolean isCheapest=Math.abs(x.price-cheap.price)<0.05;
            boolean isHighest=s.size()>1 && expensive.price>cheap.price+0.049 && Math.abs(x.price-expensive.price)<0.05;
            if(isHighest){addPriceAlert(c,"⚠ PRIX LE PLUS ÉLEVÉ DANS LA ZONE",Color.rgb(180,30,40));startStationBlink(c,Color.rgb(255,205,210),Color.rgb(190,220,255));}
            else if(isCheapest){addPriceAlert(c,"✓ PRIX LE MOINS CHER DANS LA ZONE",Color.rgb(0,125,55));startStationBlink(c,Color.rgb(185,246,202),Color.WHITE);}
            else if(dropped){addPriceAlert(c,"↓ BAISSE DE PRIX",Color.rgb(125,95,0));startStationBlink(c,Color.rgb(255,241,118),Color.WHITE);}'''
if needle2 in s:
    s = s.replace(needle2, replacement2, 1)

# Mémorise le prix courant après construction de chaque carte.
needle3 = 'results.addView(c); rank++; }'
replacement3 = 'results.addView(c); rememberStationPrice(x); rank++; }'
if needle3 in s:
    s = s.replace(needle3, replacement3, 1)

methods = r'''
    private String stationPriceKey(Station s){
        String raw=(s.name+"|"+s.address+"|"+s.city).toLowerCase(Locale.CANADA_FRENCH).trim();
        return "price_"+Integer.toHexString(raw.hashCode());
    }

    private Double previousStationPrice(Station s){
        SharedPreferences p=getSharedPreferences("price_history",MODE_PRIVATE);
        String k=stationPriceKey(s);
        if(!p.contains(k))return null;
        return Double.longBitsToDouble(p.getLong(k,Double.doubleToLongBits(Double.NaN)));
    }

    private void rememberStationPrice(Station s){
        getSharedPreferences("price_history",MODE_PRIVATE).edit().putLong(stationPriceKey(s),Double.doubleToLongBits(s.price)).apply();
    }

    private void addPriceAlert(LinearLayout card,String label,int color){
        TextView a=text(label,11,true,color);
        a.setGravity(Gravity.CENTER);
        a.setPadding(dp(8),dp(5),dp(8),dp(5));
        a.setBackground(round(Color.argb(28,Color.red(color),Color.green(color),Color.blue(color)),8));
        card.addView(a,0,new LinearLayout.LayoutParams(-1,-2));
    }

    private void startStationBlink(View v,int first,int second){
        Handler h=new Handler(Looper.getMainLooper());
        boolean[] flip={false};
        Runnable[] r=new Runnable[1];
        r[0]=()->{
            if(!v.isAttachedToWindow())return;
            v.setBackground(round(flip[0]?first:second,12));
            flip[0]=!flip[0];
            h.postDelayed(r[0],650);
        };
        v.post(r[0]);
    }

    private String displayStationAddress(Station s){
        String a=s.address==null?"":s.address.trim();
        String city=s.city==null?"":s.city.trim();
        if(city.isEmpty())return a;
        String al=a.toLowerCase(Locale.CANADA_FRENCH);
        String cl=city.toLowerCase(Locale.CANADA_FRENCH);
        if(al.endsWith(cl)){
            a=a.substring(0,Math.max(0,a.length()-city.length())).trim();
            while(a.endsWith(",")||a.endsWith("-")||a.endsWith(";"))a=a.substring(0,a.length()-1).trim();
        }
        if(a.isEmpty())return city;
        return a+"\n"+city;
    }

'''
marker = '    private void showTrendDialog(){'
if 'private String stationPriceKey(Station s)' not in s:
    if marker not in s:
        raise SystemExit("Point insertion alertes introuvable")
    s = s.replace(marker, methods + marker, 1)

required = [
    'previousStationPrice(x)',
    'PRIX LE MOINS CHER DANS LA ZONE',
    'PRIX LE PLUS ÉLEVÉ DANS LA ZONE',
    'BAISSE DE PRIX',
    'startStationBlink',
    'displayStationAddress(x)',
    'rememberStationPrice(x)'
]
missing=[x for x in required if x not in s]
if missing:
    raise SystemExit("Alertes manquantes: "+repr(missing))

p.write_text(s,encoding="utf-8")
