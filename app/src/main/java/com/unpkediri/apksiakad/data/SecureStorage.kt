package com.unpkediri.apksiakad.data

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SecureStorage {
    private const val PREFS_FILE = "secure_prefs"
    private const val KEY_NPM = "npm"
    private const val KEY_PASSWORD = "password"

    @Volatile
    private var sharedPreferences: SharedPreferences? = null

    @Synchronized
    private fun getPrefs(context: Context): SharedPreferences {
        return sharedPreferences ?: run {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences = prefs
            prefs
        }
    }

    suspend fun getCredentials(context: Context): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val prefs = getPrefs(context)
            val npm = prefs.getString(KEY_NPM, "") ?: ""
            val password = prefs.getString(KEY_PASSWORD, "") ?: ""
            if (npm.isNotBlank() && password.isNotBlank()) npm to password else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveCredentials(context: Context, npm: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val prefs = getPrefs(context)
            prefs.edit()
                .putString(KEY_NPM, npm)
                .putString(KEY_PASSWORD, pass)
                .commit()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearCredentials(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val prefs = getPrefs(context)
            prefs.edit().clear().commit()
            withContext(Dispatchers.Main) {
                CookieManager.getInstance().let {
                    it.removeAllCookies(null)
                    it.flush()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
