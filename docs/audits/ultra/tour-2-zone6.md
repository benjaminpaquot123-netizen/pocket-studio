# Audit ultra, tour 2 — zone 6 : textes, langues, accessibilité (n° 28 à 31)

| N° | Verdict | Gravité | Test (rouge avant, vert après) | Commit |
|---|---|---|---|---|
| 28 | appli | mineur | `EverydayPadWordsTest` (project-model) | voir plus bas |
| 29 | appli | mineur | `KoreanRegisterTest` (project-model) | 5c133ee |
| 30 « Annuler » de nuit | appli | mineur | `ThumbOffReadableTest` (2,05:1 nuit / 2,53:1 Studio avant) | voir plus bas |
| 30 Remix / « Poser » | banc | — | `BenchContrastRemixLibraryTest` (vert : preuve) | voir plus bas |
| 31 | appli | moyen | `RemixModelAskTest`, `RemixThumbReachTest`, `HumanRemixJourneyTest` | voir plus bas |

## 28 — jargon et anglais en Mode simple
- Pads (kit et voix studio), familles de la bibliothèque, fenêtre du tempo (« 132 Tempo », TalkBack « battements par minute »),
  « Normaliser » / « Inverser » des prises : mots de tous les jours dans les 15 langues (`I18n.EVERYDAY`, nouvelles clés dans `ultra2z6.json`).
  Coréen : 큰북, 낮은 울림, 손뼉, 북 테두리, 최대 음량, 거꾸로… ; anglais : Bass drum, Handclap, Low rumble…
- Un nom de pad ou de bouton entre guillemets dans un texte (les tutos : « sur le pad « Rumble » ») dit le même mot que le pad ;
  en coréen la particule suit (`KoreanParticles`). Le Mode pro ne change pas.
- Reste (choix noté) : « Drop » et « Break » restent les noms des parties (expliqués en Simple sous chaque texte depuis le tour 1),
  « 808 » et le petit badge « FX » des pads.

## 29 — coréen (et japonais)
- Catalogue coréen relu en entier : 당신 (2), 반말 (« 네 차례 » ×3, consigne du mix simple, 8 boutons « …해 줘 »), 합쇼체 (≈ 20 lignes),
  « 나중에: 시작 비트 보기 » → « 건너뛰고 시작 비트 보기 ». Espace avant une particule après un symbole : 3 en coréen, 24 en japonais.
- Le test a aussi trouvé 3 lignes 합쇼체 dans le fichier neuf de la zone 5 (`ko/ultra2z5.json`, non commité) : corrigées sur place.

## 30 — contraste
- Réel : le libellé d'un bouton du pouce éteint (alpha 0,38 → 0,6, `THUMB_OFF_ALPHA`) ; même couleur nulle part ailleurs
  (les boutons Material éteints du reste de l'appli : exemptés par le WCAG, non touchés).
- Banc : les mêmes pages (Remix sans modèle, bibliothèque « Sons intégrés »), dans une fenêtre plein écran comme l'appli,
  thèmes Studio et Nuit, mesurées par le même `LayoutScan` une fois dessinées : aucune ligne. Le recensement des captures
  (`MySoundsScreenshotTest.remixHome`) n'en avait pas non plus. Les valeurs du banc (1,21 à 2,75:1, différentes pour trois
  « Poser » identiques) viennent d'un écran mesuré en cours de dessin.

## 31 — téléchargement du modèle
- « Choisir une chanson » sans modèle ouvre « D'abord le modèle de séparation » (taille, Wi-Fi ou forfait mobile,
  « Télécharger, puis choisir la chanson » / « Plus tard ») ; « Télécharger le modèle » ne demande que sur données mobiles.

## Captures
Ré-enregistrées (non commitées : elles montrent aussi le travail en cours des autres zones) : nav/simple_{rhythm,keys,song,send,more},
nav/launch_simple, nav/help_labels_simple, nav/guide_simple, acid_simple_{editor,clavier}, tuto_app_snare. Regardées : pads et tempo en français.
D'autres captures (Pro, guide) diffèrent aussi par le travail des autres zones : à ré-enregistrer une fois tout commité.
