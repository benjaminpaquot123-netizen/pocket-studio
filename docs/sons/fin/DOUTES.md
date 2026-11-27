# Doutes : une ligne chacun (2026-11-27)

Sources : `docs/sons/passe2/verif/DESACCORDS-A.md`, `-B.md`, `-C.md` et `docs/sons/passe2/CORRECTIONS-CRITIQUE.md`.
« levé » = preuve donnée ; « reste » = on ne sait pas, mais aucune cible n'en dépend (dit pourquoi).
Un seul doute changeait des cibles : B1 (voix, boucles mélodiques, coups « - ») ; il est levé par la preuve ci-dessous.
Zéro doute sans réponse.
Seconde critique (v3) : la 5e condition « la cible ne dépend pas d'un seul fabricant » retire 21 lignes (99 → 78) ;
elle ne lève ni n'ouvre aucun doute (détail : `REPONSE-CRITIQUE2-AUTRES.md`).

## DESACCORDS-A (RAR 1, 2, 3, 4, 12)

| doute | état | preuve ou raison |
|---|---|---|
| A1 hauteur d'attaque des kicks et toms | levé | l'outil du vérificateur prend la fenêtre du pitch stable ; avec la fenêtre de `tout.py`, il retombe sur la table |
| A2 hauteur d'attaque snare, clap, perc, cymbales | reste | sons trop peu tonaux sur 2 à 12 ms ; aucune cible sur la hauteur des coups bruités (batterie, hors lot) |
| A3 hauteur stable des kicks | reste | la hauteur glisse dans la queue ; la cible kick est en batterie, hors lot ; aucune cible ici |
| A4 hauteur stable snare, clap | reste | hauteur non définie ; jamais de cible |
| A5 hauteur stable perc, cowbell, hats, ride | reste | partiels non harmoniques ; jamais de cible |
| A6 kicks courts « vides d'un côté » | levé | l'outil du vérificateur abandonne sur queue courte ; la table a une valeur cohérente |
| A7 part tonale des kicks | levé | la bande du vérificateur garde l'énergie sous 20 Hz (valeurs > 0 dB impossibles) |
| A8 part tonale shakers, hats | levé | convention mono différente, aucun n'a tort ; pas de cible |
| A9 note des one-shots mélodiques | levé | la note lue dans le nom confirme la table 39 fois sur 41 ; de toute façon jamais de cible sur la note |

## DESACCORDS-B (RAR 5, 6, 7, 8)

| doute | état | preuve ou raison |
|---|---|---|
| B1 type des voix d'ornement de r7p2 | levé (change des cibles) | 622 sons nommés comme des voix ; durée et crête identiques à la remesure (0 écart) ; déplacés vers « voix » (`passe2/fin/reetiquetage/`) : 605 venaient des boucles mélodiques, 17 des coups mélodiques « - ». Preuve pour les 17 (table d'avant, commit eefa5745d, contre `n7_vocal.csv`) : tous sans sous-type, part chantée médiane 49 % (55 % pour les 605 autres voix de r7p2), étendue 2,6 demi-tons (5,2), hauteur 392 Hz (295), crête −3,01 dB exactement comme les 605, durée 1,9 à 7,5 s (médiane 5,6 s, bien plus longue que les coups « - » restants : 1 s) : ce sont bien des voix. Effet chiffré : `CONCLUSION-AUTRES.md`, « Le déplacement des 622 voix » |
| B2 8 lignes MIDI sans fichier, r7p2 | levé | ce sont les 8 MIDI d'un kit ; l'extraction coupe leurs noms, donc l'id change ; relus par `tout.py`, chacun retrouve une seule ligne (notes, note basse, note haute) ; rien à corriger |
| B3 9 hats ouverts très courts, rar8 | reste | batterie, hors lot ; les hats sont rangés par mesure, pas par nom : la cible ne bouge pas |
| B4 type lu par le chemin à 31,8 % sur rar7 | levé | outil du vérificateur ; la table a raison (B1 réglé, le reste est un nom de dossier lu de travers) |
| B5 styles lus par le chemin (techno/drift-phonk, dubstep/funk) | levé | règle grossière du vérificateur, aucune preuve contre la table |
| B6 hauteurs et part tonale des coups bruités | reste | pas de cible sur ces hauteurs (batterie, hors lot) |
| B7 note des sons mélodiques à 66 % | reste | jamais de cible sur la note (81 % d'accord sur tous les RAR) |

## DESACCORDS-C (RAR 9, 10, 11)

| doute | état | preuve ou raison |
|---|---|---|
| C1 part tonale des coups, 22 sur 631 | levé | convention mono, aucun n'a tort |
| C2 hauteur d'attaque, 174 sur 309 | levé | erreur du vérificateur, prouvée par un son synthétique |
| C3 hauteur stable des kicks courts | levé | outil du vérificateur ; exceptions ci-dessous |
| C4 kick r10-p1-582ac7a8de et kick qui glisse r9-p3-d439f2d798 | reste | la hauteur dépend de l'endroit mesuré ; 2 sons sur des centaines, la médiane kick (batterie, hors lot) ne bouge pas |
| C5 hauteur des snares, claps, percs | reste | pas de vraie note ; jamais de cible |
| C6 note sous 45 Hz des accords (au moins r11-p1-2180e4d09e) | reste | table fausse sur ces sons, mais jamais de cible sur la note |
| C7 42 one-shots sans note | reste | accords ou bruit ; jamais de cible sur la note |
| C8 erreurs d'octave | reste | idem, jamais de cible sur la note |
| C9 étiquettes r10-p1-5dda95bbf5 (sonne kick) et r10-p1-083b725735 (clap ou snare) | reste | batterie, hors lot ; 2 sons, aucune médiane ne bouge |
| C10 candidats 808 r11-p3-15c1a62557, r11-p3-91badefa10 | reste | batterie, hors lot ; le 808 n'a déjà pas de cible (un seul pack) |
| C11 hat r9-p3-3d8b0b8352 (ouvert, queue 145 ms) | reste | rangé par mesure, pas par nom ; la cible ne bouge pas |
| C12 self-test de `mesure.py` sur l'attaque | reste | outil du vérificateur, pas des tables ; ne sert de juge pour aucune cible de ce lot |
| C13 2 lignes sans fichier de r11p3 | levé | archive cachée, mesurées depuis, d'accord |

## CORRECTIONS-CRITIQUE (13 fautes)

| faute | état | preuve ou raison |
|---|---|---|
| 1 tonalité | levé | recalculée sur 826 boucles mélodiques (les voix de r7p2 retirées) : 21 % exacte ; jamais de cible |
| 2 nombre de sons de la hauteur | levé | n = lignes gardées ; hauteur retirée pour les coups bruités |
| 3 grosse caisse par style | levé | une ligne par style (batterie) |
| 4 kick 808 d'un seul pack | levé | marqué « un seul pack », pas de cible |
| 5 sons aberrants | levé | règle écrite dans `tables.py`, médianes refaites |
| 6 charlestons | levé | rangés par mesure |
| 7 RAR 12 | levé | archive imbriquée lue ; contre-mesure de cette archive faite (commit 2cb21bb7) |
| 8 tempo | levé | tempo replié ; boucles mélodiques : 88 % depuis le retrait des voix de r7p2 (c'était des voix, pas des boucles) ; pas de cible |
| 9 sans style, 33,8 % retiré | levé | fait ; la note n'est jamais une cible |
| 10 packs sans étiquette | levé | aucun style inventé |
| 11 archives imbriquées | levé | mesurées, lignes dans `passe2/fin/imbriquees/` |
| 12 1re contre 2e passe | levé | comptes par type dans `INVENTAIRE.md` |
| 13 remesure RAR 12 | levé | 40 sons, durée, crête et RMS 40 sur 40 |
