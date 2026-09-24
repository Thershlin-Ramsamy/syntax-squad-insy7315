package com.mcc.construction

import android.app.Application
import com.mcc.construction.data.fake.FakeAppContainer
import com.mcc.construction.di.AppContainer

/**
 * Holds the [AppContainer] for the life of the process.
 *
 * This is the one line to change when the real API is ready: replace
 * [FakeAppContainer] with the backend implementation of [AppContainer].
 */
class MccApplication : Application() {

    val container: AppContainer by lazy { FakeAppContainer() }
}
