import org.gradle.kotlin.dsl.*


plugins {
    java
    kotlin("jvm")
}

dependencies {
    api(project(Projs.`dynamic-property-api`.dependency))

    implementation(Libs.slf4j_api)
    implementation(Libs.jfix_stdlib_concurrency) {
        exclude("ru.fix")
    }

    testImplementation(project(Projs.`dynamic-property-api`.dependency))

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testImplementation(Libs.kotlin_jdk8)
    testImplementation(Libs.kotlin_stdlib)
    testImplementation(Libs.kotlin_reflect)
    testImplementation(Libs.mockk)

    testImplementation(Libs.slf4j_over_log4j)
    testImplementation(Libs.log4j_core)
}


