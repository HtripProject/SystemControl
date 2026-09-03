package com.htrip.systemcontrol.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.htrip.systemcontrol.server.PlatformApiBinder
import com.htrip.systemcontrol.server.ToastProvider

class PlatformService : Service() {

    private var platformApiBinder: PlatformApiBinder? = null

    override fun onCreate() {
        super.onCreate()
        platformApiBinder = PlatformApiBinder(ToastProvider(applicationContext))
    }

    override fun onBind(intent: Intent?): IBinder? = platformApiBinder

    override fun onDestroy() {
        platformApiBinder = null
        super.onDestroy()
    }
}
