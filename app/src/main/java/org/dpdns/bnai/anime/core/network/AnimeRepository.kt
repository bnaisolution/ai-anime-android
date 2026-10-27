package org.dpdns.bnai.anime.core.network

import org.dpdns.bnai.anime.core.model.Project
import java.util.UUID

class AnimeRepository(
    private val apiService: AnimeApiService = RetrofitClient.create()
) {

    suspend fun fetchProjects(): Resource<List<Project>> {
        return try {
            val response = apiService.getProjects()
            if (response.isSuccessful) {
                val dtoList = response.body()?.data ?: emptyList()
                val mapped = dtoList.map { dto ->
                    Project(
                        id = dto.id,
                        name = dto.name,
                        activeEpisode = "Season ${dto.current_season ?: 1} • Episode ${dto.current_episode ?: 1}",
                        status = dto.status ?: "RUNNING"
                    )
                }
                Resource.Success(mapped)
            } else {
                Resource.Error("HTTP ${response.code()}: Unable to fetch projects from backend.")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Backend connection error")
        }
    }

    suspend fun submitStudioCommand(
        projectId: String,
        intent: CommandIntent,
        mode: String = "SAVE",
        episodeNo: Int? = null,
        payload: Map<String, Any?> = emptyMap(),
        customIdempotencyKey: String? = null
    ): Resource<SubmitCommandResultDto> {
        val idempotencyKey = customIdempotencyKey ?: "cmd-${intent.name.lowercase()}-${UUID.randomUUID()}"
        return try {
            val response = apiService.submitCommand(
                idempotencyKey = idempotencyKey,
                body = CreateCommandRequest(
                    project_id = projectId,
                    intent = intent.name,
                    mode = mode,
                    episode_no = episodeNo,
                    payload = payload
                )
            )
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data)
            } else {
                Resource.Error("Command rejected (HTTP ${response.code()})")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to dispatch command to Fastify outbox")
        }
    }

    suspend fun triggerQuickMedia(
        projectId: String,
        isVideo: Boolean,
        prompt: String
    ): Resource<SubmitCommandResultDto> {
        val key = "quick-${if (isVideo) "vid" else "img"}-${UUID.randomUUID()}"
        return try {
            val response = if (isVideo) {
                apiService.quickGenerateVideo(projectId, key, QuickGenerateRequest(prompt))
            } else {
                apiService.quickGenerateImage(projectId, key, QuickGenerateRequest(prompt))
            }
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data)
            } else {
                Resource.Error("Quick tool failed (HTTP ${response.code()})")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error during quick generation")
        }
    }
}
