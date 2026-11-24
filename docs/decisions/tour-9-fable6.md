# Décisions du lot « fable6 » (tour 9)

- 2026-11-24 : a-V1 (tuto, note demandée hors de vue après un toucher sur la ligne) : les rangées sont jugées sur la vue une fois amenée à la mesure (`TutoRollHint.atBar`) et sur toutes les notes en pointillé de cette mesure, pas sur la seule note annoncée ni sur le bas des accords. Raison : c'est la cause commune des retours des tours 7 et 8, les réparations d'avant ne traitaient que la première mesure. Retour arrière : annuler a8e520d95.
- 2026-11-24 : montee-vide et cible-autre-partie : pas de changement de code (déjà réglés par 494a125e6, 584c3df8 et a3aafd9fc) ; deux balayages tous styles ajoutés dans project-model pour qu'ils ne reviennent pas sans bruit.
- 2026-11-24 : le test du toucher joue 4 styles par défaut (house, techno, pop, trap ; 35 s) et tous avec `PS_FABLE6_STYLES=all` (plus de 20 minutes). Raison : quota et durée de la CI. Retour arrière : changer `SPREAD` dans `Tour9Fable6TouchHereEveryBarTest`.
- 2026-11-24 : l'alerte du registre n'est pas éteinte à la main : la ligne `ok` et la ligne « constaté » sont proposées au chef dans le rapport, après un testeur neuf.
