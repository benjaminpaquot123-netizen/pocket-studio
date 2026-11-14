# Audit ultra, tour 3 : zone 2 (exports, partage, reprendre ailleurs), n° 10 à 14

Sièges A d, A c, C c, D c, F b, F d. Tests : `app/src/test/.../export/Round3Zone2Test.kt` (écrits avant les corrections : ne compilaient pas ou échouaient).

| n° | Défaut | Verdict | Gravité | Test | Ce qui change |
|---|---|---|---|---|---|
| 10 | Club/streaming loin de l'export, absent de « Prêt à partager ? » | appli | moyen ×3 | `theShareSheetChoosesTheTarget`, `theExportCardChoosesTheTarget` | `TargetPicker` : puces Streaming / Club / Auto (style) / Aucun (+ Apple ou Libre si réglé dans Mix) dans « Prêt à partager ? » (Pro) et dans la carte Exporter. Même réglage du projet que Mix (un seul endroit de vérité, « Le même réglage que dans Mix »). Défaut laissé « désactivé » (le changer changerait le son en direct : DECISIONS). |
| 10 | Aide « mesuré sur le Drop » sur un morceau sans Drop | appli | moyen | `theTargetsHelpNeverAssumesADrop` | « mesuré sur le passage le plus fort » (c'est ce que mesure le moteur : la partie la plus forte), partout : puces, aide « Volume visé » (« une cible par projet », plus « pour toute l'appli »), rapport d'export, ligne « crêtes tassées ». |
| 11 | Aucune liste de mes exports | appli | moyen | `myExportsAreListed` | « Mes exports » (MediaStore Musique › BenStudio, les fichiers de l'appli, plus récents d'abord, « Envoyer » sur chacun) depuis « Prêt à partager ? » et la carte Exporter. |
| 11 | « Sauvegardé dans… » disparaît après Annuler | appli | mineur | `aCancelledRunGivesBackTheLastResult` | Un export annulé rend le résultat d'avant (`withResultOf`). |
| 12 | Ancien export proposé après modification | appli | moyen | `aStaleExportIsNotOffered` | L'export garde l'empreinte du projet (`madeFrom`, sans le nom) ; s'il a changé : « Le morceau a changé depuis cet export : exporte à nouveau… », plus de bouton Partager ni de rapport périmé. |
| 12 | « Partager les 2 fichiers » n'en envoie qu'un | **banc** | — | `twoFilesLeaveTogether` (vert avant) | L'appli envoie ACTION_SEND_MULTIPLE avec les 2 fichiers ; `HumanTester.kt:971` et `BenchShareTest` ne lisent que `firstOrNull()` du partage. À corriger par l'équipe banc. |
| 12 | Synthé absent du piste par piste sans explication | appli (explication) | mineur | `aTrackWithoutAFileIsSaid` | La piste était vide (pas de mélodie). La carte dit « Sans fichier : Synthé (vide), Basse (coupée) » (`stemSkips`). |
| 12 | Remix : un seul fichier « remix » | appli | grave | `theRemixGivesOneFilePerStem` | 4 fichiers (`remix-voix`, `-batterie`, `-basse`, `-reste`) : la piste remix rendue avec une seule partie (`REMIX_STEM_BASE`) ; une partie coupée est sautée et dite. |
| 12 | « Prêt : aucune piste muette » voix du remix coupée | appli | grave | `aMutedRemixStemIsPointedOut` | Les parties du remix coupées / en solo entrent dans les vérifications (« Voix (Ma chanson) »), « Tout réentendre » / « Tout réactiver » les rendent. |
| 13 | Envoyer le projet caché, absent de l'aide | appli | moyen | `theProjectIsSentFromShareAndTheHelpSaysHow`, `theHelpSaysHowToGoOnElsewhere` | « Envoyer le projet » (.zip) dans « Prêt à partager ? » (sheet et page Envoyer du mode Simple) ; l'Aide explique comment reprendre ailleurs (Importer). |
| 13 | Le nom du partage renomme le projet à chaque export | appli | moyen | `aVersionsNameLeavesTheProjectsName` | Le nom tapé nomme le fichier et ses étiquettes (`fileTitle`) ; il ne renomme le projet que si celui-ci a un nom donné par l'appli (« Mon projet 3 »). Champ « Nom du morceau envoyé » + « Le projet garde son nom « … ». » |
| 13 | « (copie) » en français dans l'appli anglaise | appli | moyen | `aCopysNameIsShownInTheAppsLanguage` | Le champ part du nom affiché (`DefaultNames.shown`) : « Beat for Tom (copy) ». |
| 14 | Jargon LUFS/dBTP/kHz/kb/s après export en Simple | appli | moyen ×3 | `theSimpleReportHasNoJargon` | « C'est prêt » en Simple : durée, niveau en mots, « Pas de saturation » / « Trop fort par endroits… », « Fichier M4A : léger… ». La feuille de partage en Simple : « Niveau : … » sans chiffre ni « détails » LUFS, sans ligne « crêtes tassées ». |
| 14 | Durée différente Partager (8 mes., 0:16) / Mix (1 mesure) | appli | moyen | `oneLoopLengthEverywhere`, `ExportLengthTest` | La boucle par défaut de la carte Exporter = celle de « Partager » (`ShareSong.loopBars`, au moins 8 mesures) ; la longueur par défaut est proposée parmi les puces. |
| 14 | « Length: 3 s », chiffres identiques | banc | — | — | Banc (BenchEngine, équipe banc). |

## Vérifications
- Tests ciblés : voir le compte rendu final (Round3Zone2Test, ShareSheetTest, ExportTargetRound2Test, StemExportsTest, ExportLengthTest, ExportCardTest, ShareSongTest, ShareReadyTextTest, OneTargetExportTest, ExportCheckTest, HelpCoverageTest, SimpleModeAuditTest ; `:project-model:test --tests *I18nCatalogTest`).
- 27 chaînes nouvelles en 14 langues (`i18n/<langue>/ultra3z2.json`).

## Reste
- Le nom tapé ne nomme pas la vidéo 9:16 (elle garde le nom du projet).
- « Mes exports » ne voit que les fichiers copiés par l'appli dans Musique (pas les rapports .txt de Documents) ; à vérifier sur téléphone (MediaStore réel).
- « (copie 2) » n'est pas traduit (`DefaultNames` ne connaît que « (copie) ») ; mots « remix/streaming » dans les noms de fichiers coréens : zone 6.
- En mode Simple, pas de choix de volume visé (pas de jargon) ; la puce reste dans Pro.
- Captures ré-enregistrées et regardées : `share_sheet` (puces Volume visé), `share_sheet_ok` (« Mes exports ») ; `mixer_export`, `export_report` inchangées.
