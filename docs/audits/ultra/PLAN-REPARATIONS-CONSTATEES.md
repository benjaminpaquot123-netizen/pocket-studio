# Plan : une réparation n'est « faite » que quand un testeur la constate

Écrit le 2026-11-13 dans la discussion « Plan d'audit utilisateur », à la demande de Benjamin, pour le chef d'orchestre.
> « Des réparations sont faites, mais à l'audit suivant elles n'apparaissent pas comme faites. C'est un problème récurrent. Comment le résoudre ? »

**Rien n'est lancé, aucun fichier de l'audit n'est modifié par ce plan.** Il propose un complément à la méthode v3 (`docs/audits/AUDIT-ULTRA.md` § 13.5) et un lot du banc.

## En trois lignes
1. Aujourd'hui l'équipe qui répare décide seule que c'est « corrigé », avec un test sur un bouton isolé. Le testeur, lui, juge un parcours, dans sa langue, sur son appareil. Les deux ne parlent pas de la même chose.
2. On écrit avant de réparer ce que le testeur doit voir après, on prouve la réparation dans les conditions où le défaut a été vu, et un testeur neuf la constate **avant** la fusion.
3. On ne dit plus « corrigé » à Benjamin que pour ce qu'un testeur a constaté. Le reste porte son vrai nom : pas pris, tenté, preuve rejouée.

## 1. Le constat
| Contrôle (13 novembre) | Réparés | Sur | Source |
|---|---|---|---|
| Tuto, trois langues | 12 | 32 | `AUDIT-NUIT-TOUR-6.md` § C |
| Remix voix (B a, espagnol, main gauche) | 1 | 8 | `EN-COURS.md`, 12 h 30 |
| Exports | 1 | 7 | `EN-COURS.md`, 12 h 30 |
| Pad Live | 2 | 3 | `EN-COURS.md`, 12 h 30 |

Le détail du contrôle Remix (`sieges/tour-6/controle/Ba`, branche `claude/benstudio-tour6-Ba-controle`, code `b3617da2`, rubrique « defauts-avant ») :
- **1 corrigé** ;
- **3 « autre »** : la réparation a suivi la phrase de la fiche, pas le but de la personne. Exemples cités par le testeur : « la page glisse maintenant, mais les puces Voz/Batería/Bajo/Resto restent hors pouce » ; le bouton se touche au pouce, mais sa phrase d'aide reste coupée sous un bouton flottant ;
- **4 « encore là »**. Non vérifié ici : s'ils étaient dans un lot de correction. S'ils ne l'étaient pas, « 1 sur 8 » mélange les réparations ratées et les défauts jamais pris.

Même schéma au tuto japonais : 6 corrigés, 7 « autre », 3 encore là, sur 16.

Déjà dit par l'audit de la nuit, et à garder : les tests des lots sont vrais, mais « ils prouvent un bouton, pas un parcours » ; le contrôle par un testeur n'existait pas avant la fusion ; un seul rejeu protégeait l'appli. La v3 y répond pour le tuto et pour les bloquants et graves. Ce plan étend la réponse aux autres zones et aux défauts moyens, qui sont la masse.

## 2. Les trois causes
1. **Personne n'écrit ce que le testeur doit voir après.** La fiche dit le symptôme (« hors pouce », « incompris ») ; l'équipe en déduit une cause et répare la cause. Souvent, le symptôme reste (3 sur 8 au Remix, 7 sur 16 au tuto japonais).
2. **La preuve n'est pas jouée dans les conditions du constat.** Défaut vu en espagnol, main gauche, téléphone, dans l'appli entière ; preuve : un test d'un écran isolé, souvent en français à 100 %.
3. **L'équipe se déclare elle-même « corrigé ».** `corrections.tsv` n'a qu'un état pour une réparation (255 lignes « corrigé »), posé par le lot. Le testeur n'arrive qu'après la fusion, quand défaire coûte cher.

## 3. Les cinq règles (complément au § 13.5)
1. **La phrase d'acceptation.** Au tri, chaque défaut retenu pour un lot reçoit une phrase vue du testeur, avec ses conditions : « en espagnol, main gauche, téléphone à 100 %, Mode simple : depuis le Remix séparé, je touche "Voz" au pouce et la piste voix s'ouvre ». Le trieur l'écrit, pas l'équipe qui répare. Le lot répare cette phrase, pas la fiche.
2. **La preuve dans les conditions d'origine**, par le banc, appli entière, avec le `start.txt` du siège qui a vu le défaut (langue, appareil, police, mode, options) :
   - bloquant, grave, moyen : un rejeu « non tenu » avant, « tenu » après (`rejeu-nouveau.sh`, `rejeu-defauts.sh`), visé par étiquettes ;
   - mineur (`finition` : texte coupé, mot faux) : un test ou une capture dans la langue, l'appareil et la police du constat suffit.
   Sans cette preuve, le défaut est « tenté », jamais « corrigé ».
3. **Le siège de contrôle avant le portail, pour toute zone**, comme le tuto l'a depuis le 13 novembre : un testeur neuf, sur la branche du lot, avec le profil et les conditions du siège d'origine, un budget court (la mission du siège d'origine, pas plus), et la liste des défauts visés. Il rend une ligne par défaut : `corrigé`, `autre` (avec ses mots), `encore là`, `pas atteint`. Le lot entre si : aucun bloquant ni grave visé n'est `autre` ou `encore là`, et aucun grave neuf.
4. **Un `autre` retourne tout de suite à la même équipe**, dans le même lot, avec les mots du testeur. Deux allers-retours au plus ; au troisième, le défaut sort du lot, noté « tenté » avec ce qu'on sait, et le lot entre sans lui.
5. **Quatre états, un seul registre.** Dans `corrections.tsv`, l'état d'une réparation devient l'un de :
   | État | Qui le pose | Sur quelle preuve |
   |---|---|---|
   | `pas pris` | le trieur | le défaut n'est dans aucun lot |
   | `tenté` | l'équipe | un commit, sans preuve dans les conditions d'origine |
   | `rejoué` | le portail | la preuve de la règle 2 tenue sur la tête fusionnée |
   | `constaté` | le chef, d'après un siège | la ligne `corrigé` d'un siège de contrôle, avec son numéro d'écran |
   **Seul `constaté` se dit « corrigé » à Benjamin.** Un point d'étape donne les quatre chiffres ; « réparés sur N » compte `constaté` sur les défauts pris, et donne à part les `pas pris`.

## 4. Ce que les programmes doivent apprendre (un lot du banc)
Règle de `CLAUDE.md` : chaque point avec son test rouge puis vert, self-test vert.
1. `corrections.tsv` : les quatre états ; une colonne « phrase d'acceptation » ; une colonne « siège d'origine ». Les lignes d'avant le tour 6 ne sont pas réécrites.
2. Le portail (`fusion-equipes.sh`) : refuse qu'un lot écrive `corrigé` ou `constaté` ; pose `rejoué` lui-même quand la preuve tient après la fusion.
3. Les sièges de contrôle : le banc accepte un dossier de contrôle (aujourd'hui seulement `lot<X>/<a-f>` : note du siège B a) et le gabarit du rapport reçoit la rubrique « defauts-avant » avec les quatre réponses de la règle 3.
4. `tableau-de-bord.py` : une ligne « réparations » avec les quatre chiffres par tour.
5. `GABARIT-FICHES-V3.md` : la phrase d'acceptation dans la fiche du trieur (§ 4), la preuve dans les conditions d'origine dans celle du correcteur (§ 2), la fiche du siège de contrôle (§ 1).

## 5. Pour commencer, sans rien construire
Le tour 6 a déjà ses contrôles. On peut appliquer les règles 1, 3 et 4 à la main dès les lots « remix2 » et « exports2 » :
- le chef écrit la phrase d'acceptation des défauts `autre` et `encore là` des contrôles Remix et Exports ;
- chaque lot rend ses preuves dans les conditions du siège d'origine ;
- le même profil de siège rejoue avant le portail.
Si ces deux lots passent de 1 sur 8 et 1 sur 7 à une nette majorité de `constaté`, la règle est gardée et le lot du banc du § 4 est ouvert. Sinon, on regarde les `autre` restants avant d'aller plus loin.

## 6. Ce que ça coûte, et ce qu'on n'attend pas de ce plan
- Un siège de contrôle par lot, et un rejeu par défaut moyen : chaque réparation coûte plus cher. Il y aura moins de réparations par nuit, mais elles compteront. C'est le sens du verdict de l'audit de la nuit : « corrige trop, trop vite ».
- Pour tenir le quota : des lots plus petits (une zone, huit défauts au plus), et les mineurs groupés par famille avec une seule preuve par famille et par langue.
- Limite : un siège de contrôle reste un testeur simulé. Ce que le banc ne sait pas (le son, le vrai doigt, la vitesse) ne passe pas `constaté` par lui : c'est la liste du téléphone de Benjamin.
- Limite : le compte « corrigé » d'un siège dépend un peu du testeur. D'où la phrase d'acceptation et le numéro d'écran : un `corrigé` se relit.
