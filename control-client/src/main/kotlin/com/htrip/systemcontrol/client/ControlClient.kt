package com.htrip.systemcontrol.client

import android.content.Context

class ControlClient(context: Context) {

    private val platformClient = PlatformClient(context)

    fun connect(onConnected: () -> Unit, onError: (Throwable) -> Unit): Boolean =
        platformClient.connect(onConnected, onError)

    fun showToast(message: String): Boolean = platformClient.showToast(message)

    fun disconnect() = platformClient.disconnect()
}
