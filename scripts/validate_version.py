import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
version = (root / "VERSION").read_text().strip()
name, code = version.split("+")
app_gradle = (root / "app/build.gradle.kts").read_text()
identity = json.loads((root / "app_identity.json").read_text())
github = json.loads((root / "github-manager.json").read_text())

checks = [
    (f'versionName = "{name}"' in app_gradle, "app versionName"),
    (f'versionCode = {code}' in app_gradle, "app versionCode"),
    (identity["versionName"] == name and identity["versionCode"] == int(code), "app_identity"),
    (github["version"] == version, "github-manager version"),
    (github["android"]["versionName"] == name and github["android"]["versionCode"] == int(code), "github-manager android version"),
]
failed = [label for ok, label in checks if not ok]
if failed:
    raise SystemExit("Version mismatch: " + ", ".join(failed))
print(f"Version metadata OK: {version}")
