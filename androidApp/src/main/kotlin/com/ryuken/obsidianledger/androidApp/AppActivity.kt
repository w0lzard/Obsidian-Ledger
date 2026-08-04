package com.ryuken.obsidianledger.androidApp

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.ryuken.obsidianledger.App
import com.ryuken.obsidianledger.androidApp.di.SyncScheduler
import com.ryuken.obsidianledger.androidApp.di.androidModule
import com.ryuken.obsidianledger.core.di.initKoin
import com.ryuken.obsidianledger.navigation.RootComponent
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import org.koin.android.ext.koin.androidContext
import org.koin.java.KoinJavaComponent.getKoin

class AppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // filekit-core auto-initializes its Context via App Startup, but the picker/saver
        // ActivityResultRegistry has no such auto-init — must be wired per-Activity, before
        // the activity reaches STARTED, or FileKit.openFilePicker/openFileSaver throw
        // FileKitNotInitializedException.
        FileKit.init(this)

        // Check if the user already has a valid Supabase session (persisted)
        val isAlreadySignedIn = try {
            val supabase = getKoin().get<SupabaseClient>()
            supabase.auth.currentSessionOrNull() != null
        } catch (e: Exception) {
            // Surfaces real init failures and would also catch a malformed/intercepted
            // session payload rather than silently treating it as "signed out".
            Napier.w("AppActivity: session check failed, treating as signed out", e)
            false
        }

        val root = RootComponent(
            componentContext = defaultComponentContext(),
            initiallyAuthenticated = isAlreadySignedIn
        )

        setContent {
            App(root = root)
        }
    }
}

class LedgerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // No antilog is attached in release, so Napier.d/e/w calls are no-ops there —
        // this is what keeps auth token/session/userId logging out of release logcat.
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }
        initKoin(platformModule = androidModule) {
            androidContext(this@LedgerApplication)
        }
        getKoin().get<SyncScheduler>().schedule()
    }
}
