package com.aldogor.stilme_qe_app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.GeneralSecurityException

/**
 * Factory for creating EncryptedSharedPreferences with recovery from keystore corruption.
 *
 * Android Keystore can rarely become corrupted (e.g., after backup/restore, OS updates,
 * or hardware issues), causing AEADBadTagException when decrypting the Tink keyset.
 * When this happens the encrypted data is already unrecoverable.
 * This factory catches the exception and recreates the prefs file so the app
 * doesn't crash-loop.
 */
object EncryptedPrefsFactory {

    private const val TAG = "EncryptedPrefsFactory"

    /**
     * Creates EncryptedSharedPreferences with automatic recovery from keystore corruption.
     * If the keyset is corrupted (AEADBadTagException), deletes the prefs file and retries.
     * Data in the corrupted file is already unrecoverable at that point.
     */
    fun create(context: Context, fileName: String): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return try {
            EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Keystore corruption detected for '$fileName'. " +
                    "Encrypted data is unrecoverable. Resetting prefs file.", e)

            // Delete the corrupted SharedPreferences file
            val prefsFile = File(context.filesDir.parent, "shared_prefs/$fileName.xml")
            if (prefsFile.exists()) {
                prefsFile.delete()
                Log.w(TAG, "Deleted corrupted prefs file: ${prefsFile.name}")
            }

            // Retry creation with a fresh file
            EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
