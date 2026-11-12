# Tour 5, lot « hardtechno » : rapport de l'auteur (2026-11-12)

Atelier `eq-tour-5-hardtechno`, branche `eq/tour-5-hardtechno`. Rien de poussé. À relire par un relecteur neuf.

Point de départ : « Tout tester » sur le téléphone de Benjamin (HONOR BKQ-N49, Android 17) : Hard techno 25 décrochages, charge moyenne 44 %, « pire bloc » 67 % ; Techno 1 décrochage ; Jump up et Liquid DnB, plus lourds (60 %, pire 74 %), aucun.

## Ce que montrent les mesures du téléphone (carnet.tsv)
- Les 25 décrochages de Hard techno arrivent en **deux paquets** (+13 puis +12, à 3 s d'écart), environ 9 s et 12 s après le début du Drop ; entre les deux, rien. Ce sont deux arrêts d'au moins une vingtaine de millisecondes, pas une charge trop haute.
- Juste avant le premier paquet, la « charge » chute de 0,49 à 0,22 en 1,3 s puis remonte ; le relevé de l'interface suivant arrive avec 270 ms de retard : toute l'application a calé au même moment, pas seulement le son. Même dessin pour l'unique décrochage de Techno (chute de 0,45 à 0,19, décrochage à la remontée). Aucun style sans décrochage n'a de telle chute pendant le Drop, sauf Detroit et Tech house (charge plus basse, pas de décrochage).
- Le « pire bloc » du rapport est le maximum de la charge **lissée sur 0,5 s et plafonnée à 4** (`Engine::process`) : un rappel 13 fois trop long la bouge de quelques pour cent. Il ne pouvait donc rien dire de ces arrêts.

## Ce que montre le banc (le morceau rejoué bloc par bloc)
- `LiveSongRecorder` complété (`-Pbench.listen=drop` : le chemin « Écouter » du téléphone, 8 mesures avant le Drop ; `-Pbench.packs` : le pack Hard techno installé) ; `ps_live` rejoue chaque rappel de 96 trames comme le flux du téléphone.
- Hard techno, meilleur de 3 passes par bloc, 60 s : moyenne 10,3 % du budget, p99,9 19 %, **pire bloc 21 %** (2,0 fois la médiane). Techno 19 %, Techno industrielle 24 %, Gabber 21 %, Jump up 33 %. En temps mural aussi : 22 %. Aucun bloc isolé lent, aucune allocation (ALLOC 0), aucune conversion hors limites (UBSan float-cast-overflow), aucun dénormal possible côté ARM (FZ dans le rappel).
- Le seul passage plus chargé de Hard techno (+40 % pendant 0,8 s, plus de voix du Supersaw et du Hoover sur la mesure d'après le Drop) tombe sur le second paquet mais reste à ~14 % du budget ici ; il ne suffit pas, seul, à expliquer 12 décrochages d'affilée.
- Règles du rappel relues : ni verrou, ni journal, ni JNI, ni allocation dans `AudioHost::onAudioReady` → `Engine::process`.

## Cause
Pas dans le traitement de Hard techno : le moteur rend ce morceau aussi régulièrement que les autres. Les décrochages viennent d'arrêts courts **hors du calcul du son**, vus en même temps sur le fil de l'interface (système ou application qui cale, ou changement de fréquence du processeur à la reprise après la chute de charge). La mesure du téléphone ne pouvait pas le dire : son « pire bloc » n'en est pas un. Ce n'est pas prouvé sur le téléphone ; c'est ce que la correction ci-dessous permet de trancher au prochain passage.

## Fait (rouge puis vert, même sujet)
- `CallbackLoad` (moteur, en-tête seul) : la charge lissée (même calcul qu'avant, son et charge inchangés), plus deux relevés que le relevé de santé « prend » toutes les 250 ms : **le vrai pire bloc** (ni lissé ni plafonné) et **le plus long écart entre deux débuts de rappel** (en blocs ; 1 = à l'heure ; > 1 s = flux rouvert, ignoré). Dans le rappel : deux comparaisons et des atomiques relâchés, aucune allocation, aucun verrou.
- JNI `healthMeters` : 6 valeurs (`NativeEngine.HEALTH` = 6). `HealthSample.cpuWorst` / `gapWorst`, écrits dans journal.json (`cpuWorst`, `gapWorst`), et la ligne du journal à chaque décrochage : « Décrochage audio : +13 (total 26) ; pire bloc 60 %, plus long écart 16,0 blocs ». Pire bloc > 100 % : le rappel a calé ; écart grand avec pire bloc < 100 % : le rappel a démarré en retard.
- Tests hôte : `ht_worst_block_sees_one_slow_callback` (un rappel 13× parmi des 0,45× : avant 0,45, après 13,00), `ht_worst_gap_sees_a_late_callback` (rappel en retard de 30 ms : écart 16,0, pire bloc 0,40), `ht_engine_reports_its_worst_block` (le moteur réel, mesuré aussi du dehors). Rouge 24f4a701 (les relevés rendaient la charge lissée) / vert : commit suivant.
- Test Kotlin : `JournalLogicTest#anUnderrunSaysWhetherACallbackStalledOrStartedLate`.

## Son
Aucun changement de traitement. Hard techno rendu par `ps_live` (60 s, chemin « Écouter », pack installé) avant et après : fichiers WAV identiques à l'octet (même md5), donc même niveau et même spectre.

## Vérifié
- `./scripts/host-tests.sh` : vert sauf `s4_no_alloc_and_cpu` une fois sur la machine partagée (synthé seul, code non touché, 16,1 % pour 15 %), rejoué seul vert deux fois (12,2 %, 12,8 %). Aucune limite de temps touchée.
- `:app:testDebugUnitTest --tests 'com.pocketstudio.app.report.*'` : 54 tests verts ; `:engine:assembleDebug` (code natif Android) compile.

## Restes
- La vraie cause des deux arrêts reste à lire sur le téléphone : rejouer « Tout tester » (Hard techno, Techno) et lire dans journal.json les lignes « Décrochage audio … pire bloc …, plus long écart … ». BenStudio Test (hors de ce dépôt) peut afficher `cpuWorst` au lieu du maximum de `cpu` comme « pire bloc ».
- Si l'écart domine : piste du réglage `setPerformanceHintEnabled` (ADPF) et de ce que fait l'application à ce moment (le fil de l'interface cale aussi). Si le pire bloc dépasse 100 % : piste d'une faute de page dans le rappel (mémoire rendue par Android), à confirmer avant tout correctif.

## Essais téléphone à rejouer
« Tout tester » : Style Hard techno, Style Techno ; puis lire journal.json (lignes « Décrochage audio »).

chemin: Signaler un bug › journal.json (relevés de santé, lignes « Décrochage audio ») | test: JournalLogicTest#anUnderrunSaysWhetherACallbackStalledOrStartedLate

tests: com.pocketstudio.app.report.JournalLogicTest com.pocketstudio.app.report.BugReportTextTest com.pocketstudio.app.report.BugReportFlowTest
