package com.mcc.construction

import android.app.Application
import com.mcc.construction.di.AppContainer
import com.mcc.construction.di.SupabaseAppContainer

class MccApplication : Application() {

    val container: AppContainer by lazy {
        SupabaseAppContainer(
            applicationContext
        )
    }
}