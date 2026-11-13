# En cours

Tenu par le chef d'orchestre (méthode v3, `docs/audits/AUDIT-ULTRA.md` §13.7). Une page, mise à jour à chaque événement. C'est aussi la fiche de reprise : qui reprend le rôle de chef lit ce fichier, puis `docs/PROGRESS.md`.

- **Mise à jour** : 2026-11-13, 09 h 40 UTC.
- **Chef d'orchestre** : la discussion principale, `session_01NqonxZBBysqqWBbnsC2VTP` (outil `send_message`).
- **Temps en cours** : CORRECTION du tour 6. Aucune vague de testeurs ne joue.
- **Dernier départ jugé** : `1585bb46` (tour 6, 19 sièges, 12 novembre). Sièges de contrôle du tuto joués sur `9f26c244`.
- **Intégration** : branche `ccr-80af1a1b-mn0wec` ; tout y est poussé après chaque portail.

## En arrivant (toute discussion, avant toute mission)

1. Remplis le cache des tests : `bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*BenchSelfTest'` (si Maven répond 429, réessaie toutes les dix minutes).
2. Écris au chef d'orchestre, en une ligne : « prêt : <un nom court> ; modifier l'appli : oui/non ; pousser une branche neuve : oui/non ; tests chauds : oui/non ».
3. Économie de quota (Benjamin, 13 novembre) : aucun texte pour Benjamin dans ta discussion, pas de résumé ni de récit d'étape ; tes messages au chef tiennent en une ligne ; sorties de commandes filtrées (`tail`, `grep`), jamais un fichier entier quand un extrait suffit ; un sous-agent seulement quand la fiche le demande.
4. Attends ta fiche (ton rôle, tes fichiers, ton commit). Aucune question à Benjamin : une ligne au chef.

## Ce qui n'est pas vert

1. **CI de la tête** (`9f26c244`, passage 43375199284) : « Emulator smoke test » ROUGE depuis 06 h 44 (`JourneyTest.aFirstSongInTheModeSimple` : « nothing tagged wizard after 15000 ms ») ; le reste de ce passage est vert (tests, captures, bancs ; l'endurance finit). **Cause trouvée (07 h 50)** : le lot « tutoconsignes » (`b621715da`) a écrit dans `PlainWords.kt` un motif `(?iU)…` ; le moteur de motifs du téléphone (ICU) refuse le drapeau « U », la JVM des tests et du banc l'accepte. Sur un vrai téléphone, l'assistant et le panneau du tuto en Mode simple plantent : **ne rien installer sur un téléphone depuis cette tête.** Réparation : lot « emulateur », avec un test de garde sur tous les motifs de l'appli.
2. **Le tuto** : les trois contrôles vont au « Bravo », mais 12 défauts corrigés sur 32, trois graves neufs (flèches contradictoires au chapitre 7, doubles-croches refusées au chapitre 4 du tuto Pro, flottement après « Copier sur les mesures suivantes »), trois chapitres bloqués. Lot 10 « tutofleches » en cours.
3. **Le juge du chantier** : 68 écarts (8 après le lot « juge7 »), presque tous des paires rouge/vert dépareillées. À ramener par le lot du banc « portail3 ».
4. **Les rejeux** : un seul (`t4-26-prise-clavier`) ; aucun des bloquants ou graves corrigés dans la nuit n'a le sien. Lot du banc « rejeux7 ».
5. **Le lot 5 « langues2 »** (textes coupés) : fait et relu dans la discussion « Sons modernes », pas poussé : sa branche porte l'essai de sons ; il attend l'accord de Benjamin pour une branche neuve.
6. **Captures de référence** : seize périmées depuis le lot 1 (signalées par le lot 5), à réenregistrer et à regarder.
7. **Suivi du tour 6** : `sieges/tour-6/hors-banc.tsv` incomplet ; `docs/REVIEW.md` sans les « moyens » des relectures du tour 6.
8. **Paquet du portail** : le paquet 2 a reçu 17 fusions sans CI entre elles. C'est l'écart que la règle 13.6 ferme : le paquet 3 ne s'ouvre qu'après une CI verte.

**09 h 40** : lot « emulateur » fusionné et poussé (`b3617da21`), CI lancée sur cette tête. Le portail est FERMÉ jusqu'à son vert, émulateur compris. En attente pour le paquet 3 : `tutofleches` (agent local, en cours), `langues2` (`eq/tour-6-langues2`), et les lots du banc `portail3` (`eq/tour-6-portail3`, juge 68 → 15) et `rejeux7` (`eq/tour-6-rejeux7`, 3 rejeux). Contrôle du tuto : sur la tête poussée, pendant la CI du paquet 3, par Testeurs 2 et 3 ; le lot est retiré s'il échoue.

## Lots ouverts (un fichier = un seul lot ouvert)

| Lot | Zone | Fichiers | Qui | État |
|---|---|---|---|---|
| `tour-6 emulateur` (réparation de la CI) | coquille, premier lancement | `app/…/library/PlainWords.kt` (une ligne), deux tests neufs sous `app/src/test`, peut-être `JourneyTest` (ne plus avaler l'exception) | agent local, atelier `eq-tour-6-emulateur` | cause trouvée, correction en cours ; passe seul, avant tout autre lot |
| `tour-6 tutofleches` (lot 10) | tuto | `PianoRoll.kt`, `RhythmScreen.kt`, `TutoPanel.kt`, `TutoController.kt`, `TutoChecks.kt`, `resources/tutos/*.json`, `i18n/*/tuto*.json` | agent local, atelier `eq-tour-6-tutofleches` | en cours ; siège de contrôle sur sa branche avant le portail (13.5) |
| `tour-6 langues2` (lot 5) | textes | `i18n/*/*.json` hors tuto, `SimpleMix.kt`, `LanguagePicker.kt`, `LiveScreen.kt`, `MyExports.kt`, `FadingScroll.kt` | discussion « Sons modernes » | poussé (`c8a107b1`), relu, pré-vol prêt ; entre au paquet 3 |

## File du portail

1. `emulateur`, seul dans son paquet ; puis la CI. Rien d'autre n'entre avant qu'elle soit verte.
2. Paquet 3 (après cette CI verte) : `tutofleches` s'il passe son contrôle, `langues2` s'il est poussé.
3. Lots du banc, hors de toute vague : `portail3` (13.9), puis `rejeux7`.

## Discussions

- Testeurs (machines chaudes, ne modifient pas l'appli) : « Testeur 1 » (tuto Facile, français), « Testeur 2 » (tuto Facile, japonais), « Testeur 3 » (tuto Pro, allemand, tablette).
- « Sons modernes » : lot 5 livré. Essai de sons : Benjamin préfère les sons d'avant, on ne fusionne pas (sauvegarde `f6f6df66`).
- « Plan d'audit utilisateur » : la discussion de Benjamin (règle des niveaux, `PLAN-NIVEAUX.md`).
- Fermées dans la nuit, à rouvrir au besoin (machine froide au réveil) : les discussions des lots kick, gaucher, langues, écoute, et cinq discussions de sièges.

## Prochain départ

Pas avant : la CI verte, le tuto passé par son contrôle (0 chapitre bloqué, 0 grave neuf), le lot « portail3 » fusionné, et le lot du banc « niveaux » (`PLAN-NIVEAUX.md` : huit missions neuves). Avant la vague : un siège pilote par mission (13.4).
