package com.mcc.construction.ui.screens.splash

import com.mcc.construction.data.fake.testContainer
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val container = testContainer()

    @Test
    fun staysOnSplashUntilTheDisplayTimeHasPassed() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = SplashViewModel(container.authRepository)

        advanceTimeBy(SplashViewModel.DISPLAY_MILLIS - 1)
        assertNull(viewModel.destination.value)

        advanceTimeBy(2)
        assertEquals(SplashDestination.SignIn, viewModel.destination.value)
    }

    @Test
    fun goesHomeWhenAlreadySignedIn() = runTest(mainDispatcherRule.dispatcher) {
        container.authRepository.signIn("manager@mcc.co.za", "pw")
        val viewModel = SplashViewModel(container.authRepository)

        advanceTimeBy(SplashViewModel.DISPLAY_MILLIS + 1)

        assertEquals(SplashDestination.Home(UserRole.MANAGER), viewModel.destination.value)
    }
}
