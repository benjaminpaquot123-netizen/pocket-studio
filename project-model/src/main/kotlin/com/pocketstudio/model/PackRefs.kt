package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Role

/**
 * The format v2 references of a style to the downloaded packs (lot gr-l1b): "pack:sound" (a sound of
 * a pack, by its file or its file without the extension), a layer ("kit:N", "lib:pack:sound" or a
 * pack id) and what each one is on this phone. Pure: the app decodes the files it names.
 */
object PackRefs {
    /** What a layer plays: a built-in kit voice, or a sound of a downloaded pack. */
    sealed interface Source {
        data class Kit(val voice: Int) : Source
        data class Pack(val packId: String, val sound: PackSound) : Source
    }

    /** The sound [ref] ("pack:sound") names among [installed], null when its pack is not downloaded or has no such sound. */
    fun resolve(ref: String, installed: List<PackManifest>): Source.Pack? {
        val packId = ref.substringBefore(':', "").takeIf { it.isNotEmpty() } ?: return null
        val name = ref.substringAfter(':')
        val pack = installed.firstOrNull { it.id == packId } ?: return null
        val sound = pack.sounds.firstOrNull { it.file == name } ?: pack.sounds.firstOrNull { it.file.substringBeforeLast('.') == name }
        return sound?.let { Source.Pack(pack.id, it) }
    }

    /**
     * What layer [spec] of a [role] line plays: "kit:N" the kit voice N, "lib:pack:sound" that sound,
     * a pack id that pack's best sound of the role's kind. Null when it is not on the phone.
     */
    fun layer(spec: String, role: Role, installed: List<PackManifest>): Source? = when {
        spec.startsWith("kit:") -> spec.removePrefix("kit:").toIntOrNull()?.takeIf { it in 0 until DrumPattern.PAD_COUNT }?.let { Source.Kit(it) }
        spec.startsWith("lib:") -> resolve(spec.removePrefix("lib:"), installed)
        else -> installed.firstOrNull { it.id == spec }?.let { m -> m.ofKind(role.packKind).firstOrNull()?.let { Source.Pack(m.id, it) } }
    }

    /** The hit of a transition from the fx packs downloaded ([installed]): an impact, else null. */
    fun impact(installed: List<PackManifest>): Source.Pack? =
        installed.firstNotNullOfOrNull { m -> m.sounds.firstOrNull { it.kind == "fx" && ("impact" in it.file.lowercase() || "impact" in it.tags) }?.let { Source.Pack(m.id, it) } }
}
