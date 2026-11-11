# Tour 5, chantier 7 « Pad Live » : rapport de l'auteur (2026-11-11)

Atelier `eq-tour-5-padlive`, branche `eq/tour-5-padlive` (fx-int + lots « navigation » et « plus »). Rien de poussé. À relire par un relecteur neuf.

## Fait (rouge puis vert, même nom de test)
- **R.7 / T5-PADRETRIG (moteur)** : `ClipLauncher::toggle`, lecture arrêtée et aucun autre pad allumé : un « coup » sonne tout de suite à chaque toucher (retouché : il repart, l'ancien passage s'efface en 5 ms). Avec une boucle qui joue, il garde le temps des boucles, mais un retoucher ne l'annule plus. Lecture en marche : inchangé. Le test `known_defect_t5_padretrig` est inversé en `padlive_hit_retouched_sounds_at_once` (niveau sonore mesuré, pas seulement l'état) ; miroir `BenchClips` et `BenchPadLiveTest#aHitRetouchedSoundsAtOnce`. Commentaire de `Challenge.example` corrigé. Callback audio : ni allocation ni verrou ajoutés (un `none_of` sur 64 cases, hors rendu). Rouge 854026f9 / vert 16f520d7.
- **« Tout couper » visible (G8)** : carré rouge (■ noir, mot dessous, 64 dp) à gauche de « Enregistrer ce que je joue » (qui passe sur deux lignes, mots entiers) ; sorti de « Plus ». Noir sur rouge : le blanc ne faisait que 2,8:1 (AccessibilityScanTest).
- **Il coupe tout, tempo compris (4A)** : `RhythmViewModel.cutEverything` (Pad Live, carte et plein écran des pads live) arrête pads et lecture. `BenchPadLiveStopTest` rouge 8765d690 / vert e6531060.
- **« ■ Tout » de la vue Live (T4-01)** : arrêt sur la ligne de lancement gardé, puis la lecture s'arrête quand plus rien n'attend (`stopAllArmed`, annulé par un autre lancement, un glissé ou un arrêt). @KnownDefect T4-LIVESTOP levée : rouge 0ac5cf4b / vert c423455d. Le faux moteur `SongEngine` joue maintenant les arrêts armés ; `Tour4AppProofTest` suit la nouvelle règle.
- **Un glissé ne déclenche plus un effet (G14, T5-Ed-40)** : Montée part au lâcher d'un toucher sans glissé ; Écho, Chute, Bégaie partent tout de suite mais lâchent si le doigt glisse en haut/bas dans leurs 120 premières ms (le geste va au défilement). Filtre et Roulement se règlent en glissant : inchangés. `PadLiveGlideTest` rouge fb0c0f8b / vert 9aaaccf8.
- Aide de « Plus » réécrite (zone `padlivestop`, 14 langues), ancienne clé retirée de `restes`. Captures regardées et réenregistrées : padlive, padlive_more, padlive_small, challenge(_done), filter, roll, levels, perf, recording.
- verdicts.tsv / base.tsv : T4-01, T5-Ed-11, T5-Ed-40, T5-PADRETRIG « corrigé » (preuves et paires notées). 6 lignes dans DECISIONS.md.

## Restes
- Hors périmètre (noté) : badge visible des réglages du pad (M6), vibration et second toucher qui arrête Montée (G14 c), défilement couché seulement si le contenu dépasse (G14 d).
- Un glissé sur Écho/Chute/Bégaie peut encore les faire sonner le temps d'atteindre le seuil (quelques ms) : choix d'un départ sans retard (G14 b).
- Rouges antérieurs, pas de ce lot : `I18nCatalogTest#noTranslationIsLeftUnused` (« Renommer le morceau », nav.json), `LexiconWordsTest#theGuideHasOneWordInEveryLanguage` (ar) ; verifie-chantier : 3 écarts hérités (2b34c216, 96a7a702, 3fa7eeb8) + résumé absent.

## Suite complète
`./scripts/host-tests.sh` vert. Une fois (PS_STALL_MIN=70) `:project-model:test :app:verifyRoborazziDebug` : 8242 cas ; de ce lot, `BenchTour4Test#liveStopsReachTheirLaunchLineOnTheBench` (règle 4A, corrigé ensuite) et les 5 captures `sizes/*_padlive` (regardées, réenregistrées), rejoués verts. Restent rouges, hors Pad Live et non réenregistrés : recette, cut_screen(_magnifier), remix_stem_edit_menu, rhythm_scrolled_under_part (interdits), nav_guide_first (fr/nl 130 %, tablette), simple_mix_muet (fr 130 %, nav), venus des lots fusionnés.

## Essais téléphone à rejouer
tel-08 (Tout couper : tout s'arrête, temps compris), tel-12 (■ Tout du Live : la lecture s'arrête à la ligne), tel-11 (tablette couchée : un glissé depuis Montée fait défiler, rien ne s'allume), tel-17 (clap retouché en rythme, lecture arrêtée : chaque toucher sonne).
- M1 corrigé : 6aa204c5

tests: *BenchPadLiveStopTest *PadLiveGlideTest *BenchPadLiveTest *PadLiveTakeTest *Tour5AppProof3Test *Tour4AppProofTest *BenchTour4Test
