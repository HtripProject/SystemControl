package com.htrip.systemcontrol.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.htrip.systemcontrol.api.IPlatformApi
import com.htrip.systemcontrol.api.ServiceContract

internal class PlatformClient(context: Context) {

    private val applicationContext = context.applicationContext
    private val lock = Any()

    @Volatile
    private var api: IPlatformApi? = null
    private var isBound = false
    private var connectedCallback: (() -> Unit)? = null
    private var errorCallback: ((Throwable) -> Unit)? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val callback: (() -> Unit)?
            synchronized(lock) {
                api = IPlatformApi.Stub.asInterface(service)
                callback = connectedCallback
            }
            callback?.invoke()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            synchronized(lock) {
                api = null
                isBound = false
            }
        }
    }

    fun connect(onConnected: () -> Unit, onError: (Throwable) -> Unit): Boolean {
        synchronized(lock) {
            if (api != null) {
                onConnected()
                return true
            }
            connectedCallback = onConnected
            errorCallback = onError
            if (isBound) {
                return true
            }
        }

        val intent = Intent(ServiceContract.SERVICE_ACTION).apply {
            component = ComponentName(
                ServiceContract.SERVICE_PACKAGE,
                ServiceContract.SERVICE_CLASS,
            )
        }

        return try {
            val bound = applicationContext.bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE,
            )
            synchronized(lock) {
                isBound = bound
            }
            if (!bound) {
                onError(IllegalStateException("PlatformService is unavailable"))
            }
            bound
        } catch (error: SecurityException) {
            onError(error)
            false
        }
    }

    fun showToast(message: String): Boolean {
        val remoteApi = api ?: return false
        return try {
            remoteApi.showToast(message)
        } catch (error: RemoteException) {
            synchronized(lock) {
                api = null
            }
            errorCallback?.invoke(error)
            false
        }
    }

    fun disconnect() {
        val shouldUnbind = synchronized(lock) {
            val wasBound = isBound
            isBound = false
            api = null
            connectedCallback = null
            errorCallback = null
            wasBound
        }
        if (shouldUnbind) {
            applicationContext.unbindService(serviceConnection)
        }
    }
}
