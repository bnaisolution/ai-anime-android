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

fun isGlobalFabVisible(route: String): Boolean = route !in setOf("ai_brain", "studio")
