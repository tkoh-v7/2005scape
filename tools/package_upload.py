"""Produce a manual-upload archive and verify its contents using only stdlib."""
import hashlib
import json
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT.parent / "2005scape-0.5.0-website-upload.zip"
DIRECTORIES = ("src", "site", "tools", "third-party", ".github")
FILES = ("index.html", ".nojekyll", "README.md", "LICENSE", "NOTICE.md", "build.gradle", "settings.gradle", "runelite-plugin.properties", ".gitignore")

subprocess.run([sys.executable, str(ROOT / "tools/generate_goals.py")], check=True)
subprocess.run([sys.executable, str(ROOT / "tools/validate_data.py")], check=True)
subprocess.run([sys.executable, str(ROOT / "tools/refresh_audit.py")], check=True)
files = [ROOT / name for name in FILES]
for directory in DIRECTORIES:
    files.extend(p for p in (ROOT / directory).rglob("*") if p.is_file() and "__pycache__" not in p.parts)
files = sorted(files, key=lambda p: str(p.relative_to(ROOT)))
manifest = {str(p.relative_to(ROOT)).replace("\\", "/"): hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
with zipfile.ZipFile(OUTPUT, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    for p in files:
        archive.write(p, "2005scape/" + str(p.relative_to(ROOT)).replace("\\", "/"))
with zipfile.ZipFile(OUTPUT) as archive:
    if archive.testzip() is not None:
        raise ValueError("Corrupt upload archive")
    for name, expected in manifest.items():
        if hashlib.sha256(archive.read("2005scape/" + name)).hexdigest() != expected:
            raise ValueError("Archive content mismatch: " + name)
    cache = (ROOT / ".reference-cache").resolve()
    cache.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="scape2005-verify-", dir=cache) as directory:
        if Path(directory).resolve().parent != cache:
            raise ValueError("Verification directory outside intended cache")
        archive.extractall(directory)
        subprocess.run([sys.executable, str(Path(directory) / "2005scape/tools/validate_data.py")], check=True)
print(f"Verified {len(files)} files: {OUTPUT} ({OUTPUT.stat().st_size:,} bytes)")
print("SHA256: " + hashlib.sha256(OUTPUT.read_bytes()).hexdigest())
