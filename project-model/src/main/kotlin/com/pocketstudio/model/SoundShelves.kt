package com.pocketstudio.model

/**
 * The sound browser's folders (Benjamin's list n° 13–14): sounds filed by meaning, not by the pack
 * they come from. First level: the families (Machines, Techno, House…, Fête, Bruitages); second
 * level, only when it helps: the machine inside « Machines », the measured character elsewhere
 * (« Gros et grave », « Sec et court »…). Two levels at most; a folder with fewer than
 * [MIN_FOLDER] sounds joins its parent; inside a folder the packs take turns, each one's best
 * sound first ([PackPicks]), so the first sounds shown are the best of every pack.
 */
object SoundShelves {
    /** Below this, a folder is not worth a touch: its sounds join the folder above. */
    const val MIN_FOLDER = 8

    /** Where a family too small to stand alone goes. */
    const val OTHERS = "Autres"

    /** The style families in the order of the style chooser (scripts/packs/styles.json), then the libraries'. */
    val FAMILIES = listOf("Machines", "Techno", "House", "Hip-hop", "Bass music", "Sons actuels", "Autres", "Acoustique", "Bruitages")

    /** One sound to file: its [family], its second-level group ([sub], null: none) and its [pack] (the turns are taken by pack). */
    data class Item<T>(val value: T, val family: String, val sub: String? = null, val pack: String? = null)

    /** A folder: its sub-folders first, then its own sounds. [id] is its path (`family` or `family/sub`). */
    data class Shelf<T>(val id: String, val title: String, val children: List<Shelf<T>>, val items: List<T>) {
        /** Every sound inside, sub-folders included. */
        val count: Int get() = items.size + children.sumOf { it.count }
        val all: List<T> get() = children.flatMap { it.items } + items

        /** The folder with [id] here or below, if any. */
        fun find(id: String): Shelf<T>? = if (this.id == id) this else children.firstNotNullOfOrNull { it.find(id) }
    }

    /**
     * Files [items] (in their order: best first within each pack) into folders. [pinned] families
     * keep a folder of their own however small (« Sons intégrés », « Fête », the style's own),
     * and come first in their order; the others follow [FAMILIES], unknown families after.
     */
    fun <T> build(items: List<Item<T>>, pinned: List<String> = emptyList()): List<Shelf<T>> {
        val byFamily = LinkedHashMap<String, MutableList<Item<T>>>()
        items.forEach { byFamily.getOrPut(it.family) { mutableListOf() }.add(it) }
        // A small family joins « Autres » (the root holds folders only).
        val small = byFamily.filter { (f, list) -> f !in pinned && f != OTHERS && list.size < MIN_FOLDER }.keys
        // (A lone small family stays itself: « Autres » would say less.)
        if ((byFamily.keys - small).isNotEmpty() || small.size > 1) {
            small.forEach { f -> byFamily.getOrPut(OTHERS) { mutableListOf() }.addAll(byFamily.remove(f)!!.map { it.copy(family = OTHERS) }) }
        }
        val order = pinned + FAMILIES
        val families = byFamily.keys.sortedBy { f -> order.indexOf(f).let { if (it < 0) order.size else it } }
        return families.map { f -> shelf(f, byFamily.getValue(f)) }
    }

    private fun <T> shelf(family: String, items: List<Item<T>>): Shelf<T> {
        val bySub = LinkedHashMap<String, MutableList<Item<T>>>()
        val loose = mutableListOf<Item<T>>()
        items.forEach { if (it.sub == null) loose.add(it) else bySub.getOrPut(it.sub) { mutableListOf() }.add(it) }
        // Small groups join the family's own sounds.
        bySub.filterValues { it.size < MIN_FOLDER }.keys.forEach { loose.addAll(bySub.remove(it)!!) }
        // One group alone is no choice: its sounds are the family's, in their order (« Fête »: the horns first).
        if (bySub.size == 1) {
            bySub.clear()
            loose.clear()
            loose.addAll(items)
        }
        val children = bySub.map { (sub, list) -> Shelf("$family/$sub", sub, emptyList(), turns(list)) }
        return Shelf(family, family, children, turns(loose))
    }

    /**
     * The packs take turns (best sound of each pack, then the second best…), in the order they
     * first appear; sounds without a pack (the built-in ones) keep their place at the front.
     */
    fun <T> turns(items: List<Item<T>>): List<T> {
        val own = items.filter { it.pack == null }.map { it.value }
        val byPack = LinkedHashMap<String, ArrayDeque<T>>()
        items.filter { it.pack != null }.forEach { byPack.getOrPut(it.pack!!) { ArrayDeque() }.addLast(it.value) }
        val out = ArrayList<T>(items.size)
        out.addAll(own)
        while (byPack.values.any { it.isNotEmpty() }) byPack.values.forEach { q -> q.removeFirstOrNull()?.let(out::add) }
        return out
    }

    /** The folder path that holds [value] (family, then sub-folder), to open the browser on it. */
    fun <T> pathOf(shelves: List<Shelf<T>>, value: T): List<String> {
        shelves.forEach { s ->
            s.children.firstOrNull { value in it.items }?.let { return listOf(s.id, it.id) }
            if (value in s.items) return listOf(s.id)
        }
        return emptyList()
    }

    /** « Cartes à jouer 3 » → « Cartes à jouer »: a library sound's group (its label without its number). */
    fun groupOfName(name: String): String = name.trim().replace(Regex("""\s+\d+$"""), "")
}
