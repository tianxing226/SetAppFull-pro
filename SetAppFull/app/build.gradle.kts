import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}
val workspaceRoot = providers.environmentVariable("SETAPPFULL_ROOT").orElse("F:/SetAppFull").get()
val releasePropertiesFile = file("$workspaceRoot/signing/release.properties")
val releaseProperties = Properties().apply {
    if (releasePropertiesFile.isFile) releasePropertiesFile.inputStream().use(::load)
}
android {
    namespace = "ss.colytitse.setappfull"
    compileSdk = 37
    buildToolsVersion = "37.0.0"
    defaultConfig {
        applicationId = "ss.colytitse.setappfull"
        minSdk = 30
        targetSdk = 37
        versionCode = 200
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        getByName("debug") { storeFile = file("$workspaceRoot/signing/debug.keystore") }
        if (releasePropertiesFile.isFile) {
            create("release") {
                storeFile = file(releaseProperties.getProperty("storeFile"))
                storePassword = releaseProperties.getProperty("storePassword")
                keyAlias = releaseProperties.getProperty("keyAlias")
                keyPassword = releaseProperties.getProperty("keyPassword")
            }
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    buildTypes {
        debug { isDebuggable = true }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releasePropertiesFile.isFile) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        htmlReport = true
        xmlReport = true
    }
}
dependencies {
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.android)
    implementation(libs.backdrop)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
