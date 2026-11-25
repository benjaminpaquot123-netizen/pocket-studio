# Tour 9, lot « porte-paquet » : relecture neuve

relu : 01ded6d2

Deux relectures par un sous-agent neuf, qui n'a pas écrit le code (fichiers : `scripts/bench/porte-paquet.sh`, `scripts/bench/fusion-equipes.sh`, `scripts/bench/test_porte_paquet.py`, rapport et décisions).

Première relecture (sur f9e48dfe) : 3 graves, tous corrigés en 0121aa26 avec un test rouge avant, vert après :
- le juge montait dès la trace de la première zone (les fusions suivantes du même paquet n'avaient pas encore leur trace) : ces écarts « dus » ne comptent plus ;
- le faux juge du test affichait toujours 0 : il imprime maintenant les vrais écarts de `merge_trace_gaps` ;
- l'étape « traces » croyait un dossier de paquet fabriqué : elle vérifie sujet, second parent, ascendance et tête testée (sortie 3 sinon).
Mineurs corrigés aussi : plus de `reset --hard` (retour par `--keep`, refus clair sinon), dossier du paquet gardé en cas de rouge, message « créée depuis origin ».

Seconde relecture (sur 0121aa26, puis rapport 01ded6d2 relu : texte seul) : 0 bloquant, 0 grave. Mineurs notés, non corrigés :
- écrire `tete` à la main contourne la vérification ; sujet, parent et ascendance n'ont pas chacun leur test ;
- la trace écrit le compte brut du juge (le compte « dû » n'est que dans la sortie) ;
- `--limite` ne remplace pas une limite `BASH_*` plus basse ;
- une annotation sur la même ligne qu'une classe peut la cacher au contrôle des filtres ;
- un `PS_PAQUET_ETAPE` hérité passe dans le repli zone par zone ;
- une référence origin périmée est prise sans `fetch`.

Preuves rejouées : `test_porte_paquet.py` 14 verts, `test_socle.py`, `test_constate.py` verts, `selftest.sh` vert (389 verts, 0 rouge), empreinte pre-vol « 18 90127c7c59 » inchangée.

Verdict : 0 bloquant, 0 grave
