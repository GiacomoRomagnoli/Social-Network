import com.lordcodes.turtle.shellRun
import io.github.zuccherosintattico.gradle.BuildCommandExecutable
import io.github.zuccherosintattico.utils.NodeCommandsExtension.npmCommand

plugins {
    alias(libs.plugins.typescript.gradle.plugin)
}

typescript {
    entrypoint = "src/main/typescript/main.js"
    tsConfig = "tsconfig.json"
    buildCommandExecutable = BuildCommandExecutable.NODE
    buildCommand = "node_modules/typescript/bin/tsc --build"
}

node {
    shouldInstall = true
    version = "22.1.0"
}

val commonsLibDirName = "commons-lib"
val piperKtCommonsCompiledPath = "src/main/typescript/$commonsLibDirName"

tasks.named("check") {
    doLast {
        runCatching {
            shellRun(project.projectDir) {
                npmCommand(project, "run", "type-checking")
            }
        }
            .onFailure { logger.error(it.stackTraceToString()) }
    }
}

tasks.named("npmDependencies") {
    dependsOn(":commons:jsNodeProductionLibraryDistribution")
    doFirst {
        copy {
            from("../commons/build/dist/js/productionLibrary")
            into(piperKtCommonsCompiledPath)
        }
    }
}

tasks.named("compileTypescript") {
    doLast {
        copy {
            from(piperKtCommonsCompiledPath)
            into("build/dist/$commonsLibDirName")
        }
    }
}