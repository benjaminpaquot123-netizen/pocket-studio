# Tour 5 — lot « navigation » (chantier 4) : rapport

Source : `docs/audits/AUDIT-PRISE-EN-MAIN.md` (G6, G13 selon la réponse 2B), `PLAN-ACTION-AUDITS.md` étape 4.

## 1. Les deux ▶ nommés (2B) — fait
- Haut : « ▶ cette partie » (`togglePart`) boucle la partie ouverte ; pendant le morceau, la partie prend le relais ; en boucle, ■.
- Pouce : « ▶ le morceau » (`toggleWholeSong`) joue le morceau depuis le départ affiché ; reprend la main sur une partie en boucle ; ■.
- Sans morceau, les deux jouent la partie. TalkBack : « Jouer cette partie » / « Jouer le morceau », « Arrêter » sur celui qui joue.
- Sans barre du pouce (couché, tablette couchée, clavier sur écran court) : « ▶ le morceau » rejoint la barre du haut
  (8 dp d'écart), sur la ligne du nom si la rangée est trop étroite.
- Mot mesuré (10,5 sp demi-gras, 56 dp au plus, plus petit seulement quand la police du téléphone est énorme), bouton
  non rogné (les coins coupaient « ette parti ») ; à 130 % sur 411 dp le nom du projet garde sa place.
- Barre du pouce : ▶ et tempo mesurés d'abord, les autres boutons se partagent le reste ; barre de 48 dp.
- 14 langues, mots du Lexique (Partie / Morceau), zone `lectures`.

## 2. Barre du pouce à 130 % (G6) — vérifiée : incomplète
- Cause et correction de 821bdab1 justes (`thumbBarFits` lisait la hauteur à la taille du texte ; 480 dp maintenant).
- Mais la suite complète montre deux reculs dus à la barre désormais présente (rejoués avec l'ancien ▶ de 48 dp : rouges aussi) :
  - `TabletUprightTest#theSecondRowOfStepsShowsAt130` : Tab A11 debout 130 %, Sons › Rythme, 2e rangée de cases
    43 dp sous le pli (pads déjà au minimum de 48 dp sur la vitre).
  - `TutoBigFontTest#theNextStepsLineComesIntoSightWithItsCells` : 360x780, police 200 %, la ligne du tuto passe sous « Partie : A ».
- Pistes : cases de 37 dp (48 sur la vitre) sur tablette debout (−26 dp) + ligne du Guide en pastille de 32 dp (−16), comme
  le propose l'audit ; pour le tuto, laisser la tête (ligne du tuto) gagner sur la fin des cases. Non faits dans ce lot.

## Tests
- `TwoPlaysNamedTest` (+ GermanSmallPhone 360 dp 130 % DE, Tablet A11, TabletLying) : nommés en Simple et Pro, chacun joue ce qu'il dit, mots entiers.
- Ajustés (« le morceau » est au pouce) : GestureBudget, KeyActionsGesture, PlayThenTabs, GuideJourney, HelpLabels, TopBar,
  bancs BenchPlayThenTabs, BenchTour1, HumanRemixJourney, HumanLucasBugs, HumanTester ; androidTest Journey et Endurance.

## Restes
- Le message de « Mode retours » (4 s, en bas, 72 dp) recouvre « ▶ le morceau » : à remonter au-dessus de la barre du pouce.
- La phrase « En boucle (⟳ Boucler). ▶ joue cette partie seule. » cite une pastille absente du Mode simple.
- Bancs encore sur `top_play` au sens « jouer » (BenchTour4, Tour5Bloquants, BenchPlaceAndPadLive, BenchRecette) : verts, à relire.
