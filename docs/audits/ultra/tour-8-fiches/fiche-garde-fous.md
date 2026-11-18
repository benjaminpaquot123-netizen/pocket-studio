> CORRECTIONS DU VÉRIFICATEUR (2026-11-18). Elles priment sur le texte dessous.
- Base : `git fetch origin ccr-80af1a1b-mn0wec`, tête f6c6d7c7f au moins. Rapports p8b : `git fetch origin eq/tour-7-controle-p8b` avant tout `git show ad3676b3:…` (ce commit n'est pas dans l'intégration).
- Preuve du vert : cette machine n'a pas de SDK Android, aucun test Gradle n'y tourne (même `:project-model:test`). Pousse ta branche, écris « prêt pour CI : <sha> » au chef ; il lance `gh workflow run ci.yml --ref <branche>`. Vert = job « Build, unit tests, screenshots » vert ET « Captures : 0 différentes » lu dans le résumé (la CI ne rougit jamais sur une capture).
- Captures : la liste des images changées se lit dans `results-summary.json` de l'artefact `screenshots-diff` (champ `golden_file_path`), jamais dans le résumé, qui aplatit les noms (156 noms en double). Tu ne réenregistres que les écrans de ta liste ; une autre image qui change = une ligne au chef.
- Textes : aucune phrase française neuve ou modifiée hors du lot T0c. Si un mot manque, demande au chef (leçon CI 360).
- Partie A, après T0c seulement, jamais à côté. `tuto-moment-exceptions.txt` est dans `src/main` et lu par l'appli (`TutoMoments.kt`) : y retirer une ligne change ce que voit l'élève, donc un débutant neuf rejoue les chapitres touchés.
- Chaque lot du tuto retire lui-même les lignes d'exception qu'il guérit ; ce lot ne retire que celles qui restent, et baisse `FACILE_FROZEN` (409) et `PRO_RATCHET` (3725) au nombre mesuré par la CI, jamais à l'estime.

---

# Fiche tour 8 : lot « garde-fous » (points MOYEN et mineurs de la revue du tour 7)

Réponse courte : oui, ce lot touche la zone tuto. Les exceptions et les tests visés sont ceux du tuto. Il passe donc en série avec T0 : après T0c, ou fondu dans T0c. Une seule partie est hors tuto : la partie B ci-dessous, qui peut partir en parallèle.

Rappels :
- Dépôt PUBLIC.
- Jamais `git add -A`, `git add .` ni `git commit -a` : on ajoute par chemin.
- Pas de push sur l'intégration (`ccr-80af1a1b-mn0wec`) ni sur `main`. Pas de PR.
- Aucun identifiant de modèle dans les commits.
- Commits « rouge: » puis « vert: ».
- Code et commits en anglais.

## Partie A, zone tuto (branche `eq/tour-8-garde-fous`, après T0c)

> Chef d'orchestre → <NOM>, rôle CORRECTEUR. Lot tour-8 garde-fous : MOYEN « exceptions périmées », mineurs du tuto de REVIEW.md (tour 7). Aucune question à Benjamin : une ligne pour moi.

1. Base : `git fetch origin ccr-80af1a1b-mn0wec`, puis la branche neuve depuis `FETCH_HEAD`.
2. Lis :
   - `docs/REVIEW.md`, section « Tour 7 : revue de fin de tour (2026-11-18) » (l. ~1207) ;
   - les deux fiches de `docs/AIDE-FUSIONS-TESTS.md`.
3. Tes fichiers, et eux seuls :
   - `app/src/test/resources/tuto/readability-facile-exceptions.txt` et le test qui le lit ;
   - `project-model/src/main/resources/tuto-moment-exceptions.txt` et le test qui le lit ;
   - `app/src/test/java/com/pocketstudio/app/tuto/Tour7TutoPanelFoldTest.kt` ;
   - le test des étapes de volume (à localiser : grep « sous le curseur » sous `app/src/test` et `project-model/src/test`). Demande au chef s'il est hors de ces dossiers.
   - Aucun fichier de l'appli (`src/main/java`, `src/main/kotlin`). Si un test neuf ne passe qu'en changeant l'appli : une ligne au chef, et le point va dans un lot du tuto.
4. Points et phrases d'acceptation (ce sont des tests : l'acceptation est une assertion lue, pas un écran) :
   - MOYEN, exceptions périmées : « chaque entrée des deux fichiers d'exception échoue encore à la règle qu'elle excuse ; une entrée devenue inutile fait rougir le test, avec son nom » ; plus un plafond pour `tuto-moment-exceptions.txt` (52 entrées aujourd'hui) : plafond = nombre actuel après nettoyage.
     - Rouge d'abord : ajouter une fausse entrée qui passe déjà la règle, voir le test rouge, puis l'enlever.
     - Les entrées périmées sont retirées (après T0c, il y en aura sans doute).
   - Mineur : `Tour7TutoPanelFoldTest` ne peut plus passer sans au moins un cas coupé (compteur de cas > 0, sinon rouge).
   - Mineur : les étapes de volume sans « sous le curseur » sont vérifiées aussi (toutes les étapes de volume, de toutes les langues).
5. Tests ciblés : seulement les classes touchées, plus `I18nCatalogTest`. Pas de capture : rien ne change à l'écran.
6. Rapport `docs/audits/ultra/tour-8-garde-fous-rapport.md`. Puis :
   - self-test des rejeux ;
   - `pre-vol.sh --avant-relecture tour-8 garde-fous` ;
   - relecture neuve en dernier commit ;
   - pré-vol PRÊT.
   Pas de siège de contrôle (rien de visible), sauf avis du chef.

## Partie B, hors tuto (peut partir à côté de tout, sauf M)

Mineur : compteurs de test dans le code de l'appli : `cutCurveCompositions` (`remix/CutScreen.kt`, lu par `remix/CutPlayHeadTest.kt`) et `fitProbeCompositions` (`mixer/MixerScreen.kt`, lu par `ui/MixerFitStateTest.kt`).
- `fitProbeCompositions` est dans le lot M : ne pas y toucher ici.
- `cutCurveCompositions` : un mini-lot à part, ou rien. Proposition au chef : le garder, c'est le seul moyen de prouver le coût, et le noter « voulu » dans REVIEW.md, sans changer le code.

## Hors de ce lot

- `PianoRoll` qui utilise `TutoHints.active` comme clé de LaunchedEffect : déjà dans T0a, même fichier.
- « Étape cochée qui se décoche quand sa pièce change » : voulu (siège d), rien à faire.

> **Demander au chef.** Un fichier hors de ta liste, un doute sur la phrase d'acceptation, un conflit avec un autre lot, un test qui casse hors de ta zone : une ligne au chef (`send_message`), puis tu continues le reste de ta fiche en attendant sa réponse. Jamais de question à Benjamin, jamais de décision hors de ta fiche.
> **Travailler en silence.** Aucun récit, aucun résumé, aucun texte pour Benjamin dans ta discussion. Tu n'écris qu'au chef, une ligne, à trois moments : « prêt », « demande / bloqué », « fini ». Sorties de commandes filtrées (`tail`, `grep`).
