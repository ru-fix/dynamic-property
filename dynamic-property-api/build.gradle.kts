import org.gradle.kotlin.dsl.*


plugins {
    java
    kotlin("jvm")
}

dependencies {
    // Should depend only on slf4j, stdlib-reference and JVM
    api(Libs.slf4j_api)
    api(Libs.jfix_stdlib_reference)

    testImplementation(Libs.junit_api)
    testImplementation(Libs.junit_engine)
    testImplementation(Libs.kotlin_jdk8)
    testImplementation(Libs.kotlin_stdlib)
    testImplementation(Libs.kotlin_reflect)
    testImplementation(Libs.slf4j_simple)
}


