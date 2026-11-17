# Tour 7 · lot « tuto2 » : rapport

Branche `eq/tour-7-tuto2`, base : tête de `ccr-80af1a1b-mn0wec` (fusionnée, déjà à jour).

## Fait (4 sur 4 demandés, plus L10)

| Défaut | Phrase d'acceptation (vue de l'utilisateur) | Rouge | Vert |
|---|---|---|---|
| D3 carte du kit | Au Clavier, en 130 %, je vois « Télécharger le kit … » et « Plus tard » sans toucher « Lire la suite ». | 58292036 | cc825600 |
| D4 deux gestes | En Mode Simple (Facile), chaque consigne demande au plus deux gestes ; le reste vient à l'étape suivante. Vrai dans les 36 styles. | 333ef460 | 0a87087f |
| D4 accords | Dans une étape d'accords, « Accords » est déjà allumé : toucher la note du bas pose l'accord. | 333ef460 | e04c85de |
| D4 Pro intact | En Pro, les étapes gardent leur texte et leur longueur d'avant la découpe (basse house, basse drill, « Doux » techno). | 31b6b2f1 | 6dac2e7e |
| N26 / L10 | Sur un Groove vide, la consigne du Drop ne dit plus que « tout le Groove joue » ; « Partie … créée » dit ce qu'il y a dedans. | c17b70b0 | d3d07645 |

Reggaeton et afrobeats Facile : couverts par la découpe D4 (même test, tous les styles).

Traductions : 166 consignes neuves dans les 14 langues (`tuto_autres.json`), reprises des traductions relues ; 102 anciennes clés retirées.

## Laissés de côté

L1 à L9 et L11 (liste de `lots-correction.md` l. 50 à 62) : non traités, plafond du lot.

## À juger par le chef

- Sur le chemin Facile de la basse house, la copie se fait à l'étape `bass_2` par le bouton « Copier » du panneau ; le bouton « Copier » du piano (`tuto_roll_copy`) n'est plus exercé par ce test, car le piano n'est pas à l'écran quand `bass_2` s'ouvre (changement de navigation dans AppRoot.kt, hors de mes fichiers).
- La phrase « tout le Groove y joue » du message de partie reste (clé partagée dans `ultra3z5.json`, hors de mes fichiers) ; elle est maintenant suivie de « Dedans : … ».

## Vérifications

Voir la fin du rapport (rempli après le passage complet).
