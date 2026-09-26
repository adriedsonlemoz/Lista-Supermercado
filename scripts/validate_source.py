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
    "app/src/main/java/com/listamercado/app/model/CatalogProduct.kt",
    "app/src/main/java/com/listamercado/app/model/BarcodeLookupResult.kt",
    "app/src/main/java/com/listamercado/app/model/ListTemplate.kt",
    "app/src/main/java/com/listamercado/app/model/Recurrence.kt",
    "app/src/main/java/com/listamercado/app/data/ProductCatalogRepository.kt",
    "app/src/main/java/com/listamercado/app/data/BarcodeLookupRepository.kt",
    "app/src/main/java/com/listamercado/app/data/TemplateRepository.kt",
    "app/src/main/java/com/listamercado/app/ui/ListDetailActivity.kt",
    "app/src/main/java/com/listamercado/app/ui/PurchaseModeActivity.kt",
    "app/src/main/java/com/listamercado/app/ui/PurchaseModeAdapter.kt",
    "app/src/main/java/com/listamercado/app/ui/CatalogActivity.kt",
    "app/src/main/java/com/listamercado/app/ui/CatalogProductAdapter.kt",
    "app/src/main/java/com/listamercado/app/ui/BarcodeScannerActivity.kt",
    "app/src/main/java/com/listamercado/app/ui/TemplatesActivity.kt",
    "app/src/main/java/com/listamercado/app/ui/TemplateAdapter.kt",
    "app/src/main/java/com/listamercado/app/ui/RecurringProductsActivity.kt",
    "app/src/main/java/com/listamercado/app/ui/RecurringProductAdapter.kt",
    "app/src/main/res/layout/activity_purchase_mode.xml",
    "app/src/main/res/layout/item_purchase_mode.xml",
    "app/src/main/res/layout/activity_catalog.xml",
    "app/src/main/res/layout/item_catalog_product.xml",
    "app/src/main/res/layout/activity_barcode_scanner.xml",
    "app/src/main/res/layout/activity_templates.xml",
    "app/src/main/res/layout/item_list_template.xml",
    "app/src/main/res/layout/activity_recurring_products.xml",
    "app/src/main/res/layout/item_recurring_product.xml",
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

if "android.permission.CAMERA" not in manifest:
    raise SystemExit("Missing CAMERA permission for local barcode scanning")

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

if "#111F2B" not in night_colors or "#142330" not in night_colors:
    raise SystemExit("Updated graphite-blue dark surfaces are missing")

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

catalog_source = (root / "app/src/main/java/com/listamercado/app/data/ProductCatalogRepository.kt").read_text(encoding="utf-8")
barcode_lookup_source = (root / "app/src/main/java/com/listamercado/app/data/BarcodeLookupRepository.kt").read_text(encoding="utf-8")
barcode_lookup_model = (root / "app/src/main/java/com/listamercado/app/model/BarcodeLookupResult.kt").read_text(encoding="utf-8")
item_dialog_source = (root / "app/src/main/java/com/listamercado/app/ui/ItemDialog.kt").read_text(encoding="utf-8")
scanner_source = (root / "app/src/main/java/com/listamercado/app/ui/BarcodeScannerActivity.kt").read_text(encoding="utf-8")
settings_activity_source = (root / "app/src/main/java/com/listamercado/app/ui/SettingsActivity.kt").read_text(encoding="utf-8")
settings_layout = (root / "app/src/main/res/layout/activity_settings.xml").read_text(encoding="utf-8")
if "normalizeName" not in catalog_source or "Normalizer.normalize" not in catalog_source or "catalog_products_v1" not in catalog_source:
    raise SystemExit("Normalized local product catalog persistence is missing")
if "seedFromLists" not in catalog_source or "lastUnitPrice" not in catalog_source or "barcode" not in catalog_source:
    raise SystemExit("Catalog must seed existing items and persist last price/barcode")
if "MaterialAutoCompleteTextView" not in item_dialog_source or "findByBarcode" not in item_dialog_source or "buttonScanBarcode" not in item_dialog_source:
    raise SystemExit("Known-product suggestions or barcode fill flow is missing from item editor")
if "lookupBarcodeOnline" not in item_dialog_source or "BarcodeLookupRepository" not in item_dialog_source:
    raise SystemExit("Automatic optional online barcode lookup is missing from item editor")
if "Open Food Facts" not in barcode_lookup_source or "product_name" not in barcode_lookup_source or "BarcodeLookupResult" not in barcode_lookup_model:
    raise SystemExit("Online barcode lookup mapping is incomplete")
if "BarcodeScanning.getClient" not in scanner_source or "InputImage.fromMediaImage" not in scanner_source:
    raise SystemExit("On-device barcode scanner implementation is missing")
if "buttonTorch" not in scanner_source or "chooseBestBarcode" not in scanner_source or "lastCandidateHits" not in scanner_source:
    raise SystemExit("Scanner quality-of-life improvements (torch/selection/stability) are missing")
if 'implementation("com.google.mlkit:barcode-scanning:17.3.0")' not in app_gradle:
    raise SystemExit("Bundled ML Kit barcode-scanning dependency is missing")
if "play-services-mlkit-barcode-scanning" in app_gradle:
    raise SystemExit("Barcode scanning must not depend on the dynamically downloaded Play Services model")
for camera_dep in (
    'implementation("androidx.camera:camera-camera2:1.5.3")',
    'implementation("androidx.camera:camera-lifecycle:1.5.3")',
    'implementation("androidx.camera:camera-view:1.5.3")',
):
    if camera_dep not in app_gradle:
        raise SystemExit(f"CameraX dependency missing: {camera_dep}")
if re.search(r'androidx\.camera:camera-[^:\"]+:1\.6\.', app_gradle):
    raise SystemExit(
        "CameraX 1.6.x is incompatible with this project's compileSdk 35 / AGP 8.7.3 baseline; "
        "keep the scanner on the validated 1.5.3 line unless the Android toolchain is upgraded together"
    )
if "CatalogActivity" not in manifest or "BarcodeScannerActivity" not in manifest:
    raise SystemExit("Catalog/scanner activities are missing from AndroidManifest.xml")
if "rowCatalog" not in settings_layout or "CatalogActivity::class.java" not in settings_activity_source:
    raise SystemExit("Catalog entry point is missing from Settings")

template_source = (root / "app/src/main/java/com/listamercado/app/data/TemplateRepository.kt").read_text(encoding="utf-8")
templates_activity = (root / "app/src/main/java/com/listamercado/app/ui/TemplatesActivity.kt").read_text(encoding="utf-8")
recurring_activity = (root / "app/src/main/java/com/listamercado/app/ui/RecurringProductsActivity.kt").read_text(encoding="utf-8")
catalog_model = (root / "app/src/main/java/com/listamercado/app/model/CatalogProduct.kt").read_text(encoding="utf-8")
catalog_activity = (root / "app/src/main/java/com/listamercado/app/ui/CatalogActivity.kt").read_text(encoding="utf-8")
list_actions_source = (root / "app/src/main/java/com/listamercado/app/ui/ListActionsDialog.kt").read_text(encoding="utf-8")
if "var favorite: Boolean" not in catalog_model or "var recurringFrequency: String?" not in catalog_model:
    raise SystemExit("Catalog favorite/recurrence fields are missing")
if "setFavorite" not in catalog_source or "setRecurringFrequency" not in catalog_source or "recurringProducts" not in catalog_source:
    raise SystemExit("Catalog favorite/recurrence persistence is missing")
recurrence_source = (root / "app/src/main/java/com/listamercado/app/model/Recurrence.kt").read_text(encoding="utf-8")
if 'const val WEEKLY = "weekly"' not in recurrence_source or 'const val BIWEEKLY = "biweekly"' not in recurrence_source or 'const val MONTHLY = "monthly"' not in recurrence_source:
    raise SystemExit("Weekly/biweekly/monthly recurrence options are missing")
if "buttonAddRecurring" not in detail_source or "RecurringProductsActivity" not in manifest or "selectedProducts" not in recurring_activity:
    raise SystemExit("Add recurring products flow is missing")
for name in ("Cicloviagem", "Compra do mês", "Churrasco", "Camping", "Limpeza"):
    if name not in template_source:
        raise SystemExit(f"Initial list template missing: {name}")
if 'ListTemplate(BUILTIN_MONTHLY_ID, "Compra do mês", builtIn = true)' not in template_source or 'ListTemplate(BUILTIN_CAMPING_ID, "Camping", builtIn = true)' not in template_source:
    raise SystemExit("Non-Cicloviagem initial templates must remain empty")
if "saveFromList" not in template_source or "createListFromTemplate" not in template_source or "buttonActionTemplate" not in list_actions_source:
    raise SystemExit("Save-list-as-template flow is missing")
if "TemplatesActivity" not in manifest or "Usar modelo" not in main_source or "templateRepository.createListFromTemplate" not in templates_activity:
    raise SystemExit("Create-list-from-template flow is missing")

backup_source = (root / "app/src/main/java/com/listamercado/app/data/BackupRepository.kt").read_text(encoding="utf-8")
if 'const val SCHEMA_VERSION = 2' not in backup_source or '"meu-supermercado-backup"' not in backup_source:
    raise SystemExit("Versioned JSON backup format is missing")
if 'put("templates",' not in backup_source or 'put("favorite", favorite)' not in backup_source or 'put("recurringFrequency", recurringFrequency' not in backup_source:
    raise SystemExit("Schema 2 backup must preserve templates, favorites and recurrence")
if "parseAndValidate" not in backup_source or "mergeWith" not in backup_source or "replaceWith" not in backup_source:
    raise SystemExit("Backup validation/import modes are missing")
if 'put("priceHistory", history)' not in backup_source or "createCsvExport" not in backup_source:
    raise SystemExit("Price-history backup or CSV export is missing")
if "rowExportBackup" not in settings_layout or "rowImportBackup" not in settings_layout or "rowExportCsv" not in settings_layout:
    raise SystemExit("Backup/CSV controls are missing from Settings")
if "ActivityResultContracts.CreateDocument" not in settings_activity_source or "ActivityResultContracts.OpenDocument" not in settings_activity_source:
    raise SystemExit("Backup must use Android document pickers instead of a fixed folder")
if 'setPositiveButton("Mesclar")' not in settings_activity_source or 'setNeutralButton("Substituir")' not in settings_activity_source:
    raise SystemExit("Import summary must offer merge and replace modes")
item_layout = (root / "app/src/main/res/layout/dialog_item.xml").read_text(encoding="utf-8")
if "PRODUTO" not in item_layout or "COMPRA" not in item_layout or "DETALHES" not in item_layout:
    raise SystemExit("Redesigned item editor sections are missing")
if "Widget.Material3.TextInputLayout.FilledBox" not in item_layout:
    raise SystemExit("Item editor must use the reduced-outline filled field style")
if 'placeholderText="Ex.: Arroz, café, sabão em pó"' not in item_layout or "textLookupStatus" not in item_layout:
    raise SystemExit("Item editor refinements for product-name entry and lookup feedback are missing")

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
