# Tour 9 : lot « gr-house2 » (écoute de Benjamin, famille house), rapport

Branche : `eq/tour-9-gr-house2` (base `eq/tour-9-gr-styles` 042775a8f). Rouge 227cc0c2, vert 28957b95. Tout est « tenté » (aucun testeur neuf ne l'a encore entendu).

- acid_house (« la basse sonne comme le kick, tout démarre d'un coup ») : la 303 ne joue plus jamais sur les kicks (notes et accents entre les temps), decay 0,5 pour qu'elle chante ; elle s'ouvre depuis un filtre fermé dans le Groove ; le clap attend le Drop ; montée légère au lieu du souffle.
- house (« petit son toutes les dix secondes », « vieux synthé », « ringard ») : plus de bout de voix qui revient ; shaker doux au lieu du tambourin 707 ; une mélodie pincée syncopée (Pluck future) au lieu de l'arpège à l'ancienne.
- tech_house (claps, 10 premières secondes ringardes) : clap sur 2 et 4 seulement, sans écho ni petit clap de relance ; le motif de « toc » et la conga attendent le Drop, l'intro = kick, charleys, basse ; basse et voix hachées gardées.
- afro_house (« commence comme la tech house », « toc » et tom) : vraies percussions à la main de la bibliothèque (conga, bongos en 3-3-2, cajon à la place du tom aigu) ; plus de bois ; les accords chauds jouent dès le début ; montée légère.
- deep_house : pas touché ; banc identique (-14,5 .. -10,1 LU, courbe 0,63).
- Hors liste accordés par le chef : 14 catalogues `i18n/*/model.json` (5 textes ajoutés, 6 orphelins retirés) et 4 guides régénérés (`guides/{house,acid_house,tech_house,afro_house}.json`). `StyleRecipes.kt` régénéré contient aussi le hiphop.json de 8c6b8545, pas encore régénéré sur la base.
- Aucun son neuf : les percussions viennent de `percussions_acoustiques` déjà présent, rien à ajouter à THIRD_PARTY.md.

live-bench 75 s, 5 styles : avant 5/5, après 5/5, 0 alerte.

| style | avant (LU) | après (LU) |
|---|---|---|
| acid_house | -14,6 .. -10,8 | -13,9 .. -10,5 |
| afro_house | -19,2 .. -10,1 | -13,2 .. -9,7 |
| deep_house | -14,5 .. -10,1 | -14,5 .. -10,1 |
| house | -18,5 .. -9,5 | -18,5 .. -9,5 |
| tech_house | -16,7 .. -10,4 | -17,0 .. -10,4 |

tests: :project-model:test (complet, 1301, vert) ; :app:testDebugUnitTest *OneTouchStyleTest

## Chemins

chemin: Projets → « Partir d'un style » en une touche, morceau fait | test: OneTouchStyleTest#aModeChosenOnceOneTouchMakesTheSong
chemin: assistant → lecture en direct des styles house | test: LiveSongRecorder#recordsTheLivePlaybackOfEveryStyle
chemin: assistant → acid house, la 303 entre les kicks et qui s'ouvre | test: HouseRetouchTest#theAcidLineStaysOffTheKick, HouseRetouchTest#theAcidSongBuildsInsteadOfStartingAllAtOnce
chemin: assistant → house, tech house, afro house | test: HouseRetouchTest#houseLosesItsDatedSounds, HouseRetouchTest#techHouseStartsCleanAndClapsOnce, HouseRetouchTest#afroHouseStartsOnItsHandDrums

## À confier (lot tuto)

- `tutos/afro_house.json` : parle encore du « toc » de bois (pad du bois, l. 93-94), des toms (l. 28, 55, 59, 256, 287, 366) et du pad « Tom médium » (l. 103, 371) ; le pad joue maintenant un cajon et le bois a laissé place aux bongos. Les tests du tuto restent verts (même rôle, même motif).
- `tutos/tech_house.json` : l'étape clap_do dit « 2 et 4, et un écho » (plus d'écho) ; le textPro l. 91 parle du motif de rim sur 2 mesures.
