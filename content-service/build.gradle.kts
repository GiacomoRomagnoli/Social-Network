import com.lordcodes.turtle.shellRun
import io.github.zuccherosintattico.gradle.BuildCommandExecutable
import io.github.zuccherosintattico.utils.NodeCommandsExtension.nodeCommand

plugins {
    alias(libs.plugins.typescript.gradle.plugin)
}

typescript {
    outputDir = "build/dist/main/typescript"
    entrypoint = "main.js"
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
                nodeCommand(project, "node_modules/typescript/bin/tsc", "--noEmit")
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
            into("build/dist/main/typescript/$commonsLibDirName")
        }
    }
}