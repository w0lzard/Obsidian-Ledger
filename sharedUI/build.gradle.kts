import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.android.kmp.library)
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
}

kotlin {
    android {
        namespace = "com.ryuken.obsidianledger"
        compileSdk = 36
        minSdk = 23
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.resources)
            implementation(libs.compose.ui.tooling.preview)

            // SQLDelight
            implementation(libs.sqldelight.runtime)

            // Supabase
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.realtime)

            // Ktor (Supabase HTTP engine)
            implementation(libs.ktor.client.core)

            // DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            // Utils
            implementation(libs.datetime)
            implementation(libs.coroutines.core)
            implementation(libs.napier)
            implementation(libs.uuid)
        }

        androidMain.dependencies {
            implementation(libs.sqldelight.android)
            implementation(libs.ktor.client.android)
        }

        iosMain.dependencies {
            implementation(libs.sqldelight.native)
            implementation(libs.ktor.client.ios)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}

sqldelight {
    databases {
        create("LedgerDatabase") {
            packageName.set("com.ryuken.obsidianledger.database")
        }
    }
}
