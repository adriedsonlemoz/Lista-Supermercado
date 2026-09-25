from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
workflow_dir = root / ".github/workflows"
release = workflow_dir / "android-release.yml"

if not release.is_file():
    raise SystemExit("Missing .github/workflows/android-release.yml")

other_yml = [p.name for p in workflow_dir.glob("*.yml") if p.name != "android-release.yml"]
other_yaml = [p.name for p in workflow_dir.glob("*.yaml")]
if other_yml or other_yaml:
    raise SystemExit(
        "Only the main Build and Release workflow is allowed. Extra workflows: "
        + ", ".join(other_yml + other_yaml)
    )

text = release.read_text(encoding="utf-8")

required_checks = {
    "main branch trigger": re.search(r'branches:\s*\n\s*-\s*"main"', text) is not None,
    "manual trigger": "workflow_dispatch:" in text,
    "tag trigger": re.search(r'tags:\s*\n\s*-\s*"v\*"', text) is not None,
    "release APK build": "gradle :app:assembleRelease" in text,
    "release APK output": "app/build/outputs/apk/release/app-release.apk" in text,
    "direct GitHub Release publication": "softprops/action-gh-release@v2" in text,
    "versioned APK name": 'Meu-Supermercado-v${VERSION_NAME}.apk' in text,
    "APK signature validation": 'keytool -printcert -jarfile "$OUT"' in text,
    "permanent keystore input": "KEYSTORE_BASE64" in text or "SIGNING_KEY" in text,
}
failed = [name for name, ok in required_checks.items() if not ok]
if failed:
    raise SystemExit("Release workflow rule violation: " + ", ".join(failed))

for forbidden in ("actions/upload-artifact", "source.zip", "flutter build", "flutter pub get"):
    if forbidden.lower() in text.lower():
        raise SystemExit(f"Forbidden workflow content: {forbidden}")

print("Workflow OK: one workflow only, signed Release APK generated and published directly.")
