plugins {
    java
    application
    id("org.springframework.boot") version "3.0.2"
    id("io.spring.dependency-management") version "1.1.0"
}

java {
    targetCompatibility = JavaVersion.VERSION_21
    sourceCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass.set("dk.acto.fafnir.iam.Iam")
    applicationDefaultJvmArgs = listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:6001")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")

    implementation(project(":ui"))
    implementation(project(":fafnir-client"))

    implementation("jakarta.validation:jakarta.validation-api:3.0.2")

    implementation("io.vavr:vavr:0.10.4")
    annotationProcessor("io.vavr:vavr:0.10.4")
    implementation("com.google.guava:guava:31.1-jre")

    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-codec:commons-codec:1.15")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-mustache")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("com.hazelcast:hazelcast:5.2.1")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation(project(":fafnir-client"))
}

tasks.test {
    ignoreFailures = false
    useJUnitPlatform()
}

tasks.compileJava {
    options.encoding = "UTF-8"
}
