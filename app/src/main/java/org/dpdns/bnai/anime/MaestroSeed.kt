package org.dpdns.bnai.anime

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.di.AppContainer
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.model.Project
import org.dpdns.bnai.anime.core.network.AnimeRepositoryApi
import org.dpdns.bnai.anime.core.network.ApprovalDecisionDto
import org.dpdns.bnai.anime.core.network.JobDto
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.SystemPromptDto
import org.dpdns.bnai.anime.ui.AppViewModelFactory

/**
 * Debug-only fixture used by the Maestro media-playback flow.
 *
 * This is intentionally guarded by [BuildConfig.DEBUG] in MainActivity; release
 * builds never seed a session and never install the fixture repository.
 */
internal object MaestroSeed {
    private const val PREFS_NAME = "anime_maestro_seed"
    private const val KEY_SEEDED = "seeded"
    private const val TAG = "MaestroSeed"
    private const val DEEP_LINK = "anime://maestro/seed"
    private const val EXTRA_SEED = "maestro_seed"
    private const val PROJECT_ID = "maestro-project"
    private const val MEDIA_URL = "android.resource://org.dpdns.bnai.anime/raw/maestro_tone"

    fun handle(context: Context, intent: Intent?): Boolean {
        val data = intent?.data?.toString()
        val fromDeepLink = data == DEEP_LINK
        val fromViewIntent = intent?.action == Intent.ACTION_VIEW
        val fromLaunchExtra = intent?.getBooleanExtra(EXTRA_SEED, false) == true ||
            intent?.getStringExtra(EXTRA_SEED)?.equals("true", ignoreCase = true) == true
        Log.i(TAG, "debug=${BuildConfig.DEBUG} action=${intent?.action} data=$data " +
            "extra=$fromLaunchExtra view=$fromViewIntent")
        if (!fromDeepLink && !fromLaunchExtra && !fromViewIntent) return false
        val tokenManager = TokenManager.init(context)
        tokenManager.saveSession(
            accessToken = "maestro-fixture-token",
            refreshToken = null,
            expiresInSeconds = 3600,
            userId = "maestro-user",
            workspaceId = "maestro-workspace",
            role = "owner",
            email = "maestro@anime.local"
        )
        tokenManager.saveActiveProjectId(PROJECT_ID)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SEEDED, true)
            .apply()
        return true
    }

    fun isSeeded(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SEEDED, false)

    fun container(base: AppContainer): AppContainer {
        val seeded = object : AppContainer by base {
            override val repository: AnimeRepositoryApi = MaestroFixtureRepository()
        }
        return object : AppContainer by seeded {
            override val viewModelFactory: ViewModelProvider.Factory =
                AppViewModelFactory(seeded)
        }
    }

    private class MaestroFixtureRepository : AnimeRepositoryApi {
        override fun currentProjectId(): String = PROJECT_ID

        override suspend fun checkBackendHealth(): Boolean = true

        override suspend fun resolveConfiguredAiProviderBadge(): String = "Maestro Fixture"

        override suspend fun fetchProjects(): Resource<List<Project>> = Resource.Success(
            listOf(
                Project(
                    id = PROJECT_ID,
                    name = "Maestro Playback Project",
                    activeEpisode = "EP 1",
                    status = "ACTIVE"
                )
            )
        )

        override suspend fun fetchEpisodes(projectId: String): Resource<List<Episode>> =
            Resource.Success(emptyList())

        override suspend fun fetchApprovals(projectId: String): Resource<List<ApprovalDecisionDto>> =
            Resource.Success(emptyList())

        override suspend fun fetchJobs(projectId: String): Resource<List<JobDto>> =
            Resource.Success(emptyList())

        override suspend fun fetchSystemPrompts(
            projectId: String,
            promptCategory: String?,
            query: String?,
            favoritesOnly: Boolean?,
            genre: String?,
            format: String?
        ): Resource<List<SystemPromptDto>> = Resource.Success(emptyList())

        override suspend fun fetchProjectMedia(projectId: String): Resource<JsonMap> =
            Resource.Success(
                mapOf(
                    "clips" to listOf(
                        mapOf(
                            "id" to "maestro-clip-1",
                            "scene_order" to 1,
                            "clip_url" to MEDIA_URL,
                            "status" to "READY"
                        )
                    ),
                    "character_refs" to listOf(
                        mapOf(
                            "id" to "maestro-char-1",
                            "name" to "Maestro",
                            "image_url" to "https://example.com/ref.png"
                        )
                    ),
                    "scenes" to listOf(
                        mapOf(
                            "id" to "maestro-scene-1",
                            "scene_order" to 1,
                            "description" to "Maestro playback fixture scene",
                            "status" to "READY"
                        )
                    )
                )
            )
    }
}
