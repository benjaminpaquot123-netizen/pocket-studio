# Tour 5 — lot « courbes » (chantier 8) : rapport
Source : `PLAN-ACTION-AUDITS.md` étape 4 ligne 8 ; retours de Benjamin du 2026-11-08 (tel-14) ; `defauts-caches.tsv` B19, B20.
## Fait
1. **La nouvelle courbe se montre.** Après « Automatiser un réglage », la carte Courbes défile jusqu'à la courbe créée,
   l'encadre en ambre et dit « Nouvelle courbe : dessine-la ci-dessous. ». L'encadré part au premier trait.
2. **L'option reste visible.** Un réglage qui a sa courbe reste dans la liste, à sa place, « … (déjà une courbe) »,
   cerné d'ambre ; le toucher mène à cette courbe, sans en créer une seconde, et la ramène encore la fois suivante.
3. **« Voir le réglage » y mène.** « Effets · piste » s'ouvre sur le réglage : envoi, potard d'un effet (l'effet s'ouvre)
   ou bouton macro, encadré, avec « Une courbe fait bouger ce réglage pendant la lecture. ».
4. Trouvé en route : une courbe demandée d'ailleurs (« Ce qui joue », la frise) prend la place de celle encore encadrée ;
   le fond ambre de la courbe encadrée faisait passer « Vider » sous le seuil de contraste (4,37:1) : cadre seul.
## Preuves
- `CurvesFindTest`, 7 tests. Rouges ccca1dc3, 47709bb1, a434e1c1 ; verts a53e385e, c1d05caf, cc301649.
- Selftest du banc sur 44d381da (dernier commit de code, avant la fusion du lot « socle ») : 192 verts, 0 rouge.
- Vu rouge sans la ligne corrigée : sans l'attente de `SpotFrame`, l'envoi et la macro ; les deux derniers tests avant
  cc301649 ; le recensement de `automation_new_curve` avant 44d381da. La courbe n'a pas d'attente (verte sans).
- Captures neuves regardées : `automation_new_curve`, `automation_picker_has_curve`, `effects_spot_send`,
  `effects_spot_macro`. Réenregistrées : `automation`, `tablet8/automation` (cadre sans fond). 14 langues (`courbes.json`).
## Non fait, non prouvé
- Curseur figé pendant que la courbe joue (tel-14, T5-Fb-13) : hors chantier, la ligne reste ouverte.
- Réverbe et écho communs, sortie générale, 303 : « Voir le réglage » garde son chemin d'avant, sans encadré.
- Rien n'est essayé sur un appareil. Essais : (1) Mix › « Automatiser un réglage » › Batterie › « Filtre (balayage) » :
  courbe à l'écran, encadrée ? (2) rouvrir : « Filtre Coupure (déjà une courbe) » la ramène ? (3) courbe « Envoi
  réverbe · Batterie » › « Voir le réglage » : l'envoi à l'écran, encadré ? Pareil pour une macro.
- Passage ciblé sur la dernière fusion de fx-int (c4743738, lot « socle ») : 3 rouges hors lot, `simple_mix_muet` ×2
  (écart déjà noté par le lot accueil) et `remix_home` (écran du lot remix, une ligne de bouton diffère) ; le reste vert.
- Pré-vol : un seul « RECALÉ », les 8 liens des sous-modules de l'atelier (lignes « T »), que le contrôle de l'atelier
  d'équipe ne filtre pas ; ces lignes masquées le temps du contrôle, « PRÊT ». Script non touché. Décisions : `docs/decisions/tour-5-courbes.md`.
## Tests à rejouer
`*CurvesFindTest`, `*ScreenScanTest`, `*WhatPlaysTest`, `*TutoLot4Test`, `*I18nCatalogTest`, `*LexiconWordsTest`, `*ScreenshotTest`.
