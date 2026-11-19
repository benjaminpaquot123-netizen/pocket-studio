# Tour 9, lot L15 : décisions

- Puce « Kit » pâlie vue par L4 à 200 % : non reproduite (la puce est active, « Kit : Classique » tient sur une ligne sans « … ») ; c'était une capture sans fond. Rien changé, garde : Tour9L15GuardTest#kitChipWholeAt200. Retour arrière : supprimer le test.
- En-tête « PAS : KICK » écrasé : le nom du pad reste sur la ligne du bas de l'en-tête ; quand la place manque, la puce « Tous les pads » passe sous l'en-tête au lieu de le serrer en colonne. Retour arrière : revenir à la rangée unique (36c00640).
- F049 : plus reproduit après 36c00640 à 130 et 200 % ; gardes Tour9L15GuardTest#f049HeadClearsTheGridAt130 et #f049HeadClearsTheGridAt200.
- Test de débordement en largeur (didOverflowWidth) abandonné : faux positif sous Robolectric ; on mesure les bornes à l'écran à la place.
- F052 et F054 : aucune clé de texte neuve ; F052 réutilise « barré : à enlever », F054 réutilise le nom du pad sur le bouton de la ligne d'aide.
- F057 : le Swing reçoit « − » et « + » à 1 % près, sur le modèle de PadLoopRow (même boutons texte, valeur en MonoStyle cyan). La moitié « montrer la valeur visée » demande que le tuto passe sa cible (TutoHints, posé dans AppRoot.kt, hors de ma liste) : demandée au chef, non faite.
- F132 : la cause est dans DefaultNames.shownSample (hors de ma liste) ; non corrigé, demandé au chef.
- D05 (« Ik doe het », t9-13) : relève de L5, pas touché.
- corrections.tsv : aucune ligne écrite ; le registre n'a pas encore de ligne du trieur pour le tour 9 et un lot n'en ajoute pas (reparations.py). États et preuves dans le rapport.
- Captures de référence réenregistrées : seulement les 3 de l'écran Rythme que le lot change (nav/simple_rhythm, rythme_empty_project, rhythm_scrolled_under_part). sons_synth_take et pad_sound_name_whole diffèrent aussi mais pas à cause de ce lot (texte du Guide, nuances de pixels) : non touchées.
