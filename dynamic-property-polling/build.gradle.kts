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
    compile(project(":dynamic-property-api"))
    compile("ru.fix:jfix-stdlib-concurrency:1.0.13")
}


