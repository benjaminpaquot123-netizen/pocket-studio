# Tour 9, gr-l12 : attaque et poids (rapport)

Plan : `docs/audits/GRAND-REMPLACEMENT-V2.md` §5 L12. Code testé : la tête de `eq/tour-9-gr-l12`.

## Ce qui entre
- Trois effets neufs dans le moteur (`InsertSlot` 30, 31, 32) et dans la liste (`EffectType`) :
  - **Attaque** (`TRANSIENT`) : durcit ou adoucit le début de chaque coup, allonge ou raccourcit sa queue, sans toucher au niveau moyen ;
  - **Bus batterie** (`DRUM_BUS`) : saturation douce normalisée sur le pic, le pic du kick ne monte pas ;
  - **Graves mono** (`MID_SIDE_EQ`) : tout le grave sous la coupure au centre, aigus des côtés réglables.
- Préréglages `body` (attaque + bus batterie) et `mono_lows`. Rien n'est posé d'office : tout reste neutre tant qu'une recette ne l'allume pas (StyleRecipes n'est pas à ce lot).
- La réverbe « pièce » (type 1) est réutilisée telle quelle, aucun code neuf.
- Thread audio : aucune allocation, aucun verrou, aucun log ; mémoire préparée hors du callback. Le banc direct compte 0 allocation (ALLOC news=0).

## Mesures (A = sans, B = attaque 0.65/0.5/1.0/0.5 + bus batterie 0.5/0.6/0.5 sur les cases 3 et 4 de la batterie)
Batterie seule avant le master (`--no-limiter --solo 0`) :

| Style | LUFS A → B | Gain |
|---|---|---|
| techno | −18.34 → −16.94 | +1.40 LU |
| house | −16.96 → −15.63 | +1.33 LU |
| trap | −14.35 → −13.02 | +1.33 LU |

Export complet : true peak −1.19 partout (inchangé) ; LUFS +0.30, +0.35, −0.16.
Claque du kick (`ps_measure --punch`, montée du coup dans l'export), A → B :
- techno : Groove 6.17 → 7.04, Montée 4.36 → 5.05, Drop 2.86 → 3.51 ;
- house : Groove 7.73 → 8.55, Montée 8.06 → 8.96, Drop 4.92 → 5.42 ;
- trap : Refrain 3.58 → 3.35, Couplet 5.23 → 4.45.

En trap, la perte vient du bus batterie (le 808, pad 17, est dans la même tranche que les fûts). Réglage doux conseillé au lot des recettes (V4 : drive 0.2, attaque 0.85, mix 0.5) : batterie +1.0 LU, claque 3.34 / 5.01, à 0.25 dB de A.

## Écoute en direct (`ps_live`, 150 s, A et B)
- 6 passages : 0 alerte (fondu, silence, saut), 0 allocation ; B coûte +0.6 à +1 % de CPU moyen.
- Batterie seule en direct : techno −12.08 → −10.94, house −10.72 → −9.80, trap −10.55 → −9.30 (+0.9 à +1.25 LU), même sens et même ordre que l'export.
- Craquements seulement sur deux passages A (techno 2, trap 3) : machine chargée par 12 rendus en parallèle ; B en a 0.

## Fichiers A/B (non commités)
`/tmp/claude-0/-home-user-pocket-studio/10830bc3-bd36-5bb2-b582-7faf3a73426b/scratchpad/ab/` : `wav/<style>.<A|B|S|T|D|V1-V4>.*`, commandes dans `cmds/` et `cmds2/`, direct dans `live/`.

## Tests
- host-tests : ALL PASSED (tests neufs `engine/host-tests/l12_tests.inc`).
- `:project-model:test` vert (textes du guide traduits dans les 14 langues, registre coréen poli).
- App : Tour9EffectAddTest, FxBrowseTest et voisins verts ; captures `effects_picker` et `effects_full` réenregistrées et regardées en entier (pastilles neuves alignées sur leurs voisines).

## Ce qui n'est pas vert
- `verifyRoborazziDebug` sur ScreenshotTest : `rhythm`, `rhythm_after_wizard`, `session`, `timeline` diffèrent déjà sans ce lot (« Tous les pads » passe à la ligne) : hors de ma zone, non touchés.
- Claque du kick en trap en léger recul avec les réglages B : à régler dans les recettes (conseil V4 ci-dessus).
