# Tour 5 — lot « accueil » (chantier 6) : rapport

Source : `PLAN-ACTION-AUDITS.md` étape 4 ligne 6 ; `docs/audits/AUDIT-PRISE-EN-MAIN.md` G9 et G11 ; promesses P009/P046
(« un style en une touche », non tenue T1 à T4) ; retour de Benjamin du 2026-11-06 (carte coupée sans signe).

## 1. Les accueils montrent qu'ils défilent (G9) — fait
- Composant partagé `ScrollHintBox` (ui-components) : fondu de 24 dp en bas tant qu'il reste du contenu, et puce
  « ↓ 4 autres » (cartes pas entières à l'écran) dans le coin bas droit tant que la liste est en haut. Un toucher amène
  la suivante (sans animation si « animations coupées ») ; défilée, le fondu seul reste ; en bas de liste, rien.
- Posé sur Bienvenue, « Que veux-tu faire ? » et Nouveau projet. Tablette couchée : tout tient, rien n'est dit.
- `LayoutScan.unhintedScrolls` : une liste qui déborde sans puce ni fondu. Lu sur les trois accueils seulement.

## 2. « Un style en une touche » tient sa promesse (G11) — fait
- Nouveau projet : une carte « Un style en une touche » (3e, visible sans défiler sur 360 × 780) avec Techno, House,
  Trap, Pop, Lo-fi hip-hop. Un toucher = le morceau, qui s'ouvre et joue (« prêt à écouter »).
- Depuis l'accueil : 2 touchers (3 la toute première fois, « Simple ou Pro ? » une fois, réponse 3A).
- « Autres façons de commencer » devient un repli qui dit ce qu'il cache (règle 13).
- Défaut trouvé en route : après un morceau créé depuis Nouveau projet, « Nouveau morceau » rouvrait « Mes projets »
  (état gardé de la page revenu pour l'ancien projet). Corrigé.
## Non fait
- M1 (« Conseillé pour débuter », démo sans question) : hors de la ligne du plan, contredit la décision du 2026-10-29.
- La règle 17 sur les autres écrans qui défilent (feuilles, pages) : `unhintedScrolls` est prêt, chantier à part.
- Corrigé au passage : catalogue de textes laissé rouge par le lot Plus (clé « Renommer le morceau », mot arabe du Guide).

## Tests à rejouer
- Nouveaux : `*HomeScreensSayTheyScrollTest`, `*OneTouchStyleTest`, `*OneTouchSongPlaysTest`.
- Touchés par le lot : `*ScreenScanTest` (recensement projectsNew 4 → 9), `*OnboardingUiTest`, `*ModeChoiceTest`, `*QuickStyleCardTest`, `*I18nCatalogTest`, `*LexiconWordsTest`.
- Captures (45 réenregistrées, regardées) : `*ScreenshotTest`, `*Nav*ScreenshotTest`, `*I18n*ScreenshotTest`,
  `*Tablet*ScreenshotTest`, `*UiScale*ScreenshotTest`, `*Guide*ScreenshotTest`, `*BrandTest`, `*QuickStyleCardTest`.
  Non réenregistrées (pas de ce lot) : `simple_mix_muet` (léger écart déjà là) et les cinq captures interdites.
