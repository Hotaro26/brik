import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.serialization)
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(project.findProperty("StoreFile") ?: "../keys/brik-release.jks")
            storePassword = (project.findProperty("StorePassword") as String?) ?: "brik123"
            keyPassword = (project.findProperty("KeyPassword") as String?) ?: "brik123"
            keyAlias = (project.findProperty("KeyAlias") as String?) ?: "brik_key"
        }
        create("defaultSignature") {
            storeFile = file(project.findProperty("StoreFile") ?: "testkey.jks")
            storePassword = (project.findProperty("StorePassword") as String?) ?: "testkey"
            keyPassword = (project.findProperty("KeyPassword") as String?) ?: "testkey"
            keyAlias = (project.findProperty("KeyAlias") as String?) ?: "testkey"
        }
    }
    namespace = "com.hotaro.brik"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    lint.checkReleaseBuilds = false
    lint.disable += "All"

    defaultConfig {
        applicationId = "com.hotaro.brik"
        minSdk = 26
        targetSdk = 37
        versionCode = 25
        versionName = "2.0.6"
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
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

composeCompiler {
    includeSourceInformation = false
    includeTraceMarkers = false
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
    sourceSets {
        all {
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.android.desugar)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.serialization)
    implementation(libs.shizuku.provider)
    implementation(libs.shizuku.api)
    implementation(libs.dhizuku.api)
    implementation(libs.hiddenApiBypass)
    implementation(libs.libsu.core)
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
}
