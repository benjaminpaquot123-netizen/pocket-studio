# Tour 9, lot gr-l6 (flûte trap) : décisions

- La flûte reste dans l'instrument « Bois » (instrument_bois) au lieu d'un instrument neuf : un instrument neuf demandait de toucher MultiInstruments.kt (hors liste du lot). Retour arrière : 4452f0b4^ (e4af8358).
- La flûte joue dès le do du milieu (60) au lieu du sol (67), la clarinette s'arrête au si (59) : la phrase trap (fa 4 = 65) tombait en partie sur la clarinette. Retour arrière : remettre les touches [52, 66] et [67, 100].
- Notes tenues jusqu'à 6 s dans le grave, 4,5 s dans l'aigu (au lieu de 3 s) : la boucle du vibrato ne s'entend plus sur une note longue ; budget mémoire 48 Mo (au lieu de 32) pour que la réduction automatique ne raccourcisse pas ces durées. Version 2 : le téléphone propose la mise à jour.
- Attaque 40 ms (au lieu de 20) : le souffle d'entrée est adouci, comme le conseille la recherche (30 à 50 ms).
- Phrase trap neuve et originale : une ligne liée qui descend de l'octave à la tonique sur deux mesures, avec deux silences. Pas de copie d'une mélodie connue (« façon Mask Off » = l'ambiance, pas les notes).
- Drill et phonk : vérifiés, leurs leads (Piano FM, cloche 808) gardent leur identité ; la flûte n'y est pas mise.
- Pas fait : la variante sifflée (synthèse, à faire dans un lot moteur) et la couche « sans vibrato » (FluteSusNV) en vélocité douce : le constructeur de packs ne sait pas répartir une partie par vélocité.
- Aucun son neuf : VSCO-2 CE (CC0) est déjà crédité ; aucun Freesound dans ce lot.
