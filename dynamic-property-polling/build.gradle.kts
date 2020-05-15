plugins {
    java
    kotlin("jvm")
}

dependencies {
    api(project(Projs.`dynamic-property-api`.dependency))
    api(Libs.jfix_stdlib_concurrency) {
        exclude("ru.fix")
    }
    api(Libs.slf4j_api)


    testImplementation(Libs.aggregating_profiler)

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testImplementation(Libs.kotlin_jdk8)
    testImplementation(Libs.kotlin_stdlib)
    testImplementation(Libs.kotlin_reflect)
    testImplementation(Libs.mockk)

    testImplementation(Libs.slf4j_over_log4j)
    testImplementation(Libs.log4j_core)

}
