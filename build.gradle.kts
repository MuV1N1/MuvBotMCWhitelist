import java.util.Properties

plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val mcVersion = "26.2"

dependencies {
    compileOnly("io.papermc.paper:paper-api:${mcVersion}.build.+")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    jar {
        archiveBaseName.set("whitelist-mc${mcVersion}")
    }

    runServer {
        minecraftVersion(mcVersion)
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun localProp(key: String): String =
    localProps.getProperty(key)
        ?: System.getenv(key)
        ?: error("Missing required property: $key — set it in local.properties or as an env var")

val backendUrl: String = localProp("backendUrl")

val generatedDir = layout.buildDirectory.dir("generated/constants")

sourceSets.main { java.srcDir(generatedDir) }

abstract class GenerateConstantsTask : DefaultTask() {
    @get:Input
    abstract val backendUrl: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val pkg = outputDir.get().dir("de/muv1n/whitelist").asFile
        pkg.mkdirs()
        File(pkg, "BuildConstants.java").writeText(
            """
            package de.muv1n.whitelist;
            public final class BuildConstants {
                public static final String BACKEND_URL = "${backendUrl.get()}";
                private BuildConstants() {}
            }
            """.trimIndent()
        )
    }
}

val generateConstants by tasks.registering(GenerateConstantsTask::class) {
    backendUrl.set(localProp("backendUrl"))
    outputDir.set(generatedDir)
}

tasks.compileJava { dependsOn(generateConstants) }