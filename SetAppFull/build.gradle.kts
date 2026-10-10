buildscript {
    dependencies {
        // AGP 9 has built-in Kotlin; use the compiler required by Backdrop.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
val workspaceRoot = providers.environmentVariable("SETAPPFULL_ROOT").orElse(
    rootProject.projectDir.parentFile.parentFile.absolutePath
)
allprojects {
    layout.buildDirectory.set(file("${workspaceRoot.get()}/build/${if (path == ":") "root" else path.removePrefix(":").replace(':', '/')}"))
}
