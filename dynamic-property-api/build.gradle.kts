import org.gradle.kotlin.dsl.*


plugins {
    java
    kotlin("jvm")
}

dependencies {
    compile(Libs.jackson_core)
    compile(Libs.jackson_databind)
    compile(Libs.jackson_jsr310)
    compile(Libs.jackson_module_kotlin)
    compile(Libs.slf4j_api)
    compile(Libs.slf4j_simple)

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testCompile(Libs.kotlin_jdk8)
    testCompile(Libs.kotlin_stdlib)
    testCompile(Libs.kotlin_reflect)
}


