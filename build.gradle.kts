plugins {
    id("java-library")
    id("maven-publish")
}

group = "com.archtest.cleanarch"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.tngtech.archunit:archunit:1.3.0")
}

tasks.test {
    useJUnitPlatform()
}
