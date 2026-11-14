package com.pocketstudio.app.journey

import androidx.compose.ui.geometry.Rect

/**
 * What [Journey] does on the screen, six moves and nothing else, by the screens' test tags in
 * every window of the app (the activity, its dialogs and sheets). Two drivers: the compose test
 * rule's (sharedTest, the tests' paths) and the in-app one of « BenStudio Test » (src/benTest:
 * it reads Compose's semantics tree and sends real touches).
 */
interface JourneyDriver {
    /** How many nodes are tagged [tag]: every node ([merged] false) or as a touch sees them (merged into their clickable parent). */
    fun count(tag: String, merged: Boolean): Int

    /** Where the one node tagged [tag] is, in its window (throws when there is not exactly one). */
    fun bounds(tag: String): Rect

    /** Scrolls the lists holding [tag] until it is in view. */
    fun scrollTo(tag: String)

    /** A touch at the centre of the one node tagged [tag]; returns once the app has taken it. */
    fun click(tag: String)

    /** Lets the app work for [ms] (frames, the main thread's queue). */
    fun idle(ms: Long)

    /** The tags on screen that match [pattern] (the keys of a keyboard, the cells of a grid). */
    fun tags(pattern: Regex): List<String>
}
