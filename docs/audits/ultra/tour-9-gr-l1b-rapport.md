# Tour 9, grand remplacement v2, lot L1b : rapport

Le corps commun du format v2 s'entend dans l'appli : couches (lib / kit / pack) sur les pads libres, boucle sur sa piste audio dès le Groove, chop vocal échantillonné, coup de transition sur le crash, shaker du corps (pad 15) ; SongShape connaît les nouvelles lignes ; les fills de toms laissent le pad 9 (Snap).

Fichiers :
- project-model : StarterWizard.kt (Extras, LoopSound, layerLanes, transitionLane, withLayers, withLoopTrack, crochet softTransitions, ordre contre-chant / doublure / texture, tomLine des fills), PackRefs.kt (nouveau : lire « lib:pack:son », « kit:N », un id de pack, l'impact), WizardMix.kt (honest() garde la pompe « chords » du style), SongShape.kt (ENTRY / MUTES), DrumHumanizer.kt et PartIntents.kt (toms sur 8 et 7), DrumArrangement.kt (tomLine).
- app : library/SoundLibraryViewModel.kt (sons du corps proposés et chargés : kitSounds en tête de liste, couches, boucle, voix, impact ; build reçoit les Extras).
- scripts : packs/recipes_to_kotlin.py (ajout 1 du chef : tout pack marqué « recipes » dans libraries.json est accepté, voix comprises ; StyleRecipes.kt régénéré seulement).
- tests : BodyHeardTest.kt (nouveau), PlainStyles.kt (nouveau : un style sans corps tiré de n'importe quelle recette), RecipeFormatV2Test.kt, PartIntentsTest.kt, StarterWizardTest.kt (le tom aigu passe du pad 9 au pad 8), StyleBodyTest.kt, app SoundCategoriesTest.kt.

- rouge : 9e6efdbf (tests seuls, rouge à la compilation : StarterWizard.Extras n'existait pas) ; vert : 2f06be7c ; relecture : dernier commit.
- Ajout 1 (packs recipes) : rouge 104bc954, vert d3d4931c.
- Ajout 2 (tests indépendants des styles convertis) : 87ad19c2.
- Ajout 2 (forme du morceau) : rouge 3a5848d9, vert 5a0a2915. Le diagnostic « withShakerInDrop » ne tenait pas (avec ou sans shaker, même morceau) : la cause est le contre-chant du corps sans lead ni accords (il fabriquait ses propres stabs sur 2 tours : reggaeton 84 → 92 mesures, scènes Crash / Fill perdues) et la texture tenue jusque sur le temps de silence avant le Drop (drill Couplet 3, techno Montée 2). Correctif : contre-chant seulement avec lead ou accords ; texture coupée au temps de silence. Test : BodyHeardTest#theBodyKeepsTheSongsShape (tous styles, humeurs, parties, acid : même durée, mêmes scènes, mêmes silences avec ou sans corps).
- Grave de la relecture (SoundLibraryViewModel `packs.first`) : rouge f1942838, vert 11c818c7 (un son nommé d'un autre type ne plante plus la liste).
- Grave de la relecture 2 (Doublure hors des accords) : 5948fdcc, protection (aucun style actuel n'atteint le cas : test vert avant et après, donc pas de commit rouge).
- Tests : project-model complet vert ; app SoundLibrary* et Wizard* verts ; live-bench trap / techno / house 3/3 sans alerte.

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

- Un style à corps dont les parties n'ont ni lead ni accords n'a pas de contre-chant (choix : sinon le morceau s'allonge).

Statut : tenté.
