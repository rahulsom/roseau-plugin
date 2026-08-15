package com.github.rahulsom

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class RoseauPluginTest {
    @Test fun `plugin registers task`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.github.rahulsom.roseau")
        project.plugins.apply("java")

        // Verify the result
        assertNotNull(project.tasks.findByName("roseau"))
    }
}
