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

- Jalon 1.1 : module `project-model` (Kotlin pur) : format JSON tolérant aux versions, `ProjectStore` à écriture atomique + sauvegarde de secours, `History` annuler/rétablir illimité avec regroupement des gestes, `ProjectSession` à sauvegarde automatique différée. 15 tests, dont plantage en pleine écriture.
- Jalon 1.2 : `Sequencer` (horloge de doubles-croches à l'échantillon près, swing, changement de tempo sans à-coup, décompte), `Metronome`, commandes moteur (transport, tempo, swing, métronome, pas de batterie), `TapTempo`. Tests : timing exact quelle que soit la taille de bloc, swing, changement de tempo, décompte, métronome à l'échantillon près.
- Jalon 1.3 (moteur) : `DrumKit` 16 sons synthétisés (DaisySP + clap/rim/cloche/shaker maison), déclenchés à l'échantillon près par le séquenceur, pads en direct (`TriggerPad`), charleston fermé qui coupe l'ouvert. Kit complet à 5,9 % du temps réel (x86). Tests : chaque son démarre pile et s'éteint, vélocité, étouffement, motif → batterie, budget CPU.
- Jalon 1.3 (écran) : onglet Rythme (transport, tempo −/+/Tap, annuler/rétablir, 16 pads colorés qui s'allument, grille 16 pas avec tête de lecture, appui long = vélocité, swing, métronome, décompte), projet chargé/sauvé via `ProjectSession`, sauvegarde forcée à la mise en arrière-plan. Onglet Outils (latence, son de test).
- Jalon Acide A.1–A.3 (moteur) : `AcidTrack` = Open303 + séquenceur par pas (note, octave, accent, slide, silence, 1–64 pas), 9 réglages (cutoff, résonance, env mod, decay, accent, accord ±1 octave, onde scie→carré, saturation, niveau). Tests : départ à l'échantillon près, accent ×2,2 en énergie, slide qui tient la note, arrêt propre, 1,2 % du temps réel.
- Jalon Acide A.4–A.5 : `AcidGenerator` (inspiré de The Endless Acid Banger, CC BY 4.0, crédité), densité, verrous ; `AcidPattern` dans le projet ; onglet Acide (générer, grille, éditeur de pas, 9 curseurs de son).
- Dette : la logique Acide vit dans `RhythmViewModel` (transport et projet partagés) ; à extraire dans un contrôleur de session partagé quand le synthé arrivera.

## À retenir pour la suite
- Afficher une « latence de jeu » (sortie + tactile) à côté de l'aller-retour ; essayer un buffer d'un seul burst.
- MagicOS chiffre logcat (bloc HKS…HKE) : remplacer `LogTail` par un journal circulaire interne à l'appli.
- Recette phase 0 : issues #2 et #3 (doublon), 8/8 ✅, fermées.

## Prochaine étape exacte
1. Recette 1.2/1.3 par Benjamin (aperçu de la PR de phase 1).
2. Recette du jalon Acide par Benjamin ; puis 1.4 synthé soustractif.
