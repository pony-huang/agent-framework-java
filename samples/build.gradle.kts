import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("java")
    id("application")
}

group = "github.ponyhuang.agentframework.samples"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("io.projectreactor:reactor-core:3.8.2")
    implementation("io.opentelemetry:opentelemetry-api:1.47.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.47.0")
    implementation("io.opentelemetry:opentelemetry-exporter-logging:1.47.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

application {
    mainClass.set("github.ponyhuang.agentframework.samples.A2AExample")
}
