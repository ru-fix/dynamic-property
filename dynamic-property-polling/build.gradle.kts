import org.gradle.kotlin.dsl.*


plugins {
    java
    kotlin("jvm")
}

dependencies {
    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testCompile(Libs.kotlin_jdk8)
    testCompile(Libs.kotlin_stdlib)
    testCompile(Libs.kotlin_reflect)
    compile(Libs.jfix_stdlib_concurrency)
    compile(project(":dynamic-property-api"))

    testCompile(Libs.slf4j_simple)
}


