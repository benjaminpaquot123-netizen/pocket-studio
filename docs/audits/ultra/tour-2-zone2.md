# Audit ultra, tour 2 : zone 2 (exports et master), n° 8 à 10

Siège A d (DJ club, anglais, Pro, Techno). Chaque correction a son test, écrit pour échouer avant (AUDIT-ULTRA §4).

| n° | Verdict | Gravité | Test | Ce qui change |
|---|---|---|---|---|
| 8 LUFS club = streaming | banc (preuve sans banc) | grave | hôte `mp_club_and_streaming_exports_differ` (renforcé), `OneTargetExportTest` | Le gain de la cible est appliqué par le moteur natif (rendu en deux passes, `OfflineRenderer`), pas en Kotlin. Le test hôte écrit maintenant de vrais fichiers WAV (comme l'export de l'appli) et les mesure avec un compteur indépendant : club −8,15 LUFS, streaming −14,01 LUFS, crêtes ≤ −1 dBTP. L'appli envoie bien la cible au moteur d'export (−8 sur le Drop, −14 sur tout le morceau). Le banc rendait la même sinusoïde pour toutes les cibles : son moteur de remplacement (`BenchEngine`) applique désormais la cible reçue (club ≈ −8, streaming ≈ −14 sur ses 3 s). « Durée : 3 s » : banc (3 s de son fixe). |
| 8 crête −12 dBTP « pas crédible » | banc | moyen | idem | La sinusoïde du banc a 0,9 dB entre crête et LUFS ; le vrai moteur sort −1,1 dBTP pour −8,15 LUFS. |
| 9 Club −7,5 (puce) / −8 (aide) | appli | moyen | `ExportTargetRound2Test.theHelpSaysTheChipsOwnValues` | L'aide de « Volume visé » donne les valeurs du projet, celles des puces (Techno : Auto −7,5, Club −7,5 ; House : −8), et porte le même nom que le panneau (« Volume visé », plus « Cible de volume »). |
| 9 Auto et Club indiscernables | appli | mineur | `autoAndClubAreToldApart` | La puce Auto nomme le style : « Auto (Techno) · −7,5 LUFS ». Quand le style est déjà au volume club, l'aide sous Club le dit : « ici, Club et Auto visent le même volume ». |
| 9 rapport club « comme le streaming (−14) » | appli | moyen | `aClubReportNeverSaysStreamingNorReadyAboveAGap`, `theCardShowsTheClubReport` | Avec une cible, la ligne « Niveau » se compare à elle : « à la cible (Club) », « moins fort / plus fort que la cible (Club) ». Sans cible, les mots d'avant. |
| 9 « ✓ Prêt » vert au-dessus d'un écart | appli | mineur | idem | Écart de plus de 0,5 LU : verdict orange « Prêt, mais moins fort que la cible (−5,4 LU) » (ou plus fort), jamais la coche verte. |
| 10 aucune progression | appli | mineur | `theExportSaysHowFarItIs` | Sous la barre : « Fabrication du son · 42 % » (ou l'étape : « Compression M4A · 80 % »), lue aussi par TalkBack sur la barre. |
| 10 M4A « 16 bits » | appli | mineur | `anM4aSaysItsBitRate`, `ExportCheckTest` | « Fichier final : 48 kHz · 256 kb/s » et rapport « M4A, 48 kHz, 256 kb/s ». |
| 10 dossier absent du rapport WAV | appli (racine trouvée) | moyen | `everyReportSaysWhereTheFileIs`, `OneTargetExportTest` (`savedInMusic`) | Deux causes. 1) La copie du rapport texte (« Rapport avec le fichier », activé par défaut) passait par la collection Audio de MediaStore, qui refuse `text/plain` (Robolectric le refuse aussi) : un seul échec effaçait la ligne « Sauvegardé dans Musique ». Le rapport va maintenant dans Documents › BenStudio, à part. 2) `audio/wav` refusé par MediaStore (sa table dit `audio/x-wav`) : on essaie d'abord le type de la plateforme pour l'extension. Chaque rapport dit où est le fichier, ou « Pas copié dans Musique › BenStudio : touche « Partager »… ». C'est un texte normal, lu par TalkBack. |
| 10 Partager sans cible | appli | mineur | `theShareSheetSaysTheTarget` | « Prêt à partager ? » (mode Pro) : « Volume visé : Club · −7,5 LUFS (se règle dans Mix) ». En Simple, rien (pas de Mix, pas de jargon). |
| 10 Partager présélectionne M4A, Mix WAV | pas un défaut | — | — | Le partage retient son propre format (M4A pour envoyer) ; la carte Exporter garde le sien. |

## Vérifications
- `scripts/host-tests.sh mp_club_and_streaming_exports_differ` : vert (fichiers mesurés −8,15 et −14,01 LUFS).
- Verts : `ExportTargetRound2Test` (8), `OneTargetExportTest` (club ≈ −8, streaming ≈ −14 sur le banc, WAV copié dans Musique), `ExportCheckTest`, `HelpCoverageTest`, `ExportResultInViewTest`, `ShareSheetTest`, `ShareReadyTextTest`, `BenchShareTest`, `BenchEngineTest`, `MasterProFreeTargetTest`, `WavExporterTest`, `AtomicExportTest`.
- Avant correction (preuve d'échec) : le banc donnait −12,9 LUFS aux deux exports ; une sonde Robolectric a montré `audio/wav` et `text/plain` refusés par MediaStore Audio (`savedInMusic` faux).
- Captures ré-enregistrées et regardées : `mixer_master` (« Auto (House) »), `mixer_export` (« Acide (2/4) · 40 % »), `export_report` (« M4A, 48 kHz, 256 kb/s »), `share_sheet` (« Volume visé : Club · −8 LUFS »).
- `I18nCatalogTest` : aucune erreur sur nos clés (14 chaînes en 14 langues dans `ultra1_export.json` ; 3 clés devenues inutiles retirées) ; les échecs restants viennent d'autres zones.

## Reste
- Sur téléphone, à confirmer : la copie dans Musique d'un WAV et du rapport dans Documents (MediaStore réel).
- Banc : la durée d'export reste 3 s (son fixe) ; à documenter dans le rapport du banc, pas un défaut de l'appli.
