> CORRECTIONS DU RÉDACTEUR (2026-11-18). Elles priment sur le texte dessous.
- Base : `git fetch origin ccr-80af1a1b-mn0wec`, sur une tête qui contient captures8 (K change l'en-tête du Clavier, donc les captures `synth*` que captures8 refait aussi par `TrackBar.kt`).
- SDK et Gradle : comme le bloc « CORRECTIONS DU CHEF » de `fiche-captures8.md`, puis `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh …`. Une dépendance qui tombe au hasard : relance avec `--refresh-dependencies` (8 fois au plus).
- Vert = job « Build, unit tests, screenshots » vert ET « Captures : 0 différentes » ; la liste des images changées se lit dans `results-summary.json` de l'artefact `screenshots-diff` (champ `golden_file_path`). Tu ne réenregistres que les captures du Clavier (`synth*`, `*keyboard*`) ; toute autre image changée = une ligne au chef.
- Textes : aucune phrase française neuve ou modifiée (lot T0c seul). « Enregistrer ma mélodie » ne change pas de mot.
- Le rejeu `t4-26-prise-clavier` ne se modifie pas : il doit passer tel quel.

---

# Fiche tour 8 : lot K « clavier200 » (branche `eq/tour-8-clavier200`)

> Chef d'orchestre → <NOM>, rôle CORRECTEUR. Lot tour-8 clavier200 : défaut t4-26, l'en-tête du Clavier pousse les touches sous le pli à 200 %. Aucune question à Benjamin : une ligne pour moi.

Rappels :
- Dépôt PUBLIC.
- Jamais `git add -A`, `git add .` ni `git commit -a` : on ajoute par chemin.
- Pas de push sur l'intégration (`ccr-80af1a1b-mn0wec`) ni sur `main`. Pas de PR.
- Aucun identifiant de modèle dans les commits.
- Commits « rouge: » puis « vert: ».
- Code et commits en anglais.

1. Base : `git fetch origin ccr-80af1a1b-mn0wec`, puis la branche neuve `eq/tour-8-clavier200` depuis `FETCH_HEAD`.
2. Lis :
   - `docs/audits/ultra/PLAN-TOUR-8-brouillon.md`, lot K (l. ~94-100) ;
   - `docs/audits/ultra/sieges/tour-7/incidents-banc.tsv` (ligne t4-26 : cause « appli (guide) ») et le rapport guide2 (le rejeu divergeait au pas 29 : `key_62` hors d'atteinte) ;
   - le rejeu `docs/audits/ultra/rejeux/t4-26-prise-clavier/` (`start.txt` : fr, phone, 200 %, Simple ; `oracle.txt` : `rejeu.absent=Rien n'a été joué pendant la prise`) ;
   - les deux fiches de `docs/AIDE-FUSIONS-TESTS.md`.
3. Tes fichiers, et eux seuls :
   - `app/src/main/java/com/pocketstudio/app/synth/SynthScreen.kt` : l'en-tête du Clavier debout (colonne défilante l. ~418-433 : `TrackBar(it)` l. 423, `ModeSwitch`, `record?.invoke(false)`, `KeyboardCard`) et, couché, la rangée `synth_keys_row` / `synth_record_side` (l. ~382-416) ;
   - `app/src/main/java/com/pocketstudio/app/record/RecordBar.kt` (277 lignes ; `RecordBar(… compact)` l. 92), seulement si la carte de prise doit se replier ;
   - tests neufs sous `app/src/test/java/com/pocketstudio/app/synth/`.
   - Hors liste, jamais : `synth/PianoRoll.kt` (lot T0a), `ui/TrackBar.kt` et `ShareSheet.kt` (captures8), `ui/AppRoot.kt` (lot M ; c'est lui qui donne « Enregistrer ma mélodie » l. 3149, sans y toucher), `ui/DoneBar.kt`, les textes `i18n/*` et `guides/*` (T0c). Besoin d'un de ces fichiers = une ligne au chef.
4. Rejeu d'abord : `PS_REJEUX_SELFTEST=$(mktemp -d) scripts/bench/rejeu-defauts.sh t4-26-prise-clavier` sur ta base : « oracle non tenu » attendu. Commit « rouge: » avec un test Robolectric qui le dit aussi (phone 360 × 780, police 200 %, Mode Simple, Clavier ouvert : `key_62`, `key_65` et le bouton de prise ne sont pas tous entiers à l'écran sans défiler).
5. Défaut et phrase d'acceptation :
   - t4-26 : « en fr, deux mains, phone à 200 %, Mode Simple : depuis le Clavier, je touche les touches 62 et 65 puis « Enregistrer ma mélodie » et la prise démarre. »
   - Preuves :
     - le test du point 4 passe au vert : à 200 %, les touches 62 et 65 et « Enregistrer ma mélodie » sont dessinées en entier sans geste de défilement ;
     - même test à 100 % et 130 % (phone 360) et sur tablet8 : rien ne recule (touches à la même hauteur qu'avant à 100 %) ;
     - `rejeu-defauts.sh t4-26-prise-clavier` : « oracle tenu », rejeu inchangé.
   - Piste (à toi de choisir, une ligne dans `docs/decisions/tour-8-clavier200.md`) : à grande police, replier l'en-tête (consigne, « Nouveau son ») ou placer la carte de prise à côté des touches comme en paysage, plutôt que réduire les touches. Ne pas toucher le Mode Pro.
6. Tests ciblés : `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*synth*' --tests '*Synth*' --tests '*RecordBar*'`. Captures : celles du Clavier, regardées en entier (écran complet, voisins comparés, `docs/DESIGN.md` principe 11), Simple et Pro, 100 % / 130 % / 200 %, phone 360 et tablet8.
7. Rapport `docs/audits/ultra/tour-8-clavier200-rapport.md`. Puis :
   - dans `corrections.tsv`, ta ligne passe à « tenté » ;
   - `PS_REJEUX_SELFTEST=$(mktemp -d) scripts/bench/rejeu-defauts.sh` (tous tenus) ;
   - `pre-vol.sh --avant-relecture tour-8 clavier200` ;
   - relecture neuve en dernier commit ;
   - `pre-vol.sh tour-8 clavier200` doit dire PRÊT.
8. Parallèle et place :
   - libre à côté de M, T0a, T0b, T0c, B, S, N (fichiers disjoints) ;
   - après captures8 (même dossier `synth`, captures du Clavier) ;
   - place proposée : dans le paquet « M + T0a » ou juste après (paquet de trois au plus).
   - Contrôle : siège C-b (rejeu t4-26).
9. Une ligne : « <NOM> fini lot clavier200 : <branche> <sha>, corrigés N/1, rejeux ajoutés R, demandes : …, laissés de côté : … ; pré-vol PRÊT ; self-test V verts, 0 rouge ; rejeux tous tenus ».

> **Demander au chef.** Un fichier hors de ta liste, un doute sur la phrase d'acceptation, un conflit avec un autre lot, un test qui casse hors de ta zone : une ligne au chef (`send_message`), puis tu continues le reste de ta fiche en attendant sa réponse. Jamais de question à Benjamin, jamais de décision hors de ta fiche.
> **Travailler en silence.** Aucun récit, aucun résumé, aucun texte pour Benjamin dans ta discussion. Tu n'écris qu'au chef, une ligne, à trois moments : « prêt », « demande / bloqué », « fini ». Sorties de commandes filtrées (`tail`, `grep`).
