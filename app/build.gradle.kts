import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ia.ankherth.grease"
    compileSdk = 36

    // Configure NDK for 16KB page size support (required for Android 15+ devices)
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "ia.ankherth.grease"
        minSdk = 29
        targetSdk = 36
        versionCode = 11
        versionName = "5.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Enable resource optimization (modern approach)
        androidResources {
            // Solo español e inglés para reducir tamaño
            localeFilters += listOf("en", "es")
            // Elimina recursos no utilizados automáticamente
            ignoreAssetsPattern = "!.svn:!.git:.*:!CVS:!thumbs.db:!picasa.ini:!*.scc:*~"
        }

        // NDK configuration for proper alignment with 16KB page sizes
        ndk {
            // Filter to only include necessary architectures (reduces APK size)
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
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
            signingConfig = signingConfigs.getByName("release")

            // Additional optimizations for release
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }

            // Optimizaciones adicionales
            isDebuggable = false
            isJniDebuggable = false
            isPseudoLocalesEnabled = false
            isCrunchPngs = true
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }

    // Habilitar APK splits por ABI (genera APKs separados por arquitectura)
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true // También genera un APK universal como fallback
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module",
                "kotlin/**",
                "META-INF/androidx.*.version",
                "META-INF/proguard/androidx-*.pro",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
                "okhttp3/**",
                "META-INF/com.google.*.version"
            )
        }
        jniLibs {
            // CRITICAL: Disable legacy packaging to ensure 16KB alignment
            useLegacyPackaging = false
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        // Desactiva features no utilizadas
        aidl = false
        renderScript = false
        resValues = false
        shaders = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = false
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all",
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions"
        )
    }

    // Optimize build performance
    lint {
        checkReleaseBuilds = false
        abortOnError = false
        disable += setOf("MissingTranslation", "ExtraTranslation")
    }

    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
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

    // Palette para extracción de colores
    implementation("androidx.palette:palette-ktx:1.0.0")

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

    // Coil para carga de imágenes con optimizaciones
    implementation(libs.coil.kt) {
        exclude(group = "androidx.appcompat")
        exclude(group = "androidx.core")
    }

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}