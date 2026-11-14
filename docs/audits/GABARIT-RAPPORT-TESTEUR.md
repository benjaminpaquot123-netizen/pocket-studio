# Rapport d'un testeur IA (gabarit v2, méthode d'audit v2 du 2026-11-06)

Remplace le gabarit v1 (tours 0 à 4). Pourquoi : `docs/audits/ultra/META-AUDIT.md`, question 5. Le script de fusion (`scripts/bench/fuse-reports.py`) lit les deux versions ; seul le v2 permet de reconnaître le même défaut chez deux testeurs de langues différentes.

Règles :
- Un bloc par siège, **60 lignes au plus, 10 défauts au plus**, un constat par fiche (une liste de mots faux = une fiche par mot, ou la ligne `mots-faux:`).
- `geste:` = le numéro `#N` du geste fautif (celui que tu as fait), pas l'écran que tu regardais ensuite : le script lit alors dans le journal le bouton vraiment touché et l'écran où tu étais. `ecran.vu:` = l'écran où tu as constaté le défaut.
- `cible:` = la ligne de l'écran copiée telle quelle (langue de l'écran), pas une paraphrase.
- Une promesse = une ligne `promesse:` (5 au plus), avec l'`id` donné par le brief ; « en partie » est interdit : on découpe en deux promesses.
- **Le banc n'est pas l'appli** : ce qui vient peut-être du banc va dans `banc?:` (fiche) ou dans `incidents-banc:` (le banc ne répond plus, attend longtemps). Jamais une fiche « bloque » parce que l'écran n'est pas revenu.
- Un défaut `mission-impossible` ou `contourne` de plus de 3 gestes doit avoir été **revu** (refait après un Retour ou par un autre chemin) : sinon `revu: non`, et il compte au plus « moyen ».
- Gravité = conséquence vérifiable : la mission est-elle encore faisable, et en combien de gestes de détour ? (`mission-impossible` = bloquant ; `contourne-en-N-gestes` = grave si N > 3, moyen sinon ; `finition` = mineur.)
- Aucun jugement sur le son (qualité, mixage, niveau) : une fiche qui en contient est écartée, pas le rapport entier. « Ça sonne » (un son part) est permis.
- Pas de note sur 10. Les chiffres (gestes, premier son, buts atteints, fin) sont ceux du banc (`mesures.json`), pas du testeur.

```
siege: <lettre> | profil: <id court, ex. enfant-6-ne-lit-pas> | lang: <code> | device: <profil> | font: <100|130|200> | mode: <simple|pro, à la fin>
mission: <nom> | buts: <les 3 ids donnés par le brief>
promesse: id=<id du brief> | texte: <copié de l'écran> | ecran: <n°> | tenue: <oui|non|pas vue> | pourquoi: <une ligne>
hesitations:   (3 au plus)
  - ecran: <n°> | cherchais: <une ligne> | regarde: <une ligne>
abandon: ecran <n°> | parce que <une ligne>   (ou « jamais »)
incidents-banc:   (0 à n ; ne comptent pas comme défauts)
  - geste: <n°> | attente: <secondes> | vu: <une ligne>
defauts:   (10 au plus)
  - geste: <n° du #N> | ecran.vu: <n°> | cible: <ligne copiée de l'écran> | geste.type: <touche|appui-long|glisse|ecris|retour>
    attendu: <une ligne> | obtenu: <une ligne>
    symptome: <rien|mauvais-ecran|plantage|texte-faux|texte-coupe|cache|hors-pouce|sans-nom|sans-aide|lent|perdu|incompris>
    gravite: <mission-impossible|contourne-en-N-gestes|finition> | revu: <oui: ecran n°|non> | banc?: <non|son|temps|moteur|micro|rythme|clavier|fichier|horloge|gel> | promesse: <id ou ->
mots-faux:   (facultatif, une ligne par mot)
  - ecran: <n°> | vu: <mot> | attendu: <mot>
defauts-avant:   (siège de contrôle seulement : une ligne par défaut visé par le lot, dans l'ordre du brief)
  - <id du défaut visé> | <corrigé|autre|encore là|pas atteint> | ecran: <n°> | <ce que tu as vu, avec tes mots>
```

Rubrique `defauts-avant` (siège de contrôle, `docs/audits/ultra/PLAN-REPARATIONS-CONSTATEES.md`, règle 3) : le brief donne, pour chaque défaut visé, sa phrase d'acceptation (ce que tu dois voir, avec ses conditions). Tu refais le parcours et tu réponds par un seul de ces quatre mots :
- `corrigé` : tu as vu exactement ce que dit la phrase ; `ecran:` = le numéro de l'écran où tu l'as vu (obligatoire : un « corrigé » sans écran ne compte pas) ;
- `autre` : la chose a changé mais la phrase n'est pas tenue ; dis avec tes mots ce que tu vois à la place ;
- `encore là` : le défaut est toujours là, comme avant ; `ecran:` = où tu l'as revu ;
- `pas atteint` : ton budget ou un autre défaut t'a empêché d'arriver jusqu'à cet endroit.
Seul un `corrigé` avec son écran fait passer la réparation à « constaté » (`scripts/bench/reparations.py --constate`). Ces lignes ne sont pas des fiches de défaut : un défaut neuf va dans `defauts:`.

Clé de fusion calculée par le script : route + bouton touché au geste `#N` (lus dans le journal) + famille de symptôme. Une grappe vue par deux sièges est un défaut confirmé sans juge ; une clé déjà « corrigée » qui revient est marquée `[revient]`, une clé classée « banc » qui revient `[banc contredit]`.
