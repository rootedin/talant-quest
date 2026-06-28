package com.talantquest.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

sealed class WriteResult {
    data class Success(val payload: String) : WriteResult()
    data class Failure(val reason: String) : WriteResult()
}

object NfcWriter {

    /** 태그에 "text/plain" MIME 레코드로 텍스트를 기록한다. (게임 읽기 필터와 일치) */
    fun writeText(tag: Tag?, text: String): WriteResult {
        if (tag == null) return WriteResult.Failure("태그를 인식하지 못했습니다")

        val record = NdefRecord.createMime("text/plain", text.toByteArray(Charsets.UTF_8))
        val message = NdefMessage(arrayOf(record))

        // 이미 NDEF 포맷된 태그
        Ndef.get(tag)?.let { ndef ->
            return try {
                ndef.connect()
                when {
                    !ndef.isWritable -> WriteResult.Failure("쓰기 금지된 태그입니다")
                    ndef.maxSize < message.toByteArray().size ->
                        WriteResult.Failure("태그 용량이 부족합니다")
                    else -> {
                        ndef.writeNdefMessage(message)
                        WriteResult.Success(text)
                    }
                }
            } catch (e: Exception) {
                WriteResult.Failure(e.message ?: "쓰기 중 오류가 발생했습니다")
            } finally {
                runCatching { ndef.close() }
            }
        }

        // 아직 포맷되지 않은 빈 태그
        NdefFormatable.get(tag)?.let { formatable ->
            return try {
                formatable.connect()
                formatable.format(message)
                WriteResult.Success(text)
            } catch (e: Exception) {
                WriteResult.Failure(e.message ?: "포맷 중 오류가 발생했습니다")
            } finally {
                runCatching { formatable.close() }
            }
        }

        return WriteResult.Failure("NDEF를 지원하지 않는 태그입니다")
    }
}
