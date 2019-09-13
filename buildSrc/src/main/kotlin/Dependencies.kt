import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.project

object Vers {
    //Plugins
    const val dokkav = "0.9.18"
    const val gradle_release_plugin = "1.3.9"
    const val asciidoctor = "1.5.9.2"

    //Dependencies
    const val kotlin = "1.3.41"
    const val sl4j = "1.7.26"
    const val junit = "5.4.2"
    const val curator_recipes = "4.2.0"
    const val curator_test = "4.2.0"
    const val spring = "5.1.8.RELEASE"
    const val spring_boot_auto_configure = "2.1.5.RELEASE"
    const val jackson = "2.9.9"

    const val log4j =  "2.12.0"
}

object Libs {

    //Plugins
    const val gradle_release_plugin = "ru.fix:gradle-release-plugin:${Vers.gradle_release_plugin}"
    const val dokka_gradle_plugin =  "org.jetbrains.dokka:dokka-gradle-plugin:${Vers.dokkav}"
    const val asciidoctor = "org.asciidoctor:asciidoctor-gradle-plugin:${Vers.asciidoctor}"
    const val nexus_staging_plugin = "io.codearte.nexus-staging"
    const val nexus_publish_plugin = "de.marcphilipp.nexus-publish"

    //Dependencies
    const val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Vers.kotlin}"
    const val kotlin_jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Vers.kotlin}"
    const val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect:${Vers.kotlin}"

    const val awaitility = "org.awaitility:awaitility:4.0.1"

    val jfix_stdlib_concurrency = "ru.fix:jfix-stdlib-concurrency:1.0.49"

    const val slf4j_api = "org.slf4j:slf4j-api:${Vers.sl4j}"
    const val slf4j_simple = "org.slf4j:slf4j-simple:${Vers.sl4j}"

    val log4j_api = "org.apache.logging.log4j:log4j-api:${Vers.log4j}"
    val log4j_core = "org.apache.logging.log4j:log4j-core:${Vers.log4j}"
    val log4j_kotlin = "org.apache.logging.log4j:log4j-api-kotlin:1.0.0"
    val slf4j_over_log4j = "org.apache.logging.log4j:log4j-slf4j-impl:${Vers.log4j}"

    const val curator_recipes = "org.apache.curator:curator-recipes:${Vers.curator_recipes}"
    const val curator_test = "org.apache.curator:curator-test:${Vers.curator_test}"

    const val spring_beans = "org.springframework:spring-beans:${Vers.spring}"
    const val spring_boot_auto_configure = "org.springframework.boot:spring-boot-autoconfigure:${Vers.spring_boot_auto_configure}"
    const val spring_test = "org.springframework:spring-test:${Vers.spring}"

    const val jackson_core = "com.fasterxml.jackson.core:jackson-core:${Vers.jackson}"
    const val jackson_databind = "com.fasterxml.jackson.core:jackson-databind:${Vers.jackson}"
    const val jackson_jsr310 = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Vers.jackson}"
    const val jackson_module_kotlin = "com.fasterxml.jackson.module:jackson-module-kotlin:${Vers.jackson}"

    const val hamkrest = "com.natpryce:hamkrest:1.7.0.0"

    const val mockito = "org.mockito:mockito-all:1.10.19"
    const val mockito_kotiln = "com.nhaarman:mockito-kotlin-kt1.1:1.5.0"
    const val kotlin_logging = "io.github.microutils:kotlin-logging:1.4.9"

    const val junit_api = "org.junit.jupiter:junit-jupiter-api:${Vers.junit}"
    const val junit_engine = "org.junit.jupiter:junit-jupiter-engine:${Vers.junit}"

    const val jfix_zookeeper = "ru.fix:jfix-zookeeper:1.0.6"
}

enum class Projs{
    dynamic_property_jackson,
    dynamic_property_api,
    dynamic_property_zk,
    dynamic_property_spring,
    dynamic_property_polling,
    dynamic_property_std_source;

    val directory get() = this.name.replace('_', '-')
    val dependency get(): String = ":$directory"

}