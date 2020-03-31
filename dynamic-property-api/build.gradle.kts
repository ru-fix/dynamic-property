import org.gradle.kotlin.dsl.*


plugins {
    java
    kotlin("jvm")
}

dependencies {
    implementation(Libs.slf4j_api)

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testImplementation(Libs.kotlin_jdk8)
    testImplementation(Libs.kotlin_stdlib)
    testImplementation(Libs.kotlin_reflect)
    testImplementation(Libs.slf4j_simple)
}


