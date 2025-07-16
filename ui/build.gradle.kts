plugins {
    `java-library`
    id("io.miret.etienne.sass") version "1.5.0"
    id("com.magnetichq.gradle.css") version "3.0.2"
}

java {
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.compileSass {
    outputDir = project.file("${buildDir}/tmp/css")
    setSourceDir(project.file("${projectDir}/src/scss"))
}

tasks.minifyCss {
    source = project.fileTree("${buildDir}/tmp/css/styles.css")
    setDest("${projectDir}/src/main/resources/static/css/styles.css")
}

tasks.processResources {
    dependsOn(tasks.minifyCss)
}

