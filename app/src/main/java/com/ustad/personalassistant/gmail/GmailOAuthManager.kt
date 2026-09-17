package com.ustad.personalassistant.gmail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.ustad.personalassistant.security.SecureConfigStore
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import org.json.JSONObject

class GmailOAuthManager(private val context: Context, private val store: SecureConfigStore, private val clientId: String) : GmailAuthManager, GmailAccessTokenProvider {
    private val authEndpoint = "https://accounts.google.com/o/oauth2/v2/auth"
    private val tokenEndpoint = "https://oauth2.googleapis.com/token"
    private val redirectUri = "ustad-gmail://oauth2redirect"
    private val scopes = "https://www.googleapis.com/auth/gmail.modify"
    override fun isConnected(): Boolean = store.get("access_token")?.isNotBlank() == true && (store.get("expires_at")?.toLongOrNull() ?: 0L) > System.currentTimeMillis()
    override fun beginAuthorization(): Result<Unit> = runCatching {
        require(clientId.isNotBlank()) { "Gmail OAuth client ID is not configured" }
        val verifier = randomVerifier(); val challenge = Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING); val state = UUID.randomUUID().toString()
        store.put("oauth_verifier", verifier); store.put("oauth_state", state)
        val uri = Uri.parse("$authEndpoint?client_id=${enc(clientId)}&redirect_uri=${enc(redirectUri)}&response_type=code&scope=${enc(scopes)}&access_type=offline&prompt=consent&code_challenge=$challenge&code_challenge_method=S256&state=${enc(state)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun handleRedirect(uri: Uri): Result<Unit> = runCatching {
        val state = uri.getQueryParameter("state").orEmpty(); val expected = store.get("oauth_state").orEmpty(); if (state.isBlank() || state != expected) error("OAuth state mismatch")
        uri.getQueryParameter("error")?.let { error("OAuth failed: $it") }
        val code = uri.getQueryParameter("code").orEmpty(); require(code.isNotBlank()) { "OAuth authorization code missing" }
        val verifier = store.get("oauth_verifier").orEmpty(); require(verifier.isNotBlank()) { "OAuth verifier missing" }
        val form = "code=${enc(code)}&client_id=${enc(clientId)}&redirect_uri=${enc(redirectUri)}&grant_type=authorization_code&code_verifier=${enc(verifier)}"
        val connection = URL(tokenEndpoint).openConnection() as HttpURLConnection; connection.requestMethod = "POST"; connection.doOutput = true; connection.connectTimeout = 10_000; connection.readTimeout = 15_000; connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.outputStream.use { it.write(form.toByteArray()) }; val response = connection.inputStream.bufferedReader().use { it.readText() }; connection.disconnect(); val json = JSONObject(response)
        val access = json.optString("access_token"); require(access.isNotBlank()) { "OAuth access token missing" }; store.put("access_token", access); store.put("expires_at", (System.currentTimeMillis() + json.optLong("expires_in", 3600L) * 1000L - 60_000L).toString()); json.optString("refresh_token").takeIf { it.isNotBlank() }?.let { store.put("refresh_token", it) }; store.remove("oauth_state"); store.remove("oauth_verifier")
        verifyConnection()
    }
    override fun refreshOrReauthenticate(): Result<Unit> = runCatching {
        val refresh = store.get("refresh_token") ?: error("Gmail refresh token unavailable")
        val form = "client_id=${enc(clientId)}&refresh_token=${enc(refresh)}&grant_type=refresh_token"; val c = URL(tokenEndpoint).openConnection() as HttpURLConnection; c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 10_000; c.readTimeout = 15_000; c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); c.outputStream.use { it.write(form.toByteArray()) }; val code = c.responseCode; val response = (if (code in 200..299) c.inputStream else c.errorStream).bufferedReader().use { it.readText() }; c.disconnect(); if (code !in 200..299) error("OAuth token refresh failed"); val json = JSONObject(response); store.put("access_token", json.getString("access_token")); store.put("expires_at", (System.currentTimeMillis() + json.optLong("expires_in", 3600L) * 1000L - 60_000L).toString()); verifyConnection()
    }
    override fun accessToken(): Result<String> = if (isConnected()) Result.success(store.get("access_token")!!) else refreshOrReauthenticate().flatMap { Result.success(store.get("access_token") ?: error("Gmail access token unavailable")) }
    override fun disconnect() { store.remove("access_token"); store.remove("refresh_token"); store.remove("expires_at"); store.remove("oauth_state"); store.remove("oauth_verifier") }
    private fun verifyConnection() { val token = store.get("access_token") ?: error("Gmail access token unavailable"); val c = URL("https://gmail.googleapis.com/gmail/v1/users/me/profile").openConnection() as HttpURLConnection; c.requestMethod = "GET"; c.connectTimeout = 10_000; c.readTimeout = 10_000; c.setRequestProperty("Authorization", "Bearer $token"); val code = c.responseCode; c.disconnect(); if (code !in 200..299) error("Gmail connection verification failed") }
    private fun randomVerifier(): String = Base64.encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) }, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
}
