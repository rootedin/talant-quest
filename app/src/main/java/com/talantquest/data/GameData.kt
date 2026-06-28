package com.talantquest.data

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

data class QuizTag(
    val id: String,
    val questions: List<QuizQuestion>
)

data class CodeTag(
    val id: String,
    val hint: String,
    val answer: String,
    val reward: Int
)

data class EventItem(
    val description: String,
    val flavor: String,
    val amount: Int
)

data class InvestOption(
    val name: String,
    val emoji: String,
    val multiplier: Float,
    val successRate: Float,
    val description: String
)

object GameData {

    val investOptions = listOf(
        InvestOption("안전 투자", "🌱", 1.5f, 0.70f, "성공률 70% · 실패 시 원금 절반 반환"),
        InvestOption("일반 투자", "💼", 2.0f, 0.50f, "성공률 50% · 실패 시 원금 전액 손실"),
        InvestOption("고위험 투자", "🎰", 3.0f, 0.30f, "성공률 30% · 실패 시 원금 전액 손실")
    )

    val eventPool = listOf(
        EventItem("성령의 바람이 불어왔다!", "🍀 은혜가 임했습니다", +100),
        EventItem("오병이어의 기적!", "🐟 기적이 일어났습니다", +50),
        EventItem("만나가 내렸다!", "☁️ 하늘의 양식을 받았습니다", +80),
        EventItem("솔로몬의 지혜 보상!", "👑 지혜로운 선택입니다", +120),
        EventItem("감사 헌금 타임!", "💝 감사함으로 드립니다", +30),
        EventItem("다윗의 물맷돌!", "🪨 작지만 강한 힘!", +70),
        EventItem("세금 징수관 등장!", "😰 달란트를 납부해야 합니다", -80),
        EventItem("광야의 시험...", "🏜️ 시련이 찾아왔습니다", -50),
        EventItem("40년 광야 생활", "😢 험난한 여정입니다", -100),
        EventItem("큰 시련이 찾아왔다", "⚡ 믿음으로 이겨내세요", -30),
    )

    val quizTags = listOf(
        QuizTag("01", listOf(
            QuizQuestion("노아의 방주에서 정결한 동물은 몇 쌍씩 태웠나요?",
                listOf("2쌍", "5쌍", "7쌍", "10쌍"), 2),
            QuizQuestion("예수님이 광야에서 금식한 기간은?",
                listOf("20일", "30일", "40일", "50일"), 2),
            QuizQuestion("다음 중 모세가 한 일이 아닌 것은?",
                listOf("홍해를 가름", "반석에서 물을 냄", "사자굴에 들어감", "십계명을 받음"), 2),
            QuizQuestion("오병이어에서 '이어'는 무엇인가요?",
                listOf("포도", "물고기", "감람나무 열매", "무화과"), 1)
        )),
        QuizTag("02", listOf(
            QuizQuestion("사자굴에 들어간 성경 인물은?",
                listOf("모세", "다니엘", "요셉", "엘리야"), 1),
            QuizQuestion("다음 중 예수님의 12제자가 아닌 사람은?",
                listOf("베드로", "요한", "바나바", "마태"), 2),
            QuizQuestion("요셉이 형제들에게 받은 것은?",
                listOf("금반지", "채색옷", "은 30냥", "양 떼"), 1),
            QuizQuestion("골리앗을 물리친 사람은?",
                listOf("사울", "요나단", "다윗", "삼손"), 2)
        )),
        QuizTag("03", listOf(
            QuizQuestion("시편은 성경에서 몇 번째 책인가요?",
                listOf("17번째", "19번째", "21번째", "23번째"), 1),
            QuizQuestion("예수님이 태어나신 곳은?",
                listOf("나사렛", "예루살렘", "베들레헴", "여리고"), 2),
            QuizQuestion("주기도문 '우리에게 일용할 ___를 주시고'의 빈칸은?",
                listOf("은혜", "양식", "지혜", "평안"), 1),
            QuizQuestion("바울이 전도 여행 전 원래 하던 일은?",
                listOf("어부", "세리", "천막 제조업", "목수"), 2)
        )),
        QuizTag("04", listOf(
            QuizQuestion("성경에서 가장 짧은 구절로 알려진 것은?",
                listOf("하나님이 빛이 있으라", "예수께서 우시더라", "아멘", "주여"), 1),
            QuizQuestion("여리고 성벽이 무너질 때 이스라엘 백성이 한 것은?",
                listOf("기도", "외침과 나팔 소리", "금식", "제사"), 1),
            QuizQuestion("엘리야를 하늘로 데려간 것은?",
                listOf("큰 바람", "불 말과 불 수레", "구름", "천사"), 1),
            QuizQuestion("탕자의 비유에서 아버지가 돌아온 아들에게 씌워준 것은?",
                listOf("면류관", "가락지와 신발", "채색옷", "모두 맞음"), 3)
        )),
        QuizTag("05", listOf(
            QuizQuestion("사도행전은 누가 썼나요?",
                listOf("바울", "베드로", "누가", "요한"), 2),
            QuizQuestion("'여호와는 나의 목자시니'로 시작하는 시편은?",
                listOf("시편 1편", "시편 23편", "시편 100편", "시편 150편"), 1),
            QuizQuestion("다음 중 구약성경이 아닌 것은?",
                listOf("말라기", "에스더", "요나", "빌립보서"), 3),
            QuizQuestion("삼손의 힘의 원천은?",
                listOf("기도", "믿음", "머리카락", "하나님의 영"), 2)
        )),
        QuizTag("06", listOf(
            QuizQuestion("예수님의 첫 번째 기적은?",
                listOf("물 위를 걸음", "물을 포도주로 바꿈", "오병이어", "죽은 자를 살림"), 1),
            QuizQuestion("베드로의 원래 이름은?",
                listOf("시몬", "요한", "안드레", "야고보"), 0),
            QuizQuestion("창세기 다음에 오는 책은?",
                listOf("레위기", "민수기", "출애굽기", "신명기"), 2),
            QuizQuestion("열두 지파 중 요셉의 두 아들로 나뉜 지파는?",
                listOf("에브라임과 므낫세", "유다와 베냐민", "시므온과 레위", "단과 납달리"), 0)
        )),
        QuizTag("07", listOf(
            QuizQuestion("룻기에서 룻의 시어머니 이름은?",
                listOf("라합", "드보라", "나오미", "한나"), 2),
            QuizQuestion("다음 중 예수님의 비유가 아닌 것은?",
                listOf("탕자의 비유", "선한 사마리아인", "달란트 비유", "홍해 도하"), 3),
            QuizQuestion("사도 바울이 처음 불린 이름은?",
                listOf("사울", "실라", "아나니아", "스데반"), 0),
            QuizQuestion("성경에서 가장 긴 장(章)은?",
                listOf("시편 119편", "신명기 28장", "이사야 40장", "열왕기상 8장"), 0)
        )),
        QuizTag("08", listOf(
            QuizQuestion("요나는 어느 도시에 가서 회개를 외치라 명받았나요?",
                listOf("예루살렘", "바벨론", "니느웨", "애굽"), 2),
            QuizQuestion("에스더는 어느 나라의 왕비가 되었나요?",
                listOf("바벨론", "페르시아", "앗수르", "이집트"), 1),
            QuizQuestion("예수님이 세례를 받으신 강은?",
                listOf("유프라테스 강", "티그리스 강", "요단 강", "나일 강"), 2),
            QuizQuestion("다음 중 바울 서신이 아닌 것은?",
                listOf("로마서", "갈라디아서", "히브리서", "빌립보서"), 2)
        )),
        QuizTag("09", listOf(
            QuizQuestion("예수님을 은 30냥에 판 사람은?",
                listOf("베드로", "가룟 유다", "도마", "빌립"), 1),
            QuizQuestion("성경의 마지막 책은?",
                listOf("유다서", "요한일서", "요한계시록", "히브리서"), 2),
            QuizQuestion("다윗이 왕이 되기 전 한 일은?",
                listOf("목수", "양치기", "어부", "세리"), 1),
            QuizQuestion("예수님이 부활하신 날은?",
                listOf("안식일 (토요일)", "주일 (일요일)", "월요일", "금요일"), 1)
        )),
        QuizTag("10", listOf(
            QuizQuestion("오순절 성령 강림 때 몇 명이 모여 있었나요?",
                listOf("70명", "100명", "120명", "200명"), 2),
            QuizQuestion("성경에서 에녹에 대한 설명으로 맞는 것은?",
                listOf("홍수에서 살아남음", "죽음을 보지 않고 하나님께 데려가짐", "불 수레를 타고 올라감", "바다에서 기적을 행함"), 1),
            QuizQuestion("모세가 십계명을 받은 산은?",
                listOf("감람 산", "시내 산", "갈멜 산", "헐몬 산"), 1),
            QuizQuestion("예수님이 부활 후 40일간 지상에 계시다가 올라가신 것을?",
                listOf("변모", "승천", "재림", "강림"), 1)
        ))
    )

    val codeTags = listOf(
        CodeTag("01", "강당 무대 왼쪽 기둥 아래를 살펴봐!", "JOY1", 120),
        CodeTag("02", "식당 게시판 뒤편에 숨겨진 암호를 찾아라!", "HOPE2", 100),
        CodeTag("03", "예배실 피아노 아래에 비밀이 있다!", "LOVE3", 130),
        CodeTag("04", "화장실 앞 게시판 오른쪽 모서리를 확인해봐!", "FAITH4", 110),
        CodeTag("05", "숙소 입구 신발장 맨 윗칸에 뭔가 있다!", "PEACE5", 120),
        CodeTag("06", "운동장 농구 골대 기둥에 붙어있어!", "GRACE6", 150),
    )

    fun getQuizTag(id: String) = quizTags.find { it.id == id }
    fun getCodeTag(id: String) = codeTags.find { it.id == id }
    fun getRandomEvent() = eventPool.random()
}
