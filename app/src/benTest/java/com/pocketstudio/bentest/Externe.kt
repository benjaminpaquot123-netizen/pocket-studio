package com.pocketstudio.bentest

/** The robot never touches a control that opens another app (docs/PLAN-TEST-APK.md): out there it can neither see nor come back. */
object Externe {
    const val RAISON = "passé : ouvre une autre appli"

    /** True when touching [tag] would leave the app; [ecran] lists the tags on screen (read only when the tag depends on its card). */
    fun lance(tag: String, ecran: () -> Collection<String> = { emptyList() }): Boolean = when {
        tag in EXTERNES -> true
        EXTERNES_MOTIF.matches(tag) -> true
        // « Écouter » of the reference card opens the file picker (no song picked yet).
        tag == "guide_option_listen" -> "guide_card_mix.reference" in ecran()
        else -> false
    }

    /** The file manager (Mes fichiers, the share menu, the cover and reference pickers), the device's settings. */
    private val EXTERNES = setOf(
        "export_my_exports", "share_my_exports", "my_export_send", "guide_reference_change",
        "export_cover", "pro_reference", "permission_settings",
    )

    /** Links out: the project page, the store. */
    private val EXTERNES_MOTIF = Regex(".*(github|play_store|store_link).*")
}

/** A touch refused because it would open another app. */
class PasseExterne(val tag: String) : RuntimeException("${Externe.RAISON} : $tag")
