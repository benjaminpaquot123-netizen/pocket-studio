# Relecture neuve : lot gr-house (tour 9)

Relu : ebd45e51f

Relecteur : sous-agent neuf, n'a pas écrit le code. Diff relu : `git diff e3e84c07f ebd45e51f -- scripts/packs/recipes/house.json project-model/src/test/kotlin/com/pocketstudio/model/HouseSignatureTest.kt docs/audits/ultra/tour-9-gr-house-rapport.md` (la fusion de `origin/eq/gr-l1b` est ignorée).

- JSON valide (`json.load` OK) ; `recipes_to_kotlin.py` passe sans erreur (37 styles écrits, sortie dans le dossier temporaire du relecteur seulement).
- Acid : motif, accents et glisses font tous 16 pas (le convertisseur exige 16 ou 32 et la même longueur pour accents et glisses) ; 5 accents, 4 glisses ; le cutoff bouge dans les 4 sections ; kit 909 et kick « punch » présents.
- Rien d'enlevé côté pistes de batterie : pour les 9 styles, les lignes (rôle, voix, source) sont identiques avant et après ; mêmes ids, même ordre. L'acid ne perd rien : shaker et cloche sont seulement retenus jusqu'au Drop (liste `drop`).
- Tech house : clap inchangé (même motif, sans kit, kitSounds ni couche), effets validés inchangés, pas de `body`, conga et shaker présents, ligne rim en voix 4 (clap), accords en `double: octave`. Seul changement : le conseil de la ligne rim, désormais juste (« un petit clap », c'est bien la voix clap qui joue, plus « claquement de doigts »).
- `body: true` retiré sur 6 styles, remplacé par `counter: Pluck` et une doublure d'accords écrite (octave ; both pour deep house). L'ensemble du corps L1 n'est pas repris : la montée (riser) imposée disparaît sur deep house et amapiano (voulu et expliqué dans le rapport et le commit 796a1cfca), et la doublure passe de both à octave sur 5 styles (expliqué : dépassement de la plage).
- Le corps L1 n'a jamais été dans l'intégration (`ccr-80af1a1b-mn0wec` n'a aucun `body`) : face à ce que les testeurs ont, rien ne disparaît.
- Test : le retrait de « deux mesures (32 pas) » est cohérent avec le riff d'une mesure ; le test vérifie toujours du réel (tempo, son 303, réglages, accents, glisses, courbes dans chaque section, kick punch), et la longueur reste gardée par le convertisseur.
- Textes français corrects (« doubles croches » corrigé, conseil rim juste). Aucun fichier audio dans le diff, `THIRD_PARTY.md` non touché.

Mineurs (notés, pas bloquants) :
- Texture « Souffle » : le corps L1 l'ajoutait à house, uk garage, speed garage et afro house (sans texture propre) ; elle disparaît avec `body` et le rapport ne le dit pas. Pas une perte face à l'intégration (jamais entrée), mais à écrire dans le rapport ou à remettre par `texture` si on la veut.
- `StyleRecipes.kt` n'est pas régénéré dans le commit (voulu, « à régénérer à la fusion ») : la copie locale non commitée est déjà en retard sur 5fc8d5cac (ancien conseil rim). Le chef doit régénérer à la fusion, sinon HouseSignatureTest tourne sur l'ancien Kotlin.
- Acid : la courbe du cutoff du Groove (0,30 → 0,36) commence plus bas que la fin de l'Intro (0,38) : petit saut vers le bas à l'entrée du Groove.
- Acid : les conseils du shaker et de la cloche ne disent pas qu'ils arrivent au Drop (celui du charley ouvert le dit).
- Le test ne vérifie pas le contre-chant Pluck ni la doublure écrite sur les 6 styles qui ont perdu `body`.

Verdict : 0 bloquant, 0 grave
