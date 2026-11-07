plugins {
    base
    alias(libs.plugins.nebulaRelease)
    alias(libs.plugins.spotless)
}

allprojects {
    group = "io.github.rahulsom"
}

repositories {
    mavenCentral()
}

// Configure all repository files here, including sources in subprojects.
spotless {
    val generatedFiles = listOf("**/build/**", "**/.gradle/**", "**/.git/**", "**/.idea/**", "**/.kotlin/**")

    fun repositoryFiles(vararg patterns: String) =
        fileTree(rootDir) {
            include(*patterns)
            exclude(generatedFiles)
        }

    kotlin {
        target(repositoryFiles("**/*.kt"))
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target(repositoryFiles("**/*.gradle.kts"))
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
    json {
        target(repositoryFiles("**/*.json"))
        gson().indentWithSpaces(4)
        endWithNewline()
    }
    yaml {
        target(repositoryFiles("**/*.yml", "**/*.yaml"))
        prettier().config(mapOf("tabWidth" to 2))
        endWithNewline()
    }
    format("misc") {
        target(
            repositoryFiles(
                "**/*.toml",
                "**/*.properties",
                "**/*.adoc",
                ".editorconfig",
                ".gitattributes",
                ".gitignore",
                ".git-blame-ignore-revs",
            ),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Also enforce repository-wide formatting when checking the plugin directly.
project(":plugin") {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(rootProject.tasks.named("spotlessCheck"))
    }
}
