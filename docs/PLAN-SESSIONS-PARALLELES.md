# Plan : gagner du temps avec deux discussions en parallèle (2026-11-11)

But : finir tout ce qui reste avant les vrais audits (tour 6) plus vite, sans perdre de temps en conflits.
Chaque nouvelle discussion a sa propre machine (4 cœurs, sa propre file Gradle) : c'est du calcul en plus, gratuit en temps.

## Qui fait quoi

| Où | Travail | Fichiers touchés | Quand |
|---|---|---|---|
| Discussion principale (celle-ci) | Chantiers 8 Courbes et 9 Accessibilité, fusion du Pad Live, toutes les fusions dans l'intégration, fin de l'étape 3 (10 contrôles complets, tour d'essai à 4 sièges) | écrans du mixeur, feuilles, aide « ? », portail | en cours |
| **Discussion A : « socle du banc »** | Les décisions de `docs/AIDE-FUSIONS-TESTS.md` qui suppriment les refus à répétition | `scripts/bench/`, `.github/workflows/ci.yml`, tests du banc, rejeux | **maintenant** |
| **Discussion B : « Remix »** | Chantier 10 de l'étape 4 | écrans Remix seulement | **maintenant** |
| Discussion B, ensuite | Plan test APK, étapes T1 à T3 | dossier de BenStudio Test seulement | **après** le rapport de l'essai éclair |

Les zones ne se touchent pas : aucune discussion n'écrit dans les fichiers d'une autre.

## Règles communes aux deux discussions

- Partir de la branche `ccr-80af1a1b-mn0wec` (l'intégration poussée) ; travailler et pousser sur la branche que la discussion reçoit, **jamais** sur `ccr-80af1a1b-mn0wec`.
- Pas de PR, pas de fusion vers `main` : la discussion principale fusionne par le portail.
- Appliquer la « fiche des équipes » et la « fiche du relecteur » de `docs/AIDE-FUSIONS-TESTS.md`.
- Relecture neuve par un sous-agent qui n'a pas écrit le code, fichier `docs/audits/ultra/tour-5-<zone>-relecture.md` au format de la fiche du relecteur.
- Avant de rendre la main : fusionner la dernière `ccr-80af1a1b-mn0wec` dans sa branche, puis `scripts/bench/pre-vol.sh --avant-relecture tour-5 <zone>`, puis après la relecture `scripts/bench/pre-vol.sh tour-5 <zone>` : tout « RECALÉ » est corrigé.
- Décisions : une ligne par choix dans `docs/decisions/tour-5-<zone>.md` (fichier propre au lot, pas `docs/DECISIONS.md`, pour éviter les conflits).
- Message final à Benjamin : 5 lignes, en français simple, avec le nom de la branche poussée.

## Prompt à coller dans la discussion A (« socle du banc »)

```
Lis CLAUDE.md, docs/PLAN-SESSIONS-PARALLELES.md (section « Règles communes ») et docs/AIDE-FUSIONS-TESTS.md en entier.
Pars de la branche ccr-80af1a1b-mn0wec. Zone du lot : « socle ». Tu ne touches ni aux écrans de l'appli (app/src/main/java/…/ui, mixer, remix, padlive), ni aux fichiers de BenStudio Test.
Fais, dans cet ordre, chaque point avec un test qui rougit puis verdit :
1. Une base verte : les 5 captures instables (recette, cut_screen, cut_screen_magnifier, remix_stem_edit_menu, rhythm_scrolled_under_part) et le test capricieux du renommage sont réglés pour de bon ; sinon mis en quarantaine nommée, avec la raison. Plus aucun lot ne doit les retrier.
2. Fichiers de décisions par lot : chaque lot écrit dans docs/decisions/tour-5-<zone>.md ; le portail (scripts/bench/fusion-equipes.sh) les accepte et docs/DECISIONS.md les liste. Fin des conflits sur DECISIONS.md. Même idée pour PLAN-RESTE, le journal et verdicts.tsv si c'est simple ; sinon dis pourquoi.
3. Rejeux par étiquette : le format des rejeux (docs/audits/ultra/rejeux/) accepte « touche <étiquette de test> » en plus des pixels, et t4-26-prise-clavier passe aux étiquettes. Un changement de mise en page ne doit plus casser un rejeu.
4. CI : les contrôles de quelques secondes (tests Python du banc, pre-vol --ci) en première étape de .github/workflows/ci.yml.
5. Règles du portail gelées par paquet : une règle neuve vaut pour le paquet suivant (note-le dans le portail et dans l'aide).
Mets à jour docs/AIDE-FUSIONS-TESTS.md et pre-vol.sh (ligne KNOWN_RULES) si le portail change.
Gradle uniquement via scripts/bench/gradle-file.sh. Jamais git add -A ; aucun lien symbolique commité.
Relecture neuve par un sous-agent, pre-vol, push sur ta branche, puis dis-moi en 5 lignes ce qui est fait et le nom de la branche.
```

## Prompt à coller dans la discussion B (« Remix »)

```
Lis CLAUDE.md, docs/PLAN-SESSIONS-PARALLELES.md (section « Règles communes ») et docs/AIDE-FUSIONS-TESTS.md (les deux fiches).
Pars de la branche ccr-80af1a1b-mn0wec. Zone du lot : « remix ». Chantier 10 de l'étape 4 de docs/audits/ultra/PLAN-ACTION-AUDITS.md : « Une seule carte conseillée, un bouton clair, l'alerte « la fin sera coupée » ». Cherche les constats d'origine dans docs/audits/ultra/ (rapports du tour 5, verdicts.tsv).
Tu ne touches qu'aux écrans et à la logique du Remix (et leurs textes en 14 langues) ; pas au mixeur, aux feuilles, au Pad Live, au banc, ni à BenStudio Test.
Chaque correction : commit « rouge: <sujet> » (test seul, qui échoue sur son assertion) puis « vert: <même sujet> ». Regarde tes captures en Simple et en Pro ; ne réenregistre jamais recette, cut_screen, cut_screen_magnifier, remix_stem_edit_menu, rhythm_scrolled_under_part.
Gradle uniquement via scripts/bench/gradle-file.sh. Jamais git add -A ; aucun lien symbolique commité.
Relecture neuve par un sous-agent, pre-vol, push sur ta branche, puis dis-moi en 5 lignes ce qui est fait, les essais téléphone à prévoir et le nom de la branche.
Ensuite, attends : le plan test APK (T1 à T3) te sera confié quand le rapport de l'essai éclair sera revenu.
```

## Ce que fait la discussion principale quand une branche revient

1. `git fetch` de la branche, copie en `eq/tour-5-<zone>`, `pre-vol.sh tour-5 <zone>`.
2. Fusion par le portail, une seule CI par paquet.
3. Si le portail refuse : le dire dans la discussion concernée, avec la ligne « RECALÉ ».

## Gain attendu

Deux files Gradle de plus. Le socle du banc supprime l'essentiel des refus vus le 11 novembre (DECISIONS en conflit, rejeux en pixels cassés, captures instables). Le Remix avance pendant que la discussion principale fait les courbes et l'accessibilité. Avec ça, l'étape 4 peut se terminer en même temps que la fin de l'étape 3.
