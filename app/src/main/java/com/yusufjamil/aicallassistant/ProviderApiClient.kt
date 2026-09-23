package com.yusufjamil.aicallassistant

import android.content.Context

object ProviderApiClient {
    fun test(context: Context, providerId: String): ApiTestResult =
        ProviderAdapters.test(context, providerId)

    fun chat(context: Context, providerId: String, userText: String): String =
        ProviderAdapters.chat(context, providerId, userText)

    fun chatWithFallback(context: Context, userText: String): String =
        ProviderAdapters.chatWithFallback(context, userText)
}