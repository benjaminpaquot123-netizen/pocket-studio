package com.pocketstudio.bentest

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The floating banner's model (the question on screen, the answer waited for); the view comes with it in the studio's window. */
object Banniere {
    data class Etat(val question: Question)

    private val _etat = MutableStateFlow<Etat?>(null)
    val etat: StateFlow<Etat?> = _etat

    /** Shows [question], waits at most [maxMs] for the answer. */
    fun poser(question: Question, maxMs: Long): Reponse = Reponse(Verdict.SANS_REPONSE)

    /** The answer of the banner's buttons. */
    fun repondre(verdict: Verdict, commentaire: String = "") = Unit
}
