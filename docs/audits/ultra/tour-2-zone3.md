# Audit ultra, tour 2 : zone 3 (navigation, prise en main, sauvegarde, une main), n° 11 à 17

Sièges B a (parent, une main, téléphone 360 dp, premier lancement), B d (anxieuse, nuit, Simple), A c (enfant, ne lit pas), B c (Pro).
Chaque correction a son test dans `Zone3Round2Test` (la vraie appli dans Robolectric, comme les sièges), rouge avant, vert après (§4).

| n° | Verdict | Gravité | Test | Ce qui change |
|---|---|---|---|---|
| 11 Guide ouvert = barre du pouce cachée | appli | grave | `theOpenGuideNeverHidesTheThumbsBar` (rouge avant : pas de `thumb_play` sous le panneau, 360 × 780) | Le panneau du Guide ne retire plus jamais la barre du pouce (▶ reste au pouce) ; sous 820 dp de haut il perd la hauteur de la barre (52 dp) : la page au-dessus garde la place d'avant ; au-dessus, rien ne change (les deux tenaient déjà). `guidePanelHeightDp`. |
| 11 panneau vide « Le Guide lit ton morceau… » sur la moitié basse | appli | moyen | `theGuidesPanelMakesRoomForTheBarAndIsSmallWhileReading` | Pendant la lecture du morceau, le panneau ne fait que sa barre et la ligne (96 dp). |
| 12 16 écrans avant le son (×2) | appli | grave | `aBeatPlaysOneTouchAfterTheStyle` (rouge avant : pas de bouton) | La carte du style ouverte propose d'abord « ▶ Créer et écouter » : le morceau est fait avec les choix conseillés et joue dès qu'il s'ouvre (depuis ~8 mesures avant le Drop, comme « Écouter » du Guide). Accueil → son : Créer, Simple, famille, style, « Créer et écouter » = 5 touches, 1 après le style. L'assistant reste : « Choisir ce style, pas à pas ». |
| 13 « Choisir ce style » sous la liste d'exemples | appli | moyen | `theStylesChoicesComeRightUnderItsDescription` | Les deux boutons juste sous la description, avant les références. |
| 13 étape sans titre, « Créer tout de suite » invisible | appli (racine) | moyen | `eachStepOpensAtItsTop` (rouge avant) | Le défilement était partagé entre les étapes : chaque étape s'ouvrait aussi bas que la précédente (titre « Kick » et « Créer tout de suite » hors écran). Une position par étape. |
| 13 « Les vrais sons du style » répété | appli | mineur | — (ordre de mise en page) | Sous le choix du son de la ligne, plus au-dessus. |
| 13 deux « Retour » | appli | mineur | — | Celui du navigateur de sons s'appelle « Dossier précédent ». |
| 13 carte « Simple » haute, « Mes projets » en haut | non fait / pas un défaut | moyen | — | « Mes projets » est au pouce dans « Plus » de la barre du pouce (le siège l'y a trouvé). La place de la carte Simple : non fait (écran partagé par tous les départs, à reprendre avec une capture). |
| 14 projet fantôme « Mon premier rythme » | appli | mineur | `noProjectNobodyMadeInMyProjects` (rouge avant : listé) | Le premier projet d'une installation est noté tel qu'écrit ; s'il est laissé intact pour un autre projet, il est effacé (jamais un projet modifié : comparaison du fichier). `CurrentProject.takeUnusedStarter`. |
| 15 « Mon pr… » + coche, pas de « Sauvegardé » (×2) | appli | moyen | `theTopBarSaysSavedInWords` (rouge avant : nom coupé) | En Simple avec la barre du pouce, ↶ n'est plus qu'au pouce : ses 48 dp vont au nom et au mot « ✓ Sauvegardé » (entiers sur le téléphone de Benjamin). |
| 15 aucun mot en quittant | appli | moyen | `backOnHomeLeavesTheAppAndSaysItIsSaved` | « Continuer » dit « ✓ Sauvegardé à l'instant » (heure de la dernière sauvegarde, en vert) ; Retour sur l'accueil : « Ton morceau est sauvegardé. » (seulement si c'est vrai). |
| 15 Retour sur l'accueil ne quitte pas | banc | — | même test : `isTaskMovedToBack` vrai | L'appli passe bien en arrière-plan (`moveTaskToBack`) ; Robolectric ne le montre pas à l'écran. À l'équipe banc : écrire « l'appli est passée derrière ». |
| 15 Retour depuis Mes projets → accueil | pas un défaut (décision de Benjamin, 2026-10-29) | — | `HomeAndPadsTest.myProjectsBackLeadsHome` | Gardé : « ← » de Mes projets mène à l'accueil, « Continuer » ramène au morceau. À trancher par Benjamin s'il veut l'inverse. |
| 16 accueil changé (chapeau vert disparu) | appli | moyen | `theHomeKeepsJapprends` | Carte « J'apprends » (chapeau vert) sur l'accueil, comme sur la bienvenue. |
| 17 nouveau son, piste supprimée : aucun message | appli | moyen | `aTracksNewSoundAndItsDeletionAreSaid` | « ✓ Nouveau son : Basse sub · Annuler », « ✓ Piste « Basse sub » supprimée. · Annuler » (la barre « Fait » existante), partout : synthé, Plaits, instrument, favori, piste audio, Acid 303. |
| 17 « Supprimer » dans « Volume de Synthé » | appli | moyen | — | Le bouton s'appelle « Réglages de Synthé : volume, couper, supprimer ». |
| 17 nom de piste qui ne suit pas le son (×2, Simple et Pro) | appli | moyen | `aTracksNewSoundAndItsDeletionAreSaid`, `aKeptSoundNamesATrackTheAppNamed` | Une piste au nom donné par l'appli (« Synthé », « Piste 2 », un nom de son) prend celui du son choisi ; un nom tapé reste. Les noms de sons de la bibliothèque sont traduits comme les autres noms de l'appli. |

## Vérifications
- `Zone3Round2Test` 11/11 vert ; rouge avant pour 11, 13 (défilement) et 15 (barre du haut), vérifié en remettant temporairement l'ancien code ; 12, 14, 16, 17 n'existaient pas avant (bouton, effacement, carte, message).
- `:project-model:test` i18n (`I18nCatalogTest`, `ModelTextsTest`) vert ; « modifié %s » retiré des 14 catalogues (plus affiché).
- Nouvelles chaînes : `i18n/<langue>/ultra2z3.json` (14 langues).

- Suite ciblée (33 classes : navigation, accueil, Guide, assistant, pistes, sons, barre du haut, textes) : vertes après retouches de tests qui supposaient ↶ en haut en Simple (`NavigationMapTest`, `Zone3JourneysTest`), « modifié » (`LaunchFlowTest`, `LaunchRuleTest`), la barre « Fait » posée sur « Partager » (`SpacesTest`). `SimpleModeAuditTest.theRemixAsksForNotifications` rouge : vient de la question du modèle Remix (zone 6, a70337d), pas de cette zone.
- Captures ré-enregistrées et regardées : `wizard_style_open`, `wizard_kick_packs`, `nav/launch*`, `nav/simple_*`, `nav/guide_simple`, `nav/help_labels_simple` (elles portent aussi les changements non commités des autres zones visibles sur ces écrans, ex. carte « Enregistrer ma voix »).

## Reste
- La barre « Fait : … · Annuler » (6 s) se pose sur le bas de la page (ex. « Partager ») : motif existant, à revoir.
- Nom long dans la barre du haut (« Mon premier ryt… ») : le mot « Sauvegardé » est là, le nom entier est sur « Continuer ».
- Carte « Simple » de « Simple ou Pro ? » dans le tiers haut (une main) : non fait.
- Banc : Retour qui fait passer l'appli derrière n'est pas montré au testeur.
- n° 15 Retour de Mes projets : décision de Benjamin à confirmer.
