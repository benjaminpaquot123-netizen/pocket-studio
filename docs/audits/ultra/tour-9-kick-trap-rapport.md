# Tour 9, kick-trap : « claquement du kick trap » (rapport)

Code testé : d87dea73 (intégration ccr-80af1a1b-mn0wec) et e17592fe (eq/gr-l12, hors intégration).

## Ce que dit la source
La promesse « claquement kick trap → lot recettes » (docs/PROMESSES.md) vient du rapport gr-l12
(`docs/audits/ultra/tour-9-gr-l12-rapport.md` sur eq/gr-l12) : c'est la **claque** du kick
(`ps_measure --punch`) qui recule en trap avec les réglages B du nouvel effet « Bus batterie »
(Refrain 3.58 → 3.35, Couplet 5.23 → 4.45), pas un clic. Remède déjà mesuré là-bas : réglage doux
dans la recette trap (V4 : drive 0.2, attaque 0.85, mix 0.5 ; claque 3.34 / 5.01, à 0.25 dB de A).

## Mesures (aucun clic trouvé)
- Les 32 kicks des packs trap, house, techno, drill (release sons-packs-6, vrais FLAC) : premier et
  dernier échantillon à 0, 64 derniers sous 0.0001 du pic ; aigus au-dessus de 5 kHz dans les 10 ms
  d'attaque : trap 0.04 % en moyenne (le plus propre des quatre ; techno 2.1 %, house 0.9 %).
- Écoute en direct (`ps_live`, rejeu du chant trap avec les vrais samples à la place des sinus de
  BenchFlac) : mix complet et batterie seule (`--solo 0`), 30 s : 0 alerte (fondu, silence, saut),
  0 allocation.
- Pad du kick en trap : aucun groupe de choke, release 3 ms par défaut, aucun vol de voix (motif
  `X......X..X.....`, le son de 0.2 s finit avant la frappe suivante).

## Conclusion
Rien à corriger dans le moteur ni dans les sons. La correction est un réglage de la recette trap
(`scripts/packs/recipes/hiphop.json`, hors de ma liste) et n'a de sens qu'une fois gr-l12 (Bus
batterie) entré : le lot recettes qui allumera le Bus batterie en trap reprend le réglage V4.

tests: aucun : pas de défaut reproduit, aucun code modifié
