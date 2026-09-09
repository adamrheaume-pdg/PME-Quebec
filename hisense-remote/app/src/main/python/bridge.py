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


def _port_open(ip, port, timeout=1.5):
    try:
        s = socket.create_connection((ip, port), timeout=timeout)
        s.close()
        return True
    except Exception:
        return False


def _probe_mac(ip):
    try:
        from vidaa.discovery import probe_ip
        d = probe_ip(ip, timeout=2.0)
        if d and d.mac:
            return d.mac
    except Exception:
        pass
    return FIXED_MAC


def _storage(files_dir):
    from vidaa.config import TokenStorage
    return TokenStorage(Path(files_dir) / "vidaa_tokens.json")


def _new_dynamic(ip, files_dir, mac, method):
    from vidaa import VidaaTV
    return VidaaTV(
        host=ip,
        port=36669,
        enable_persistence=True,
        storage=_storage(files_dir),
        mac_address=mac,
        use_dynamic_auth=True,
        brand="his",
        auth_method=method,
        auto_detect_protocol=False,
    )


def _new_static(ip, files_dir):
    from vidaa import VidaaTV
    return VidaaTV(
        host=ip,
        port=36669,
        enable_persistence=True,
        storage=_storage(files_dir),
        use_dynamic_auth=False,
        auto_detect_protocol=False,
    )


def _disconnect_current():
    global _tv
    if _tv:
        try:
            _tv.disconnect()
        except Exception:
            pass
    _tv = None


def connect(ip, files_dir):
    global _tv
    host = ip.strip()
    with _lock:
        try:
            _disconnect_current()

            if not _port_open(host, 36669, 2.0):
                upnp = _port_open(host, 38400, 1.5)
                detail = "La télé répond sur le réseau, mais le service de télécommande VIDAA (36669) est fermé." if upnp else "La télé ne répond pas aux services VIDAA sur le réseau local."
                return json.dumps({"ok": False, "message": detail})

            from vidaa.protocol import AuthMethod
            mac = _probe_mac(host)
            # Le calcul VIDAA est sensible à la casse de l'UUID/MAC. On essaie les deux formes.
            mac_variants = []
            for m in (mac, mac.lower(), mac.upper()):
                if m not in mac_variants:
                    mac_variants.append(m)

            attempts = []
            for m in mac_variants:
                attempts.extend([
                    (f"moderne/{m}", lambda mm=m: _new_dynamic(host, files_dir, mm, AuthMethod.MODERN)),
                    (f"intermédiaire/{m}", lambda mm=m: _new_dynamic(host, files_dir, mm, AuthMethod.MIDDLE)),
                    (f"ancien/{m}", lambda mm=m: _new_dynamic(host, files_dir, mm, AuthMethod.LEGACY)),
                ])
            attempts.append(("statique", lambda: _new_static(host, files_dir)))

            tried = []
            for label, maker in attempts:
                client = None
                try:
                    client = maker()
                    ok = client.connect(timeout=4.0, try_fallback=False)
                    tried.append(label)
                    if ok:
                        _tv = client
                        if _tv.is_authenticated():
                            return json.dumps({"ok": True, "paired": True, "method": label, "message": "Connecté à la Hisense 50A6KV."})
                        started = _tv.start_pairing()
                        return json.dumps({"ok": True, "paired": False, "pair_started": bool(started), "method": label, "message": "Connexion VIDAA établie. Regarde la télé : un PIN devrait apparaître."})
                except Exception:
                    tried.append(label)
                finally:
                    if client is not None and client is not _tv:
                        try:
                            client.disconnect()
                        except Exception:
                            pass

            return json.dumps({
                "ok": False,
                "message": "La télé répond sur le port VIDAA, mais refuse toutes les méthodes d’authentification testées. Ouvre Paramètres VIDAA et autorise le contrôle par appareil mobile, puis réessaie.",
                "attempts": tried,
            })
        except Exception as e:
            return json.dumps({"ok": False, "message": "Erreur VIDAA : " + str(e)})


def pair(pin):
    global _tv
    with _lock:
        if not _tv:
            return json.dumps({"ok": False, "message": "Connecte d’abord la télé."})
        try:
            ok = _tv.authenticate(str(pin).strip(), timeout=12.0)
            return json.dumps({"ok": bool(ok), "message": "Association réussie." if ok else "PIN refusé ou expiré. Relance Connexion puis réessaie avec le nouveau PIN."})
        except Exception as e:
            return json.dumps({"ok": False, "message": "Erreur d’association : " + str(e)})


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
        for port in (9, 7):
            s.sendto(packet, ("255.255.255.255", port))
            s.sendto(packet, ("192.168.2.255", port))
        s.close()
        return True
    except Exception:
        return False
