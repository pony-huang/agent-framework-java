import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("java")
}

group = "github.ponyhuang.agentframework"
version = "1.0.0-beta"

repositories {
    mavenCentral()
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.5.32")
    implementation("com.anthropic:anthropic-java:2.13.0")
    implementation("com.openai:openai-java:4.22.0")
    implementation("io.projectreactor:reactor-core:3.8.2")
    implementation("io.opentelemetry:opentelemetry-api:1.47.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.47.0")
    implementation("io.opentelemetry:opentelemetry-exporter-logging:1.47.0")
    implementation("io.opentelemetry.semconv:opentelemetry-semconv:1.29.0-alpha") // Semantic conventions
    // MCP Java SDK
    implementation("io.modelcontextprotocol.sdk:mcp:1.0.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.11.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}