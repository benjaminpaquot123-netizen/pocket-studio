# Prompt pour Fable 5.1 (ultracode) : audit de l'audit

À coller tel quel dans une nouvelle session Claude Code sur le dépôt `benjaminpaquot123-netizen/pocket-studio`, branche `ccr-80af1a1b-mn0wec`, quand l'audit ultra est en pause.

---

Tu es un expert en assurance qualité d'applications mobiles et en méthodes de test utilisateur. Tu ne corriges pas l'application : tu audites **la méthode d'audit** qui l'a testée, pour que les prochains tours trouvent plus de vrais défauts, plus vite, avec moins de fausses alertes et moins de défauts qui reviennent.

## Contexte

BenStudio est une appli Android de création musicale (Kotlin, Compose, moteur audio C++). Depuis le 4 novembre 2026, elle passe un « audit ultra » en tours successifs :
- des testeurs simulés (agents qui ne voient que l'écran) pilotent la vraie appli dans un « banc » Robolectric ;
- leurs rapports sont fusionnés en une synthèse par zones ;
- des équipes de correction travaillent en parallèle, puis un intégrateur assemble, une relecture neuve vérifie, et la CI tourne.

Le propriétaire veut auditer « jusqu'à presque parfait ». Le critère d'arrêt : deux tours de suite sans défaut bloquant ou grave confirmé côté appli, sans défaut qui revient, et au moins 90 % des promesses tenues.

## Ce que tu dois lire (tout est dans le dépôt)

1. **Le plan et les règles** :
   - `docs/AUDIT-ULTRA.md` (méthode) ;
   - `docs/CARTE-FONCTIONS.md` ;
   - `docs/GABARIT-RAPPORT-TESTEUR.md` et les autres gabarits ;
   - `CLAUDE.md` (règles du projet).
2. **Chaque tour** dans `docs/audits/ultra/` :
   - synthèse, rapports de zone, banc, intégration, relecture ;
   - les rapports bruts des testeurs et leurs consignes, dans `docs/audits/ultra/sieges/tour-N/` (profils et promesses dans `sieges.tsv`, `consignes-testeur.md`, `consignes-equipes.md`, `notes-banc.txt`, `lot*/*/rapport.md`).
3. **Le banc** :
   - `app/src/test/java/com/pocketstudio/app/human/` (HumanTester.kt, BenchEngine.kt, SeatOptions.kt) ;
   - `scripts/human-tester.sh`, `scripts/human-act.py`, `scripts/bench/*.py`.
4. **L'historique** : `git log --oneline` depuis le début de l'audit ultra, `docs/DECISIONS.md` et `docs/REVIEW.md`.

## Ce que tu dois produire

Un rapport `docs/audits/ultra/META-AUDIT.md` (en français simple, avec des preuves : chemins de fichiers, numéros de tour, de lot, de siège et d'écran). Il répond aux sept questions ci-dessous.

1. **Défauts qui reviennent.** Liste chaque défaut signalé dans au moins deux tours, par exemple :
   - la fenêtre du kit qui remplace le tuto ;
   - les noms des pads qui ne collent pas au tuto ;
   - les bouts de chanson qui écrasent un pad ;
   - « Tout couper » ;
   - le gel en renommant ;
   - la mélodie au clavier « rien n'a été joué ».

   Pour chacun, dis pourquoi la correction n'a pas tenu. Les causes possibles :
   - un seul chemin corrigé ;
   - un test qui ne rejouait pas les gestes du testeur ;
   - une correction qui en a cassé une autre ;
   - un faux verdict « banc » ou « appli ».

   Propose une règle qui l'aurait empêché.
2. **Fausses alertes du banc.** Recense les défauts classés « banc » après coup : export de 3 s, LUFS identiques, Pad Live vide, « Poser » sans effet, dialogues avec champ de texte, position de lecture, micro. Estime ce qu'ils ont coûté (équipes, temps). Propose ce qu'il faut ajouter ou vérifier dans le banc **avant** un tour, avec une batterie d'« auto-tests du banc » à faire passer à chaque lot.
3. **Verdicts banc ou appli contradictoires.** Exemple : au tour 3, l'agent banc a dit « appli » pour le gel du renommage, puis l'équipe zone 3 a dit « banc ». Analyse comment on tranche, et propose une procédure de preuve obligatoire.
4. **Couverture.** Croise la carte des fonctions avec les sièges des tours 0 à 4. Liste les zones jamais ou peu visitées, les combinaisons appareil, police, langue et mode jamais essayées, et les promesses écrites dans l'appli jamais vérifiées. Pour les trouver, cherche dans les textes de l'appli les phrases qui promettent quelque chose, par exemple « en un toucher » ou « ça sonne tout de suite ». Propose les profils des prochains tours.
5. **Qualité des rapports de testeurs.** Le gabarit est-il bon ? Repère :
   - les doublons mal fusionnés ;
   - les gravités incohérentes ;
   - les jugements que le banc ne peut pas faire ;
   - les agents qui ont abandonné trop tôt ou se sont figés.

   Propose un meilleur gabarit et un meilleur brief.
6. **Organisation des corrections.** Six équipes travaillent dans le même dossier et commitent par morceaux. Cela a produit des lignes mal placées, des commits qui embarquent le travail d'une autre équipe, et des manques de mémoire quand deux Gradle tournent en même temps. Propose une meilleure organisation, par exemple :
   - une branche ou un worktree par équipe ;
   - une file d'attente Gradle ;
   - un ordre de fusion ;
   - des tests de non-régression qui rejouent les gestes des testeurs.

   Chiffre le gain attendu.
7. **Critère d'arrêt.** Est-il mesurable et suffisant ? Propose un tableau de bord par tour, avec :
   - le nombre de défauts par gravité ;
   - le nombre de défauts qui reviennent ;
   - les promesses tenues ;
   - les fausses alertes du banc ;
   - la courbe de convergence.

Termine par **les 10 changements les plus rentables**, classés par gain et par coût. Chacun dit concrètement quoi modifier (fichier, script ou règle de `docs/AUDIT-ULTRA.md`).

## Règles

- Ne modifie pas le code de l'application.
- Tu peux écrire `docs/audits/ultra/META-AUDIT.md`.
- Tu peux proposer des modifications de `docs/AUDIT-ULTRA.md` et des scripts du banc, dans une section « Correctifs proposés » avec des diffs. Ne les applique pas.
- Fais vérifier chaque affirmation importante par un agent sceptique : vérification adverse.
- Cite toujours la preuve.
- Commit par chemin uniquement : jamais `git add -A`. Les messages de commit sont en anglais. Pousse sur `ccr-80af1a1b-mn0wec`.
- Pour le propriétaire, termine par un résumé de 10 lignes en français simple, sans jargon.
