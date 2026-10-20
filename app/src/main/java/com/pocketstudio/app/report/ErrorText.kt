package com.pocketstudio.app.report

import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * An error in words a beginner understands, in French: the network's and the phone's usual
 * failures get a sentence, anything else keeps its own message (or "erreur").
 */
fun Throwable.forUser(): String = when (this) {
    is UnknownHostException, is ConnectException -> "pas de connexion internet"
    is SocketTimeoutException -> "la connexion est trop lente ou a coupé"
    is SSLException -> "la connexion sécurisée a échoué"
    is FileNotFoundException -> "fichier introuvable"
    is IOException -> if (message?.contains("ENOSPC") == true || message?.contains("No space") == true) "le téléphone est plein" else message ?: "erreur de lecture ou d'écriture"
    else -> message ?: "erreur"
}
