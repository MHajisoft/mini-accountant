package ir.mhajisoft.hesabres.data.secret

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.mhajisoft.hesabres.data.local.dao.SecretBlobDao
import ir.mhajisoft.hesabres.data.local.entity.SecretBlobEntity
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PAN and generic secrets: Tink AES-256-GCM wrapped by Android Keystore.
 * CVV: optional, default off. Encrypted with a Keystore AES-256-GCM key that
 * requires USER_AUTHENTICATION (BiometricPrompt CryptoObject) to decrypt.
 *
 * Never log PAN/CVV.
 */
@Singleton
class SecretVault @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blobs: SecretBlobDao,
) {
    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    suspend fun encryptPan(panAscii: String): String {
        val id = UUID.randomUUID().toString()
        val cipher = aead.encrypt(panAscii.toByteArray(Charsets.UTF_8), id.toByteArray())
        blobs.upsert(
            SecretBlobEntity(
                id = id,
                kind = KIND_PAN,
                nonce = ByteArray(0),
                ciphertext = cipher,
                createdAt = System.currentTimeMillis(),
            ),
        )
        return id
    }

    suspend fun decryptPan(id: String): String? {
        val blob = blobs.get(id) ?: return null
        val plain = aead.decrypt(blob.ciphertext, id.toByteArray())
        return String(plain, Charsets.UTF_8)
    }

    fun createCvvEncryptCipher(): Cipher {
        ensureCvvKey()
        val key = cvvKey()
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }

    fun createCvvDecryptCipher(iv: ByteArray): Cipher {
        val key = cvvKey()
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher
    }

    suspend fun persistCvv(cipher: Cipher, cvvAscii: String): String {
        val id = UUID.randomUUID().toString()
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(cvvAscii.toByteArray(Charsets.UTF_8))
        blobs.upsert(
            SecretBlobEntity(
                id = id,
                kind = KIND_CVV,
                nonce = iv,
                ciphertext = ciphertext,
                createdAt = System.currentTimeMillis(),
            ),
        )
        return id
    }

    suspend fun revealCvv(id: String, unlockedCipher: Cipher): String? {
        blobs.get(id) ?: return null
        val plain = unlockedCipher.doFinal(blobs.get(id)!!.ciphertext)
        return String(plain, Charsets.UTF_8)
    }

    suspend fun cvvIv(id: String): ByteArray? = blobs.get(id)?.nonce

    suspend fun delete(id: String) {
        blobs.delete(id)
    }

    suspend fun deleteAllCvv() {
        blobs.deleteAllCvv()
    }

    private fun ensureCvvKey() {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(CVV_KEY_ALIAS)) return
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            CVV_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setRandomizedEncryptionRequired(true)
        if (Build.VERSION.SDK_INT >= 30) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        generator.init(builder.build())
        generator.generateKey()
    }

    private fun cvvKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return ks.getKey(CVV_KEY_ALIAS, null) as SecretKey
    }

    companion object {
        private const val PREF_FILE = "tink_mini_accountant"
        private const val KEYSET_NAME = "aead_keyset"
        private const val MASTER_KEY_URI = "android-keystore://mini_accountant_tink_master"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val CVV_KEY_ALIAS = "mini_accountant_cvv_aes256"
        private const val AES_GCM = "AES/GCM/NoPadding"
        const val KIND_PAN = "pan"
        const val KIND_CVV = "cvv"

        fun scrubLog(message: String): String {
            if (message.contains("cvv", ignoreCase = true) || message.contains("pan", ignoreCase = true)) {
                return "[redacted]"
            }
            return message
        }

        fun logSafe(tag: String, message: String) {
            Log.d(tag, scrubLog(message))
        }
    }
}
