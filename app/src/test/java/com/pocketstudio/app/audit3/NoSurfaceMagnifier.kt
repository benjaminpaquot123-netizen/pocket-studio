package com.pocketstudio.app.audit3

import android.widget.Magnifier
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Robolectric draws no window surface, so the text magnifier's popup has none and [Magnifier.dismiss]
 * fails on it (tour 7 integration, CI 43766927698: a random long press on a text field, then
 * « Cannot invoke Surface.destroy() because mSurface is null »). The magnifier is the platform's, not the
 * app's: the random finger skips it.
 */
@Implements(Magnifier::class)
class NoSurfaceMagnifier {
    @Implementation fun show(sourceCenterX: Float, sourceCenterY: Float) {}
    @Implementation fun show(sourceCenterX: Float, sourceCenterY: Float, magnifierCenterX: Float, magnifierCenterY: Float) {}
    @Implementation fun dismiss() {}
    @Implementation fun update() {}
}
