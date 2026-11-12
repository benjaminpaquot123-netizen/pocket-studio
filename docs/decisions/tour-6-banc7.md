# Décisions du lot « banc7 » (tour 6) : les défauts du banc

- 2026-11-12 · « Rien ne change » tient compte du morceau : un geste qui change le projet ouvert (empreinte de son contenu) remet le compte à 1 ; l'empreinte de l'écran (rejeux) reste la même · raison : D b clos pendant qu'il posait des notes · retour arrière : retirer `songDigest()` du compte.
- 2026-11-12 · E4 (les notes dessinées absentes du texte de l'écran) n'est pas traité : le texte de l'écran reste sans mots de TalkBack (choix « réalisme » R1) · retour arrière : sans objet.
- 2026-11-12 · E12 n'est pas un défaut de l'appli ni du « tape » : la ligne du son compte maintenant les coups par pad · retour arrière : remettre `distinct()`.
- 2026-11-12 · `seat-metrics.py` sans `etat/` garde le verdict commité du siège, ou dit `inconnu`, jamais un « non » qui aurait l'air mesuré · retour arrière : retirer `kept_verdict`.
- 2026-11-12 · `tableau-de-bord.py` compte toutes les lignes `promesse:` (oui / non / pas vue) ; la règle D (vues par 2 sièges) est inchangée · retour arrière : retirer la colonne.
- 2026-11-12 · Un tuto entier demande 250 gestes au moins (`fabrique-sieges.py` refuse moins, en nommant le siège) ; le délai du lanceur suit le plus long `timeout.minutes` du lot + 10 min · raison : 120 gestes finissaient au chapitre 7/15 (D a, H a) · retour arrière : retirer `MIN_GESTES` et l'`export PS_STALL_MIN`.
- 2026-11-12 · `tampon.sh verifie` dit la raison de chaque refus, `human-act.py` dit de lancer `scripts/human-tester.sh <lot>` quand aucun banc ne tourne (marqueur `<lot>/.banc-en-cours`, retiré à la fin) · retour arrière : retirer le marqueur et les messages.
- 2026-11-12 · `verifie-chantier.py` n'est pas changé : 20 des 28 écarts venaient d'un résumé d'un autre code (refus voulu, règle P3) ; les 8 restants sont des commits historiques à apparier · retour arrière : sans objet.
