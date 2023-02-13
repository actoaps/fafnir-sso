plugins {
    `java-library`
    `maven-publish`
    signing
}


java {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}


repositories {
    mavenCentral()
}


dependencies {
    implementation("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.26")

    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")

    implementation("io.vavr:vavr:0.10.4")
    annotationProcessor("io.vavr:vavr:0.10.4")

    compileOnly("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")

    implementation("com.hazelcast:hazelcast:5.2.1")
    implementation("com.google.guava:guava:31.1-jre")

    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    implementation("org.springframework:spring-context:6.0.4")
    implementation("org.springframework.security:spring-security-web:6.0.1")

    implementation("io.projectreactor:reactor-core:3.5.2")
    testImplementation("io.projectreactor:reactor-test:3.5.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "dk.acto"
            artifactId = "fafnir-client"
            from(components["java"])

            pom {
                name.set("Fafnir SSO Client")
                description.set("Client library for the Fafnir SSO solution, for use with Spring Boot")
                url.set("https://github.com/actoaps/fafnir-sso")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("EliasJorgensen")
                        name.set("Elias Jørgensen")
                        email.set("ej@acto.dk")
                    }
                }

                scm {
                    url.set("https://github.com/actoaps/fafnir-sso")
                    connection.set("scm:https://github.com/actoaps/fafnir-sso.git")
                    developerConnection.set("scm:git://github.com/actoaps/fafnir-sso.git")
                }
            }

        }

    }
}

signing {
    requireNotNull(project.version)
    val x = project.version as String
    require(!x.endsWith("-SNAPSHOT"))
    useInMemoryPgpKeys(findProperty("signingKey") as String?, "")
    sign(publishing.publications.findByName("mavenJava"))
}

tasks.test {
    ignoreFailures = false
    useJUnitPlatform()
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}
