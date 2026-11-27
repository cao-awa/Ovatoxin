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

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.cao-awa:Cason:${project.property("cason_version")}")

    implementation("org.openjfx:javafx-base:${javafxVersion}:win")
    implementation("org.openjfx:javafx-graphics:${javafxVersion}:win")
    implementation("org.openjfx:javafx-controls:${javafxVersion}:win")
    implementation("org.openjfx:javafx-media:${javafxVersion}:win")
    implementation("org.openjfx:javafx-base:${javafxVersion}:mac")
    implementation("org.openjfx:javafx-graphics:${javafxVersion}:mac")
    implementation("org.openjfx:javafx-controls:${javafxVersion}:mac")
    implementation("org.openjfx:javafx-media:${javafxVersion}:mac")
    implementation("org.openjfx:javafx-base:${javafxVersion}:mac-aarch64")
    implementation("org.openjfx:javafx-graphics:${javafxVersion}:mac-aarch64")
    implementation("org.openjfx:javafx-controls:${javafxVersion}:mac-aarch64")
    implementation("org.openjfx:javafx-media:${javafxVersion}:mac-aarch64")
    implementation("org.openjfx:javafx-base:${javafxVersion}:linux")
    implementation("org.openjfx:javafx-graphics:${javafxVersion}:linux")
    implementation("org.openjfx:javafx-controls:${javafxVersion}:linux")
    implementation("org.openjfx:javafx-media:${javafxVersion}:linux")
    implementation("org.openjfx:javafx-base:${javafxVersion}:linux-aarch64")
    implementation("org.openjfx:javafx-graphics:${javafxVersion}:linux-aarch64")
    implementation("org.openjfx:javafx-controls:${javafxVersion}:linux-aarch64")
    implementation("org.openjfx:javafx-media:${javafxVersion}:linux-aarch64")
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
        .orElse("com.github.kusa233.ovatoxin.example.full.FullSample")
    classpath = sourceSets["test"].runtimeClasspath
}
