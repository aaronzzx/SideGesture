import java.util.Properties

val releaseSigningProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use(::load)
    }
}
val releaseSigningPropertyNames = listOf(
    "STORE_FILE_NAME",
    "KEYSTORE_PASSWORD",
    "STORE_ALIAS",
    "KEY_PASSWORD"
)
val hasReleaseSigningProperties = releaseSigningPropertyNames.all {
    !releaseSigningProperties.getProperty(it).isNullOrBlank()
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.jetbrains.kotlin.parcelize)
}

android {
    namespace = "com.aaron.sidegesture"
    compileSdk = 35

    defaultConfig {
        applicationId = "gulu.gulugulu"
        minSdk = 23
        targetSdk = 35
        versionCode = 10601
        versionName = "1.6.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    signingConfigs {
        if (hasReleaseSigningProperties) {
            register("release") {
                storeFile = file(releaseSigningProperties.getProperty("STORE_FILE_NAME"))
                storePassword = releaseSigningProperties.getProperty("KEYSTORE_PASSWORD")
                keyAlias = releaseSigningProperties.getProperty("STORE_ALIAS")
                keyPassword = releaseSigningProperties.getProperty("KEY_PASSWORD")
            }
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
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
        }
        debug {
            applicationIdSuffix = ".dev"
            isMinifyEnabled = false
            resValue("string", "app_name", "@string/app_name_dev")
            resValue("string", "home_title", "@string/app_name_dev")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
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
        aidl = true
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

gradle.taskGraph.whenReady {
    val releasePackageRequested = allTasks.any { task ->
        task.name == "assembleRelease" ||
            task.name == "bundleRelease" ||
            task.name == "packageRelease"
    }
    if (releasePackageRequested && !hasReleaseSigningProperties) {
        throw GradleException(
            "Release 打包缺少签名配置：${releaseSigningPropertyNames.joinToString()}"
        )
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(files("libs/tinypinyin-2.0.3.jar"))
    implementation(libs.org.ahocorasick)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.com.aaronzzx.fastcompose.compose)
    implementation(libs.com.aaronzzx.fastcompose.compose.accessibility)
    implementation(libs.com.tiann.freereflection)
    implementation(libs.jetbrains.kotlin.serialization)
    implementation(libs.androidx.datastore)
    implementation(libs.compose.colorpicker)
    implementation(libs.material.icons.extended)
    implementation(libs.sh.calvin.reorderable)
    implementation(libs.dev.rikka.shizuku.api)
    implementation(libs.dev.rikka.shizuku.provider)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
