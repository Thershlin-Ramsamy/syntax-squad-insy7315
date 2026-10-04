package com.mcc.construction.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Owns one Supabase client for the application's real repository container.
 *
 * Auth tokens are kept in memory during this foundation stage.
 * Persistent session storage will be configured explicitly in the
 * authentication integration rather than using automatic default storage.
 */
class SupabaseProvider(
    private val configuration: SupabaseConfig
) {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = configuration.url,
            supabaseKey = configuration.publishableKey
        ) {
            httpEngine = OkHttp.create()

            install(Auth) {
                autoLoadFromStorage = false
                autoSaveToStorage = false
            }

            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}