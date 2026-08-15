package com.github.rahulsom

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class RoseauPluginFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    @Test fun `can run task`() {
        // Set up the test build
        settingsFile.writeText(
            @Language("kotlin")
            """
            rootProject.name = "nothing-java"
            """,
        )
        buildFile.writeText(
            @Language("kotlin")
            """
            plugins {
                id("com.github.rahulsom.roseau")
                id("java")
            }
            repositories {
                mavenCentral()
            }
            group = "com.github.rahulsom"
            """,
        )

        // Run the build
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("roseau")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        // Verify the result
        assertThat(result.output)
            .contains("Breaking Changes found:")
            .contains("1 (1 binary-breaking, 1 source-breaking)")
            .contains("✗ com.github.rahulsom.nothing.java.Foo TYPE_REMOVED")
            .contains("✗ binary-breaking ✗ source-breaking")
            .contains("→ com/github/rahulsom/nothing/java/Foo.java:-1")
    }
}