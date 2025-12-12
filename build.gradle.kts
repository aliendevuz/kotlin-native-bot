plugins {
    kotlin("multiplatform") version "2.2.21"
}

group = "uz.alien"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                entryPoint = "uz.alien.main"
            }
        }
    }

    sourceSets {
        val linuxX64Main by getting {
            kotlin.srcDir("src/main/kotlin")
        }
        val linuxX64Test by getting
    }
}