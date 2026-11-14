# Ce qu'a apporté le méta-audit de Fable 5.1 ultracode

Comparaison des tours 3 et 4 (méthode v1) avec le tour 5 (méthode v2, 24 sièges, lots A à F).
Sources : `docs/audits/ultra/META-AUDIT.md`, `AUDIT-ULTRA.md` §12, rapports et `mesures.json` du tour 5,
`clusters-complet.md`, `tableau-de-bord.md`, `verdicts.tsv`, `base.tsv`, git log (85fc8c3 à e9bae3e).
« B c 23 » = lot B, siège c, geste 23. Bilan fait par lecture seule ; le tour 5 n'est pas encore trié (aucune ligne dans `verdicts.tsv`).

## 1. En bref

Le méta-audit a surtout réparé **l'instrument de mesure** (le banc et la façon de compter), pas l'appli : un tour de
24 sièges sans gel long ni siège perdu (un seul gel de 815 ms au démarrage de B a), des rapports lisibles par programme. Mais les « revient » restent invisibles et le
critère d'arrêt ne peut toujours pas être calculé.

## 2. Gels du banc et sièges perdus

| | Tour 3 (v1) | Tour 4 (v1) | Tour 5 (v2) |
|---|---|---|---|
| Gel au renommage | B a (dès le geste 9), E c (5 min, deux fois) | C a 63 et E a 7 : 5 min sur ✕ « Effacer », renommage impossible | A c : promesse « Renommer » tenue, « chaque écran revient tout de suite » (écrans 8-12, 24-28, 50-54) |
| Attentes du banc > 8 s | non mesuré | non mesuré | 0 ligne « BANC repos non venu » dans les 24 journaux ; un seul « GEL 815 ms » au démarrage (B a) |
| Sièges coupés | 3 (lot F : manque de mémoire, disque plein) | 3 (lot A : A b, A c, A d sans fin, « daemon disappeared ») + lot F jamais joué | 0 coupé, 0 lot non joué, 24 « fin » sur 24, 0 plantage |

Reste : après ✕ « Effacer », le champ perd le curseur (A c 61, D c 4, D b 16, « banc? ») ; T4-19 attend le téléphone de Benjamin.

## 3. Fausses alarmes « banc » : avant mêlées, maintenant mises à part

- **v1** : environ 90 fiches classées banc sur les tours 0 à 3 (≈ 25 au tour 3), et un verdict « banc »
  démenti ensuite (prise au clavier T3-26c, par la CI 243) ; deux autres ont été étendus sans preuve et restent
  « non tranchés » dans `verdicts.tsv` (« Jouer = une boucle » T3-15/T4-16, « ■ Tout » T3-02/T4-01). Ces fiches
  comptaient d'abord comme graves, puis étaient retirées à la main.
- **v2** : 23 fiches marquées « banc? » (12 % des 190 ; deux lignes d'incident le portent aussi) et 13 lignes « incidents-banc » dans 10 sièges sont
  rangées à part et **ne comptent pas comme graves**. Les incidents sont réels et utiles : séparation du Remix très
  lente (C b 5 : environ 13 min ; E c 12, F d 26 : durée non chiffrée par le siège, non vérifié), démarrage lent (F a, F c), écran sans texte (B a 19, D b 7).
- **Limite** : cette case peut aussi cacher un vrai défaut. F b 63 met « compteur bloqué sur A Takt 1/2 » en
  incident du banc, alors qu'A b (promesse t5-Ab-2 non tenue) voit le même compteur figé : c'est T4-17 ; appli ou
  banc : non tranché. Combien des 23 « banc? » sont vraiment le banc : **on ne le sait pas encore** (pas de tri).

## 4. Graves déclarés par siège

Tableau de bord : 2,25 (tour 3), 1,35 (tour 4), **0,33 (tour 5 : 8 graves pour 24 sièges)**.
**Ces chiffres ne se comparent pas** : en v1 le testeur écrivait « grave » ; en v2 il écrit la conséquence
(3 « mission-impossible », 72 « contourne-en-N-gestes », 115 « finition ») et le programme décide : plus de 3 gestes
= grave, un défaut non revu ne dépasse jamais « moyen ». La baisse vient en bonne partie de cette règle, pas seulement
d'une appli meilleure. Sûr : les graves restants sont mieux étayés (geste n°, revu à un autre écran).
Les 3 bloquants du tour 5 : A a 27 (impossible de changer de partie dans l'éditeur, hindi), B c 23 (« Ich mach's »
montre un réglage Swing introuvable, chapitre 1 impossible ; « bloquant » non vérifié, Swing peut-être plus bas), E a 117 (une prise de 4 notes disparaît après un passage par Pad Live).

## 5. Qualité de la fusion

- **v1** (refusion par le méta-audit, Q5) : tour 3 = 230 grappes dont 1 vue par 2 sièges ; tour 4 = 186 dont 1 ;
  sur les 4 rapprochements des tours 1 à 4, 2 étaient faux. Les écritures non latines étaient jetées.
- **v2** : 176 grappes pour 190 fiches, **5 vues par ≥ 2 sièges**, écritures hindi, arabe, chinois, coréen,
  japonais gardées. Vérifiées une à une :
  - justes : Retour depuis « Mes projets » (A c 12 + E a 105) ; texte « copie de la partie » inversé, en italien
    et en allemand (D c 32 + F b 30, vrai rapprochement entre langues) ; outils de découpe cachés en mode Phrases
    (C b 22 en espagnol + F d 58).
  - à moitié juste : mots anglais en hindi et en arabe (A a 9 + B d 13), mélangés à deux autres défauts (A a 20, B d 17).
  - fausse : « Loop » en anglais (B d 33) collé à « Octave 3 » (E a 12), sans rapport.
- **Paires évidentes manquées** (la clé dépend encore de l'écran et du symptôme choisis) : « Tout couper » de Pad
  Live laisse jouer le morceau (A d 10 + E d 11) ; la prise ne part pas au premier pad (A d 49 + E d 32) ;
  le .zip absent de « Mes exports » (C c 41 + C d 68) ; compteur figé (A b 14, F a 5, F c 8 ; F b 63 est un incident, pas une fiche).
- Verdict : net progrès (5 au lieu de 1), mais environ 3 vraies sur 5 et au moins 4 paires ratées.

## 6. « Revient » : toujours aveugle

Le tableau dit 0 « revient » au tour 5. C'est faux : `base.tsv` n'a toujours que les 29 clés du tour 0
(31 lignes), car `--maj-base` n'a jamais été passé sur les tours 1 à 4. Retours visibles à l'œil :
« Ma mélodie » prend un autre son (A a 30, promesse t5-Aa-3 non tenue = T4-27) ; le tuto nomme un pad « Perc »
qui n'existe pas (B a 43 = famille T4-30) ; le .zip absent de « Mes exports » (C c 41, C d 68 = T4 C a 53) ;
compteur figé (= T4-17) ; « Tout couper » de Pad Live (= famille T3-02 / T4-01) ; mots anglais en hindi (= T4-42).
Le changement 4 est livré dans le script, mais il tourne à vide.

## 7. Missions mesurées par l'oracle

- Tours 3 et 4 : **0 `mesures.json`** (missions jugées à la lecture). Tour 5 : **24 sur 24**, avec gestes, retours, touchers à vide, gels. Résultat : 20 réussies, 4 partielles
  (B a : 2 chapitres sur 15 en 108 gestes à 200 % ; B c : tuto Pro bloqué ; C a : défi sans étoile ; D b : synthé).
- Limite : l'oracle regarde l'état final, pas les buts. A a est « réussie » (morceau partagé) alors que le siège
  n'a jamais pu changer de partie (bloquant A a 27).

## 8. Respect du gabarit

| | Tours 3-4 (v1) | Tour 5 (v2) |
|---|---|---|
| Promesses | 1 ligne par siège, sans id | 90 lignes avec id (2 à 5 par siège) ; 38 tenues (42 %) |
| N° du geste fautif | non (écran seulement) | oui pour les 190 défauts |
| Gravité par conséquence, « revu », « banc? » | non | oui pour tous les défauts |
| Section « incidents-banc » | absente | 24 sièges sur 24 |
| Plafonds (10 défauts, 60 lignes) | 12 défauts | respectés (A a pile 60 lignes) |

Écarts : ids de promesses fabriqués siège par siège dans les briefs (« t5-Aa-1 », `fabrique.py`), pas les 120 du catalogue, donc la condition D
(« 85/120 vues ») **ne veut rien dire** ; une ligne d'incident vide (C d) ; A a lie son bloquant 27 à une promesse « tenue ».

## 9. Bugs et causes trouvés en appliquant le méta-audit

1. **Le gel du renommage venait très probablement du banc** (`verdicts.tsv` garde T4-19 « non tranché » tant que le téléphone de Benjamin n'a pas confirmé). L'interrupteur « ne pas attendre le repos » cherchait un réglage
   de Compose sur la mauvaise classe, échouait sans rien dire, et ce depuis le tour 3 (531564f). Le banc attendait
   donc 4 × 60 s = 240 s sur ✕ « Effacer ». Trouvé parce que la garde `BenchTour4Test.renameAfterTheClearButton`
   devait passer au vert (changement 1). Corrigé dans d3eccff : 11 s au lieu de 240 s, plus une attente plafonnée à 8 s.
   Les 4 verdicts contradictoires du gel (tours 3-4) n'avaient pas vu ce bug muet.
2. **Le moteur prêté bouton par bouton** (listes de tags tenues à la main) : remplacé par un moteur toujours là
   (changement 2, 350079e). Effet visible : « Jouer » joue tout le morceau (A b, promesse t5-Ab-1 tenue) et
   « ■ Tout » de la vue Live arrête tout (A d, t5-Ad-1 tenue).
3. Ce moteur toujours là a révélé **trois trous du banc cachés par les listes** : les pads prêts du Pad Live, que le faux moteur ne savait pas
   fabriqués (73e50d0), la prise de performance jamais « marquée » (elle restait « Prêt »), les sons de fête (OGG)
   que le banc ne savait pas décoder (d3eccff).
4. **Un vrai défaut de l'appli, T4-26** : les notes d'une prise au clavier allaient dans la partie entendue, et
   l'écran montrait une autre partie (« 4 notes », aucune visible). Racine vue par la CI 243 avant le méta-audit,
   prouvée par `Tour4AppProofTest`, corrigée dans d3eccff. Au tour 5, A a voit ses notes (t5-Aa-1 tenue).
5. Deux tests du tour 4 étaient eux-mêmes faux (touches hors gamme, morceau à deux parties qui ne se chargeait pas).

## 10. Ce que le méta-audit n'a pas vu, ou a mal jugé

- **Il n'a pas été contre-vérifié** (il le dit lui-même : quota épuisé avant les agents sceptiques).
- **Fichiers d'aide partagés entre testeurs** : le raccourci de commande de E a, rangé dans un dossier commun,
  a été réécrit par le testeur de E c ; 3 gestes sont partis dans le mauvais siège (E a 2, E c 1 et 13).
  Le méta-audit traitait le partage de Gradle et de git, pas celui des testeurs.
- **Le portail du banc contourné dès le premier usage** : 117 auto-tests lancés : 78 verts, 1 rouge,
  38 sautés (`selftest-gradle.log`) ; le seul rouge, `Tour3RenameIdleTest`, dépendrait de l'ordre des tests (2e lancement
  de l'appli dans la même classe ; non vérifié : c'est la note du lanceur, pas une preuve). Les 6 lots
  sont partis avec `PS_SKIP_SELFTEST`. Le méta-audit n'avait pas prévu de test instable.
- **Préalables du banc P1 à P6** : le méta-audit les nomme (Q4 §5) mais les laisse hors du top 10. Ils manquent
  toujours : défi sans geste « rythme » (C a, P3 : 0 étoile, mission partielle), pas d'horloge avancée de 3 jours
  (A c, P2), pas de MIDI simulé (P6), pas de sélecteur de fichiers hors Remix (P1).
- **Le « revient »** : le changement 4 ne prévoyait pas de remplir `base.tsv` avec les tours 1 à 4 (§6).
- **Le changement d'échelle des gravités** rend la courbe trompeuse (§4) ; rien ne l'annonce dans le tableau.
- **Conditions G, H, I** (relecture, CI, téléphone, couverture) : toutes « ? » ; personne ne remplit
  `hors-banc.tsv`, donc l'arrêt de l'audit reste incalculable.
- Séparation du Remix lente sous le banc (environ 13 min chez C b ; non vérifié pour les autres sièges) : coût non traité.

## 11. Coût

- Fable 5.1 : une session, environ 2 h (commits 15:10 → 17:14 le 5 nov.), arrêtée faute de quota de Benjamin.
- Application des 10 changements : environ 3 h 30 (17:14 → 20:45), 3 commits : 350079e (62 fichiers, 1 431 lignes)
  puis 73e50d0 et d3eccff pour réparer le banc.
- Tour 5 : environ 1 h (lancement 20:57, commit e9bae3e à 21:52). Au tour 4, le seul lot A a perdu 3 sièges.

## 12. Verdict

1. Oui, ça valait le coût : pour environ 5 h 30, le banc ne gèle plus, 24 sièges sur 24 vont au bout, et la cause du « gel du renommage » côté banc est trouvée (reste à confirmer sur le téléphone).
2. L'apport principal est un instrument fiable (banc, mesures, gabarit), pas des défauts nouveaux de l'appli ; la baisse des graves vient surtout du nouveau mode de calcul.
3. À garder : auto-tests avant chaque lot, moteur toujours là, `mesures.json`, gabarit v2, « banc? » à part, tableau de bord.
4. À finir avant le tour 6 : remplir `base.tsv` (tours 1-4), relier les promesses aux 120 ids, réparer le test instable, raccourcis privés par siège, P1-P3.
5. Refaire un méta-audit tous les 3 tours (ou après 2 tours sans convergence), et cette fois avec les agents sceptiques jusqu'au bout.
