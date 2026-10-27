# Le Guide, G6 : vérification des critères (§ 4.5 du plan)

2026-10-27, branche `g6`. Pas d'émulateur ici et la CI est en pause : tout est vérifié dans Robolectric (l'appli entière, `MainActivity` recréée à l'ouverture du morceau comme sur un téléphone), par les tests du modèle et par le banc (`ps_render` sur Linux). Une ligne par critère : **tenu**, **pas tenu** ou **pas mesurable ici**.

## Le parcours testé

`GuideJourneyTest` (Robolectric, ~40 s) : premier lancement › « Créer mon premier morceau (guidé) » › l'assistant › jump up › Créer ; le Guide s'ouvre seul sur « Ton morceau est prêt » ; « Pour commencer » (Écouter, un son changé, Sauter, Plus tard) ; « Voilà ce que j'ai mis » (Voir le réglage › « Ce qui joue », Garder) ; batterie appliquée ; basse sautée ; Sub écouté « Sans / Avec » (rendu factice) puis refusé ; fiche de piste ; couche « Air » ajoutée puis ramenée depuis l'historique ; « Prêt à partager ? ». Puis l'appli fermée, sauvée, horloge du Guide +3 jours, rouverte : « Que veux-tu faire ? » › Continuer › « Te revoilà ! Tu en étais à : … ». Le même en Mode simple, plus court (Voir le réglage › le clavier, un Break tiré du Drop, annuler / rétablir, « Aller plus loin », Envoyer).

## Gestes

- Appliquer une carte = 2 : **tenu** pour les deux premières options (barre › option) ; **pas tenu** pour une 3ᵉ option sur un téléphone de 890 dp (un défilement de plus : 3).
- Écouter une proposition = 1 : **tenu** (▶ de l'option).
- Ouvrir la fiche d'une piste = 1 : **pas tenu** : 2 depuis la carte (le lien est sous les options, un défilement), 2 depuis la barre des pistes (appui long › Fiche).
- Ajouter une couche conseillée = 2 : **pas tenu** : 4 (défiler jusqu'à « Voir les couches », l'ouvrir, le rôle, le son).
- Revenir sur un choix = 2 : **pas tenu** : 3 (la fiche, défiler jusqu'à l'historique, Ramener).

## Couverture

- Chaque piste de l'assistant a sa fiche complète (37 styles × 4 ambiances, 2 164 fiches) : **tenu** (`GuideFollowTest`) ; une courbe sur tout le morceau n'y dit pas dans quelles parties elle bouge (mineur, REVIEW).
- Morceau de l'assistant intact : 12 à 20 cartes à décider : **tenu** (`GuideAnalyzerTest`, 37 × 4).
- Parcours complet de 30 à 40 cartes : **tenu** (`GuideParcoursTest`).

## Reprise

- Fermer, tuer, rouvrir 3 jours plus tard : même étape, même résumé, rien de perdu : **tenu** dans Robolectric (même carte, même numéro d'étape, états, refus et historique identiques, rien de redemandé) ; la mort du processus est simulée (activité fermée, mémoire vidée), pas un vrai processus tué : à refaire sur téléphone.
- Exporter le zip puis l'importer : le Guide suit : **tenu** (`GuideStateTest`).

## Son (banc `docs/son/banc/guide-G6.md`, morceaux suivis jusqu'au bout par `GuideFollow`)

- Drop − Break ≥ +4 : **pas tenu** pour 4 styles sur les 25 qui ont un Break (acidcore +2,7, gabber +3,3, hard techno +2,8, trance +2,3) ; **pas mesurable** pour 12 styles sans Break (afrobeats, boom bap, drill, grime, jersey club, lo-fi, phonk, pop, reggaeton, synthwave, trap, UK garage). La carte « Le contraste » demande les mesures du téléphone : le banc ne la joue pas.
- Trois Drops différents (variation entre Drops ≥ 0,25) : **tenu** pour la variation, calculée sur les 37 styles (minimum 0,44, rétro techno ; inchangée ou meilleure une fois suivi) ; mais 15 styles n'ont que **deux** Drops (techno, house, disco…), pas trois.
- Aucune piste sans coupe-bas sauf kick et sub : **tenu** (37 styles suivis : 0 piste sans coupe-bas).
- Marge respectée : **tenu** (cible de volume, master ≤ 0 dB ; true peak −1,17 dBTP au plus).

## Jamais bloquant

- Aucune carte n'est modale : **tenu** (panneau sous l'écran, les espaces restent touchables).
- Budgets de gestes des espaces avec le Guide ouvert : **tenu pour 14 actions sur 15** (`KeyActionsGuideOpenTest`) ; « Enregistrer une mélodie » en coûte 3 au lieu de 2 sur un petit téléphone (360 × 780) : le panneau (40 %) cache la 5ᵉ piste de la ligne du temps.

## Réalisme

- Parcours complet sur émulateur : **pas mesurable ici** (pas d'émulateur, CI en pause) ; le même parcours tourne dans Robolectric, l'activité recréée comprise (ce que G5 renvoyait à l'émulateur).
- Les 8 testeurs à l'audit : **pas mesurable ici** (à faire avec Benjamin).
- Durée des mesures sur un vrai téléphone : **pas mesurable ici**.

## Corrigé pendant G6

- Revenu un jour ou plus après, le Guide ne disait rien tant qu'on ne l'ouvrait pas (§ 3.6) : il s'ouvre maintenant seul sur « Te revoilà », une fois par visite.
