package org.dpdns.bnai.anime.core.model

data class Episode(
    val id: String,
    val seasonNumber: Int = 1,
    val episodeNumber: Int,
    val title: String,
    val status: String,
    val synopsis: String? = null,
    val updatedAt: String? = null,
    val qcStatus: String? = null,
    val scriptText: String? = null,
    val masterVideoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val projectName: String? = null
)
