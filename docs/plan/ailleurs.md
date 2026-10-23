# Ce que font les meilleures applis pour qu'on finisse un morceau (recherche du 2026-10-23)

But : trouver ce qui rend « finir un morceau complet » facile ailleurs, et le relier aux problèmes de `docs/AUDIT-33.md`.
Méthode : guides officiels (Apple, Image-Line, Ableton, Native Instruments, teenage engineering, Elektron, BandLab, Splice), notes de version 2024-2026 et critiques sérieuses (Sound On Sound, MusicTech, MusicRadar, CDM). Les nombres de gestes viennent des guides officiels quand ils sont indiqués ; sinon ils sont comptés d'après les étapes décrites et notés « env. ».

Abréviations des renvois à l'audit : **AUDIT n** = point n du résumé `AUDIT-33.md` ; **A1 P2** = amateur 1, point P2 ; **A2 #1** = amateur 2, ligne 1 du tableau ; **P1 §1** = pro 1, point 1 ; **P3 c1** = pro 3, constat 1.

---

## 1. Navigation : les écrans et le chemin « de l'idée au morceau fini »

### Ce que font les meilleurs
- **Koala Sampler : 3 onglets qui sont 3 étapes**, pas 3 outils : Sample → Sequence → Perform. On enregistre un son en tenant un pad, on enregistre une séquence (env. 5 gestes), puis on joue les séquences en direct avec 8 curseurs d'effets ; « on va du premier son au morceau terminé sans sortir de Koala », export WAV. Le développeur revendique de « rester dans le flux, sans pages de réglages ». ([TapSmart](https://www.tapsmart.com/tips-and-tricks/koala-sampler-how-to/), [Sound On Sound](https://www.soundonsound.com/reviews/elf-audio-koala-sampler), [App Store](https://apple.co/2NwcL8m))
- **GarageBand (iPhone) : pas d'onglets.** Mes morceaux → choix d'un instrument → vue Pistes, et un bouton qui bascule entre Pistes et la grille Live Loops (le bouton n'apparaît que si une grille existe). Les sections sont dans la règle du haut. ([guide Apple, barre de commandes](https://support.apple.com/guide/garageband-iphone/chs157aec9e/ios))
  - Piège identique au nôtre : ▶ joue la section choisie ; pour tout entendre il faut ouvrir les sections, choisir « Toutes les sections », OK, puis ▶ (**4 gestes**). C'est exactement notre problème AUDIT 2 / A1 P1 : ne pas copier ce défaut. ([guide Apple, sections](https://support.apple.com/guide/garageband-iphone/chs3c3ef5dc/ios))
- **Ableton Note : une seule vue de projet** (8 pistes × 8 clips × 8 scènes) et une vue d'instrument. L'export audio **met les scènes bout à bout dans l'ordre** : la liste des scènes *est* le morceau, sans autre écran. ([Ableton](https://www.ableton.com/en/note/), [Sound On Sound](https://www.soundonsound.com/reviews/ableton-note))
- **FL Studio Mobile 5 (13 octobre 2026)** : la Playlist reste l'écran principal ; nouveau mode portrait, zoom arrière pour voir tout le projet, option « animations réduites », 15 000 sons intégrés sans compte. Le panneau d'accueil se limite à 4 rubriques : Morceaux, Réglages, Boutique, Partage. ([notes de version Image-Line](https://forum.image-line.com/viewtopic.php?f=1964&t=260871), [manuel, Home Panel](https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/plugins/FL%20Studio%20Mobile_HomePanel.htm))
- **BandLab** : un seul bouton « Créer » ouvre des portes d'entrée (Studio, Looper, SongStarter, Sampler, Splitter, Mastering), qui mènent toutes au même Studio. ([App Store BandLab](https://apps.apple.com/app/bandlab-music-making-studio/id968585775), [aide Looper](https://help.bandlab.com/hc/en-us/articles/115004496573-Using-the-Looper))
- **Deux vues du même contenu, une bascule plutôt que deux pages** : Maschine (vue Idées = scènes, vue Morceau = sections qui *renvoient* aux scènes ; modifier une scène change toutes les sections qui l'utilisent) et BeatMaker 3 (vue Scènes pour jouer, vue Morceau pour la ligne du temps). ([manuel Maschine](https://www.native-instruments.com/ni-tech-manuals/maschine-mk3-manual/en/working-with-the-arranger), [aide BeatMaker](https://support.intua.net/hc/en-us/articles/210181486-Sequencer-View))
- **Simple contre pro** : Logic Pro (Mac) démarre les débutants avec « Outils avancés » décoché : « fonctions de base et interface épurée » ; on coche ensuite seulement ce dont on a besoin. GarageBand et Logic sont le même moteur avec deux niveaux. ([aide Logic Pro](https://help.apple.com/logicpro/mac/10.4.7/en.lproj/lgcp5cbf192f.html))

### Ce qui coince chez nous
7 onglets classés par outil (AUDIT 1, P3 c1, A4 #7) ; trois vues d'arrangement (P3 c1) ; trois « Live » dont le plus gros bouton de Morceau (P3 c2, A1 P4) ; Partager caché dans le Mixeur (AUDIT 2, A1 P8, A4 #4) ; ▶ qui joue « cette section » (A1 P1, P3 c9).

### Proposition pour Pocket Studio
1. **Barre du bas à 4 espaces**, les mêmes en mode simple et en mode complet (une seule navigation à entretenir) :
   - **Morceau** (accueil du projet) : les parties du morceau + grand « ▶ Écouter tout » + « + Partie » ; la ligne du temps est la vue « Détail » du même écran (bascule, comme Maschine).
   - **Sons** : la liste des pistes (Batterie, Basse, Mélodie, Nappe, Voix…) ; toucher une piste ouvre *son* éditeur (pads, clavier/notes, 303, micro) dans la même page (proposition P3 « Sons »).
   - **Mix** : console côte à côte + « Équilibrer » + « Finition » (voir §4).
   - **Plus** : projets, aide, réglages, latence, MIDI, recette (avancé).
2. **Live sort du flux** : un bouton « Jouer en live » plein écran depuis Morceau (comme la bascule Live Loops de GarageBand), pas en tête de page.
3. **Barre du haut commune** : ← Projets · nom · **[▶ Morceau | ▶ Cette partie]** (deux états écrits dans le bouton) · position « mes. 14/60 » · ↶ · **⤴ Partager** · **?** aide.
4. **Mode simple = « Outils avancés » décochés** (question à l'accueil « Je débute / Je connais déjà », un geste, réversible dans Plus). Il cache Acide (sauf styles acides), WAV/FLAC, automation dessinée, réglages de synthé ; il ne change pas la navigation.
5. **Nombre de gestes visé** : écouter tout le morceau = 1 ; partager = 2 ; changer le son d'une piste = 3 (Sons › piste › Changer le son) ; enregistrer ma mélodie = 2 (Sons › Clavier › ●).

---

## 2. Structure et arrangement

### Ce que font les meilleurs
- **GarageBand, sections** : ajouter une section = **3 gestes**, la dupliquer = **4 gestes** ; 8 mesures par défaut, longueur libre au pas d'une mesure ou « Automatique » ; morceau jusqu'à 2 000 mesures. ([guide Apple](https://support.apple.com/guide/garageband-iphone/chs3c3ef5dc/ios))
- **GarageBand Drummer** : par région, un **pavé XY « doux ↔ fort » × « simple ↔ complexe »** et un curseur **Fills** (plus ou moins de roulements), 1 glissé chacun ; changer de style = 3 gestes ; swing = 2 gestes ; toucher un fût le coupe. ([guide Apple, Drummer](https://support.apple.com/guide/garageband-iphone/chs9692b2133/ios))
- **Logic Pro, Session Players** : curseurs Intensité et Complexité, plus « Quantité de fills » et « Complexité des fills » ; l'astuce enseignée : copier la région du couplet pour le refrain et monter l'intensité. ([guide Apple](https://support.apple.com/en-ca/guide/logicpro-ipad/lpipdc3bba32/ipados), [CreativeLive](https://site.creativelive.com/class/songwriting-with-ai-tomas-george-and-peter-darling/lessons/upbeat-part-1-drums-arranging-with-logic-pro-11-s-session-players))
- **Claviers arrangeurs Yamaha** (le modèle le plus clair pour les débutants) : boutons **Intro**, **Main A → D** (de plus en plus chargé), **Fill In** (1 mesure, joué automatiquement au changement de partie si « Auto Fill »), **Break**, **Ending** (joue une fin puis s'arrête). ([PSR Tutorial](https://forum.psrtutorial.com/index.php/topic,49595.0.html), [Yamaha Musicians](https://yamahamusicians.com/forum/viewtopic.php?p=130431))
- **EP-133 K.O. II** : **Commit** duplique la scène en cours et bascule dans la copie (une variation en un geste) ; 99 scènes ; le mode morceau (OS 2.0) enchaîne jusqu'à 99 positions ; sauvegarde continue. ([Sound On Sound](https://www.soundonsound.com/reviews/teenage-engineering-ep-133-ko-ii), [Guitarguitar](https://www.guitarguitar.co.uk/product/260121453006027--teenage-engineering-ep-133-ko-ii-128mb-edition), [radiocolon](https://www.radiocolon.com/Novedades-Teenage-Engineering-Abril-25-nt1422.html))
- **Elektron** : mode morceau qui enchaîne des dizaines de motifs ; condition **FILL** : un pas ne joue que quand le mode fill est tenu, donc fills et variations sans dupliquer le motif. ([Sound On Sound](https://www.soundonsound.com/node/4929439), [guide Elektronauts](https://www.elektronauts.com/t/digitakt-unofficial-guide/40705/97), [notes Digitakt II](https://www.elektron.se/release-notes/digitakt-ii))
- **Novation Circuit Tracks** : une **scène** = photo des motifs en cours ; les scènes s'enchaînent en morceau ; 16 pads de scène sur la page Mixeur. ([Sound On Sound](https://www.soundonsound.com/reviews/novation-circuit-tracks?amp))
- **Maschine** : la section *renvoie* à la scène ; modifier la scène change toutes ses sections (comme nos parties). **FL Studio** : « Rendre unique » sépare une copie pour la varier sans toucher aux autres ; sur mobile, Copier puis appui long pour Coller. ([manuel Maschine](https://www.native-instruments.com/ni-tech-manuals/maschine-mk3-manual/en/working-with-the-arranger), [Packt](https://hub.packtpub.com/using-playlist/amp/), [forum Image-Line](https://forum.image-line.com/viewtopic.php?p=1575377))
- **GarageBand Live Loops → Pistes** et **Remixlive 6 (mode morceau)** : on joue la grille, on enregistre, la prestation devient une ligne du temps qu'on retouche. ([guide Apple](https://support.apple.com/guide/garageband-iphone/chs02aae0e6b/ios), [Bonedo](https://www.bonedo.de/?p=586130))
- **IA, pour l'idée de geste** : Udio **Étendre** (ajouter une intro, avant, après, ou une outro ; paroles, instrumental ou automatique) et **Inpaint** (refaire une zone en gardant le reste) ; Suno **Remplacer / Étendre une section**. ([Tom's Guide](https://www.tomsguide.com/how-to-extend-a-song-in-udio), [Jack Righteous, Suno](https://jackrighteous.com/ja/blogs/guides-using-suno-ai-music-creation/suno-ai-in-song-editor-v4-replace-extend-crop-more))

### Ce qui coince chez nous
Pas de « drop plus fort », « break », « fin » en un geste (AUDIT 3, A1 P2, A1 P7) ; un fill = 6 gestes ou plus et une scène de plus (P1 §1) ; partie = scène × répétitions, longueur non réglable (P1 §2) ; batterie en une seule rangée (A3 #1) ; on ne sait pas qu'une scène sert à plusieurs parties (P1 §10, A1 P6) ; plan du morceau à construire à la main depuis un projet vide (A2 #6).

### Proposition pour Pocket Studio
1. **Un seul mot : « Partie »** (Intro, Couplet, Refrain, Drop, Break, Fin). La scène devient un détail interne. Une partie qui partage son contenu affiche « liée à Refrain 1 » et un bouton **« Rendre unique »** (copie la scène pour cette partie). « Dupliquer la partie » crée par défaut une copie indépendante (comme GarageBand) ; « Répéter ×2 » garde le lien.
2. **Curseur « Énergie » par partie (1 à 4)**, inspiré de Main A→D et du pavé XY de Drummer. Un glissé règle en coulisses : les pistes qui jouent (`SongPart.muted`), la densité de la batterie (variante générée), l'ouverture du filtre et le volume (courbes « par partie »). Affichage : la hauteur du bloc de la partie. **1 geste** au lieu d'un mur d'automation (A1 P2).
3. **Menu de partie « Transformer »** (chaque action annulable en une étape) :
   - **Variation** (Commit EP-133) : copie + petite mutation (charley, contre-temps, notes de passage), et on bascule dessus ;
   - **Break** : copie sans kick ni basse (A3 #1, top 1) ;
   - **Fill à la fin** : dernière mesure de batterie remplacée par un fill (roulement de caisse claire en crescendo, tom, coupure), sans nouvelle scène visible (P1 §1) ;
   - **Montée avant** : sur les 2 ou 4 dernières mesures de la partie précédente : filtre qui s'ouvre, souffle montant, roulement qui accélère (A3 #5-6) ;
   - **Fin** (Ending Yamaha) : partie « Fin » générée depuis l'Intro, sans kick ni basse, impact + crash, fondu de 4 mesures dans le morceau (pas seulement à l'export), puis arrêt (A1 P7).
4. **« Fills automatiques »** (réglage du morceau : aucun / légers / marqués), comme « Auto Fill » : un fill d'une mesure avant chaque changement de partie.
5. **« Allonger mon morceau »** (Étendre d'Udio, sans IA générative) : « + avant » / « + après » propose la partie logique suivante (Couplet → Refrain → Pont → Refrain → Fin) fabriquée par la recette du style avec les mêmes sons. Plus **« Plan du morceau »** : Court (1 min 30) / Radio (3 min) / Club (5 min), qui bâtit Intro 8 / Couplet 16 / Refrain 8… à partir de la scène en cours (A2 #6).
6. **Ligne du temps** : longueur d'une partie en mesures, au bord du bloc ou en tapant le nombre (P1 §2) ; batterie dépliable pad par pad (A3 #1) ; noms des parties au-dessus des courbes d'automation (A3 #5).

---

## 3. Enregistrer : mélodie, voix, prestation, prises

### Ce que font les meilleurs
- **GarageBand** : décompte et métronome par morceau (Réglages › Métronome et décompte, avec un « décompte visuel ») ; bouton métronome toujours dans la barre ; ● enregistre à la position de la tête de lecture. ([guide Apple](https://support.apple.com/en-lb/guide/garageband-iphone/chs2a762fad/ios))
- **GarageBand, prises multiples** : une nouvelle prise chaque fois que la tête repart au début de la section ; choisir une prise = toucher deux fois la région › Prises › la prise (**4 gestes**), mais l'activer coûte **5 gestes** dans les réglages de piste : trop caché. ([Apple](https://support.apple.com/en-ca/101934))
- **GarageBand, quantification non destructive** : par piste et par section, « Droit / Triolet / Swing » ; « Aucune » rend le jeu d'origine. ([guide Apple](https://support.apple.com/guide/garageband-iphone/chs175dc411/ios), [Macworld](https://www.macworld.com/article/668236/how-to-edit-in-garageband-for-ipad-iphone.html))
- **Ableton Note, Capture** : on joue sans appuyer sur ●, puis **Capture garde le jeu**, devine tempo et longueur, et boucle ; les notes jouées apparaissent en **notes fantômes** : on valide ou on jette (« rien n'est gardé sans ton accord »). ([Ableton](https://www.ableton.com/en/note/), [Sound On Sound](https://www.soundonsound.com/reviews/ableton-note))
- **Logic Pro, Flashback Capture (mai 2025)** : récupère une prestation MIDI *ou audio* jouée sans avoir appuyé sur ● ; en boucle, chaque passage devient une prise rangée dans un dossier de prises. **Quick Swipe Comping** : on glisse le doigt sur la meilleure prise, morceau par morceau. ([TechRadar](https://www.techradar.com/computing/websites-apps/apples-logic-pro-for-ipad-and-mac-can-now-capture-your-performances-even-when-you-dont-hit-record), [AlternativeTo](https://alternativeto.net/news/2025/5/logic-pro-brings-enhanced-stem-splitter-flashback-capture-and-new-sound-packs-to-mac-and-ipad), [guide Apple, comping](https://support.apple.com/guide/logicpro-ipad/lpipc9ddd617/ipados))
- **FL Studio Mobile** : décompte d'une mesure, pas de quantification à l'entrée, on quantifie après dans le piano roll. ([forum Image-Line](https://forum.image-line.com/viewtopic.php?p=1782114), [forum](https://forum.image-line.com/viewtopic.php?p=1255750))
- **BandLab** : enregistrer en boucle range les prises dans la région (le montage de prises est réservé aux abonnés) ; **Voice Cleaner** en un geste (bruit, réverbe de la pièce, égalisation auto) ; **AutoPitch** réglé sur la gamme du morceau, intensité en 11 crans. ([MusicTech](https://musictech.com/features/how-to-remove-background-noise-from-your-vocals-within-seconds-using-bandlabs-voice-cleaner), [App Store BandLab](https://apps.apple.com/app/bandlab-music-making-studio/id968585775), [aide BandLab](https://help.bandlab.com/hc/en-us/articles/48566287349017-Understanding-Composite-Recording))
- **Prestation live** : GarageBand Live Loops écrit chaque rangée dans sa piste ; les cellules déjà en cours sont prises depuis le début de la section ; les gestes Remix FX deviennent une région violette sur une piste FX (min. 4 gestes). Endlesss : une forme d'onde défile, **toucher garde la boucle** (« riff »), et chaque riff reste dans un journal, exportable en stems ou en vidéo. ([guide Apple, Live Loops](https://support.apple.com/guide/garageband-iphone/chs02aae0e6b/ios), [guide Apple, Remix FX](https://support.apple.com/guide/garageband-iphone/chsabb64c325/ios), [Engadget](https://engadget.com/2020-03-31-endless-music-collaboration-app.html), [CDM](https://cdm.link/2024/05/endlesss-discontinued/))
- **Zenbeats 3.1.12** : enregistrement continu d'une cellule de boucle à la suivante. ([Roland Cloud](https://www.rolandcloud.com/news/zenbeats-3-1-12-now-available))

### Ce qui coince chez nous
Petit rond rouge sans mot (A4 #1) ; décompte et métronome cachés dans Rythme, boucle d'une mesure par défaut (A4 #2) ; « Quantifier 50 % » incompréhensible (A4 #3) ; voix sans repère de mesure, une seule « prise précédente », pas de punch-in, pas de recalage au tempo (A2 #4-5) ; pads et notes non enregistrés en live, « Remplacer le morceau » par défaut (A5 #1-3, #5).

### Proposition pour Pocket Studio
1. **Bouton « ● Enregistrer » dans chaque éditeur qui se joue** (clavier, pads, micro), ≥ 56 dp, avec un mot. Il ouvre une feuille courte : partie visée (« Couplet, 16 mesures »), décompte **1 mesure + métronome activés par défaut**, grand « 4 · 3 · 2 · 1 » avec vibration à chaque temps, puis compteur « Couplet · mes. 5/16 ».
2. **Après la prise** (notes fantômes d'Ableton Note) : « **Garder** · **Remettre en rythme** (doux / complet) · **Refaire** · ▶ Écouter ». « Remettre en rythme » est non destructif (comme GarageBand) : un curseur 0-100 % qu'on peut ramener à zéro plus tard. « Humaniser » et la grille passent dans « Avancé ».
3. **« Garder ce que je viens de jouer »** (Capture / Flashback) : le clavier et les pads mémorisent en permanence les 8 dernières mesures jouées (événements horodatés par la position du moteur, côté Kotlin, aucune allocation dans le callback) ; un bouton les pose dans la partie en cours. Pour un débutant comme pour le performeur (A5 #2), on ne rate plus une bonne idée.
4. **Prises en boucle** : ● sur une partie bouclée = prise 1, 2, 3… ; on choisit une prise d'un glissé, puis mesure par mesure (« prise 2 pour mes. 5-8 ») : un Quick Swipe Comping simplifié.
5. **Punch-in « Refaire mes. 5-8 »** : sélectionner des mesures sur la forme d'onde, ● part une mesure avant et n'écrit que la zone (A2 #4).
6. **Voix** : bouton « Mesurer la latence » dans l'écran micro avant la 1re prise ; « Nettoyer ma voix » (porte de bruit + égaliseur + compresseur réglés pour la voix, un geste) ; « Recaler sur le tempo » quand le tempo change (Signalsmith Stretch déjà dans le projet) (A2 #5).
7. **Prise live** : pads plein écran et notes enregistrés ; par défaut « **Nouvelle version** » (copie), jamais « Remplacer » sans confirmation ; chaque prise reste dans un journal de versions (Endlesss) (A5 #1, #5).

---

## 4. Les aides créatives en un geste

### Ce que font les meilleurs
- **Accords sur tout le morceau** : dans Logic Pro, les Session Players basse et clavier suivent la **piste d'accords** globale ; sans accord, ils jouent la tonique. Hookpad propose l'accord suivant avec un score de popularité de 1 à 100. Scaler 3 synchronise les couloirs mélodie, basse et phrases sur une piste d'accords. GarageBand : bandes d'accords + « Autoplay » qui joue un accompagnement en touchant un accord. ([guide Apple, Session Players](https://support.apple.com/guide/logicpro-ipad/lpip7b39bb07/ipados), [Hookpad](https://www.hooktheory.com/hookpad), [Scaler 3](https://www.pluginboutique.com/products/14563-Scaler-3), [guide Apple, bandes d'accords](https://support.apple.com/guide/garageband-iphone/chs39282dbe/ios))
- **« Plus fort / plus chargé »** : pavé XY de Drummer, Intensité de Logic, Main A→D de Yamaha (voir §2) ; préréglage « Fire » du mastering BandLab (basses plus percutantes).
- **Humain, groove, variations** : Maschine **Variation** (Humaniser : décale les notes comme jouées à la main ; Aléatoire : nouveaux motifs) ; Ableton Live 12, outils MIDI (Ornement = flas et notes d'agrément pour la batterie, Strum, Rythme, Seed, Shape, Stacks pour les accords). ([Synth Anatomy](https://synthanatomy.com/2017/02/native-instruments-releases-maschine.html), [CDM](https://cdm.link/2023/11/ableton-live-12-everything-new/), [Sound On Sound](https://www.soundonsound.com/techniques/ableton-live-12-midi-transformations?amp))
- **Fills automatiques** : curseur Fills de Drummer, « Quantité de fills » de Logic, Auto Fill de Yamaha, condition FILL d'Elektron (voir §2).
- **Mix automatique** : BandLab **AutoMix** (abonnés) : genre + phrase libre facultative (500 caractères, « garde le kick et la basse nets, voix devant »), règle volume et panoramique ; Voloco **Auto Mix** : **1 geste « Appliquer »**, Annuler en 1 geste, puis Auto Master ; iZotope Mix Assistant : un équilibre statique de départ. ([Jack Righteous](https://jackrighteous.com/en-gb/blogs/bandlab-guides/bandlab-automix-automation-mixing-workflow), [Voloco](https://voloco.com/auto-mix.html), [iZotope](https://support.izotope.com/hc/en-us/articles/6658264913553-Neutron-3-Mix-Assistant-FAQ))
- **Mastering automatique** :
  - BandLab, gratuit : 4 ambiances (Universal, Fire, Clarity, Tape), gain auto, Appliquer (**env. 4 gestes**) ; abonnés : 4 de plus et une intensité en 11 crans (mise à jour de septembre 2026) ([aide BandLab](https://help.bandlab.com/hc/en-us/articles/360001374513-How-do-I-master-my-music-on-BandLab), [Magnetic](https://magneticmag.com/2026/09/bandlab-mastering-update-gives-artists-more-control-before-release/), [MusicTech](https://musictech.com/news/music/bandlabs-mastering-capabilities-just-got-an-upgrade-heres-whats-new/)) ;
  - Logic **Mastering Assistant** : analyse, puis égalisation corrective, volume et largeur ; caractères Clean, Valve, Punch, Transparent ([Apple](https://support.apple.com/en-gb/108294)) ;
  - FL Cloud : préréglages par genre, comparaison avec l'original ([Image-Line](https://www.image-line.com/fl-cloud/mastering)).
- **Démarreurs d'idées** :
  - BandLab **SongStarter** : un genre, ou des paroles/émojis (50 caractères), 3 idées à la fois, « Régénérer », ouvrir dans le Studio ([aide BandLab](https://help.bandlab.com/hc/en-us/articles/24226593087001-What-is-SongStarter), [App Store](https://apps.apple.com/app/bandlab-music-making-studio/id968585775)) ;
  - FL **Loop Starter** : un genre, « relancer », garder les boucles qui plaisent et changer les autres ([Image-Line](https://www.image-line.com/fl-cloud/loop-starter)) ;
  - Splice **Create** : icône Stacks + un genre (**2 gestes**) donnent jusqu'à 8 boucles compatibles ; glisser à droite = changer la couche, à gauche = la retirer, toucher = muet ([Splice](https://support.splice.com/en/articles/8871089-how-to-use-create-stacks-in-the-splice-mobile-app), [MusicRadar](https://www.musicradar.com/news/splice-create-stacks)) ;
  - Soundful : genre, modèle, tempo et tonalité, export en pistes séparées ([vidmuse](https://vidmuse.ai/blog/soundful-ai-review)).

### Ce qui coince chez nous
Accords qui ne touchent qu'une scène et une piste, tonique qui ne transpose rien, chiffres romains (A3 #2) ; pas de « plus costaud » (A1 P2) ; pas de « rééquilibrer » après coup (P2 §11) ; compresseur qui ajoute ~5 dB de gain fixe, limiteur figé, pas de LUFS en direct (P2 §1-4, AUDIT 5) ; roulements sans crescendo (A2 #2).

### Proposition pour Pocket Studio
1. **« Accords du morceau »** : une rangée d'accords en haut de l'écran Morceau, avec les noms (Lam Fa Do Sol) ; toucher une partie › choisir une progression (▶ écoute avant) › **basse, nappe, arpège et accords de toutes les parties suivent** (regénérés depuis les accords) ; la mélodie est transposée. « Suggérer l'accord suivant » d'après les recettes de style (à la Hookpad). Changer la tonique transpose tout.
2. **« Énergie » / « Plus fort » par partie** (voir §2, idée 2).
3. **« Groove »** : puces Droit / Swing léger / Swing fort / Triolet + curseur « Jeu humain » (décalage et force), non destructifs, par piste et par partie.
4. **« Une autre idée »** (dé) sur chaque piste : regénère batterie, basse ou mélodie depuis la recette du style ; cadenas pour garder une couche (Loop Starter, Splice). Déjà en partie dans l'assistant : le rendre disponible partout.
5. **« Équilibrer mon mix »** : un geste, Annuler en un geste, qui rejoue `WizardMix` sur le projet actuel ; plus des puces d'intention sans IA : « Voix devant », « Plus de basse », « Plus brillant », « Kick plus présent ».
6. **« Finition »** (mastering) : 4 ambiances (Équilibré, Punch, Clair, Chaud) + Intensité 0-10 + A/B + cible « Streaming −14 » / « Fort −9 », LUFS affiché en direct. **Préalable** : corriger le gain fixe du compresseur (Effects.cpp:176) et le limiteur (P2 §2, §4), sinon les ambiances poussent dans le mur.
7. **Roulements** : crescendo et hauteur qui monte, ×6/×8, 1/32 (A2 #2), utilisés aussi par « Fill à la fin ».

---

## 5. Finir et partager

### Ce que font les meilleurs
- **GarageBand** : appui long sur le morceau › Partager › Morceau › qualité › artiste, compositeur, titre › image › Partager (**env. 6 gestes**) ; aussi « Sonnerie » et « Projet ». Le WAV part à 48 kHz par défaut depuis la 2.3.18. ([guide Apple](https://support.apple.com/guide/garageband-iphone/chs39284d66/ios), [notes de version](https://support.apple.com/en-us/HT212740))
- **FL Studio Mobile** : WAV, FLAC, MP3, AAC, MIDI, ZIP ; normaliser, couper le silence du début, rendre jusqu'à la dernière mesure, pistes séparées ; destination d'envoi mémorisée. ([manuel](https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/plugins/FL%20Studio%20Mobile_HomePanel.htm))
- **OP-1, mode Album** : on « grave » le morceau des 4 pistes vers une face A ou B de 6 minutes, avec les effets du master imprimés. Le geste final est un moment à part entière. ([guide teenage engineering](https://teenage.engineering/guides/op-1/original/song-rendering-and-connectivity))
- **Vidéo** : Medly partage « en vidéo, audio, MIDI ou fichier Medly » ; Endlesss exportait chaque riff en stems ou en vidéo ; le format vertical 9:16 (1080 × 1920, extraits de 15, 30 ou 60 s) est la norme TikTok / Reels. ([App Store Medly](https://apps.apple.com/app/medly/id940268124), [CDM](https://cdm.link/2024/05/endlesss-discontinued/), [Storrito](https://storrito.com/help-center/music-story-video-maker/))
- **BandLab** : pochette par projet, publication sur le fil, partage vers les réseaux et les plateformes. ([App Store](https://apps.apple.com/app/bandlab-music-making-studio/id968585775), [aide BandLab, album](https://help.bandlab.com/hc/en-us/articles/900000198546-How-do-I-publish-an-album-on-BandLab))

### Ce qui coince chez nous
Pas d'écran « Ton morceau est prêt » (AUDIT 2, P3 c11) ; Partager dans le Mixeur (A1 P8, A4 #4) ; aucune vérification avant l'envoi : solo oublié, limiteur qui force, morceau sans fin (P3 c10) ; WAV en 6 gestes avec défilement (A3 #9) ; nom demandé seulement à l'envoi (A4 #12).

### Proposition pour Pocket Studio
1. **⤴ Partager dans la barre du haut, partout.** Il ouvre la feuille **« Prêt à partager ? »** : durée, parties, et une liste qui se vérifie toute seule, avec un bouton « Réparer » d'un geste par ligne : morceau sans fin (« Ajouter une fin »), solo actif, limiteur qui force (« Baisser de 3 dB »), silence au début, volume trop faible (« Finition »).
2. **Formats en gros boutons** : « Envoyer » (M4A, fondu 4 s), « Vidéo » (9:16, 30 s du refrain), « Qualité studio » (WAV), « Sonnerie » (30 s) ; le dernier choix est retenu ; tout le reste sous « Pour les experts ».
3. **Écran « Ton morceau est prêt »** (fin de l'assistant, et quand la liste est verte) : nom, pochette générée (couleur du style + nom), « 3 min 05 · 6 parties », ▶ Écouter en entier, Partager, Continuer à modifier.
4. **Nom demandé à la fin de l'assistant** (« Comment s'appelle ton morceau ? ») et métadonnées dans les fichiers (titre, artiste, tempo).
5. **Vidéo** : forme d'onde ou pads qui s'allument, noms des parties qui défilent, couleur de la pochette ; rendu hors ligne avec MediaCodec / MediaMuxer (l'encodeur AAC existe déjà).
6. **Versions** (face A / face B, journal Endlesss) : « Graver la version 1 » garde une copie figée et partageable, et on continue sur la v2.

---

## 6. Apprendre

### Ce que font les meilleurs
- **Groovepad, Beat School** : une leçon par pack de sons, pour apprendre à jouer les sons avant d'improviser seul ; l'apprentissage est attaché au contenu qu'on vient de choisir. ([App Store](https://apps.apple.com/app/id1454398991))
- **Ableton Learning Music** : cours gratuit dans le navigateur, en français ; chapitres rythme → notes et gammes → accords → basse → mélodie → **structure du morceau**, chaque notion illustrée par un morceau connu et un petit éditeur pour essayer, plus un « Playground ». ([Ableton](https://ableton.com/en/blog/learn-music-in-your-browser), [Engadget](https://www.engadget.com/2017-05-08-ableton-interactive-lessons.html))
- **Ableton Live** : vue **Info** (le nom et le rôle de ce qu'on survole) et vue **Learn** (leçons guidées dans le logiciel). ([manuel Live 12](https://www.ableton.com/en/live-manual/12/first-steps/))
- **GarageBand iPhone** : un « ? » qui pose des étiquettes sur les commandes, selon un utilisateur ; Apple renvoie aussi vers « Afficher l'aide ». ([Apple Communities](https://discussions.apple.com/thread/255073689))
- **FL Studio Mobile** : Aide › manuel, tutoriels vidéo, vidéo d'introduction. **Zenbeats** : leçons, modèles de morceaux, série vidéo « How-To ». ([manuel FL](https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/plugins/FL%20Studio%20Mobile_HomePanel.htm), [Roland](https://www.roland.com/fr/products/rc_zenbeats/))
- **Bonnes pratiques d'accueil** : montrer une bulle au premier passage sur un écran, ne jamais bloquer l'accès derrière un tutoriel, apprendre juste avant le moment où ça sert. ([AppStorys](https://appstorys.com/blog-Mobile-App-Onboarding-Best-Practices), [UX Planet](https://uxplanet.org/mobile-onboarding-interact-don-t-tell-f0c35da2b2b4))

### Ce qui coince chez nous
Guide en pages qu'on fait défiler sans rien retenir (A1, A3) ; jargon (Drop, Montée, 303, Coupure, Quantifier) (AUDIT 8, A1 P9, A4 #6) ; la carte « Prochaine étape » ne vit que dans Rythme et ne dit jamais « terminé » (P3 c3, A4 #9) ; l'aide est le 1er sous-onglet du 7e onglet (P3 c7).

### Proposition pour Pocket Studio
1. **« Mon premier morceau » en missions** : une barre fine sur tous les écrans, « 3/7 · Rends le refrain plus fort → ». Missions : Écouter · Changer un son · Jouer ma mélodie (● avec décompte) · Refrain plus fort (Énergie) · Ajouter une fin · Équilibrer · Partager. Chaque mission ouvre le bon écran et entoure le bon bouton ; à la fin : « Ton morceau est prêt ».
2. **« ? » dans la barre du haut** = étiquettes posées sur les commandes de l'écran (GarageBand) ; **appui long sur un bouton** = bulle d'une phrase (vue Info d'Ableton, adaptée au toucher).
3. **Glossaire parlant** : les mots soulignés en pointillé (Drop, Break, Montée, Sidechain) ouvrent une phrase + **▶ un exemple sonore de 4 mesures** (méthode Ableton Learning Music : entendre la notion).
4. **Leçons de 2 minutes par style** (Beat School) : « Le rythme trap », « Un drop qui tape », « Faire une fin », lancées depuis le projet ouvert.
5. **Une bulle par écran, au premier passage seulement**, jamais bloquante ; « Revoir les bulles » dans Plus.

---

## 7. Design et impression de « vivant »

### Ce que font les meilleurs
- **OP-1** : des animations joueuses au lieu de chiffres techniques ; les dessins « bougent avec le son » à 60 images/s ; l'écran fait partie de l'instrument. ([MusicRadar](https://www.musicradar.com/reviews/teenage-engineering-op-1), [CDM](https://cdm.link/teenage-engineerings-op-1-instrument-hands-on-videos-why-its-different/), [Sonic State](https://sonicstate.com/news/2011/08/18/sonic-lab-teenage-engineering-op-1-review/))
- **Ableton Note** : « amusante et facile à prendre en main » ; n'affiche que les couloirs qui contiennent des notes. ([Sound On Sound](https://www.soundonsound.com/reviews/ableton-note))
- **Koala** : pas la plus belle, mais logique, simple, rapide ; le plaisir vient du flux. ([Sound On Sound](https://www.soundonsound.com/reviews/elf-audio-koala-sampler))
- **Logic / GarageBand Live Loops** : une cellule en attente **clignote** jusqu'à son départ ; FX Lock ; pavés XY qu'on peut piloter en inclinant le téléphone. ([guide Logic Remote](https://support.apple.com/guide/logicremote-logicpro-iphone/chs6fd8984b8/ios), [guide Apple, Remix FX](https://support.apple.com/guide/garageband-iphone/chsabb64c325/ios))
- **Endlesss** : une forme d'onde qui défile en permanence. **FL Studio Mobile 5** : nouveau look, mise à l'échelle en direct, option animations réduites. ([Engadget](https://engadget.com/2020-03-31-endless-music-collaboration-app.html), [Image-Line](https://forum.image-line.com/viewtopic.php?f=1964&t=260871))

### Proposition pour Pocket Studio (dans les règles de `DESIGN.md`)
1. **La forme du morceau** : sur l'écran Morceau, chaque partie est un bloc coloré dont la **hauteur = l'énergie** ; on voit d'un coup d'œil la courbe Intro → Drop → Fin, et la tête de lecture la parcourt.
2. **Ce qui joue respire avec le tempo** : halo au temps sur ▶, sur la partie en cours et sur les pistes qui sonnent ; les pads et les vignettes de piste réagissent au niveau (vumètre intégré à la vignette).
3. **Clignotement « en attente »** pour tout ce qui part à la prochaine mesure (déjà dans Live : l'étendre à « Jouer cette partie ensuite »).
4. **Une couleur par instrument**, la même dans Sons, Morceau, Mix et Live.
5. **Écouter partout** : toucher un son, une progression, une ambiance de finition = l'entendre tout de suite.
6. **Une seule célébration** : l'écran « Ton morceau est prêt » (animation courte, son discret) ; ailleurs, des animations de 150 à 250 ms qui servent à quelque chose.
7. **Option « animations réduites »** et vibrations au décompte.

---

## 8. Combien de gestes : ailleurs, chez nous, la cible

| Tâche | Ailleurs | Pocket Studio aujourd'hui (audit) | Cible proposée |
|---|---|---|---|
| Écouter tout le morceau | GarageBand 4 gestes (Toutes les sections) | ▶ joue la section ; aller dans Morceau + bouton en bas (A1 P1) | 1 |
| Ajouter une partie | GarageBand 3, dupliquer 4 | Nouvelle + Insérer après, 2 étapes (A3 #11) | 2 (« + Partie » › type) |
| Partie plus forte | Drummer : 1 glissé XY | impossible sans automation, ≈ 8 gestes (A1 P2) | 1 glissé « Énergie » |
| Fill avant le drop | Drummer : 1 glissé « Fills » | 6 gestes ou plus + une scène (P1 §1) | 2 (⋮ › Fill à la fin) |
| Fin du morceau | Yamaha : 1 bouton Ending | ≈ 6 gestes, fondu seulement à l'export (A1 P7) | 2 (⋮ › Fin) |
| Choisir une prise | GarageBand 4 (+5 pour activer) | une seule « prise précédente » (A2 #4) | 1 glissé |
| Mixer automatiquement | Voloco 1 + Annuler 1 | aucun (P2 §11) | 1 + Annuler 1 |
| Mastering | BandLab ≈ 4 | aucun, limiteur figé (P2 §4) | 2 (Finition › ambiance) |
| Partager | GarageBand ≈ 6 | Mixeur › bouton (caché) ; WAV 6 + défilement (A3 #9) | 2 (⤴ › format) |
| Démarrer une idée | Splice Create 2 | assistant 13 étapes ou « Créer tout de suite » (A1 P3) | 3 (style › ambiance › créer) |

---

## 9. Les 20 meilleures idées à reprendre

Classées par impact sur « finir un morceau complet » ; à impact égal, la moins chère d'abord. Effort : **S** = quelques jours, **M** = une à deux semaines, **L** = une phase entière.

| # | Idée | Vient de | Règle (AUDIT-33) | Effort |
|---|---|---|---|---|
| 1 | **▶ joue tout le morceau par défaut**, bouton à deux états écrits (« ▶ Morceau » / « ▶ Cette partie ») et position « mes. 14/60 » dans la barre du haut | piège GarageBand à éviter | AUDIT 2, A1 P1, A4 #5, P3 c9 | S |
| 2 | **⤴ Partager dans la barre du haut** + feuille « Prêt à partager ? » qui vérifie (fin, solo, limiteur, silence) et répare en un geste | GarageBand, OP-1 Album, bonnes pratiques | AUDIT 2, A1 P8, A4 #4, P3 c10 | S |
| 3 | **Écran « Ton morceau est prêt »** + barre « chemin du morceau » sur tous les écrans, qui finit par « terminé » | BandLab, missions guidées | AUDIT 2, P3 c3/c11, A4 #9 | M |
| 4 | **« Ajouter une fin » en un geste** : partie Fin sans kick ni basse, impact, fondu dans le morceau, arrêt | Yamaha Ending | A1 P7, AUDIT 2 | M |
| 5 | **Curseur « Énergie » par partie** (1 à 4 : pistes jouées, densité, filtre, volume) | Drummer XY, Yamaha Main A→D, Session Players | A1 P2, AUDIT 3 | M |
| 6 | **Menu « Transformer » d'une partie** : Variation, Break, Fill à la fin, Montée avant | EP-133 Commit, Elektron FILL, Drummer Fills | AUDIT 3, P1 §1/§8, A3 #1/#5 | M |
| 7 | **Un seul mot « Partie »**, écran Morceau = parties + grand ▶ + « + Partie » ; lien visible + « Rendre unique » ; Dupliquer = copie indépendante | GarageBand, Maschine, FL « Rendre unique » | AUDIT 1, A3 #4, P1 §10, P3 c5 | M |
| 8 | **● Enregistrer dans chaque éditeur** : décompte 1 mesure + métronome par défaut, compteur « Couplet · mes. 5/16 » | GarageBand (décompte visuel) | AUDIT 4, A4 #1-2, A2 #4 | S |
| 9 | **Après la prise : Garder / Remettre en rythme / Refaire**, quantification non destructive | Ableton Note (fantômes), GarageBand | A4 #3 | S |
| 10 | **4 espaces : Morceau, Sons, Mix, Plus** ; Live hors du flux ; mode simple = outils avancés cachés | Koala (étapes), Logic « Outils avancés » | AUDIT 1, A4 #7, P3 c1/c2/c7 | L |
| 11 | **« Changer le son » depuis le nom d'une piste** + **« Changer tout le kit »** + kits par style (trap, house…) | GarageBand Sons, Splice Create | A1 P5, A2 #3, A3 #3/#8 | S |
| 12 | **« Équilibrer mon mix »** un geste + Annuler, puces « Voix devant », « Plus de basse » | Voloco Auto Mix, BandLab AutoMix | AUDIT 5, P2 §11 | S |
| 13 | **« Accords du morceau »** : rangée d'accords nommés, toutes les parties et pistes suivent, écoute avant | Logic piste d'accords + Session Players, Hookpad | A3 #2 | L |
| 14 | **« Finition »** : 4 ambiances + intensité + A/B + cible LUFS en direct (après correction du compresseur et du limiteur) | BandLab Mastering, Logic Mastering Assistant | AUDIT 5, P2 §1/§2/§4 | M |
| 15 | **« Allonger mon morceau »** (+ avant / + après) et **« Plan du morceau »** Court / Radio / Club | Udio Étendre, sections GarageBand | A2 #6, A1 | M |
| 16 | **« Garder ce que je viens de jouer »** (mémoire des 8 dernières mesures jouées) | Ableton Note Capture, Logic Flashback | A4, A5 #2 | M |
| 17 | **Prise live = nouvelle version**, pads et notes enregistrés, journal des versions | Endlesss, GarageBand Live Loops → Pistes | A5 #1/#3/#5 | M |
| 18 | **Prises en boucle + punch-in « Refaire mes. 5-8 »** + choix d'un glissé ; voix : latence, « Nettoyer ma voix », « Recaler au tempo » | GarageBand multi-prises, Logic Quick Swipe, BandLab Voice Cleaner | AUDIT 4, A2 #4/#5 | L |
| 19 | **Missions « Mon premier morceau »** + « ? » qui étiquette l'écran + glossaire avec exemple sonore | Groovepad Beat School, Ableton Learning Music, GarageBand « ? » | AUDIT 8, A1 P9, P3 c14 | M |
| 20 | **Export vidéo 9:16** (30 s du refrain, forme d'onde, noms des parties, pochette) | Medly, Endlesss, formats TikTok/Reels | A2 (partage social), P3 c10 | L (M en image fixe + forme d'onde) |

Idées utiles mais hors du top 20 : forme du morceau en blocs dont la hauteur = l'énergie (§7, S une fois l'idée 5 faite) ; « Une autre idée » (dé) sur chaque piste (§4, S) ; puces Groove / Jeu humain (§4, S) ; roulements en crescendo, ×8 et 1/32 (A2 #2, M) ; batterie dépliable pad par pad dans la ligne du temps (A3 #1, M) ; longueur de partie au bord du bloc (P1 §2, S).

Ordre conseillé : 1, 2, 8, 9, 11, 12 (petits et immédiats) ; puis 4, 5, 6, 7 et 3 (le cœur « finir ») ; puis 10 (la nouvelle navigation, qui range tout le reste) ; puis 14, 15, 16, 17 ; enfin 13, 18, 19, 20.
