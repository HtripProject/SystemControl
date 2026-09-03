package com.htrip.systemcontrol.server

import com.htrip.systemcontrol.api.IPlatformApi

class PlatformApiBinder(
    private val toastProvider: ToastProvider,
) : IPlatformApi.Stub() {
    override fun showToast(message: String?): Boolean = toastProvider.show(message)
}
