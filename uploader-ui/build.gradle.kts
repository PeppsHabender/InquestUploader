import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("inquest.compose-config")
}

dependencies {
    commonMainApi(project(":uploader-compose-commons"))
    commonMainApi(project(":uploader-business-logic"))

    compileOnly(ui.compose.kodein)

    implementation(inquest.logging.logback.classic)
    implementation(inquest.logging.logback.core)
    implementation(ui.darklaf)
    implementation(ui.compose.voyager.nav)
    implementation(ui.compose.voyager.tabnav)
    implementation(ui.compose.voyager.kodein)
    implementation(ui.compose.voyager.transitions)

    implementation(project(":uploader-adapters"))
}

compose.desktop {
    application {
        mainClass = "org.inquest.uploader.ui.main.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            includeAllModules = true
            packageName = "inquest-uploader"
            packageVersion = "1.0.0"
        }
    }
}
