plugins {
    jacoco
    id("org.sonarqube") version "3.5.0.2730"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

jacoco {
    toolVersion = "0.8.8"
}

group = "dk.acto"
val myVersion: String? = System.getProperty("versionOverride")
if (myVersion?.trim()?.isNotEmpty() == true) {
    project.version = myVersion
} else {
    project.version = "4.0-SNAPSHOT"
}

subprojects {
    plugins.withId("com.github.node-gradle-node") {
        sonar {
            properties {
                setProperty("sonar.sources", "src")
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype()
    }
}

tasks.getByName("initializeSonatypeStagingRepository") {
    shouldRunAfter(tasks.withType<Sign>())
}
