package com.ryuken.obsidianledger.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable

class MainComponent(
    componentContext: ComponentContext
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    val stack: Value<ChildStack<Config, Child>> = childStack(
        source               = navigation,
        serializer           = Config.serializer(),
        initialConfiguration = Config.Dashboard,
        handleBackButton     = true,
        childFactory         = ::createChild
    )

    fun navigateTo(config: Config) = navigation.bringToFront(config)

    private fun createChild(
        config : Config,
        ctx    : ComponentContext
    ): Child = when (config) {
        Config.Dashboard -> Child.Dashboard
        Config.Analytics -> Child.Analytics
        Config.Splits    -> Child.Splits
        Config.Budgets   -> Child.Budgets
        Config.Profile   -> Child.Profile
    }

    @Serializable
    sealed interface Config {
        @Serializable data object Dashboard : Config
        @Serializable data object Analytics : Config
        @Serializable data object Splits    : Config
        @Serializable data object Budgets   : Config
        @Serializable data object Profile   : Config
    }

    sealed interface Child {
        data object Dashboard : Child
        data object Analytics : Child
        data object Splits    : Child
        data object Budgets   : Child
        data object Profile   : Child
    }
}