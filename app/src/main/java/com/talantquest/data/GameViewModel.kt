package com.talantquest.data

import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GameViewModel(private val repo: GameRepository) : ViewModel() {

    val teamName = mutableStateOf(repo.getTeamName())
    val talant = mutableIntStateOf(repo.getTalant())
    val ttsVolume = mutableFloatStateOf(repo.getTtsVolume())

    fun setTtsVolume(v: Float) {
        ttsVolume.floatValue = v
        repo.setTtsVolume(v)
    }

    // 태그 사용 상태가 바뀔 때마다 증가 → 진행도 UI 갱신용
    val tagsVersion = mutableIntStateOf(0)

    val totalQuiz = GameData.quizTags.size
    val totalCode = GameData.codeTags.size

    fun hasTeam(): Boolean = repo.hasTeamName()

    fun setTeamName(name: String) {
        repo.setTeamName(name)
        teamName.value = name
    }

    fun addTalant(amount: Int) {
        repo.addTalant(amount)
        talant.intValue = repo.getTalant()
    }

    fun isTagUsed(tagId: String) = repo.isTagUsed(tagId)

    fun markTagUsed(tagId: String) {
        repo.markTagUsed(tagId)
        tagsVersion.intValue++
    }

    fun quizFound(): Int = GameData.quizTags.count { repo.isTagUsed("QUIZ_${it.id}") }
    fun codeFound(): Int = GameData.codeTags.count { repo.isTagUsed("CODE_${it.id}") }

    fun resetGame() {
        repo.resetAll()
        teamName.value = ""
        talant.intValue = 0
        tagsVersion.intValue++
    }
}

class GameViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(GameRepository(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
