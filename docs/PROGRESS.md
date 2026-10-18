# Avancement

**Phase 0 terminée et fusionnée le 2026-10-18.** Phase en cours : 1 (MVP jouable).

## Fait
- Démarrage : SPEC, CLAUDE.md, ROADMAP, DECISIONS, ARCHITECTURE, THIRD_PARTY, README.
- Moteur C++ : sinusoïde lissée (sans clic), file SPSC, compteur de crêtes, mesure aller-retour. 10 tests Linux verts, aussi sous ASan/UBSan.
- Android : Oboe AAudio exclusif LowLatency, reconnexion casque, écrans Studio / Latence / Recette, rapports GitHub, crash au lancement suivant, mise à jour intégrée.
- CI : tests moteur (+ ASan/UBSan), tests Kotlin, captures, APK signé, aperçu sur PR, Release sur main, test de fumée émulateur.

- Revue critique de fin de phase faite (docs/REVIEW.md) : graves corrigés.
- Jalon Acide (Open303) ajouté entre la phase 1 et la phase 2.

- Recette sur le Honor Magic 8 Pro : sortie 10 ms (AAudio exclusif MMAP, burst 96), aller-retour haut-parleur → micro 44,5 ms stable (45,2 / 44,4 / 44,5).

## À retenir pour la suite
- Afficher une « latence de jeu » (sortie + tactile) à côté de l'aller-retour ; essayer un buffer d'un seul burst.
- MagicOS chiffre logcat (bloc HKS…HKE) : remplacer `LogTail` par un journal circulaire interne à l'appli.
- Recette phase 0 : issues #2 et #3 (doublon), 8/8 ✅, fermées.

## Prochaine étape exacte
1. Phase 1, jalon 1.1 : module `project-model` (projet JSON, écritures atomiques, sauvegarde auto, annuler/rétablir illimité).
