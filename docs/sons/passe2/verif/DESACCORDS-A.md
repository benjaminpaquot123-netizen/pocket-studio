# Désaccords entre les tables et la contre-mesure (RAR 1, 2, 3, 4, 12)

Accord global par colonne, les cinq archives réunies (son comparé quand les deux côtés ont une valeur ;
hauteur comparée seulement si le son est assez tonal) :
durée, crête, RMS, t20, t60, centre spectral, aigus, attaque/corps : 100 % ;
tonalité 97,3 % (1348) ; hauteur stable 55,3 % (825) ; hauteur d'attaque 56,6 % (824) ; f0 des one-shots mélodiques 72,7 % (183).

Méthode : pour chaque paire type × colonne sous 98 %, 10 sons en désaccord repassés par un troisième chemin
(passages par zéro montants, pic FFT fin interpolé sur la queue, ma mesure avec la fenêtre de `tout.py`,
étiquette de note lue dans le nom du fichier par la table, indépendante de la mesure). « Médiane bouge » =
la médiane par style × famille (au moins 5 sons) se déplace-t-elle plus que son étendue interquartile
si l'on remplaçait la table par ma mesure.

Une ligne par cause :

- **Hauteur d'attaque, kick / kick_808 / tom (≈ 200 sons, RAR 1 à 12)** : c'est mon outil qui se trompe. Il prend une période du pitch *stable* comme fenêtre ; `tout.py` prend une période du pitch *d'attaque mesuré*, comme sa doc l'annonce. Avec la fenêtre de `tout.py`, ma mesure retombe dans les 200 cents (kick 7/10, 808 presque tous à ±130 cents). Le « raté » de `tout.py` au self-test (99,6 contre 88,0 Hz) vient de ma convention de vérité : sur 2–12 ms la vérité vaut 101,8 Hz. Médiane bouge : oui avec mon outil (multi/808, drift-phonk, techno, lofi, house, rave), mais seulement à cause de cette erreur de fenêtre ; la table reste juste.
- **Hauteur d'attaque, snare / clap / perc / cowbell / cymbales (≈ 40 sons)** : aucun des deux n'est fiable. Ces sons sont trop peu tonaux sur 2–12 ms, le résultat dépend du filtre de bande ; le troisième chemin est trop bruité pour trancher. Médiane bouge : non.
- **Hauteur stable, kick (≈ 125 sons)** : aucun des deux n'est fiable. Le pitch continue de glisser dans la queue, la valeur dépend de la fenêtre (de −10 dB à −30/−40 dB chez `tout.py`, médiane des périodes chez moi). Pic FFT fin de la queue : à ±100 cents, moi 36/93 et `tout.py` 22/93 sur RAR 1, 8/16 contre 1/16 sur RAR 2, mais `tout.py` plus proche sur RAR 4 (6/7). Médiane bouge : non (aucun groupe).
- **Hauteur stable, snare / clap (≈ 85 sons)** : aucun des deux n'est fiable, avec un léger avantage à mon outil sur les snares (plus proche 38/56 sur RAR 1, 8/9 sur RAR 3) et à `tout.py` sur les claps (7/8 sur RAR 12, 2/2 sur RAR 3). Médiane bouge : non.
- **Hauteur stable, perc / cowbell / hats / ride (≈ 80 sons)** : aucun des deux n'est fiable. Cowbell : deux partiels non harmoniques (rapport ≈ 1,48) dans la même bande, chaque méthode en choisit un ; perc : partage à peu près égal (RAR 1 `tout.py` plus proche 13/22, RAR 4 7/12, RAR 12 4/7). Médiane bouge : non.
- **Hauteur stable et d'attaque « vide d'un côté », kick très tonal court (≈ 30 sons, RAR 2 surtout)** : c'est mon outil qui abandonne. Kick quasi sinusoïdal (tonalité ≈ 0 dB) à 40–53 Hz avec une queue trop courte : moins de 4 passages par zéro entre −10 et −60 dB, alors que `tout.py` allonge sa fenêtre jusqu'à 6 périodes. Les valeurs de la table (41–53 Hz) sont cohérentes avec un sinus pur. Médiane bouge : non.
- **Tonalité, kicks (≈ 20 sons)** : c'est mon outil qui se trompe. Ma fenêtre « près du fondamental » garde l'énergie sous 20 Hz, d'où des valeurs au-dessus de 0 dB, impossibles par définition ; `tout.py` limite à 20 Hz–20 kHz. Médiane bouge : non (les « 0 contre 0 » de mon calcul sont des arrondis).
- **Tonalité, shakers / perc / hats / crash (≈ 12 sons)** : convention différente, aucun n'a tort. Règle de canal mono : moi « moyenne < moitié du canal le plus fort », `tout.py` « perte > 3 dB contre la moyenne ». Médiane bouge : non.
- **f0 des one-shots mélodiques (50 sons, RAR 2, 4, 12)** : c'est mon outil qui se trompe. Il retient un harmonique (×2, ×3, ×5, ×6, ×10, jamais en dessous) quand le fondamental grave est faible. Troisième chemin, la note écrite dans le nom du fichier (41 sons en ont une) : la table tombe sur la bonne note 39/41, moi 21/41 (seulement les cas d'octave). Médiane bouge : non (la table reste juste).

Conclusion : aucune colonne des tables n'est à corriger d'après cette contre-mesure. Les écarts viennent de mon outil
(fenêtre d'attaque, bande de tonalité, f0 harmonique, abandon sur queue courte), d'une convention (mono) ou de sons
où la hauteur n'est pas définie de façon stable (queue qui glisse, partiels non harmoniques, sons bruités).
Pour ces derniers, la hauteur stable et la hauteur d'attaque des snares, claps, percs et cowbells ne devraient pas
servir de cible fine (au mieux une indication à ±1 demi-ton).
