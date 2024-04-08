plugins {
    `java-library`
    id("io.miret.etienne.sass") version "1.5.0"
    id("com.magnetichq.gradle.css") version "3.0.2"
}

java {
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.compileSass {
    outputDir = project.layout.buildDirectory.file("tmp/css").get().asFile
    sourceDir = project.file("${projectDir}/src/scss")

    finalizedBy(tasks.minifyCss)
}

tasks.minifyCss {
    source = project.layout.buildDirectory.dir("tmp/css/styles.css").get().asFileTree
    setDest(project.layout.buildDirectory.file("tmp/css/styles.css").get().asFile)
}

tasks.processResources {
    dependsOn(tasks.compileSass)
}

