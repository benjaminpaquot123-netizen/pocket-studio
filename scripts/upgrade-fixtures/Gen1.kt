import com.pocketstudio.model.*
// A small beat as the version of that time saved it: a name, a tempo, a swing, kick and snare, two mixer moves.
fun beat(): Project {
    val p = Project()
    val lanes = p.drums.lanes.toMutableList()
    lanes[0] = lanes[0].copy(hits = listOf(0, 4, 8, 12).associateWith { Hit(1f) })
    lanes[1] = lanes[1].copy(hits = listOf(4, 12).associateWith { Hit(0.8f) })
    val ch = p.mixer.channels.toMutableList()
    ch[0] = ch[0].copy(volume = 0.65f)
    ch[1] = ch[1].copy(mute = true)
    return p.copy(name = "Mon beat", tempoBpm = 128.0, swing = 0.2, drums = p.drums.copy(lanes = lanes), mixer = p.mixer.copy(channels = ch))
}
fun main(args: Array<String>) {
    val p = if (args.firstOrNull() == "notes") beat().copy(name = "Ma mélodie", synthNotes = NoteClip().copy(notes = listOf(Note(0, 4, 60), Note(4, 4, 63), Note(8, 4, 67), Note(12, 4, 70)))) else beat()
    println(ProjectJson.encode(p))
}
