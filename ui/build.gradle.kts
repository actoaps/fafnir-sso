plugins {
    `java-library`
    id("io.miret.etienne.sass") version "1.4.2"
    id("com.magnetichq.gradle.css") version "3.0.2"
}

tasks.compileSass {
    outputDir = project.file("${buildDir}/tmp/css")
    setSourceDir(project.file("${projectDir}/src/scss"))

    finalizedBy(tasks.minifyCss)
}

tasks.minifyCss {
    source = project.fileTree("${buildDir}/tmp/css/styles.css")
    setDest("${buildDir}/resources/main/static/css/styles.css")
}

tasks.processResources {
    dependsOn(tasks.compileSass)
}

