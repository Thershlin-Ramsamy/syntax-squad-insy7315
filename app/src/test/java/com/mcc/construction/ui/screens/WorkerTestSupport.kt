package com.mcc.construction.ui.screens

import com.mcc.construction.data.fake.FakeAppContainer
import com.mcc.construction.data.fake.testContainer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** The demo worker, Sipho Dlamini. */
const val WORKER_ID = 5

/** A test container with the demo worker already signed in. */
suspend fun workerContainer(): FakeAppContainer = testContainer().also {
    it.authRepository.signIn("worker@mcc.co.za", "pw")
}

/** `WhileSubscribed` state only updates while something collects it. */
fun TestScope.keepCollecting(state: StateFlow<*>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
}
