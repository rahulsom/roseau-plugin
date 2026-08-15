package com.github.rahulsom

import org.gradle.api.Project
import org.gradle.api.provider.Property

open class RoseauExtension(
    project: Project,
) {
    /**
     * The version of Roseau to use. Defaults to 0.6.0
     */
    var version: Property<String> =
        project.objects
            .property(String::class.java)
            .convention("0.6.0")

    enum class VerbosityLevel {
        NONE,
        SOME,
        VERBOSE,
    }

    /**
     * The verbosity level. Defaults to NONE.
     * Permitted values are:
     * NONE - No output
     * SOME - Some output
     * VERBOSE - Verbose output
     */
    var verbosity =
        project.objects
            .property(VerbosityLevel::class.java)
            .convention(VerbosityLevel.NONE)

    /**
     * Whether to generate HTML report. Defaults to true.
     */
    var html = project.objects.property(Boolean::class.java).convention(true)

    /**
     * Whether to generate CSV report. Defaults to true.
     */
    var csv = project.objects.property(Boolean::class.java).convention(true)

    /**
     * Whether to generate MD report. Defaults to true.
     */
    var md = project.objects.property(Boolean::class.java).convention(true)

    /**
     * Whether to generate CLI report. Defaults to true.
     */
    var cli = project.objects.property(Boolean::class.java).convention(true)

    /**
     * Whether to generate JSON report. Defaults to true.
     */
    var json = project.objects.property(Boolean::class.java).convention(true)

    /**
     * Whether to generate plain text report to console. Defaults to false, i.e. ANSI-colored output.
     */
    var plain = project.objects.property(Boolean::class.java).convention(false)
}