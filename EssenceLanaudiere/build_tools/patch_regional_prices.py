from pathlib import Path

p = Path("EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java")
s = p.read_text(encoding="utf-8")

needle = 'mapCard.addView(stationMap,new LinearLayout.LayoutParams(-1,dp(330)));'
if 'PRIX MOYENS AUTOUR DU QUÉBEC' not in s:
    repl = '''mapCard.addView(stationMap,new LinearLayout.LayoutParams(-1,dp(330)));
        TextView regionalTitle=text("PRIX MOYENS AUTOUR DU QUÉBEC",11,true,Color.rgb(80,88,102));
        regionalTitle.setGravity(Gravity.CENTER);
        regionalTitle.setPadding(dp(6),dp(7),dp(6),0);
        mapCard.addView(regionalTitle,new LinearLayout.LayoutParams(-1,-2));
        TextView regionalPrices=text("Ontario ≈ 182,0 ¢/L   •   Vermont ≈ 4,30 $US/gal   •   N.-B. ≈ 190,3 ¢/L",11,true,ink);
        regionalPrices.setGravity(Gravity.CENTER);
        regionalPrices.setPadding(dp(6),dp(3),dp(6),dp(2));
        mapCard.addView(regionalPrices,new LinearLayout.LayoutParams(-1,-2));
        TextView regionalSource=text("Repères récents • essence ordinaire • CAA / AAA / données régionales",9,false,Color.GRAY);
        regionalSource.setGravity(Gravity.CENTER);
        regionalSource.setPadding(dp(6),0,dp(6),dp(6));
        mapCard.addView(regionalSource,new LinearLayout.LayoutParams(-1,-2));'''
    if needle not in s:
        raise SystemExit("Point insertion prix régionaux introuvable")
    s = s.replace(needle, repl, 1)

required = [
    'PRIX MOYENS AUTOUR DU QUÉBEC',
    'Ontario ≈ 182,0 ¢/L',
    'Vermont ≈ 4,30 $US/gal',
    'N.-B. ≈ 190,3 ¢/L'
]
missing=[x for x in required if x not in s]
if missing:
    raise SystemExit("Prix régionaux manquants: "+repr(missing))

p.write_text(s, encoding="utf-8")
