import com.adarshr.gradle.testlogger.theme.ThemeType

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.gradlePublish)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.testLogger)
}

repositories {
    mavenCentral()
}

testing {
    suites {
        val test = named<JvmTestSuite>("test") {
            useKotlinTest("2.4.0")
        }

        val functionalTest = register<JvmTestSuite>("functionalTest") {
            useKotlinTest("2.4.0")

            dependencies {
                implementation(project())
            }

            targets {
                all {
                    // This test suite should run after the built-in test suite has run its tests
                    testTask.configure { shouldRunAfter(test) }
                }
            }
        }
    }
}

gradlePlugin {
    val roseau = plugins.create("roseau") {
        id = "com.github.rahulsom.roseau"
        implementationClass = "com.github.rahulsom.RoseauPlugin"
    }
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

tasks.named<Task>("check") {
    dependsOn(testing.suites.named("functionalTest"))
}

dependencies {
    testImplementation(libs.assertj)
    "functionalTestImplementation"(libs.assertj)
}

testlogger {
    theme = ThemeType.MOCHA
}

listOf("final", "candidate").forEach { taskName ->
    rootProject.tasks.named(taskName).configure {
        dependsOn(tasks.named("publishPlugins"))
    }
}

tasks.named("publishPlugins").configure {
    mustRunAfter("check")
}