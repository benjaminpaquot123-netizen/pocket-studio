# Tour 9 : lot « cat-sauve » (rapport)

Base : `eq/tour-9-robot-a2`. Branche : `eq/tour-9-cat-sauve`. Catalogue BenStudio Test, thème sauvegarde, projets et export. Tout est « tenté ».

- 18 scénarios `sav_export_<style>_<format>` (ScenariosSauve.kt) : des styles qu'aucun autre scénario n'exporte, en WAV (les plus forts : la saturation n'est pas masquée par un codeur), FLAC et M4A. Geste existant « export » (aucun geste neuf) ; oracle lu sur le disque : durée contre celle de l'appli, crête, ni silence ni saturation, temps pris.
- Scenarios.kt : une seule ligne (`+ ScenariosSauve.LISTE`). Rouge 263ec392, vert 92fe4138.
- Laissés au pilote d'intégration, comme demandé : ScenariosTest.kt:262 (taille 133 en dur) et :275 (plafond d'estimation 65 min).

tests: *ScenariosSauveTest

## Chemins

chemin: BenStudio Test > Scénarios complets > export d'un style | test: ScenariosSauveTest#each one exports a real style in a format the export knows, and none repeats another
