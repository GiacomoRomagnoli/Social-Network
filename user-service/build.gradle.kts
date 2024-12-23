plugins {
    kotlin("jvm")
    alias(libs.plugins.ktlint)
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.vertx.core)
    implementation(libs.vertx.web)
    implementation(libs.vertx.web.client)
    implementation(project(":commons"))
    implementation("mysql:mysql-connector-java:8.0.33")
    testImplementation(kotlin("test"))
    testImplementation(libs.archunit)
    testImplementation(libs.mockito.core)
}

project.setProperty("mainClassName", "social.user.MainKt")

tasks {
    listOf("distZip", "distTar", "startScripts").forEach {
        named(it) {
            dependsOn(shadowJar)
        }
    }

    test {
        useJUnitPlatform()
        exclude("**/UserSQLRepositoryTest.class")
        exclude("**/RESTUserAPIVerticleTest.class")
    }

    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("") // ensure the shadow JAR has no classifier
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors = true
    }
}
