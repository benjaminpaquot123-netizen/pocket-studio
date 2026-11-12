# Audit ultra, tour 3 : zone 3 (lecture, navigation, sauvegarde, prise en main), n° 15 à 22

Tests sans le banc : `Zone3Round3NavTest` (appli entière, AppHarness), `Zone3Round3LogicTest` (ViewModel), `Zone3Round3FieldTest`, `Zone3DialogDiagTest`.

| n° | Verdict | Gravité | Test | Ce qui change / pourquoi |
|---|---|---|---|---|
| 15 morceau entier, depuis une partie, depuis ma prise | banc (verdict du banc, `tour-3-banc.md`) | — | `Tour3AppProofTest` | Avec un moteur, la lecture part de la bonne partie. |
| 15 « Jouer » = boucle « Montée 1/4 (en boucle) » | banc | — | même preuve | Sans moteur, ▶ retombe sur la partie seule (`startPlaying`) et la bande dit « en boucle ». |
| 15 compteur « mes. 1 » figé | banc | — | lecture du code | `pollPosition` sort tout de suite sans moteur ; le compteur lit la position du moteur (`songStep`/`absoluteStep`). |
| 16 gel en tapant un nom (B a, E c) | **banc** (contre l'hypothèse « appli » du banc) | — | `Zone3DialogDiagTest` | Sous Robolectric, un `AlertDialog` de Material avec un simple `OutlinedTextField`, sans thème ni code de l'appli, ne revient jamais au repos (aussi : `Dialog` + `BasicTextField`, graphismes LEGACY ou NATIVE) ; le même champ dans un `Popup` ou seul se repose. Toutes les fenêtres « Renommer » sont des dialogues, d'où le gel du banc. Rien à corriger dans `NameField` ; les preuves `Tour3RenameIdleTest` restent `@Ignore` avec ce verdict. À l'équipe banc : ne pas attendre le repos de Compose quand un dialogue contient un champ (horloge manuelle). |
| 16 renommer depuis Pad Live colle derrière l'ancien nom (D b, revient) | appli | moyen | `Zone3Round3FieldTest` (rouge avant : pas de ✕) | Le siège touche deux fois : le 2e toucher pose le curseur (comportement Android normal). Racine : aucun moyen simple d'effacer. ✕ « Effacer » au bout de chaque champ de nom (`NameField`, `SelectAllTextField` : projet, piste, partie, son gardé, partage). |
| 17 accueil sans « Reprendre mon morceau » | banc | — | `LaunchFlowTest` (vert) | Le siège « revient 3 jours après » mais le banc fait un premier lancement (bienvenue, projet de départ). Au vrai retour, « Continuer » est la 1re carte. |
| 17 « ouvert à l'instant » au lieu de « il y a 3 jours » | banc | — | `anOpenedProjectKeepsItsLastEdit` (vert sans changement) | Ouvrir un projet ne change pas sa date ; « à l'instant » = projet de départ créé par le banc. |
| 17 « Rétablir » introuvable | pas un défaut | — | — | ↷ apparaît au pouce dès qu'on a annulé ; rien n'avait été annulé. |
| 18 plus de Retour après suppression du projet ouvert | appli | grave | `backAfterDeletingTheOpenProjectLeadsHome` (rouge avant) | Sans projet, « ← »/Retour de Mes projets mènent à « Que veux-tu faire ? » (état sans projet), d'où Retour sort ; avant : l'appli passait derrière. |
| 18 Retour pendant « Enregistrer ma mélodie » jette les notes | banc | — | `backDuringAKeyboardTakeKeepsItsNotes` (vert sans changement) | Les notes restent (mélodie ou clavier) ; la position de prise vient du moteur, absent au banc. |
| 19 « Enregistrer ma voix » ouvre « Live » | appli | moyen | — (geste d'accueil, lecture du câblage) | Quand le projet ouvert est le « Live » de Pad Live, la carte ouvre le dernier morceau sur sa page Audio. (E c, premier lancement : le projet de départ, normal.) |
| 19 « J'apprends » repart au chapitre 1 dans un nouveau projet | appli | grave | `learningAgainTakesUpTheCourseUnderWay` | Même style et niveau, pas fini, projet encore là : on rouvre ce projet et le tuto reprend, avec « Ton tuto reprend là où tu l'avais laissé. » (15 langues, `ultra3z3.json`). Fini : nouveau projet comme avant. |
| 21 paysage téléphone : rail coupé (Mix, Envoyer) | appli | moyen | `lyingDownTheFiveSpacesFitInTheRail` (rouge avant) | Si 5 × 60 dp ne tiennent pas, entrées compactes à parts égales (icône + nom 11 sp). Capture `nav/phone_lying_rail_simple.png` regardée : les cinq entières. |
| 20, 21 (reste), 22 | non fait | moyen/mineur | — | Voir « Reste ». |

## Vérifications
- Rouge avant puis vert : `Zone3Round3NavTest` (2), `Zone3Round3FieldTest`. Verts sans changement (verdict banc) : `Zone3Round3LogicTest` (prise clavier, date), `TutoResume` vert après.
- Verts aussi : `NameFieldTest`, `LaunchFlowTest`, `ZeroProjectsTest`, `Zone3DialogDiagTest.theSameFieldInAPopupRests` ; i18n `ModelTextsTest` vert, `I18nCatalogTest` : 2 échecs venus d'autres zones (en/ultra1z1 « Plus de pad libre… » inutilisée, es « «%s» … Mezcla »), rien de `ultra3z3`.

## Reste
- 20 une main : OK de Renommer, choix de boucle et carte Simple hauts ; à reprendre avec captures.
- 21 : en paysage, barres empilées (haut, bande de lecture, Guide, barre « Mode retours » des testeurs) laissent ~190 dp ; carte Continuer vide ; Guide sur la piste Basse en tablette 130 %.
- 22 : « Un style en une touche » (4 écrans), « ⋮ › Aide » cité sur la bienvenue sans ⋮, bandeau « Nouveau son… / Annuler » qui reste en changeant d'onglet (6 s, possiblement le temps du banc).
- 19 n'a pas de test d'écran (câblage de l'accueil).
