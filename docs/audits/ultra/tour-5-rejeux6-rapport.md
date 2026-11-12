# Tour 5, lot « rejeux6 » : les 8 rejeux en attente passés des pixels aux étiquettes

Branche : `eq/tour-5-rejeux6`. Décisions : `docs/decisions/tour-5-rejeux6.md`. Journaux de chaque rejeu : `docs/audits/ultra/rejeux-attente/_journaux/<id>/`.

## Ce que fait le lot
- t5-01 à t5-08 visent les boutons par leur étiquette de test (`touche <étiquette>`), plus par leurs pixels ; chaque oracle « absent » a sa preuve positive (`rejeu.attendu`).
- `rejeu-defauts.sh` garde le journal de chaque rejeu ; un « tenu » exige que l'étiquette voulue soit touchée dans ce journal.
- `reachableNear` : un toucher par étiquette tombe dans la portée du pouce quand le milieu du bouton en est hors.

## Résultats des rejeux (rejeu-defauts.sh)
- t5-01 tenu.
- Défauts réels confirmés (restent dans `rejeux-attente/`, non corrigés) : t5-04 (carte « Simple » hors de portée du pouce gauche), t5-05 (après « Dans un nouveau projet » l'appli retombe sur « Simple ou Pro ? »), t5-06 (poignée « Fin du bout » hors de portée du pouce gauche : glissement refusé).
- Non tenu, cause à instruire (défaut de l'appli non établi) : t5-03 (curseur « Swing » absent alors que le tuto est à l'étape swing).
- Divergés (banc, pas de verdict d'oracle) : t5-02 au pas 122, t5-07 au pas 18 (fenêtre de dialogue du pliable : le doigt tombe sur une autre ligne), t5-08 au pas 9 (export lent du banc).

## Chemins (endroits qui lisent ou montrent les mêmes données)
- chemin: Banc › toucher par étiquette dans la portée du pouce | test: BenchSeatOptionsTest#aTagOutOfTheThumbsReachIsTouchedInsideTheReach
- chemin: Banc › verdict d'un rejeu (bouton déplacé) | test: BenchReplayReadableTest#aMovedButtonIsSaidInTheVerdict

tests: *BenchSeatOptionsTest* *BenchReplayReadableTest*

t5-01-partie-simple : oracle tenu
t5-02-prise-apres-padlive : oracle non tenu (divergé au pas 122, étiquette « track_2 » introuvable)
t5-03-swing-tuto-pro : oracle non tenu (curseur « Swing » absent)
t5-04-gaucher-mode : oracle non tenu (carte « Simple » hors de portée du pouce gauche : défaut confirmé)
t5-05-gaucher-remix-projet : oracle non tenu (retour sur « Simple ou Pro ? » après « Dans un nouveau projet » : défaut confirmé)
t5-06-gaucher-fin-du-bout : oracle non tenu (poignée « Fin du bout » hors de portée du pouce gauche : défaut confirmé)
t5-07-etoile-sons : oracle non tenu (divergé au pas 18, étiquette « name_clear » introuvable)
t5-08-mes-exports-simple : oracle non tenu (divergé au pas 9, étiquette « vue_Drive » introuvable)
