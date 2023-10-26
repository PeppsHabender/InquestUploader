plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.6.0")
    implementation("org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin:1.9.0")
    implementation("org.jetbrains.compose:org.jetbrains.compose.gradle.plugin:1.5.1")
}