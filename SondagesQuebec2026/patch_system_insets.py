from pathlib import Path

p = Path('app/src/main/java/com/pmequebec/sondages2026/MainActivity.java')
s = p.read_text(encoding='utf-8')

# Import WindowInsets once
if 'import android.view.WindowInsets;' not in s:
    s = s.replace('import android.view.ViewGroup;\n', 'import android.view.ViewGroup;\nimport android.view.WindowInsets;\n')

# Apply safe system insets to the main scroll container.
needle = 'scroll.setBackgroundColor(LIGHT);\n'
insert = '''scroll.setBackgroundColor(LIGHT);\n        scroll.setClipToPadding(false);\n        scroll.setOnApplyWindowInsetsListener((v, insets) -> {\n            int top = insets.getSystemWindowInsetTop();\n            int bottom = insets.getSystemWindowInsetBottom();\n            int left = insets.getSystemWindowInsetLeft();\n            int right = insets.getSystemWindowInsetRight();\n            v.setPadding(left, top, right, bottom);\n            return insets;\n        });\n'''
if needle in s and 'scroll.setOnApplyWindowInsetsListener' not in s:
    s = s.replace(needle, insert, 1)

# Apply safe insets to dialog roots too so WebViews/close buttons never hide under system bars.
needle2 = 'root.setBackgroundColor(Color.WHITE);\n'
insert2 = '''root.setBackgroundColor(Color.WHITE);\n        root.setOnApplyWindowInsetsListener((v, insets) -> {\n            int top = insets.getSystemWindowInsetTop();\n            int bottom = insets.getSystemWindowInsetBottom();\n            int left = insets.getSystemWindowInsetLeft();\n            int right = insets.getSystemWindowInsetRight();\n            v.setPadding(left, top, right, bottom);\n            return insets;\n        });\n'''
if needle2 in s and 'root.setOnApplyWindowInsetsListener' not in s:
    s = s.replace(needle2, insert2, 1)

p.write_text(s, encoding='utf-8')
print('System bar safe-insets patch applied')
