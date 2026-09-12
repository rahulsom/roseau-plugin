package com.github.rahulsom

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.io.File
import javax.inject.Inject

private const val REPORT = "--report"
private const val CONFIG = "--config"

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
            val jarTask = project.tasks.named("jar", Jar::class.java)
            val jarFile = jarTask.flatMap { it.archiveFile }
            val reportsDir = project.layout.buildDirectory.dir("reports/roseau")
            // Detached configurations bypass composite build substitution, ensuring
            // latest.release resolves from the remote repository rather than the local included build.
            val v1Dep = project.dependencies.create("${project.group}:${project.name}:latest.release")
            val v1Single = project.configurations.detachedConfiguration(v1Dep).also { it.isTransitive = false }
            val v1Full = project.configurations.detachedConfiguration(v1Dep)
            val v2Classpath = project.configurations.getByName("compileClasspath")
            // `.elements` (unlike `.asPath`/`.files`) is a Provider the configuration cache knows how
            // to serialize, so resolution stays deferred to doFirst without capturing the
            // Configuration object itself.
            val v1SingleElements = v1Single.elements
            val v1FullElements = v1Full.elements
            val v2ClasspathElements = v2Classpath.elements

            task.classpath = roseauConfiguration
            task.dependsOn(jarTask)
            task.mainClass.set("io.github.alien.roseau.cli.RoseauCLI")
            task.javaLauncher.set(
                getJavaToolchains().launcherFor {
                    it.languageVersion.set(JavaLanguageVersion.of(25))
                },
            )
            task.doFirst {
                task.args =
                    computeArgs(
                        initArgs,
                        extension,
                        reportsDir.get().asFile,
                        jarFile.get().asFile,
                        v1SingleElements.toPath(),
                        v1FullElements.toPath(),
                        v2ClasspathElements.toPath(),
                        task.logger,
                    )
            }
        }
    }

    private fun computeArgs(
        initArgs: Array<out String>,
        extension: RoseauExtension,
        reportsDir: File,
        jarFile: File,
        v1SinglePath: String,
        v1FullPath: String,
        v2ClasspathPath: String,
        logger: Logger,
    ): MutableList<String> {
        val args = mutableListOf(*initArgs)
        logger.info("V1 single jar: $v1SinglePath")
        mapOf(RoseauExtension.VerbosityLevel.SOME to "-v", RoseauExtension.VerbosityLevel.VERBOSE to "-vv").forEach { (level, flag) ->
            if (extension.verbosity.get() == level) {
                args.add(flag)
            }
        }
        args.addAll("--v1", v1SinglePath)
        args.addAll("--v2", jarFile.toString())
        args.addAll("--v1-classpath", v1FullPath)
        args.addAll("--v2-classpath", v2ClasspathPath)
        if (extension.html.get()) args.addAll(REPORT, "HTML=${reportsDir.resolve("report.html")}")
        if (extension.csv.get()) args.addAll(REPORT, "CSV=${reportsDir.resolve("report.csv")}")
        if (extension.md.get()) args.addAll(REPORT, "MD=${reportsDir.resolve("report.md")}")
        if (extension.cli.get()) args.addAll(REPORT, "CLI=${reportsDir.resolve("report.cli")}")
        if (extension.json.get()) args.addAll(REPORT, "JSON=${reportsDir.resolve("report.json")}")
        if (extension.plain.get()) args.add("--plain")
        resolveConfigFile(extension, reportsDir)?.let { args.addAll(CONFIG, it.toString()) }
        return args
    }

    /**
     * Roseau's own `--config` doesn't yet support excludes-by-build-script, so when a user
     * hasn't supplied their own config file, we generate one from [RoseauExtension.excludeNames].
     */
    private fun resolveConfigFile(
        extension: RoseauExtension,
        reportsDir: File,
    ): File? {
        if (extension.config.isPresent) return extension.config.get().asFile
        val excludeNames = extension.excludeNames.get()
        if (excludeNames.isEmpty()) return null
        val configFile = reportsDir.resolve("roseau.yaml")
        configFile.parentFile.mkdirs()
        configFile.writeText(
            buildString {
                appendLine("common:")
                appendLine("  excludes:")
                appendLine("    names:")
                excludeNames.forEach { pattern ->
                    appendLine("      - \"${pattern.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
                }
            },
        )
        return configFile
    }

    private fun <T> MutableList<T>.addAll(vararg elements: T) {
        elements.forEach { add(it) }
    }

    private fun Provider<Set<FileSystemLocation>>.toPath(): String = get().joinToString(File.pathSeparator) { it.asFile.absolutePath }
}