package com.htrip.systemcontrol.server

import android.content.ContextWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToastProviderTest {

    @Test
    fun showForwardsTrimmedMessageToExecutor() {
        val messages = mutableListOf<String>()
        val provider = ToastProvider(
            context = ContextWrapper(null),
            executor = ToastExecutor { _, message -> messages += message },
        )

        assertTrue(provider.show("  hello  "))
        assertEquals(listOf("hello"), messages)
    }

    @Test
    fun showRejectsBlankMessage() {
        val provider = ToastProvider(
            context = ContextWrapper(null),
            executor = ToastExecutor { _, _ -> error("blank messages must not be executed") },
        )

        assertFalse(provider.show("   "))
    }

}
