plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.listamercado.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.listamercado.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.0.7"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val ks = System.getenv("LISTA_MERCADO_KEYSTORE_PATH")
            if (!ks.isNullOrBlank()) {
                storeFile = file(ks)
                storePassword = System.getenv("LISTA_MERCADO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("LISTA_MERCADO_KEY_ALIAS")
                keyPassword = System.getenv("LISTA_MERCADO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val ks = System.getenv("LISTA_MERCADO_KEYSTORE_PATH")
            if (!ks.isNullOrBlank()) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
