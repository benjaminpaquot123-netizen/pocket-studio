# Décisions — lot « exports2 » (tour 6, zone Exports)

- **E1 — chiffre du niveau en Mode simple.** L'audit round 3 n° 14 avait retiré les nombres du Mode
  simple (mots seuls). La phrase d'acceptation E1 de Benjamin demande explicitement « le même mot et le
  même chiffre qu'en Pro » pour le volume du fichier. Choix : la ligne « Niveau » du Mode simple porte
  désormais le chiffre du fichier, en clair et sans l'unité « LUFS » (« Niveau : −8,5, comme la musique
  de club »). Raison : la phrase de Benjamin prime sur la décision d'audit pour ce point précis. Retour
  arrière : remettre `tr("Niveau : %s", Loudness.describe(lufs))` dans `ShareSheet.kt` et annuler la
  mise à jour de `Lot6UiShareLevelTest`.
- **E1 — sélecteur de cible en Mode simple.** Choix : afficher la cible (Streaming/Club/Auto) aussi en
  Mode simple, en mots simples sans LUFS (`SimpleTargetPicker`), au lieu de forcer le passage en Pro.
  Raison : « en deux gestes au plus sans passer en Pro ». Retour arrière : remettre `target = if
  (simple) null …` et retirer `SimpleTargetPicker`.
- **E4 — nom sur une ligne, « … » au milieu.** Choix : `maxLines = 1` + `TextOverflow.MiddleEllipsis`
  plutôt qu'un retour à la ligne entre mots (les noms de fichiers n'ont pas d'espaces). Raison : option
  première de la phrase E4. Retour arrière : revenir à `maxLines = 2` + `Ellipsis`.
- **E6 — « Envoyer le projet (.zip) ».** Choix : renommer la seule ligne de la page Partager qui ouvre
  le menu de partage ; garder « Sauvegarder » pour le vrai enregistrement de Mes projets. Raison : le
  mot doit dire ce que fait le geste. Retour arrière : remettre « Sauvegarder le morceau en .zip ».
- **SDK Android.** Le conteneur n'avait pas de SDK : installé `/opt/android-sdk` (cmdline-tools,
  platforms;android-37.0, build-tools;35.0.0, ndk 30.0.16248370) + `local.properties` (gitignoré, non
  commité) pour lancer les tests unitaires. Sans conséquence sur le dépôt.
