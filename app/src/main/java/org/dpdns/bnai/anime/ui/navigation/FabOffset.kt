package org.dpdns.bnai.anime.ui.navigation

data class FabOffset(val xDp: Float, val yDp: Float)

fun clampFabOffset(xDp: Float, yDp: Float, maxLeftDp: Float, maxUpDp: Float): FabOffset {
    val safeLeft = maxLeftDp.coerceAtLeast(0f)
    val safeUp = maxUpDp.coerceAtLeast(0f)
    return FabOffset(
        xDp = xDp.coerceIn(-safeLeft, 0f),
        yDp = yDp.coerceIn(-safeUp, 0f)
    )
}

/**
 * The AI Director FAB is available on every screen except the AI Director chat itself
 * (where it would open a duplicate of the current screen). Home must show it too (B-12).
 */
fun isGlobalFabVisible(route: String): Boolean = route != "ai_brain"
