import org.gradle.kotlin.dsl.*


plugins {
    java
    kotlin("jvm")
}

dependencies {

    compile(Libs.kotlin_logging)
    compile(Libs.curator_recipes)
    compile(Libs.curator_test)
    compile(Libs.junit_api)
    compile(Libs.junit_engine)

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testCompile(Libs.kotlin_jdk8)
    testCompile(Libs.kotlin_stdlib)
    testCompile(Libs.kotlin_reflect)

    testCompile(Libs.slf4j_simple)

}