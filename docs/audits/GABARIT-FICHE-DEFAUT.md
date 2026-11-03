# Fiche de défaut (gabarit)

Copier ce bloc par défaut dans le fichier du tour (`docs/audits/tour-N/defauts.md`). Un défaut non reproduit n'est pas corrigé : il est d'abord reproduit par un test (AUDIT §1, point 2).

```
## D-<tour>-<numéro> : <titre court, ce qui est faux>

- Étage / siège : <étage 0 à 7> / <personnalité, langue, appareil (profil de la matrice), orientation, police>
- Version : <étiquette audit-N> (release R8) · commit <sha court>
- Zone (carte des risques, AUDIT §2) : <zone> · priorité de zone <1, 2 ou 3>
- Gravité (AUDIT §5) : <Bloquant | Grave | Moyen | Mineur | Idée>
- Fréquence : <nombre de sièges ou de cas sur le total>
- Persistance : <contournable (comment) | non contournable>
- Priorité = gravité × fréquence × persistance (×2 en zone de priorité 1) : <valeur>

### Reproduire
- Projet de départ : <projet de référence (vide, petit, énorme, ancienne version…) ou fichier joint>
- Graine du hasard : <graine, ou « aucune »>
- Étapes : 1. … 2. … 3. …
- Attendu : <ce qui devrait se passer>
- Obtenu : <ce qui se passe>
- Preuve : <capture annotée, vidéo, son, extrait du journal (chemin)>

### Oracle
<la règle, la mesure, la comparaison ou la référence qui dit que c'est faux (FEW HICCUPPS, WCAG, son maître, spécification…)>

### Cause racine
- Famille : <oubli de branchement | état non sauvegardé | texte | mise en page | son | performance | plateforme | autre>
- Explication : <une ou deux phrases>
- Même cause déjà vue : <D-x-y, D-x-z> (trois défauts de la même cause = une correction de fond, AUDIT §5)

### Correction
- Test qui échoue avant : <nom du test, fichier>
- Correction : <commit, branche>
- Vérification « partout » (règle de Benjamin) : <où d'autre le défaut pouvait se reproduire, et ce qui a été vérifié ou corrigé>
- Relance ciblée : <étages et tests relancés>
- Captures « après » / extrait sonore après : <chemin>

### État
<ouvert | reproduit | corrigé | vérifié au tour suivant | refusé (raison)>
```
