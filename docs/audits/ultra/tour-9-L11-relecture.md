# Tour 9, lot L11 : relecture neuve
Relu : 68e818aa

Diff lu : base de l'intégration..68e818aa (AppRoot.kt, LiveDock.kt, LiveScreen.kt, Help.kt, tuto.json des 14 langues, tests T9F131, T9L11*, T9D17, CutLocalNoteScroll). Thread audio et sauvegarde non touchés.

rouge constaté : b028a173 : rejoué sur 05e7ab07 : `T9L11SwitchStaysWholeTest` est rouge (« the switch cut or gone after the parts moved by 300 px », l'interrupteur Ligne du temps / Parties n'est plus en entier en haut) ; vert sur 68e818aa.

## Constats
1. **moyen** : Live sur téléphone à 130 % : « Cases » passe à la ligne dans la barre du bas et la grille ne montre plus que les rangées A et B au lieu de A à C ; rien n'est coupé et la grille défile, c'est le prix de la place gagnée par « Lancer à ».
2. **mineur** : D17, le bouton de la carte d'erreur du kit garde le libellé du kit (pas « Réessayer » mot pour mot) ; il relance bien le chargement. L'état « kit en échec » n'est pas gardé à la rotation (la carte revient au prochain essai).
3. **vérifié, pas un défaut** : F131, le nouveau Retour qui déplie le tuto passe après ceux du Live et de la Ligne du temps. Sonde jouée sur 68e818aa (tuto replié, onglet Live, Retour) : le Live se ferme bien au premier Retour, le tuto reste replié. Ce test de sonde n'est pas commité.

## Relancé sur 68e818aa
T9F131BackUnfoldsTutoTest 1/1, T9L11LiveBarWholeTest 4/4, T9L11KitErrorSaidTest 2/2, T9L11SwitchStaysWholeTest 1/1, T9D17KitDisappearsTest 2/2, CutLocalNoteScrollTest 1/1 (les « drive » hérités sont sautés, normal) : tout vert. Les 14 tuto.json sont du JSON valide.

## Captures regardées en entier (avant / après)
Live arabe 360 dp (rien de coupé), Live 130 % (« Lancer à » entier, voir constat 1), Structure Parties à 130 % et en paysage tablette (interrupteur en haut, entier), feuille Tempo à 130 % (identique, rien de coupé). Le libellé « es : » coupé du panneau d'effets existait déjà avant le lot.

Verdict : 0 bloquant, 0 grave
