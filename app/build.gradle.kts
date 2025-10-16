import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ia.ankherth.grease"
    compileSdk = 36

    defaultConfig {
        applicationId = "ia.ankherth.grease"
        minSdk = 29
        targetSdk = 36
        versionCode = 6
        versionName = "0.3.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    // Carga condicional de propiedades de firma para release
    val keystoreProperties = Properties()
    val keystorePropertiesFile = rootProject.file("keystore/keystore.properties")
    val hasKeystoreProps = if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
        val requiredKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
        requiredKeys.all { !keystoreProperties.getProperty(it).isNullOrBlank() }
    } else false

    signingConfigs {
        create("release") {
            if (hasKeystoreProps) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            } else {
                // Fallback estable: firma con el debug keystore por defecto para no romper el build
                val home = System.getProperty("user.home")
                storeFile = file("$home/.android/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Firma SIEMPRE con la config de release (sin caer en la buildType debug)
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            // Keep debug as non-minified for easier dev
            isMinifyEnabled = false
        }
    }

    packagingOptions {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.pdfviewer)

    // Architecture Components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.activity.ktx)

    // RecyclerView
    implementation(libs.androidx.recyclerview)

    // SharedPreferences para persistencia simple
    implementation(libs.androidx.preference.ktx)

    // Gson para serialización JSON
    implementation(libs.google.gson)

    // Room Database para persistencia robusta (KSP)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // SwipeRefreshLayout para pull-to-refresh
    implementation(libs.androidx.swiperefreshlayout)

    // Permisos
    implementation(libs.permissionx)

    // DataStore para preferencias de usuario
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}