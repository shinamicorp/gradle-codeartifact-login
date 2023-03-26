package com.shinami.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import org.gradle.api.plugins.ExtensionAware
import org.gradle.testfixtures.ProjectBuilder

class CodeArtifactLoginPluginTest : FunSpec() {
    init {
        context("With applied plugin") {
            val project = ProjectBuilder.builder().build()
            project.plugins.apply("com.shinami.codeartifact-login")

            test("it registers task") {
                project.tasks.findByName("codeArtifactLogin").shouldNotBeNull()
            }
            test("it registers project extension") {
                project.extensions
                    .findByName("codeArtifact")
                    .shouldBeInstanceOf<CodeArtifactExtension>()
            }
            test("it registers repository handler extension") {
                (project.repositories as ExtensionAware)
                    .extensions
                    .findByName("codeArtifact")
                    .shouldBeInstanceOf<CodeArtifactRepositoryHandler>()
            }
        }
    }
}
