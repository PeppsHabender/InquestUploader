
plugins {
    id("inquest.project-config")
}

dependencies {
    commonMainApi(inquest.logging.slf4j)
    commonMainApi(inquest.kotlin.coroutines)
    commonMainApi(inquest.kotlin.serialcore)

    commonMainCompileOnly(inquest.json.jackson.annots)
    commonMainCompileOnly(ui.compose.voyager.nav)
    commonMainCompileOnly(ui.compose.kodein)
}
