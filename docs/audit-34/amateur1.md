# Testeur Amateur 1 (débutant total, téléphone) : vague 2, après la phase 33

Même profil, même mission que le 2026-10-23 (vague 1) : faire un morceau techno complet d'environ 3 minutes, le rendre « à moi » (changer des sons et la mélodie, un drop qui tape plus fort, une vraie fin), puis l'envoyer à un ami.

Méthode : lecture du code et des captures de `/home/user/ps-p22` (branche `ccr-80af1a1b-mn0wec`, phase 33 fusionnée). Rien n'a été lancé. Chemins abrégés : `…/` = `app/src/main/java/com/pocketstudio/app/`. J'ai refait tout le parcours seul, puis seulement ensuite relu mon rapport de la vague 1.

Résumé en une phrase : **écouter et partager sont devenus faciles (c'est le gros progrès de la phase 33), mais « faire mien » un morceau (changer, renforcer le drop, finir) est exactement aussi dur qu'avant, et deux nouveautés créent de la confusion.**

---

## Parcours réel

Nombre de touchers (« tap ») compté à partir des composables ; « défil. » = un défilement.

### 1. Accueil → morceau créé : 5 taps par le raccourci, ~16 en suivant tout

1. **Accueil** (welcome.png, `…/onboarding/Welcome.kt:103-120`). Trois gros choix, le guidé en premier. Clair. Tap 1 : « Créer mon premier morceau (guidé) ».
2. **Assistant, « Étape 1 sur 13 »** (wizard_style.png, `…/library/WizardScreen.kt:160`). Texte : « Choisis un style : je prépare la batterie, puis les **sections** du morceau ». Cinq familles repliées. Tap 2 : « Techno » (ça déplie 10 styles : Techno, Techno de Detroit, Acid techno, Hard techno, Industrielle, Rétro, Acidcore, Trance, Minimal, Gabber). Je ne sais toujours pas ce qui les sépare ; je prends « Techno ». Tap 3 : la carte (elle s'ouvre). Tap 4 : « Choisir ce style ».
3. **Étape 2 « Ambiance »** (wizard_mood.png, `WizardScreen.kt:747`). « Je choisis la gamme et les accords… impossible de jouer une fausse note, pas besoin de solfège. » Ça, je comprends. Mais chaque carte dit « Gamme La dorienne · accords i – i – IV – IV » : je n'y comprends rien et ça me fait peur. En bas, le petit bouton-texte orange « Créer tout de suite (choix conseillés) » (`WizardScreen.kt:200`). Tap 5 : je le prends.
   - Si je continue au lieu de ça : étapes 3 à 9 = une par ligne de batterie (« Kick (grosse caisse) » est bien expliqué ; « Charley ouvert », « Ride », « Percussion 2 » non), étape 10 « Ligne acide (TB-303) » (`WizardScreen.kt:498` : « couine et gronde », grille de pas, « accent », « slide ⌒ », « Densité », « Coupure », « Résonance » : je ne touche à rien), étapes 11-12 « Basse », « Nappe / accords », « Lead / mélodie » (wizard_lead.png : un interrupteur « Créer cette piste », éteint par défaut pour le Lead… donc mon morceau techno n'aura pas de « mélodie » au sens où je l'entends), étape 13 résumé (wizard_summary.png : « QUATRE SECTIONS », « Le morceau : Intro → Groove → Drop → Break → Drop → Intro pour finir : environ 2 min 50 »). Soit 11 « Suivant » + « Créer le morceau » ≈ 16 taps.
4. **Préparation** puis l'écran se reconstruit, et j'arrive sur l'onglet Morceau avec la carte **« Ton morceau est prêt ! »** (song_ready.png, `…/onboarding/SongReady.kt:106-150`) : « House du soir · 2:40 · 6 parties », quatre gros boutons Écouter / Changer un son / Modifier le morceau / Partager, « Lire le guide de ton morceau », « Plus tard ». C'est **le meilleur nouvel écran** : enfin on me dit quoi faire ensuite.
   - Tap 6 : « Écouter ». Le morceau joue en entier, la carte reste. Sous la barre du haut je vois « 124 BPM · Intro mes. 1/60 ▾ · ⟳ Partie » (play_strip.png) : je comprends que ça avance. Bien.
   - Petit piège : le morceau ne joue pas tout seul à l'arrivée ; et si je touche à côté de la carte (en dehors), elle disparaît **pour toujours** pour ce projet (`SongReady.kt:92` `Dialog(onDismissRequest = done.close)` + `markSeen`). Je l'ai fait par réflexe en voulant voir ce qu'il y avait derrière : plus de carte, plus de « Changer un son ».

### 2. Rendre le morceau « mien » : là, ça coince

5. **Changer un son.** Tap « Changer un son » sur la carte → onglet Synthé, piste principale choisie (`…/ui/AppRoot.kt:850-854`, `mainTrackOf`). Je tombe sur un clavier géant (synth.png) ; le choix des sons est **sous** le clavier, après « Le son de la piste (le même pour toutes ses notes) » (`…/synth/SynthScreen.kt:314`) : 1 défil., puis « SONS PRÊTS À JOUER » (`:420`, tuiles Favoris / Basses / Leads / Nappes…, sound_browser.png). Tap famille, tap tuile : **3 taps + 1 défil.** depuis la carte, ça va. Mais : (a) quelle piste je modifie ? C'est écrit en petit dans la barre de pistes ; (b) sans la carte « prêt » (voir le piège ci-dessus), c'est Synthé › barre de pistes › défil. › famille › tuile = 4 taps + défil., et je dois deviner que « la nappe » est dans Synthé, « la basse qui couine » dans Acide, les tambours dans Rythme ; (c) **changer les sons de la batterie** reste pad par pad : Rythme › pad › défil. › « Changer le son » (`…/rhythm/RhythmScreen.kt:680`, pad_sound.png) › famille › « Poser » (library.png) = **5 taps par pad**, et il n'y a toujours pas de « Kit ▾ ».
6. **Changer la mélodie.** Synthé › puce « Notes » (`SynthScreen.kt:473`, piano_roll.png) : « ✏ Dessiner / Sélectionner / Zoom / Boucle 1 mes. 2 mes. 4 mes. 8 mes. / Quantifier 50 % / 100 % / Basse en fantôme » (`…/synth/PianoRoll.kt:150-199`). Le mode d'emploi est écrit en bas, je peux poser des notes. **Mais voilà le nouveau piège** : j'appuie ▶ pour entendre ce que je fais, ▶ joue maintenant tout le morceau (c'est voulu), et comme « Pendant la lecture : suivre le morceau » est activé par défaut (`…/rhythm/RhythmViewModel.kt:1638`, `:2041` `switchScene`), **la grille sous mon doigt change toute seule** toutes les 2 à 4 mesures : Intro, puis Groove, puis Drop. La puce « C ▾ » en haut change de lettre, je ne comprends pas pourquoi mes notes « disparaissent » et réapparaissent. Pour que ça tienne il faut : « ⟳ Partie » (1 tap) + puce « C ▾ » (1 tap) + « C · Drop » (1 tap) = 3 taps que rien ne m'indique. Et rien ne me dit « cette mélodie joue dans Groove et Drop ».
7. **Le drop qui tape plus fort.** Toujours aucun bouton. Ce que je trouve : « Ce qui joue dans C · Drop » (session.png, what_plays.png) qui liste des « Effets et envois : Compresseur · Saturation · OTT multibande · Pompe » avec « Modifier » vers le Mixeur ; dans le Mixeur, « Volume visé : Auto (style) · −8 / Club · −8 / Streaming · −14 » (mixer_master.png, `…/mixer/MasterLoudness.kt:90-108`) : ça monte **tout** le morceau, pas le drop ; la ligne du temps permet de couper des pistes dans une partie (`…/session/TimelineScreen.kt:738`), donc je pourrais enlever des choses dans le Groove pour que le Drop paraisse plus gros… personne ne devine ça. J'abandonne, comme à la vague 1.
8. **Ajouter une fin.** Le morceau finit par « Intro pour finir » puis s'arrête net. Tap « Modifier le morceau » sur la carte → **ligne du temps plein écran** (timeline_screen.png, `AppRoot.kt:855` `showTimeline = true`). Il n'y a **aucun bouton « ajouter une partie »** là-dedans : seulement un appui long sur un bloc › « Insérer une scène après… » (`TimelineScreen.kt:1540`). Et un simple toucher sur un bloc **coupe la piste dans cette partie** (`:738`, `:1506`) : en « explorant », j'ai fait taire la batterie du Drop sans le vouloir (le bloc s'est juste assombri). Je reviens (« Revenir au morceau ») sur l'onglet Morceau : 2 défil. jusqu'à « SCÈNES », tap A, « Dupliquer », « Nommer », puce « Fin » (`…/session/SessionScreen.kt:735-765`, `project-model/…/Scenes.kt:43`), « Valider », défil. haut, « + E · Fin » dans « Ajouter à la fin : » (`SessionScreen.kt:344-350`) = **7 taps + 3 défil.**, et ma « Fin » est une copie de l'Intro qui s'arrête aussi net. Le fondu existe seulement à l'export (`…/mixer/MixerScreen.kt:586`) et il est appliqué en silence au partage (3 s, `…/export/ShareSong.kt:29`, `RhythmViewModel.kt:218`) sans que l'écran de partage le dise.

### 3. Partager : enfin simple

9. **⤴ en haut à droite, sur tous les onglets** (top_bar.png, `…/ui/TopBar.kt:272-276`) → feuille « Prêt à partager ? » (share_sheet.png, `…/export/ShareSheet.kt:289-345`) : nom déjà rempli, « 2:58 · 6 parties », ▶ pour réécouter, vérifications en français (« Solo actif… Tout réactiver », « Le limiteur travaille fort… Baisser le volume général »), « M4A · pour partager / WAV · qualité max », gros « Partager ». Tap « Partager » → « Je prépare ton morceau… Ensuite, choisis l'appli pour l'envoyer » (`…/export/ShareSongUi.kt:63-79`) → choix de l'appli. **2 taps + le choix de l'appli.** C'est réglé.
   - Détail : quand le volume est mesuré, la feuille dit « Volume perçu : −8,1 LUFS, … » : « LUFS » ne me parle pas.

### 4. Le fil « Prochaine étape » : il me parle d'autre chose

10. Dès l'arrivée, sous la barre du haut, sur tous les onglets (rhythm_after_wizard.png, `…/onboarding/NextStepStrip.kt:65-120`) : pour un morceau fait par l'assistant, les étapes 1 à 5 sont déjà cochées, donc la ligne dit **« 6/8 · Pose ta voix → »** (`…/onboarding/NextSteps.kt:108-110`). Je fais de la techno, je n'ai pas de micro ni d'écouteurs. Pour passer : tap sur la ligne (→ onglet Audio), tap sur la ligne (elle se déplie), « C'est fait » = 3 taps, puis « 7/8 · Règle les volumes » (encore 3 taps ou bouger un curseur), puis « 8/8 · Partage ton morceau ». Ça ne me dit jamais « il te manque une fin » ni « change un son ». Et ses textes parlent encore de « duplique ta **scène** dans l'onglet Morceau » (`NextSteps.kt:101-102`).

---

## Points durs

### P1 · La grille/le piano roll changent de partie pendant que j'édite (pénible, nouveau, peut passer pour un bug)
- Où : `…/rhythm/RhythmViewModel.kt:1638` (`_followSong = true` par défaut), `:2041` (`if (_followSong.value) session.editQuietly { it.switchScene(part.scene) }`), `:5638` (▶ joue le morceau dès qu'il existe). Écrans Rythme et Synthé › Notes.
- Pourquoi : la phase 33 a rendu ▶ « morceau » partout (bien), mais les éditeurs suivent la partie qui joue. Avant, ▶ bouclait la section et la grille restait stable. Maintenant, un débutant qui écoute en entier et touche des cases édite au hasard Intro, Groove ou Drop, et voit ses notes « sauter ». Rien à l'écran ne dit « tu édites la partie qui passe ».
- Fix : quand on touche une case/une note pendant la lecture du morceau, passer automatiquement en « ⟳ Partie » sur la partie affichée (avec un petit message « Tu modifies C · Drop, en boucle. ▶ pour reprendre le morceau »). Ou bien : ne pas suivre le morceau sur Rythme/Synthé/Acide tant qu'on touche la grille ; afficher en gros au-dessus de la grille « Partie : C · Drop (joue dans Drop ×2) ».

### P2 · Aucun moyen de « rendre le drop plus fort » (bloquant pour mon objectif, pareil qu'en vague 1)
- Où : rien n'existe. Le plus proche : `…/mixer/MasterLoudness.kt:90-108` (volume visé, global), `…/mixer/AutomationCard.kt` (courbes), `…/session/TimelineScreen.kt:1506` (couper une piste dans une partie).
- Pourquoi : je pense « plus de punch dans le Drop ». L'appli me propose des LUFS, une cible « Club », des courbes. Le volume d'une piste vaut pour tout le morceau.
- Fix (c'est la phase 34 prévue, à ne pas repousser) : sur chaque partie du morceau, « Énergie 1-4 » ou « Plus fort / Moins fort » ; sur le Drop, un bouton « Faire taper » (kick + basse +2 dB, filtre ouvert, impact) écrit en coulisses comme une courbe par partie.

### P3 · « Modifier le morceau » ouvre l'écran le plus dangereux, sans « + » (pénible, nouveau)
- Où : `…/ui/AppRoot.kt:855` (`structure = { …; showTimeline = true }`), `…/session/TimelineScreen.kt:738-742` (l'aide : « Toucher un clip : couper ou remettre la piste », « Appui long… »), `:1540` (insérer = appui long seulement). Capture timeline_screen.png.
- Pourquoi : le bouton promet « modifier le morceau » ; j'arrive sur M/S, « Suivre », « Boucle 3-4 », des courbes, et un simple toucher sur un bloc coupe une piste (sans message). Pour ajouter une partie ou une fin, il n'y a rien de visible ; il faut redescendre sur l'onglet Morceau et trouver « SCÈNES ».
- Fix : « Modifier le morceau » → la liste des parties (Le morceau) avec un « + » après la dernière qui propose « Intro, Couplet, Refrain, Break, Drop, **Fin**, copie de… ». Dans la ligne du temps : un bandeau la première fois (« Toucher un bloc = couper la piste dans cette partie ; appui long = options ») et un « + Partie » au bout de la règle.

### P4 · Pas de « Fin » (pénible, pareil)
- Où : `…/session/SessionScreen.kt:344-350` (« Ajouter à la fin : + A + B… » : des lettres), `:735-765` (le nom « Fin » existe dans la liste), `…/mixer/MixerScreen.kt:586` (fondu uniquement à l'export), `…/export/ShareSong.kt:29` (fondu de 3 s appliqué sans le dire).
- Pourquoi : 7 taps + 3 défil. pour obtenir une copie de l'Intro qui s'arrête net. Le morceau de l'assistant finit déjà « Intro pour finir » mais ça s'arrête d'un coup.
- Fix : bouton « Ajouter une fin » (Intro allégée sans kick à la dernière mesure, impact, fondu écrit dans le morceau) ; dans « Prêt à partager ? », écrire « Fondu de fin : 3 s » à côté de la durée (c'est déjà fait en coulisses, autant le dire).

### P5 · Quatre mots pour la même chose : scène, section, partie, clip (pénible, pareil ; le plan 33 promettait « Partie » seul)
- Où : onglet toujours nommé « Morceau » (`…/ui/AppRoot.kt:188`), carte « SCÈNES » et « Une scène = la batterie, l'acide et le piano roll » (`…/session/SessionScreen.kt:573`), « Insérer une scène après » (`:546`), puce « C ▾ » / « Scène à modifier » dans la barre du haut (`…/ui/TopBar.kt:322`), assistant « QUATRE SECTIONS » (`…/library/WizardScreen.kt:667-690`), carte « 6 parties » (`SongReady.kt:118`), étapes « Fais une deuxième section » (`NextSteps.kt:38`), ligne du temps « clip » (`TimelineScreen.kt:738`), glossaire « Scène » (`…/help/Glossary.kt:12-19`).
- Pourquoi : à chaque écran un mot différent ; je ne comprends pas qu'il s'agit de la même chose, ni que « A » = « Intro » = « la partie 1 » = « la scène A ».
- Fix : « Partie » partout dans l'interface (la lettre reste un détail) ; l'onglet devient « Structure » comme prévu ; dans la barre du haut « Partie : C · Drop ▾ ».

### P6 · L'assistant : 13 étapes et du jargon dès l'étape 2 (pénible, pareil)
- Où : `…/library/WizardScreen.kt:160` (« Étape x sur 13 »), `:200` (raccourci en bouton-texte), `:747` (ambiance : « Gamme La dorienne · accords i – i – IV – IV »), `:498-615` (303 : accent, slide, Densité, Coupure, Résonance), `:784` (« Lead / mélodie », interrupteur éteint par défaut), `project-model/…/StarterWizard.kt:436-440`.
- Pourquoi : le raccourci est bien là, mais discret ; chaque écran demande un choix entre des mots inconnus. Le résultat n'a pas de « Lead » si je ne sais pas que je dois l'allumer : mon morceau techno est sans mélodie reconnaissable.
- Fix : « Créer tout de suite » en vrai bouton plein dès l'étape 2, « Choix détaillés » en bouton-texte. Cacher « Gamme/accords i – IV » derrière un « ? ». Lead allumé par défaut en Techno/House, ou une étape « Une mélodie ? Oui / Non » en mots simples.

### P7 · « Changer un son » : bon raccourci, mais l'écran derrière n'a pas changé (pénible, mieux)
- Où : `…/ui/AppRoot.kt:850-854`, `…/synth/SynthScreen.kt:314`, `:420`, `:430` (« Réglages du son : Forme d'onde, Coupure, Résonance, Enveloppe du filtre »), `…/rhythm/RhythmScreen.kt:680` (son d'un pad, un par un).
- Pourquoi : le clavier occupe l'écran, les sons sont dessous ; la piste modifiée n'est pas dite en grand ; aucun « Kit » pour la batterie.
- Fix : sur Synthé, « Sons prêts à jouer » **au-dessus** du clavier avec le nom de la piste en titre (« Son de : Nappe ») ; puce « Kit ▾ » en tête de Rythme ; toucher le nom d'une piste dans le Morceau/la ligne du temps = feuille « Changer le son ».

### P8 · Le fil « Prochaine étape » pousse vers la voix et les volumes, pas vers ce qui manque (détail → pénible)
- Où : `…/onboarding/NextSteps.kt:98-115`, `…/onboarding/NextStepStrip.kt:65-70`.
- Pourquoi : après l'assistant il dit « 6/8 · Pose ta voix » à tout le monde, 3 taps pour l'ignorer ; il ne regarde ni « une fin », ni « un son changé », ni « le drop ». Ses conseils disent encore « scène ».
- Fix : pour un morceau d'assistant, étapes = Écouter → Changer un son → Une fin → Partager (Voix facultative, marquée « si tu veux ») ; une croix « pas pour moi » sur l'étape elle-même.

### P9 · La carte « Ton morceau est prêt » se perd d'un toucher (détail, nouveau)
- Où : `…/onboarding/SongReady.kt:92` (`Dialog(onDismissRequest = done.close)` → `markSeen`), `:62-70` (jamais remontrée).
- Fix : un toucher à l'extérieur ne compte pas comme « vu » ; et garder les quatre choix accessibles quelque part (en tête de l'onglet Morceau tant que rien n'a été changé, ou dans Outils › Aide).

### P10 · Le bouton le plus voyant de l'onglet Morceau est « Live », et REC y remplace le morceau par défaut (détail, pareil)
- Où : `…/session/SessionScreen.kt:235-248` (bouton orange en haut), `…/live/LiveScreen.kt:606` (« Remplacer le morceau » sélectionné par défaut, `RhythmViewModel.kt:3172` `takeAppend = false`).
- Pourquoi : un débutant tape sur le premier gros bouton ; l'écran Live (live_grid.png : M ■, Effets/303/Pads, Filtre DJ, REC) est un autre monde ; s'il joue avec REC, son morceau est réécrit (Annuler existe, mais il faut le voir).
- Fix : descendre « Live » sous la liste des parties ; « Ajouter à la suite » par défaut.

### P11 · Deux ▶ et deux tempos sur l'onglet Rythme (détail, nouveau)
- Où : `…/rhythm/RhythmScreen.kt:231-243` (carte « ▶ Jouer », « 120 BPM », annuler/rétablir) juste sous la barre du haut qui a déjà ▶, annuler, et la bande « 120 BPM · Intro mes. 1/60 · ⟳ Partie » (`…/ui/UnderTopBar.kt:63`). Capture rhythm_after_wizard.png.
- Pourquoi : trois lignes de commandes avant la première case ; je me demande si les deux ▶ font la même chose (oui).
- Fix : retirer la carte de transport de Rythme, garder la bande du haut.

### P12 · Jargon qui reste (détail, pareil)
« Acide », « 303 », « Génère-moi un pattern » (`…/acid/AcidScreen.kt:160`), « Quantifier », « Basse en fantôme » (`PianoRoll.kt:197-199`), « Lead », « Nappe », « Pluck », « Coupure », « Résonance », « OTT multibande », « Pompe » (what_plays.png), « LUFS · TP » (mixer_master.png), « Automation ». Le glossaire (`…/help/Glossary.kt`) explique « Scène » et « Automation » mais pas depuis ces mots-là.

---

## Ce qui manque pour finir un morceau complet

- Un bouton **« Ajouter une fin »** (et dire que le partage ajoute déjà un fondu).
- Un réglage **par partie** « plus fort / moins fort » ou « Énergie », sans courbes.
- **Un seul mot** pour scène/section/partie/clip, et l'onglet « Structure ».
- Une **protection pendant l'édition** : ce que je touche ne doit pas changer de partie sous mon doigt pendant que le morceau joue (ou le dire très clairement).
- Un **« + »** visible là où j'arrive quand je touche « Modifier le morceau ».
- **« Kit ▾ »** pour la batterie et le nom de la piste en gros au-dessus du choix de sons.
- « **Joue dans : Groove, Drop** » au-dessus du piano roll (et « modifier toutes / seulement celle-ci »).
- Un fil d'étapes qui regarde **mon** morceau (fin ? son changé ?) et non « la voix ».
- Le Lead allumé par défaut (ou une question simple) : sans lui, pas de mélodie à « rendre mienne ».

---

## Comparé à la vague 1

Top 5 de la vague 1, un verdict par ligne :

1. **▶ doit jouer tout le morceau partout, et dire ce qu'il joue** → **réglé** : `listensToSong` (`RhythmViewModel.kt:1636`), puce « ⟳ Partie », position « mes. 9/60 » sous la barre ; le guide et l'appli disent la même chose. Mais ça a créé P1 (la grille suit la lecture).
2. **« Plus fort / plus costaud » par partie pour le drop** → **pareil** : rien ; le nouveau « Volume visé » du Mixeur est global et en LUFS.
3. **« Changer le son » en un geste + « Joue dans : … » dans le piano roll** → **mieux à l'entrée** (bouton « Changer un son » → Synthé sur la bonne piste), **pareil derrière** (clavier d'abord, piste peu visible, batterie pad par pad, pas de « Joue dans »).
4. **Assistant plus court, écran Morceau simplifié, « partie » au lieu de « scène », mots expliqués** → **pareil** : 13 étapes, même raccourci discret ; l'onglet Morceau a toujours Live, Ligne du temps, Le morceau, Réglages, Scènes dans l'ordre ; vocabulaire inchangé ; onglet pas renommé.
5. **« Ajouter une fin » + « Partager » visible hors du Mixeur** → **réglé pour Partager** (⤴ partout, carte « prêt », « Prêt à partager ? » avec vérifications : c'est la meilleure nouveauté), **pareil pour la fin**.

Ce qui est **nouveau** avec la phase 33 (régressions ou confusions) :
- **P1** : ▶ « morceau » + « suivre le morceau » = les éditeurs changent de partie pendant l'édition (avant, ▶ bouclait la section : stable).
- **P3** : « Modifier le morceau » envoie dans la ligne du temps plein écran, où toucher = couper une piste et où il n'y a pas de « + partie ».
- **P8** : le fil « Prochaine étape » sous la barre, sur tous les onglets, qui dit « 6/8 · Pose ta voix » à un débutant techno.
- **P9** : la carte « Ton morceau est prêt » disparaît définitivement sur un toucher à côté.
- **P11** : doublon ▶/tempo/annuler sur Rythme (bande du haut + carte).
- Nouveau jargon visible : « LUFS », « TP », « Club · −8 », « Streaming · −14 », « Le limiteur travaille fort » (compréhensible grâce au bouton « Baisser le volume général », heureusement).
- Le fondu de fin n'est plus mentionné au partage (l'ancien bouton du Mixeur le disait) alors qu'il est appliqué.

---

## Top 5 des priorités (maintenant)

1. **Stabiliser l'édition pendant la lecture** (P1) : toucher une case/une note pendant que le morceau joue bascule en « ⟳ Partie » sur la partie affichée, avec un message ; et afficher en gros « Partie : C · Drop » au-dessus de chaque grille.
2. **« Ajouter une fin » et un « + » visible** (P3, P4) : un « + » après la dernière partie (Intro, Couplet, Refrain, Break, Drop, Fin, copie de…) dans la liste du Morceau **et** dans la ligne du temps ; « Modifier le morceau » ouvre cette liste, pas la ligne du temps ; dire « Fondu de fin : 3 s » dans « Prêt à partager ? ».
3. **« Plus fort / Énergie » par partie** (P2), sans passer par l'automation : c'est le seul point bloquant de mon objectif, inchangé depuis la vague 1.
4. **Un seul mot et un onglet « Structure »** (P5) : « Partie » partout (carte Scènes, menus, barre du haut, assistant, étapes, glossaire).
5. **Changer un son sans chercher** (P7) : sons au-dessus du clavier avec le nom de la piste, « Kit ▾ » sur Rythme, Lead allumé par défaut dans l'assistant, et un fil « Prochaine étape » qui propose « Change un son » / « Ajoute une fin » plutôt que « Pose ta voix » (P6, P8).
