package com.shinami.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner

class CodeArtifactLoginPluginFunctionalTest : FunSpec() {
    init {
        context("codeArtifactLogin") {
            val cases = listOf("kotlin" to "build.gradle.kts", "groovy" to "build.gradle")

            cases.forEach { (name, script) ->
                test(name) {
                    val projectDir = tempdir()
                    val buildFile = projectDir.resolve(script)

                    javaClass.getResourceAsStream("/basic/$script")!!.use {
                        buildFile.writeText(it.reader().readText())
                    }

                    val result =
                        GradleRunner.create()
                            .forwardOutput()
                            .withPluginClasspath()
                            .withProjectDir(projectDir)
                            .withArguments("codeArtifactLogin")
                            .build()

                    result.output shouldContain
                        Regex(
                            "Failed to read .*/\\.codeartifact/aws\\.properties\\. Run codeArtifactLogin task to populate it"
                        )
                    result.output shouldContain
                        Regex(
                            "CodeArtifact credentials saved at .*/\\.codeartifact/aws\\.properties"
                        )
                }
            }
        }
    }
}
