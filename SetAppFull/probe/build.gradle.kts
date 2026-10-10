plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}
val workspaceRoot = providers.environmentVariable("SETAPPFULL_ROOT").orElse(
    rootProject.projectDir.parentFile.parentFile.absolutePath
).get()
android {
    namespace = "ss.colytitse.setappfull.probe"
    compileSdk = 37
    buildToolsVersion = "37.0.0"
    defaultConfig {
        applicationId = "ss.colytitse.setappfull.probe"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    signingConfigs.getByName("debug") { storeFile = file("$workspaceRoot/signing/debug.keystore") }
    buildFeatures { compose = true }
    buildTypes { debug { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
}
