pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.8"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

gitHooks {
    preCommit {
        tasks("ktlintCheck")
    }
    commitMsg { conventionalCommits() }
    createHooks()
}

rootProject.name = "Social-Network"

include("commons")
include("user-service")
