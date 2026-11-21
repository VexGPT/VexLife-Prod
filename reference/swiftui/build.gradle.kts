import org.gradle.api.tasks.JavaExec

plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(project(":"))
    testImplementation(kotlin("test"))
}

val swiftUiReferenceOutput =
    layout.buildDirectory.dir(
        "generated/swiftui-reference",
    )

tasks.register<JavaExec>(
    "materializeSwiftUiReference",
) {
    group = "verification"
    description =
        "Materialize deterministic SwiftUI reference source and manifests for native proof."
    dependsOn("classes")
    classpath =
        sourceSets["main"]
            .runtimeClasspath
    mainClass.set(
        "vexlife.reference.swiftui.VexLifeSwiftUiReferenceMaterializerKt",
    )
    args(
        swiftUiReferenceOutput
            .get()
            .asFile
            .absolutePath,
    )
    outputs.dir(
        swiftUiReferenceOutput,
    )
}

// [VXG RealForever]
