from pathlib import Path

p = Path('app/src/main/java/com/pmequebec/sondages2026/MainActivity.java')
s = p.read_text(encoding='utf-8')

marker = '        setContentView(scroll);\n'
footer = '''        TextView footer = text("Créé AdamMimi (TikTok)", 12, Color.GRAY, true);\n        footer.setGravity(Gravity.CENTER);\n        footer.setPadding(0, dp(24), 0, dp(18));\n        content.addView(footer, matchWrap());\n\n        setContentView(scroll);\n'''

if 'Créé AdamMimi (TikTok)' not in s and marker in s:
    s = s.replace(marker, footer, 1)

p.write_text(s, encoding='utf-8')
print('Footer AdamMimi added')
