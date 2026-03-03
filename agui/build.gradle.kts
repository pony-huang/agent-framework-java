plugins {
    id("java-library")
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "github.ponyhuang.agentframework"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Project dependencies
    implementation(project(":core"))
    implementation(project(":agui-core"))
    implementation("io.projectreactor:reactor-core:3.8.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.1")
    implementation("ch.qos.logback:logback-classic:1.5.32")

    // Spring Boot (optional - for Spring integration)
    compileOnly("org.springframework.boot:spring-boot-starter-webflux:3.2.0")
    compileOnly("org.springframework:spring-web:6.1.1")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.2.0")

    // Vert.x (optional - for Vert.x integration)
    compileOnly("io.vertx:vertx-web:4.5.4")
    compileOnly("io.vertx:vertx-web-client:4.5.4")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("io.projectreactor:reactor-test:3.8.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}