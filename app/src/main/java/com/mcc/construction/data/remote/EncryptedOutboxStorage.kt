package com.mcc.construction.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EncryptedOutboxStorage(
    context: Context,
    projectUrl: String
) : OutboxStorage {

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
        "mcc_outbox_$projectIdentifier"

    private val file = AtomicFile(
        File(
            context.applicationContext.noBackupFilesDir,
            "mcc_outbox_$projectIdentifier.bin"
        )
    )

    override suspend fun read(): List<OutboxMessage> {
        return withContext(Dispatchers.IO) {
            if (
                !file.baseFile.exists() &&
                !File(file.baseFile.path + ".bak").exists()
            ) {
                return@withContext emptyList()
            }

            val encoded = file.openRead().use { input ->
                require(
                    input.channel.size() in
                            MIN_FILE_BYTES.toLong()..MAX_FILE_BYTES.toLong()
                ) {
                    "Invalid outbox file size."
                }

                input.readBytes()
            }

            require(encoded[0] == FORMAT_VERSION) {
                "Unsupported outbox format."
            }

            val iv = encoded.copyOfRange(
                1,
                1 + IV_BYTES
            )

            val encrypted = encoded.copyOfRange(
                1 + IV_BYTES,
                encoded.size
            )

            val cipher = Cipher.getInstance(
                "AES/GCM/NoPadding"
            )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, iv)
            )

            cipher.updateAAD(byteArrayOf(FORMAT_VERSION))

            val plaintext = cipher.doFinal(encrypted)

            try {
                val messages =
                    json.decodeFromString<List<OutboxMessage>>(
                        plaintext.toString(Charsets.UTF_8)
                    )

                require(messages.size <= MAX_MESSAGES)

                require(
                    messages.map { it.localId }.distinct().size ==
                            messages.size
                )

                require(
                    messages.map {
                        it.operation.operationId
                    }.distinct().size == messages.size
                )

                messages
            } finally {
                plaintext.fill(0)
            }
        }
    }

    override suspend fun write(
        messages: List<OutboxMessage>
    ) {
        withContext(Dispatchers.IO) {
            require(messages.size <= MAX_MESSAGES) {
                "The message queue is full. Sync before sending more."
            }

            val plaintext = json.encodeToString(messages)
                .toByteArray(Charsets.UTF_8)

            require(
                plaintext.size <=
                        MAX_FILE_BYTES - 1 - IV_BYTES - TAG_BYTES
            )

            val cipher = Cipher.getInstance(
                "AES/GCM/NoPadding"
            )

            cipher.init(
                Cipher.ENCRYPT_MODE,
                getOrCreateKey()
            )

            cipher.updateAAD(byteArrayOf(FORMAT_VERSION))

            val encrypted = try {
                cipher.doFinal(plaintext)
            } finally {
                plaintext.fill(0)
            }

            check(cipher.iv.size == IV_BYTES)

            val encoded =
                byteArrayOf(FORMAT_VERSION) +
                        cipher.iv +
                        encrypted

            val output = file.startWrite()

            try {
                output.write(encoded)
                file.finishWrite(output)
            } catch (exception: Exception) {
                file.failWrite(output)
                throw exception
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
        private const val FORMAT_VERSION: Byte = 1
        private const val IV_BYTES = 12
        private const val TAG_BYTES = 16
        private const val MIN_FILE_BYTES =
            1 + IV_BYTES + TAG_BYTES

        private const val MAX_FILE_BYTES =
            10 * 1024 * 1024

        private const val MAX_MESSAGES = 500
    }
}