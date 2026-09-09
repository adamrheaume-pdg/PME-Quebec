import json
import threading
import socket
from pathlib import Path

_tv = None
_lock = threading.RLock()
FIXED_MAC = "e0:3e:cb:ea:88:08"

def discover():
    from vidaa.discovery import discover_all
    devices = discover_all(timeout=2.5)
    out = []
    for ip, d in devices.items():
        out.append({"ip": ip, "name": d.name or "Hisense VIDAA", "model": d.model or "", "mac": d.mac or ""})
    return json.dumps(out)

def _make_client(ip, files_dir):
    from vidaa import VidaaTV
    from vidaa.discovery import probe_ip
    from vidaa.config import TokenStorage
    mac = None
    try:
        d = probe_ip(ip, timeout=2.0)
        if d:
            mac = d.mac
    except Exception:
        pass
    if not mac:
        mac = FIXED_MAC
    storage = TokenStorage(Path(files_dir) / "vidaa_tokens.json")
    kwargs = dict(host=ip, enable_persistence=True, storage=storage)
    kwargs.update(mac_address=mac, use_dynamic_auth=True, brand="his")
    return VidaaTV(**kwargs)

def connect(ip, files_dir):
    global _tv
    with _lock:
        try:
            if _tv:
                try:
                    _tv.disconnect()
                except Exception:
                    pass
            _tv = _make_client(ip.strip(), files_dir)
            ok = _tv.connect(timeout=7.0)
            if not ok:
                return json.dumps({"ok": False, "message": "Connexion refusée. Vérifie l’adresse IP et active le contrôle mobile/réseau sur la télé."})
            if _tv.is_authenticated():
                return json.dumps({"ok": True, "paired": True, "message": "Connecté à la Hisense 50A6KV."})
            started = _tv.start_pairing()
            return json.dumps({"ok": True, "paired": False, "pair_started": bool(started), "message": "Regarde la télé : un PIN devrait apparaître."})
        except Exception as e:
            return json.dumps({"ok": False, "message": str(e)})

def pair(pin):
    global _tv
    with _lock:
        if not _tv:
            return json.dumps({"ok": False, "message": "Connecte d’abord la télé."})
        try:
            ok = _tv.authenticate(str(pin).strip(), timeout=10.0)
            return json.dumps({"ok": bool(ok), "message": "Association réussie." if ok else "PIN refusé ou expiré."})
        except Exception as e:
            return json.dumps({"ok": False, "message": str(e)})

def key(code):
    global _tv
    with _lock:
        if not _tv:
            return False
        try:
            return bool(_tv.send_key(code))
        except Exception:
            return False

def source(name):
    global _tv
    with _lock:
        if not _tv:
            return False
        try:
            return bool(_tv.set_source(name))
        except Exception:
            return False

def wake(mac=FIXED_MAC):
    try:
        clean = mac.replace(":", "").replace("-", "")
        if len(clean) != 12:
            return False
        packet = bytes.fromhex("FF" * 6 + clean * 16)
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        s.sendto(packet, ("255.255.255.255", 9))
        s.close()
        return True
    except Exception:
        return False
