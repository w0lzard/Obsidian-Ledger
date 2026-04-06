import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.ryuken.obsidianledger.androidApp"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36

        applicationId = "com.ryuken.obsidianledger.androidApp"
        versionCode = 1
        versionName = "1.0.0"
        val props = gradleLocalProperties(rootDir, providers)
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${props.getProperty("SUPABASE_URL")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_KEY",
            "\"${props.getProperty("SUPABASE_KEY")}\""
        )
        buildConfigField(
            "String",
            "GOOGLE_ANDROID_CLIENT_ID",
            "\"${props.getProperty("GOOGLE_ANDROID_CLIENT_ID")}\""
        )
        buildConfigField(
            "String",
            "RESEND_API_KEY",
            "\"${props.getProperty("RESEND_API_KEY") ?: ""}\""
        )
    }
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":sharedUI"))
    implementation(libs.androidx.activityCompose)
    implementation(libs.decompose.core)
    implementation(libs.koin.android)
    implementation(libs.androidx.work.runtime)
}
