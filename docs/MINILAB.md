# Clavier MiniLab 3 (et autres petits claviers)

## Comment s'en servir (pour Benjamin)

1. Branche le MiniLab 3 au téléphone avec un câble USB-C (ou un adaptateur OTG). BenStudio le reconnaît tout seul et affiche « MiniLab 3 connecté — prêt à jouer ». Rien à régler.
2. Laisse le MiniLab sur son programme de départ (« ARTURIA », celui qui s'allume au branchement).
3. En bas à gauche, une petite pastille « MiniLab 3 » : touche-la pour voir ce que fait chaque bouton sur l'écran affiché. Touche encore pour la fermer.
4. Partout :
   - les **touches** jouent la piste choisie, avec la force de frappe ; bande de gauche = hauteur, bande de droite = modulation, pédale = tenue ;
   - les **4 faders** règlent le volume des 4 premières pistes (Batterie, Acide…). Ils ne bougent le son qu'une fois arrivés à la valeur à l'écran : pas de saut ;
   - le **gros bouton** change de piste (avec Shift : le tempo) ; **clic** = lecture / arrêt ; Shift + clic = arrêt.
5. Selon l'écran, les **8 potards** et les **pads** changent de rôle :
   - **Rythme** : pads = la batterie (banque B, Shift + pad 2 : pads 9 à 16) ; potards = la voie de la batterie ;
   - **Basse acide** : potards = la 303 (coupure, résonance, enveloppe, déclin, accent, saturation, accordage, volume) ;
   - **Synthé** : potards = les 8 réglages principaux du son ;
   - **Mixeur** : potards = gauche/droite puis envois réverbe des 4 premières pistes ;
   - **Live, panneau Effets** : pads 1 à 8 = lancer les lignes A à H (au prochain temps de lancement, comme sur la grille) ; banque B = couper / rallumer les colonnes ; potards 1 et 2 = le filtre DJ (le pad XY), puis Montée, Réverbe, Écho et la 303 ; gros bouton = tempo ;
   - **Live, panneau 303** : pads = lancer la ligne 303 de A à H ; potards = les 6 gros curseurs de la 303 (ils bougent à l'écran) + réverbe et écho ;
   - **Live, panneau Pads** (ou pads plein écran) : pads = les pads live 1 à 16, à la force de frappe (doux = moins fort, et la prise le garde) ;
   - **Live, une case qui enregistre** (« Jouer et enregistrer ici ») : les touches jouent la piste de la case et vont dedans ; pour une case de batterie, les pads jouent la batterie dans la case ;
   - **Live, panneau Notes** : les touches jouent la piste montrée dans Notes ;
   - **ailleurs** : potards = la voie de la piste choisie (volume, gauche/droite, couleur, saleté, mouvement, espace, envois).
6. Pendant une prise du Live (●), tout ce que tu tournes sur la 303 et les effets est enregistré, comme à l'écran ; les notes jouées au clavier et les pads live aussi.
7. Pour changer quelque chose : **Plus › Clavier MIDI › Contrôleur MIDI**. On y voit l'appareil, son profil, ce que fait chaque commande écran par écran, ce qui arrive en direct (« Ce qui arrive »), et des boutons « Apprendre » (pads, faders, gros bouton, clic, lecture, arrêt, enregistrer). « Voir et changer les potards » pour les potards. « Oublier mes réglages pour cet appareil » remet tout comme au départ.

Un autre clavier (Akai MPK mini, Novation Launchkey Mini, Korg nanoKONTROL2, Alesis, M-Audio, Nektar…) marche aussi : les touches jouent tout de suite, les pads sur le canal 10 aussi ; ses potards se rangent dans l'ordre où tu les tournes la première fois (ou « Apprendre »). Si ses pads n'envoient pas sur le canal 10 : « Apprendre mes pads », puis tape-les l'un après l'autre et « Terminé ». Tout ce que tu apprends est gardé pour cet appareil (son profil à toi).

## À tester avec ton vrai MiniLab 3

1. Brancher : le message « MiniLab 3 connecté — prêt à jouer » apparaît, la pastille « MiniLab 3 » aussi.
2. Touches, bande pitch, bande modulation, pédale de tenue (si tu en as une) sur un son de Synthé.
3. Rythme : les 8 pads jouent la batterie ; Shift + pad 2 (banque B) : les pads 9 à 16.
4. Basse acide : tourner le potard 1 jusqu'à la valeur à l'écran : le son ne saute pas, puis la coupure suit.
5. Live, panneau 303 : les 6 gros curseurs bougent avec les potards 1 à 6 ; pads 1 à 4 lancent la 303 des lignes A à D.
6. Live, panneau Effets : pad 2 lance la ligne B au bon moment ; banque B pad 1 coupe la batterie, encore une fois la rallume ; potards 1 et 2 = filtre DJ.
7. Faders 1 à 4 : volumes des 4 premières pistes, sans saut.
8. **Gros bouton** (le plus incertain, voir plus bas) : Plus › Clavier MIDI › « Ce qui arrive » : tourne-le, clique-le, avec et sans Shift, et dis-moi ce qui s'affiche (« CC 28 = 65 »…). S'il ne fait rien, « Apprendre » › « Gros bouton » le règle en une seconde.
9. Une prise Live (●) en tournant la 303 : à la réécoute, les mouvements sont là.
10. Débrancher puis rebrancher en pleine lecture : rien ne reste coincé.

## Table du MiniLab 3

| Commande | Ce qu'il envoie (programme ARTURIA, celui de départ) | Programme DAW (Shift + pad 3) | Sûr ? |
|---|---|---|---|
| Touches (25) | notes, canal du clavier (1 par défaut, Shift + touches pour le changer), vélocité | idem | sûr (manuel) |
| Bande pitch | pitch bend, retour au centre | idem | sûr (manuel) |
| Bande modulation | CC 1 | idem | sûr (manuel) |
| Pédale (prise jack) | CC 64 (tenue) sur le canal du clavier | idem | sûr (manuel MCC) |
| Pads banque A | notes 36 à 43, canal 10, vélocité + pression | notes 36 à 43, canal 10 | sûr (manuel + script Ableton) |
| Pads banque B (Shift + pad 2) | notes 44 à 51, canal 10 | notes 44 à 51, canal 10 | sûr |
| Potards 1 à 8 (sans fin) | CC 74, 71, 76, 77, 93, 18, 19, 16, absolus | CC 86, 87, 89, 90, 110, 111, 116, 117, relatifs autour de 64 | sûr (manuel / script Ableton) |
| Faders 1 à 4 | CC 82, 83, 85, 17 | CC 14, 15, 30, 31 | sûr (manuel / script Ableton) |
| Gros bouton (tourner) | non documenté pour ARTURIA ; l'appli écoute CC 28 (relatif autour de 64) | CC 28 | DAW sûr, ARTURIA à vérifier |
| Gros bouton + Shift | l'appli écoute CC 29 | CC 29 | idem |
| Clic / Shift + clic | l'appli écoute CC 118 / CC 119 | CC 118 / 119 | idem |
| Shift | — | CC 27 | DAW sûr |
| Shift + pads 4 à 7 (boucle, stop, lecture, enregistrer) | Mackie Control (notes sur le canal 1) en mode DAW | non utilisé par l'appli (ces notes se confondent avec les touches) | — |
| Nom USB | « Minilab3 » (Arturia), USB 0x1C75 / 0x220B | | sûr (script Ableton) |

Ce que l'appli fait de chaque commande, écran par écran : voir « Comment s'en servir » ci-dessus, et la légende dans l'appli (elle suit l'écran).

### Lumières des pads et écran (SysEx)
Le MiniLab 3 accepte des messages SysEx pour colorer ses pads (`F0 00 20 6B 7F 42 02 02 16 <pad> <R> <G> <B> F7`, pads 0x34 à 0x3B pour la banque A) et écrire sur son écran (`F0 00 20 6B 7F 42 04 02 60 …`), mais seulement une fois passé en programme « DAW » par un message de connexion (`F0 00 20 6B 7F 42 02 00 40 6A 21 F7`). Ce passage change ce qu'envoient les potards et les faders et laisse le clavier en mode DAW si l'appli s'arrête mal. Non testé sur un vrai MiniLab : **pas activé** pour l'instant (pas de lumières de clips). À reprendre quand on pourra l'essayer.

## Autres claviers connus de l'appli

| Clavier | Potards | Faders | Boutons | Sûr ? |
|---|---|---|---|---|
| Arturia MiniLab mkII | CC 74, 71, 76, 77, 93, 18, 19, 16 | — | — | à vérifier |
| Akai MPK mini (mk3, IV, Play) | CC 70 à 77 | — | — | à vérifier (guide Spectrasonics) |
| Novation Launchkey Mini (mk3, mk4) | CC 21 à 28 | — | Lecture CC 115, Enregistrer CC 117 (canal 16) | à vérifier |
| Novation Launchkey / Launch Control | CC 21 à 28 | — | — | à vérifier |
| Korg nanoKONTROL2 | CC 16 à 23 | CC 0 à 7 | Lecture 41, Arrêt 42, Enregistrer 45 | sûr (réglage d'usine Korg) |
| Korg nanoKONTROL | CC 16 à 23 | — | — | à vérifier |
| Akai LPD8 | CC 1 à 8 | — | — | à vérifier |
| Alesis V-Mini | CC 20 à 23 | — | — | à vérifier |
| Tout autre clavier (Keystation, Oxygen, Nektar, nanoKEY…) | dans l'ordre où on les tourne (blocs 21-28, 70-77, 16-23, 102-109 reconnus d'office) | « Apprendre » | « Apprendre » | générique |

Pads : canal 10, notes 36 à 51 par défaut ; sinon « Apprendre mes pads ». Les potards sans fin (relatifs) d'un clavier inconnu sont reconnus tout seuls à ce qu'ils envoient (toujours le même petit pas, jamais leur valeur « rien »).

## Sources
- Arturia, MiniLab 3 User Manual 1.0.5 (Shift functions, pads bank A/B channel 10, touch strips, knobs 74/71/76/77/93/18/19/16, faders 82/83/85/17) : https://dl.arturia.net/products/minilab-3/manual/minilab-3_Manual_1_0_5_EN.pdf
- Arturia, MIDI Control Center for MiniLab 3, manuel 1.14.1 (encodeur, Shift, pédale CC 64, modes relatifs) : https://dl.arturia.net/products/minilab-3/manual/minilab-3-mcc_Manual_1_14_1_EN.pdf
- Script Ableton Live « MiniLab_3 » décompilé (programme DAW : CC 86… relatifs, faders 14/15/30/31, encodeur 28/29, clic 118/119, Shift 27, SysEx des pads et de l'écran, USB 7285/8715) : https://github.com/matttturnbull/minilab3
- Forum Arturia (fader 2 = CC 83) : https://forum.arturia.com/t/minilab-3-fader-cc-remapping-doesnt-work/1903
- Elektronauts (MiniLab 3, potards relatifs depuis le firmware 1.1) : https://www.elektronauts.com/t/arturia-minilab-3-relative-encoders-with-ableton/204017
- Spectrasonics, MPK mini mk3 (potards CC 70 à 77) : https://support.spectrasonics.net/manual/Omnisphere3HW/3/en/topic/setting-up-the-mpk-mini-mk3
- Novation, Launchkey MK3 Programmer's Reference (mode autonome : boutons en CC sur le canal 16, pads batterie canal 10) : https://fael-downloads-prod.focusrite.com/customer/test/s3fs-public/downloads/Launchkey_MK3_Programmers_Reference.pdf
