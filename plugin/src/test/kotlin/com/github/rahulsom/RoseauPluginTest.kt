package com.github.rahulsom

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.tasks.JavaExec
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
        assertNotNull(roseauTask)
        assertEquals("io.github.alien.roseau.cli.RoseauCLI", roseauTask.mainClass.get())

        val roseauCheckTask = project.tasks.findByName("roseauCheck") as? JavaExec
        assertNotNull(roseauCheckTask)
        assertEquals("io.github.alien.roseau.cli.RoseauCLI", roseauCheckTask.mainClass.get())

        val config = project.configurations.findByName("roseau")
        assertNotNull(config)
        assertThat(roseauTask.classpath).containsAll(config)
        assertThat(roseauCheckTask.classpath).containsAll(config)

        val jarTask = project.tasks.findByName("jar")
        assertThat(roseauTask.taskDependencies.getDependencies(roseauTask)).contains(jarTask)
        assertThat(roseauCheckTask.taskDependencies.getDependencies(roseauCheckTask)).contains(jarTask)

        val dependency = config.dependencies.firstOrNull()
        assertNotNull(dependency)
        assertEquals("io.github.alien-tools", dependency.group)
        assertEquals("roseau-cli", dependency.name)
        assertEquals("0.6.0", dependency.version)
    }

    @Test
    fun `extension has expected defaults`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.rahulsom.roseau")

        val extension = project.extensions.findByType(RoseauExtension::class.java)
        assertNotNull(extension)
        assertEquals("0.6.0", extension.version.get())
        assertEquals(RoseauExtension.VerbosityLevel.NONE, extension.verbosity.get())
        assertTrue(extension.html.get())
        assertTrue(extension.csv.get())
        assertTrue(extension.md.get())
        assertTrue(extension.cli.get())
        assertTrue(extension.json.get())
        assertEquals(false, extension.plain.get())
    }

    @Test
    fun `computeArgs generates default arguments for roseau task`() {
        val project = createProject()
        project.plugins.apply("java")
        project.plugins.apply("io.github.rahulsom.roseau")

        val task = project.tasks.getByName("roseau") as JavaExec
        executeDoFirst(task)

        val args = task.args
        assertNotNull(args)
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
        assertNotNull(args)
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
    fun `verbosity level enum values`() {
        val values = RoseauExtension.VerbosityLevel.values()
        assertThat(values).containsExactly(
            RoseauExtension.VerbosityLevel.NONE,
            RoseauExtension.VerbosityLevel.SOME,
            RoseauExtension.VerbosityLevel.VERBOSE,
        )
        assertEquals(
            RoseauExtension.VerbosityLevel.SOME,
            RoseauExtension.VerbosityLevel.valueOf("SOME"),
        )
    }
}
