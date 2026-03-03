package com.aldogor.stilme_qe_app.network

import android.content.Context
import android.util.Log
import com.aldogor.stilme_qe_app.EncryptedPrefsFactory
import com.aldogor.stilme_qe_app.study.RedcapResult
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import com.aldogor.stilme_qe_app.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * REDCap API service for submitting questionnaire data.
 */

// ============================================================================
// CONFIGURATION
// ============================================================================

object RedcapConfig {
    // REDCap API URL - injected from local.properties via BuildConfig
    val API_URL: String = BuildConfig.REDCAP_API_URL

    // Token storage key (for future use if token needs to be updated)
    const val TOKEN_KEY = "redcap_api_token"

    // API Token - injected from local.properties via BuildConfig
    val API_TOKEN: String = BuildConfig.REDCAP_API_TOKEN
}

// ============================================================================
// API INTERFACE
// ============================================================================

interface RedcapApiService {
    @FormUrlEncoded
    @POST(".")
    suspend fun importRecords(
        @Field("token") token: String,
        @Field("content") content: String = "record",
        @Field("format") format: String = "json",
        @Field("type") type: String = "flat",
        @Field("overwriteBehavior") overwrite: String = "normal",
        @Field("data") data: String,
        @Field("returnContent") returnContent: String = "ids"
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST(".")
    suspend fun exportRecords(
        @Field("token") token: String,
        @Field("content") content: String = "record",
        @Field("format") format: String = "json",
        @Field("type") type: String = "flat",
        @Field("records") records: String? = null,
        @Field("fields") fields: String? = null,
        @Field("events") events: String? = null
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST(".")
    suspend fun deleteRecords(
        @Field("token") token: String,
        @Field("content") content: String = "record",
        @Field("action") action: String = "delete",
        @Field("records[0]") record: String
    ): Response<ResponseBody>
}

// ============================================================================
// TOKEN MANAGER
// ============================================================================

class SecureTokenManager(context: Context) {
    private val prefs = EncryptedPrefsFactory.create(context, "stilme_secure_tokens")

    fun getToken(): String {
        // Check if we have a stored token (for future token updates)
        val storedToken = prefs.getString(RedcapConfig.TOKEN_KEY, null)
        if (!storedToken.isNullOrEmpty()) {
            return storedToken
        }

        // Return the hardcoded production token
        return RedcapConfig.API_TOKEN
    }

    fun setToken(token: String) {
        prefs.edit().putString(RedcapConfig.TOKEN_KEY, token).apply()
    }
}

// ============================================================================
// API CLIENT
// ============================================================================

object RedcapApiClient {
    private var retrofit: Retrofit? = null
    private var apiService: RedcapApiService? = null

    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    fun getApiService(): RedcapApiService {
        if (apiService == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(RedcapConfig.API_URL)
                .client(createOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit!!.create(RedcapApiService::class.java)
        }
        return apiService!!
    }
}

// ============================================================================
// REPOSITORY
// ============================================================================

class RedcapRepository(private val context: Context) {
    private val api = RedcapApiClient.getApiService()
    private val tokenManager = SecureTokenManager(context)

    companion object {
        private const val TAG = "RedcapRepository"
    }

    suspend fun submitRecord(payload: String): RedcapResult {
        val token = tokenManager.getToken()

        return try {
            val response = api.importRecords(
                token = token,
                data = payload
            )

            if (response.isSuccessful) {
                val body = response.body()?.string()
                Log.d(TAG, "REDCap submission successful: $body")
                RedcapResult.Success(body ?: "")
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "REDCap submission failed: ${response.code()} - $errorBody")
                RedcapResult.ServerError(response.code(), errorBody ?: "Unknown error")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during submission", e)
            RedcapResult.NetworkError(e.message ?: "Network error")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during submission", e)
            RedcapResult.ParseError(e.message ?: "Unexpected error")
        }
    }

    suspend fun checkRecordExists(studyId: String, eventName: String): Boolean {
        val token = tokenManager.getToken()

        return try {
            val response = api.exportRecords(
                token = token,
                records = studyId,
                events = eventName,
                fields = "record_id"
            )

            if (response.isSuccessful) {
                val body = response.body()?.string()
                // Check if response contains data
                !body.isNullOrEmpty() && body != "[]"
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking record existence", e)
            false
        }
    }

    /**
     * Mark a participant as withdrawn from the study.
     * This deletes all their data and creates a minimal tombstone record with only:
     * - record_id (anonymous ID)
     * - consent_timestamp (when they joined)
     * - withdrawn = 1
     * - withdrawn_timestamp (when they left)
     *
     * @param studyId The participant's study ID
     * @return RedcapResult indicating success or failure
     */
    suspend fun markAsWithdrawn(studyId: String): RedcapResult {
        val token = tokenManager.getToken()

        return try {
            // Step 1: Fetch the consent_timestamp before deleting
            var consentTimestamp: String? = null
            try {
                val exportResponse = api.exportRecords(
                    token = token,
                    records = studyId,
                    fields = "consent_timestamp",
                    events = "baseline_arm_1"
                )
                if (exportResponse.isSuccessful) {
                    val body = exportResponse.body()?.string()
                    if (!body.isNullOrEmpty() && body != "[]") {
                        // Parse JSON to extract consent_timestamp
                        val gson = com.google.gson.Gson()
                        val listType = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
                        val records: List<Map<String, Any>> = gson.fromJson(body, listType)
                        if (records.isNotEmpty()) {
                            consentTimestamp = records[0]["consent_timestamp"]?.toString()
                        }
                    }
                }
                Log.d(TAG, "Retrieved consent_timestamp: $consentTimestamp")
            } catch (e: Exception) {
                Log.w(TAG, "Could not retrieve consent_timestamp: ${e.message}")
            }

            // Step 2: Delete the entire record (all events)
            val deleteResponse = api.deleteRecords(
                token = token,
                record = studyId
            )
            if (!deleteResponse.isSuccessful) {
                Log.w(TAG, "Delete failed: ${deleteResponse.code()}, continuing to create tombstone anyway")
            } else {
                Log.d(TAG, "Record deleted successfully")
            }

            // Step 3: Create minimal tombstone record
            val withdrawTimestamp = java.time.LocalDateTime.now().toString()
            val tombstoneData = mutableMapOf<String, Any>(
                "record_id" to studyId,
                "redcap_event_name" to "baseline_arm_1",
                "withdrawn" to 1,
                "withdrawn_timestamp" to withdrawTimestamp
            )
            // Include consent_timestamp if we retrieved it
            if (!consentTimestamp.isNullOrEmpty()) {
                tombstoneData["consent_timestamp"] = consentTimestamp
            }

            val jsonPayload = "[${com.google.gson.Gson().toJson(tombstoneData)}]"
            Log.d(TAG, "Creating tombstone record for: $studyId")

            val importResponse = api.importRecords(
                token = token,
                data = jsonPayload
            )

            if (importResponse.isSuccessful) {
                Log.d(TAG, "Tombstone record created successfully")
                RedcapResult.Success(studyId)
            } else {
                val errorBody = importResponse.errorBody()?.string()
                Log.e(TAG, "Failed to create tombstone: ${importResponse.code()} - $errorBody")
                RedcapResult.ServerError(importResponse.code(), errorBody ?: "Unknown error")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during withdrawal", e)
            RedcapResult.NetworkError(e.message ?: "Network error")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during withdrawal", e)
            RedcapResult.ParseError(e.message ?: "Unexpected error")
        }
    }

    /**
     * Delete a participant's record from REDCap.
     * This will delete ALL data associated with this record_id across ALL events.
     *
     * Note: The API token must have "Delete Records" permission in REDCap.
     * @deprecated Use markAsWithdrawn() instead to leave an audit trail.
     */
    @Deprecated("Use markAsWithdrawn() to leave an audit trail", ReplaceWith("markAsWithdrawn(studyId)"))
    suspend fun deleteRecord(studyId: String): RedcapResult {
        val token = tokenManager.getToken()

        return try {
            val response = api.deleteRecords(
                token = token,
                record = studyId
            )

            if (response.isSuccessful) {
                val body = response.body()?.string()
                Log.d(TAG, "REDCap record deletion successful: $body")
                RedcapResult.Success(studyId)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "REDCap record deletion failed: ${response.code()} - $errorBody")
                RedcapResult.ServerError(response.code(), errorBody ?: "Unknown error")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during deletion", e)
            RedcapResult.NetworkError(e.message ?: "Network error")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during deletion", e)
            RedcapResult.ParseError(e.message ?: "Unexpected error")
        }
    }
}
