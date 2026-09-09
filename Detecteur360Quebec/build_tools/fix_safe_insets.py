from pathlib import Path

p = Path('Detecteur360Quebec/app/src/main/java/quebec/detecteur360/MainActivity.java')
s = p.read_text(encoding='utf-8')
old = '''        ScrollView scroll = new ScrollView(this);\n        scroll.setBackgroundColor(Color.rgb(244, 247, 255));\n        content = new LinearLayout(this);'''
new = '''        ScrollView scroll = new ScrollView(this);\n        scroll.setBackgroundColor(Color.rgb(244, 247, 255));\n        scroll.setClipToPadding(false);\n        scroll.setOnApplyWindowInsetsListener((v, insets) -> {\n            int top = insets.getSystemWindowInsetTop();\n            int bottom = insets.getSystemWindowInsetBottom();\n            v.setPadding(0, top, 0, bottom);\n            return insets;\n        });\n        content = new LinearLayout(this);'''
if old not in s:
    raise SystemExit('Target block not found; source layout changed')
s = s.replace(old, new, 1)
p.write_text(s, encoding='utf-8')
print('Safe system-bar insets applied')
