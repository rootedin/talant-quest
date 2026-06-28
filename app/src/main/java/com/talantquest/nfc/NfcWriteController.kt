package com.talantquest.nfc

/**
 * 관리자 화면(Compose)이 액티비티에 NFC 쓰기를 요청하기 위한 인터페이스.
 * MainActivity가 구현하며, reader mode로 태그를 감지해 기록한다.
 */
interface NfcWriteController {
    /**
     * 쓰기 세션을 시작한다. 태그가 감지될 때마다 [payloadProvider]가 반환하는
     * 문자열을 기록하고, 결과를 [onResult]로 (메인 스레드에서) 알린다.
     */
    fun startWriteSession(payloadProvider: () -> String, onResult: (WriteResult) -> Unit)

    /** 쓰기 세션을 종료하고 일반 게임 읽기 모드로 복귀한다. */
    fun stopWriteSession()
}
