plugins {
    id("java-library")
    id("application")
}

application {
    mainClass.set("github.ponyhuang.agentframework.agui.example.AguiVertxServer")
}

sourceSets {
    main {
        java.srcDir("backend/src/main/java")
        resources.srcDir("frontend")
    }
}

group = "github.ponyhuang.agentframework"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    implementation(project(":agui-core"))
    implementation(project(":agui"))
    implementation("io.projectreactor:reactor-core:3.8.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.1")
    implementation("ch.qos.logback:logback-classic:1.5.32")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.2.0")

    implementation("io.vertx:vertx-web:4.5.4")
    implementation("io.vertx:vertx-web-client:4.5.4")
    implementation("io.vertx:vertx-core:4.5.4")

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

tasks.register("printClasspath") {
    doLast {
        println(sourceSets["main"].runtimeClasspath.asPath)
    }
}

tasks.register<JavaExec>("runServer") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("github.ponyhuang.agentframework.agui.example.AguiVertxServer")
}
