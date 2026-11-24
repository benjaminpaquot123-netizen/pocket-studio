# Décisions du lot gr-l0 (tour 9, grand remplacement v2, lot L0)

- 2026-11-24 · Licences publiées : liste blanche CC0, CC-BY (toutes versions) et domaine public ; NC, SA, ND, Sampling+ et licence sans nom arrêtent `build_index.py` avant tout zip. Raison : dépôt et packs publics, Salamander (CC-BY-3.0) et Splendid (domaine public) déjà publiés. Retour : retirer une ligne de `_ALLOWED`.
- 2026-11-24 · Claquements de doigts pour les recettes : par le pack existant `bruitages_maison` (son `doigts`, venu de oga-snaps), pas de pack neuf. Raison : un pack neuf demande sa traduction dans I18n (hors du lot, PackCatalogTest). Retour : un pack dédié avec ses traductions.
- 2026-11-24 · Flûte pour les recettes : la partie flûte (touches 67 à 100, `FluteSusVib.sfz`) de `instrument_bois`, déclarée `recipes: {flute: [67, 100]}`, pas d'instrument neuf. Raison : `MultiInstruments.KNOWN` est hors du lot. Retour : un `instrument_flute` (L6).
- 2026-11-24 · Index : un pack marqué `recipes` publie `recipeSounds` (ses slugs vraiment construits) ou `recipeParts` ; l'appli ignore ces champs (ignoreUnknownKeys) ; c'est à L1 de les lire pour valider `lib:<pack>:<son>`.
