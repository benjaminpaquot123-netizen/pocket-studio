# Recherche : le « Guide » de Pocket Studio

Objectif de Benjamin : « réaliser un morceau pro avec un guide pas à pas même si ça prend deux heures et plus ; le guide doit toujours être présent, savoir où on en est, ce qu'on peut faire et ce qu'on veut faire pour la suite ; chaque étape de chaque piste scrutée et guidée : chaque son, quand, avec quel effet, combien de fois, si on veut l'utiliser ailleurs ».

Ce document rassemble (1) ce que font les meilleurs produits, (2) la liste de contrôle d'une production pro étape par étape et piste par piste, (3) la pédagogie d'une séance de deux heures, (4) les pièges, puis (5) dix principes classés pour le Guide. Les sources sont en fin de document ; les chiffres (dB, ms, mesures) sont des points de départ de praticiens, pas des lois.

---

## 1. Comment les meilleurs produits guident

### 1.1 Ce que fait chaque produit (en une ou deux lignes)

| Produit | Mécanisme de guidage | À retenir pour nous |
|---|---|---|
| Ableton **Learning Music / Learning Synths** [S1][S2][S3] | Mini-leçons dans le navigateur ; chaque notion est un jouet interactif (on touche, on écoute) ; 10 chapitres beats → gammes → accords → basse → structure ; « recettes » (kick, basse pluck…) ; export vers Live. Les experts peuvent sauter les leçons. | Apprendre en manipulant, une notion par écran, recette = modèle à copier, sortie de la leçon vers le vrai projet. |
| Ableton **Live 12 : Info View + Learn View** [S4] | Deux outils séparés : aide contextuelle au survol (nom + fonction de ce qu'on touche, toujours disponible, touche ?) et leçons guidées dans un panneau latéral non modal. On peut écrire ses propres notes dans le Set. | Deux canaux : aide « qu'est-ce que c'est ? » en permanence + parcours « que faire maintenant ? » dans un panneau qui ne bloque pas. Notes personnelles sauvées dans le projet. |
| **FL Studio Mobile** [S5] | Pas de guide intégré sérieux ; les débutants vont chercher des tutos externes. Workflow pattern → playlist réutilisable. | Le vide à combler : le guide dans l'appli. Les patterns réutilisables = « l'utiliser ailleurs ». |
| **GarageBand** (iOS) [S6][S7] | Live Loops : grille + modèles par style ; bibliothèque de sons téléchargeable ; « Learn to Play » (Mac) = vidéos à jouer en même temps ; promesse « sonner pro sans jamais avoir joué une note ». | Modèles par style comme point de départ, apprentissage par imitation en jouant. |
| **Koala Sampler** [S8][S9] | Trois onglets qui disent le chemin : Sample → Sequence → Perform. Interface sans menus, geste direct. | Le parcours est écrit dans la structure de l'écran ; peu de choix à chaque instant. |
| **BandLab** (SongStarter, Mastering) [S10][S11] | SongStarter : un mot ou une humeur → 3 idées complètes (rythme + mélodie + accords), bouton « encore 3 » ; Mastering : analyse puis 4 styles nommés (clair, chaud…) + intensité. | Propositions multiples prêtes à écouter, un bouton « autre idée », résultats nommés avec des mots simples. |
| **Endlesss** [S12][S13] | Jam en boucles de 8 mesures, chaque couche est une pile de fichiers audio, headroom géré en arrière-plan. Critique : options cachées dans « avancé ». | Le moteur gère la marge de volume à la place de l'utilisateur ; attention aux fonctions cachées. |
| iZotope **Neutron Mix Assistant / Ozone Master Assistant** [S14][S15][S16] | Écoute l'audio, détecte le genre, propose une **chaîne complète** (EQ, dynamique, limiteur) vers une **cible** (genre ou morceau de référence) ; vue « Assistant » simple et vue « Détail » complète ; comparaison avant/après avec compensation de volume. Les critiques : un bon point de départ, jamais une fin ; patine si le mix est mauvais. | Cible nommée par style ; chaîne proposée + explication ; deux niveaux de contrôle ; écoute A/B à volume égal ; dire « c'est un point de départ ». |
| **Logic Pro** Session Players / Mastering Assistant [S17][S18][S19] | Session Players suivent la grille d'accords ; curseurs Complexité, Intensité, Fills, Swing, bouton Régénérer. Mastering Assistant : analyse le projet (ou juste la boucle), montre la courbe d'EQ appliquée, vise ≈ −14 LUFS et −1 dBTP, 4 caractères (Clean, Punch, Valve, Transparent), bouton de compensation de volume pour comparer honnêtement. | Quelques curseurs à noms humains ; « régénérer en gardant mes réglages » ; montrer ce qu'on a fait ; A/B à volume égal. |
| **Splice Create** [S20][S21] | Choisir un genre → pile de 8 boucles compatibles (tempo et tonalité alignés automatiquement) ; on remplace une couche ou on régénère la pile. | Compatibilité garantie par la machine ; remplacer une couche sans tout refaire. |
| **Soundtrap** [S22] | Courbe d'apprentissage courte, Patterns Beatmaker, cours « premiers pas » ; limites assumées. | Rester simple est une qualité revendiquée. |
| **Moises** [S23] | Pistes séparées, accords détectés, sections détectées pour boucler, tempo/tonalité. | Détecter la structure pour proposer une boucle de travail. |
| **Suno / Udio** (prompt-to-edit) [S24][S25] | Éditeur de sections : remplacer une section, étendre, réparer ; la règle est « un seul outil par problème » (Extend si incomplet, Replace si une seule zone faible, Studio pour le détail). | Vocabulaire de réparation par section : « refaire juste ce passage ». |
| **Duolingo** [S26][S27][S28] | Chemin unique (plus d'arbre à choix libre depuis 2022, « pour réduire la confusion ») ; leçons ≈ 5 min ; série quotidienne ; révisions insérées au bon moment ; progression visible après chaque leçon. Critiques : perte de liberté, gamification agaçante. | Un chemin lisible, étapes courtes, progression visible ; mais garder une sortie du chemin. |
| **Jeux vidéo** (tutoriels « just-in-time ») [S29][S30][S31] | Enseigner une mécanique juste avant qu'elle serve, dans le contexte, avec un indice visuel plutôt qu'un texte ; les études montrent plus de motivation et de maîtrise chez les novices ; l'aide contextuelle mal faite (Clippy) frustre. | Le bon moment + le bon endroit, pas un manuel en entrée. |
| **Sentry « Assistant »** [S32] | Leur checklist d'onboarding était ignorée (« liste de corvées ») ; remplacée par un widget discret qui apparaît quand l'utilisateur arrive sur quelque chose de nouveau, guides décrits en données (JSON). | Une checklist nue ne suffit pas ; il faut un compagnon qui réagit au contexte. Guides = données, pas code. |
| **Apple HIG : onboarding** [S33] | « Enseigner par l'interaction » ; préférer des conseils contextuels à un flux unique ; tout tutoriel doit être sautable et retrouvable plus tard. | Règle officielle de plateforme qui va dans le même sens. |

### 1.2 Les patrons qui ressortent

1. **Panneau compagnon persistant plutôt qu'assistant modal.** Live (Learn View), Sentry, Apple HIG : le guide vit à côté de l'écran de travail, jamais devant. Le wizard modal est accepté uniquement pour la création initiale (choix du style, des sons), comme notre assistant actuel.
2. **Deux canaux séparés** : « qu'est-ce que c'est ? » (Info View : toujours là, au toucher) et « que faire maintenant ? » (le parcours). Ne pas mélanger.
3. **État détecté dans le projet → prochaine meilleure action.** Les assistants iZotope et Logic analysent l'audio pour proposer ; Moises détecte les sections. Pour nous : lire le projet (pistes vides, piste sans effet, section sans variation, pas d'automation, marge insuffisante, kick et basse qui se marchent dessus) et déduire la carte « prochaine étape ».
4. **Proposer une chaîne entière avec explication, puis laisser régler** : vue simple (2 à 4 curseurs à noms humains) et vue détail. Toujours « c'est un point de départ, écoute et ajuste ».
5. **Écouter avant d'appliquer et comparer à volume égal** (Loudness Compensation chez Logic et iZotope, A/B chez Ozone). Sans compensation, « plus fort » paraît toujours « mieux ».
6. **Plusieurs propositions prêtes à l'écoute + bouton « autre idée »** (BandLab, Splice, Logic Regenerate « en gardant mes réglages »).
7. **Checklists par piste et par étape, mais incarnées** : une checklist nue est ignorée (Sentry) ; elle fonctionne quand chaque case est une action d'un geste, avec un pourquoi et une écoute.
8. **Sauter / revenir / reprendre** : Apple HIG, Live 12 (lessons retrouvables dans le menu Help), Learning Synths (les experts sautent). Le chemin Duolingo montre le risque inverse : trop rigide = frustration.
9. **Sessions courtes et progression qui survit aux jours** : Duolingo (≈ 5 min, retour visible, révisions programmées), Apple (ne pas faire mémoriser). Pour un morceau de 2 h : ~30 à 40 micro-étapes, chacune marquée faite / sautée / à revoir, résumé « où on en est » à la réouverture.
10. **Le moteur prend en charge ce qui n'apprend rien** : tempo/tonalité alignés (Splice), headroom (Endlesss), cible de loudness (Logic). Le guide n'enseigne que les décisions musicales.
11. **Guides décrits en données** (Sentry) : un fichier par style, étapes = données, pour que 37 styles ne soient pas 37 fois du code.

---

## 2. La liste de contrôle d'une production pro

Formulée comme Benjamin le demande : **quand / quel effet / combien / combien de fois / réutilisable ailleurs ?** Les valeurs sont des points de départ de praticiens [S34]–[S50] ; le Guide doit les proposer, faire écouter, puis laisser bouger.

### 2.1 Étape 0 : le cadre (5 min)
- Style, tempo, tonalité, durée visée, destination (téléphone/streaming ou club). Streaming : le refrain/drop tôt (les 30 premières secondes comptent) ; club : intro et outro longues pour le DJ [S41].
- Le Guide fixe la **grille de phrases** : tout bouge par 8 mesures (4/8/16/32) ; dévier est un effet voulu, pas un accident [S41][S42].

### 2.2 Étape 1 : choix des sons et patterns, piste par piste (ordre pro : kick → caisse claire/clap → charleys → basse → accords/nappe → lead → voix/FX)
Pour chaque piste, quatre questions : **quel rôle** (fondation, groove, harmonie, mélodie, air), **quel son** (un seul par rôle au début), **quel motif** (1 ou 2 mesures), **où il jouera** (quelles sections).
- Kick : un seul, accordé à la tonalité si possible ; en trap, un kick court en couche sur le 808 pour qu'il passe sur un téléphone [S45].
- Basse : en DnB et dubstep, **couper en deux couches** : sub (sinus/propre, mono, < 120–150 Hz) et médiums (saturation, mouvement, stéréo) [S47][S48].
- Charleys : vélocité variable (−30 % une hit sur deux) et décalage pour « respirer » ; trap : triolets et roulements [S45][S49].
- Réutilisable ailleurs ? Oui par défaut : un pattern est un **objet nommé** qu'on place dans plusieurs sections (FL Studio) ; le Guide propose « ce motif revient au 2e drop ? ».

### 2.3 Étape 2 : superposition (layering), avec parcimonie
- Une couche n'est ajoutée que si elle répond à un manque entendu : « plus de claquant » (transitoire), « plus de corps » (médium), « plus d'air » (aigu). Jamais deux sons pour le même rôle dans la même bande de fréquence [S43].
- Au-dessous de 150 Hz : **une seule source à la fois** (kick ou sub) [S47].

### 2.4 Étape 3 : chaîne d'effets par rôle (ordre + réglages de départ)
Ordre général pro : **correction (EQ coupe-bas) → dynamique (compresseur) → couleur (saturation) → EQ de forme → effets d'espace en envoi (réverb/délai) en dernier** ; nettoyer le grave *avant* de compresser évite le pompage [S34][S35][S36].

| Rôle | Quand | Quel effet | Combien (départ) | Combien de fois | Réutilisable ? |
|---|---|---|---|---|---|
| Kick | dès la pose du son | coupe-bas 25–30 Hz ; compresseur attaque moyenne pour le punch ; saturation légère pour les petits haut-parleurs | 1–3 dB de réduction ; saturation « à peine audible » | 1 chaîne pour tout le morceau | oui : « préréglage Kick du style » |
| Sub/basse | après le kick | coupe-bas 25–30 Hz ; **sidechain** déclenché par le kick ; saturation sur la couche médium seulement | ratio 3:1 à 6:1, attaque 0–10 ms, release 60–120 ms (ou synchro tempo pour « pomper ») [S37][S38] | 1 fois, piste basse (et 808) | oui, même sidechain pour nappes en house |
| Caisse claire/clap | après kick | coupe-bas 100–150 Hz ; compresseur léger ; réverb courte en envoi | envoi −12 à −18 dB | 1 fois | oui |
| Charleys/percus | après rythme | coupe-bas 300–500 Hz ; panoramique léger (±15 %) ; délai 1/8 ou 1/16 très discret | envoi délai −18 dB | 1 fois | oui |
| Nappe/accords | après basse | coupe-bas 150–250 Hz (laisse la place à la basse) ; réverb longue en envoi ; filtre qui respire (automation ±10–15 %) [S50] | envoi −6 à −12 dB | 1 fois + automation par section | oui |
| Lead | après nappe | coupe-bas 200–300 Hz ; compresseur modéré ; délai 1/8 ou 3/16 + réverb moyenne ; stéréo légère | envoi délai −9 dB, réverb −12 dB | 1 fois | oui, sur le 2e lead |
| Voix/vocal chop | fin | coupe-bas 100–150 Hz ; compresseur ; pré-délai de réverb 20–40 ms pour garder le mot clair [S39] | — | 1 fois | oui |
| Envois (bus) | dès le premier envoi | **1 réverb courte, 1 réverb longue, 1 délai** pour tout le morceau ; sur chaque retour : coupe-bas 200 Hz, coupe-haut 10 kHz ; option : réverb « duckée » par la voix/lead [S39][S40] | — | créés une fois | c'est le principe même |
| Bus batterie | quand le rythme est posé | compresseur « colle » | 10 ms / 0,1 s / 4:1, 2–3 dB de réduction [S44] | 1 fois | oui |

Règle à dire au débutant : **couper plutôt que booster** ; le coupe-bas est l'effet n° 1 de toutes les pistes sauf kick et sub [S43].

### 2.5 Étape 4 : arrangement par section (que fait chaque piste ?)
Méthode pro : **construire le drop/refrain complet d'abord, puis enlever** pour fabriquer intro, build, break (arrangement soustractif) [S41].

| Section | Longueur type | Kick | Basse | Nappe/accords | Lead | Percus/FX |
|---|---|---|---|---|---|---|
| Intro | 16–32 (club) / 8 (streaming) | seul ou filtré | absente ou sub discret | filtrée, lointaine | motif isolé, filtré | charley fermé seul, bruit/atmosphère |
| Build | 8–16 | roulement ou 4 kicks par temps à la fin | retirée ou montante | réverb qui augmente | répété, filtre qui s'ouvre | riser bruit blanc, caisse claire ×2 |
| Drop/refrain | 16–32 (DnB 32–64) | plein | pleine | pleine ou retirée (techno) | plein | tout, + crash au début |
| Break | 8–16 | absent | absente ou filtrée | seule, large | variation ou absent | atmosphères, délais qui traînent |
| 2e drop | = 1er + 1 élément | plein | variante (slide, octave) | idem | idem + contre-chant | idem |
| Outro | 16–32 (club) / 8 | seul à la fin | retirée | filtrée | absent | éléments retirés un par un |

### 2.6 Étape 5 : automation (le mouvement)
- **Build** : filtre passe-bas qui s'ouvre sur 4–8 mesures (un peu de résonance), envoi réverb qui monte, volume qui monte, riser [S50].
- **Au drop** : couper net réverb et délai juste avant pour que le drop claque ; ne pas laisser une automation retomber brutalement à zéro (clic) [S50].
- **Break** : filtre qui se ferme, envoi réverb plus haut qu'au refrain.
- **Partout** : une nappe sans mouvement devient « papier peint » ; 10–15 % de filtre sur plusieurs mesures suffit [S50].
- Fréquence : **une automation par section au minimum, pas plus de deux par piste** (sinon le mix bouge trop).

### 2.7 Étape 6 : marge, balance, mastering, export
- **Marge (gain staging)** : chaque piste commence à −10/−12 dB (kick de référence), le bus master culmine vers −6 dB avant mastering [S51][S52]. Le moteur peut l'imposer silencieusement (Endlesss).
- **Balance par rôle** (ordre de réglage pro) : kick → basse à son niveau → batterie → voix/lead → reste. Le mix « statique » (faders et panoramique seuls) doit déjà marcher avant tout effet [S36].
- **Mastering** : cible streaming ≈ −14 LUFS intégrés, plafond −1 dBTP (Logic vise exactement cela) ; club −6 à −8 LUFS ; dynamique < 3 LU = trop écrasé [S52][S53][S19].
- **Vérifications d'export** : écoute en **mono** (rien ne disparaît), sur **haut-parleur de téléphone** (le kick et la basse se devinent ?), à faible et fort volume, comparaison avec un morceau de référence du style à **volume égal**, export WAV 24 bits sans normalisation [S53][S54].

### 2.8 Spécificités par style (ce que le Guide change)
- **DnB / jump up** (170–175 BPM) : batterie simple (jump up) ; **la basse est la mélodie** ; sub + médium séparés, sub mono ; drops de 32/48/64 mesures ; intro 32 ; le 2e drop est « plus gros » (double-time, variante) [S55][S56][S57].
- **Techno** (125–140) : kick + « rumble » (envoi parallèle : réverb longue → distorsion → EQ qui ne garde que la queue grave) ; charleys hors temps à vélocité ≤ 70 % ; peu d'harmonie ; l'intérêt vient de l'automation et de la soustraction ; intro/outro longues [S58][S59].
- **House** (120–128) : intro 16–32, build 16, drop 32, break 16 ; la basse revient au drop pendant qu'on retire des éléments d'intro ; nappes et vocals au break ; sidechain audible = signature [S41][S42].
- **Trap** (130–160, ressenti mi-temps) : 808 accordé, glissé seulement vers un temps fort, kick court en couche ; charleys en triolets et roulements ; couplet 16 mesures « avec de la place pour la voix », refrain 8 avec tout ; transitions par cymbale inversée / roulement / filtre [S45][S49].
- **Pop électronique** : couplet 8–16, pré-refrain 4–8, refrain 8–16, pont 8 ; le refrain porte mélodie + accords pleins ; la cible loudness est streaming [S60].

---

## 3. Pédagogie : guider deux heures sans perdre ni ennuyer

1. **Micro-étapes de 1 à 3 minutes, une décision chacune.** Le principe de **segmentation** de Mayer (leçon découpée en morceaux à son rythme) est soutenu par 10 expériences sur 10, effet médian 0,79 ; le **pré-entraînement** (connaître les noms des pièces avant) par 13/16, effet 0,75 [S61]. Les praticiens placent les tâches simples à 2–3 min et les décisions complexes à 7–10 min : garder 1–3 min et couper les décisions complexes en deux [S62].
2. **Le glossaire sert le pré-entraînement** : avant une étape « sidechain », une carte d'une phrase « le compresseur baisse la basse quand le kick frappe, pour que les deux se partagent le grave ».
3. **Des défauts avec un « pourquoi »** : chaque proposition dit « valeur de départ : X, parce que… » (le pourquoi de iZotope/Logic, le « Info View » d'Ableton). Trois mots d'explication, un bouton « en savoir plus » pour l'approfondissement optionnel.
4. **Écouter avant d'appliquer** : bouton « écouter avec / sans » à volume égal, dans la section concernée (Logic analyse « juste la boucle » : on fait pareil).
5. **Résumé « où on en est »** à chaque ouverture et sur demande : les sections, les pistes, les cases faites/sautées, « prochaine étape recommandée », en 3 lignes lisibles à voix haute.
6. **Célébrer les jalons** sans bruit : « Le rythme tient debout » / « Première minute écoutable » / « Prêt à exporter » ; la progression visible après chaque étape augmente la rétention chez Duolingo (+1 à 3 %) [S26][S27].
7. **Pauses prévues** : au bout de ~25 min le Guide propose « bon moment pour une pause, tout est sauvé » ; la série de jours remplace la série Duolingo sans la menace de la perdre (pas de « streak » punitif).
8. **Vocabulaire pour non-musicien, en français** : « grave / médium / aigu » plutôt que Hz ; « coupe le bas » plutôt que « passe-haut » ; « la basse s'écarte quand le kick frappe » plutôt que « sidechain » (le mot technique entre parenthèses, cliquable vers le glossaire) ; « plus fort / plus doux » plutôt que dB ; « plus loin / plus près » pour la réverb ; « 8 mesures » toujours accompagné de « ≈ 15 s » au tempo du morceau. Phrases courtes, impératif doux (« Écoute… », « Choisis… »), une seule question par carte.
9. **Entrée et sortie libres** : chaque étape se fait depuis le Guide ou directement dans l'éditeur ; le Guide reconnaît ce qui a été fait sans lui (détection d'état) et coche tout seul.

---

## 4. Risques et anti-patrons

- **Wizard qui bloque** : Clippy était modal et faisait perdre la frappe ; les tutoriels modaux à étapes ne sont pas retenus [S31][S63]. → Le Guide ne bloque jamais l'éditeur ; seule la création initiale est un wizard.
- **Conseils qui harcèlent** : « l'aide contextuelle peut frustrer plus qu'aider » quand elle ignore le contexte (Clippy ne savait ni qui vous étiez ni votre but) [S30][S31]. → Un seul conseil à la fois, jamais répété s'il a été rejeté, silence quand l'utilisateur joue ou enregistre.
- **Contradire l'utilisateur** : si Benjamin a retiré la réverb d'une piste, le Guide ne la repropose pas ; il note « choix : sans réverb » et adapte la suite. Le chemin unique de Duolingo a montré la colère qu'engendre la perte de contrôle [S28].
- **Trop automatiser → tout sonne pareil** : critique récurrente des outils IA (« point de départ identique à chaque morceau », « pousse tout vers le milieu ») [S64][S65]. → Les préréglages varient par style et par ambiance, proposent trois options plutôt qu'une, et le Guide pousse à une décision personnelle par section (« qu'est-ce qui rend ce drop à toi ? »).
- **Checklist-corvée** : une liste de cases sans contexte est ignorée (Sentry) [S32]. → Chaque case est une action d'un geste avec écoute.
- **Fonctions cachées dans « avancé »** (Endlesss) [S13] → la vue détail est à un tap, jamais enterrée.
- **Louder is better** : sans compensation de volume, toute comparaison ment [S19].
- **Tour qui disparaît** : un tutoriel fermé sans moyen de le rouvrir (plainte ClickUp) [S63] → le Guide est toujours à portée (icône fixe, « Reprendre »).
- **Trop long d'un coup** : « si vous essayez d'enseigner trop, les gens se sentent dépassés » (Apple) [S33].

---

## 5. Dix principes pour le « Guide » de Pocket Studio (classés)

1. **Toujours là, jamais devant** : un panneau compagnon repliable sur chaque écran, qui ne bloque jamais l'éditeur ; le wizard modal n'existe qu'à la création.
2. **Il lit le projet** : l'état (pistes, effets, sections, automations, marge) est détecté, les cases se cochent toutes seules, et la carte « prochaine étape » en découle.
3. **Une décision par carte, 1 à 3 minutes** : une question, 2 à 3 options écoutables, une valeur de départ et son pourquoi en trois mots.
4. **Écouter avant d'appliquer, à volume égal** : avec/sans, dans la section concernée, jamais de comparaison qui ment.
5. **Quand / quel effet / combien / combien de fois / ailleurs ?** : chaque piste a sa fiche selon ces cinq colonnes, et le Guide propose explicitement de réutiliser un motif ou une chaîne.
6. **Le refrain d'abord, puis on enlève** : arrangement soustractif, grille de 8 mesures imposée, chaque section dit ce que fait chaque piste.
7. **Sauter, revenir, reprendre** : tout est optionnel, tout est retrouvable, le résumé « où on en est » accueille chaque réouverture, même des jours après.
8. **Respecter les choix** : un refus est mémorisé, jamais reproposé, et la suite s'adapte ; le Guide conseille, Benjamin tranche.
9. **Varier pour ne pas sonner pareil** : préréglages par style et ambiance, trois propositions et « autre idée », une décision personnelle demandée par section.
10. **Le moteur fait l'ennuyeux, le Guide enseigne le musical** : tempo, tonalité, marge, cible de loudness, coupe-bas de base sont automatiques et silencieux ; les mots sont ceux du quotidien, le terme technique reste au glossaire.

---

## Sources

- [S1] Engadget, « Ableton's interactive lessons teach the basics of music production » (2017) : https://www.engadget.com/2017-05-08-ableton-interactive-lessons.html
- [S2] FACT, « Ableton Learning Synths » (2019) : https://factmag.com/2019/06/25/ableton-learning-synths ; Ableton blog : https://www.ableton.com/es/blog/learn-synthesis-in-your-browser/
- [S3] Ethan Hein, « Learning music from Ableton » (DeSantis et Schaedler, 10 chapitres) : https://ethanhein.substack.com/p/learning-music-from-ableton
- [S4] Manuel Ableton Live 12, « First Steps » (Learn View, Info View, Edit Info Text) : https://www.ableton.com/en/live-manual/12/first-steps/
- [S5] HWBusters, « FL Studio Mobile: a 2024 review » : https://hwbusters.com/audio/fl-studio-mobile-a-2024-review-on-the-popular-mobile-daw/
- [S6] Apple Support, GarageBand iPhone (Live Loops, Sound Library) : https://support.apple.com/en-ae/guide/garageband-iphone/chsff8c943/ios
- [S7] Stuff, « How to master GarageBand » (Learn to Play) : https://www.stuff.tv/?p=314954
- [S8] Sound On Sound, « Elf Audio Koala Sampler » : https://www.soundonsound.com/reviews/elf-audio-koala-sampler
- [S9] TapSmart, « Koala Sampler how to » (Sample → Sequence → Perform) : https://www.tapsmart.com/tips-and-tricks/koala-sampler-how-to/
- [S10] RouteNote, « What is BandLab SongStarter » : https://create.routenote.com/blog/what-is-bandlab-songstarter/
- [S11] Jack Righteous, « BandLab AI tools explained » : https://jackrighteous.com/blogs/bandlab-guides/bandlab-ai-tools-explained-songstarter-splitter-automix-more
- [S12] MusicTech, « Review: Endlesss » : https://musictech.com/reviews/endlesss/
- [S13] Engadget, « Endlesss is a simple, fun music collaboration app » : https://engadget.com/2020-03-31-endless-music-collaboration-app.html
- [S14] iZotope, Neutron Mix Assistant : https://www.izotope.com/en/products/neutron/features/mix-assistant
- [S15] Sound On Sound, « iZotope Neutron 5 » : https://soundonsound.com/reviews/izotope-neutron-5
- [S16] Violet Recording, « iZotope Ozone 11 review » : https://violetrecording.com/izotope-ozone-11-review/
- [S17] Logic Studio Training, « Mastering Session Players » : https://logicstudiotraining.com/session-players-logic-pro/
- [S18] Apple, « Use Mastering Assistant in Logic Pro » : https://support.apple.com/HT213974
- [S19] Apple, « Mastering Assistant parameters » (−14 LUFS, −1 dBTP, Loudness Compensation) : https://support.apple.com/ar-kw/guide/logicpro/lgcpa4b810ba/mac ; Production Expert, écoute : https://www.production-expert.com/production-expert-1/how-good-is-logic-pro-mastering-assistant-take-a-listen
- [S20] MusicTech, « Splice Create revamped » : https://musictech.com/news/gear/splice-create/
- [S21] Splice Support, « How do I create a Stack using a loop » : https://support.splice.com/en/articles/8652644-how-do-i-create-a-stack-using-a-loop
- [S22] Produce Like A Pro, « Soundtrap review » : https://producelikeapro.com/blog/soundtrap-review/
- [S23] Violet Recording, « Moises app review » : https://violetrecording.com/moises-review/
- [S24] Jack Righteous, « Suno Song Editor guide » : https://jackrighteous.com/ja/blogs/guides-using-suno-ai-music-creation/suno-ai-in-song-editor-v4-replace-extend-crop-more
- [S25] Jack Righteous, « Udio edit workflow » : https://jackrighteous.com/en-gb/blogs/ai-creator-tools/udio-edit-replace-section-lyrics-workflow
- [S26] Duolingo blog, « How the Duolingo streak builds habit » : https://blog.duolingo.com/how-duolingo-streak-builds-habit
- [S27] Prototypr, « Research: in-game streaks » (A/B : +3 % DAU, +3 % rétention J14) : https://blog.prototypr.io/research-in-game-streaks-92bfb229e776
- [S28] Duolingo blog, « New home screen design » et NBC News (interview von Ahn) : https://blog.duolingo.com/new-duolingo-home-screen-design ; https://www.nbcnews.com/tech/tech-news/duolingos-update-redesign-luis-von-ahn-interview-rcna44655 ; critique : https://uxdesign.cc/down-the-wrong-path-the-disaster-of-the-latest-duolingo-ui-update-a4cdd1e6ea1c
- [S29] Loading…, « L2P NOOB: examining tutorials in digital games » : https://loading.journals.publicknowledgeproject.org/index.php/loading/article/view/109
- [S30] Andersen et al., CHI 2012, « The impact of tutorials on games of varying complexity » : https://grail.cs.washington.edu/projects/game-abtesting/chi2012/chi2012.pdf
- [S31] Alan Dix, sur Clippy (modal, perte de frappe) : https://alandix.com/blog/2024/07/ ; CommandBar, « What Clippy taught us all » : https://www.commandbar.com/blog/what-clippy-taught-us-all
- [S32] Sentry, « Growing adoption by building a modern Clippy » : https://blog.sentry.io/growing-adoption-building-modern-clippy
- [S33] Apple Human Interface Guidelines, « Onboarding » : https://developer.apple.com/design/human-interface-guidelines/onboarding
- [S34] Sound On Sound, « Using plug-ins » (ordre gate → comp → EQ → réverb) : https://www.soundonsound.com/techniques/using-plug-ins
- [S35] URM Academy, « 20 simple tricks… » (EQ avant compression) : https://urm.academy/category/blog/page/40/
- [S36] KVR forum, « basic effects questions » (inserts vs envois) : https://www.kvraudio.com/forum/viewtopic.php?p=1271865
- [S37] EDMProd, « Sidechain compression: 5 simple tips » : https://edmprod.com/sidechain-compression
- [S38] Unison, « Side-chain compression » ; Sonarworks : https://unison.audio/side-chain-compression/ ; https://www.sonarworks.com/blog/learn/sidechain-compression
- [S39] Sound On Sound, « How to optimise your reverb treatments » (coupe-bas 200 Hz, coupe-haut 10 kHz) : https://www.soundonsound.com/techniques/how-optimise-your-reverb-treatments ; Violet, pré-délai : https://violetrecording.com/how-to-add-reverb-to-vocals/
- [S40] Ask.Audio, « Self-ducking reverb » : https://ask.audio/articles/mix-tricks-self-ducking-reverb-in-logic-pro-and-pro-tools
- [S41] Mixed In Key, « How to arrange a dance music track » : https://mixedinkey.com/captain-plugins/wiki/how-to-arrange-a-dance-music-track/ ; Inspired by Beatz, « EDM arrangement: from loop to exciting track » : https://www.inspiredbybeatz.com/en/edm-arrangement-from-loop-to-exciting-track/
- [S42] KVR forum, « usual structure of a House song » : https://www.kvraudio.com/forum/viewtopic.php?p=6152552 ; Gearspace : https://gearspace.com/threads/house-music-structure.956852/
- [S43] iZotope, « EQ cheat sheet » ; Produce Like A Pro : https://izotope.com/en/learn/eq-cheat-sheet ; https://producelikeapro.com/blog/eq-cheat-sheet/
- [S44] Unison, « Drum bus compression » ; eMastered, « Glue compressor » : https://unison.audio/drum-bus-compression/ ; https://emastered.com/blog/glue-compressor
- [S45] Violet Recording, « How to make trap beats » : https://violetrecording.com/how-to-make-trap-beats/ ; Soundtrap, « Producing trap music » : https://blog.soundtrap.com/producing-trap-music/
- [S46] Bass Gorilla, « How to make trap music » : https://bassgorilla.com/how-to-make-trap-music/
- [S47] MusicRadar, « Ultimate guide to sub bass » (mono < 150 Hz, split sub/mid) : https://musicradar.com/news/ultimate-guide-to-sub-bass
- [S48] Violet Recording, « How to design a bass sound » (Reese, wobble) : https://violetrecording.com/how-to-design-a-bass-sound/ ; Armada University, kick & bass : https://www.armadamusic.com/university/music-production-articles/how-to-mix-your-kick-and-bass-5-must-know-methods
- [S49] Sample Focus, « Trap beat cheat sheet » : https://samplefocus.com/blog/make-a-trap-beat-with-fl-studio-cheat-sheet-beginners-guide/
- [S50] Waves, « 6 automation techniques » ; Unison, « Filter sweeps » ; Puremix, Fred Everything ; Beat Kitchen, « Automation and movement » : https://www.waves.com/6-automation-techniques-every-producer-needs-to-know ; https://unison.audio/filter-sweeps/ ; https://www.puremix.com/blog/filter-automation-with-fred-everything ; https://beatkitchen.io/guides/electronic-music/08-automation-and-movement/
- [S51] Elephorm, « Calibrage du kick et headroom » (kick à −12 dB) : https://www.elephorm.com/formation/audio-mao/cubase/maitrisez-cubase-pro-10/point-de-depart-pour-avoir-une-marge-de-dynamique
- [S52] Mastering The Mix, « Mixing and mastering using LUFS » et « Streaming vs clubs » : https://www.masteringthemix.com/blogs/learn/mixing-and-mastering-using-lufs ; https://www.masteringthemix.com/blogs/learn/mixing-for-streaming-platforms-vs-clubs-key-differences
- [S53] Violet Recording, « How to check your master » (−1 dBTP, LRA, encodage test) : https://violetrecording.com/how-to-check-your-master/
- [S54] Sound On Sound, « 10 essential mix checks » ; Production Expert, mono : https://www.soundonsound.com/sound-advice/10-essential-mix-checks ; https://www.production-expert.com/production-expert-1/the-best-way-to-check-mono-compatibility
- [S55] Unison, « How to make drum and bass » : https://unison.audio/how-to-make-drum-and-bass/
- [S56] Loopmasters, « How to get the perfect jump up bass » : https://www.loopmasters.com/articles/4480-How-To-Get-the-Perfect-Jump-Up-Bass-A-D-B-Bass-Tutorial
- [S57] DnB Forum, « Structure » (32/48 intro, 64 drop) et Pirate, « Jungle mixing tips » : https://dnbforum.com/threads/structure.99382/post-825811 ; https://community.pirate.com/t/jungle-mixing-tips/2378
- [S58] Future Proof Music School, « Making hard techno » (kick + rumble en parallèle) : https://futureproofmusicschool.com/blog/making-hard-techno-a-path-to-unique-sound-design
- [S59] Attack Magazine, walkthrough techno (charleys hors temps, vélocité ≤ 70 %) : https://attackmagazine.com/?p=71776
- [S60] MusicRadar, « 3 successful track arrangements for pop and EDM » ; Rhymebook, song structure : https://www.musicradar.com/how-to/3-successful-pop-and-edm-track-arrangement-structures-explained ; https://rhymebook.com/guides/song-structure
- [S61] Mayer, « Principles for managing essential processing: segmenting, pretraining, modality », Cambridge Handbook of Multimedia Learning (2014) : https://www.cambridge.org/core/books/cambridge-handbook-of-multimedia-learning/principles-for-managing-essential-processing-in-multimedia-learning-segmenting-pretraining-and-modality-principles/DD24C2F48B9B1277CE59F78276110258
- [S62] AWorld, « How long should a microlearning module last » ; Macmillan, « LSI Microlearning » : https://www.aworld.org/blog/esg/how-long-should-a-microlearning-module-last-the-answer-isn-t-always-5-minutes/ ; https://go.macmillanlearning.com/rs/122-CFG-317/images/LSI-Microlearning.pdf
- [S63] UX Mastery, « The UX of onboarding » (Paul Sherman) ; ClickUp feedback : https://uxmastery.com/transcript-ux-onboarding/ ; https://feedback.clickup.com/feature-requests/p/day-1-and-im-already-gonna-be-that-guy
- [S64] We Rave You, « Do you really need AI plugins » : https://weraveyou.com/2025/08/do-you-really-need-ai-plugins-understanding-the-pros-and-cons/
- [S65] Native Instruments community, « Overwhelming mastering results » ; RA, « MixGenius / LANDR » : https://community.native-instruments.com/discussion/48157/overwhelming-mastering-results ; https://ra.co/reviews/15518

Note de fiabilité : S1–S4, S14–S19, S33, S61 sont des sources primaires ou de référence ; les chiffres de mixage (S34–S60) viennent de tutoriels de praticiens et de forums, concordants entre eux mais à présenter toujours comme « point de départ, puis écoute ».
