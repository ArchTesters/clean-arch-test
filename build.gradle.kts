plugins {
    id("java")
}

group = "com.archtest.cleanarch"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.tngtech.archunit:archunit:1.3.0")
}

tasks.test {
    useJUnitPlatform()
}