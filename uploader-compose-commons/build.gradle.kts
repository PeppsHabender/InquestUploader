plugins {
    id("inquest.compose-config")
}

dependencies {
    commonMainApi(project(":uploader-api"))
    commonMainApi(project(":uploader-business-logic"))
    commonMainApi(compose.materialIconsExtended)
    commonMainApi(ui.compose.kodein)
    commonMainApi(ui.compose.kodein.jvm)
    commonMainApi(ui.compose.voyager.kodein)
    commonMainApi(ui.compose.voyager.tabnav)
}
