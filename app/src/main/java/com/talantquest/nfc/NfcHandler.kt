package com.talantquest.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NfcAdapter

object NfcHandler {

    private const val PREFIX = "TALANT"

    enum class TagType { QUIZ, CODE, INVEST, EVENT, UNKNOWN }

    data class TagData(val type: TagType, val id: String)

    fun parseIntent(intent: Intent): TagData? {
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            ?: return null
        if (rawMessages.isEmpty()) return null

        val ndefMessage = rawMessages[0] as NdefMessage
        val record = ndefMessage.records.firstOrNull() ?: return null

        val text = try {
            val payload = record.payload
            val statusByte = payload[0].toInt()
            val langLen = statusByte and 0x3f
            String(payload, 1 + langLen, payload.size - 1 - langLen, Charsets.UTF_8)
        } catch (e: Exception) {
            return null
        }

        return parseText(text.trim())
    }

    fun parseText(text: String): TagData? {
        val parts = text.split(":")
        if (parts.size != 3 || parts[0] != PREFIX) return null

        val type = when (parts[1].uppercase()) {
            "QUIZ" -> TagType.QUIZ
            "CODE" -> TagType.CODE
            "INVEST" -> TagType.INVEST
            "EVENT" -> TagType.EVENT
            else -> TagType.UNKNOWN
        }
        return TagData(type, parts[2])
    }

    fun enableForeground(activity: Activity, adapter: NfcAdapter?) {
        adapter ?: return
        val intent = Intent(activity, activity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(
            activity, 0, intent, PendingIntent.FLAG_MUTABLE
        )
        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                try { addDataType("text/plain") } catch (_: Exception) {}
            }
        )
        adapter.enableForegroundDispatch(activity, pending, filters, null)
    }

    fun disableForeground(activity: Activity, adapter: NfcAdapter?) {
        adapter?.disableForegroundDispatch(activity)
    }
}
