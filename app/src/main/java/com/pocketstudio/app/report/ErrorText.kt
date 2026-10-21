package com.pocketstudio.app.report

import com.pocketstudio.model.i18n.tr
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * An error in words a beginner understands, in the interface's language: the network's and the
 * phone's usual failures get a sentence, anything else keeps its own message (or "erreur").
 */
fun Throwable.forUser(): String = when (this) {
    is UnknownHostException, is ConnectException -> tr("pas de connexion internet")
    is SocketTimeoutException -> tr("la connexion est trop lente ou a coupé")
    is SSLException -> tr("la connexion sécurisée a échoué")
    is FileNotFoundException -> tr("fichier introuvable")
    is IOException -> if (message?.contains("ENOSPC") == true || message?.contains("No space") == true) tr("le téléphone est plein") else message ?: tr("erreur de lecture ou d'écriture")
    else -> message ?: tr("erreur")
}
