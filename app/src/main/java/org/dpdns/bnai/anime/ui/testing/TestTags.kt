package org.dpdns.bnai.anime.ui.testing

object TestTags {
    object State {
        const val LOADING = "state.loading"
        const val EMPTY = "state.empty"
        const val ERROR = "state.error"
        const val RETRY = "state.retry"
    }

    object Player {
        const val VIEW = "player.view"
        const val CLOSE = "player.close"
        const val PLAY = "player.play"
        const val STATE = "player.state"
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
        const val PROMPT_OPTION_PREFIX = "screen.studio.prompt_option"
        const val PROJECT_DELETE_PREFIX = "screen.studio.project_delete"
        const val PROJECT_DELETE_CONFIRM = "screen.studio.project_delete_confirm"
        const val PROJECT_MENU_PREFIX = "screen.studio.project_menu"
        const val BACKEND_STATUS = "screen.studio.backend_status"
        const val VIEW_ALL_EPISODES = "screen.studio.view_all_episodes"
    }

    object ProjectDetail {
        const val SAVE_SETTINGS = "screen.project_detail.save_settings"
        const val SETTINGS_BUTTON = "screen.project_detail.settings"
        const val AUTO_APPROVE = "screen.project_detail.auto_approve"
        const val TOTAL_SEASONS = "screen.project_detail.total_seasons"
        const val EPISODES_PER_SEASON = "screen.project_detail.episodes_per_season"
        const val EPISODE_DURATION = "screen.project_detail.episode_duration"
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
        const val MEDIUM = "screen.prompt_studio.medium"
        const val GENRE = "screen.prompt_studio.genre"
        const val FORMAT = "screen.prompt_studio.format"
        const val TEMPLATE = "screen.prompt_studio.template"
        const val APPLY_TEMPLATE = "screen.prompt_studio.apply_template"
        const val SEARCH = "screen.prompt_studio.search"
        const val BROWSE_ALL = "screen.prompt_studio.browse_all"
        const val BROWSE_SEARCH = "screen.prompt_studio.browse_search"
        const val BROWSE_APPLY = "screen.prompt_studio.browse_apply"
        const val BROWSE_CLEAR = "screen.prompt_studio.browse_clear"
        const val SEASON_ASSIGN = "screen.prompt_studio.season_assign"
        const val FAVORITES = "screen.prompt_studio.favorites"
        const val FILTER_GENRE_FORMAT = "screen.prompt_studio.filter_genre_format"
        const val CARD_PREFIX = "screen.prompt_studio.card"
        const val FAVORITE_PREFIX = "screen.prompt_studio.favorite"
        const val ADD = "screen.prompt_studio.add"
        const val ADD_NAME = "screen.prompt_studio.add_name"
        const val ADD_TEXT = "screen.prompt_studio.add_text"
        const val ADD_BASE = "screen.prompt_studio.add_base"
        const val ADD_GENRE = "screen.prompt_studio.add_genre"
        const val ADD_FORMAT = "screen.prompt_studio.add_format"
        const val ADD_MEDIUM = "screen.prompt_studio.add_medium"
        const val ADD_SAVE = "screen.prompt_studio.add_save"
        const val ACTIVATE = "screen.prompt_studio.activate"
        const val VERSIONS = "screen.prompt_studio.versions"
    }

    object Settings {
        const val LOGOUT = "screen.settings.logout"
        const val SAVE = "screen.settings.save"
        const val DB_TAB = "screen.settings.db_tab"
        const val DB_TEST = "screen.settings.db_test"
        const val DB_DSN = "screen.settings.db_dsn"
        const val DB_TEST_DSN = "screen.settings.db_test_dsn"
        const val DB_CONNECT = "screen.settings.db_connect"
        const val DB_DISCONNECT = "screen.settings.db_disconnect"
        const val DB_HEALTH = "screen.settings.db_health"
        const val DB_MIGRATE = "screen.settings.db_migrate"
        const val OWNER_CONSOLE = "screen.settings.owner_console"
        const val AI_ADD = "screen.settings.ai_add"
        const val AI_ADD_KEY = "screen.settings.ai_add_key"
        const val AI_PRIMARY = "screen.settings.ai_primary"
        const val AI_FALLBACK = "screen.settings.ai_fallback"
    }

    object Character {
        const val NAME_FIELD = "screen.characters.name"
        const val ROLE_FIELD = "screen.characters.role"
        const val ADD_CONFIRM = "screen.characters.add_confirm"
    }

    object Bible {
        const val SAVE_DRAFT = "screen.bible.save_draft"
        const val PROMPT_LIBRARY = "screen.bible.prompt_library"
        const val PROMPT_OPTION_PREFIX = "screen.bible.prompt_option"
        const val PATCH_APPROVE_PREFIX = "screen.bible.patch_approve"
        const val PREMISE_FIELD = "screen.bible.premise"
        const val TONE_GENRE_FIELD = "screen.bible.tone_genre"
        const val WORLD_SETTING_FIELD = "screen.bible.world_setting"
        const val WORLD_RULES_FIELD = "screen.bible.world_rules"
        const val POWER_SYSTEM_FIELD = "screen.bible.power_system"
        const val CHARACTER_FOUNDATIONS_FIELD = "screen.bible.character_foundations"
        const val TIMELINE_FIELD = "screen.bible.timeline"
        const val STRUCTURE_INSTRUCTION = "screen.bible.structure_instruction"
    }

    object EpisodePlanner {
        const val SAVE_METADATA = "screen.episodes.save_metadata"
        const val GENERATE = "screen.episodes.generate"
        const val REGENERATE = "screen.episodes.regenerate"
    }

    object QuickTools {
        const val PROMPT = "screen.quick_tools.prompt"
        const val GENERATE = "screen.quick_tools.generate"
    }

    object Owner {
        const val EMERGENCY_STOP = "screen.owner.emergency_stop"
        const val PLATFORM_MANAGED = "screen.owner.platform_managed"
        const val REPLICATION_STATUS = "screen.owner.replication_status"
        const val TENANTS = "screen.owner.tenants"
        const val TENANT_DATABASES = "screen.owner.tenant_databases"
        const val DB_FILTER_ALL = "screen.owner.tenant_databases.filter_all"
        const val DB_FILTER_ACTIVE = "screen.owner.tenant_databases.filter_active"
        const val DB_FILTER_ERROR = "screen.owner.tenant_databases.filter_error"
        const val DB_RECHECK = "screen.owner.tenant_databases.recheck"
        const val DB_PROVISION = "screen.owner.tenant_databases.provision"
        const val DB_AUDIT = "screen.owner.tenant_databases.audit"
        const val FEATURE_FLAGS = "screen.owner.feature_flags"
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
        const val OWNER_ACCESS_DENIED = "screen.owner_access_denied"
        const val OWNER_OVERVIEW = "screen.owner_overview"
        const val PREFLIGHT = "screen.preflight"
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
