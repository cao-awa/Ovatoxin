import java.net.URI

plugins {
    kotlin("jvm") version "2.4.0"
    `java-library`
    `maven-publish`
}

group = "com.github.kusa233"
version = project.property("version") as String

val publicationGroupId = providers.gradleProperty("group_id").orElse(group.toString())
val publicationArtifactId = providers.gradleProperty("artifact_id").orElse(project.property("artifact_id") as String)
val publicationVersion = providers.gradleProperty("version_name").orElse(version.toString())

base {
    // Keep published file names in the form <artifactId>-<version>.<ext>.
    archivesName.set(publicationArtifactId)
}

repositories {
    mavenCentral()
    maven {
        url = URI.create("https://jitpack.io")
    }
}

val javafxVersion = "25.0.5"

// JavaFX ships platform specific artifacts; pick the classifier for this machine.
val javafxPlatform: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val aarch64 = arch.contains("aarch64") || arch.contains("arm64")
    when {
        os.contains("win") -> "win"
        os.contains("mac") -> if (aarch64) "mac-aarch64" else "mac"
        os.contains("linux") -> if (aarch64) "linux-aarch64" else "linux"
        else -> error("Unsupported platform for JavaFX: $os/$arch")
    }
}

dependencies {
    api("org.openjfx:javafx-base:$javafxVersion:$javafxPlatform")
    api("org.openjfx:javafx-graphics:$javafxVersion:$javafxPlatform")
    api("org.openjfx:javafx-controls:$javafxVersion:$javafxPlatform")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("gpr") {
            groupId = publicationGroupId.get()
            artifactId = publicationArtifactId.get()
            version = publicationVersion.get()

            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

/**
 * Runs the example.
 *
 * The demo lives in `src/test/kotlin`, so it never ends up in the published jar; this task
 * just puts the test runtime classpath on a JVM and starts it.
 *
 * Override the entry point with `-PdemoMain=...`, e.g. to run the rendering probe:
 * `gradlew demo -PdemoMain=com.github.kusa233.ovatoxin.example.RenderProbeKt`
 */
tasks.register<JavaExec>("demo") {
    group = "application"
    description = "Launches the demo program."
    mainClass = providers.gradleProperty("demoMain")
        .orElse("com.github.kusa233.ovatoxin.example.DemoKt")
    classpath = sourceSets["test"].runtimeClasspath
}
