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
    "app/src/main/java/com/listamercado/app/ui/PurchaseModeActivity.kt",
    "app/src/main/java/com/listamercado/app/ui/PurchaseModeAdapter.kt",
    "app/src/main/res/layout/activity_purchase_mode.xml",
    "app/src/main/res/layout/item_purchase_mode.xml",
    "app/src/main/java/com/listamercado/app/ui/SettingsActivity.kt",
    "app/src/main/java/com/listamercado/app/ui/CompareActivity.kt",
    "app/src/main/java/com/listamercado/app/ui/NearbyMarketsActivity.kt",
    "app/src/main/java/com/listamercado/app/ui/WhatsNewActivity.kt",
    "app/src/main/java/com/listamercado/app/ui/WhatsNewContent.kt",
    "app/src/main/java/com/listamercado/app/ui/BudgetDialog.kt",
    "app/src/main/java/com/listamercado/app/util/PriceUnitHelper.kt",
    "app/src/main/res/layout/dialog_budget.xml",
    "app/src/main/assets/nearby_markets_map.html",
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

manifest = (root / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
for permission in (
    "android.permission.INTERNET",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.ACCESS_FINE_LOCATION",
):
    if permission not in manifest:
        raise SystemExit(f"Missing map permission in AndroidManifest.xml: {permission}")

map_source = (root / "app/src/main/java/com/listamercado/app/ui/NearbyMarketsActivity.kt").read_text(encoding="utf-8")
if "overpass-api.de" not in map_source or 'shop"="supermarket' not in map_source:
    raise SystemExit("Nearby supermarket map/Overpass query is missing or incomplete")

settings_source = (root / "app/src/main/java/com/listamercado/app/data/SettingsRepository.kt").read_text(encoding="utf-8")
main_source = (root / "app/src/main/java/com/listamercado/app/ui/MainActivity.kt").read_text(encoding="utf-8")
if "lastSeenWhatsNewVersionCode" not in settings_source or "WhatsNewActivity" not in main_source:
    raise SystemExit("Per-version What's New flow is missing")

whats_activity = (root / "app/src/main/java/com/listamercado/app/ui/WhatsNewActivity.kt").read_text(encoding="utf-8")
if "savedInstanceState == null" not in main_source:
    raise SystemExit("What's New launch must be guarded against MainActivity recreation")
if "FLAG_ACTIVITY_SINGLE_TOP" not in main_source or "FLAG_ACTIVITY_CLEAR_TOP" not in main_source:
    raise SystemExit("What's New launch flags for duplicate-instance prevention are missing")
if "markWhatsNewSeen(BuildConfig.VERSION_CODE)" not in whats_activity or "acknowledgeAndClose" not in whats_activity:
    raise SystemExit("What's New must be marked seen only when the user acknowledges it")
if ".commit()" not in settings_source:
    raise SystemExit("What's New acknowledgement must be synchronously persisted with commit()")
if 'android:launchMode="singleTop"' not in manifest:
    raise SystemExit("WhatsNewActivity must use singleTop to prevent duplicate instances")

current_version = (root / "VERSION").read_text(encoding="utf-8").strip()
changelog = (root / "CHANGELOG.md").read_text(encoding="utf-8")
if f"## {current_version}" not in changelog:
    raise SystemExit(f"CHANGELOG.md does not contain the current version {current_version}")

whats_new_source = (root / "app/src/main/java/com/listamercado/app/ui/WhatsNewContent.kt").read_text(encoding="utf-8")
current_code = int(current_version.split("+")[1])
match = re.search(r"CONTENT_VERSION_CODE\s*=\s*(\d+)", whats_new_source)
if not match or int(match.group(1)) != current_code:
    raise SystemExit(
        f"WhatsNewContent.kt must be updated for versionCode {current_code}; "
        "set CONTENT_VERSION_CODE to the current version and refresh the change list"
    )


list_model = (root / "app/src/main/java/com/listamercado/app/model/ShoppingList.kt").read_text(encoding="utf-8")
repo_source = (root / "app/src/main/java/com/listamercado/app/data/ShoppingRepository.kt").read_text(encoding="utf-8")
compare_source = (root / "app/src/main/java/com/listamercado/app/ui/CompareActivity.kt").read_text(encoding="utf-8")
if "var budget: Double" not in list_model or 'put("budget", budget)' not in repo_source:
    raise SystemExit("Per-list budget feature is missing or not persisted")
if "buildPriceInsight" not in compare_source or "Média:" not in compare_source or "Histórico de preço" not in kotlin_sources:
    raise SystemExit("Smart product price history is missing")

colors = (root / "app/src/main/res/values/colors.xml").read_text(encoding="utf-8")
night_colors = (root / "app/src/main/res/values-night/colors.xml").read_text(encoding="utf-8")
if "#2E6B47" in colors or "#2E6B47" in night_colors or 'color name="on_primary"' not in colors:
    raise SystemExit("Updated violet theme / explicit on-primary contrast is missing")

if "#101820" not in night_colors or "#121B24" not in night_colors:
    raise SystemExit("Vigia-style graphite dark surfaces are missing")

# Android resources in qualified folders (such as values-night) must have default declarations.
# Missing defaults fail lintVitalRelease and can crash when queried in another configuration.
base_color_names = set(re.findall(r'<color\s+name="([^"]+)"', colors))
night_color_names = set(re.findall(r'<color\s+name="([^"]+)"', night_colors))
missing_default_colors = sorted(night_color_names - base_color_names)
if missing_default_colors:
    raise SystemExit(
        "values-night/colors.xml contains colors without base declarations in values/colors.xml: "
        + ", ".join(missing_default_colors)
    )

if "KEY_CICLOVIAGEM_PRICES_V1" not in repo_source or '"arroz branco" to ("kg" to 18.0 / 5.0)' not in repo_source:
    raise SystemExit("Cicloviagem reference-price migration is missing")

price_helper = (root / "app/src/main/java/com/listamercado/app/util/PriceUnitHelper.kt").read_text(encoding="utf-8")
if '"g", "mL" -> internalPrice * 1000.0' not in price_helper:
    raise SystemExit("Readable kg/L price normalization is missing")


purchase_mode_source = (root / "app/src/main/java/com/listamercado/app/ui/PurchaseModeActivity.kt").read_text(encoding="utf-8")
purchase_adapter_source = (root / "app/src/main/java/com/listamercado/app/ui/PurchaseModeAdapter.kt").read_text(encoding="utf-8")
detail_source = (root / "app/src/main/java/com/listamercado/app/ui/ListDetailActivity.kt").read_text(encoding="utf-8")
if "PurchaseModeActivity" not in manifest or "buttonPurchaseMode" not in detail_source:
    raise SystemExit("Purchase mode entry point is missing")
if "filter { !it.purchased }" not in purchase_mode_source or 'setAction("Desfazer")' not in purchase_mode_source:
    raise SystemExit("Purchase mode must focus pending items and support undo")
if "totalPrice / item.quantity" not in purchase_mode_source or "inputQuickPrice" not in purchase_adapter_source:
    raise SystemExit("Quick total-price editing is missing or does not preserve internal unit-price semantics")

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
