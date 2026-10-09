"""Check the artifact is a Java 11 plugin, with current facts and no client."""
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
archives = list((ROOT / "build/libs").glob("2005scape-*.jar"))
if len(archives) != 1:
    raise ValueError("Clean-build exactly one plugin JAR before verification")
with zipfile.ZipFile(archives[0]) as archive:
    if archive.testzip() is not None:
        raise ValueError("Plugin JAR CRC mismatch")
    classes = [name for name in archive.namelist() if name.endswith(".class")]
    if not classes or any(not name.startswith("com/tkoh/scape2005/") or name.endswith("DevelopmentLauncher.class") for name in classes):
        raise ValueError("JAR contains a client, dependency or development launcher")
    if any(int.from_bytes(archive.read(name)[6:8], "big") != 55 for name in classes):
        raise ValueError("Plugin must target Java 11")
    resources = ROOT / "src/main/resources"
    for file in resources.rglob("*"):
        if file.is_file() and archive.read(file.relative_to(resources).as_posix()) != file.read_bytes():
            raise ValueError("Stale bundled resource: " + file.name)
    print(f"PASS: plugin-only JAR; {len(classes)} Java 11 classes; all resources and licenses match. No RuneLite client or developer launcher bundled.")
