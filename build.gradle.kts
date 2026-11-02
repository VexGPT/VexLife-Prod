plugins {
    kotlin("multiplatform") version "2.4.20"
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

// [VXG RealForever]
