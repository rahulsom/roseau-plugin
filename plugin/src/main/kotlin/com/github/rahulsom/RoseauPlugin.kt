package com.github.rahulsom

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import javax.inject.Inject

private const val REPORT = "--report"

abstract class RoseauPlugin : Plugin<Project> {
    @Inject
    abstract fun getJavaToolchains(): org.gradle.jvm.toolchain.JavaToolchainService

    override fun apply(project: Project) {
        val extension = project.extensions.create("roseau", RoseauExtension::class.java, project)
        val roseauConfiguration = project.configurations.create("roseau")
        project.dependencies.add(roseauConfiguration.name, "io.github.alien-tools:roseau-cli:${extension.version.get()}")

        buildTask(project, roseauConfiguration, extension, "roseau", "--diff")
        buildTask(project, roseauConfiguration, extension, "roseauCheck", "--diff", "--fail-on-bc")
    }

    private fun buildTask(
        project: Project,
        roseauConfiguration: Configuration,
        extension: RoseauExtension,
        name: String,
        vararg initArgs: String,
    ) {
        project.tasks.register(name, JavaExec::class.java) { task ->
            task.classpath = roseauConfiguration
            val jarTask = project.tasks.named("jar", Jar::class.java)
            task.dependsOn(jarTask)
            task.mainClass.set("io.github.alien.roseau.cli.RoseauCLI")
            task.javaLauncher.set(
                getJavaToolchains().launcherFor {
                    it.languageVersion.set(JavaLanguageVersion.of(25))
                },
            )
            task.doFirst {
                val jar = jarTask.get()
                task.args =
                    computeArgs(initArgs, project, extension, jar, task.logger)
            }
        }
    }

    private fun computeArgs(
        initArgs: Array<out String>,
        project: Project,
        extension: RoseauExtension,
        jar: Jar,
        logger: Logger,
    ): MutableList<String> {
        val args = mutableListOf(*initArgs)
        val buildDirectory = project.layout.buildDirectory
        val reportsDir = buildDirectory.dir("reports/roseau").get().asFile
        // Detached configurations bypass composite build substitution, ensuring
        // latest.release resolves from the remote repository rather than the local included build.
        val v1Dep = project.dependencies.create("${project.group}:${project.name}:latest.release")
        val v1Single = project.configurations.detachedConfiguration(v1Dep).also { it.isTransitive = false }
        val v1Full = project.configurations.detachedConfiguration(v1Dep)
        logger.info("V1 single jar: ${v1Single.asPath}")
        mapOf(RoseauExtension.VerbosityLevel.SOME to "-v", RoseauExtension.VerbosityLevel.VERBOSE to "-vv").forEach { (level, flag) ->
            if (extension.verbosity.get() == level) {
                args.add(flag)
            }
        }
        args.addAll("--v1", v1Single.asPath)
        val jarFile = jar.archiveFile.get().asFile
        args.addAll("--v2", jarFile.toString())
        args.addAll("--v1-classpath", v1Full.asPath)
        args.addAll("--v2-classpath", project.configurations.getByName("compileClasspath").asPath)
        if (extension.html.get()) args.addAll(REPORT, "HTML=${reportsDir.resolve("report.html")}")
        if (extension.csv.get()) args.addAll(REPORT, "CSV=${reportsDir.resolve("report.csv")}")
        if (extension.md.get()) args.addAll(REPORT, "MD=${reportsDir.resolve("report.md")}")
        if (extension.cli.get()) args.addAll(REPORT, "CLI=${reportsDir.resolve("report.cli")}")
        if (extension.json.get()) args.addAll(REPORT, "JSON=${reportsDir.resolve("report.json")}")
        if (extension.plain.get()) args.add("--plain")
        return args
    }

    private fun <T> MutableList<T>.addAll(vararg elements: T) {
        elements.forEach { add(it) }
    }
}