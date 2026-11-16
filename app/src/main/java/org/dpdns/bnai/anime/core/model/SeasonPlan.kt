package org.dpdns.bnai.anime.core.model

import org.dpdns.bnai.anime.core.network.JsonMap

data class SeasonPromptOverrides(
    val season: String? = null,
    val episode: String? = null,
    val image: String? = null,
    val aiDirector: String? = null,
    val genre: String? = null,
    val visualMedium: String? = null
) {
    fun toJson(): JsonMap = buildMap {
        season?.takeIf { it.isNotBlank() }?.let { put("season", it) }
        episode?.takeIf { it.isNotBlank() }?.let { put("episode", it) }
        image?.takeIf { it.isNotBlank() }?.let { put("image", it) }
        aiDirector?.takeIf { it.isNotBlank() }?.let { put("ai_director", it) }
        genre?.takeIf { it.isNotBlank() }?.let { put("genre", it) }
        visualMedium?.takeIf { it.isNotBlank() }?.let { put("visual_medium", it) }
    }

    fun valueFor(category: String): String? = when (category.uppercase()) {
        "SEASON" -> season
        "EPISODE" -> episode
        "IMAGE" -> image
        "AI_DIRECTOR" -> aiDirector
        "GENRE" -> genre
        "VISUAL_MEDIUM" -> visualMedium
        else -> null
    }

    fun withValue(category: String, promptId: String?): SeasonPromptOverrides = when (category.uppercase()) {
        "SEASON" -> copy(season = promptId)
        "EPISODE" -> copy(episode = promptId)
        "IMAGE" -> copy(image = promptId)
        "AI_DIRECTOR" -> copy(aiDirector = promptId)
        "GENRE" -> copy(genre = promptId)
        "VISUAL_MEDIUM" -> copy(visualMedium = promptId)
        else -> this
    }

    companion object {
        fun fromJson(raw: Any?): SeasonPromptOverrides {
            val map = raw as? Map<*, *> ?: return SeasonPromptOverrides()
            fun text(key: String): String? = map[key]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
            return SeasonPromptOverrides(
                season = text("season"),
                episode = text("episode"),
                image = text("image"),
                aiDirector = text("ai_director") ?: text("aiDirector"),
                genre = text("genre"),
                visualMedium = text("visual_medium") ?: text("visualMedium")
            )
        }
    }
}

data class SeasonPlan(
    val seasonNo: Int,
    val title: String = "",
    val arc: String = "",
    val goals: String = "",
    val status: String = "PLANNED",
    val episodeStart: Int = 1,
    val episodeEnd: Int = 12,
    val genres: List<String> = emptyList(),
    val locked: Boolean = false,
    val blueprint: String = "",
    val promptOverrides: SeasonPromptOverrides = SeasonPromptOverrides()
) {
    val displayTitle: String
        get() = title.ifBlank { "Season $seasonNo" }

    fun toJson(): JsonMap = mapOf(
        "season_no" to seasonNo,
        "title" to title,
        "arc" to arc,
        "goals" to goals,
        "status" to status,
        "episode_start" to episodeStart,
        "episode_end" to episodeEnd,
        "genres" to genres,
        "locked" to locked,
        "blueprint" to blueprint,
        "prompt_overrides" to promptOverrides.toJson()
    )

    companion object {
        fun fromJson(raw: Any?): SeasonPlan? {
            val map = raw as? Map<*, *> ?: return null
            val seasonNo = (map["season_no"] as? Number)?.toInt() ?: return null
            val genres = (map["genres"] as? List<*>)
                ?.mapNotNull { it?.toString()?.trim()?.takeIf { value -> value.isNotBlank() } }
                .orEmpty()
            return SeasonPlan(
                seasonNo = seasonNo,
                title = map["title"]?.toString().orEmpty(),
                arc = map["arc"]?.toString().orEmpty(),
                goals = map["goals"]?.toString().orEmpty(),
                status = map["status"]?.toString()?.takeIf { it.isNotBlank() } ?: "PLANNED",
                episodeStart = (map["episode_start"] as? Number)?.toInt() ?: 1,
                episodeEnd = (map["episode_end"] as? Number)?.toInt() ?: 12,
                genres = genres,
                locked = map["locked"] == true || map["locked"]?.toString().equals("true", ignoreCase = true),
                blueprint = map["blueprint"]?.toString().orEmpty(),
                promptOverrides = SeasonPromptOverrides.fromJson(map["prompt_overrides"])
            )
        }
    }
}

fun JsonMap.storyBibleMap(): JsonMap {
    val nested = this["story_bible"] as? Map<*, *> ?: this
    return nested.entries
        .filter { it.key is String }
        .associate { it.key as String to it.value }
}

fun JsonMap.intValue(key: String, fallback: Int): Int =
    (this[key] as? Number)?.toInt() ?: fallback

fun JsonMap.textValue(key: String): String =
    this[key]?.toString()?.takeIf { it.isNotBlank() && it != "null" }.orEmpty()

fun seasonPlansFromBible(
    bible: JsonMap,
    totalSeasons: Int,
    episodesPerSeason: Int
): List<SeasonPlan> {
    val parsed = (bible["seasons"] as? List<*>)
        ?.mapNotNull { SeasonPlan.fromJson(it) }
        ?.associateBy { it.seasonNo }
        .orEmpty()
    val safeTotal = totalSeasons.coerceIn(1, 100)
    val safeEpisodes = episodesPerSeason.coerceIn(1, 500)
    return (1..safeTotal).map { seasonNo ->
        val existing = parsed[seasonNo]
        val defaultStart = (seasonNo - 1) * safeEpisodes + 1
        val defaultEnd = seasonNo * safeEpisodes
        existing?.copy(
            episodeStart = existing.episodeStart.takeIf { it > 0 } ?: defaultStart,
            episodeEnd = existing.episodeEnd.takeIf { it > 0 } ?: defaultEnd
        ) ?: SeasonPlan(
            seasonNo = seasonNo,
            episodeStart = defaultStart,
            episodeEnd = defaultEnd
        )
    }
}
