# Tour 9 : lot « gr-styles » (réunion hip-hop + house sur L1b), rapport

Branche : `eq/tour-9-gr-styles`, partie de `eq/tour-9-gr-l1b` avec les fusions de `ccr-80af1a1b-mn0wec`, `eq/tour-9-gr-hiphop` et `eq/tour-9-gr-house`. `StyleRecipes.kt` régénéré en local seulement (skip-worktree), à régénérer à la fusion. Tout est « tenté ».

- project-model : 1296 tests, 0 échec (suite entière, en local).
- Pistes : `InstrumentTrack.MAX` reste à 8 (moteur taillé pour 8 pistes). Un style avec du corps garde une piste pour « ma mélodie » : cèdent d'abord la texture, puis le contre-chant ; jamais le sub, la basse, la voix ni la doublure. Détail : `docs/decisions/tour-9-gr-styles.md`.
- Guides régénérés (le générateur lit la voix devenue objet), 9 textes neufs traduits dans les 12 autres langues, 2 vieux textes house retirés, coréen remis au 해요체 (cette dernière retouche est entrée avec le vert des guides, sans rouge séparé).
- Lofi : le corps ne lui force plus de montée (pas de Drop), donc ni piste « Montée » ni son clipper ; boom bap pareil.
- Doublure : les notes au-dessus de do 7 (96) descendent d'une octave (le reggaeton montait à 97).
- Acid house (Benjamin : « il fait mal aux oreilles ») : résonance du Break 0,70 à 0,75 (au lieu de 0,80 à 0,92), et une cloche large de −4 dB à 6 kHz sur sa tranche.
- live-bench, 18 styles touchés (boom_bap, trap, drill, lofi, phonk, grime, jersey_club, reggaeton, afrobeats, house, deep_house, tech_house, uk_garage, speed_garage, acid_house, afro_house, disco, amapiano) : 18/18 ok, 0 alerte.
- Reste connu, hors lot : jump_up remplit ses 7 pistes, « ma mélodie » n'y a pas de place.

tests: *OneTouchStyleTest *WizardTracksOfferedTest

## Chemins

chemin: Projets → « Partir d'un style » en une touche, morceau fait | test: OneTouchStyleTest#aModeChosenOnceOneTouchMakesTheSong
chemin: assistant, pistes proposées d'un style | test: WizardTracksOfferedTest#aSongWithoutTheAcidStepShowsNoAcidTrack
