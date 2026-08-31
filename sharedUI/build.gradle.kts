import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.buildconfig)
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
}

sqldelight {
    databases {
        create("LedgerDatabase") {
            packageName.set("com.ryuken.obsidianledger.core.database")
            srcDirs.setFrom("src/commonMain/database")
            verifyMigrations.set(true)
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    android {
        namespace = "com.ryuken.obsidianledger"
        compileSdk = 36
        minSdk = 26

        // Wires commonTest -> androidUnitTest into a host-runnable unit test
        // variant; the KMP android plugin does not create one by default.
        withHostTest { }
    }

    // iosX64 (Intel Mac simulator) dropped — FileKit ships no variant for it, and it's
    // EOL-adjacent now that Apple Silicon Macs use iosSimulatorArm64 for the simulator.
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "sharedUI"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.resources)
            implementation(libs.compose.ui.tooling.preview)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            // Supabase
            implementation(libs.supabase.postgrest)
            api(libs.supabase.auth)
            implementation(libs.supabase.realtime)

            // Ktor (Supabase HTTP engine)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Utils
            implementation(libs.datetime)
            implementation(libs.coroutines.core)
            implementation(libs.napier)
            implementation(libs.uuid)
            implementation(libs.serialization.json)

            // Settings
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)

            // Decompose
            implementation(libs.decompose.core)
            implementation(libs.decompose.compose)
            implementation(libs.essenty.lifecycle)

            // Charts
            implementation(libs.koalaplot.core)

            // Lifecycle ViewModel (KMP)
            implementation(libs.lifecycle.viewmodel)

            // File picking / saving (CSV import/export) — api, not implementation:
            // AppActivity (androidApp module) needs FileKit.init(activity) directly.
            api(libs.filekit.core)
            api(libs.filekit.dialogs.compose)
        }

        androidMain.dependencies {
            implementation(libs.sqldelight.android)
            implementation(libs.ktor.client.android)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
        }

        iosMain.dependencies {
            implementation(libs.sqldelight.native)
            implementation(libs.ktor.client.ios)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

buildConfig {
    packageName.set("com.ryuken.obsidianledger")

    buildConfigField("SUPABASE_URL", localProperties.getProperty("SUPABASE_URL") ?: "")
    buildConfigField("SUPABASE_KEY", localProperties.getProperty("SUPABASE_KEY") ?: "")
    buildConfigField("GOOGLE_WEB_CLIENT_ID", localProperties.getProperty("GOOGLE_WEB_CLIENT_ID") ?: "")
}
