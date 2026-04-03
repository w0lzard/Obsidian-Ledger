package com.ryuken.obsidianledger.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable


class RootComponent(
    componentContext: ComponentContext
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    val stack: Value<ChildStack<Config, Child>> = childStack(
        source               = navigation,
        serializer           = Config.serializer(),
        initialConfiguration = Config.Auth,
        handleBackButton     = true,
        childFactory         = ::createChild
    )

    @OptIn(DelicateDecomposeApi::class)
    fun navigateTo(config: Config) = navigation.push(config)
    fun pop()                        = navigation.pop()

    private fun createChild(
        config : Config,
        ctx    : ComponentContext
    ): Child = when (config) {
        Config.Auth           -> Child.Auth
        Config.Main           -> Child.Main(MainComponent(ctx))
        Config.AddTransaction -> Child.AddTransaction
    }

    @Serializable
    sealed interface Config {
        @Serializable data object Auth           : Config
        @Serializable data object Main           : Config
        @Serializable data object AddTransaction : Config
    }

    sealed interface Child {
        data object Auth                                    : Child
        data class  Main(val component: MainComponent)     : Child
        data object AddTransaction                          : Child
    }
}
