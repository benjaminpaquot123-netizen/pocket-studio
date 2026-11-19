# Tour 9, lot L5b : décisions

- Fichiers : `i18n/hi/tuto.json` et `i18n/ar/tuto.json` comptent comme fichiers du tuto (`tuto*.json`) : F073, F075 (« Le mix »), F088 y sont corrigés. Retour arrière : revert du commit vert concerné.
- F097 : `i18n/tr/model.json` (nom de la partie « Post-refrain ») est un `tr/*.json` permis par la fiche (ligne F099) ; la carte des parties lit ce nom. 14 lignes de tuto turques suivent le même mot.
- F077 élargi à toutes les langues : la consigne nomme la feuille par la traduction de son titre (`Tonalité du morceau`), 61 clés × 14 langues ; en ko, les deux variantes (« 키 시트 », « 톤 시트 ») sont unifiées.
- F068 : en de, le glossaire dit « Wirbel » mais le bouton dit « Roll » : la consigne garde le mot du bouton (« Roll ») et gagne son verbe.
- F090 limité à ja et nl (le constat du siège). La source française et les autres langues disent encore « croix rouge » alors que la légende dit « barré : à enlever » : seule la source des tutos (`tutos/*.json`, hors fiche) peut le corriger partout. À reprendre par le lot qui tient les sources.
- Reste hors fiche : `ar/extract.json` décrit encore le Hoover avec « حفلات الرّيف » (la campagne) ; pas un fichier du tuto, laissé au lot qui tient `extract.json`.
- Laissés de côté (fichiers hors fiche, le chef a ouvert les fichiers à 19 h 15 puis a repris les défauts non commencés pour des équipes neuves ; rendus) : F089 et F099 (`ShareSongUi.kt`, texte en dur, lot L8), F078 (`tutos/lofi.json`), F081 (`ko/audit1z56.json`), F084 (`es/home.json`), F086 (`tutos/uk_garage.json`), F087 (`tr/audit1z56b.json`), F093 (`zh-CN/audit1z56.json`).
- Éditions ligne par ligne (certains fichiers ont des clés en double) : seules les valeurs des clés du lot changent, aucune clé déplacée.
