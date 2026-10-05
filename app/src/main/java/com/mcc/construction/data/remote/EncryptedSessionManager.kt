package com.mcc.construction.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Persists Supabase sessions with AES-GCM encryption.
 *
 * The encryption key lives in Android Keystore.
 * The encrypted session file is excluded from Android backups.
 *
 * Implements SessionManager as defined by supabase-kt 3.2.6.
 */
class EncryptedSessionManager(
    context: Context,
    projectUrl: String
) : SessionManager {

    private val mutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val projectIdentifier = MessageDigest
        .getInstance("SHA-256")
        .digest(projectUrl.toByteArray(Charsets.UTF_8))
        .take(12)
        .joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }

    private val keyAlias =
        "mcc_supabase_session_$projectIdentifier"

    private val sessionFile = AtomicFile(
        File(
            context.applicationContext.noBackupFilesDir,
            "supabase_session_$projectIdentifier.bin"
        )
    )

    override suspend fun saveSession(
        session: UserSession
    ) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val cipher = Cipher.getInstance(
                    "AES/GCM/NoPadding"
                )

                cipher.init(
                    Cipher.ENCRYPT_MODE,
                    getOrCreateKey()
                )

                val plaintext = json.encodeToString(session)
                    .toByteArray(Charsets.UTF_8)

                val ciphertext = try {
                    cipher.doFinal(plaintext)
                } finally {
                    plaintext.fill(0)
                }

                val encoded = ByteArrayOutputStream().use { buffer ->
                    DataOutputStream(buffer).use { output ->
                        output.writeInt(FORMAT_VERSION)
                        output.writeInt(cipher.iv.size)
                        output.write(cipher.iv)
                        output.writeInt(ciphertext.size)
                        output.write(ciphertext)
                    }

                    buffer.toByteArray()
                }

                val output = sessionFile.startWrite()

                try {
                    output.write(encoded)
                    sessionFile.finishWrite(output)
                } catch (exception: Exception) {
                    sessionFile.failWrite(output)
                    throw exception
                }
            }
        }
    }

    override suspend fun loadSession(): UserSession? {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    if (
                        !sessionFile.baseFile.exists() &&
                        !File(
                            sessionFile.baseFile.path + ".bak"
                        ).exists()
                    ) {
                        return@withLock null
                    }

                    val encoded = sessionFile.openRead().use {
                        val bytes = it.readBytes()

                        require(bytes.size <= MAX_SESSION_BYTES) {
                            "Invalid session file size."
                        }

                        bytes
                    }

                    val input = DataInputStream(
                        ByteArrayInputStream(encoded)
                    )

                    val plaintext = input.use {
                        require(it.readInt() == FORMAT_VERSION) {
                            "Unsupported session file format."
                        }

                        val ivLength = it.readInt()

                        require(ivLength == 12) {
                            "Invalid session IV."
                        }

                        val iv = ByteArray(ivLength)
                        it.readFully(iv)

                        val encryptedLength = it.readInt()

                        require(
                            encryptedLength in 16..MAX_SESSION_BYTES
                        ) {
                            "Invalid encrypted session length."
                        }

                        require(it.available() == encryptedLength) {
                            "Invalid session file contents."
                        }

                        val encrypted = ByteArray(encryptedLength)
                        it.readFully(encrypted)

                        val cipher = Cipher.getInstance(
                            "AES/GCM/NoPadding"
                        )

                        cipher.init(
                            Cipher.DECRYPT_MODE,
                            getOrCreateKey(),
                            GCMParameterSpec(128, iv)
                        )

                        cipher.doFinal(encrypted)
                    }

                    try {
                        json.decodeFromString<UserSession>(
                            plaintext.toString(Charsets.UTF_8)
                        )
                    } finally {
                        plaintext.fill(0)
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    // Corrupt or undecryptable sessions require a new login.
                    sessionFile.delete()
                    null
                }
            }
        }
    }

    override suspend fun deleteSession() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                sessionFile.delete()
            }
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(
            "AndroidKeyStore"
        ).apply {
            load(null)
        }

        val existing = keyStore.getKey(
            keyAlias,
            null
        ) as? SecretKey

        if (existing != null) {
            return existing
        }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setKeySize(256)
                .build()
        )

        return generator.generateKey()
    }

    companion object {
        private const val FORMAT_VERSION = 1
        private const val MAX_SESSION_BYTES = 2 * 1024 * 1024
    }
}