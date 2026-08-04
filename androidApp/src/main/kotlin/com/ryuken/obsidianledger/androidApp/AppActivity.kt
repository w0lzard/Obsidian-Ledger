package com.ryuken.obsidianledger.androidApp

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.ryuken.obsidianledger.App
import com.ryuken.obsidianledger.androidApp.di.SyncScheduler
import com.ryuken.obsidianledger.androidApp.di.androidModule
import com.ryuken.obsidianledger.core.di.initKoin
import com.ryuken.obsidianledger.core.auth.SupabaseSessionManager
import com.ryuken.obsidianledger.navigation.RootComponent
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.android.ext.koin.androidContext
import org.koin.java.KoinJavaComponent.getKoin

class AppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Handle OAuth redirect that launched the activity cold
        handleIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        when {
            data.host == "mqyfrodljzbm.supabase.co" -> {
                try {
                    getKoin().get<SupabaseClient>().handleDeeplinks(intent)
                    SupabaseSessionManager.onSessionEstablished()
                } catch (_: Exception) { }
            }
            data.scheme == "obsidianledger" && data.host == "auth" -> {
                try {
                    getKoin().get<SupabaseClient>().handleDeeplinks(intent)
                    SupabaseSessionManager.onSessionEstablished()
                } catch (_: Exception) { }
            }
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
