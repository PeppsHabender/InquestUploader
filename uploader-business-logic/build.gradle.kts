plugins {
    id("inquest.project-config")
}

dependencies {
    commonMainApi(project(":uploader-api"))
    commonMainApi(ui.compose.kodein.jvm)

    commonMainImplementation(inquest.apache.http.core)
    commonMainImplementation(inquest.apache.http.client)
    commonMainImplementation(inquest.apache.http.mime)
    commonMainImplementation(inquest.kotlin.properties)
    commonMainImplementation(inquest.kotlin.protobuf)
    commonMainImplementation(inquest.json.jackson)
    commonMainImplementation(inquest.apache.commons)
    commonMainImplementation(inquest.persistence.rocksdb)
}
