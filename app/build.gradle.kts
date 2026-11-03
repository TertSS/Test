plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.readtracker.lite"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.readtracker.lite"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-lite"
    }

    signingConfigs {
        create("release") {
            // Self-signed keystore generated in CI (GitHub Actions) for unsigned-release replacement.
            if (rootProject.file("keystore/release.keystore").exists()) {
                storeFile = rootProject.file("keystore/release.keystore")
                storePassword = System.getenv("RTL_KEYSTORE_PASSWORD") ?: "readtracker"
                keyAlias = System.getenv("RTL_KEY_ALIAS") ?: "release"
                keyPassword = System.getenv("RTL_KEY_PASSWORD") ?: "readtracker"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isOptimizationEnabled = true // removeUnusedCode + removeResourceFromResourceTable
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    // material-icons-extended (~1.5 МБ dex) вырезан: в коде используются только 7 иконок,
    // они объявлены локально в ui/IconsLite.kt — в APK не грузится ничего неиспользуемого.
    implementation(libs.coil.compose)
    debugImplementation(libs.compose.ui.tooling)
}
