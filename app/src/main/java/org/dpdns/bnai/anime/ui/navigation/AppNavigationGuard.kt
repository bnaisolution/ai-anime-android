package org.dpdns.bnai.anime.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

/**
 * Shared exit guard for screens with unsaved edits. The active screen supplies an
 * interceptor that either proceeds immediately or holds the pending exit until the
 * user chooses Save, Discard, or Cancel.
 */
object AppNavigationGuard {
    private var interceptor: ((proceed: () -> Unit) -> Unit)? = null

    fun register(handler: (proceed: () -> Unit) -> Unit) {
        interceptor = handler
    }

    fun unregister(handler: (proceed: () -> Unit) -> Unit) {
        if (interceptor === handler) interceptor = null
    }

    fun runOrProceed(proceed: () -> Unit) {
        val active = interceptor
        if (active == null) proceed() else active(proceed)
    }
}

@Composable
fun RegisterNavigationExitGuard(handler: (proceed: () -> Unit) -> Unit) {
    val latestHandler = rememberUpdatedState(handler)
    DisposableEffect(Unit) {
        val registered: (() -> Unit) -> Unit = { proceed -> latestHandler.value(proceed) }
        AppNavigationGuard.register(registered)
        onDispose { AppNavigationGuard.unregister(registered) }
    }
}
