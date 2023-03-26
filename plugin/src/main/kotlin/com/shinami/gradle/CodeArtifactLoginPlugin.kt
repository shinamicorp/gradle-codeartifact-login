package com.shinami.gradle

import java.io.IOException
import java.util.Properties
import javax.inject.Inject
import org.gradle.api.*
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized

class CodeArtifactLoginPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit =
        project.run {
            val codeArtifact = extensions.create("codeArtifact", CodeArtifactExtension::class.java)
            val loginAllTask =
                tasks.register("codeArtifactLogin") {
                    it.group = CodeArtifactLoginTask.GROUP
                    it.description = "Logs in to all AWS CodeArtifact repositories."
                }

            (repositories as ExtensionAware)
                .extensions
                .create(
                    "codeArtifact",
                    CodeArtifactRepositoryHandler::class.java,
                    project,
                    codeArtifact,
                    loginAllTask
                )
        }
}

open class CodeArtifactExtension
@Inject
constructor(layout: ProjectLayout, objects: ObjectFactory) {
    val credentialsDir: DirectoryProperty =
        objects.directoryProperty().also {
            it.convention(layout.projectDirectory.dir(".codeartifact")).finalizeValueOnRead()
        }

    val domains: NamedDomainObjectContainer<CodeArtifactDomain> =
        objects.domainObjectContainer(CodeArtifactDomain::class.java)
}

open class CodeArtifactDomain @Inject constructor(val name: String, objects: ObjectFactory) {
    val domain: Property<String> =
        objects.property(String::class.java).also { it.convention(name).finalizeValueOnRead() }
    val owner: Property<String> =
        objects.property(String::class.java).also { it.finalizeValueOnRead() }
    val region: Property<String> =
        objects.property(String::class.java).also { it.finalizeValueOnRead() }
}

open class CodeArtifactRepositoryHandler
@Inject
constructor(
    private val project: Project,
    private val codeArtifact: CodeArtifactExtension,
    private val loginAllTask: TaskProvider<*>,
) {
    fun maven(configure: Action<in CodeArtifactMavenRepository>): MavenArtifactRepository {
        val repo =
            project.objects
                .newInstance(CodeArtifactMavenRepository::class.java)
                .also(configure::execute)
        val maven =
            project.repositories.maven { maven -> repo.name.orNull?.let { maven.name = it } }
        val credsFile = codeArtifact.credentialsDir.file("${maven.name}.properties")

        val loginTask =
            project.tasks.register(
                "${loginAllTask.name}${maven.name.capitalized()}",
                CodeArtifactLoginTask::class.java
            ) { task ->
                val caDomain =
                    codeArtifact.domains.getByName(
                        repo.domain.orNull
                            ?: error(
                                "CodeArtifact domain not configured in maven repository '${maven.name}'."
                            )
                    )

                task.repository.set(
                    repo.repository.orNull
                        ?: error(
                            "CodeArtifact repository not configured in maven repository '${maven.name}'."
                        )
                )
                task.domain.set(caDomain.domain)
                task.domainOwner.set(caDomain.owner)
                task.region.set(caDomain.region)
                task.credentialsFile.set(credsFile)
            }

        loginAllTask.configure { it.dependsOn(loginTask) }

        project.afterEvaluate { project ->
            val props = project.tryLoadProperties(credsFile)

            maven.url =
                props.getProperty("url")?.let(project::uri)
                    ?: run {
                        project.logger.warn(
                            "Failed to read {}. Run {} task to populate it.",
                            project.file(credsFile),
                            loginAllTask.name
                        )
                        project.repositories.remove(maven)
                        return@afterEvaluate
                    }
            maven.credentials {
                it.username = props.getProperty("username")
                it.password = props.getProperty("password")
            }
        }

        return maven
    }
}

open class CodeArtifactMavenRepository @Inject constructor(objects: ObjectFactory) {
    val name: Property<String> =
        objects.property(String::class.java).also { it.finalizeValueOnRead() }
    val domain: Property<String> =
        objects.property(String::class.java).also { it.finalizeValueOnRead() }
    val repository: Property<String> =
        objects.property(String::class.java).also { it.finalizeValueOnRead() }
}

fun Project.tryLoadProperties(path: Any): Properties =
    Properties().apply {
        try {
            file(path).inputStream().use { load(it) }
        } catch (_: IOException) {}
    }
