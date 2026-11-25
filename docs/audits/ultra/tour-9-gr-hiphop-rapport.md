# Tour 9, zone « gr-hiphop » (grand remplacement v2, lots L2, L6, L7, L8) : rapport

Branche : eq/tour-9-gr-hiphop (= eq/gr-hiphop), sur eq/gr-l1 avec eq/gr-l1b fusionnée (d3d4931c).

Ce qui change (scripts/packs/recipes/hiphop.json, instruments.json, libraries.json, traductions des conseils) :
- Trap : la flûte VSCO-2 (vibrato tenu, relâchement 0,2 s, passe-bas vers 7 kHz, plate en envoi) devient le lead, la Cloche FM reste en secours ; ad-lib « yeah » ; 808 en avant avec la pompe.
- Boom bap, drill, lofi, phonk, grime, reggaeton, afrobeats, jersey club : corps moderne (body), son signature de chaque style, kick et basse devant ; ad-libs CC0 (brrr, skrrt, kick it) sur drill, phonk et jersey club.
- Pack « Voix : ad-libs rap » : 8 aperçus Freesound CC0 (pages lues), aucun audio dans git.

Bancs (après) : live-bench 9/9 styles sans alerte ; song-bench, médianes au vert (écart à la cible −0,0 LU, true peak −1,19 dBTP, Drop − Couplet +2,6 LU). Rendus A/B : /tmp/claude-0/-home-user-pocket-studio/65f01ad1-2357-5225-9ae1-e42b0cb3d016/scratchpad/avant/song/wav et /tmp/claude-0/-home-user-pocket-studio/65f01ad1-2357-5225-9ae1-e42b0cb3d016/scratchpad/final/song/wav.

Rouges connus hors de la zone (à gr-l1b, signalés au chef) : withBody() force le riser (lofi ClubTransitions, HonestMix clipper du lofi, reggaeton 230 s), make_guides.py et le vocal en objet (GuideParcours), GuideActions (le trap n'a plus de contre-chant), RecipeFormatV2Test (le trap a déjà un snap), StyleBody, StarterWizard, SongShape drill.

tests: *OneTouchStyleTest *WizardTracksOfferedTest

chemin: Projets → « Partir d'un style » en une touche, morceau fait | test: OneTouchStyleTest#aModeChosenOnceOneTouchMakesTheSong
chemin: assistant, pistes proposées d'un style | test: WizardTracksOfferedTest#aSongWithoutTheAcidStepShowsNoAcidTrack

Statut : tenté (aucun testeur neuf ne l'a encore constaté).
