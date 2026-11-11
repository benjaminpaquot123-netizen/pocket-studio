package com.pocketstudio.app.mixer

import androidx.compose.runtime.staticCompositionLocalOf
import com.pocketstudio.model.AutoTarget

/** « Voir le réglage » (a curve's way to the control it moves): that control, null: none. */
val LocalControlSpot = staticCompositionLocalOf<AutoTarget?> { null }
