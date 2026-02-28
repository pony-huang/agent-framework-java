plugins {
    id("java")
}

group = "org.agent.framework"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.anthropic:anthropic-java:2.13.0")
    implementation("com.openai:openai-java:4.22.0")
    implementation("io.projectreactor:reactor-core:3.8.2")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}