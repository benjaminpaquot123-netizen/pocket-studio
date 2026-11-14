# Audit ultra, tour 3 — zone 6 : langues et accessibilité (n° 37, 38, et n° 7)

| N° | Verdict | Gravité | Test (rouge avant, vert après) | Commit |
|---|---|---|---|---|
| 7 coins des pads | appli | grave | `TalkBackNamesTest.padLivesCornersSayWhatTheyOpen` | 4d8f967 |
| 37 noms parlés | appli en partie, banc en partie | grave | `TalkBackNamesTest` (4 cas), `ScreenScanTest` (LABEL 0 sur les 300 écrans) | 4d8f967 |
| 37 contraste (gris pâles) | banc | — | `SeatOptions` : `lit=non` grise tout texte < 18 sp | — |
| 38 pluriels | appli | moyen | `CountWordsTest` (sources + formes), `Round3LanguagesTest` | 2ce48c3 |
| 38 langues | appli | moyen | `Round3LanguagesTest`, `I18nCatalogTest` | 2ce48c3 |

## 37 — TalkBack, vérifié sans le banc
- Le siège `lit=non` n'entend pas les noms TalkBack (`ScreenText` les tait) : « bouton (icône) » = bouton nommé.
  Relu sur l'arbre que l'appli donne à TalkBack (Robolectric, Mode simple, 5 onglets) : barre du haut, ▶ et ≡ des pistes,
  touches du clavier ont un nom → **banc**. Le contraste « 1,4:1 » vient des barres grises du banc → **banc**.
- **Appli**, vrais « sans nom » (du banc, `sans nom`, et du recensement) :
  - la poignée de chaque feuille : Material 1.4 la met dans une infobulle et un cliquable sans nom → poignée à nous
    (`SheetHandle`, même dessin), nommée « Poignée : glisse vers le bas pour fermer » ; la feuille se ferme toujours.
  - n° 7 : les 16 coins « Réglages du pad » n'avaient qu'un libellé d'action → « Réglages du pad, pad A1 ».
  - Live : boutons tenus (Freinage, Hachoir, Sans graves…), poignée du panneau, voiles des menus de case ;
    Rythme « Tous les pads » : 256 cases sans nom → « Grosse caisse, Pas 3 », allumée = sélectionnée.
  - Racine : `LayoutScan` comptait le libellé d'action comme un nom ; il compte désormais ce que TalkBack dit
    (mots + description). `ScreenScanTest` refuse tout « sans nom », sans marge ni plafond (26 plafonds LABEL retirés).
- « Rien ne dit qu'une note est enregistrée » (appli) : pendant la prise, « Ta prise : 3 notes » s'affiche et
  TalkBack l'annonce (région polie), mis à jour à chaque note ou coup.
- Touches noires « désactivé » : touches hors de la gamme verrouillée ; elles disent maintenant pourquoi.
- Reste : bulle d'aide qui couvre « Enregistrer ma mélodie » (moyen, même famille que le n° 22, zone 3).

## 38 — pluriels
- « Timeline: 1 parts » / « 1 parti » : phrase à deux nombres → deux `trCount`. Balayage : 35 textes passés par
  `trCount` (mesures, pas, temps, demi-tons, notes, parties, réglages, cents…). `CountWordsTest` lit les sources :
  un `tr("… %d mot …")` échoue, sauf liste motivée (limites, unités, nombres ≥ 2 garantis).
- Chaque compte a sa forme « 1 » dans les 14 langues, et ses formes polonaises (few/many) et arabes (two/few/many) :
  ≈ 230 formes écrites. Polonais : « kroki: 16 » → « 16 kroków » partout où une forme existe.

## 38 — langues (fautes vues + balayage de chaque catalogue)
- ko : particules écrites des deux façons (« 을(를) », « (이)란 ») choisies d'après le mot glissé (`KoreanParticles.resolve`
  dans `tr`) ; « streaming », « club », « remix » des noms de fichiers et de Mes projets traduits.
- pt-BR / es : « em a parte A » → « na parte A » (contraction devant un argument, `Contractions`).
- it / es / tr : « Re♭ » (plus « Ré♭ ») ; it : « Le sue prime carte » et 3 « Ses … » réécrits.
- hi : un seul registre (तुम) : 40 lignes ; bar/Verse/Chorus/Intro/Bridge/Build-up/Riser en devanagari (≈ 1 190 lignes).
- pl : « Dla: », « Długość pętli tego pada: », « Start w rytmie: » ; comptes en mots.
- nl : « Onderdelen » (≠ « Delen » = partager), « Vakjes » (≠ « Stap » du tuto), « Half/Dubbel tempo », « maten ».
- de : « Einfach » partout (plus « Leicht »), « Stereobreite », « Umschalter » (plus « Schildchen »).
- id : « ia » pour une chose (198 lignes), « menit » (durées du style), cliquetis « tik-tok » réécrit.
- tr : « aşağıdaki çubukta », « Ayara git », « müzik yavaşlar ». zh : 声部 (unisson), 合唱效果, 比特破碎, 摇摆.
- ar : Bass/Snare/Hi-hat/Cymbal/Transients/Live/Remix traduits (293 lignes) ; notes دو ري مي (solfège arabe) ;
  le titre d'une chanson n'est plus mis en capitales.
- Reste (noté) : « Pad », Kick, Drop… en hindi et en arabe (centaines de lignes, à traiter par terme) ; tuto Pro
  allemand (n° 32, zone 5) ; « -remix- » des pistes séparées (n° 12, zone 2).

## Tests lancés
- `:project-model:test --tests 'com.pocketstudio.model.i18n.*'` : les miens verts ; restent rouges 4 cas d'autres
  zones (clés inutilisées, « %s (vide) » pt-BR, « «» » espagnol, variables de LibraryScreen).
- `:app:testDebugUnitTest` : `CountWordsTest`, `TalkBackNamesTest`, `SongGuideTest` verts ; `ScreenScanTest` : LABEL 0
  partout ; 3 rouges HELP d'autres zones (pistes audio, partage).
- Captures : poignée des feuilles au pixel près (`NavScreenshotTest`, `TrackSheetScreenshotTest` : feuilles vertes) ;
  les écarts restants (étiquettes d'aide, partage) viennent des zones 2 et 5. Aucune capture ré-enregistrée.
