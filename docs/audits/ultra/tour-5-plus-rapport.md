# Tour 5 — lot « Plus » (chantier 5) : rapport

Source : `PLAN-ACTION-AUDITS.md` étape 4 ligne 5 ; `docs/audits/AUDIT-PRISE-EN-MAIN.md` G15 et Q3 (réponse 3A) ; retours-benjamin 2026-11-08.

## 1. Le même « Plus » dans les deux modes (G15) — fait
- En Simple, le ⋮ du haut et « Plus » du pouce ouvrent la page Plus de Pro (plus de menu de 12 lignes nues).
- Liste Simple : Accueil, Mes projets, Live, Remix, Ma voix, Bibliothèque de sons, Ma bibliothèque, Aide, Réglages,
  Passer en mode Pro, Signaler un bug, **Recette**. « Mon premier morceau » et le lexique sont dans Aide.
- Sur téléphone, en Simple, les cartes sont des tuiles (3 par rangée) : en liste, la Recette, l'Aide et les Réglages
  tombaient sous le pli (4 gestes au lieu de 3). Tablette : deux volets, comme Pro.
- Retours : page › « ← Plus » ; liste › « ← Retour » (et le Retour du téléphone) vers l'écran d'origine.
- « Renommer le morceau » quitte le menu (appui long sur le nom, nom de la page Partager).

## 2. « Simple ou Pro ? » plus demandé au deuxième morceau (3A) — fait
- Tant qu'aucun mode n'a été choisi : la question reste. Ensuite, un nouveau morceau prend le dernier choix
  (Nouveau projet, Mon premier morceau, Remix, assistant du Guide).
- « J'apprends » demande encore : le mode y fixe le niveau du tuto (choix noté dans DECISIONS.md).
- L'assistant dit « Ce morceau sera en mode Simple. » avec « Changer » (rouvre « Simple ou Pro ? »).
- Textes : zone `plus`, 14 langues.

## Non touché
- T5-Fc-31 (non tranché) : comportement inchangé. Son test de banc oublie le dernier choix pour garder la route du
  siège ; sur le téléphone de Benjamin, l'essai § 8 n° 3 ne montre plus la question au 2e morceau (passer par
  « J'apprends » pour la voir).
- Hors périmètre de la ligne du plan : porte unique Simple ↔ Pro (G17), menu unique des parties (M7), icône ⋮/⋯ (M-1).

## Tests à rejouer
- Nouveaux : `*PlusSameInBothModesTest`, `*NavigationMapTest` (chemin « Recette »).
- Ajustés : `*ModeChoiceTest`, `*TutoTest`, `*Lot2TextsTest`, `*HomeAndPadsTest`, `*SpacesTest`, `*SimpleModeAuditTest`,
  `*RemixFindableTest`, `*BugReportFlowTest`, `*Tour5BloquantsTest`, `*HumanLucasBugsTest`, `*BenchRecetteTest`.
- Captures : `*NavScreenshotTest` (simple_more) et les captures de l'assistant (ligne du mode).
