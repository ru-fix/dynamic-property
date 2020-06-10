plugins {
    java
    kotlin("jvm")
}

dependencies {

    implementation(project(":dynamic-property-api"))

    implementation(Libs.jackson_core)
    implementation(Libs.jackson_databind)
    implementation(Libs.jackson_jsr310)
    implementation(Libs.jackson_module_kotlin)

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
}
