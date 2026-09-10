from pathlib import Path
import os
root=Path(os.environ.get('GRADLE_DIR','source-5.10.4/PME-Quebec-5.10-Native-Wyze-Source'))
p=root/'app/src/main/java/com/pmequebec/app/MainActivity.java'
s=p.read_text(encoding='utf-8')
if 'PME_PERMISSION_FLOW_STABLE_7100' in s:
    print('Flux permissions stable déjà présent')
    raise SystemExit(0)

# 1) Ne jamais ouvrir les boîtes de permissions avant que le WebView soit chargé.
old='''        setContentView(webView);\n        requestInitialPermissions();\n        startGo2RtcIfBundled();\n        webView.loadUrl("file:///android_asset/index.html");'''
new='''        setContentView(webView);\n        startGo2RtcIfBundled();\n        webView.loadUrl("file:///android_asset/index.html");\n        // PME_PERMISSION_FLOW_STABLE_7100\n        // Laisser Android rendre la page et lui donner le focus avant les dialogues système.\n        webView.postDelayed(() -> requestInitialPermissions(), 900);'''
if old not in s:
    raise SystemExit('Bloc de démarrage permissions attendu introuvable')
s=s.replace(old,new,1)

# 2) Empêcher deux demandes système concurrentes.
anchor='''    private PermissionRequest pendingCameraRequest;\n    private GeolocationPermissions.Callback pendingGeoCallback;'''
repl='''    private PermissionRequest pendingCameraRequest;\n    private boolean initialPermissionRequestInFlight = false;\n    private GeolocationPermissions.Callback pendingGeoCallback;'''
if anchor not in s:
    raise SystemExit('Champs permissions attendus introuvables')
s=s.replace(anchor,repl,1)

old='''    private void requestInitialPermissions() {\n        if (android.os.Build.VERSION.SDK_INT < 23) return;\n        java.util.ArrayList<String> needed = new java.util.ArrayList<>();'''
new='''    private void requestInitialPermissions() {\n        if (android.os.Build.VERSION.SDK_INT < 23 || initialPermissionRequestInFlight) return;\n        java.util.ArrayList<String> needed = new java.util.ArrayList<>();'''
if old not in s:
    raise SystemExit('requestInitialPermissions attendu introuvable')
s=s.replace(old,new,1)

old='''        if (!needed.isEmpty()) requestPermissions(needed.toArray(new String[0]), REQ_INITIAL);\n    }'''
new='''        if (!needed.isEmpty()) {\n            initialPermissionRequestInFlight = true;\n            requestPermissions(needed.toArray(new String[0]), REQ_INITIAL);\n        }\n    }'''
if old not in s:
    raise SystemExit('Fin requestInitialPermissions attendue introuvable')
s=s.replace(old,new,1)

# 3) Toujours rendre le focus au WebView à la fermeture du dialogue de permissions.
needle='''        if (requestCode == REQ_LOCATION) {\n            boolean ok = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;\n            GeolocationPermissions.Callback cb = pendingGeoCallback; String origin = pendingGeoOrigin; pendingGeoCallback = null; pendingGeoOrigin = null;\n            if (cb != null) cb.invoke(origin, ok, false);\n            return;\n        }\n    }'''
replacement='''        if (requestCode == REQ_LOCATION) {\n            boolean ok = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;\n            GeolocationPermissions.Callback cb = pendingGeoCallback; String origin = pendingGeoOrigin; pendingGeoCallback = null; pendingGeoOrigin = null;\n            if (cb != null) cb.invoke(origin, ok, false);\n            resumeWebViewAfterPermissionFlow();\n            return;\n        }\n        if (requestCode == REQ_INITIAL) {\n            initialPermissionRequestInFlight = false;\n            resumeWebViewAfterPermissionFlow();\n            return;\n        }\n    }\n\n    private void resumeWebViewAfterPermissionFlow() {\n        if (webView == null) return;\n        webView.postDelayed(() -> {\n            try {\n                webView.onResume();\n                webView.resumeTimers();\n                webView.setEnabled(true);\n                webView.setFocusable(true);\n                webView.setFocusableInTouchMode(true);\n                webView.requestFocus(View.FOCUS_DOWN);\n                webView.evaluateJavascript("if(window.PMENetworkScan){void(0)};document.body&&document.body.classList.remove('permission-dialog-open');", null);\n            } catch (Exception ignored) {}\n        }, 180);\n    }\n\n    @Override protected void onResume() {\n        super.onResume();\n        if (webView != null) {\n            try { webView.onResume(); webView.resumeTimers(); webView.requestFocus(View.FOCUS_DOWN); } catch (Exception ignored) {}\n        }\n    }'''
if needle not in s:
    raise SystemExit('onRequestPermissionsResult attendu introuvable')
s=s.replace(needle,replacement,1)

# 4) Le refus d'une permission ne doit pas bloquer l'application. L'utilisateur peut relancer depuis Sécurité.
p.write_text(s,encoding='utf-8')
print('Gel après permissions corrigé : chargement avant demandes, garde anti-concurrence et reprise WebView')
