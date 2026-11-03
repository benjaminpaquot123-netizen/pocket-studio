# Tableau de bord du tour (gabarit)

Une page par tour (`docs/audits/tour-N/tableau-de-bord.md`), d'après AUDIT §7. Les cases « critère » reprennent les critères d'arrêt de AUDIT §6.

```
# Tour N : tableau de bord (<date>, étiquette audit-N, commit <sha>)

## En une ligne
<ce qui a changé depuis le tour précédent, en français simple>

## Compteurs par gravité
| Gravité  | Trouvés | Corrigés | Restants | Tour précédent |
|----------|---------|----------|----------|----------------|
| Bloquant |         |          |          |                |
| Grave    |         |          |          |                |
| Moyen    |         |          |          |                |
| Mineur   |         |          |          |                |
| Idée     |         |          |          |                |

## Courbe des découvertes
| Tour | Défauts trouvés | % du tour 1 |
|------|-----------------|-------------|
| 1    |                 | 100 %       |
| N    |                 |             |
Critère : le dernier tour trouve moins de 20 % des défauts du tour 1.

## Causes racines (top 5)
| Cause | Nombre | Correction de fond ? |
|-------|--------|----------------------|

## Carte des fonctions
Cases vides : <nombre> (liste ou lien). Critère : 0.

## Étages
| Étage | État (vert / rouge / pas lancé) | Ce qui a été relancé | Remarque |
|-------|---------------------------------|----------------------|----------|
| 0 Santé (R8, lint, ASan/UBSan/TSan, secrets, licences) | | | |
| 1 Automatiques (captures, accessibilité, son, exports, performance, tests) | | | |
| 2 Tout est connecté | | | |
| 3 Robustesse et chaos | | | |
| 4 Audio expert | | | |
| 5 Utilisabilité experte | | | |
| 6 Testeurs IA (sièges : …) | | | |

## Son
| Mesure | Valeur | Cible | OK ? |
|--------|--------|-------|------|
| Banc des 37 styles (sonie, true peak, kick dans le mix) | | dans les cibles | |
| Rendu maître (écart au son maître) | | identique (tolérance chiffrée) | |
| Direct = export | | oui | |
| XRuns, pire cas sur ordinateur | | 0 | |

## Performance
| Mesure | Valeur | Cible | OK ? |
|--------|--------|-------|------|
| Démarrage à froid (téléphone standard, médiane) | | < 1,5 s | |
| Images au-delà du seuil (16 ms, 8 ms à 120 Hz) | | 0 | |
| Fuites (LeakCanary) | | 0 | |
| Mémoire (PSS) après 30 min de Live | | | |

## Utilisabilité (testeurs IA)
| Mesure | Valeur | Cible |
|--------|--------|-------|
| Réussite par tâche clé (la plus basse) | | ≥ 90 % |
| SUS | | ≥ 80 |
| Premier son du Pad Live depuis l'accueil | | 1 toucher |
| Notes des designers (la plus basse) | | ≥ 8/10 |

## Accessibilité et langues
- Erreurs automatiques (ATF) : <nombre> (critère : 0)
- Critères WCAG 2.2 AA non tenus : <liste>
- Captures signalées (textes coupés, chevauchements) : <nombre par langue>

## Critères d'arrêt (AUDIT §6)
- [ ] 2 tours de suite sans Bloquant ni Grave, au plus 5 Moyens
- [ ] courbe aplatie (< 20 % du tour 1)
- [ ] carte des fonctions sans case vide
- [ ] captures propres (15 langues, matrice d'appareils)
- [ ] accessibilité : 0 erreur automatique, WCAG AA tenu
- [ ] son : banc, maître, direct = export, 0 XRun
- [ ] performance : démarrage < 1,5 s, pas de saccade, pas de fuite
- [ ] utilisabilité : 90 %, SUS 80, Pad Live en 1 toucher, 8/10

## Prochain tour
<sièges, langues, appareils, zones corrigées à retester>
```
