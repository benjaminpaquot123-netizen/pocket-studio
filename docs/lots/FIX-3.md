# Lot FIX-3, échecs de la vérification complète avant le gel de l'audit : note de suivi

Branche fx-fix3 (= fx-int au départ). Six échecs du passage complet (`gel-gradle.log`, 4 workers) et deux bancs CPU hôtes.

## Fait (2026-11-03)
- **1. `BackupRulesTest.everyFileUnderFilesDirIsClassified`** : deux dossiers neufs du Master pro (`references/<id>.wav`, `covers/<id>.jpg`, `mixer/MasterPro.kt`).
  - Décision : ils sont à l'utilisateur, mais une référence fait jusqu'à 90 s de WAV flottant (~35 Mo) et une pochette jusqu'à 2 Mo : quelques-unes dépasseraient le plafond de 25 Mo du nuage (et alors rien n'est sauvegardé, audit 14/19). Classés comme `mysounds` : transfert de téléphone à téléphone seulement (`data_extraction_rules.xml`, `device-transfer`), jamais le nuage. `backup_rules.xml` (nuage, vieil Android) inchangé.
  - Test : la classification prend le premier dossier du chemin (`references/$id.wav` → `references`) ; nouvelle garde « ni références ni pochettes dans le nuage ».
  - Règle partout : tous les `File(…filesDir, …)` relus (y compris ceux sans chaîne littérale : journaux, `live-take-*`, `retours`, `applog*`, `last_crash.txt`) ; les prises Pad Live, coupes, cases Remix et prises jouées des lots B à E vivent dans le dossier du projet (`projects/<id>/samples`) : rien d'autre à classer.
- **2. `BenchShareTest.annulerStopsAndClosesAtOnce`** : vraie régression, du commit `073528b` (lot D). `exportJob?.isActive` est faux dès « Annuler », alors que le rendu annulé tourne encore (`running != null`) : `cancelExport` remettait `running = false` tout de suite, et le « Partager » suivant passait la garde de `shareSong` puis était jeté sans un mot par celle de `startExport` (`running != null`). Aussi : `releaseUnusedFonts` pouvait fermer une banque que le rendu annulé copiait encore.
  - Correction (`RhythmViewModel.exportJobLive`) : `exportJob?.isCompleted == false` aux cinq endroits. Un travail annulé qui se termine compte comme occupé (comme avant `073528b`), un travail fini resté dans le champ (fin sans suspension, le cas du lot D) compte comme libre. `OldScreensLikeLiveTest` (le cas du lot D) reste vert.
  - Test : `BenchShareTest` (5) vert, rouge avant (reproduit seul).
  - Règle partout : `PerfShare.cancel()` met `job = null` aussitôt : un « Partager les pistes » relancé pendant qu'un rendu annulé finit est possible, et le `finally` de l'ancien appelle `ExportRun.end` sur la notification du nouveau. Mineur (fichiers distincts, l'ancien s'arrête avant d'écrire) : noté, pas changé.
- **3. `ZeroProjectsTest.theLastProjectDeletedLeavesNone`** (attendu 1, trouvé 2) : pas de recul de l'appli. Un projet vide jamais touché passe par la corbeille (`catalog.delete`, un renommage) puis en est effacé (`deleteForever`) ; le test attendait seulement « la liste est vide », vraie entre les deux, et comptait la corbeille à ce moment-là sous charge. Prouvé : une pause de 500 ms entre les deux pas fait échouer l'ancien test avec exactement « expected 1 but was 2 », et passer le nouveau.
  - Correction du test (rien d'affaibli, même assertion) : attendre aussi la fin de l'opération (`!vm2.state.value.busy`).
- **4. Captures `Nav{Tablet,TabletLandscape,TabA11Landscape}ScreenshotTest.mix`** : seule différence, la ligne « Avancé : master pro » du Master pro dans la Sortie générale (et le léger décalage qu'elle entraîne). Réenregistrées ces trois-là seulement, regardées.
- **5. Bancs CPU hôtes** : voir ci-dessous.

## Bancs CPU (ce conteneur, 4 cœurs, machine calme après les tests Gradle)
- `master_cpu_budget` (borne 2 % d'un cœur) : 2,09 % (raté, compté « machine chargée »), 1,78 %, 1,80 %. Passe seul sur machine calme ; marge ~10 %.
- `m17_worst_case_live_cpu_bench` (bornes moyenne < 25 %, p99 < 33 %) : moyenne 23,2 / 23,1 / 22,3 %, p99 36,2 / 44,6 / 37,5 % (les deux premiers comptés « machine chargée », le troisième en échec). Pendant les tests Gradle : p99 35 à 37 %. Répartition (moyenne) : sans les effets des tranches 7,4 à 9,3 %, sans ceux du master 20 à 23 %, sans les effets du Live 22 à 26 %, rien 7 à 8 %.
- Profil (callgrind, le banc m17 entier, instructions propres) : `Saturator::process` 22,9 % et le `log` double de son antidérivée (ADAA, `DriveShaper`) 8,8 %, `Ott` 7,8 %, `Mixer::processChunk` 5,0 %, `ClipLauncher` 7,3 %, `Reverb` 3,4 %, chaîne master (clip + process + limit) 5,4 %. Les points chauds sont du code ancien (`DriveShaper.h` inchangé depuis la phase 33, `MasterChain` : seulement `boxLen_` au lot E) : ce qui pèse, ce sont les quatre saturations des tranches (×4 suréchantillonné, `log` en double à chaque point).
- Gain sûr et bon marché : aucun trouvé sans changer le son (un `log` en simple précision casse l'ADAA, qui divise des différences de F). Bornes non touchées.
- Décision à recopier : le p99 de m17 dépasse sa borne sur ce conteneur ; à mesurer sur le téléphone avant de conclure. Piste si besoin : une table ou une approximation de `ln` en double sur [1, 4] (x² + 3) / 3 ∈ [1, 4]) dans `SoftClipShape::F`, vérifiée par `drive_effects_aliasing` (changement de son à quelques 1e-7 près).

## Restes
- `PerfShare.cancel()` (voir 2) : `job = null` tout de suite ; à aligner sur `isCompleted` si un « Partager les pistes » relancé pendant une annulation pose problème.
- Bancs CPU m17 et master : marge mince ou dépassée sur ce conteneur (ci-dessus).
