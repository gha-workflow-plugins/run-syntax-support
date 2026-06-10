import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

val pluginChangeNotes = providers.fileContents(layout.projectDirectory.file("CHANGE_NOTES.html")).asText.get().trim()

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = "com.gha-workflow-plugins.run-syntax-support"
version = "1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create("IU", "2025.1.4.1")
        bundledPlugin("org.jetbrains.plugins.yaml")
        bundledPlugin("com.intellij.modules.json")

        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        testBundledPlugins("com.intellij.modules.json", "com.jetbrains.sh", "org.jetbrains.plugins.github", "org.toml.lang", "JavaScript")
        testPlugin("PythonCore", "251.26927.70")

    }

    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:3.27.7")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }

        changeNotes = pluginChangeNotes
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
}
