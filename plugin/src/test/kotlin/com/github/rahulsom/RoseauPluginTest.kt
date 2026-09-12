package com.github.rahulsom

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.tasks.JavaExec
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test

class RoseauPluginTest {

    private fun createProject(): org.gradle.api.Project {
        val project = ProjectBuilder.builder().withName("nothing-java").build()
        project.group = "com.github.rahulsom"
        project.repositories.mavenCentral()
        return project
    }

    private fun executeDoFirst(task: JavaExec) {
        task.actions.first().execute(task)
    }

    @Test
    fun `plugin registers tasks and configuration`() {
        val project = createProject()
        project.plugins.apply("java")
        project.plugins.apply("io.github.rahulsom.roseau")

        val roseauTask = project.tasks.findByName("roseau") as? JavaExec
        assertThat(roseauTask).isNotNull()
        assertThat(roseauTask?.mainClass?.get()).isEqualTo("io.github.alien.roseau.cli.RoseauCLI")

        val roseauCheckTask = project.tasks.findByName("roseauCheck") as? JavaExec
        assertThat(roseauCheckTask).isNotNull()
        assertThat(roseauCheckTask?.mainClass?.get()).isEqualTo("io.github.alien.roseau.cli.RoseauCLI")

        val config = project.configurations.findByName("roseau")
        assertThat(config).isNotNull()
        assertThat(roseauTask?.classpath).containsAll(config)
        assertThat(roseauCheckTask?.classpath).containsAll(config)

        val jarTask = project.tasks.findByName("jar")
        assertThat(roseauTask?.taskDependencies?.getDependencies(roseauTask)).contains(jarTask)
        assertThat(roseauCheckTask?.taskDependencies?.getDependencies(roseauCheckTask)).contains(jarTask)

        val dependency = config?.dependencies?.firstOrNull()
        assertThat(dependency).isNotNull()
        assertThat(dependency?.group).isEqualTo("io.github.alien-tools")
        assertThat(dependency?.name).isEqualTo("roseau-cli")
        assertThat(dependency?.version).isEqualTo("0.6.0")
    }

    @Test
    fun `extension has expected defaults`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.rahulsom.roseau")

        val extension = project.extensions.findByType(RoseauExtension::class.java)
        assertThat(extension).isNotNull()
        assertThat(extension?.version?.get()).isEqualTo("0.6.0")
        assertThat(extension?.verbosity?.get()).isEqualTo(RoseauExtension.VerbosityLevel.NONE)
        assertThat(extension?.html?.get()).isTrue()
        assertThat(extension?.csv?.get()).isTrue()
        assertThat(extension?.md?.get()).isTrue()
        assertThat(extension?.cli?.get()).isTrue()
        assertThat(extension?.json?.get()).isTrue()
        assertThat(extension?.plain?.get()).isFalse()
    }

    @Test
    fun `computeArgs generates default arguments for roseau task`() {
        val project = createProject()
        project.plugins.apply("java")
        project.plugins.apply("io.github.rahulsom.roseau")

        val task = project.tasks.getByName("roseau") as JavaExec
        executeDoFirst(task)

        val args = task.args
        assertThat(args).isNotNull()
        assertThat(args).containsSequence("--diff")
        assertThat(args).contains("--v1", "--v2", "--v1-classpath", "--v2-classpath")
        assertThat(args).contains(
            "--report",
            "HTML=${project.layout.buildDirectory.get().asFile.resolve("reports/roseau/report.html")}",
            "--report",
            "CSV=${project.layout.buildDirectory.get().asFile.resolve("reports/roseau/report.csv")}",
            "--report",
            "MD=${project.layout.buildDirectory.get().asFile.resolve("reports/roseau/report.md")}",
            "--report",
            "CLI=${project.layout.buildDirectory.get().asFile.resolve("reports/roseau/report.cli")}",
            "--report",
            "JSON=${project.layout.buildDirectory.get().asFile.resolve("reports/roseau/report.json")}",
        )
        assertThat(args).doesNotContain("-v", "-vv", "--plain")
    }

    @Test
    fun `computeArgs generates expected initArgs for roseauCheck task`() {
        val project = createProject()
        project.plugins.apply("java")
        project.plugins.apply("io.github.rahulsom.roseau")

        val task = project.tasks.getByName("roseauCheck") as JavaExec
        executeDoFirst(task)

        val args = task.args
        assertThat(args).isNotNull()
        assertThat(args).containsSubsequence("--diff", "--fail-on-bc")
    }

    @Test
    fun `computeArgs with verbosity SOME`() {
        val project = createProject()
        project.plugins.apply("java")
        project.plugins.apply("io.github.rahulsom.roseau")

        val extension = project.extensions.getByType(RoseauExtension::class.java)
        extension.verbosity.set(RoseauExtension.VerbosityLevel.SOME)

        val task = project.tasks.getByName("roseau") as JavaExec
        executeDoFirst(task)

        assertThat(task.args).contains("-v")
        assertThat(task.args).doesNotContain("-vv")
    }

    @Test
    fun `computeArgs with verbosity VERBOSE`() {
        val project = createProject()
        project.plugins.apply("java")
        project.plugins.apply("io.github.rahulsom.roseau")

        val extension = project.extensions.getByType(RoseauExtension::class.java)
        extension.verbosity.set(RoseauExtension.VerbosityLevel.VERBOSE)

        val task = project.tasks.getByName("roseau") as JavaExec
        executeDoFirst(task)

        assertThat(task.args).contains("-vv")
        assertThat(task.args).doesNotContain("-v")
    }

    @Test
    fun `computeArgs with reports individually disabled`() {
        // Test HTML disabled
        val p1 = createProject()
        p1.plugins.apply("java")
        p1.plugins.apply("io.github.rahulsom.roseau")
        val ext1 = p1.extensions.getByType(RoseauExtension::class.java)
        ext1.html.set(false)
        val t1 = p1.tasks.getByName("roseau") as JavaExec
        executeDoFirst(t1)
        assertThat(t1.args).noneMatch { it.startsWith("HTML=") }

        // Test CSV disabled
        val p2 = createProject()
        p2.plugins.apply("java")
        p2.plugins.apply("io.github.rahulsom.roseau")
        val ext2 = p2.extensions.getByType(RoseauExtension::class.java)
        ext2.csv.set(false)
        val t2 = p2.tasks.getByName("roseau") as JavaExec
        executeDoFirst(t2)
        assertThat(t2.args).noneMatch { it.startsWith("CSV=") }

        // Test MD disabled
        val p3 = createProject()
        p3.plugins.apply("java")
        p3.plugins.apply("io.github.rahulsom.roseau")
        val ext3 = p3.extensions.getByType(RoseauExtension::class.java)
        ext3.md.set(false)
        val t3 = p3.tasks.getByName("roseau") as JavaExec
        executeDoFirst(t3)
        assertThat(t3.args).noneMatch { it.startsWith("MD=") }

        // Test CLI disabled
        val p4 = createProject()
        p4.plugins.apply("java")
        p4.plugins.apply("io.github.rahulsom.roseau")
        val ext4 = p4.extensions.getByType(RoseauExtension::class.java)
        ext4.cli.set(false)
        val t4 = p4.tasks.getByName("roseau") as JavaExec
        executeDoFirst(t4)
        assertThat(t4.args).noneMatch { it.startsWith("CLI=") }

        // Test JSON disabled
        val p5 = createProject()
        p5.plugins.apply("java")
        p5.plugins.apply("io.github.rahulsom.roseau")
        val ext5 = p5.extensions.getByType(RoseauExtension::class.java)
        ext5.json.set(false)
        val t5 = p5.tasks.getByName("roseau") as JavaExec
        executeDoFirst(t5)
        assertThat(t5.args).noneMatch { it.startsWith("JSON=") }
    }

    @Test
    fun `computeArgs with plain set to true`() {
        val project = createProject()
        project.plugins.apply("java")
        project.plugins.apply("io.github.rahulsom.roseau")

        val extension = project.extensions.getByType(RoseauExtension::class.java)
        extension.plain.set(true)

        val task = project.tasks.getByName("roseau") as JavaExec
        executeDoFirst(task)

        assertThat(task.args).contains("--plain")
    }

    @Test
    fun `computeArgs generates roseau yaml from excludeNames`() {
        val project = createProject()
        project.plugins.apply("java")
        project.plugins.apply("io.github.rahulsom.roseau")

        val extension = project.extensions.getByType(RoseauExtension::class.java)
        extension.excludeNames.set(listOf(".*\\$.*Converter"))

        val task = project.tasks.getByName("roseau") as JavaExec
        executeDoFirst(task)

        val configFile = project.layout.buildDirectory.get().asFile.resolve("reports/roseau/roseau.yaml")
        assertThat(task.args).contains("--config", configFile.toString())
        assertThat(configFile).exists()
        assertThat(configFile.readText())
            .contains("common:")
            .contains("excludes:")
            .contains("names:")
            .contains(".*\\\\\$.*Converter")
    }

    @Test
    fun `computeArgs without excludeNames omits config`() {
        val project = createProject()
        project.plugins.apply("java")
        project.plugins.apply("io.github.rahulsom.roseau")

        val task = project.tasks.getByName("roseau") as JavaExec
        executeDoFirst(task)

        assertThat(task.args).doesNotContain("--config")
    }

    @Test
    fun `computeArgs prefers explicit config file over excludeNames`() {
        val project = createProject()
        project.plugins.apply("java")
        project.plugins.apply("io.github.rahulsom.roseau")

        val extension = project.extensions.getByType(RoseauExtension::class.java)
        extension.excludeNames.set(listOf(".*\\$.*Converter"))
        val userConfig = project.layout.projectDirectory.file("my-roseau.yaml").asFile
        userConfig.writeText("common: {}")
        extension.config.set(userConfig)

        val task = project.tasks.getByName("roseau") as JavaExec
        executeDoFirst(task)

        assertThat(task.args).contains("--config", userConfig.toString())
        val generated = project.layout.buildDirectory.get().asFile.resolve("reports/roseau/roseau.yaml")
        assertThat(generated).doesNotExist()
    }

    @Test
    fun `verbosity level enum values`() {
        val values = RoseauExtension.VerbosityLevel.values()
        assertThat(values).containsExactly(
            RoseauExtension.VerbosityLevel.NONE,
            RoseauExtension.VerbosityLevel.SOME,
            RoseauExtension.VerbosityLevel.VERBOSE,
        )
        assertThat(RoseauExtension.VerbosityLevel.valueOf("SOME"))
            .isEqualTo(RoseauExtension.VerbosityLevel.SOME)
    }
}
