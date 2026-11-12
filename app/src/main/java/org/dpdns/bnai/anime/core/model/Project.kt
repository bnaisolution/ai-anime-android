package org.dpdns.bnai.anime.core.model

data class Project(
    val id: String,
    val name: String,
    val activeEpisode: String,
    val status: String
)

/**
 * Builds the "Season X • Episode Y" line from database values only.
 * A project whose current_episode is 0/null has not started production yet (B-02).
 */
fun projectProgressLabel(season: Int?, episode: Int?): String {
    val safeSeason = season ?: 0
    val safeEpisode = episode ?: 0
    if (safeEpisode <= 0) return "Not started"
    val parts = mutableListOf<String>()
    if (safeSeason > 0) parts += "Season $safeSeason"
    parts += "Episode $safeEpisode"
    return parts.joinToString(" • ")
}
