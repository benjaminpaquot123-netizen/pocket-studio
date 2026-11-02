# Plan de reprise, audité pour aller vite en économisant le quota (2026-11-02)

Relecture du plan sous 3 regards (gestion d'agents IA, build Android, responsable qualité), avec les astuces des pros trouvées en ligne. Ce sont des conseils de la communauté, pas de règles officielles.

## Ce qui coûte le plus aujourd'hui (constaté)
1. **Les équipes qui cherchent dans le code** : chacune relit les mêmes fichiers pour se repérer.
2. **Les longs journaux de tests** lus par les équipes, et les suites complètes relancées plusieurs fois par lot.
3. **Les captures** : jusqu'à 195 réenregistrées pour un seul changement de couleur.
4. **Les allers-retours** quand une consigne est floue (« partout » sans liste).
5. **Mes propres messages** longs à chaque notification.

## Les astuces retenues
- **Carte du code (une fois)** : un fichier `docs/CARTE-CODE.md` qui dit où est quoi (écrans, modèle, moteur, tests). Chaque consigne pointe vers les fichiers exacts : les équipes ne cherchent plus.
- **Consignes précises et fermées** : la liste exacte des endroits à changer, les tests qui prouvent que c'est fini, et ce qu'il ne faut pas toucher.
- **Les tests longs, c'est moi qui les lance** (une commande, presque aucun coût) : les équipes ne lancent que leurs tests ciblés et ne lisent que le résumé (jamais le journal entier).
- **Une seule grande vérification**, au moment de ranger dans la version commune, et pas une par équipe.
- **Captures ciblées** : seulement les écrans touchés ; je regarde 2 ou 3 captures, pas toutes.
- **Le modèle** : toujours le plus fort (choix 1B de Benjamin, 2026-11-02) ; l'économie vient des consignes fermées, pas d'un modèle plus léger.
- **Deux équipes en parallèle d'office, jamais plus**, sur des parties qui ne se touchent pas (au-delà, la machine à 4 cœurs ralentit tout et les tests deviennent instables). Seule exception : les sièges d'un tour de testeurs de l'Audit (AUDIT §6), qui tournent seuls sur la machine.
- **Build plus rapide** : cache de build déjà actif ; essai du cache de configuration de Gradle (évite de tout reconfigurer à chaque lancement), gardé seulement s'il ne casse rien.
- **Messages plus courts** de ma part : une nouvelle par étape, en 3 à 5 lignes.

## L'ordre (corrigé le 2026-11-02 après relecture) : la seule liste à suivre
« continue » = la première case non cochée ci-dessous. Chaque étape a son **test de sortie** : tant qu'il n'est pas vrai, l'étape n'est pas finie. Cocher ici et dans `docs/PROGRESS.md` au fil de l'eau.

- [ ] **A1. Lot A sur fx-fin** (une équipe). Réparer les 5 tests rouges du passage complet fin-8 : `TutoJourneyTest` (techno Pro : `synth_knob_8/9` absents ; acid techno et acidcore Facile : `mix_strip_20` absent du Mix simple), `TutoScreenTest.theGridShowsTheBarOfTheDottedCellsLeft`, `DrumLengthTest` ; et le geste « glisser » de « Montre-moi » (n° 20). **Sortie** : ces 5 tests verts, `TutoJourneyTest` complet vert, un test qui voit le doigt glisser ; si `DrumLengthTest` échoue aussi sur fx-int, la cause est corrigée dans le même lot.
- [ ] **A2. Rangement fx-fin → fx-int** (moi). Fusion ; conflits des docs (DECISIONS, REVIEW, PROGRESS, CLAUDE.md) : garder les deux côtés, rien de supprimé. Puis la grande vérification, une seule fois : `./scripts/host-tests.sh`, `:app:testDebugUnitTest`, `:app:verifyRoborazziDebug`. **Sortie** : zéro test rouge, poussé, CI verte ; REVIEW n° 18 à 23 marqués faits avec leurs restes. **B et C ne partent qu'après A2** (ils ont besoin du navigateur unique de 7d et des icônes haut-parleur/casque).
- [ ] **A3. Carte du code** (moi, commencée pendant A1, finie sur fx-int après A2) : `docs/CARTE-CODE.md`, une ligne par zone, et pour B et C la liste exacte des fichiers qu'ils peuvent toucher. **Sortie** : chaque fichier des consignes B, C, D, E y figure ; aucun fichier partagé entre B et C hors de la liste du § « Fichiers partagés ».
- [ ] **B. Pad Live (n° 25)**, branche à part, maquette `docs/maquettes/padlive-trois.png`, en trois sous-lots rangés chacun dans fx-int :
  - **B1 le cœur** : carte « Pad Live » sous « Remix » sur l'accueil → projet « Live » créé ou repris (dans Mes projets), Live en plein écran ; pads prêts à jouer ; niveau simple (pads, tempo, 3 effets, « Plus ») ; pages A à D ; modes de pad (coup, boucle, tenir, groupes « un seul à la fois », réglages par pad) ; packs par style (Techno, Trap, Fête…) ; règle de design : rien ne bouge sur un pad ou une case inactifs (retirer l'onde des points vides de la grille des briques), lumière intérieure, anneau d'avancement, onde de 200 ms au doigt, voile pour l'effet maintenu. **Sortie** : sur le banc testeur, premier son en moins de 5 s et premier rythme qui tourne en moins de 30 s depuis l'accueil, aucune fonction à plus de 3 touchers (mesurés, chiffres dans la note de suivi) ; sauvegarde → réouverture et annulation testées ; captures des seuls écrans touchés.
  - **B2 la scène** : effets à maintenir (Filtre, Écho, Bégaiement, Montée, Chute, Roulement en 1/4, 1/8, 1/16 et triolets) ; lancer toute une ligne de briques (départ à la mesure) ; « Enregistrer ce que je joue » → un pad ou une case ; tap tempo ; horloge MIDI en entrée et en sortie. **Sortie** : bancs hôtes à l'échantillon près et sans allocation ; prise → annuler, sauvegarde, export ; tests MiniLab/MIDI de chaque nouveau geste.
  - **B3 le jeu et le partage** : défis « refais ce rythme », lumières qui suivent le son, mode « Fête » plein écran, export de la performance (audio et pistes séparées), accessibilité (TalkBack, vibration, gros pads). Vidéo de l'écran et Ableton Link : une demi-journée d'essai au plus chacun ; si ce n'est pas propre, noté « Idée » dans REVIEW et DECISIONS, sans bloquer. **Sortie** : passage de 3 personas (enfant, DJ, débutant) sur le banc, sans Bloquant ni Grave.
- [ ] **C. Remix** (en parallèle de B), branche à part, maquette `docs/maquettes/decoupe-deux.png` : nouvelle découpe (n° 24 : coupes auto Phrases/Attaques/Mesures, « Couper ici », coupe déplacée au doigt avec loupe et aimant, bouts numérotés et colorés, écoute d'un toucher, envoi vers pads / Live / Ma bibliothèque / piste, annulation) ; « Garder » du Remix dans le navigateur unique (reste 7d) ; reste du n° 23 : hauteur de la voix en notes vérifiée sur la vraie voix séparée. **Sortie** : tests avec la chanson de référence (ignorés, pas rouges, si la copie locale a disparu) ; la voix en notes suit la voix à ±1 demi-ton sur la plupart des notes, sur toute la chanson ; captures des écrans touchés.
- [ ] **D. Restes** (après B, une équipe ; en parallèle de E seulement si la carte du code montre des fichiers séparés) : case Remix du Live (un passage de la chanson par case, évolution du moteur, sur la grille de B, d'où « après B ») ; MIDI vers la 303 pendant l'enregistrement d'une case (n° 9) ; anciens écrans branchés comme le nouveau Live (reste du n° 11 : prise, annulation, sauvegarde, export, MIDI) ; lumières des pads du MiniLab si faisable sans l'appareil (sinon dans la liste de Benjamin). **Sortie** : un test de bout en bout par branchement ajouté ; bancs hôtes sans allocation pour la case Remix.
- [ ] **E. Lot « Master pro »** (PLAN-AUDITS-FINAUX §1 bis, exigé AVANT le tour 1 de l'Audit) : cibles et plafonds, chaîne du master réglable, A/B à volume égal, référence, mesures pro et rapport, dither, fondus, plage, métadonnées, stems pros. **Sortie** : la matrice des exports du §1 bis passe (LUFS ±0,5, true peak sous le plafond après encodage, nullité des stems sous −60 dB, fichiers lus par ffmpeg) ; Mode simple inchangé (3 choix).
- [ ] **F. Relecture neuve** (une équipe qui n'a rien écrit) des seuls changements depuis A1 : callback audio, sauvegarde, plantages, fuites, tests qui testent. **Sortie** : graves corrigés, le reste dans REVIEW, résumé de 3 lignes à Benjamin.
- [ ] **G. Gel du code et version d'audit** (moi) : changelog, `recette.json` réécrite, APK release signée (demandée par Benjamin le 2026-10-31, étape 9), étiquette `audit-1` = point de gel. Envoi à Benjamin avec **sa liste, moment 1** (ci-dessous). **Sortie** : CI verte, zéro test rouge, APK installée sur son téléphone.
- [ ] **H. Préparation de l'Audit** (pendant que Benjamin teste, sans attendre son signal ; rien ne change dans l'appli) : AUDIT §3 (projets de référence, sons maîtres rendus depuis `audit-1`, matrice d'appareils, pseudo-traduction, instruments, gabarits). **Sortie** : tout le §3 coché.
- [ ] **I. Audit** (`docs/audits/AUDIT.md`), au signal de Benjamin : étages 0 à 7 dans l'ordre, critères d'arrêt du §6. Le test fermé du Play Store (12 testeurs, 14 jours) démarre dès que les étages 0 à 5 sont verts, pour que ses 14 jours courent pendant les tours de l'étage 6.
- [ ] **J. Fin** : dernière relance complète, PR vers main, Release. Publication sur le Play Store seulement à la demande de Benjamin.

## Fichiers partagés entre deux équipes (règles anti-conflit)
- **Docs** (DECISIONS, REVIEW, PROGRESS) : les équipes n'y écrivent pas ; chacune tient sa note de suivi `docs/lots/<lot>.md` (fait, en cours, prochaine sous-étape, décisions à recopier) ; je recopie au rangement.
- **Textes et traductions** : chaque lot a son propre fichier `project-model/src/main/resources/i18n/<langue>/<lot>.json` (comme `fin8.json`), jamais les fichiers des autres.
- **`AppRoot.kt` et l'accueil** : seul B les touche ; **la grille du Live** : B, puis D ; **le moteur** : B2 et D seulement, jamais en même temps ; **l'export et le master** : E seulement.
- **Rangement** : la première équipe finie est rangée d'abord ; l'autre reprend fx-int dans sa branche avant sa dernière vérification.

## Risques et parades
- **Conteneur redémarré** : un commit à chaque morceau vert et un push de fx-int à chaque rangement ; la note de suivi dit où reprendre ; les maquettes validées sont dans `docs/maquettes/` (plus seulement dans le scratchpad) ; la chanson de test n'est jamais dans le dépôt : si elle manque, les tests qui en ont besoin sont ignorés et la demande va dans la liste de Benjamin.
- **Limite de quota de la semaine en plein lot** : sous-lots courts (une demi-journée environ) toujours rangés avant le suivant ; ne pas lancer un gros sous-lot (B2, D, E) quand la limite approche, finir et ranger ce qui est commencé ; au retour, la note de suivi suffit.
- **Disque** : une 2e équipe seulement au-dessus de 5 Go libres ; supprimer les ateliers déjà fusionnés (`/home/user/fx-*`) et leurs dossiers de build avant chaque lot.
- **Tests instables** : relancés une fois ; s'ils passent, notés « instable » dans la note de suivi et réparés avant G (jamais mis de côté).
- **Équipe qui tourne en rond** : la surveillance de CLAUDE.md ; au 2e échec identique, je recadre ou je découpe.

## Ce que Benjamin fera (une seule liste, deux moments)
**Moment 1, avec l'APK d'audit (étape G)** :
1. Installer l'APK et dire « Audit » quand il veut qu'on lance l'Audit.
2. Sur le téléphone : OboeTester (latence, glitchs) ; 30 min de Live et de Pad Live au MiniLab 3 (pads, touches, potards, lumières) ; un casque Bluetooth ; une trace Perfetto (guide fourni) ; écouter une boucle et la chanson étirées en changeant le tempo ; écouter la voix en notes.
3. Créer le compte Google Play Console (25 $, une fois) et dire oui pour recruter les 12 testeurs (messages prêts en français et en anglais ; rien posté sans son accord).
4. Si on le lui demande : renvoyer la chanson de test « ADÉLA - Boys » et son projet Acidcore (seulement si le conteneur les a perdus).

**Moment 2, à l'étage 7 de l'Audit** : le bug bash d'une heure avec sa fille et son ami, le journal de 3 jours, et suivre le test fermé du Play Store.

## Gains attendus (estimation, pas une mesure)
Environ un tiers à la moitié de quota en moins par lot (moins de recherche, moins de journaux, moins de captures), et 2 lots qui avancent en même temps (B et C, puis D et E si possible).

Sources : [Réduire les jetons dans Claude Code (KDnuggets)](https://kdnuggets.com/7-practical-ways-to-reduce-claude-code-token-usage) · [9 façons (Composio)](https://composio.dev/blog/ways-to-cut-token-consumption-in-claude-code) · [Astuces (DEV)](https://dev.to/vishnub33527201/tips-for-token-optimisation-in-claude-code-1kn5) · [Astuces et liens (SaltyCrane)](https://www.saltycrane.com/blog/2026/08/my-claude-code-tips-and-links/) · [Accélérer Gradle (ProAndroidDev)](https://proandroiddev.com/speeding-up-your-gradle-builds-caching-parallel-execution-and-incremental-builds-b70fd9ec391b) · [Temps de build (Glovo)](https://medium.com/glovo-engineering/accelerate-your-android-development-essential-tips-to-minimize-gradle-build-time-part-ii-of-ii-b74f5d505982) · [Cache de configuration (Bitrise)](https://bitrise.io/blog/post/accelerate-your-gradle-builds-with-gradle-config-cache) · [Robolectric instable en parallèle (Bitrise)](https://discuss.bitrise.io/t/robolectric-tests-have-become-flaky/26010)
