# Tour 9, grand remplacement v2, lot L1b : rapport

Le corps commun du format v2 s'entend dans l'appli : couches (lib / kit / pack) sur les pads libres, boucle sur sa piste audio dès le Groove, chop vocal échantillonné, coup de transition sur le crash, shaker du corps (pad 15) ; SongShape connaît les nouvelles lignes ; les fills de toms laissent le pad 9 (Snap).

Fichiers :
- project-model : StarterWizard.kt (Extras, LoopSound, layerLanes, transitionLane, withLayers, withLoopTrack, crochet softTransitions, ordre contre-chant / doublure / texture, tomLine des fills), PackRefs.kt (nouveau : lire « lib:pack:son », « kit:N », un id de pack, l'impact), WizardMix.kt (honest() garde la pompe « chords » du style), SongShape.kt (ENTRY / MUTES), DrumHumanizer.kt et PartIntents.kt (toms sur 8 et 7), DrumArrangement.kt (tomLine).
- app : library/SoundLibraryViewModel.kt (sons du corps proposés et chargés : kitSounds en tête de liste, couches, boucle, voix, impact ; build reçoit les Extras).
- tests : BodyHeardTest.kt (nouveau, 8 tests), PartIntentsTest.kt, StarterWizardTest.kt (le tom aigu passe du pad 9 au pad 8).

- rouge : 9e6efdbf (tests seuls, rouge à la compilation : StarterWizard.Extras n'existait pas) ; vert : 2f06be7c ; relecture : dernier commit.
- Tests : project-model complet vert (1288) ; app SoundLibrary* et Wizard* verts ; live-bench trap / techno / house 3/3 sans alerte.

tests: com.pocketstudio.model.BodyHeardTest com.pocketstudio.model.PartIntentsTest com.pocketstudio.model.StarterWizardTest com.pocketstudio.model.StyleBodyTest com.pocketstudio.app.bench.LiveSongRecorder

chemin: assistant → morceau en un geste → lecture en direct (corps v2 câblé ; aucun style ne l'active encore ; preuve modèle : BodyHeardTest) | test: LiveSongRecorder#recordsTheLivePlaybackOfEveryStyle

Mineurs de la relecture L1 :
- pompe « chords » retirée par honest() : corrigé (le canal pompé par le style est sauté) ;
- doublure / texture avant le contre-chant : corrigé (contre-chant d'abord) ;
- clé i18n du conseil shaker : déjà présente dans les 14 catalogues, rien à faire.

Choix et limites :
- Quand la ligne Tom d'un style se tait dans une partie (Break), le fill de toms ne joue que le tom grave (pad 7) : le pad 8 reste à sa ligne.
- Les couches copient le pattern et le canal de leur ligne (−4 dB), pas l'automation de volume du pad source.
- La boucle se coupe dans les parties d'Intro du début (avant la première scène pleine), joue ensuite jusqu'au bout.
- Transitions douces : seulement le crochet `StarterWizard.softTransitions(project, shape)` (rend le projet tel quel), appelé dans build juste après la forme du morceau ; le lot transitions le remplira.
- Moteur C++ non touché. StyleRecipes.kt et scripts/packs/ non touchés.
- Aucun style de StyleRecipes n'a encore `body = true` ni couches / boucle / voix : le live-bench ne fait donc pas encore entendre ces sons ; seuls les tests du modèle les prouvent.

Statut : tenté.
