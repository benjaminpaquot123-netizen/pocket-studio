# Plan « Le Guide » : un morceau pro, pas à pas, même en deux heures (2026-10-24)

Demande de Benjamin : « Il faut clairement améliorer l'assistant pour que chaque étape de chaque piste soit scrutée et guidée. Chaque son : quand, avec quel effet, combien de fois, si on veut l'utiliser ailleurs. Il faut pouvoir réaliser un morceau pro avec un guide pas à pas même si ça prend deux heures et plus ; le guide doit toujours être présent, savoir où on en est, ce qu'on peut faire et ce qu'on veut faire pour la suite. » Et : « Pour une piste, intégrer plusieurs sons avec des effets et tout ce qui va avec, facilement, et pouvoir y revenir si on change d'avis plus tard. »

Sources : son projet « Jump up DnB » (`scratchpad/benj-projets`, gardé comme modèle de test), le diagnostic (`scratchpad/guide/diagnostic.md`), la recherche sur 18 produits et la liste de contrôle pro (`scratchpad/guide/recherche.md`, 65 sources), `docs/hits/*.md`, `docs/PLAN-SON-PRO.md`.

## 1. Le diagnostic, en cinq constats

1. **L'assistant fait beaucoup, en silence.** Sur le morceau de Benjamin, il a posé 25 effets (growl, OTT, écrêteur et coupe-bas sur les basses ; gros son, chorus et écho ping-pong sur le lead ; élargisseur et filtre sur la nappe ; punch sur kick et caisse ; compresseur, saturation et écrêteur sur le bus batterie ; OTT, compresseur et saturation au master), 2 bus (réverbe plate, écho 1/8), un vrai sidechain sur le kick avec un bus « Musique », les graves en mono, et 10 courbes (filtres, fondu vers la Basse 2 au Drop 2, lead coupé au Drop 1, réverbe qui monte en fin de phrase). Il n'en a jamais dit un mot : l'assistant n'a aucune étape effets, mixage, automation ou arrangement, et le guide du morceau n'a qu'une ligne sur les filtres. Benjamin a donc raison : pour lui, il n'y a pas eu d'effets.
2. **Le morceau reste en deçà d'un vrai jump up.** Les trois Drops sont identiques (la même boucle de 8 mesures répétée 96 mesures), la basse ne varie pas et ne change pas avant le Drop 2, le lead-réponse est trop court, la nappe joue pendant tout le Drop, le Sub, la Voix et le Sub drop n'ont aucun effet, tous les faders sont à 0 dB et rien n'est panné. Le banc mesure Drop − Montée à −2,1 LU sur ce projet.
3. **Une anomalie à corriger tout de suite** : les courbes font 216 points pour un morceau de 184 mesures. Le morceau a été raccourci (intro et outro 32 → 16) et les courbes semblent décalées de 16 mesures (le passage à la Basse 2 écrit à la mesure 96 alors que le Drop 2 est à la 80). Piste : `PartIntents.withPartBars` / `lengthenPart`. À reproduire sur son projet, corriger, tester.
4. **Rien ne survit.** Treize surfaces guident un peu (assistant, « Ton morceau est prêt », guide du morceau, prochaines étapes, Missions, « ? », lexique, fiche de partie, « Ce qui joue », presets conseillés, « Prêt à partager ? »…), mais aucune ne sait où on en est : l'assistant ne se reprend pas (« Tes choix seront perdus »), le guide du morceau meurt avec l'écran, seules deux cases sont mémorisées. Aucun champ de guide dans le projet.
5. **Rien ne regarde le projet.** Les prochaines étapes lisent le projet avec des critères binaires (des coups ? une piste grave ? trois parties ?). Rien ne détecte « nappe partout », « basse sans variation », « trois Drops identiques », « rien de panné », « piste sans coupe-bas ». Et l'avant/après n'existe que dans le lexique, sur des exemples génériques, jamais sur le projet ouvert.

## 2. Les dix principes (tirés de la recherche, classés)

1. **Toujours là, jamais devant** : un panneau compagnon repliable sur chaque écran, qui ne bloque jamais l'éditeur. Le pas-à-pas modal n'existe qu'à la création.
2. **Il lit le projet** : l'état (pistes, sons, effets, sections, courbes, niveaux, marge) est détecté ; les cases se cochent toutes seules, et la carte « prochaine étape » en découle.
3. **Une décision par carte, 1 à 3 minutes** : une question, 2 ou 3 options écoutables, une valeur de départ et son pourquoi en trois mots.
4. **Écouter avant d'appliquer, à volume égal** : avec / sans, dans la partie concernée. Une comparaison plus forte ment toujours.
5. **Quand / quel effet / combien / combien de fois / ailleurs ?** : chaque piste a sa fiche selon ces cinq colonnes, et le Guide propose de réutiliser un motif, un son ou une chaîne.
6. **Le refrain d'abord, puis on enlève** : arrangement par soustraction, grille de 8 mesures, chaque partie dit ce que fait chaque piste.
7. **Sauter, revenir, reprendre** : tout est optionnel, tout se retrouve ; un résumé « où on en est » accueille chaque réouverture, même des jours après.
8. **Respecter les choix** : un refus est mémorisé et jamais reproposé ; le Guide conseille, Benjamin tranche.
9. **Varier pour ne pas sonner pareil** : réglages par style et ambiance, trois propositions et « autre idée », une décision personnelle demandée par partie.
10. **Le moteur fait l'ennuyeux, le Guide enseigne le musical** : tempo, tonalité, marge, cible de volume, coupe-bas de base sont automatiques et silencieux ; les mots sont ceux du quotidien, le terme technique reste au lexique.

## 3. Ce que Benjamin verra : « Le Guide »

### 3.1 Le panneau compagnon
- Une pastille **Guide** fixe en bas à droite de tous les espaces (Structure, Sons, Mix, Live, Plus) et des 4 onglets du Mode simple. Un toucher ouvre un panneau qui prend le tiers bas de l'écran (la moitié sur tablette), repliable d'un geste, jamais par-dessus ce qu'on édite.
- Le panneau a trois zones : **Où on en est** (une ligne : « Jump up DnB · 4:14 · 7 pistes · étape 12/34 : la basse au Drop 2 »), **La carte** (la prochaine décision), **Le plan** (la liste repliée des étapes, faites ✓, sautées ⟶, à revoir ↺).
- Il se tait pendant qu'on joue au clavier, qu'on enregistre ou qu'on est en Live.

### 3.2 La carte
Une carte = une décision, 1 à 3 minutes :
- un titre en mots simples (« La basse s'écarte quand le kick frappe ») ;
- une phrase de pourquoi (« sinon les deux se battent dans le grave, surtout sur un téléphone ») ;
- 2 ou 3 propositions prêtes à écouter (« Léger », « Normal », « Qui pompe ») avec ▶ chacune, à volume égal, dans la partie concernée ;
- **Appliquer**, **Sauter** (mémorisé), **Plus tard**, **En savoir plus** (le lexique, avec l'exemple sonore) ;
- un lien « Voir le réglage » qui ouvre l'écran exact, le contrôle mis en lumière (le « ? » qui étiquette sait déjà le faire).

### 3.3 La fiche de piste (les cinq colonnes)
Depuis le nom d'une piste, partout : **sa fiche**.
- **Son** : le son en place, les couches (voir 3.4), les sons essayés et écartés, « Autre idée ».
- **Quand** : dans quelles parties la piste joue (ligne de 46 cases cliquables), ce que le style conseille.
- **Quel effet et combien** : la chaîne, dans l'ordre pro (coupe-bas → compresseur → couleur → égaliseur → envois), chaque effet avec sa valeur de départ, son pourquoi, « écouter sans », et un seul curseur « plus / moins ».
- **Combien de fois** : « cette chaîne est la chaîne Kick du style, appliquée une fois pour tout le morceau » ; les courbes qui bougent, partie par partie.
- **Ailleurs ?** : « réutiliser ce motif au Drop 2 », « copier cette chaîne sur la Basse 2 », « garder ce son dans mes favoris ».
- **Historique** : la liste datée des choix sur cette piste (gardé / écarté / modifié), chacun ramenable en un geste.

### 3.4 Les couches (la demande de Benjamin)
- Une piste peut empiler jusqu'à 4 **couches** : un son chacune, avec son volume, sa place dans la stéréo, sa plage de notes (le sub en bas, le médium au-dessus), ses propres effets, et son accordage. Le moteur les joue comme une seule piste (mêmes notes), donc rien ne change dans l'édition des notes.
- Le Guide propose une couche seulement pour combler un manque entendu : « plus de claquant » (une attaque), « plus de corps » (un médium), « plus d'air » (un aigu), « un sub propre sous la basse ». Jamais deux sons pour le même rôle dans la même bande.
- Chaque couche s'écoute seule, avec les autres, sans ; se coupe ; se retire ; revient depuis l'historique. Rien n'est détruit.
- Les basses de bass music naissent déjà en deux couches (sub mono + médium large) : la fiche le montre.

### 3.5 Le parcours (le « plan » d'un morceau)
Environ 30 à 40 cartes, en 7 étapes, générées depuis le style (un fichier de données par style, pas du code) :
0. **Le cadre** (5 min) : style, tempo, tonalité, durée, destination (téléphone / streaming ou club), grille de 8 mesures. Le moteur fixe le reste.
1. **Les sons et les motifs**, piste par piste dans l'ordre pro : kick → caisse/clap → charleys → basse → accords/nappe → lead → voix/FX. Quatre questions par piste : quel rôle, quel son, quel motif, où il joue.
2. **Les couches**, avec parcimonie (3.4).
3. **La chaîne d'effets par rôle** : l'assistant pose la chaîne du style (il le fait déjà), et le Guide **la montre et la fait écouter** effet par effet : « voilà ce que j'ai mis sur la basse et pourquoi ; écoute sans ; garde, règle, ou retire ».
4. **L'arrangement** : le Drop complet d'abord, puis on enlève ; chaque partie dit ce que fait chaque piste ; les trois Drops doivent différer (une couche, un switch de basse, un contre-chant) ; les coupures avant le Drop ; « qu'est-ce qui rend ce Drop à toi ? ».
5. **Le mouvement** : une courbe par partie au moins, deux par piste au plus ; filtre qui s'ouvre, réverbe qui monte, coupure nette avant le Drop.
6. **Niveaux et master** : la balance par rôle (kick, basse, batterie, lead, reste), les pans, la cible du style, le master honnête, et les vérifications : mono, haut-parleur de téléphone, volume égal avec une référence.
7. **Exporter et partager** : le rapport « prêt à partager », le M4A, la vidéo.
Chaque étape se fait depuis le Guide **ou** directement dans l'éditeur : le Guide reconnaît ce qui a été fait sans lui et coche tout seul.

### 3.6 Reprendre, des jours après
- « Que veux-tu faire ? » → Continuer → le Guide dit en trois lignes : « Tu en étais à la basse du Drop 2. Fait : les sons, les chaînes, l'arrangement. Reste : le mouvement, les niveaux, l'export. On reprend ? »
- Tout est dans le projet, sauvegardé avec lui, exporté avec lui dans le zip, restauré avec une version.
- Au bout de 25 minutes : « bon moment pour une pause, tout est sauvé ». Pas de série à ne pas perdre.

### 3.7 En Mode simple
Le même Guide, avec moins de cartes (les étapes 1, 4, 6 et 7, les autres repliées sous « Aller plus loin ») et les mots du quotidien : « coupe le bas », « la basse s'écarte quand le kick frappe », « plus loin / plus près » pour la réverbe, « 8 mesures ≈ 11 s » au tempo du morceau.

## 4. Comment c'est construit

### 4.1 Le modèle : `GuideState` dans le projet
- Un champ `guide` dans `Project` (omis quand vide, JSON tolérant) : la version du parcours, l'étape courante, pour chaque carte son état (à faire / faite / sautée / plus tard / faite ailleurs), la date, le choix retenu, les refus mémorisés ; par piste : l'historique des choix (son, couche, effet, valeur, courbe) avec la valeur d'avant pour revenir ; les notes libres de Benjamin.
- Les couches : `Track.layers` (son, gain, pan, plage de notes, accordage, inserts), 4 au plus, omises quand il n'y en a qu'une. Le moteur reçoit une couche comme une voix de plus sur la même piste (préallocation, budget CPU vérifié) ; l'export et la vidéo les rendent pareil.
- Le lot de données « parcours » : un fichier par style (`guides/<style>.json`) : les cartes, dans l'ordre, avec pour chacune le rôle visé, les détecteurs qui la cochent, les propositions (preset + valeur + pourquoi), les mots du Mode simple, les traductions EN/NL.

### 4.2 Le cerveau : `GuideAnalyzer`
- Lit le projet (hors fil audio) et produit des **faits** : « nappe joue dans 46/46 parties », « basse : 8 répétitions identiques au Drop », « Drop 1 = Drop 2 = Drop 3 », « lead sans écho », « 6 pistes sans pan », « Sub sans coupe-bas », « courbes décalées », « Drop − Montée −2,1 LU », « marge au master 0 dB ».
- Réutilise ce qui existe : `whatActs`, `MelodyMetrics` M1–M20, `MelodyRules`, `DrumHumanizer.Feel`, `ChordTrack.guess`, `songSections` / `SongShape`, `FxPresets.suggestedFor`, `shareLevels`, le compteur LUFS du moteur, `OfflineExport.renderToArray` pour l'A/B.
- Les faits cochent les cartes et choisissent la prochaine (la première non faite dont les conditions sont réunies, dans l'ordre du parcours ; une alerte grave passe devant).
- Déterministe et testé sur les 37 styles × 4 ambiances : pour un morceau de l'assistant non modifié, le parcours est à 40 % fait et la première carte est « voilà ce que j'ai mis ».

### 4.3 L'écoute avant d'appliquer
- Une proposition se rend hors-ligne sur la partie visée (8 mesures), avec et sans, **égalisée en volume** (même LUFS court terme), et se joue par le lecteur d'aperçu ; en direct quand c'est possible (bypass d'un insert, solo d'une couche).
- Les écoutes passent par le banc « écoute en direct » dans les tests, comme tout le reste.

### 4.4 L'interface
- `GuidePanel` (compose) branché dans `AppRoot` sous le contenu de chaque espace, au-dessus de la barre des espaces ; `GuideCard`, `TrackSheet` (la fiche), `LayersEditor` ; les repères de contrôles (`SpotRegistry`) pour « Voir le réglage ».
- Les Missions deviennent le premier parcours (« Mon premier morceau » = le Guide en 6 cartes). Les prochaines étapes, le guide du morceau et « Ton morceau est prêt » fusionnent dans le Guide : moins de surfaces, une seule voix.

### 4.5 Mesure et critères d'acceptation
- **Gestes** : appliquer une carte = 2 gestes ; écouter une proposition = 1 ; ouvrir la fiche d'une piste = 1 ; ajouter une couche conseillée = 2 ; revenir sur un choix = 2 (fiche › historique › Ramener).
- **Couverture** : sur les 37 styles, chaque piste de l'assistant a une fiche complète (5 colonnes remplies, chaque effet avec un pourquoi) ; un morceau de l'assistant non modifié présente 12 à 20 cartes « à décider » ; le parcours complet fait 30 à 40 cartes.
- **Reprise** : fermer, tuer l'appli, rouvrir 3 jours plus tard (horloge simulée) : même étape, même résumé, rien de perdu ; exporter le zip puis l'importer : le Guide suit.
- **Son** : un morceau suivi jusqu'au bout du parcours passe le banc avec Drop − Break ≥ +4, trois Drops différents (métrique « variation entre Drops » à ajouter au banc), aucune piste sans coupe-bas sauf kick et sub, marge respectée.
- **Jamais bloquant** : aucune carte n'est modale ; tous les tests de budget de gestes des espaces restent verts avec le Guide ouvert.
- **Réalisme** : parcours complet joué sur l'émulateur (création → Guide 10 cartes → export), et par les 8 testeurs à l'audit.

## 5. Les chantiers, dans l'ordre

| # | Chantier | Contenu | Effort |
|---|---|---|---|
| G0 | **Corriger ce que le diagnostic a trouvé** | Courbes décalées de 16 mesures quand le morceau est raccourci ; trois Drops identiques (variation obligatoire : couche, switch de basse, contre-chant, mute) ; Sub / Voix / Sub drop sans effet ; nappe retirée au Drop 1 en bass music ; pans de départ par rôle. Tests sur le projet de Benjamin. | M |
| G1 | **Modèle et cerveau** | `GuideState` dans le projet, `Track.layers` (modèle + moteur + export), `GuideAnalyzer` et ses faits, le format de parcours en données, les 37 parcours (générés depuis les recettes, puis relus style par style). Tests sur 37 × 4. | L |
| G2 | **Le panneau et la carte** | `GuidePanel`, `GuideCard`, « où on en est », le plan replié, « Voir le réglage », écoute avec / sans à volume égal, Sauter / Plus tard / refus mémorisés, Mode simple. Captures phone / 130 % / tablette. | L |
| G3 | **La fiche de piste et les couches** | `TrackSheet` à cinq colonnes, `LayersEditor`, historique par piste et retour en arrière, « ailleurs » (réutiliser un motif, copier une chaîne, favoris). | L |
| G4 | **Les étapes 3 à 6 en cartes** | « Voilà ce que j'ai mis » effet par effet, arrangement par soustraction, mouvement, niveaux et vérifications (mono, téléphone, référence à volume égal). | M |
| G5 | **Fusion des anciennes surfaces** | Missions, prochaines étapes, guide du morceau et « Ton morceau est prêt » absorbés par le Guide ; lexique relié aux cartes (pré-entraînement) ; le **Remix** (séparateur de pistes, très apprécié) mis en avant : une entrée dans « Que veux-tu faire ? », une carte « Remixer un morceau » et une place visible dans Sons et en Mode simple. | M |
| G6 | **Vérification** | Parcours complet sur émulateur, test de reprise à 3 jours, métrique « variation entre Drops » au banc, revue neuve, 8 testeurs. | M |

Durée estimée : G0 une demi-journée ; G1 à G3 en parallèle, une journée ; G4 à G6 une journée. Soit deux à trois jours d'agents, hors audits.

## 6. Articulation avec le reste

- **L'audit d'abord** (demande de Benjamin) : le gros audit (pros + 8 testeurs, écoute en direct) tourne sur l'appli telle qu'elle est (APK 181 + lot de bugs), avant toute modification du son. Les chantiers son (basses qui tapent, leads pros) restent sur leurs branches et se fusionnent après, pour comparer.
- **Le Guide n'attend pas l'audit** : G0 et G1 commencent tout de suite ; G0 corrige des bugs, G1 ne change pas le son. G2 à G5 arrivent après l'audit avec les basses et les leads, pour que la deuxième écoute compare tout d'un coup.
- Pas d'APK sans la demande de Benjamin ; la règle « écoute en direct » s'applique à toutes les écoutes du Guide.
- **Le Remix en avant** (demande du 2026-10-24) : le séparateur de pistes est une des fonctions les plus appréciées ; il n'apparaît aujourd'hui ni dans « Que veux-tu faire ? » ni dans les espaces. Dès le lot de bugs fusionné : entrée au lancement et dans Sons, puis la carte du Guide (G5). L'audit vérifie qu'on le trouve en deux gestes.

## 7. Ce que Benjamin aura à faire

- Rien avant la fin de G3. Ensuite : suivre le parcours complet sur un style qu'il aime, dire où le Guide parle trop, pas assez, ou faux, et où il a eu envie de faire autrement.
- Optionnel : dire quelle basse roulante de sa vidéo lui plaît le plus, pour la mettre en avant dans les cartes « motif de basse ».

## 8. Décisions prises en attendant (modifiables)

- Panneau compagnon plutôt que nouvel assistant modal ; une seule voix pour tout le guidage.
- Les parcours sont des données par style, générées puis relues, pas 37 fois du code.
- 4 couches par piste au plus, avec un budget CPU vérifié ; l'édition des notes reste commune aux couches.
- L'état du Guide vit dans le projet, pas dans les réglages de l'appli.
- Les valeurs des effets sont des « points de départ, puis écoute », jamais présentées comme des lois.
