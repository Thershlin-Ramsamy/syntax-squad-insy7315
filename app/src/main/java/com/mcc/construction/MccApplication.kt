package com.mcc.construction

import android.app.Application
import com.mcc.construction.data.remote.SupabaseConfig
import com.mcc.construction.di.AppContainer
import com.mcc.construction.di.SupabaseAppContainer

class MccApplication : Application() {
    val isBackendConfigured: Boolean by lazy {
        runCatching {
            SupabaseConfig.fromBuildConfig()
        }.isSuccess
    }

    val container: AppContainer by lazy {
        SupabaseAppContainer(applicationContext)
    }
}