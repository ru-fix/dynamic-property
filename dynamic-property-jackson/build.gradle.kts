plugins {
    java
    kotlin("jvm")
}

dependencies {

    compile(project(":dynamic-property-api"))

    compile(Libs.jackson_core)
    compile(Libs.jackson_databind)
    compile(Libs.jackson_jsr310)
    compile(Libs.jackson_module_kotlin)

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
}
