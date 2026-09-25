from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]

required = [
    "VERSION",
    "settings.gradle.kts",
    "build.gradle.kts",
    "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/com/listamercado/app/model/ShoppingList.kt",
    "app/src/main/java/com/listamercado/app/ui/ListDetailActivity.kt",
    "app/src/main/java/com/listamercado/app/ui/SettingsActivity.kt",
    "app/src/main/java/com/listamercado/app/ui/CompareActivity.kt",
    "app_identity.json",
    "github-manager.json",
    "scripts/validate_workflow.py",
    ".github/workflows/android-release.yml",
]
missing = [path for path in required if not (root / path).is_file()]
if missing:
    raise SystemExit("Missing required native Android files: " + ", ".join(missing))

app_gradle = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
root_gradle = (root / "build.gradle.kts").read_text(encoding="utf-8")

checks = [
    ('id("com.android.application")' in app_gradle, "Android application plugin"),
    ('id("org.jetbrains.kotlin.android")' in app_gradle, "Kotlin Android plugin"),
    ("compileSdk = 35" in app_gradle, "compileSdk 35"),
    ("targetSdk = 35" in app_gradle, "targetSdk 35"),
    ("minSdk = 26" in app_gradle, "minSdk 26"),
    ('version "8.7.3"' in root_gradle, "Android Gradle Plugin 8.7.3"),
    ('version "2.0.21"' in root_gradle, "Kotlin 2.0.21"),
]
failed = [label for ok, label in checks if not ok]
if failed:
    raise SystemExit("Native Android source validation failed: " + ", ".join(failed))

# BuildConfig is disabled by default in modern AGP configurations unless explicitly enabled.
kotlin_sources = "\n".join(p.read_text(encoding="utf-8") for p in root.rglob("*.kt"))
if "BuildConfig" in kotlin_sources and not re.search(
    r"buildFeatures\s*\{[^}]*buildConfig\s*=\s*true", app_gradle, re.S
):
    raise SystemExit(
        "Kotlin source references BuildConfig, but app/build.gradle.kts does not enable "
        "android.buildFeatures.buildConfig = true"
    )

# This project is Kotlin/XML. A Flutter workflow was accidentally committed previously.
flutter_markers = ["flutter pub get", "flutter analyze", "flutter test", "flutter build apk"]
workflow_dir = root / ".github/workflows"
for workflow in workflow_dir.glob("*.yml"):
    content = workflow.read_text(encoding="utf-8").lower()
    bad = [marker for marker in flutter_markers if marker in content]
    if bad:
        raise SystemExit(
            f"{workflow.relative_to(root)} contains Flutter commands in a native Kotlin project: "
            + ", ".join(bad)
        )

# Keep source packages clean: APKs are release outputs and must not be committed/zipped.
apks = [p.relative_to(root).as_posix() for p in root.rglob("*.apk")]
if apks:
    raise SystemExit("APK files found inside source tree: " + ", ".join(apks))

# Modular-project guard used by this project.
oversized = []
for kt in root.rglob("*.kt"):
    lines = sum(1 for _ in kt.open(encoding="utf-8"))
    if lines > 500:
        oversized.append(f"{kt.relative_to(root)} ({lines} lines)")
if oversized:
    raise SystemExit("Kotlin files over 500 lines: " + ", ".join(oversized))

print("Native Android source OK: Kotlin/XML project, BuildConfig valid, release workflow present, no Flutter commands, no APK in source.")
