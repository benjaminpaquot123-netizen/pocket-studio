package com.pocketstudio.bentest

/** The robot never touches a control that opens another app (docs/PLAN-TEST-APK.md): out there it can neither see nor come back. */
object Externe {
    const val RAISON = "passé : ouvre une autre appli"

    /** True when touching [tag] would leave the app; [ecran] lists the tags on screen (read only when the tag depends on its card). */
    fun lance(tag: String, ecran: () -> Collection<String> = { emptyList() }): Boolean = false
}

/** A touch refused because it would open another app. */
class PasseExterne(val tag: String) : RuntimeException("${Externe.RAISON} : $tag")
