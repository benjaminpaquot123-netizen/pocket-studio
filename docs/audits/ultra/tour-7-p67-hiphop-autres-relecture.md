# Relecture neuve : consignes Facile hip-hop et autres (tour 7, p67)

relu : 7f5194d85

## Ce qui a été vérifié

- Les 11 cours (boom_bap, drill, grime, jersey_club, lofi, phonk, trap, afrobeats, pop, reggaeton, synthwave), ancienne base 3cd69a965 contre 7f5194d85, étape par étape.
- Mêmes étapes (ids, niveaux, check, spot, target, part, length, song, listen, why, skip) : aucun champ autre que `text`/`textPro` n'a bougé.
- Texte Pro inchangé : pour chaque étape vue en Pro, le texte affiché (`textPro`, sinon `text`) est identique à l'ancien. Les seules différences sont sur des étapes `levels: "f"` (pas affichées en Pro).
- Chaque texte Facile comparé à son `check` et à son texte Pro : nombres, positions, pads, valeurs, ×4, ordre et durées des parties (`song`). Les boutons du moteur retirés du texte (« Passer à N mesures », « Créer la piste … », « Copier sur les mesures suivantes », « Donner les durées conseillées », « Aller dans la partie », « Poser ce qui manque ») sont bien affichés par le panneau en Facile (« À toi : touche « … » »), donc les retirer est correct (AUTHORING §12).
- Détecteur du roulement (TutoKind.DrumFill, TutoChecks.fillEnd/gapCells/gapStep) relu pour pop, afrobeats, reggaeton et synthwave.
- Traductions : script python (aucun fichier écrit) qui charge tous les `i18n/<langue>/*.json` des 14 langues. Tous les JSON se lisent ; les 775 phrases françaises des 11 cours (text, textPro, why, skip, titres) existent dans les 14 langues, aucune vide ; les 71 phrases neuves de ce lot comprises. Chiffres comparés au français : les écarts viennent seulement de l'écriture (Ré2 → D3 en notation anglaise, « 2 mesures » → duel arabe, chiffres collés au japonais).
- Ancien grave (pop, `fill` : « avant le dernier temps » et « ×4 » manquaient) : corrigé en français et dans les 14 langues ; chaque traduction garde « avant le dernier temps » et « ×4 ».

## Bloquant

Aucun.

## Grave

Aucun.

## Moyen

- reggaeton `fill` et `gap` (déjà avant ce lot, même texte en Pro) : à 96 BPM (< 100), le silence ne fait que 2 cases et la fenêtre du roulement tombe sur les cases 11 à 14 de la dernière mesure, à cheval sur le 3e et le 4e temps. Le texte « juste avant le dernier temps » / « efface les coups du dernier temps » ne colle donc qu'à moitié ; les cases en pointillé montrent le bon endroit, donc pas bloquant. À régler dans un lot moteur ou texte à part (donner `length: 4` au `gap`, ou dire « les 2 dernières cases »).
- pop `loudness` (315 caractères), trap `loudness` et `map_bars` (208), pop `topline` (198), drill `map_bars` (187), boom_bap `loudness` et synthwave `ready` (174) : textes Facile non réécrits par ce lot, toujours au-dessus du budget de 170 caractères.

## Mineur

- trap `song_phrases` (déjà avant, identique en Pro) : la liste s'arrête à « Pré-refrain 2 » sans « Outro 4 », alors que les 10 autres cours citent la partie de fin.
- pop `fill` : le Facile utilise « Roulement », alors que AUTHORING dit que le Facile n'a pas de roulement (déjà avant ; c'est la seule façon de réussir ce check sur la caisse, qui monte dans la partie).
- grime `bass_sub` : « Écoute la piste « Sub » » pour un check `track:sub` ; c'est le bouton du moteur « Copier la basse une octave plus bas » qui fait le geste, la phrase dit « écoute » là où il faut d'abord toucher. Clair à l'usage, tournure à revoir.
- hi, `fill` de pop : « आख़िरी बीट से ठीक पहले “Snare” लंबा दबाओ » ne dit plus « le dernier coup » de caisse ; le sens (juste avant le dernier temps, ×4) reste.
- synthwave : le `gap` n'existe qu'en Pro ; en Facile la fenêtre du roulement reste calée 4 cases avant la fin (gapStep cherche tous niveaux), ce qui colle au texte. Rien à faire, noté pour mémoire.

Verdict : 0 bloquant, 0 grave
