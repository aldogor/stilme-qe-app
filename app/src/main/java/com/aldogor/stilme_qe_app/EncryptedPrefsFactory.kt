package com.aldogor.stilme_qe_app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.GeneralSecurityException
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.AEADBadTagException

/**
 * Factory for creating EncryptedSharedPreferences with recovery from keystore corruption.
 *
 * Android Keystore can rarely become genuinely corrupted (e.g., after backup/restore, OS
 * updates, or hardware issues), causing AEADBadTagException when decrypting the Tink keyset.
 * When that happens the encrypted data is already unrecoverable and the file must be reset
 * so the app doesn't crash-loop.
 *
 * Two safeguards make the reset safe:
 *  - **Instance caching**: each prefs file is created exactly once per process. First-time
 *    creation of EncryptedSharedPreferences / Tink keyset generation is expensive and NOT
 *    thread-safe for concurrent creation of the same file — multiple activities and workers
 *    creating the same file on different threads could otherwise throw a spurious
 *    GeneralSecurityException.
 *  - **Corruption-only deletion**: the prefs file is deleted ONLY when the failure is a true
 *    AEADBadTagException (keystore corruption). A transient GeneralSecurityException (e.g. the
 *    creation race above) triggers a retry that preserves the existing file and its data.
 */
object EncryptedPrefsFactory {

    private const val TAG = "EncryptedPrefsFactory"

    /** One shared instance per file name — creation happens at most once per process. */
    private val cache = ConcurrentHashMap<String, SharedPreferences>()
    private val creationLock = Any()

    fun create(context: Context, fileName: String): SharedPreferences {
        // Fast path: already created this process.
        cache[fileName]?.let { return it }
        // Slow path: serialize creation so the same file is never built concurrently.
        return synchronized(creationLock) {
            cache[fileName] ?: buildPrefs(context.applicationContext, fileName)
                .also { cache[fileName] = it }
        }
    }

    private fun buildPrefs(context: Context, fileName: String): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return try {
            createEncrypted(context, fileName, masterKey)
        } catch (e: GeneralSecurityException) {
            if (isKeystoreCorruption(e)) {
                // Real corruption: the encrypted data is already unrecoverable. Delete and reset.
                Log.e(TAG, "Keystore corruption (AEADBadTagException) for '$fileName'. " +
                        "Encrypted data is unrecoverable. Resetting prefs file.", e)
                deletePrefsFile(context, fileName)
                try {
                    createEncrypted(context, fileName, masterKey)
                } catch (e2: GeneralSecurityException) {
                    Log.e(TAG, "Retry after reset failed for '$fileName'. " +
                            "Falling back to unencrypted prefs to prevent crash loop.", e2)
                    context.getSharedPreferences("${fileName}_fallback", Context.MODE_PRIVATE)
                }
            } else {
                // Transient failure (most likely a concurrent-creation race). Do NOT delete —
                // the existing encrypted file and its data are almost certainly intact. Retry once.
                Log.w(TAG, "Transient security exception creating '$fileName' (not corruption). " +
                        "Retrying without deleting data.", e)
                try {
                    createEncrypted(context, fileName, masterKey)
                } catch (e2: GeneralSecurityException) {
                    // Still failing but not corruption: preserve the encrypted file (a future
                    // launch may succeed once the keystore recovers) and fall back for this run.
                    Log.e(TAG, "Retry failed for '$fileName' without corruption. " +
                            "Falling back to unencrypted prefs for this session; data preserved.", e2)
                    context.getSharedPreferences("${fileName}_fallback", Context.MODE_PRIVATE)
                }
            }
        }
    }

    private fun createEncrypted(
        context: Context,
        fileName: String,
        masterKey: MasterKey
    ): SharedPreferences = EncryptedSharedPreferences.create(
        context,
        fileName,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun deletePrefsFile(context: Context, fileName: String) {
        val prefsFile = File(context.filesDir.parent, "shared_prefs/$fileName.xml")
        if (prefsFile.exists() && prefsFile.delete()) {
            Log.w(TAG, "Deleted corrupted prefs file: ${prefsFile.name}")
        }
    }

    /** True only if a real keystore/keyset corruption (AEADBadTagException) is in the cause chain. */
    private fun isKeystoreCorruption(throwable: Throwable): Boolean {
        var cause: Throwable? = throwable
        while (cause != null) {
            if (cause is AEADBadTagException) return true
            cause = cause.cause
        }
        return false
    }
}
