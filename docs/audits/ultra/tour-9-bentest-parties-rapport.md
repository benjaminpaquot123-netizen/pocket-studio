# Tour 9 — lot « bentest-parties » : rapport

Source : demande de Benjamin (2026-11-25), relayée par le chef et le pilote d'arrivée.

## Fait
- Scénarios complets découpés en « Partie 1, Partie 2… » de 50 au plus, calculés depuis `Scenarios.LISTE` à
  l'affichage (133 scénarios aujourd'hui, donc 3 parties ; suit tout seul quand LISTE grandit).
- Chaque partie se lance seule ; une partie finie est gardée dans `bentest/parties/partie-n` (carnet et captures),
  « Tout recommencer » ne l'efface pas ; les parties faites sont cochées (✓, vert).
- Pendant une partie : « Partie 2 · Test 3 sur 50 » (compté dans la partie) et « Test en cours : <nom> ».
- Tout en bas : « Générer le rapport et l'envoyer » : un zip avec `parties.md`, chaque partie jouée
  (`parties/partie-n/` : rapport, résumé, scenarios.json, carnet, captures) et le dernier passage, puis le menu de partage.
- Tous les boutons d'avant restent.

tests: --tests '*PartiesTest' --tests '*bentest*'
chemin: BenStudio Test, accueil → « Partie n » | test: PartiesTest#eachPartStartsAloneTheDoneOnesAreTickedAndTheReportButtonIsAtTheBottom
chemin: BenStudio Test, découpage de LISTE en parties | test: PartiesTest#thePartsCoverEveryScenarioOnceAndHoldFiftyAtMost
chemin: BenStudio Test → « Générer le rapport et l'envoyer » | test: PartiesTest#theReportButtonGathersEveryPartPlayedAndOpensTheShareMenu

## À dire au relecteur
- Rouge f0dbb280, vert 0d15780f. Capture : `app/screenshots/bentest/accueil_parties.png`.
- `ScenariosTest#eachEffectTypeGoesOnAStripIsSavedSoundsAndIsTakenOff` a raté une fois (réverbe silencieuse) puis
  est passé seul : sans lien avec ce lot (aucun changement de moteur ni d'effet).
