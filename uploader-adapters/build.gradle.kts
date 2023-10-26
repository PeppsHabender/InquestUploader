plugins {
    id("inquest.compose-config")
}

dependencies {
    compileOnly(ui.compose.kodein)
    compileOnly(ui.compose.voyager.transitions)

    commonMainImplementation(project(":uploader-compose-commons"))
    commonMainImplementation(kotlin("reflect"))
    commonMainImplementation(inquest.json.jackson)
    commonMainImplementation(inquest.json.jsonata)
    commonMainImplementation(inquest.apache.commons)
}
