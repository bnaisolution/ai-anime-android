package org.dpdns.bnai.anime.core.runtime

import kotlin.math.roundToInt

data class RuntimeContract(
    val targetDurationMin: Int,
    val targetDurationSec: Int,
    val dialogueWordBudget: Int,
    val screenplayWordLow: Int,
    val screenplayWordHigh: Int,
    val actCount: Int,
    val beatLow: Int,
    val beatHigh: Int,
    val sceneLow: Int,
    val sceneHigh: Int,
    val shotLow: Int,
    val shotHigh: Int,
    val renderTier: String
) {
    val screenplayWordTarget: Int
        get() = ((screenplayWordLow + screenplayWordHigh) / 2.0).roundToInt()

    val summary: String
        get() = "$targetDurationMin min · ~$dialogueWordBudget dialogue words · " +
            "$beatLow–$beatHigh beats · $sceneLow–$sceneHigh scenes · " +
            "$shotLow–$shotHigh shots · $renderTier"
}

object RuntimeContractCalculator {
    const val MIN_EPISODE_MINUTES = 1
    const val MAX_EPISODE_MINUTES = 30

    private const val DIALOGUE_WORDS_PER_SECOND = 2.2
    private const val TARGET_SECONDS_PER_SHOT = 7.0
    private const val SHOTS_PER_SCENE = 3.0

    fun build(episodeDurationMin: Int?): RuntimeContract {
        val minutes = (episodeDurationMin ?: 24).coerceIn(MIN_EPISODE_MINUTES, MAX_EPISODE_MINUTES)
        val targetSec = minutes * 60
        val dialogueWords = (targetSec * DIALOGUE_WORDS_PER_SECOND).roundToInt()
        val screenplayLow = (dialogueWords / 0.60).roundToInt()
        val screenplayHigh = (dialogueWords / 0.50).roundToInt()
        val beatCenter = (4 + minutes * 2).toDouble()
        val shotCenter = targetSec / TARGET_SECONDS_PER_SHOT
        val sceneCenter = shotCenter / SHOTS_PER_SCENE
        return RuntimeContract(
            targetDurationMin = minutes,
            targetDurationSec = targetSec,
            dialogueWordBudget = dialogueWords,
            screenplayWordLow = screenplayLow,
            screenplayWordHigh = screenplayHigh,
            actCount = actCount(minutes),
            beatLow = rangeLow(beatCenter, 5, 70),
            beatHigh = rangeHigh(beatCenter, 5, 70),
            sceneLow = rangeLow(sceneCenter, 3, 90),
            sceneHigh = rangeHigh(sceneCenter, 3, 90),
            shotLow = rangeLow(shotCenter, 8, 270),
            shotHigh = rangeHigh(shotCenter, 8, 270),
            renderTier = renderTier(minutes)
        )
    }

    private fun actCount(minutes: Int): Int = when {
        minutes <= 3 -> 1
        minutes <= 8 -> 3
        minutes <= 15 -> 4
        else -> 6
    }

    private fun renderTier(minutes: Int): String = when {
        minutes <= 3 -> "FULL_MOTION"
        minutes <= 10 -> "SELECTIVE_MOTION"
        else -> "HYBRID"
    }

    private fun rangeLow(center: Double, minValue: Int, maxValue: Int): Int =
        (center * 0.85).roundToInt().coerceIn(minValue, maxValue)

    private fun rangeHigh(center: Double, minValue: Int, maxValue: Int): Int =
        (center * 1.15).roundToInt().coerceIn(minValue, maxValue)
}
