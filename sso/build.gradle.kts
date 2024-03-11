import de.undercouch.gradle.tasks.download.Download
import io.mateo.cxf.codegen.wsdl2java.Wsdl2Java

plugins {
    java
    application
    id("de.undercouch.download") version "5.3.1"
    id("org.springframework.boot") version "3.0.2"
    id("io.spring.dependency-management") version "1.1.0"
    id("io.mateo.cxf-codegen") version "1.0.3"
}

java {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("dk.acto.fafnir.sso.Sso")
    applicationDefaultJvmArgs = listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:6001")
}

repositories {
    mavenCentral()
    maven { setUrl("https://build.shibboleth.net/maven/releases/") } // For OpenSAML
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.26")

    implementation(project(":ui"))
    implementation(project(":fafnir-client"))

    implementation("io.vavr:vavr:0.10.4")
    annotationProcessor("io.vavr:vavr:0.10.4")
    implementation("com.google.guava:guava:31.1-jre")

    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-codec:commons-codec:1.15")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-mustache")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-saml2-service-provider")

    implementation("org.opensaml:opensaml-core:4.3.0")
    implementation("org.opensaml:opensaml-saml-api:4.3.0")
    implementation("org.opensaml:opensaml-saml-impl:4.3.0")

    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("com.hazelcast:hazelcast:5.2.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation(project(":fafnir-client"))

    implementation("org.hibernate.validator:hibernate-validator:8.0.0.Final")
    implementation("org.glassfish:jakarta.el:5.0.0-M1")

    implementation("com.github.scribejava:scribejava-apis:8.3.3")
    implementation("com.github.scribejava:scribejava-httpclient-okhttp:8.3.3")

    implementation("com.auth0:java-jwt:4.2.1")

    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:4.0.0")
    implementation("org.apache.cxf:cxf-rt-transports-http:4.0.0")
    implementation("org.apache.cxf:cxf-rt-ws-policy:4.0.0")
    implementation("org.apache.cxf:cxf-rt-ws-security:4.0.0")

    implementation ("org.springframework.boot:spring-boot-starter-mustache")
    

    // WSDL / Apache-cxf stuff
    implementation("jakarta.xml.ws:jakarta.xml.ws-api:4.0.0")
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")
    cxfCodegen(platform("org.apache.cxf:cxf-bom:4.0.0"))
    cxfCodegen("org.apache.cxf:cxf-rt-frontend-jaxws:4.0.0")
    cxfCodegen("org.apache.cxf:cxf-rt-transports-http:4.0.0")
    cxfCodegen("org.apache.cxf:cxf-core:4.0.0")
    cxfCodegen("org.apache.cxf:cxf-tools-common:4.0.0")
    cxfCodegen("org.apache.cxf:cxf-tools-wsdlto-core:4.0.0")
    cxfCodegen("org.apache.cxf:cxf-tools-wsdlto-databinding-jaxb:4.0.0")
    cxfCodegen("org.apache.cxf:cxf-tools-wsdlto-frontend-jaxws:4.0.0")
    cxfCodegen("org.apache.cxf:cxf-tools-wsdlto-frontend-javascript:4.0.0")
}


tasks.register<Download>("downloadWSDLWsiBruger") {
    src("https://wsibruger.unilogin.dk/wsibruger-v6/ws?WSDL")
    dest("src/main/resources/wsdl/wsibruger_v6.wsdl")
}

tasks.register<Download>("downloadWSDLWsiInst") {
    src("https://wsiinst.unilogin.dk/wsiinst-v5/ws?WSDL")
    dest("src/main/resources/wsdl/wsiinst_v5.wsdl")
}

tasks.register<Wsdl2Java>("genWsiBruger") {
    toolOptions {
        markGenerated.set(true)
        outputDir.set(file("$buildDir/generated/sources/wsdl"))
        wsdl.set(file("src/main/resources/wsdl/wsibruger_v6.wsdl"))
        encoding.set("UTF-8")
    }

    dependsOn(tasks.getByName("downloadWSDLWsiBruger"))
}

tasks.register<Wsdl2Java>("genWsiInst") {
    toolOptions {
        markGenerated.set(true)
        outputDir.set(file("$buildDir/generated/sources/wsdl"))
        wsdl.set(file("src/main/resources/wsdl/wsiinst_v5.wsdl"))
        encoding.set("UTF-8")
    }

    dependsOn(tasks.getByName("downloadWSDLWsiInst"))
}

tasks.compileJava {
    options.encoding = "UTF-8"
    dependsOn(tasks.wsdl2java)
}
