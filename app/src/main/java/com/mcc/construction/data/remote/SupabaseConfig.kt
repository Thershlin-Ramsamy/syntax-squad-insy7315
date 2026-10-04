package com.mcc.construction.data.remote

import com.mcc.construction.BuildConfig
import java.net.URI

/**
 * Public client configuration.
 *
 * The publishable key identifies the Supabase project. Database permissions
 * are enforced by the user's session and server-side policies.
 *
 * This class intentionally accepts only modern publishable keys.
 */
class SupabaseConfig(
    url: String,
    publishableKey: String
) {
    val url: String = url.trim().trimEnd('/')

    val publishableKey: String = publishableKey.trim()

    init {
        require(this.url.isNotBlank()) {
            "SUPABASE_URL is missing from local.properties."
        }

        val parsedUrl = try {
            URI(this.url)
        } catch (_: Exception) {
            throw IllegalArgumentException(
                "SUPABASE_URL is not a valid URL."
            )
        }

        require(
            parsedUrl.scheme.equals("https", ignoreCase = true)
                    && !parsedUrl.host.isNullOrBlank()
                    && parsedUrl.rawUserInfo == null
                    && parsedUrl.rawQuery == null
                    && parsedUrl.rawFragment == null
                    && parsedUrl.rawPath.isNullOrEmpty()
        ) {
            "SUPABASE_URL must be the HTTPS project origin."
        }

        require(
            this.publishableKey.startsWith("sb_publishable_")
                    && this.publishableKey.length >
                    "sb_publishable_".length
                    && this.publishableKey.none { it.isWhitespace() }
        ) {
            "Use the Supabase publishable key beginning sb_publishable_."
        }
    }

    override fun toString(): String {
        return "SupabaseConfig(url=$url, publishableKey=<redacted>)"
    }

    companion object {
        const val PHOTO_BUCKET = "mcc-photos"
        const val DOCUMENT_BUCKET = "mcc-documents"

        fun fromBuildConfig(): SupabaseConfig {
            return SupabaseConfig(
                url = BuildConfig.SUPABASE_URL,
                publishableKey =
                    BuildConfig.SUPABASE_PUBLISHABLE_KEY
            )
        }
    }
}