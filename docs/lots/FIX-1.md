# FIX-1 : corrections de la revue (fuite de la prise, stop ciblé, référence, horloge MIDI, TSan)

## Corrigé
1. **Moyen, fuite de « Enregistrer ce que je joue » à la fermeture.** `PlayedTake.release()`, appelé par
   `RhythmViewModel.onCleared()`, termine la prise armée ou en cours sur `ClosingSaves` (hors de la portée
   annulée) : le `ResampleBuffer` natif est libéré. Le handle est un `AtomicLong` retiré une seule fois
   (`collect`, `cancel`, `release`) ; `collect` et `cancel` appellent `finish` en `NonCancellable`.
   Test : `PadLiveTakeTest.closingTheProjectFreesTheTakesBuffer` (armée, puis en cours : `finish` une fois).
   Échouait avant (appel retiré), passe après.
2. **Mineur, `ResampleStop` sans cible.** La commande porte le pointeur de sa prise (`Command::resample`,
   comparé, jamais déréférencé) ; le moteur l'ignore si ce n'est pas `resample_` (nullptr : la prise en
   cours, comme avant). Le destructeur ne touche `command.resample` que pour `ResampleArm`.
   JNI `resampleStop(handle)`, `resampleFinish` envoie aussi le sien ; `ResampleSource.stop(handle)`.
   Tests : hôte `padlive_resample_stop_targets_its_take` (3 échecs avant, passe après) ;
   `PadLiveTakeTest.theStopNamesItsTake`.
3. **Mineur, référence Master pro restée dans le moteur.** `attach()` la retire quand le projet affiché
   n'en a pas (ou fichier absent) ; `close()` (depuis `onCleared`, seulement si le contrôleur a été créé)
   la retire si c'est ce contrôleur qui l'a installée (jeton global `installedBy` : jamais celle du
   projet suivant) et remet l'écoute sur le master si elle était sur la référence.
   Test : `AbSameLevelTest.theReferenceLeavesTheEngineWithItsProject`.
4. **Mineur, deux lecteurs de la file d'horloge MIDI.** Le nouveau job attend (en `NonCancellable`) le
   job précédent, encore en train d'envoyer le Stop ; la chaîne tient même si on coupe pendant l'attente.
   Test : `PadLiveMidiTest.theClockQueueHasOneReaderWhenSwitchedQuickly` (chevauchement détecté avant,
   0 après, 3 passages). Crochet de test `clockDrainForTest`.
5. **Harnais TSan.** `thread_tests.cpp` : `liveThreads()` couvre ResampleArm/Stop ciblé/lecture façon
   `resampleFinish`, stop tardif vers une prise disparue, vidage de l'horloge MIDI, InstallReference
   (avec et sans), `clipLevel`, InstallClip avec kRefit, SetStemClip/CommitStemClips. Après fusion de
   fx-int (cellules remix incluses) : `scripts/host-tests-tsan.sh` → THREADS OK, aucune course.

## Vérifications
- `host-tests.sh` complet : tout passe sauf des bancs de charge CPU (`m17_worst_case_live_cpu_bench`,
  et selon les passages `master_cpu_budget`, `bass_808`, `s4_no_alloc_and_cpu`) : machine chargée
  (load ~3,5 sur 4 cœurs). `m17` échoue pareil avec l'Engine.cpp de fx-int : pas une régression.
- Kotlin : `padlive.*`, `AbSameLevelTest`, `MasterProScreenshotTest`, `LiveVmTest`, `TakeVaultTest` verts.
- JNI compilé : `:engine:buildCMakeDebug[arm64-v8a]`.

## Décisions à recopier (DECISIONS.md)
- ResampleStop cible sa prise par le pointeur du buffer (pas d'id) : la file est FIFO et le buffer n'est
  libéré qu'après le traitement de ses stops, donc pas d'ABA. Retour arrière : stop sans pointeur.
- Libération de la prise à la fermeture sur `ClosingSaves` (exécuteur déjà existant), pas un nouveau fil.
- Référence Master pro : jeton global du contrôleur propriétaire, pour qu'une fermeture tardive n'efface
  pas celle du projet suivant.
