package org.dpdns.bnai.anime.ui.testing

object TestTags {
    object State {
        const val LOADING = "state.loading"
        const val EMPTY = "state.empty"
        const val ERROR = "state.error"
        const val RETRY = "state.retry"
    }

    object Dialog {
        const val CONFIRM = "dialog.confirm"
        const val CANCEL = "dialog.cancel"
        const val UNSAVED_DISCARD = "dialog.unsaved.discard"
        const val UNSAVED_KEEP_EDITING = "dialog.unsaved.keep_editing"
    }

    object AiDirector {
        const val SHEET = "ai_director.sheet"
        const val SEND = "ai_director.send"
    }

    object Navigation {
        const val DRAWER = "nav.drawer"
        const val AI_DIRECTOR_FAB = "nav.ai_director_fab"
        const val PROJECT_SELECTOR = "nav.project_selector"
        const val PROJECT_REFRESH = "nav.project_refresh"
        const val BOTTOM_ITEM_PREFIX = "nav.bottom"
        const val DRAWER_ITEM_PREFIX = "nav.drawer_item"
    }

    object Auth {
        const val EMAIL = "screen.auth.email"
        const val PASSWORD = "screen.auth.password"
        const val SUBMIT = "screen.auth.submit"
    }

    object Studio {
        const val CREATE_PROJECT = "screen.studio.create_project"
        const val PROJECT_LIST = "screen.studio.project_list"
        const val PROJECT_ITEM_PREFIX = "screen.studio.project"
        const val PROJECT_NAME_FIELD = "screen.studio.project_name"
        const val CREATE_CONFIRM = "screen.studio.create_confirm"
    }

    object ProjectDetail {
        const val SAVE_SETTINGS = "screen.project_detail.save_settings"
        const val SETTINGS_BUTTON = "screen.project_detail.settings"
    }

    object Production {
        const val PAUSE = "screen.production.pause"
        const val CANCEL = "screen.production.cancel"
        const val SYNC = "screen.production.sync"
    }

    object Review {
        const val APPROVE = "screen.review.approve"
        const val REJECT = "screen.review.reject"
        const val REJECT_REASON = "screen.review.reject_reason"
        const val REJECT_CONFIRM = "screen.review.reject_confirm"
    }

    object PromptStudio {
        const val SAVE = "screen.prompt_studio.save"
        const val RESET = "screen.prompt_studio.reset"
        const val EDIT = "screen.prompt_studio.edit"
        const val TAB_PREFIX = "screen.prompt_studio.tab"
        const val TITLE_FIELD = "screen.prompt_studio.title"
        const val TEXT_FIELD = "screen.prompt_studio.text"
    }

    object Settings {
        const val LOGOUT = "screen.settings.logout"
        const val SAVE = "screen.settings.save"
    }

    object Character {
        const val NAME_FIELD = "screen.characters.name"
        const val ROLE_FIELD = "screen.characters.role"
        const val ADD_CONFIRM = "screen.characters.add_confirm"
    }

    object Bible {
        const val SAVE_DRAFT = "screen.bible.save_draft"
    }

    object EpisodePlanner {
        const val SAVE_METADATA = "screen.episodes.save_metadata"
        const val GENERATE = "screen.episodes.generate"
        const val REGENERATE = "screen.episodes.regenerate"
    }

    object Owner {
        const val EMERGENCY_STOP = "screen.owner.emergency_stop"
    }

    object Screen {
        const val ADMIN_QUEUES = "screen.admin_queues"
        const val AI_BRAIN = "screen.ai_brain"
        const val ANALYTICS = "screen.analytics"
        const val AUDIT_LOG = "screen.audit_log"
        const val AUTH = "screen.auth"
        const val BIBLE = "screen.bible"
        const val BUDGET_LEDGER = "screen.budget_ledger"
        const val CANON = "screen.canon"
        const val CHARACTER_DEEP_DIVE = "screen.character_deep_dive"
        const val CHARACTER_DETAIL = "screen.character_detail"
        const val CHARACTERS = "screen.characters"
        const val CONTINUITY = "screen.continuity"
        const val DATABASE = "screen.database"
        const val EPISODES = "screen.episodes"
        const val JOBS_MONITOR = "screen.jobs_monitor"
        const val LORE_EXPLORER = "screen.lore_explorer"
        const val MEDIA_LIBRARY = "screen.media_library"
        const val NOTIFICATIONS = "screen.notifications"
        const val OWNER_OVERVIEW = "screen.owner_overview"
        const val PRODUCTION = "screen.production"
        const val PROJECT_DETAIL = "screen.project_detail"
        const val PROMPT_STUDIO = "screen.prompt_studio"
        const val PUBLISH = "screen.publish"
        const val QC = "screen.qc"
        const val QUICK_TOOLS = "screen.quick_tools"
        const val QUOTA_DASHBOARD = "screen.quota_dashboard"
        const val REVIEW_INBOX = "screen.review_inbox"
        const val SCENES = "screen.scenes"
        const val SETTINGS = "screen.settings"
        const val STUDIO = "screen.studio"
        const val VISION_ANALYSIS = "screen.vision_analysis"
        const val WORKERS = "screen.workers"
        const val WORKFLOWS = "screen.workflows"
    }

    fun item(prefix: String, id: String): String = "$prefix.$id"
}
