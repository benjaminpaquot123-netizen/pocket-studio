# Relecture neuve du tour 1 (aucun Grave ; 6 Moyens, 6 Mineurs)
M1 StemBoardRouter.kt:409 pickPad avec plusieurs bouts → putPiecesOn(lane until 16) écrase Kick, Kick 2, Caisse… ; ne poser que sur le pad touché + pads libres, ou dire tous ceux remplacés.
M2 AppRoot.kt:3418 + RhythmViewModel.kt:6088 « Découper ce que je veux » → useSong(asSource=true) passe par withRemixPlaced() : un projet en boucle devient un morceau d'1 partie de 90 mesures ; si asSource : remix.placed=true sans withRemixPlaced.
M3 PadLiveController.kt:209/229 fillShownPage remplit B–D par editQuietly (history.replace) : un Annuler d'avant efface la page ; l.185 un pack touché pendant le remplissage auto est ignoré ; faire une vraie étape fusionnée, annuler/mettre en file le remplissage auto.
M4 TutoChecks.kt:779 règle stricte sur toutes les voies du rôle (kick {0,1}, hat {5,6}) mais show() ne réécrit que laneOf(arg) : un coup parasite sur Kick 2 empêche « Montre-moi » de valider ; show() vide aussi les autres voies du rôle + test ; commentaire l.767 périmé.
M5 RhythmViewModel.kt:3360 newSongPart(copy=true) prend n'importe quelle scène vide ≠ affichée : n'évite pas les scènes avec prises audio ni celles déjà dans le morceau ; utiliser firstEmptyScene() et exclure p.song.
M6 LoudnessTarget.kt:108 wholeSong() vrai pour STREAMING à toute version de mix : anciens projets réexportés ~1–2 LU plus forts sans le dire ; le dire (changelog) ou lier à une montée de version de mix.
m GuideViewModel.kt:389 « C'est fait » + Annuler même si l'édition n'a rien changé (undoDone annulerait autre chose).
m RhythmViewModel.kt:1425/1436 applySynthFavorite : son appliqué, effets refusés (limite) → à moitié appliqué.
m WavExporter.kt:38 take(40) peut couper une paire de substitution / un signe combinant ; couper sur les graphèmes.
m NameField.kt:44 l'astuce de focus avale le premier changement (IME CJK/coréen) ; take(max) coupe les paires.
m ProjectOpening.kt:39 projet de départ enregistré dès que load() rend null : un dossier avec seulement project.json.tmp serait écrasé.
m RhythmViewModel.kt:2160 « moteur pas prêt : réessaie » trompeur si la bibliothèque native ne se charge jamais.
