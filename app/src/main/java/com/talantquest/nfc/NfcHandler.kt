package com.talantquest.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.tech.Ndef

object NfcHandler {

    private const val PREFIX = "TALANT"

    enum class TagType { QUIZ, CODE, INVEST, EVENT, UNKNOWN }

    data class TagData(val type: TagType, val id: String)

    fun parseIntent(intent: Intent): TagData? {
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            ?: return null
        if (rawMessages.isEmpty()) return null
        val ndefMessage = rawMessages[0] as? NdefMessage ?: return null
        val record = ndefMessage.records.firstOrNull() ?: return null
        val text = extractText(record) ?: return null
        return parseText(text.trim())
    }

    /** RTD_TEXT(well-known)와 MIME(text/plain) 레코드를 모두 지원한다. */
    private fun extractText(record: NdefRecord): String? {
        return try {
            val payload = record.payload
            if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                record.type.contentEquals(NdefRecord.RTD_TEXT)
            ) {
                val statusByte = payload[0].toInt()
                val langLen = statusByte and 0x3f
                String(payload, 1 + langLen, payload.size - 1 - langLen, Charsets.UTF_8)
            } else {
                String(payload, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            null
        }
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
        // Ndef tech를 함께 등록해 MIME/RTD_TEXT 등 모든 NDEF 태그를 잡는다.
        val techLists = arrayOf(arrayOf(Ndef::class.java.name))
        try {
            adapter.enableForegroundDispatch(activity, pending, filters, techLists)
        } catch (_: Exception) {
            // 액티비티가 resumed 상태가 아닐 때 등 — 무시
        }
    }

    fun disableForeground(activity: Activity, adapter: NfcAdapter?) {
        try { adapter?.disableForegroundDispatch(activity) } catch (_: Exception) {}
    }
}
