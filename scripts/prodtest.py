#!/usr/bin/env python3
"""
Runs the SHIPPED jars (build/release/) on real servers built by each loader's official launcher or
installer, and drives /slashrails selftest over RCON. Dev runs (scripts/selftest.sh) use Mojang names;
production Fabric runs intermediary names and Forge 1.20.1 SRG names, so only this catches a broken
refmap or a remapping problem.

    python scripts/prodtest.py --all                 # every jar in build/release
    python scripts/prodtest.py 1.21.5-fabric 1.20.1-forge 1.20.1-neoforge
    python scripts/prodtest.py --list
    python scripts/prodtest.py 26.2-fabric --jar path/to/slashrails-<v>+mc26.2-fabric.jar   # an unreleased jar

A jar that nests Polymer (a server-only build) is also checked for server-only mode: the log line,
and Polymer's generated pack holding SlashRails' item model, texture and lang.

Targets are <band>-<loader> as in the jar names, plus "1.20.1-neoforge" (the Forge jar on NeoForge
1.20.1). Servers live in build/prodtest/<target>/ and are reused between runs. Runtimes: JDK 17 for
MC 1.20.1-1.20.4, 21 for 1.20.5-1.21.11, 25 for 26.x (Prism's bundled JDKs, or JAVA17_HOME /
JAVA21_HOME / JAVA25_HOME). Ports 25596 (game) / 25597 (RCON), away from LocalServer and the dev
self-test. The servers run headless without "nogui": the StreamCraft testkit kills any java whose
command line contains it.
"""

from __future__ import annotations

import argparse
import os
import re
import shutil
import socket
import struct
import subprocess
import sys
import time
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RELEASE = ROOT / "build" / "release"
WORK = ROOT / "build" / "prodtest"
PORT, RCON_PORT, RCON_PASSWORD = 25596, 25597, "slashrails"
FABRIC_LOADER = "0.19.5"
FABRIC_INSTALLER = "1.1.0"
FORGE_1201 = "1.20.1-47.4.23"
NEOFORGE_1201 = "1.20.1-47.1.106"
PRISM_JAVA = Path.home() / "AppData/Roaming/PrismLauncher/java"
JAR_OVERRIDE: Path | None = None  # --jar
PACK_ASSETS = ["assets/slashrails/items/track_smoother.json", "assets/slashrails/models/item/track_smoother.json",
               "assets/slashrails/textures/item/track_smoother.png", "assets/slashrails/lang/en_us.json"]
UA = {"User-Agent": "slashdaemon/SlashRails prodtest"}


def props(path: Path) -> dict:
    out = {}
    if path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                k, v = line.split("=", 1)
                out[k.strip()] = v.strip()
    return out


def band_props(band: str, loader: str) -> dict:
    """gradle.properties of the band that built the jar (nested 26.x builds keep theirs one level up)."""
    for p in (ROOT / "versions" / f"{band}-{loader}" / "gradle.properties", ROOT / "versions" / band / "gradle.properties"):
        if p.exists():
            return props(p)
    raise RuntimeError(f"no gradle.properties for {band}-{loader}")


def java_for(mc: str) -> str:
    major = 25 if mc.startswith("26.") else 17 if mc in ("1.20.1", "1.20.2", "1.20.3", "1.20.4") else 21
    home = os.environ.get(f"JAVA{major}_HOME") or str(PRISM_JAVA / {17: "java-runtime-gamma", 21: "java-runtime-delta", 25: "java-runtime-epsilon"}[major])
    return str(Path(home) / "bin" / ("java.exe" if os.name == "nt" else "java"))


def download(url: str, dest: Path) -> Path:
    if not dest.exists():
        print(f"   downloading {dest.name}", flush=True)
        dest.parent.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=300) as r:
            dest.write_bytes(r.read())
    return dest


def jar_for(band: str, loader: str) -> Path:
    if JAR_OVERRIDE is not None:
        return JAR_OVERRIDE
    found = sorted(RELEASE.glob(f"slashrails-*+mc{band}-{loader}.jar"), key=lambda p: p.stat().st_mtime)
    if not found:
        raise RuntimeError(f"no jar for {band}-{loader} in build/release; run ./gradlew buildAll")
    return found[-1]


def targets() -> list[str]:
    out = []
    for jar in sorted(RELEASE.glob("slashrails-*.jar")):
        m = re.match(r"slashrails-[^+]+\+mc(.+)-(fabric|neoforge|forge)\.jar$", jar.name)
        if m:
            out.append(f"{m.group(1)}-{m.group(2)}")
            if m.group(2) == "forge":
                out.append(f"{m.group(1)}-neoforge")  # the same jar on NeoForge 1.20.1
    return out


def install(target: str) -> tuple[Path, list[str]]:
    """Builds (once) the server for a target; returns its dir and launch command."""
    band, loader = target.rsplit("-", 1)
    run = WORK / target
    run.mkdir(parents=True, exist_ok=True)
    java = java_for(band)
    headless = "-Djava.awt.headless=true"
    mods = run / "mods"
    mods.mkdir(exist_ok=True)
    for old in mods.glob("slashrails*.jar"):
        old.unlink()

    if loader == "fabric":
        p = band_props(band, loader)
        launcher = download(f"https://meta.fabricmc.net/v2/versions/loader/{band}/{FABRIC_LOADER}/{FABRIC_INSTALLER}/server/jar",
                            run / "fabric-server-launch.jar")
        api = p["fabric_api_version"]
        for old in mods.glob("fabric-api-*.jar"):
            if old.name != f"fabric-api-{api}.jar":
                old.unlink()
        download(f"https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{api}/fabric-api-{api}.jar",
                 mods / f"fabric-api-{api}.jar")
        shutil.copy2(jar_for(band, loader), mods)
        return run, [java, headless, "-Xmx2G", "-jar", launcher.name]

    if band == "1.20.1":  # Forge, or NeoForge 1.20.1 running the Forge jar
        coord = ("net/minecraftforge/forge", FORGE_1201, "https://maven.minecraftforge.net") if loader == "forge" \
            else ("net/neoforged/forge", NEOFORGE_1201, "https://maven.neoforged.net/releases")
        jar = jar_for(band, "forge")
    else:
        v = band_props(band, loader)["neoforge_version"]
        coord = ("net/neoforged/neoforge", v, "https://maven.neoforged.net/releases")
        jar = jar_for(band, loader)
    path, version, repo = coord
    name = path.rsplit("/", 1)[1]
    args = f"libraries/{path}/{version}/{'win' if os.name == 'nt' else 'unix'}_args.txt"
    if not (run / args).exists():
        installer = download(f"{repo}/{path}/{version}/{name}-{version}-installer.jar", WORK / "installers" / f"{name}-{version}-installer.jar")
        print(f"   installing {name} {version}", flush=True)
        subprocess.run([java, "-jar", str(installer), "--installServer", str(run)], cwd=run, check=True,
                       stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
    shutil.copy2(jar, mods)
    jvm = ["@user_jvm_args.txt"] if (run / "user_jvm_args.txt").exists() else []
    return run, [java, headless, "-Xmx2G", *jvm, f"@{args}"]


def configure(run: Path) -> None:
    (run / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    p = props(run / "server.properties")
    p.update({"level-name": "prodtest-world", "level-type": "minecraft\\:flat", "online-mode": "false",
              "server-port": str(PORT), "enable-rcon": "true", "rcon.port": str(RCON_PORT),
              "rcon.password": RCON_PASSWORD, "spawn-protection": "0", "difficulty": "peaceful",
              "spawn-monsters": "false", "gamemode": "creative", "pause-when-empty-seconds": "0",
              "max-tick-time": "60000", "view-distance": "6", "simulation-distance": "6"})
    (run / "server.properties").write_text("".join(f"{k}={v}\n" for k, v in p.items()), encoding="utf-8")
    shutil.rmtree(run / "prodtest-world", ignore_errors=True)


def rcon(command: str) -> str:
    with socket.create_connection(("127.0.0.1", RCON_PORT), timeout=10) as s:
        def send(i, kind, body):
            data = struct.pack("<ii", i, kind) + body.encode() + b"\0\0"
            s.sendall(struct.pack("<i", len(data)) + data)
            n = struct.unpack("<i", s.recv(4))[0]
            buf = b""
            while len(buf) < n:
                buf += s.recv(n - len(buf))
            return buf[8:-2].decode("utf-8", "replace")
        send(1, 3, RCON_PASSWORD)
        return send(2, 2, command)


FATAL = re.compile(r"Mixin apply .* failed|MixinApplyError|InvalidInjectionException|Incompatible mods|"
                   r"---- Minecraft Crash Report|Exception in server tick|ModLoadingException|"
                   r"Failed to load|requires .* but only", re.I)


def is_server_only(jar: Path) -> bool:
    with zipfile.ZipFile(jar) as z:
        return any(n.startswith("META-INF/jars/polymer-core") for n in z.namelist())


def check_server_only(run: Path, log: str) -> str | None:
    """None if server-only mode came up with a complete pack, else what is wrong."""
    if "clients without the mod may join" not in log:
        return "no server-only log line"
    pack = run / "polymer" / "resource_pack.zip"
    if not pack.exists():
        return "polymer/resource_pack.zip not generated"
    with zipfile.ZipFile(pack) as z:
        missing = [a for a in PACK_ASSETS if a not in z.namelist()]
    return f"pack lacks {', '.join(missing)}" if missing else None


def run_target(target: str) -> str:
    run, cmd = install(target)
    band, loader = target.rsplit("-", 1)
    server_only = loader == "fabric" and is_server_only(jar_for(band, loader))
    shutil.rmtree(run / "polymer", ignore_errors=True)
    if server_only:  # as deployed (TBS): AutoHost serves the required pack; Polymer fills in the other keys
        cfg = run / "config" / "polymer" / "auto-host.json"
        cfg.parent.mkdir(parents=True, exist_ok=True)
        cfg.write_text('{"enabled": true, "required": true, "type": "polymer:automatic"}\n', encoding="utf-8")
    configure(run)
    log_path = run / "prodtest-console.log"
    with open(log_path, "w", encoding="utf-8", errors="replace") as out:
        proc = subprocess.Popen(cmd, cwd=run, stdout=out, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL)
    try:
        deadline = time.time() + 900
        while time.time() < deadline:
            text = log_path.read_text(encoding="utf-8", errors="replace")
            if "RCON running" in text:
                break
            bad = FATAL.search(text)
            if bad or proc.poll() is not None:
                return f"FAILED TO START ({bad.group(0) if bad else f'exit {proc.returncode}'})"
            time.sleep(2)
        else:
            return "FAILED TO START (timeout)"
        rcon("slashrails selftest")
        deadline = time.time() + 1200
        while time.time() < deadline:
            text = log_path.read_text(encoding="utf-8", errors="replace")
            m = re.search(r"SELFTEST (PASSED|FAILED) [0-9/]+", text)
            if m:
                if server_only and m.group(1) == "PASSED":
                    problem = check_server_only(run, text)
                    return f"{m.group(0)}, server-only " + (f"FAILED ({problem})" if problem else "OK")
                return m.group(0)
            bad = FATAL.search(text)
            if bad or proc.poll() is not None:
                return f"CRASHED ({bad.group(0) if bad else f'exit {proc.returncode}'})"
            time.sleep(2)
        return "TIMEOUT"
    finally:
        try:
            rcon("stop")
            proc.wait(timeout=90)
        except Exception:  # noqa: BLE001 - kill it below
            pass
        if proc.poll() is None:
            subprocess.run(["taskkill", "/T", "/F", "/PID", str(proc.pid)] if os.name == "nt" else ["kill", "-9", str(proc.pid)],
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("targets", nargs="*")
    ap.add_argument("--all", action="store_true")
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--jar", type=Path, help="test this jar instead of build/release's (one target only)")
    a = ap.parse_args()
    global JAR_OVERRIDE
    if a.jar:
        if len(a.targets) != 1 or a.all:
            ap.error("--jar needs exactly one named target")
        JAR_OVERRIDE = a.jar.resolve()
    available = targets()
    if a.list:
        print("\n".join(available))
        return 0
    chosen = available if a.all else a.targets
    if not chosen:
        ap.error("name targets or pass --all")
    failed = 0
    for t in chosen:
        print(f"== {t}", flush=True)
        try:
            result = run_target(t)
        except Exception as e:  # noqa: BLE001 - report and continue with the next target
            result = f"ERROR {e}"
        print(f"{t}: {result}", flush=True)
        failed += not result.startswith("SELFTEST PASSED") or "FAILED" in result
    print(f"{len(chosen) - failed}/{len(chosen)} passed")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
