package com.ryuken.obsidianledger.features.auth

import androidx.compose.runtime.Composable

// ponytail: iOS native Google Sign-In needs the GoogleSignIn SDK wired into iosApp.xcodeproj
// (SPM, from Xcode — this repo's Gradle/Kotlin-Native build can't fetch or link it, and can't
// be verified from a non-macOS machine). Until that's done, this throws instead of pretending
// to work. To finish: add GoogleSignIn via Xcode's Package Dependencies, call
// GIDSignIn.sharedInstance.signIn(withPresenting:) from Swift, and bridge the resulting ID
// token + rawNonce into this function (e.g. via a Kotlin interface implemented in Swift and
// passed in at app startup, same pattern Compose Multiplatform apps use for other native APIs).
@Composable
actual fun rememberGoogleSignInLauncher(
    onResult: (Result<GoogleIdTokenResult>) -> Unit
): () -> Unit {
    return {
        onResult(Result.failure(NotImplementedError("iOS native Google Sign-In not wired up yet — see GoogleSignInLauncher.ios.kt")))
    }
}
