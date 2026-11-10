# Lot « textes » : rapport de l'auteur

Branche `eq/tour-5-textes` (départ 4137b066), un seul auteur, rien n'est poussé. Étape 4, chantier 3 (audit de prise en main § 5).

## Choix (aussi dans DESIGN « Lexique » et DECISIONS, 7 lignes)
- Assistant : un seul nom, « Fais-moi un morceau » (carte, écran, aides, kits des tutos). Plus de « assistant », « guidé », « pas à pas » pour lui ; « l'appli » dans les phrases où le nom serait lourd.
- « Guide » = la barre à cartes seule. La fenêtre de 5 pages = « Visite rapide » (en : Quick tour, de : Kurztour, ja : クイックツアー…) ; « Aide et premiers pas » devient « Aide ».
- Sortir un fichier = « Partager ». Mix : « Créer le fichier du morceau / par piste / MIDI », carte « Autres fichiers » ; « Envoyer le projet » = « Sauvegarder le morceau en .zip » ; « Exporter le M4A » = « Partager le M4A ».
- « Mettre les bouts sur… » (écran Découper) ; « Effets de scène » = « Effets live » (aide : « Jouer les effets live »).
- Mode simple sans « dB » : « Normal », « +3 », « −6 », « Plus de son », « Remettre au volume normal », conseils « un peu », phrase des tutos Facile.
- 13 langues : 14 zones « textes » (≈45 textes écrits à la main), 18 textes de kits réécrits par règle de langue, mêmes mots partout.

## Tests rouge puis vert
- `SimpleNoDecibelTest` (3 parcours : onglets et ⋮, Mix avec conseil/batterie/Guide, fiche de piste) : rouge 183e094c (dB partout), vert au commit « vert ».
- `LexiconWordsTest` : 4 tests (`oneWordPerThingInFrench`, nom de l'assistant et de la visite identiques dans chaque langue, un mot pour le Guide par langue, tutos Facile sans dB) : rouge 7e92aaff (4 échecs sur 6 vus), vert au commit « vert ».
- Deux tests existants disaient le contraire et ont été retournés : `TutoLot4Test` (exigeait « dB » expliqué en Facile) ; `CutSpanJourneyTest`, `LanguageTest`, 12 tests de parcours (texte de la carte d'accueil) mis à jour.

## Captures
Enregistrées, sauf les 5 instables (remises à l'état d'avant). Regardées : welcome, nav/simple_mix, scale130/fr_simple_mix (130 %), tablet/simple_mix, scale130/fr_projects (130 %), tablet/wizard_style, nav/simple_more : rien de coupé ; sur le Mix à 130 %, le défilement latéral des pistes coupe « Sortie » au bord, comme avant.

## Doutes
- « Exporter » reste dans la prose de quelques tutos (« exporte ton morceau ») et « Puis envoie-le » dans l'aide longue « Un bout » ; « Envoyer » du rapport de bug, des retours et MIDI reste (autre sens).
- « J'apprends » gardé (audit : « J'apprends à le faire ») ; « Mon premier morceau, avec le Guide » gardé.
- Non fait : « Voir le réglage » → « Ouvrir » (GuidePanel, AutomationCard), « (passe en mode Pro) », « Tout couper » de Pad Live.
- Contraste du menu ⋮ : plafond du recensement relevé de 2 à 5 (raison écrite) ; la capture est lisible.
- Traductions écrites par l'auteur, non relues par un natif.

## À rejouer à la fusion
`I18nCatalogTest`, `LexiconWordsTest`, `GuideParcoursTest`, `HelpCoverageTest`, `ScreenScanTest`, `SimpleMixTest`, `SimpleNoDecibelTest`, `TutoLot4Test`, `CutSpanJourneyTest`, `LanguageTest`, `FirstLaunchTest`, `OnboardingUiTest`, tous les `human/` qui tapent la carte d'accueil, et les captures. Conflits probables : `docs/DECISIONS.md`, `recensement-acceptes.tsv`, `i18n/*/textes.json`.
