package com.talantquest

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.talantquest.nfc.NfcHandler
import com.talantquest.ui.TalantQuestApp
import com.talantquest.ui.theme.TalantQuestTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private val nfcTagEvent = mutableStateOf<NfcHandler.TagData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        enableEdgeToEdge()
        setContent {
            TalantQuestTheme {
                TalantQuestApp(
                    nfcTagEvent = nfcTagEvent,
                    onNfcConsumed = { nfcTagEvent.value = null }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NfcHandler.enableForeground(this, nfcAdapter)
    }

    override fun onPause() {
        super.onPause()
        NfcHandler.disableForeground(this, nfcAdapter)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NfcHandler.parseIntent(intent)?.let { nfcTagEvent.value = it }
    }
}
