object Vers {
    val kotlin = "1.2.41"
    val sl4j = "1.7.25"
    val junit_gradle_plugin = "1.1.0"
    val dokkav = "0.9.14"
    val gradleReleasePlugin = "1.2.18"
}

object Libs {
    val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Vers.kotlin}"
    val kotlin_jre8 = "org.jetbrains.kotlin:kotlin-stdlib-jre8:${Vers.kotlin}"
    val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect:${Vers.kotlin}"

    val gradleReleasePlugin = "ru.fix:gradle-release-plugin:${Vers.gradleReleasePlugin}"
    val dokkaGradlePlugin =  "org.jetbrains.dokka:dokka-gradle-plugin:${Vers.dokkav}"

    val slf4j_api = "org.slf4j:slf4j-api:${Vers.sl4j}"
    val slf4j_simple = "org.slf4j:slf4j-simple:${Vers.sl4j}"

    val junit = "junit:junit:4.12"

    val guava = "com.google.guava:guava:21.0"
    val mockito = "org.mockito:mockito-all:1.10.19"
    val mockito_kotiln = "com.nhaarman:mockito-kotlin-kt1.1:1.5.0"
    val kotlin_logging = "io.github.microutils:kotlin-logging:1.4.9"

    val junit_gradle_plugin = "org.junit.platform:junit-platform-gradle-plugin:${Vers.junit_gradle_plugin}"
}