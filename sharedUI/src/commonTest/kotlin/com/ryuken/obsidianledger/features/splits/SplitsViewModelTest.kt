package com.ryuken.obsidianledger.features.splits

import com.ryuken.obsidianledger.core.domain.usecase.GetGroupsUseCase
import com.ryuken.obsidianledger.features.dashboard.GetProfileUseCase
import com.ryuken.obsidianledger.fake.FakeAuthRepository
import com.ryuken.obsidianledger.fake.FakeProfileRepository
import com.ryuken.obsidianledger.fake.FakeSplitRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import androidx.lifecycle.viewModelScope
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SplitsViewModelTest {

    private lateinit var splitRepo: FakeSplitRepository

    @BeforeTest fun setUp() { Dispatchers.setMain(kotlinx.coroutines.test.UnconfinedTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun viewModel() = SplitsViewModel(
        getGroups  = GetGroupsUseCase(splitRepo),
        splitRepo  = splitRepo,
        getProfile = GetProfileUseCase(FakeProfileRepository()),
        authRepo   = FakeAuthRepository()
    )

    @Test
    fun groupCreation_failure_emitsErrorEffectOnce() = runBlocking {
        splitRepo = FakeSplitRepository().apply { failOnCreate = IllegalStateException("rpc exploded") }
        val vm = viewModel()
        val effects = mutableListOf<SplitsEffect>()
        val done = CompletableDeferred<Unit>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val job = scope.launch {
            vm.effect.collect {
                effects += it
                done.complete(Unit)
            }
        }

        vm.onIntent(SplitsIntent.CreateGroup(name = "Trip", memberNames = listOf("A", "B")))
        withTimeout(5_000) { done.await() }

        val error = effects.filterIsInstance<SplitsEffect.Error>().single()
        assertEquals("rpc exploded", error.message)

        job.cancel(); scope.cancel(); vm.viewModelScope.cancel()
    }

    @Test
    fun groupCreation_success_emitsGroupCreatedOnce() = runBlocking {
        splitRepo = FakeSplitRepository()
        val vm = viewModel()
        val effects = mutableListOf<SplitsEffect>()
        val done = CompletableDeferred<Unit>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val job = scope.launch {
            vm.effect.collect {
                effects += it
                done.complete(Unit)
            }
        }

        vm.onIntent(SplitsIntent.CreateGroup(name = "Trip", memberNames = listOf("A")))
        withTimeout(5_000) { done.await() }

        assertTrue(effects.filterIsInstance<SplitsEffect.GroupCreated>().size == 1)

        job.cancel(); scope.cancel(); vm.viewModelScope.cancel()
    }
}
