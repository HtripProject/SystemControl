package com.htrip.systemcontrol.server

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

fun interface ToastExecutor {
    fun execute(context: Context, message: String)
}

class ToastProvider(
    private val context: Context,
    private val executor: ToastExecutor = ToastExecutor { targetContext, message ->
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(targetContext.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    },
) {
    fun show(message: String?): Boolean {
        val normalizedMessage = message?.trim().orEmpty()
        if (normalizedMessage.isEmpty()) {
            return false
        }

        executor.execute(context, normalizedMessage)
        return true
    }
}
