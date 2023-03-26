package com.shinami.gradle

import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

open class CodeArtifactLoginTask
@Inject
constructor(private val execOps: ExecOperations, objects: ObjectFactory) : DefaultTask() {
    @get:Input
    val repository: Property<String> =
        objects.property(String::class.java).also { it.finalizeValueOnRead() }

    @get:Input
    val domain: Property<String> =
        objects.property(String::class.java).also { it.finalizeValueOnRead() }

    @get:Input
    @get:Optional
    val domainOwner: Property<String> =
        objects.property(String::class.java).also { it.finalizeValueOnRead() }

    @get:Input
    @get:Optional
    val region: Property<String> =
        objects.property(String::class.java).also { it.finalizeValueOnRead() }

    @get:Input
    @get:Optional
    val durationSeconds: Property<Int> =
        objects.property(Int::class.java).also { it.convention(43200).finalizeValueOnRead() }

    @get:OutputFile
    val credentialsFile: RegularFileProperty =
        objects.fileProperty().also { it.finalizeValueOnRead() }

    init {
        group = GROUP
        description = "Logs in to an AWS CodeArtifact repository."

        outputs.upToDateWhen { task ->
            val props = task.project.tryLoadProperties(credentialsFile)
            props.getProperty("expiration")?.let { it.toLong() * 1000 > System.currentTimeMillis() }
                ?: false
        }
    }

    @TaskAction
    fun run() {
        val output = ByteArrayOutputStream()
        execOps
            .exec { exec ->
                exec.commandLine(
                    "aws",
                    "codeartifact",
                    "get-repository-endpoint",
                    "--format",
                    "maven",
                    "--output",
                    "text",
                    "--repository",
                    repository.get(),
                    "--domain",
                    domain.get(),
                    *optionalArgPair("--domain-owner", domainOwner.orNull),
                    *optionalArgPair("--region", region.orNull),
                )
                exec.standardOutput = output
            }
            .assertNormalExitValue()
        val url = output.toString().trimEnd()

        val exp = System.currentTimeMillis() / 1000 + durationSeconds.get()
        output.reset()
        execOps
            .exec { exec ->
                exec.commandLine(
                    "aws",
                    "codeartifact",
                    "get-authorization-token",
                    "--query",
                    "authorizationToken",
                    "--output",
                    "text",
                    "--domain",
                    domain.get(),
                    "--duration-seconds",
                    durationSeconds.get(),
                    *optionalArgPair("--domain-owner", domainOwner.orNull),
                    *optionalArgPair("--region", region.orNull),
                )
                exec.standardOutput = output
            }
            .assertNormalExitValue()
        val token = output.toString().trimEnd()

        val props =
            Properties().apply {
                setProperty("url", url)
                setProperty("username", "aws")
                setProperty("password", token)
                setProperty("expiration", exp.toString())
            }
        val credsFile = credentialsFile.asFile.get()
        credsFile.outputStream().use { props.store(it, null) }

        project.logger.lifecycle("CodeArtifact credentials saved at {}.", credsFile)
    }

    companion object {
        const val GROUP = "codeArtifact"

        fun optionalArgPair(name: String, value: String?): Array<String> =
            value?.let { arrayOf(name, it) } ?: emptyArray()
    }
}
