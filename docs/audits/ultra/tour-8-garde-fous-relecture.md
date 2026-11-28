# Tour 8, garde-fous : relecture neuve
Relu : 8b751f0e

Portée : `git diff d27fd0244..d32416fa5` (tests seuls) : TutoReadabilityBudgetTest, Tour7TutoPanelFoldTest, Tour6TutoMixWordsTest, TutoValidationMomentTest, le rapport. De d32416fa5 à 8b751f0e : un seul mot de commentaire (« 13 » → « 14 », demandé par la relecture) et la section « Relecture neuve » du rapport. Gradle non joué (pas de SDK ici) ; règle des volumes rejouée en Python.

Points vérifiés sans défaut :
- Périmés, moments : `TutoValidationMomentTest` nomme chaque entrée qui n'est plus un problème (`EXCEPTIONS - now`) ; `FROZEN` = 52 = entrées uniques du fichier.
- Périmés, lisibilité : `everyFacileExceptionStillMisses` compare sans le jeton « lignes/budget », comme le test de gel ; il nomme les périmés ; 409 entrées = `FACILE_FROZEN`. Les fonctions utilisées existent.
- Pli : `tuto_text_more` n'existe que si « Lire la suite » s'affiche (`TutoPanel.kt:823`) ; `cut > 0` rougit si aucun cas n'est coupé.
- Volumes : 36 étapes Facile `changed:volumes`, rôles pad, lead, bass, clap, acid, noms justes ; français + 14 langues, 0 écart ; un rôle inconnu, un rôle absent ou une traduction manquante ajoutent un échec, et le compte est vérifié (`>= 36`).
- Rouges : les fausses entrées (commit rouge 94221107f) ont rougi à la CI 387, puis ont été retirées.

Constats :
1. mineur : le commentaire de `Tour6TutoMixWordsTest` disait « 13 other languages » ; corrigé en 8b751f0e.

Verdict : 0 bloquant, 0 grave
