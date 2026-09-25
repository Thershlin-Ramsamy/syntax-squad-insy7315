package com.mcc.construction.ui.screens

import com.mcc.construction.data.fake.FakeAppContainer
import com.mcc.construction.data.fake.FakeSeedData
import com.mcc.construction.data.fake.testContainer

/** The demo manager, Naledi Khumalo, who runs Riverside and Harbour View. */
const val MANAGER_ID = 2

/** A test container with the demo manager already signed in. */
suspend fun managerContainer(): FakeAppContainer = testContainer().also {
    it.authRepository.signIn(FakeSeedData.DEMO_MANAGER_EMAIL, "pw")
}

/** Signs whoever is in out and [email] in, on the same shared data. */
suspend fun FakeAppContainer.switchTo(email: String) {
    authRepository.signOut()
    authRepository.signIn(email, "pw")
}
