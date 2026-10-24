# Testeur Amateur 4 — vague 2 (après la phase 33)

60 ans, peu à l'aise avec la technique, lit tout, tablette, interface à 130 %. Joue un peu de piano. Veut un morceau calme (« Chill » / lo-fi) avec SA mélodie jouée au clavier, l'écouter du début à la fin, l'envoyer à ses petits-enfants.

Chemins abrégés : `app/` = `app/src/main/java/com/pocketstudio/app/`, `model/` = `project-model/src/main/kotlin/com/pocketstudio/model/`. Lecture seule (code de la branche `ccr-80af1a1b-mn0wec`, phase 33 fusionnée, captures `app/screenshots/`). J'ai fait mon avis d'abord, puis seulement j'ai relu le rapport de la vague 1.

## Parcours réel

### 1. Créer le morceau (8 gestes au plus court, 16 en suivant toutes les étapes)
1. **Accueil** (`app/onboarding/Welcome.kt:93-135`, `welcome.png`, `scale130/fr_welcome.png`, `tablet/welcome.png`) : trois grandes cartes, la bonne est en orange. 1 geste : « Créer mon premier morceau (guidé) ». Sur tablette c'est parfait. Sur téléphone à 130 % la 3e carte est coupée et les langues passent sur deux lignes (pareil qu'avant).
2. **Assistant, étape « 1 sur 13 »** (`app/library/WizardScreen.kt:160, 219`, `wizard_style.png`) : « 13 » lui fait peur avant même d'avoir choisi (le total est celui du premier style, le sien en a 10). Cinq familles fermées : Techno, House, Bass music, Hip-hop, Autres. Elle cherche « calme », « doux », « piano », « Chill » : rien. Elle ouvre « Autres » (1 geste, Reggaeton / Afrobeats / Synthwave / Pop, non), referme (1), ouvre « Hip-hop » (1), fait défiler jusqu'au 7e et dernier, « Lo-fi hip-hop » (`model/StarterWizard.kt:443-460`), touche la carte (1), « Choisir ce style » (1). 5 gestes et de la chance : la description « Le hip-hop tout doux… batterie feutrée » est la seule phrase rassurante du lot, après « une 808 qui fait trembler » (Trap) et « une 808 saturée qui gronde » (Phonk).
3. **Étape 2, Ambiance** (`WizardScreen.kt:745-780`, `wizard_mood.png`) : « Sombre / épique » est l'ambiance conseillée pour le Lo-fi (`model/StyleRecipes.kt:103-118`, `mood = Mood.Dark`). Elle, elle veut « Rêveur / planant » : elle le touche (1). Sous chaque ambiance : « Gamme Fa mineure · accords i – VI – III – VII » (chiffres romains, `WizardScreen.kt:763`) : elle ne sait pas si elle doit comprendre.
4. **La suite** : soit « Créer tout de suite (choix conseillés) » (1 geste, mais c'est un lien maigre tout en bas, `WizardScreen.kt:196-200`), soit « Suivant » × 8 (quatre lignes de batterie avec grille, interrupteur, tuiles de sons, offre de pack ; trois pistes « Basse », « Nappe / accords », « Lead / mélodie »), puis « Créer le morceau ». Rien, dans ces 10 étapes, ne dit « tu pourras jouer ta propre mélodie au clavier ». La piste « Lead / mélodie » du Lo-fi est éteinte par défaut, et si elle l'allume c'est l'appli qui écrit « une petite phrase paresseuse au vibraphone », pas elle.
5. **« Ton morceau est prêt ! »** (`app/onboarding/SongReady.kt:122-171`, `song_ready.png`) : « Lo-fi hip-hop du soir · 2:40 · 6 parties », quatre gros boutons Écouter / Changer un son / Modifier le morceau / Partager, « Lire le guide de ton morceau ». Très bien. « Écouter » (1) : ça joue tout le morceau, la position s'affiche sous la barre. Elle comprend qu'un morceau existe.

### 2. Jouer sa mélodie et la garder (8 à 10 gestes, 3 pièges)
1. Onglet **Synthé** (1 geste ; elle hésite avec « Acide » et « Mixeur », l'icône piano la sauve).
2. **Quelle piste ?** La barre du haut de l'écran montre « Basse · … », « Nappe · … », « + Piste », « Effets » (`app/synth/TrackBar.kt:519-585`). La piste choisie est la dernière utilisée. Si elle joue tout de suite, ses notes vont dans la « Nappe » (les accords du morceau). Pour avoir sa piste : « + Piste » (1) → fenêtre « Ajouter une piste » (`app/synth/TrackAdd.kt:50-70`) dont le premier et plus gros bouton est « Écrire une mélodie pour moi » (l'appli l'écrit à sa place !), puis « Ou une piste à remplir toi-même, avec le bon son : Mélodie (lead) / Accords (nappe) / Basse / Arpège / Pluck / percussif / Piste vide (son de base) » (`model/NewTracks.kt:9-13`). Elle prend « Mélodie (lead) » (1), un son de synthé aigu, pas un piano.
3. **Enregistrer** : rien sur l'écran du clavier (`app/synth/SynthScreen.kt:346-385`, `synth.png`). C'est le petit rond rouge du bandeau du haut, sans mot (`app/ui/TopBar.kt:245-262`, `top_bar.png`). Elle le touche (1). Il y a bien une mesure de décompte (Réglages, `app/settings/Settings.kt:51`), mais rien ne s'affiche en grand et le métronome reste à allumer dans Rythme (`app/rhythm/RhythmScreen.kt:634-635`). Et surtout : la prise tourne **sur la scène affichée, en boucle de 4 mesures**, pas sur le morceau (`app/rhythm/RhythmViewModel.kt:5250-5284` : `NativeEngine.play(countIn, aloneSongStep())` ; `:5340-5356`, `:5375-5394` ; `app/synth/TakeLoop.kt:17-24`). Sa phrase de 8 ou 16 mesures se replie sur 4 : les mesures 5-8 s'empilent sur les 1-4. Si elle lance ▶ d'abord puis ●, ses notes tombent dans la scène entendue à ce moment-là (« REC dans Couplet » en petit sous le nom, `TopBar.kt:211-214`), et chaque partie jouant deux fois, le second passage se superpose au premier. Elle arrête avec ● (1).
4. **Réécouter** : ▶ du haut (1) joue tout le morceau depuis l'Intro : sa mélodie n'arrive qu'à la partie où elle l'a jouée, mélangée. Pour entendre juste sa partie : « ⟳ Partie » (1) puis ▶ (1).
5. **Corriger le rythme** : « Notes » (1) puis « Quantifier 50 % » / « 100 % » (1) (`app/synth/PianoRoll.kt:197-198`). Les départs sont déjà aimantés à la double-croche à l'enregistrement (`RhythmViewModel.kt:5383-5384`), mais personne ne le lui dit, et « Quantifier » ne lui dit rien. Aucune fenêtre « Voilà ta prise : Écouter / Refaire / Garder ».

### 3. Écouter du début à la fin (1 geste) — réglé
▶ du bandeau, sur n'importe quel onglet (`app/ui/AppRoot.kt:884`, `app/ui/PlayStrip.kt`), et sous la barre « Intro · mes. 1/60 » en vert pendant la lecture (`play_strip.png`). Clair. « ⟳ Partie » explique l'autre cas. C'est le vrai progrès de la phase 33.

### 4. Envoyer aux petits-enfants (3 à 4 gestes) — réglé
⤴ en haut à droite (1) → « Prêt à partager ? » (`app/export/ShareSheet.kt:247-335`, `share_sheet_ok.png`) : nom déjà rempli, « 2:58 · 6 parties », « Tout est prêt : aucun solo oublié, aucune piste coupée. », « M4A · pour partager » coché, gros « Partager » (1) → « Je prépare ton morceau… » (`app/export/ShareSongUi.kt:63-79`) → la fenêtre Android (1-2). Elle y arrive aussi par Mes projets › « Partager mon morceau » et par la 8e étape du fil. Reste : l'icône ⤴ n'a pas de mot (elle ne la connaît pas), et la ligne « Volume perçu : −14,0 LUFS, … » (`ShareSheet.kt:298`).

### 5. Savoir qu'elle a fini
Le fil « 3/8 · Ajoute une mélodie → » sous la barre, sur tous les onglets (`app/onboarding/NextStepStrip.kt:52-137`), finit par « 8/8 · Ton morceau est fini, bravo ! ». Mais pour le morceau de l'assistant, les étapes 1 à 5 sont déjà cochées (la Nappe compte comme « mélodie », `app/onboarding/NextSteps.kt:84-91`) : le fil s'ouvre sur « 6/8 · Pose ta voix → » (« chanter ou rapper », `NextSteps.kt:115`). Il ne lui demande jamais de jouer SA mélodie, et pour passer « Voix » et « Volumes » il faut : toucher le fil (va sur l'onglet) → le retoucher (ouvre la carte) → « C'est fait » (`NextStepStrip.kt:81-96`, `app/onboarding/NextStepCard.kt:118-121`).

Où elle s'arrête probablement : à l'étape 2.3 (sa mélodie s'empile et sonne faux en rythme), ou avant, à 2.2 (quelle piste ?).

## Points durs

**1. Bloquant : sa mélodie n'a pas de place dans le morceau — la prise tourne en boucle sur une scène**
- Écran : Synthé + bandeau du haut. `app/rhythm/RhythmViewModel.kt:5250-5284` (● lance la scène seule : `aloneSongStep()` = hors du morceau dès qu'il y a un morceau), `:5340-5356` (`recordAt` replie la position sur la longueur de la boucle), `:5375-5394` (la note s'écrit dans la scène entendue), `app/synth/TakeLoop.kt:17-24` (boucle poussée à 4 mesures), message `app/ui/AppRoot.kt:942-943`.
- Pourquoi : une pianiste joue une phrase de 8 à 16 mesures d'un trait, par-dessus le morceau. Ici tout se replie sur 4 mesures ou se dispatche par scène ; elle n'entend pas ce qu'elle a joué. Le message « La boucle passe à 4 mesures pour ta prise… ↶ en haut » est du jargon.
- Fix : sur l'écran Clavier, un gros « ● Enregistrer ma mélodie » qui demande « Sur quelle partie ? (Couplet / Refrain / Tout le morceau) », enregistre en ligne droite sur le morceau (comme une prise audio : les pistes audio savent déjà se caler sur le morceau, `song = true`), affiche « 4 · 3 · 2 · 1 » en grand, puis propose « Écouter / Remettre les notes en rythme / Refaire / Garder ».

**2. Bloquant : le bouton Enregistrer est toujours un petit rond rouge sans mot, loin du clavier**
- Écran : bandeau (`app/ui/TopBar.kt:245-262`, rond 30 dp, point 14 dp, le mot « Enregistrer ce que je joue » n'existe que pour le lecteur d'écran). Rien dans `app/synth/SynthScreen.kt:346-385` (carte Clavier) ni dans `PlayPanel.kt`.
- Pourquoi : à 60 ans, sur tablette, un rond rouge avec un cercle gris est un voyant. Elle joue, rien n'est gardé, l'étape « Mélodie » ne se coche pas (`NextSteps.kt:84-91` ne compte que les notes écrites), elle croit que l'appli ne marche pas.
- Fix : voir 1. Au minimum le mot « Enregistrer » sous le rond quand il y a la place (tablette, 130 %).

**3. Pénible : quelle piste reçoit ses notes ?**
- Écran : Synthé, barre des pistes (`app/synth/TrackBar.kt:519-585`) et « Ajouter une piste » (`app/synth/TrackAdd.kt:50-70`, `model/NewTracks.kt:9-13`).
- Pourquoi : la piste choisie est celle de « Changer un son » ou la dernière vue, souvent « Nappe » : ses notes vont dans les accords. La fenêtre d'ajout met en avant « Écrire une mélodie pour moi » (le contraire de son envie) et liste « Mélodie (lead) », « Accords (nappe) », « Arpège », « Pluck / percussif », « Piste vide (son de base) ».
- Fix : l'assistant crée une piste vide « Ma mélodie » (piano électrique pour le Lo-fi), choisie d'office quand elle ouvre Synthé ; dans la fenêtre d'ajout, « Jouer ma propre mélodie » en premier.

**4. Pénible : « calme » introuvable, et l'assistant conseille « Sombre / épique » pour le Lo-fi**
- Écran : assistant étape 1 (`app/library/StyleFamilies.kt:59-108`, `model/StarterWizard.kt:443-460` : Lo-fi 7e et dernier de « Hip-hop »), étape 2 (`model/StyleRecipes.kt:103-118` `mood = Mood.Dark` ; `WizardScreen.kt:763, 779`). Pareil dans Nouveau projet (`app/projects/NewProjectPage.kt:435`).
- Pourquoi : les familles sont des noms de clubs (« Bass music » en anglais). Aucune porte « Calme / Détente / Piano ». La démo « Démo · Chill » existe (`model/Demos.kt:12`) mais n'est pas un style qu'on peut créer. L'ambiance conseillée pour « le hip-hop tout doux » est « Sombre / épique ».
- Fix : famille « Calme / Détente » en tête (Lo-fi, Chill, Synthwave, Pop), ambiance conseillée « Rêveur » pour le Lo-fi, chiffres romains derrière un « ? ».

**5. Pénible : « Étape 1 sur 13 » et le raccourci caché**
- Écran : `app/library/WizardScreen.kt:160, 196-200, 219`.
- Pourquoi : 13 étapes annoncées avant tout choix (le Lo-fi en a 10). Le seul bon réflexe pour elle, « Créer tout de suite (choix conseillés) », est un lien fin au bas de l'écran, sous les cartes.
- Fix : « Étape 1 » sans total tant que le style n'est pas choisi ; dès le style choisi, un vrai bouton « Créer le morceau avec les choix conseillés » juste sous « Choisir ce style », et « Je veux choisir les sons » en second.

**6. Pénible : à 130 % le bandeau perd le nom du projet et la position se casse (nouveau)**
- Écran : bandeau (`app/ui/TopBar.kt:136, 157` : `squeezed` → `projectName = false` ; capture `scale130/fr_top_bar.png`, lignes 2 et 3 : plus que le dossier). Bandeau de lecture (`app/ui/PlayStrip.kt:185-226`, `Column(Modifier.weight(1f, fill = false))` ; capture `scale130/fr_play_strip.png`, dernière ligne : « … / me » avec « Solo »). Onglets du bas (`app/ui/AppRoot.kt:958-972` : en dessous de 58 dp par onglet, étiquettes à 85 % de `labelSmall`, soit ≈ 9 sp ; à 130 % un téléphone de 411 dp fait 316 dp, donc 45 dp par onglet). Parties « B · Co… », « C · Ref… » (`scale130/fr_session.png`).
- Pourquoi : c'est exactement elle qui met 130 %, et c'est pour elle que les textes rapetissent ou disparaissent. Elle ne sait plus sur quel morceau elle est.
- Fix : bandeau sur deux rangées à 130 % (nom seul sur la première) ; position sur deux lignes et puce « Solo » renvoyée dans le bandeau jaune ; onglets jamais sous 12 sp (5 onglets + « Plus », ou rail sur tablette) ; noms des parties sur deux lignes.

**7. Pénible : les mots (pareil qu'avant, plus deux nouveaux)**
- Onglets « Acide », « Synthé », « Mixeur » (`AppRoot.kt:173-190`). Synthé : « Lead », « Pluck », « Nappe », « Pads », « Gamme », « Scie / Carré / Triangle », « Coupure », « Résonance », « Enveloppe du filtre », « Unisson (supersaw) », « Ladder » (`SynthScreen.kt:105-126, 420-466`). Piano roll : « Quantifier », « Legato », « Humaniser », « 1/16 t », « Basse en fantôme » (`PianoRoll.kt:150-203, 244-278`). Morceau : « Live », « Pads », « 303 », « Drop », « Break », « Groove ». Rythme : « Kick », « Clap », « Rim », « Cowbell », « Zap ». Nouveaux dans la feuille de partage : « LUFS » (`ShareSheet.kt:298`), « Le limiteur travaille fort » (`:363`), « compression en M4A » (`ShareSongUi.kt:70`).
- Pourquoi : elle lit tout ; chaque mot inconnu la fait douter d'avoir fait une bêtise.
- Fix : renommer (Synthé → Clavier, Lead → Mélodie, Nappe → Ambiance, Quantifier → Remettre en rythme), cacher LUFS / limiteur derrière « Voir le détail », ou le « Mode simple » prévu (phase 36). Les « ? » du lexique existent (`app/help/Glossary.kt`) mais pas à côté de « Quantifier », « Lead », « LUFS ».

**8. Pénible : l'écran Morceau est encore plus long**
- Écran : `app/session/SessionScreen.kt:242-243` (pilule orange « Live : jouer les sections piste par piste / Pads de toutes les pistes, effets et 303 sous les doigts » en tête), `:256-272` (nouvelle carte « Ligne du temps », phase 31-32), `:327-395` (« Le morceau », parties avec − + ⋮, « Ajouter à la fin », « Jouer le morceau », « Ce qui joue », « Réglages du morceau », « Temps par mesure 2/4 … 7/4 »), puis « Scènes » avec 16 lettres et « Effets live » (`session.png`, `scale130/fr_session.png`).
- Pourquoi : le premier bloc, le plus voyant, parle de Live, pads et 303 : rien pour elle. « Jouer le morceau » est au milieu de la page. Sur tablette les deux colonnes aident (`tablet/session.png`).
- Fix : « Live » et « Ligne du temps » repliés sous « Pour aller plus loin » ; « Le morceau » en tête ; une phrase d'aide au lieu de cinq.

**9. Pénible : le fil « Prochaine étape » ne suit pas son chemin**
- Écran : `app/onboarding/NextSteps.kt:70-120`, `NextStepStrip.kt:52-137`.
- Pourquoi : après l'assistant, le fil s'ouvre sur « 6/8 · Pose ta voix » (« chanter ou rapper ») : il ne sait pas qu'elle veut jouer, et la 3e étape « Ajoute une mélodie » est déjà cochée par la Nappe de l'assistant. « Voix » et « Volumes » se cochent à la main par un chemin en trois gestes. La croix ferme le fil pour de bon (retour par Outils › Aide, qu'elle ne trouvera pas).
- Fix : après l'assistant, un fil court et dans son ordre : « Joue ta mélodie (●) » → « Écoute en entier » → « Envoie-le », avec « Voix » en « Passer ». Le mieux : « 8/8 · Ton morceau est fini, bravo ! » existe déjà.

**10. Détail : icônes sans mot**
- ⤴ (glyphe Apple `IosShare`, `TopBar.kt:288-290`), ● (`:245`), ↶, dossier orange (`:207`), puce « C ▾ » (une lettre seule), « ⟳ Partie ». Sur tablette et à 130 % il y a la place pour un mot sous chacune.

**11. Détail : après « Ton morceau est prêt »**
- « Modifier le morceau » ouvre la ligne du temps plein écran avec « Suivre », « Boucle », « Montrer les courbes », zoom (`app/ui/AppRoot.kt:855`, `app/session/TimelineScreen.kt:661-704`) : trop pour elle ; la liste des parties de Morceau suffirait. Le guide en 5 pages (`app/library/SongGuide.kt:76-100`) dit « Synthé : joue au clavier… aucune fausse note possible » (l.88) mais jamais « appuie sur ● pour garder ce que tu joues ».

**12. Détail : écran d'accueil à 130 % sur téléphone**
- `scale130/fr_welcome.png` : « Partir de zéro » coupé en bas, langues sur deux lignes, « Le guide des premiers pas reste dans Outils › Aide » en petit gris (`Welcome.kt:132-133`). Pareil qu'avant ; sur tablette c'est bien.

## Ce qui manque

- Une piste « Ma mélodie » prête, un enregistrement en ligne droite sur le morceau (ou sur la partie choisie), et la fenêtre de fin de prise « Écouter / Remettre en rythme / Refaire / Garder ». C'est le cœur de sa demande, et c'est encore impossible sans comprendre scène, boucle, piste.
- Un vrai bouton « Enregistrer ma mélodie » sur l'écran du clavier, avec décompte visible et métronome qui s'allume tout seul.
- Une porte « Calme / Détente » dans l'assistant, un Lo-fi conseillé « Rêveur », et le mot « Chill » quelque part dans l'assistant (il n'existe que dans une démo).
- Une étape de l'assistant ou du guide qui dit « maintenant, joue ta mélodie : ● ».
- Des mots sous les quatre icônes du bandeau sur tablette et à 130 %, et un bandeau qui ne jette pas le nom du projet.
- Le « Mode simple » (onglets Clavier / Rythme / Mon morceau / Envoyer, mots français) : annoncé pour la phase 36, toujours absent.
- Une case « lent / moyen / rapide » dans la feuille du tempo (70 / 90 / 120 / 140 / 174 sans mots, `app/ui/TempoSheet.kt:62`), détail.

## Comparé à la vague 1

Top 5 de la vague 1 :
1. **Gros bouton « ● Enregistrer ma mélodie », décompte, métronome, prise sur plusieurs mesures → pareil.** `TopBar.kt:245-262` est le même rond ; rien dans `SynthScreen.kt` ; même message « La boucle passe à 4 mesures… ↶ en haut » (`AppRoot.kt:942-943`). Prévu phase 35.
2. **Après la prise : Écouter / Remettre en rythme / Refaire / Garder → pareil.** `PianoRoll.kt:197-198` : « Quantifier 50 % / 100 % » inchangés, aucune fenêtre après la prise. Prévu phase 35.
3. **« Envoyer mon morceau » visible partout + « Ton morceau est prêt » → réglé.** ⤴ sur tous les onglets (`TopBar.kt:288-290`), « Prêt à partager ? » avec nom pré-rempli, M4A par défaut et vérifications en français (`ShareSheet.kt:247-335`), carte « Ton morceau est prêt ! » (`SongReady.kt:122-171`), « Partager mon morceau » dans Mes projets, 8e étape du fil. Restes : l'icône sans mot, « LUFS ».
4. **Mode simple (4 onglets, mots français, famille « Calme ») → pareil.** 7 onglets identiques (`AppRoot.kt:173-190`), familles identiques (`StarterWizard.kt:443`), même vocabulaire Synthé / piano roll. Prévu phase 36.
5. **Alléger l'écran Morceau + textes coupés à 130 % → mieux pour la lecture, pareil pour l'écran, pire pour les coupures.** Mieux : ▶ du haut joue tout le morceau partout, position « Couplet · mes. 9/60 » sous la barre, « ⟳ Partie » (`PlayStrip.kt`). Pareil : la pilule Live reste en tête et la carte « Ligne du temps » s'est ajoutée (`SessionScreen.kt:242-272`). Pire : à 130 % le nom du projet disparaît entièrement avec la puce de scène (vague 1 : « Da F… »), et la position devient « … / me » avec « Solo ».

Autres points de la vague 1 : jargon (6) pareil ; sept onglets rétrécis à 130 % (7) pareil ; Chill / Lo-fi (8) pareil, sauf que l'étape 303 n'apparaît pas pour le Lo-fi (`StarterWizard.kt:436-440`) ; « Prochaine étape » (9) mieux (une ligne sur tous les onglets, « 8/8 · Ton morceau est fini, bravo ! ») mais toujours 8 étapes avec Voix / Volumes à la main ; coupures 130 % (10) pire ; icônes sans mot (11) pareil + une de plus ; nom du morceau (12) mieux (l'assistant donne un nom, la feuille de partage le pré-remplit).

**Nouveau depuis la vague 1 (régressions et nouveautés qui la gênent) :**
- Bandeau à 130 % sans nom de projet (`TopBar.kt:136, 157`, `scale130/fr_top_bar.png`).
- Bandeau de lecture à 130 % : « … / me » avec « Solo » (`PlayStrip.kt:185-226`, `scale130/fr_play_strip.png`).
- ⤴ : nouvelle icône sans mot, glyphe iOS.
- Feuille de partage : « Volume perçu : −14,0 LUFS », « Le limiteur travaille fort » (`ShareSheet.kt:298, 363`).
- Fil « Prochaine étape » qui s'ouvre sur « Pose ta voix » après l'assistant (`NextSteps.kt:84-91, 115`).
- Morceau : carte « Ligne du temps » en plus en haut (`SessionScreen.kt:256-272`) ; « Modifier le morceau » de la carte finale envoie dans cette ligne du temps plein écran (`AppRoot.kt:855`).

## Top 5 maintenant

1. **Sa mélodie, gardée telle qu'elle la joue** (bloquant) : piste « Ma mélodie » prête et choisie, « ● Enregistrer ma mélodie » gros et nommé sur l'écran Clavier, prise en ligne droite sur le morceau ou sur une partie choisie, « 4 · 3 · 2 · 1 » en grand, puis « Écouter / Remettre en rythme / Refaire / Garder » (`TopBar.kt:245`, `SynthScreen.kt:346`, `RhythmViewModel.kt:5250-5394`, `TakeLoop.kt`, `PianoRoll.kt:197`, `TrackAdd.kt:50-70`).
2. **Trouver « calme » dans l'assistant** (pénible) : famille « Calme / Détente » en tête, Lo-fi conseillé « Rêveur », « Étape 1 » sans « sur 13 », « Créer avec les choix conseillés » en vrai bouton (`StarterWizard.kt:443-460`, `StyleRecipes.kt:103-118`, `WizardScreen.kt:160-200`).
3. **130 % qui respecte le texte** (pénible) : bandeau sur deux rangées, position sur deux lignes, onglets jamais sous 12 sp, noms des parties sur deux lignes (`TopBar.kt:136-157`, `PlayStrip.kt:185-226`, `AppRoot.kt:958-972`, `SessionScreen.kt`).
4. **Les mots** (pénible) : Clavier au lieu de Synthé, Mélodie / Ambiance au lieu de Lead / Nappe, Remettre en rythme au lieu de Quantifier, LUFS et limiteur cachés, un mot sous ⤴ ● ↶ et le dossier (`AppRoot.kt:173-190`, `SynthScreen.kt:105-126`, `PianoRoll.kt:197-278`, `ShareSheet.kt:298-363`, `TopBar.kt:207-290`).
5. **Son chemin, pas celui du rappeur** (pénible) : fil « Joue ta mélodie → Écoute en entier → Envoie », « Voix » facultative ; écran Morceau avec « Le morceau » en tête et Live / Ligne du temps repliés (`NextSteps.kt:70-120`, `SessionScreen.kt:242-272`).
