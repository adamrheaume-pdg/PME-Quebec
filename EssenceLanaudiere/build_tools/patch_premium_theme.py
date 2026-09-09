from pathlib import Path

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

# Palette bleu nuit / bleu électrique inspirée de la référence, sans intégrer l'image de référence.
s=s.replace('private final int navy=Color.rgb(57,82,118), navyDark=Color.rgb(30,45,67), ink=Color.rgb(45,52,67), accent=Color.rgb(30,125,240), pale=Color.rgb(246,248,251);',
'''private final int navy=Color.rgb(3,18,46), navyDark=Color.rgb(6,30,67), ink=Color.rgb(34,46,68), accent=Color.rgb(0,145,255), pale=Color.rgb(246,249,255);''')

# Fond principal plus riche.
s=s.replace('sc.setBackgroundColor(navy);', 'sc.setBackground(blueBackdrop());')
s=s.replace('page.setBackgroundColor(navy);', 'page.setBackground(blueBackdrop());')

# Branding plus premium.
s=s.replace('TextView name=text("ESSENCE QUÉBEC",24,true,Color.WHITE); TextView tag=text("Paye ton gaz moins cher",14,false,Color.rgb(205,220,240));',
'''TextView name=text("ESSENCE QUÉBEC",27,true,Color.WHITE); name.setShadowLayer(dp(5),0,0,Color.rgb(0,140,255));
        TextView tag=text("Paye ton gaz moins cher",14,false,Color.rgb(190,220,255));''')

# Ajoute la signature visuelle sous l'entête.
needle='page.addView(header);\n        TextView title=text("Compare les\\nprix à proximité",38,true,Color.WHITE);'
if 'SIMPLE  •  PUISSANTE  •  QUÉBÉCOISE' not in s and needle in s:
    s=s.replace(needle, '''page.addView(header);
        TextView premium=text("SIMPLE  •  PUISSANTE  •  QUÉBÉCOISE",11,true,Color.rgb(105,205,255));
        premium.setGravity(Gravity.CENTER); premium.setLetterSpacing(0.08f); premium.setPadding(0,dp(4),0,dp(8)); page.addView(premium);
        TextView title=text("Compare les\\nprix à proximité",38,true,Color.WHITE);''',1)

s=s.replace('title.setPadding(0,dp(12),0,dp(18)); page.addView(title);',
'''title.setPadding(0,dp(12),0,dp(18)); title.setShadowLayer(dp(5),0,dp(2),Color.rgb(0,90,180)); page.addView(title);''',1)

# Recherche et état visuel.
s=s.replace('hero.setBackground(round(navyDark,10));', 'hero.setBackground(neonDarkPanel());')

# Remplace les helpers visuels pour un aspect plus profond et lumineux.
s=s.replace('private LinearLayout cardWhite(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setBackground(round(Color.rgb(252,252,253),12));l.setElevation(dp(3));return l;}',
'''private LinearLayout cardWhite(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setBackground(premiumCard());l.setElevation(dp(7));return l;}''')
s=s.replace('private LinearLayout cardDark(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(14),dp(14),dp(14),dp(14));l.setBackground(round(ink,10));return l;}',
'''private LinearLayout cardDark(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(14),dp(14),dp(14),dp(14));l.setBackground(neonDarkPanel());l.setElevation(dp(6));return l;}''')
s=s.replace('private Button primaryButton(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(round(accent,10));return b;}',
'''private Button primaryButton(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(neonButton());b.setElevation(dp(6));return b;}''')
s=s.replace('private Button ghostButton(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(round(navyDark,10));return b;}',
'''private Button ghostButton(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(neonDarkPanel());b.setElevation(dp(4));return b;}''')

# Helpers de dessin natifs: aucun asset image ajouté.
marker='    private GradientDrawable round(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}'
if 'private GradientDrawable premiumCard()' not in s and marker in s:
    repl='''    private GradientDrawable premiumCard(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{Color.rgb(255,255,255),Color.rgb(239,246,255)});g.setCornerRadius(dp(16));g.setStroke(dp(1),Color.rgb(70,165,255));return g;}
    private GradientDrawable neonDarkPanel(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.rgb(7,28,62),Color.rgb(10,52,105)});g.setCornerRadius(dp(13));g.setStroke(dp(1),Color.rgb(0,155,255));return g;}
    private GradientDrawable neonButton(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(0,100,230),Color.rgb(0,180,255)});g.setCornerRadius(dp(12));g.setStroke(dp(1),Color.rgb(120,220,255));return g;}
    private GradientDrawable blueBackdrop(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(2,13,35),Color.rgb(5,35,78),Color.rgb(3,18,46)});return g;}
'''+marker
    s=s.replace(marker,repl,1)

required=['SIMPLE  •  PUISSANTE  •  QUÉBÉCOISE','premiumCard()','neonDarkPanel()','neonButton()','blueBackdrop()']
missing=[x for x in required if x not in s]
if missing: raise SystemExit('Theme premium incomplet: '+repr(missing))

p.write_text(s,encoding='utf-8')
