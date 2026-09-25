from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
workflow_dir = root / ".github/workflows"
release = workflow_dir / "android-release.yml"

if not release.is_file():
    raise SystemExit("Missing .github/workflows/android-release.yml")

# Delivery source must contain a single workflow.
extra = [
    p.name for p in workflow_dir.iterdir()
    if p.is_file() and p.name != "android-release.yml" and p.suffix in {".yml", ".yaml"}
]
if extra:
    raise SystemExit("Extra workflow files remain in source: " + ", ".join(extra))

text = release.read_text(encoding="utf-8")

required = {
    "legacy cleanup": 'LEGACY=".github/workflows/android.yml"' in text and "git push origin HEAD:main" in text,
    "main trigger": re.search(r'branches:\s*\n\s*-\s*"main"', text) is not None,
    "manual trigger": "workflow_dispatch:" in text,
    "tag trigger": re.search(r'tags:\s*\n\s*-\s*"v\*"', text) is not None,
    "release build": "gradle :app:assembleRelease" in text,
    "APK output": "app/build/outputs/apk/release/app-release.apk" in text,
    "direct release publication": "softprops/action-gh-release@v2" in text,
    "signature validation": 'keytool -printcert -jarfile "$OUT"' in text,
}
failed = [name for name, ok in required.items() if not ok]
if failed:
    raise SystemExit("Release workflow rule violation: " + ", ".join(failed))

for forbidden in ("actions/upload-artifact", "source.zip", "flutter build", "flutter pub get"):
    if forbidden.lower() in text.lower():
        raise SystemExit(f"Forbidden workflow content: {forbidden}")

print("Workflow OK: legacy CI self-cleanup enabled; signed Release APK built and published directly.")
