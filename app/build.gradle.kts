import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "com.aaron.sidegesture"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aaron.okgesture"
        minSdk = 23
        targetSdk = 35
        versionCode = 10000
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    signingConfigs {
        val properties = Properties()
        val inputStream = project.rootProject.file("local.properties").inputStream()
        properties.load(inputStream)
        register("release") {
            storeFile = file(properties.getProperty("STORE_FILE_NAME"))
            storePassword = properties.getProperty("KEYSTORE_PASSWORD")
            keyAlias = properties.getProperty("STORE_ALIAS")
            keyPassword = properties.getProperty("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".dev"
            isMinifyEnabled = false
            val appName = "DEV Gesture"
            resValue("string", "app_name", appName)
            resValue("string", "home_title", appName)
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
    lint {
        abortOnError = false
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.com.aaronzzx.fastcompose.compose)
    implementation(libs.com.aaronzzx.fastcompose.compose.accessibility)
    implementation(libs.jetbrains.kotlin.serialization)
    implementation(libs.androidx.datastore)
    implementation(libs.compose.colorpicker)
    implementation(libs.material.icons.extended)
}