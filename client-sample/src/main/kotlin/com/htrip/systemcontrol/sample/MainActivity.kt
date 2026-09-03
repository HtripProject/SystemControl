package com.htrip.systemcontrol.sample

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.htrip.systemcontrol.client.ControlClient

class MainActivity : Activity() {

    private lateinit var controlClient: ControlClient
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button
    private lateinit var showToastButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        controlClient = ControlClient(this)
        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)
        showToastButton = findViewById(R.id.showToastButton)

        connectButton.setOnClickListener { connectToService() }
        showToastButton.setOnClickListener {
            val accepted = controlClient.showToast(getString(R.string.service_toast_message))
            if (!accepted) {
                statusText.text = getString(R.string.status_failed, "Binder 未连接")
            }
        }
    }

    override fun onDestroy() {
        controlClient.disconnect()
        super.onDestroy()
    }

    private fun connectToService() {
        statusText.setText(R.string.status_connecting)
        val bound = controlClient.connect(
            onConnected = {
                statusText.setText(R.string.status_connected)
                showToastButton.isEnabled = true
            },
            onError = { error ->
                statusText.text = getString(
                    R.string.status_failed,
                    error.message ?: error.javaClass.simpleName,
                )
                showToastButton.isEnabled = false
                Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
            },
        )
        if (!bound) {
            showToastButton.isEnabled = false
        }
    }
}
