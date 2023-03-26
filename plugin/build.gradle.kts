import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
    `java-gradle-plugin`

    id("org.jetbrains.kotlin.jvm") version "1.8.10"
    id("com.gradle.plugin-publish") version "1.1.0"
    id("com.diffplug.spotless") version "6.17.0"
}

group = "com.shinami"

version = "0.1.1"

gradlePlugin {
    website.set("https://github.com/shinamicorp/gradle-codeartifact-login")
    vcsUrl.set("https://github.com/shinamicorp/gradle-codeartifact-login")

    val codeArtifactLogin by
        plugins.creating {
            id = "com.shinami.codeartifact-login"
            displayName = "Plugin to manage AWS CodeArtifact login tokens."
            description =
                """
                Manages AWS CodeArtifact login tokens and integrates with Gradle's maven repositories.
                Running the login task requires AWS CLI v2 to be available and authenticated.
                """
                    .trimIndent()
            tags.set(listOf("aws", "codeartifact", "maven"))
            implementationClass = "com.shinami.gradle.CodeArtifactLoginPlugin"
        }
}

spotless {
    val ktfmtVersion = "0.43"
    kotlin { ktfmt(ktfmtVersion).kotlinlangStyle() }
    kotlinGradle { ktfmt(ktfmtVersion).kotlinlangStyle() }
}

repositories { mavenCentral() }

dependencies {
    testImplementation(platform("io.kotest:kotest-bom:5.5.5"))
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("io.kotest:kotest-framework-api")
    testImplementation("io.kotest:kotest-framework-engine")
    testRuntimeOnly("io.kotest:kotest-runner-junit5")
}

tasks.withType<KotlinCompile> { kotlinOptions { allWarningsAsErrors = true } }

val functionalTestSourceSet = sourceSets.create("functionalTest") {}

configurations {
    named("functionalTestImplementation").extendsFrom(testImplementation)
    named("functionalTestRuntimeOnly").extendsFrom(testRuntimeOnly)
}

val functionalTest by
    tasks.registering(Test::class) {
        testClassesDirs = functionalTestSourceSet.output.classesDirs
        classpath = functionalTestSourceSet.runtimeClasspath
    }

gradlePlugin.testSourceSets(functionalTestSourceSet)

tasks.check { dependsOn(functionalTest) }

tasks.withType<Test> { useJUnitPlatform() }
