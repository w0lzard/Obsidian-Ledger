package com.ryuken.obsidianledger.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class RootComponent(
    componentContext: ComponentContext,
    private val initiallyAuthenticated: Boolean = false
) : ComponentContext by componentContext, KoinComponent {

    private val authRepository: AuthRepository by inject()
    private val navigation = StackNavigation<Config>()

    private val initialConfig: Config
        get() = if (initiallyAuthenticated || authRepository.isSignedIn()) Config.Main else Config.Auth

    val stack: Value<ChildStack<Config, Child>> = childStack(
        source               = navigation,
        serializer           = Config.serializer(),
        initialConfiguration = initialConfig,
        handleBackButton     = true,
        childFactory         = ::createChild
    )

    @OptIn(DelicateDecomposeApi::class)
    fun navigateTo(config: Config) = navigation.push(config)
    fun pop()                        = navigation.pop()
    fun replaceWithMain()            = navigation.replaceAll(Config.Main)
    fun replaceWithAuth()            = navigation.replaceAll(Config.Auth)

    private fun createChild(
        config : Config,
        ctx    : ComponentContext
    ): Child = when (config) {
        Config.Auth                -> Child.Auth
        Config.Main                -> Child.Main(MainComponent(ctx))
        Config.AddTransaction      -> Child.AddTransaction
        Config.CreateGroup         -> Child.CreateGroup
        is Config.GroupDetail      -> Child.GroupDetail(config.groupId)
        is Config.AddSplitExpense  -> Child.AddSplitExpense(config.groupId)
    }

    @Serializable
    sealed interface Config {
        @Serializable data object Auth                                  : Config
        @Serializable data object Main                                  : Config
        @Serializable data object AddTransaction                        : Config
        @Serializable data object CreateGroup                           : Config
        @Serializable data class  GroupDetail(val groupId: String)      : Config
        @Serializable data class  AddSplitExpense(val groupId: String)  : Config
    }

    sealed interface Child {
        data object Auth                                                 : Child
        data class  Main(val component: MainComponent)                  : Child
        data object AddTransaction                                       : Child
        data object CreateGroup                                          : Child
        data class  GroupDetail(val groupId: String)                     : Child
        data class  AddSplitExpense(val groupId: String)                 : Child
    }
}
