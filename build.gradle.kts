plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvm()

    js(IR) {
        browser()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

val vexLifeWebJvmMain =
    kotlin.targets
        .getByName("jvm")
        .compilations
        .getByName("main")

val vexLifeWebP1Output =
    layout.buildDirectory.dir(
        "generated/vexweb/vexlife-workspace",
    )

tasks.register<JavaExec>(
    "generateVexLifeWebP1",
) {
    group =
        "vexlife"
    description =
        "Materialize the deterministic VexLife P1 browser artifacts from accepted VexWeb source."

    dependsOn(
        vexLifeWebJvmMain.compileTaskProvider,
    )

    classpath =
        files(
            vexLifeWebJvmMain.output.allOutputs,
            vexLifeWebJvmMain.runtimeDependencyFiles,
        )

    mainClass.set(
        "vexlife.web.VexLifeWebP1ArtifactMaterializerKt",
    )

    args(
        "write",
        vexLifeWebP1Output
            .get()
            .asFile
            .absolutePath,
    )

    outputs.dir(
        vexLifeWebP1Output,
    )
}

tasks.register<JavaExec>(
    "verifyVexLifeWebP1",
) {
    group =
        "verification"
    description =
        "Verify an existing generated VexLife P1 browser artifact set against current VexWeb source."

    dependsOn(
        vexLifeWebJvmMain.compileTaskProvider,
    )

    classpath =
        files(
            vexLifeWebJvmMain.output.allOutputs,
            vexLifeWebJvmMain.runtimeDependencyFiles,
        )

    mainClass.set(
        "vexlife.web.VexLifeWebP1ArtifactMaterializerKt",
    )

    args(
        "verify",
        vexLifeWebP1Output
            .get()
            .asFile
            .absolutePath,
    )
}

// [VXG RealForever]
