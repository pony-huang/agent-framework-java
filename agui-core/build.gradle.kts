plugins {
    id("java-library")
}

group = "com.ag-ui"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    // Validation API
    implementation("javax.validation:validation-api:2.0.1.Final")

    // Test
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.7")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.test {
    useJUnitPlatform()
}