plugins {
    id("java")
}

group = "github.ponyhuang.agentframework"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    implementation(project(":agui-core"))
    implementation("com.anthropic:anthropic-java:2.15.0")
    implementation("com.openai:openai-java:4.23.0")
    implementation("io.projectreactor:reactor-core:3.8.2")
    implementation("io.opentelemetry:opentelemetry-api:1.47.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.47.0")
    implementation("io.modelcontextprotocol.sdk:mcp:1.0.0")
    implementation("ch.qos.logback:logback-classic:1.5.32")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.14.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.0")
}

tasks.test {
    useJUnitPlatform()
}