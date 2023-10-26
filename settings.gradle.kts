rootProject.name = "inquest-uploader"

includeBuild("uploader-build-logic")

include("uploader-api")
include("uploader-business-logic")
include("uploader-compose-commons")
include("uploader-adapters")
include("uploader-ui")

dependencyResolutionManagement {
    versionCatalogs {
        create("inquest") {
            val jackson = "jackson"
            val logback = "logback"
            val httpclient = "httpclient"
            val serialization = "serialization"

            version(jackson, "2.15.3")
            version(logback, "1.4.11")
            version(httpclient, "4.5.14")
            version(serialization, "1.6.0")

            library("json-jackson", "com.fasterxml.jackson.module", "jackson-module-kotlin").versionRef(jackson)
            library("json-jackson-annots", "com.fasterxml.jackson.core", "jackson-annotations").versionRef(jackson)
            library("json-jsonata", "com.ibm.jsonata4java", "JSONata4Java").version("2.4.3")
            library("persistence-rocksdb", "org.rocksdb", "rocksdbjni").version("8.3.3")

            library("apache-commons", "org.apache.commons", "commons-lang3").version("3.13.0")
            library("apache-http-core", "org.apache.httpcomponents", "httpcore").version("4.4.16")
            library("apache-http-client", "org.apache.httpcomponents", "httpclient").versionRef(httpclient)
            library("apache-http-mime", "org.apache.httpcomponents", "httpmime").versionRef(httpclient)

            library("logging-slf4j", "org.slf4j", "slf4j-api").version("2.0.9")
            library("logging-logback-classic", "ch.qos.logback", "logback-classic").versionRef(logback)
            library("logging-logback-core", "ch.qos.logback", "logback-core").versionRef(logback)

            library("kotlin-coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").version("1.7.3")
            library("kotlin-serialcore", "org.jetbrains.kotlinx", "kotlinx-serialization-core").versionRef(serialization)
            library("kotlin-properties", "org.jetbrains.kotlinx", "kotlinx-serialization-properties").versionRef(serialization)
            library("kotlin-protobuf", "org.jetbrains.kotlinx", "kotlinx-serialization-protobuf").versionRef(serialization)
        }

        create("ui") {
            val kodein = "kodein"
            val voyager = "voyager"

            version(kodein, "7.20.1")
            version(voyager, "1.0.0-rc07")

            library("darklaf", "com.github.weisj", "darklaf-core").version("3.0.2")
            library("compose-kodein", "org.kodein.di", "kodein-di-framework-compose").versionRef(kodein)
            library("compose-kodein-jvm", "org.kodein.di", "kodein-di-jvm").versionRef(kodein)
            library("compose-voyager-nav", "cafe.adriel.voyager", "voyager-navigator").versionRef(voyager)
            library("compose-voyager-tabnav", "cafe.adriel.voyager", "voyager-tab-navigator").versionRef(voyager)
            library("compose-voyager-transitions", "cafe.adriel.voyager", "voyager-transitions").versionRef(voyager)
            library("compose-voyager-kodein", "cafe.adriel.voyager", "voyager-kodein-desktop").versionRef(voyager)
        }
    }
}