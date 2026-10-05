package com.mcc.construction.data.remote

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp

class SupabaseProvider(
    context: Context,
    private val configuration: SupabaseConfig
) {

    private val sessionStore = EncryptedSessionManager(
        context = context.applicationContext,
        projectUrl = configuration.url
    )

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = configuration.url,
            supabaseKey = configuration.publishableKey
        ) {
            httpEngine = OkHttp.create()

            install(Auth) {
                sessionManager = sessionStore
                autoLoadFromStorage = true
                autoSaveToStorage = true
            }

            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}