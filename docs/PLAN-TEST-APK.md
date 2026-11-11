# Plan test APK (demande de Benjamin, 2026-11-11)

Statut : **plan seulement, rien n'est construit**. À lancer quand Benjamin dit « lance le plan test apk ». Il passe **avant** le grand audit final (`docs/PLAN-AUDIT-FINAL.md`), pour que les auditeurs partent de mesures prises sur le vrai téléphone.

## Le but

Une appli à part, « Pocket Studio Test », qui teste presque tout **toute seule** sur le téléphone et la tablette. Benjamin en fait le moins possible : il lance, il pose le téléphone, il renvoie un fichier. Il ne valide lui-même que ce qu'aucune machine ne peut juger.

## Faisabilité : oui

Presque tout existe déjà, il faut surtout l'assembler :

| Brique déjà dans le projet | Ce qu'elle apporte au test APK |
|---|---|
| Le robot des parcours (`app/src/sharedTest/.../journey/Journey.kt`, `JourneyTest`, `EnduranceTest`, `RealUse`) | Il touche les vrais boutons de la vraie appli. Aujourd'hui il ne tourne que sur l'émulateur de la CI. |
| « Signaler un bug » (`report/`) | Capture d'écran, journal des gestes, zip à partager. |
| Les mesures du son (`HealthMonitor`, 4 par seconde) | Décrochages, charge du fil audio, niveau, NaN. |
| La mesure de latence haut-parleur → micro | Le téléphone sait déjà s'écouter lui-même. |
| La page Recette (`recette/`, `recette.json`) | Les questions ✅ / ❌ et leur rapport. |
| Le banc Linux (`live-bench`, `cpu-bench`) | Les chiffres à comparer avec ceux du téléphone. |

Deux points à prouver dès le début (étape T0), car ils ne se testent pas sur la machine de l'agent :
1. **Le robot lancé depuis l'appli, sans ordinateur.** Android permet à une appli de s'instrumenter elle-même (`startInstrumentation` sur son propre paquet). Repli si ça coince sur MagicOS : le robot appelle les fonctions de l'appli au lieu de toucher l'écran (moins fidèle pour les boutons, identique pour le son, les exports et les mesures).
2. **Les captures d'écran prises par le robot** (les feuilles et menus sont dans d'autres fenêtres : `UiAutomation.takeScreenshot` sous instrumentation, sinon capture fenêtre par fenêtre).

## Décisions prises (notées dans DECISIONS.md)

- **Une appli séparée** (`com.pocketstudio.test`, icône d'une autre couleur) installée **à côté** de la vraie. Les projets de Benjamin ne risquent rien (le robot crée, supprime, remet à zéro). Et elle n'a pas besoin de la clé de signature : l'agent la compile lui-même, sans passer par GitHub.
- **Même code, même moteur** que la vraie appli (aucune branche de code « spéciale test » dans le moteur) : ce qui est mesuré est ce qui sera livré.
- **Rien ne part tout seul** : le rapport est un zip que Benjamin partage (comme « Signaler un bug »).

## Ce que fait le test APK, en trois niveaux

### Niveau 1 : le robot mesure (0 geste de Benjamin)
Téléphone posé, écran allumé, environ 30 min. Le robot fait et note tout :
- les 37 styles : morceau de l'assistant, 45 s de lecture en direct chacun → décrochages, charge du fil audio (moyenne, pire bloc), niveau seconde par seconde, alerte fondu / silence / saut (mêmes règles que `live-bench`) ;
- jump up et dnb 5 min, dont 2 min écran éteint (service de premier plan) ;
- exports : M4A, WAV, FLAC, vidéo 9:16 → durée, vérification du fichier, niveau, temps pris ;
- latence aller-retour (3 mesures) et tampon ;
- le Guide suivi jusqu'au bout sur 3 styles (mesures G4 : combien de temps elles prennent sur un vrai téléphone) ;
- Remix : séparation d'un extrait fourni avec l'appli → temps, mémoire, pas de plantage ;
- mémoire et réactivité de l'écran pendant tout le passage ; rotation ; mise en arrière-plan et retour ;
- **le téléphone s'écoute** : il joue par son haut-parleur et enregistre par son micro → la basse qui sort vraiment du haut-parleur (bande 100 à 300 Hz), craquements, trous de son. Ça remplace une bonne partie de « écoute si la basse s'entend ».

### Niveau 2 : le robot montre, l'agent regarde (0 geste de Benjamin)
Le robot visite tous les écrans (Mode simple et complet, 100 % et 130 %, debout et couché) et prend une capture de chacun, environ 120 images. **C'est l'agent qui les regarde**, pas Benjamin : textes coupés, boutons hors écran, tailles de la tablette. Même passage sur la Galaxy Tab.

### Niveau 3 : ce que seul Benjamin peut juger (objectif : 10 à 15 min, 12 questions au plus)
Un bandeau flottant pose une question à la fois, avec ✅ et ❌. Le robot a déjà tout préparé (bon morceau, bon écran, lecture lancée) : Benjamin écoute ou touche, puis répond. Un ❌ prend seul la capture, le journal et les mesures ; un mot d'explication est facultatif.
- à l'oreille : « ça tape ? » sur 4 styles (jump up, house, trap, techno), le Drop, la largeur au casque ;
- avec ses doigts : pads et clavier à plusieurs doigts, geste retour au bord (le kick reste au centre), sensation de retard en jouant ;
- avec ses accessoires : casque filaire, Bluetooth (brancher, débrancher en pleine lecture) ;
- avec ses fichiers : séparer une vraie chanson, enregistrer sa voix sur un pad, partager un export vers une autre appli.

Tout ce qui est dans la liste de recette actuelle (17 points) et peut passer au niveau 1 ou 2 y passe ; le reste rejoint ces 12 questions.

## Le rapport (« banc parfait »)

Un seul zip, bouton « Envoyer le rapport » : `rapport.md` (résumé lisible), `mesures.json` (tous les chiffres), les captures, les extraits enregistrés au micro, le journal. L'agent en tire :
- un tableau **téléphone contre banc Linux** par style → le vrai facteur « téléphone = N fois plus lent » remplace le « 3 » supposé de `cpu-bench`, et les seuils d'alerte sont recalés ;
- la liste des défauts d'affichage vus sur les captures ;
- les ❌ de Benjamin avec leur contexte.
Ces trois listes deviennent l'entrée du grand audit. Le test APK se relance tel quel après les corrections (comparaison avant / après automatique).

## Étapes et coût

| Étape | Contenu | Agents (mode économie) |
|---|---|---|
| T0 | Appli séparée, robot lancé depuis l'appli, une capture, un zip. Preuve sur l'émulateur de la CI (un seul passage) ou, à défaut, un mini APK d'essai de 2 min pour Benjamin. | 1 |
| T1 | Niveau 1 : les mesures et le téléphone qui s'écoute. | 1 |
| T2 | Niveau 2 : la visite de tous les écrans, téléphone et tablette. | 1 |
| T3 | Niveau 3 : bandeau, 12 questions préparées par le robot, rapport. | 1 (le bandeau est déjà à moitié écrit) |
| T4 | Relecture neuve, APK compilé par l'agent, mode d'emploi en 5 lignes. | 1 relecteur léger |
| T5 | Lecture du rapport de Benjamin, tableau téléphone / banc, recalage des seuils. Puis le grand audit. | 1 |

Environ 4 à 5 sessions de travail. Aucun agent en parallèle.

## Ce que Benjamin aura à faire, en tout

1. Installer « Pocket Studio Test » (à côté de la vraie appli) et accepter le micro.
2. Toucher « Tout tester », poser le téléphone 30 min sur le Wi-Fi (le modèle du Remix se télécharge).
3. Répondre aux 12 questions (10 à 15 min, casque à portée de main).
4. Toucher « Envoyer le rapport » et le donner à l'agent.
5. Refaire 2 et 4 sur la tablette (pas de questions, ou 2 ou 3).

## Risques

- MagicOS bloque l'auto-instrumentation → repli décrit plus haut, décidé en T0.
- Les touchers injectés par le robot perdent parfois des touches du clavier (déjà vu sur l'émulateur, REVIEW.md) → le jeu au clavier reste au niveau 3.
- Le micro du téléphone filtre le grave → la mesure « basse au haut-parleur » se lit en comparaison entre styles, pas en valeur absolue.
- Une deuxième appli redemande les téléchargements (Remix 174 Mo, banques de sons) : seul le Remix est nécessaire au test.
