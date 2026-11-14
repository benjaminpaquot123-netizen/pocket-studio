# Prompt pour Fable 5.2 : audit du plan du tour 6 (avant son lancement)

À coller tel quel dans une nouvelle discussion, sur la branche `ccr-80af1a1b-mn0wec` du dépôt `benjaminpaquot123-netizen/pocket-studio`, quand Benjamin a du quota.

---

Tu audites **un plan**, pas l'application, et tu ne l'exécutes pas. Le plan : `docs/audits/ultra/PLAN-TOUR-6.md`. C'est l'étape 5 de `docs/audits/ultra/PLAN-ACTION-AUDITS.md` : un petit tour de contrôle, avec 6 testeurs simulés, un lecteur et une synthèse (8 agents, 3 au plus en même temps). Il vient après les étapes 1 à 4 (banc réparé, dix chantiers de l'appli fusionnés) et après le plan test APK sur le téléphone de Benjamin. Ton travail : dire si ce plan tiendra ses promesses **avant** qu'on paie pour le jouer.

Benjamin ne code pas et ne lit pas le code. Il lira ta réponse, ou l'écoutera : écris en français simple.

## Branche
Lis la branche `ccr-80af1a1b-mn0wec`. Si `docs/audits/ultra/PLAN-TOUR-6.md` n'y est pas, arrête-toi et dis-le en une ligne : le plan n'a pas encore été poussé.

## À lire (et seulement ça)
- Le plan : `docs/audits/ultra/PLAN-TOUR-6.md`.
- Ce qui le commande : `docs/audits/ultra/PLAN-ACTION-AUDITS.md` (§ 2 et étape 5 ; Benjamin a répondu 2A : 6 testeurs).
- Ce que les audits précédents ont appris :
  - `docs/audits/ultra/META-AUDIT-2.md` : ton propre audit de la méthode, après le tour 5 ;
  - `docs/audits/ultra/tour-5-synthese.md` (§ 1, § 5 à 7) ;
  - `docs/audits/AUDIT-PRISE-EN-MAIN.md` (§ 2 et § 7 « œil neuf ») ;
  - `docs/audits/ultra/AUDIT-BANC-CREDIBLE.md` (§ 1 et 2) ;
  - `docs/audits/AUDIT-ULTRA.md` (§ 12.3, 12.10 et 12.11).
- Ce qui a changé depuis : `docs/PROGRESS.md` (les 30 premières lignes : entrées du 2026-11-10 au 12), `docs/AIDE-FUSIONS-TESTS.md`, `docs/audits/ultra/auto-audit-test-apk-2026-11-12.md`, `docs/PLAN-TEST-APK.md` (les feux), `docs/tests-telephone/` (le rapport v6 s'il y est déjà).
- La mécanique, seulement quand une affirmation l'exige :
  - les consignes : `docs/audits/ultra/GABARIT-CONSIGNES-TESTEUR-V2.md`, `GABARIT-CONSIGNES-EQUIPES-V2.md`, `docs/audits/GABARIT-RAPPORT-TESTEUR.md` ;
  - le tour 5 : `docs/audits/ultra/sieges/tour-5/` (`sieges.tsv`, `tableau-de-bord.md`, `hors-banc.tsv`, `critere1/bilan.txt`) ;
  - les fichiers de suivi : `verdicts.tsv`, `defauts-caches.tsv`, `revient.tsv`, `carte-ecrans.tsv`, `rejeux-attente/`, `rejeux/` ;
  - les scripts : `scripts/bench/{fabrique-sieges.py,fuse-reports.py,tableau-de-bord.py,rejeu-defauts.sh,rejeu-nouveau.sh,selftest.sh}` ;
  - les réglages des sièges : `app/src/test/java/com/pocketstudio/app/human/SeatOptions.kt`.
- Ne relis pas les tours 0 à 4, ni les rapports de lots `tour-5-*-rapport.md` / `-relecture.md`, sauf pour vérifier une affirmation précise.

## Six questions
1. **Trous** : qu'est-ce que le plan oublie ou laisse flou ? Pense à la condition d'entrée, au commit de départ, aux fichiers que chaque étape doit écrire, et à qui écrit `verdicts.tsv`. Pense aussi à ce qui se passe si un rejeu, un siège ou la CI échoue.
2. **Coût** : l'estimation (environ un tiers du tour 5, une journée) est-elle crédible ? Où le quota peut-il exploser ? Par exemple : les images du siège œil neuf, la séparation lente du Remix sous le banc, les sièges relancés, les lots de correction qui suivent, les relectures refusées (13 lots sur 29 au tour 5).
3. **Faux défauts** : qu'est-ce qui, dans ce plan, fera remonter des défauts qui n'en sont pas ? Pense aux limites du banc (son, chiffres, temps, système), aux rejeux au pixel après les chantiers, à la tablette à 130 %, aux langues, aux promesses du catalogue, aux points du téléphone, et à un œil neuf qui jugerait le design sur du texte.
4. **Défauts manqués** : avec 6 sièges au lieu de 24, quelles zones ou quels écrans ne seront vus par personne ? Il y avait 9 zones dans la synthèse du tour 5 et 36 écrans dans `carte-ecrans.tsv`. Quels défauts ne se voient que sur le téléphone ? La répartition des appareils, des modes et des langues est-elle la bonne ?
5. **Leçons** : le plan applique-t-il vraiment ce que les audits précédents ont appris ? Notamment : les 5 changements de ton méta-audit 2, la liste « œil neuf », les gestes sauvés pour rejouer, le « revient » enfin calculable, l'oracle « a fini » qui n'est pas « a réussi », le changement d'échelle entre deux tours, et la boucle du test APK (70 % de l'effort sur le robot). Dis ce qui est appliqué, ce qui l'est mal et ce qui manque.
6. **Mesure** : les critères de réussite sont-ils calculables sans jugement ? Les deux notes refaites (banc crédible, prise en main et design) seront-elles crédibles ? Qui note, sur quelle grille, et l'auteur d'une note peut-il être celui qui en profite ? Le plan dit lui-même que 6 sièges ne remplissent pas le critère d'arrêt du § 12.10 : quelle conséquence en tirer ?

## Règles
- **Lecture seule.** Pas de Gradle. Aucun script du banc ni de `scripts/human-*`. Aucun testeur lancé, aucun siège, aucun rejeu, aucune CI.
- Tu ne pousses rien. Tu ne fusionnes rien. Tu ne commites rien, avec une seule exception permise si tu le veux : ton rapport `docs/audits/ultra/AUDIT-PLAN-TOUR-6.md`, commité sur **ta propre branche** de session, jamais sur `ccr-80af1a1b-mn0wec` ni sur `main`. Ta réponse dans la discussion fait foi : elle doit être complète sans ce fichier.
- 4 agents au plus. Par exemple : deux analystes (questions 1 à 3, puis 4 à 6), un sceptique qui rouvre chaque chiffre et chaque fichier cité, et toi qui rédiges. Pas plus de 2 en même temps.
- Chaque affirmation porte sa preuve (fichier et ligne, ou siège et geste). Ce que tu n'as pas pu vérifier est marqué « (non vérifié) ».
- Ne refais pas les audits précédents. Juge le plan à leur lumière.

## Ta réponse (120 lignes au plus)
1. **Verdict**, en une ligne : « prêt », « prêt après corrections » ou « à refaire ». Puis trois lignes de pourquoi.
2. **Corrections numérotées**, de la plus importante à la moins importante. Pour chacune : ce qu'il faut changer, à quel endroit du plan (section), pourquoi (avec la preuve), et ce que ça coûte. Sépare celles qu'il faut faire avant le lancement de celles qui peuvent attendre.
3. **Questions pour Benjamin**, seulement ce qu'il est le seul à pouvoir trancher : numérotées, 2 à 4 options chacune, la recommandée en premier et marquée « (conseillé) », la conséquence en une ligne, réponse attendue du type « 1A, 2B ».
4. **Résumé pour Benjamin** en 5 lignes, sans jargon, lisible à voix haute.
