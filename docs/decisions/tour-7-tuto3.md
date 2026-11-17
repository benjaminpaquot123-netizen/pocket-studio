# Décisions du lot tuto3 (tour 7)

- Portée du test « chaque chapitre depuis un projet vide » : seulement les étapes sur les parties et transitions (créer, nommer, ranger, remplir, crash, variation). Le mix et le son demandent les instruments des chapitres d'avant : hors règle. Retour arrière : élargir `projectChecked`.
- Un bouton « Créer la partie » ou « Nommer » sur un projet vide construit d'abord le Groove du cours (sinon la partie serait vide et « Bravo » mentirait). Retour arrière : `TutoChecks.makePart` sans `courseGroove`.
- Guillemets doublés (ko et 8 autres langues) : on ne change que les guillemets extérieurs ; le nom du bouton reste mot pour mot (règle des boutons cités). Retour arrière : revert des 9 `tuto_techno.json`.
- Message « partie créée » : la copie est nommée d'après la partie d'où elle vient (« A »), jamais « Groove » quand le Groove n'existe pas sous ce nom. Retour arrière : `TutoController.partMade`.
- Étape « changed:drums » : la référence est reprise une fois la partie créée pendant l'étape (Drop 2, les 4 maracas). Retour arrière : `TutoController.base`.
- Allemand « Déjà fait » : « Schon da! » + « tu n'as rien à changer », plus de « erledigt / nichts zu tun » contradictoires.
- Laissés au lot tuto4 : reste de D4 (séparer « Créer la piste Basse » et « poser les notes » dans ~37 chapitres × 14 langues ; étape 3/3 copie puis mesure 4) et ko « 킥 » / « 큰북 » (vocabulaire). Trop large pour ce lot.
