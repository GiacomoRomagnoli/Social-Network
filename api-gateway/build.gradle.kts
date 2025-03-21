plugins {
    kotlin("jvm")
    alias(libs.plugins.ktlint)
    alias(libs.plugins.shadowjar)
    application
    alias(libs.plugins.dokka)
}

repositories {
    mavenCentral()
}

dependencies {
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    implementation(libs.jjwt.api)
    implementation(libs.bcrypt)
    implementation(libs.kotlin.stdlib)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.vertx.core)
    implementation(libs.vertx.web)
    implementation(libs.vertx.web.client)
    implementation(libs.vertx.kafka.client)
    implementation(libs.jackson.core)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.mysql.connector.java)
    implementation(project(":commons"))
    implementation(project(":utils"))
    testImplementation(kotlin("test"))
    testImplementation(libs.archunit)
    testImplementation(libs.mockito.core)
}

project.setProperty("mainClassName", "social.gateway.MainKt")

tasks {
    listOf("distZip", "distTar", "startScripts").forEach {
        named(it) {
            dependsOn(shadowJar)
        }
    }

    test {
        useJUnitPlatform()
    }

    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors = true
    }
}
