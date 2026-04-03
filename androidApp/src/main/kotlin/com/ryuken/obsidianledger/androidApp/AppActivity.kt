package com.ryuken.obsidianledger.androidApp

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.ryuken.obsidianledger.App
import com.ryuken.obsidianledger.androidApp.di.androidModule
import com.ryuken.obsidianledger.core.di.initKoin
import com.ryuken.obsidianledger.navigation.RootComponent
import org.koin.android.ext.koin.androidContext

class AppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val root = RootComponent(
            componentContext = defaultComponentContext()
        )

        setContent {
            App(root = root)
        }
    }
}

class LedgerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(platformModule = androidModule) {
            androidContext(this@LedgerApplication)
        }
    }
}