# Tour 8, lot mixer8 : décisions

- 2026-11-18 D7/U27 : le Mixer suit la règle de la page Sons (piste 1 vide cachée quand elle a été supprimée, ou quand le tuto la cache) dans `mixerStrips` ; aucun nouvel ordre de pistes. Retour arrière : enlever le filtre de `mixerStrips`.
- 2026-11-18 Rotation : les deux volets du Mixer passent par `movableContentWithReceiverOf`, l'état (tiroir de tranche, défilement, Effets du master) suit le passage une colonne ↔ deux colonnes. Retour arrière : revenir aux lambdas directes dans `AdaptivePanes`.
- 2026-11-18 Coût : la sonde de `FitFaders` lit les canaux au repos (volume 0), sans vu-mètres ni actions, donc un fader déplacé ne la recompose plus. Retour arrière : repasser `channels`, `meters`, `actions` à la sonde.
