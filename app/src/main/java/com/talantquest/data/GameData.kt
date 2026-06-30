package com.talantquest.data

import androidx.annotation.DrawableRes
import com.talantquest.R

/**
 * 퀴즈 문항. 객관식(MultipleChoice)과 주관식(ShortAnswer)을 함께 담는다.
 * 한 [QuizTag] 안에 두 유형을 섞어도 된다.
 *
 * 문제에 사진을 넣으려면 res/drawable 에 이미지를 추가한 뒤
 * imageRes = R.drawable.파일이름 으로 지정한다. (예: R.drawable.quiz_calendar)
 * imageRes 를 생략(null)하면 이미지 없는 일반 문제다.
 */
sealed class QuizQuestion {
    abstract val question: String

    /** 선택적 이미지(drawable 리소스 ID). null이면 이미지 없음. */
    @get:DrawableRes
    abstract val imageRes: Int?

    /** 객관식 — 보기 중 하나를 고른다. */
    data class MultipleChoice(
        override val question: String,
        val options: List<String>,
        val correctIndex: Int,
        @DrawableRes override val imageRes: Int? = null
    ) : QuizQuestion()

    /**
     * 주관식 — 직접 입력한다.
     * @param answers 허용되는 정답들. 첫 번째가 대표 정답(정답 공개·TTS에 사용).
     * @param hints   단계별 힌트. 펼칠 때마다 [Hint.penalty]만큼 점수가 깎인다.
     * @param explanation 정답을 맞히거나 실패가 확정된 뒤 보여줄 해설.
     * @param imageRes 선택적 이미지(drawable 리소스 ID).
     */
    data class ShortAnswer(
        override val question: String,
        val answers: List<String>,
        val maxPoints: Int = 100,
        val hints: List<Hint> = emptyList(),
        val explanation: String? = null,
        @DrawableRes override val imageRes: Int? = null
    ) : QuizQuestion() {
        /** 공백·대소문자를 무시하고 허용 정답 중 하나와 일치하는지 검사. */
        fun matches(input: String): Boolean {
            val normalized = input.normalizeForCompare()
            return answers.any { it.normalizeForCompare() == normalized }
        }
    }

    /** 오답 안내·정답 표시에 쓰는 대표 정답 텍스트. */
    val correctAnswerText: String
        get() = when (this) {
            is MultipleChoice -> options[correctIndex]
            is ShortAnswer -> answers.first()
        }
}

private fun String.normalizeForCompare(): String =
    trim().replace(Regex("\\s+"), "").lowercase()

/** 주관식 단계별 힌트. 펼칠 때 점수가 [penalty]만큼 깎인다. */
data class Hint(val text: String, val penalty: Int)

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
    val betAmount: Int,
    val description: String
)

object GameData {

    const val INVEST_SUCCESS_RATE = 0.50f

    val investOptions = listOf(
        InvestOption("소액 투자", "🌱", 50, "50달란트 → 성공 시 2배(100달란트) 반환"),
        InvestOption("중액 투자", "💼", 100, "100달란트 → 성공 시 2배(200달란트) 반환"),
        InvestOption("고액 투자", "🎰", 300, "300달란트 → 성공 시 2배(600달란트) 반환"),
    )

    val eventPool = listOf(
        EventItem("모든 것이 합력하여 선을 이루다", "✨ 하나님을 사랑하는 자들에게는 모든 것이 합력하여 선을 이루느니라 — 롬 8:28", +130),
        EventItem("주께서 목자가 되어 주셨다!", "🐑 주는 나의 목자시니 내가 부족함이 없으리로다 — 시 23:1", +90),
        EventItem("왕국을 먼저 구한 상급!", "👑 너희는 첫째로 하나님의 왕국과 그분의 의를 구하라 — 마 6:33", +110),
        EventItem("강하고 크게 용기를 내라!", "🛡 마음을 강하게 하고 크게 용기를 내라 주 네 하나님이 너와 함께하느니라 — 수 1:9", +80),
        EventItem("그리스도 안에서 모든 것을 하다!", "💪 나를 강하게 하시는 그리스도를 통해 내가 모든 것을 할 수 있느니라 — 빌 4:13", +60),
        EventItem("말씀이 발의 등불이 되었다!", "💡 주의 말씀은 내 발에 등불이요, 내 길에 빛이니이다 — 시 119:105", +30),
        EventItem("죄의 삯은 사망이다", "💀 죄의 삯은 사망이나 하나님의 선물은 예수 그리스도 우리 주를 통한 영원한 생명이니라 — 롬 6:23", -100),
        EventItem("나의 죄가 나를 찾아냈다!", "😰 너희 죄가 너희를 찾아낼 줄 분명히 알라 — 민 32:23", -80),
        EventItem("교만이 멸망을 불러왔다", "😔 멸망에 앞서 교만이 나가며 넘어짐에 앞서 거만한 영이 있느니라 — 잠 16:18", -50),
        EventItem("방심하다가 넘어졌다", "⚠ 서 있는 줄로 생각하는 자는 넘어지지 않도록 주의할지니라 — 고전 10:12", -20),
    )

    val quizTags = listOf(
        QuizTag("01", listOf(
            QuizQuestion.MultipleChoice("새우물침례교회 담임목사님의 성함은?",
                listOf("김장진", "김상신", "김장신", "김상진"), 3),
            QuizQuestion.MultipleChoice("새우물침례교회의 주인은?",
                listOf("김상진 목사님", "박장균 형제님", "하나님", "나님"), 2),
            QuizQuestion.MultipleChoice("새우물침례교회의 표어는 무엇인가요?",
                listOf("행복이 넘치는 그리스도인", "기본에 충실한 그리스도인", "서로 사랑하는 그리스도인", "성령의 불타는 그리스도인"), 1),
            QuizQuestion.MultipleChoice(
                "다음 중 새우물침례교회 4대 비전에 해당하지 않는 것은?",
                listOf(
                    "하나님이 주인이 되는 교회",
                    "다음 세대의 부흥을 이루는 교회",
                    "선한 사람으로 양육하는 교회",
                    "하나님의 왕국과 그분의 의를 최우선의 가치로 두는 교회",
                    "바른 예배를 세워가는 교회"
                ),
                2
            )
        )),
        // 기초교리 1강 — 교리론
        QuizTag("02", listOf(
            QuizQuestion.MultipleChoice("'교리(Doctrine)'를 뜻하는 헬라어 '디다케(Didache)'의 원래 의미는?",
                listOf("찬양과 경배", "가르침", "예언의 말씀", "믿음의 행위"), 1),
            QuizQuestion.MultipleChoice("사탄이 집중적으로 공격하는 3대 교리에 해당하지 않는 것은?",
                listOf("삼위일체 교리", "예수 그리스도의 성육신", "성경의 무오성", "천지창조 6일"), 3),
            QuizQuestion.MultipleChoice("바른 교리를 배워야 하는 이유로 맞지 않는 것은?",
                listOf("이단 교리로부터 보호받기 위해", "말씀에 대한 분별력을 갖기 위해", "하나님의 계획을 이해하기 위해", "교회를 빠르게 성장시키기 위해"), 3),
            QuizQuestion.MultipleChoice("교리 교육의 최종 목적은 무엇인가?",
                listOf("신학 학위를 얻는 것", "그리스도의 군사로 온전히 무장하는 것", "더 많은 성경 지식을 암기하는 것", "교회에서 직분을 받는 것"), 1)
        )),
        // 기초교리 2강 — 성경론
        QuizTag("03", listOf(
            QuizQuestion.MultipleChoice("성경을 기록한 인간 저자(기자)는 약 몇 명인가요?",
                listOf("약 12명", "약 40명", "약 70명", "약 120명"), 1),
            QuizQuestion.MultipleChoice("성경의 총 주제는 무엇인가요?",
                listOf("이스라엘의 역사", "하나님의 율법과 계명", "영원한 생명이신 예수 그리스도", "천지창조와 종말"), 2),
            QuizQuestion.MultipleChoice("외경(Apocrypha)에 대한 올바른 설명은?",
                listOf("하나님의 영감으로 기록된 정경이다", "구약에 포함되어야 할 책들이다", "하나님의 영감을 받지 않은 책이다", "초대 교회가 정경으로 채택했다"), 2),
            QuizQuestion.MultipleChoice("성경 66권은 구약 몇 권, 신약 몇 권으로 구성되어 있나요?",
                listOf("구약 36권 / 신약 30권", "구약 39권 / 신약 27권", "구약 40권 / 신약 26권", "구약 45권 / 신약 21권"), 1)
        )),
        // 기초교리 3강 — 삼위일체
        QuizTag("04", listOf(
            QuizQuestion.MultipleChoice("삼위일체에 대한 올바른 설명은?",
                listOf("하나님은 세 분이 계신다", "아버지·아들·성령은 각각 다른 신이다", "한 하나님이 세 위격으로 존재하신다", "성령은 하나님의 능력일 뿐이다"), 2),
            QuizQuestion.MultipleChoice("다음 중 삼위일체를 부정하는 이단 교리가 아닌 것은?",
                listOf("아리우스주의(Arianism)", "양태론(Modalism)", "단일신론(Unitarianism)", "완전영감설(Verbal Inspiration)"), 3),
            QuizQuestion.MultipleChoice("아리우스주의(Arianism)의 핵심 주장은?",
                listOf("성령은 존재하지 않는다", "예수 그리스도는 창조된 피조물이다", "아버지와 아들은 동일한 위격이다", "하나님은 여럿이시다"), 1),
            QuizQuestion.MultipleChoice("출애굽기 3장에서 하나님께서 모세에게 밝히신 이름(흠정역)은?",
                listOf("야훼(YHWH)", "엘로힘(Elohim)", "I AM THAT I AM(나는 스스로 있는 자니라)", "아도나이(Adonai)"), 2)
        )),
        // 기초교리 4강 — 죄·구원·성화
        QuizTag("05", listOf(
            QuizQuestion.MultipleChoice("'죄(Sin)'를 뜻하는 헬라어 '하마르티아(hamartia)'의 원래 의미는?",
                listOf("하나님을 배신하다", "과녁에서 벗어나다", "율법을 어기다", "사탄을 따르다"), 1),
            QuizQuestion.MultipleChoice("사람이 구원을 받는 방법은 무엇인가요?",
                listOf("선한 행위를 많이 쌓아서", "율법을 완벽하게 지켜서", "예수 그리스도를 믿음으로", "교회에 열심히 출석해서"), 2),
            QuizQuestion.MultipleChoice("한 번 얻은 구원이 취소될 수 없는 근거는?",
                listOf("구원받은 성도는 더 이상 죄를 짓지 않기 때문", "성령께서 구속의 날까지 성도를 인치셨기 때문", "교회 등록부에 이름이 있기 때문", "침례를 받았기 때문"), 1),
            QuizQuestion.MultipleChoice("성화(Sanctification)란 무엇인가요?",
                listOf("구원을 받는 순간의 경험", "침례를 통해 죄를 씻는 것", "구원받은 성도가 그리스도의 거룩함을 닮아가는 영적 성장", "성경을 전부 암송하는 것"), 2)
        )),
        // 기초교리 5강 — 천국과 지옥
        QuizTag("06", listOf(
            QuizQuestion.MultipleChoice("지옥(Hell)은 실제로 존재하는가?",
                listOf("예, 실제로 존재한다", "아니오, 비유적 표현이다", "아직 알 수 없다", "죽으면 모든 것이 소멸된다"), 0),
            QuizQuestion.MultipleChoice("성경에서 천국(하늘)은 몇 단계로 나뉘나요?",
                listOf("1단계", "2단계", "3단계", "7단계"), 2),
            QuizQuestion.MultipleChoice("타락한 천사들이 현재 갇혀 있는 지옥의 이름은?",
                listOf("하데스(Hades)", "스올(Sheol)", "게헨나(Gehenna)", "타르타로스(Tartarus)"), 3),
            QuizQuestion.MultipleChoice("카톨릭이 주장하는 '연옥(Purgatory)'에 대한 바른 설명은?",
                listOf("성경에 명확히 기록된 교리다", "어거스틴이 만들어낸 비성경적 교리다", "루터가 종교개혁으로 정립한 교리다", "신약성경에 근거한다"), 1)
        )),
        // 기초교리 6강 — 천사론
        QuizTag("07", listOf(
            QuizQuestion.MultipleChoice("천사의 기원은 무엇인가?",
                listOf("진화를 통해 생겨났다", "하나님께서 창조하셨다", "사람이 죽으면 천사가 된다", "성경에 그 기원이 기록되지 않았다"), 1),
            QuizQuestion.MultipleChoice("천사에 대한 설명으로 잘못된 것은?",
                listOf("영적인 존재다", "하나님이 창조하셨다", "성경에 여자 천사가 등장한다", "성도를 섬기는 사역을 한다"), 2),
            QuizQuestion.MultipleChoice("스랍(Seraphim)이 가진 날개의 수는?",
                listOf("2개", "4개", "6개", "12개"), 2),
            QuizQuestion.MultipleChoice("루시퍼가 타락 전에 맡은 역할은?",
                listOf("천사들의 대장(천사장)", "하나님 보좌 앞의 스랍", "하나님의 거룩한 산을 지키는 덮는 그룹", "하늘의 예배 인도자"), 2)
        )),
        // 기초교리 7강 — 마귀론
        QuizTag("08", listOf(
            QuizQuestion.MultipleChoice("루시퍼가 하늘에서 쫓겨난 근본 이유는?",
                listOf("하나님의 창조 계획에 반대했기 때문", "스스로 하나님처럼 되려고 반역했기 때문", "성도들을 유혹하다 발각됐기 때문", "다른 천사들을 부추겨 싸움을 일으켰기 때문"), 1),
            QuizQuestion.MultipleChoice("귀신(악한 영)은 실제로 존재하는가?",
                listOf("예, 성경에 기록된 실제 존재다", "아니오, 상상이나 미신의 산물이다", "예, 하지만 지금은 활동하지 않는다", "아니오, 심리적 현상에 불과하다"), 0),
            QuizQuestion.MultipleChoice("마귀를 대적하는 성경적 방법은? (약 4:7)",
                listOf("마귀와 대화하며 설득한다", "하나님께 복종하고 마귀를 대적한다", "특별한 기도 의식을 행한다", "마귀를 완전히 무시한다"), 1),
            QuizQuestion.MultipleChoice("마귀의 최후는 어디인가?",
                listOf("완전히 소멸된다", "무저갱에 영원히 갇힌다", "불과 유황 호수(게헨나)에 던져진다", "하데스에 계속 머문다"), 2)
        )),
        // 기초교리 8강 — 성도와 예배
        QuizTag("09", listOf(
            QuizQuestion.MultipleChoice("성도(saint)의 성경적 의미는?",
                listOf("도덕적으로 뛰어난 사람", "구분된 자, 거룩한 자", "교회에서 오래 신앙생활한 사람", "카톨릭이 인정한 성인"), 1),
            QuizQuestion.MultipleChoice("성도가 주일에 모여 예배드리는 이유는?",
                listOf("안식일 계명을 지키기 위해", "예수님이 주일에 태어나셨기 때문", "예수님이 부활하신 첫날을 기념하기 위해", "교회의 오랜 전통이기 때문"), 2),
            QuizQuestion.MultipleChoice("하나님께서 찾으시는 참된 예배의 조건은?",
                listOf("큰 소리로 열정적인 찬양", "영과 진리로 드리는 예배", "화려한 예배 시설과 음향", "감동적인 설교와 뜨거운 기도"), 1),
            QuizQuestion.MultipleChoice("하나님이 가인의 제사를 받지 않으신 주된 이유는?",
                listOf("제물의 종류가 틀렸기 때문", "가인이 믿음으로 드리지 않았기 때문", "제사 드리는 시간이 맞지 않았기 때문", "가인이 아담의 아들이기 때문"), 1)
        )),
        // 기초교리 9강 — 교회의 정체성
        QuizTag("10", listOf(
            QuizQuestion.MultipleChoice("교회를 뜻하는 헬라어 '에클레시아(Ekklesia)'의 의미는?",
                listOf("거룩한 예배당 건물", "부르심을 받은 자들의 모임(집회)", "성직자 조직", "기도와 찬양의 장소"), 1),
            QuizQuestion.MultipleChoice("교회의 머리(주인)는 누구인가요?",
                listOf("담임 목사", "장로회", "예수 그리스도", "교인 전체"), 2),
            QuizQuestion.MultipleChoice("신약에서 '성전(성령의 전)'이 가리키는 것은?",
                listOf("예루살렘의 성전 건물", "교회 건물", "성령께서 거하시는 성도의 몸", "하늘에 있는 성소"), 2),
            QuizQuestion.MultipleChoice("'신사도 운동(New Apostolic Reformation)'에 대한 바른 평가는?",
                listOf("초대 교회 사도직을 정당하게 계승한 운동이다", "성령 충만한 정통 부흥 운동이다", "성경에 근거하지 않은 거짓된 활동이다", "침례교의 핵심 사역이다"), 2)
        )),
        // 주관식 — 도전 퍼즐 (직접 입력 + 단계별 힌트 + 해설)
        QuizTag("11", listOf(
            QuizQuestion.ShortAnswer(
                "모든 우상을 제거하라!",
                answers = listOf("-40", "−40", "마이너스40"),
                maxPoints = 200,
                hints = listOf(
                    Hint("우상들의 위치를 확인해보면?", 10),
                    Hint("핸드폰의 숫자패드를 표에 대입해보면?", 20)
                ),
                explanation = "숫자패드처럼 아래에서부터 1·2·3, 4·5·6, 7·8·9 입니다. " +
                    "가운데 5번 자리는 '하나님'이니 그대로 두고, 나머지 우상 여덟을 모두 '제거(−)'하면 " +
                    "−1−2−3−4−6−7−8−9 = −40 이 됩니다.",
                imageRes = R.drawable.idol_table
            )
        )),
        // 주관식 — 한글 자음 퍼즐 (표 안에서 자음 모양 찾기)
        QuizTag("12", listOf(
            QuizQuestion.ShortAnswer(
                "ㄱㄴㄷㅁ = ?",
                answers = listOf("7365"),
                maxPoints = 200,
                hints = listOf(
                    Hint("표 안에 자음이 보인다", 20)
                ),
                explanation = "ㄱ은 좌측하단, ㄴ은 우측상단, ㄷ은 우측중단, ㅁ은 중앙에 있다. 각 위치에 해당하는 숫자가 정답이다.",
                imageRes = R.drawable.consonant_table
            )
        )),
        // 주관식 — 달력 빨강 더하기 퍼즐
        QuizTag("13", listOf(
            QuizQuestion.ShortAnswer(
                "역시 빨간날은 쉬어야지! 그럼 이달의 빨강을 다 더해볼까?!",
                answers = listOf("105"),
                maxPoints = 200,
                hints = listOf(
                    Hint("빨간 글씨 중 숫자가 아닌 것을 숫자로 바꿔보자!", 20)
                ),
                explanation = "보여지는 모든 숫자를 더하고 특별히 삼일절(=31), 日(=8) 을 더하면 됩니다.",
                imageRes = R.drawable.quiz_calendar
            )
        )),
        // 주관식 — 자음·모음 분해 암호 (빨간 자모를 숫자로)
        QuizTag("14", listOf(
            QuizQuestion.ShortAnswer(
                "비밀번호는?",
                answers = listOf("1200"),
                maxPoints = 200,
                hints = listOf(
                    Hint("위 문장과 아래 모양은 표현 방식의 차이일 뿐 동일하다.", 10),
                    Hint("아래 모양은 글자의 자음과 모음을 나눈 것이다. 빨간색에 위치한 자음과 모음을 자세히 살펴보자.", 30)
                ),
                explanation = "아래 각 모양은 글자의 자음과 모음을 나눈 것이고, " +
                    "빨간색에 해당하는 글자를 숫자로 변환하면 ㅣ ㄹ ㅇ ㅇ = 1200 이다.",
                imageRes = R.drawable.password_1200
            )
        )),
        // 주관식 — 착시 문장 퍼즐 (FI 가 A 처럼 보인다)
        QuizTag("15", listOf(
            QuizQuestion.ShortAnswer(
                "숨어있는 문장을 찾아라!",
                answers = listOf("YOU ARE MAN", "you are man", "유아맨"),
                maxPoints = 200,
                hints = listOf(
                    Hint("때때로 멀리서 정답이 보일 때도 있다.", 10),
                    Hint("붙어있는 두 알파벳이 하나의 알파벳으로 보일 때도 있다.", 20)
                ),
                explanation = "FI 를 살짝 붙이면 A처럼 보입니다. " +
                    "그래서 FIRE 는 ARE, MFIN 은 MAN 으로 읽혀 'YOU ARE MAN' 이 됩니다.",
                imageRes = R.drawable.quiz_you_are_man
            )
        )),
    )

    val codeTags = listOf(
        CodeTag("01", "신발장", "MEEKNESS6", 100),
        CodeTag("02", "냉동고", "LOVE9", 100),
        CodeTag("03", "1층 화장실", "JOY4", 100),
        CodeTag("04", "2층 화장실", "PEACE7", 100),
        CodeTag("05", "3층 화장실", "LONGSUFFERING3", 100),
        CodeTag("06", "당구대", "GENTLENESS8", 100),
        CodeTag("07", "컴퓨터", "GOODNESS5", 100),
        CodeTag("08", "커피머신", "FAITH2", 100),
        CodeTag("09", "3층 복도", "TEMPERANCE1", 100),
        CodeTag("10", "냉장고", "HOLYSPIRIT", 100),
    )

    val eventTagIds = (1..10).map { "%02d".format(it) }
    val investTagIds = (1..3).map { "%02d".format(it) }

    fun getQuizTag(id: String) = quizTags.find { it.id == id }
    fun getCodeTag(id: String) = codeTags.find { it.id == id }
    fun getRandomEvent() = eventPool.random()
}
