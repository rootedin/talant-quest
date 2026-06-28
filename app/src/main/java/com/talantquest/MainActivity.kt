package com.talantquest

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.talantquest.nfc.NfcHandler
import com.talantquest.nfc.NfcWriteController
import com.talantquest.nfc.NfcWriter
import com.talantquest.nfc.WriteResult
import com.talantquest.ui.TalantQuestApp
import com.talantquest.ui.theme.TalantQuestTheme

class MainActivity : ComponentActivity(), NfcWriteController {

    private var nfcAdapter: NfcAdapter? = null
    private val nfcTagEvent = mutableStateOf<NfcHandler.TagData?>(null)

    // 관리자 쓰기 세션 상태
    private var writeSessionActive = false
    private var writePayloadProvider: (() -> String)? = null
    private var writeOnResult: ((WriteResult) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        enableEdgeToEdge()
        setContent {
            TalantQuestTheme {
                TalantQuestApp(
                    nfcTagEvent = nfcTagEvent,
                    onNfcConsumed = { nfcTagEvent.value = null },
                    writeController = this
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (writeSessionActive) enableReaderMode()
        else NfcHandler.enableForeground(this, nfcAdapter)
    }

    override fun onPause() {
        super.onPause()
        if (writeSessionActive) runCatching { nfcAdapter?.disableReaderMode(this) }
        else NfcHandler.disableForeground(this, nfcAdapter)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 쓰기 세션 중에는 reader mode 콜백이 태그를 처리하므로 무시
        if (writeSessionActive) return
        NfcHandler.parseIntent(intent)?.let { nfcTagEvent.value = it }
    }

    // --- NfcWriteController ---

    override fun startWriteSession(
        payloadProvider: () -> String,
        onResult: (WriteResult) -> Unit
    ) {
        writePayloadProvider = payloadProvider
        writeOnResult = onResult
        writeSessionActive = true
        NfcHandler.disableForeground(this, nfcAdapter)
        enableReaderMode()
    }

    override fun stopWriteSession() {
        if (!writeSessionActive) return
        writeSessionActive = false
        writePayloadProvider = null
        writeOnResult = null
        runCatching { nfcAdapter?.disableReaderMode(this) }
        NfcHandler.enableForeground(this, nfcAdapter)
    }

    private fun enableReaderMode() {
        val adapter = nfcAdapter ?: return
        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        adapter.enableReaderMode(this, { tag: Tag ->
            val provider = writePayloadProvider ?: return@enableReaderMode
            val onResult = writeOnResult ?: return@enableReaderMode
            val result = NfcWriter.writeText(tag, provider())
            runOnUiThread { onResult(result) }
        }, flags, null)
    }
}
