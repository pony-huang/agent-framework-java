plugins {
    id("java")
    id("application")
}

group = "com.microsoft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // agent-framework-java (project reference)
    implementation(project(":"))

    // LLM providers
    implementation("com.anthropic:anthropic-java:2.13.0")
    implementation("com.openai:openai-java:4.22.0")

    // Reactive
    implementation("io.projectreactor:reactor-core:3.8.2")

    // Observability
    implementation("io.opentelemetry:opentelemetry-api:1.47.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.47.0")

    // MCP
    implementation("io.modelcontextprotocol.sdk:mcp:1.0.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.18")

    // YAML
    implementation("org.yaml:snakeyaml:2.2")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")

    // CLI
    implementation("info.picocli:picocli:4.7.6")
    implementation("info.picocli:picocli-spring-boot-starter:4.7.6")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.14.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.0")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.microsoft.traeagent.cli.TraeAgentCLI")
}