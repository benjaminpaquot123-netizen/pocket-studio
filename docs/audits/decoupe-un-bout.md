# Audit : « Découper la chanson », mode « Un bout » (2026-11-04)

Relu à froid (captures 411 dp à 100 % et 130 %, paysage 891 × 411 dp, code `remix/Cut*.kt`, `SongCuts.kt`), en débutant puis en pro. Après : `app/screenshots/cut_bout*.png`.

| point | gravité | avant | après | test/commit |
|---|---|---|---|---|
| Téléphone en paysage | Grave | la courbe tombe à 0 dp, Début/Fin écrasés | courbe à gauche, boutons et envoi à droite | `cut_bout_landscape` |
| Pas de règle mesures/temps | Moyen | l'aimant colle à des temps invisibles | règle sur la courbe : numéros de mesure, un trait par temps, fine ligne par mesure | captures |
| Boutons « Début » / « Fin » | Moyen | icônes « aller au début / à la fin » | « Début ici » / « Fin ici », icône = la barre et sa poignée | CutSpanJourney |
| Début posé après Fin (ou l'inverse) | Moyen | le bout s'écrase à 50 ms, invisible | le bout entier part à la ligne orange, sa longueur gardée | CutSpanRules, CutSpanJourney |
| Deux ▶ | Moyen | ▶ du haut (piste jusqu'au bout de la chanson) et ▶ « 0:04.7 », sans un mot | un seul ▶ (« Écouter depuis la ligne orange », posée sur Début à l'ouverture) ; la carte dit « 🎧 Écouter le bout » | CutSpanJourney |
| Poignées | Moyen | pilules 16 × 44 dp dans le bout, sur l'attaque | 20 × 56 dp, dehors, sous la règle ; zone de prise 48 dp toute hauteur | captures |
| Rien à entendre en lâchant une barre | Moyen | il faut toucher ▶ après chaque réglage | Début lâché : le bout joue ; Fin lâchée : ses 2 dernières secondes | CutSpanJourney |
| Barre tirée vers le bord | Moyen | la vue ne suit pas | à 32 dp du bord, la chanson défile sous la barre | CutSpanJourney |
| Longueur avec l'aimant | Moyen | bout posé hors des temps : « 0:04.7 » | posé sur les temps : « 9 temps · 0:04.8 », « 2 mesures · … » | CutSpanRules |
| Cadre blanc | Mineur | traits de 3 dp sur les quatre côtés | barres 2,5 dp, haut et bas en filet pâle | captures |
| « Sur les pads » rempli d'orange | Mineur | même allure que la puce « Voix » choisie | quatre boutons pareils, icône orange pour le premier | captures |
| « MES. 3 À 8 » et « Pince pour zoomer » en orange | Mineur | deux orange de plus | la règle remplace « Mes. » en « Un bout » ; l'aide en gris partout | captures |
| 130 % : menu « Au temps » | Mineur | coupé sur deux lignes | le « ? » de la rangée part (celui du titre explique) : une ligne | `cut_bout_130` |
| Aide « ? » du titre | Mineur | parle de Couper ici, Enlever, Boucle | aide « Un bout » : barres, les deux écoutes, aimant, envoi | HelpCoverage |
| Ligne verte de ce qui joue | Mineur | vert sur le vert de la voix | bord sombre autour de la ligne | captures |
| « Garder » | Mineur | étoile = favori | signet de « Garder dans ma bibliothèque » | captures |
| Clics aux coupes | vérifié, rien | fondus en cosinus 4 ms / 12 ms à l'envoi et à l'écoute (au moins 1 ms) : pas de clic, pas besoin de passage par zéro | — | StemEditor |
| Retour après « Sur les pads » | vérifié, rien | « « Voix 1 » est sur le pad 3. » + OK dans la carte d'envoi | — | — |
| Annuler, TalkBack | vérifié, rien | flèche ↶ ; « Début/Fin du bout » avancent ou reculent d'un temps (nœuds posés sur les poignées) | — | — |

## Reste
- Écouter le bout en boucle (régler une boucle à l'oreille) : pas ajouté, pour garder « Un bout » simple ; le moteur sait déjà le faire.
- Pas d'écoute pendant le glissé, seulement au lâcher.
- Mode « Phrases » à côté de « Le morceau » (tablette, 640 dp de haut) : la rangée d'outils reste écrasée, déjà avant cet audit.
- Sans tempo connu, pas de règle (ni secondes ni repères).
