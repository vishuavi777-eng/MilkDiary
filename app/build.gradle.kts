plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.rudrainfotech"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val javafxVersion = "23.0.1"
val hibernateVer = "6.5.2.Final"

dependencies {
    // JavaFX
    implementation("org.openjfx:javafx-controls:$javafxVersion")
    implementation("org.openjfx:javafx-fxml:$javafxVersion")

    // Hibernate / JPA
    implementation("org.hibernate.orm:hibernate-core:$hibernateVer")
    implementation("org.hibernate.orm:hibernate-community-dialects:$hibernateVer")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")

    // DB + pool
    implementation("org.xerial:sqlite-jdbc:3.46.0.1")
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Migrations
    implementation("org.flywaydb:flyway-core:10.16.0")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.13")

    // PDF generation
    implementation("com.github.librepdf:openpdf:1.3.39")


    // Tests (keep JUnit 5)
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    // ↳ match your package/class
    mainClass.set("com.rudrainfotech.milkdiary.MainApp")
}

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23)) // use JDK 23
    }
}

tasks.test {
    useJUnitPlatform()
}


/*
plugins {
    id("java")
}

group = "com.rudrainfotech"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}*/
