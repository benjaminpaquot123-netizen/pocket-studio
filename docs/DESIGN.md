# Ligne graphique

Décision de Benjamin (2026-10-18) : **moderne, mais pas trop, et facile à prendre en main.** Modèle : **FL Studio Mobile, en plus clair.**

## Principes
1. **Une action principale évidente par écran** (gros bouton, en haut ou au centre). Le reste est secondaire.
2. **Pro, façon FL Studio Mobile en clair** (mise à jour du 2026-10-20, « que ça claque ») : panneaux graphite en relief (dégradé léger du haut vers le bas, liseré clair en haut, ombre douce, coins de 16 dp) sur un fond plus sombre, accent orange, vert réservé à « ça joue / en cours ». Les halos sont réservés à ce qui joue ou à ce qui est choisi (pads, pas sous la tête de lecture, scène en cours, bouton Lecture, vignette choisie). Police Inter, chiffres à chasse fixe pour tempos et décibels.
3. **Lisible** : texte d'au moins 14 sp, contraste suffisant, majuscules réservées aux petites étiquettes de section.
4. **Fait pour les doigts** : cibles tactiles d'au moins 48 dp, pads d'au moins 64 dp, espace entre les zones tactiles.
5. **Des mots, pas du jargon** : icône + mot en français courant (« Jouer », « Annuler »), le détail technique derrière « Avancé ».
6. **Retour immédiat** : chaque geste se voit (état, couleur) et peut se sentir (légère vibration sur les pads).
7. **Animations courtes et utiles** (150 à 250 ms), jamais décoratives ; fluides à 120 Hz.
8. **Cohérence** : tous les écrans utilisent les composants de `ui-components` (cartes, boutons, potards), jamais de style ad hoc.
9. **Repliable** : chaque carte à titre se replie en touchant son titre ; la flèche est petite et pâle, pour ne pas gêner.

## Composants
- `NeonCard` (panneau en relief, repliable par son premier `SectionLabel`), `SectionLabel` (barre de couleur + petites capitales), `ProSlider` (piste fine en dégradé, pouce lumineux), `pillColors` (puce choisie remplie d'accent), `LevelMeter` (LED vert → ambre → rouge).
- `SoundTile`, `FamilyChip`, `TileGrid` : vignettes de sons (icône et couleur de famille via `SoundLooks`).
- Écrans : `DrumPad` (pads en relief qui s'allument), touches de clavier en relief, `FloatingKeyboard` (clavier qui suit).

## Références
FL Studio Mobile (organisation, couleurs franches, look « outil »), en plus clair ; simplicité de Koala Sampler ; lisibilité de Material 3.
