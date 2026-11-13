# Relecture neuve du siège son, tour 6 (deuxième passage)
Relu : 6e77ecac6e0280d1d6fd5e054c70b1e749163d89

J'ai relu le delta `86095814..6e77ecac` : `rapport-son.md`, `par-style.tsv`, `kick-par-partie.tsv`, `docs/decisions/tour-5-ecoute.md`, `scripts/son/son6.py` et `tableau_son6.py`. Je l'ai comparé aux données régénérées : `build/son6/son6.tsv` et `kick.tsv` (23 h 28), `build/son6/json-kick/`, `build/son6/report.md`, `build/son6-live/report.md` et `build/son6/cmds/`. Le premier passage, à `86095814`, est dans l'historique git de ce fichier.

## Ce que j'ai recalculé

- **Tableaux reproduits** : `tableau_son6.py build/son6` redonne `par-style.tsv` à l'octet près. `build/son6/kick.tsv` est identique à `kick-par-partie.tsv`.
- **Coup de kick contre le banc** : en pleine bande, les 122 parties restent identiques à `ps_measure --punch` (`json-kick/*.json`), nombre de coups compris.
- **Filtres téléphone dans le code** : passe-haut d'ordre 4 à 300 Hz et passe-bas d'ordre 2 à 10 kHz ; pire cas à 500 Hz et 8 kHz. C'est la proposition de `cibles-genres.md` § 7. L'ancien `kick_lift` n'existe plus : `son6.tsv` n'a plus aucune colonne `coup_*`.
- **Écoute en direct** : `build/son6-live/report.md` donne bien 8 styles sur 8 sans alerte. D'après `cmds/*.pscmd`, les 150 s jouées atteignent le premier Drop ou Refrain de chacun des 8 (le Drop de trance arrive à 111 s, celui de techno à 116 s).
- **26 chiffres du rapport refaits sur les nouvelles données, tous justes :**
  - Coup de kick, médianes : 3,87 / 1,83 / 0,96 dB.
  - Styles sous 3 dB : 7 en pleine bande, 28 au téléphone, 26 sur le pire cas. Sous 2 dB : aucun en pleine bande, 23 au téléphone, 24 sur le pire cas.
  - Le kick seul perd 12,24 dB au téléphone (de 6,25 à 14,66) et 14,83 sur le pire cas. Le morceau entier perd 3,92 et 4,58.
  - Kick contre morceau : −1,39 / −9,83 / −11,62 LU.
  - Bas-médium au téléphone : −2,88 dB, la bande la plus forte en médiane. La basse seule perd 9,68 dB au téléphone.
  - Table des 10 plus faibles : 10 premiers au téléphone, dans l'ordre, chaque valeur juste.
  - Les 9 styles qui tiennent au-dessus de 3 dB, avec leurs valeurs ; la riddim est la meilleure (6,58).
  - 19 styles dans la plage du genre ; 6 cibles estimées dépassées de moins de 1 LU.
  - Perte en mono : dubstep 1,47, neurofunk 1,30, grime 1,20 et gabber 1,09.
  - Phonk −0,8, dubstep 0,3, trance 0,5, techno 0,5, drum and bass 0,6 / −0,4, house 3,6.
  - Crête −1,20 à −1,16 dBTP ; M2 à M4 conformes au banc.

## Constats du premier passage

| N° | Constat | État |
|---|---|---|
| G1 | Écoute en direct non passée | **corrigé** : 8 styles en direct, 0 alerte ; décision écrite ; le rapport dit que les 29 autres ne sont jugés que sur l'export |
| M1 | Décision du kick fausse, ancien `kick_lift` encore là | **corrigé** |
| M2 | « Dubstep le plus doux face à son genre » | **corrigé** (phrase retirée) |
| M3 | « Acidcore le meilleur », liste « qui tiennent » incomplète, cause non mesurée | **corrigé** pour les deux premiers points ; la cause reste (« Leur kick est plus saturé, ou son attaque est plus claire »), voir N2 |
| M4 | « Image stéréo saine » malgré 4 styles au-dessus de 1 LU | **corrigé** (nouveau M5 du rapport) |
| M5 | Afro house présentée comme mesurée | **corrigé** dans `tableau_son6.py` ; le compte « 22 » n'a pas suivi, voir N1 |
| M6 | Téléphones plus doux que les sources ; banc et rapport mélangés | **corrigé** (300 et 500 Hz ; M2 et M4 disent « banc à 300 Hz ») |
| mineur | « 25 styles » sans « au téléphone » | **corrigé** |
| mineur | « 1 à 2,5 LU » alors que la drum and bass est à 0,7 | **corrigé** (« 0,2 à 2,4 ») |
| mineur | Table des pires sans ordre | **corrigé** (les 10 plus faibles au téléphone) |
| mineur | Liste des leads sombres : phonk à −14,8 dedans, riddim à −51,1 absente ; « phonk, lead très sombre » | non corrigé, voir N3 |
| mineur | Pente « norme des musiques actuelles », « filtrés donc vieux » | non corrigé, voir N3 |
| mineur | Le plafond du master touche 8 styles, pas 3 | non corrigé, voir N3 |
| mineur | Lisibilité orale des tableaux | inchangé, acceptable |

## Nouveaux constats

### BLOQUANT

Aucun.

### GRAVE

Aucun.

### MOYEN

Aucun.

### MINEUR

- **N1. Compte des cibles estimées.** L'afro house est désormais une estimation : `par-style.tsv` donne 14 cibles mesurées, donc 23 estimées. Le rapport (« Ce qui n'a pas pu être fait ici ») et `docs/decisions/tour-5-ecoute.md` (ligne « Cibles par genre ») disent encore 22. À corriger : 22 → 23.
- **N2. Une cause présentée comme établie.** « Leur kick est plus saturé, ou son attaque est plus claire » (G1, « Ceux qui tiennent ») : aucune mesure ne le montre. À écrire : « peut-être un kick plus saturé ou une attaque plus claire, non mesuré ».
- **N3. Les trois mineurs restants du premier passage**, inchangés dans `rapport-son.md` :
  - Leads sombres : retirer la phonk (−14,8) de la liste, ajouter la riddim (−51,1), et dire seulement « lead sombre » dans la fiche phonk.
  - Pente : écrire « repère ancien (corpus 1950-2010), indicatif ».
  - Plafond du master : la perte est de 3,0 LU sur 8 styles, pas seulement sur les 3 cités.
- **N4. Résumé, ligne 1.** « Sur les 37 morceaux … ni à l'export ni en écoute en direct » : l'écoute en direct ne porte que sur 8 styles, et sur leurs 150 premières secondes. À écrire : « … à l'export (37), ni en écoute en direct (8 styles) ».
- **N5. Résumé, ligne 3.** « De 0,2 à 2,4 LU » ne vaut que pour les cibles mesurées : les cibles estimées vont jusqu'à 5,2 LU (riddim). Ajouter « face aux cibles mesurées ».
- **N6. Phrase confuse.** « 8 styles, ceux à écouter plus phonk, house, riddim et acidcore » : la phonk fait déjà partie des 5 à écouter. Écrire : « les 5 à écouter, plus house, riddim et acidcore ».

Verdict : 0 bloquant, 0 grave
