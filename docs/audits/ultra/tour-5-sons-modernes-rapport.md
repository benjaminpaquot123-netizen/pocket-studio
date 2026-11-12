# Rapport du lot « sons-modernes » (tour 5) : étude des sons « années 90-2000 » et plan d'essai

## Ce qui est livré

- `docs/sons/PLAN-SONS-MODERNES.md` : d'où viennent les sons ; pourquoi ils sonnent rétro (mesuré voix par voix) ; ce qu'on a le droit de mettre dans une appli payante (sons, formes d'onde, MIDI) ; les options payantes avec un petit comparatif ; les synthés libres et leurs licences ; un plan d'essai en trois temps sur jump up, tech house et techno ; quatre questions pour Benjamin.
- `docs/decisions/tour-5-sons-modernes.md` : les décisions du lot.
- `docs/sons/outils/voix.py` et son test `test_voix.py` : la mesure d'une voix rendue seule. Le test la vérifie, colonne par colonne, sur des dents de scie dont les chiffres sont connus ; sept versions cassées de l'outil sont refusées. Les chiffres « avant » et les commandes sont dans `docs/sons/outils/mesures-2026-11-12.md`.

## Ce qui n'est pas touché

Aucun fichier de l'appli, du moteur, du banc (`scripts/bench/`), des tests Kotlin, de BenStudio Test ni des rejeux. Aucun son téléchargé ni commité : les rendus du banc sont restés hors du dépôt et ont été effacés après mesure.

## Preuves

- Mesures : `scripts/song-bench.sh` sur jump up, house, techno, hard techno, puis tech house. Chaque voix est rendue seule (`ps_render --no-limiter --solo`) et mesurée par `voix.py`. Le mix tient ses cibles ; plusieurs voix sont 15 à 35 dB plus sombres qu'une dent de scie brute au-dessus de 10 kHz.
- Licences : lues sur les pages des sources le 2026-11-12. Les clés ont été relues par la relecture neuve : Stability, 99Sounds, Splice, free-midi-chords, Groove MIDI, Vital, FAQ Surge XT, `co.xfer_ott`, AKWF, WaivOps, Freesound.
- Outil : `PYTHONDONTWRITEBYTECODE=1 python3 docs/sons/outils/test_voix.py` → « voix.py: OK ».
- Relecture neuve : `docs/audits/ultra/tour-5-sons-modernes-relecture.md`, en trois passes. Toutes les remarques ont été corrigées, dont 3 graves au premier passage (l'OTT existe déjà, le rétro vient des recettes et pas des mots des packs, le branchement de l'essai).

## Tests ciblés et chemins

tests: aucun : étude et documents seulement, aucun code de l'appli, du moteur ni du banc touché ; l'outil de mesure a son propre test Python (`docs/sons/outils/test_voix.py`)

Les chemins ci-dessous sont ceux par où passera l'essai : le choix d'un son par rôle, que l'interrupteur d'essai changera. Leurs tests existants doivent rester verts quand l'essai sera codé.

- chemin: Assistant › une ligne de batterie prend le son du pack avant le kit | test: SoundCategoriesTest#aLineWithoutAKitVoiceStillTakesThePackFirst
- chemin: Assistant › une ligne qui impose sa voix du kit la garde, pack téléchargé | test: SoundCategoriesTest#aLineThatAsksForAKitVoiceKeepsItWithThePackDownloaded
- chemin: Assistant › la machine nommée par la ligne passe en premier | test: WizardLineFxTest#theStylesPacksAreOfferedAtTheLineTheMachineOfThatLineFirst
- chemin: Bibliothèque › les packs du style proposés d'abord | test: LibraryChoicesTest#theStylesPacksAreOfferedFirstAndDownloadedInOneTouch

## Ce dont je ne suis pas sûr

- « Sombre » est mesuré face à une dent de scie brute, pas face à des morceaux récents : l'essai ajoutera des repères modernes, mesurés avec le même outil.
- Les prix d'un pack commandé à un sound designer sont des estimations, pas des tarifs publiés.
- Les petits sites « CC0 » (Signature Sounds) se vérifient pack par pack, avec une capture datée de chaque licence.
