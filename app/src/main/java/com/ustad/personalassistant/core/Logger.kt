package com.ustad.personalassistant.core

import android.util.Log

object UstadLogger {
    private const val TAG = "UstadAssistant"

    fun info(message: String) = Log.i(TAG, message)
    fun warning(message: String) = Log.w(TAG, message)
    fun error(message: String, throwable: Throwable? = null) = Log.e(TAG, message, throwable)
    fun debug(message: String) = Log.d(TAG, message)
}
