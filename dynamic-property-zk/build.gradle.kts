import org.gradle.kotlin.dsl.*


plugins {
    java
    kotlin("jvm")
}

dependencies {

    compile(Libs.kotlin_logging)
    compile(Libs.curator_recipes)
    compile(project(":dynamic-property-api"))
    compile(project(":dynamic-property-jackson"))

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testCompile(Libs.kotlin_jdk8)
    testCompile(Libs.kotlin_stdlib)
    testCompile(Libs.kotlin_reflect)

    testCompile(project(":dynamic-property-zk-test"))
}