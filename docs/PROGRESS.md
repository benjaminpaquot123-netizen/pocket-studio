# Avancement

**Phase en cours : 0 (fondations)**, code terminé, en attente de recette sur le Honor.

## Fait
- Démarrage : SPEC, CLAUDE.md, ROADMAP, DECISIONS, ARCHITECTURE, THIRD_PARTY, README.
- Moteur C++ : sinusoïde lissée (sans clic), file SPSC, compteur de crêtes, mesure aller-retour. 8 tests Linux verts (rendu 4 mesures, fréquence, blocs de tailles variées, référence, latence simulée).
- Android : Oboe AAudio exclusif LowLatency, reconnexion casque, écrans Studio / Latence / Recette, rapports GitHub, crash au lancement suivant, mise à jour intégrée.
- CI : tests moteur (+ ASan/UBSan), tests Kotlin, captures, APK signé, aperçu sur PR, Release sur main, test de fumée émulateur.

## En attente de Benjamin
- Ajouter le secret GitHub `SIGNING_PASSWORD` (marche à suivre dans le message de la PR de phase 0).
- Recette phase 0 sur le téléphone, puis fusion de la PR.

## Prochaine étape exacte
1. Lire les issues ouvertes (rapports de recette) et corriger ce qui est ❌.
2. Phase 1, jalon 1.1 : module `project-model` (projet JSON, écritures atomiques, sauvegarde auto, annuler/rétablir illimité).
