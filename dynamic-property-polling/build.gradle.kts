import org.gradle.kotlin.dsl.*


plugins {
    java
    kotlin("jvm")
}

dependencies {
    api(project(Projs.dynamic_property_api.dependency))

    implementation(Libs.slf4j_api)
    implementation(Libs.jfix_stdlib_concurrency){
        exclude("ru.fix")
    }
    implementation(Libs.jfix_stdlib_reference){
        exclude("ru.fix")
    }

    testImplementation(Libs.aggregating_profiler)
    testImplementation(project(Projs.dynamic_property_api.dependency))

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testImplementation(Libs.kotlin_jdk8)
    testImplementation(Libs.kotlin_stdlib)
    testImplementation(Libs.kotlin_reflect)
    testImplementation(Libs.mockk)

    testImplementation(Libs.slf4j_over_log4j)
    testImplementation(Libs.log4j_core)
}


