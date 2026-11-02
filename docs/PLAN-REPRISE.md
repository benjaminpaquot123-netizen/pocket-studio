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
- **Le bon modèle pour la bonne tâche** : un modèle plus léger pour les tâches mécaniques (traductions, renommages, réparer un test), le modèle le plus fort pour la conception et le moteur audio. *(À valider par Benjamin.)*
- **Deux équipes en parallèle au maximum**, sur des parties qui ne se touchent pas (au-delà, la machine à 4 cœurs ralentit tout et les tests deviennent instables).
- **Build plus rapide** : cache de build déjà actif ; essai du cache de configuration de Gradle (évite de tout reconfigurer à chaque lancement), gardé seulement s'il ne casse rien.
- **Messages plus courts** de ma part : une nouvelle par étape, en 3 à 5 lignes.

## L'ordre (le plus rapide)
1. **Lot A, petit, une équipe, modèle léger** : réparer les 5 tests rouges (3 tutos avec une cible cachée, TutoScreenTest, DrumLengthTest) et le geste « glisser » de « Montre-moi ». Puis je range tout dans la version commune (avec les icônes, la tête de lecture, « Piocher »…).
2. **Carte du code** : écrite par moi pendant le lot A.
3. **Lots B et C en parallèle, modèle fort** :
   - **B, Pad Live** (maquette validée) : carte sur l'accueil, pads A à D, packs, modes de pad, effets à maintenir, enregistrer ce qu'on joue, défis.
   - **C, Remix** : nouvelle découpe validée, plus la case Remix du Live et « Garder » du Remix dans le navigateur (même zone du code, une seule équipe).
4. **Relecture neuve** : une équipe qui ne lit que les changements, pas tout le code.
5. **APK** pour Benjamin (je la compile moi-même).
6. **Audit** : les étages 0 à 4 sont surtout des commandes que je lance moi-même (peu coûteux) ; les étages 5 et 6 avec des équipes, une ou deux à la fois.

## Gains attendus (estimation, pas une mesure)
Environ un tiers à la moitié de quota en moins par lot (moins de recherche, moins de journaux, moins de captures), et 2 lots qui avancent en même temps pour B et C.

Sources : [Réduire les jetons dans Claude Code (KDnuggets)](https://kdnuggets.com/7-practical-ways-to-reduce-claude-code-token-usage) · [9 façons (Composio)](https://composio.dev/blog/ways-to-cut-token-consumption-in-claude-code) · [Astuces (DEV)](https://dev.to/vishnub33527201/tips-for-token-optimisation-in-claude-code-1kn5) · [Astuces et liens (SaltyCrane)](https://www.saltycrane.com/blog/2026/08/my-claude-code-tips-and-links/) · [Accélérer Gradle (ProAndroidDev)](https://proandroiddev.com/speeding-up-your-gradle-builds-caching-parallel-execution-and-incremental-builds-b70fd9ec391b) · [Temps de build (Glovo)](https://medium.com/glovo-engineering/accelerate-your-android-development-essential-tips-to-minimize-gradle-build-time-part-ii-of-ii-b74f5d505982) · [Cache de configuration (Bitrise)](https://bitrise.io/blog/post/accelerate-your-gradle-builds-with-gradle-config-cache) · [Robolectric instable en parallèle (Bitrise)](https://discuss.bitrise.io/t/robolectric-tests-have-become-flaky/26010)
