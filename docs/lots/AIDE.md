# Lot « Aide à jour » : note de suivi (2026-11-03)

Branche `fx-help` (worktree `/home/user/fx-help`, depuis fx-int). Rien de poussé, pas d'APK. Question de Benjamin : « Tous les boutons avec ? en petit qui donnent des infos : est-ce bien à jour ? Il y a eu beaucoup de modifications. Et les traductions aussi. »

## 1. Les « ? » existants relus
Textes du Lexique (`app/help/Glossary.kt`) faux ou dépassés, corrigés (14 langues) :
- **Pads live** : parlait d'un seul rang de pads ; dit maintenant les 4 pages de 16 de Pad Live et les trois façons de jouer (coup, boucle, tenir).
- **Force (vélocité)** : ajoute les « Couches » du mode Pro.
- **Bibliothèque de sons** : « rangés par famille » → le navigateur unique (dossiers, Récents, Favoris ☆, Ma bibliothèque).
- **Instrument** : manquait « Instruments réels ».
- **Muet et solo** : « Muet / Solo » → les icônes d'aujourd'hui (haut-parleur barré, casque) au lieu de M/S.
- **Effet de piste** : « jusqu'à quatre » → huit, et « Joue dans » (certaines parties seulement).
- **Piste par piste** : avec ou sans les envois.
- **Remix** : la découpe en bouts et les cases Remix du Live.
- **Live** : « à la mesure suivante » → « au prochain départ » (« Lancer à »), la colonne Remix.
- **Enregistrer ma performance** : nouvelle version ou à la suite, « Versions ».
- **Volume visé** (texte sous les puces, pas un « ? » mais une promesse fausse) : « l'export y est exactement » (Auto, Club) → « l'export s'en approche autant qu'il peut sans écraser le son » (faux sur `enorme`, AUDIT-4).
Relus et justes, laissés : les 70 autres mots (Brique dit bien « Structure › Parties » : l'onglet s'appelle Parties, sa grille Briques).

## 2. Les « ? » ajoutés (un seul composant : `HelpDot` / `WithHelp`, même aspect)
`HelpDot(help, about = listOf(tags))` dit, en sémantique (`HelpAbout`), quels réglages il explique : c'est ce que lit le test de garde. Textes neufs dans `app/help/ScreenHelp.kt` (23 aides, 1 à 3 phrases, mots du Mode simple), pas dans le Lexique (ce ne sont pas des mots mais des groupes de réglages).
| Écran | « ? » ajoutés | Où |
|---|---|---|
| Pad Live | 4 | Pages et packs (à côté du pack, explique aussi la cible du Défi) ; Effets de scène (Filtre XY, Écho, Roulement et ses divisions, Montée, Chute, Bégaie) ; Enregistrer ce que je joue ; Plus (Tout couper, Briques, Effets, Fête, Partager) |
| Pad Live › Défi | 1 | titre du panneau |
| Pad Live › réglages du pad | 3 | Coup, boucle, tenir ; Un seul à la fois ; Attaque et fin |
| Tempo (feuille, Pad Live et Live) | 1 | Régler le tempo : taper en rythme, horloge MIDI |
| Live › grille | 2 | Briques et cases (coin vide au-dessus des lettres : lignes, cases, ■, colonne Remix) ; Lancer à et Cases |
| Live › case Remix | 1 | sous le titre du menu |
| Découper la chanson | 3 | Coupes et aimant ; Outils de coupe (Couper ici, Début, Fin, Enlever, Boucle) ; Envoyer les bouts |
| Master pro | 4 | Cible de volume ; Plafond de crête ; La chaîne du master (grave en mono, attaque, relâchement) ; Écouter A/B (même volume, référence) |
| Fichier final | 2 | Fichier final ; Avec ou sans les envois |
| Fin d'export | 1 | Mesures pro (LUFS, LRA, crête vraie, PLR) |
| Partager | 2 | Format (WAV, FLAC, M4A, mot du Lexique) ; Volume (LUFS, mot du Lexique) |
| Rythme › pad › Couches | 1 | Couches |
Total : 25 « ? » neufs, 10 textes corrigés.
Pour que le « ? » des pages tienne à côté du pack sans le couper (« F… » à 400 dp), le bouton « Défi » ne montre que sa cible sous 420 dp de large (avant : 350) ; TalkBack dit toujours « Défi », et le « ? » l'explique. À cause de « Moins » (« Aufnehmen, was ich spiele » en allemand), le « ? » d'« Enregistrer ce que je joue » se cache quand « Plus » est ouvert. Dans la découpe, les « ? » sont dans des rangées déjà hautes (titre, menus, « Tes bouts ») : l'écran garde sa hauteur à 780 dp (cibles de 48 dp des bouts tenues).

## 3. Traductions
53 textes (41 neufs dans `restes.json`, 12 changés dans leur fichier d'origine `tools.json`, `restes.json`, `master.json`) × 14 langues, guillemets de chaque langue. `I18nCatalogTest` vert (placeholders, aucune traduction orpheline). Captures regardées : Pad Live (accueil, Plus, réglages du pad, Défi), feuille du tempo, grille du Live et menu de la case Remix (360 dp), découpe, master pro et fichier final (360 dp, français et allemand) : rien de coupé, rien qui chevauche.

## 4. Test de garde
`app/src/test/java/com/pocketstudio/app/help/HelpCoverageTest.kt` : rend chaque écran (Pad Live avec « Plus », ses réglages de pad, le Défi, la feuille du tempo, la grille du Live, le menu de la case Remix, la découpe, le master pro, le fichier final, les pistes, les mesures pro, Partager, Couches), relève **tous** les réglages (nœud avec un tag qu'on peut toucher, cocher ou glisser) et échoue si l'un n'a ni « ? » qui le nomme ni place dans la liste des évidents (retour, ▶, fermer, une valeur). Un réglage qui arrive sans son « ? » casse le test. Il vérifie aussi que chaque aide neuve est courte, que deux aides n'ont pas le même mot, et que chacune est affichée quelque part.
`docs/audits/CARTE-FONCTIONS.md` : la colonne « Aide » de Pad Live, Cases du Live, Découpe du Remix, Cases Remix du Live, Tempo, Effets, Batterie, Export et Master pro pointe vers `HelpCoverageTest`. Cases VIDE : 40, inchangé (aucune n'était dans la colonne Aide) ; borne de `CarteFonctionsTest` laissée à 40.

## 5. Décision 1A (Club qui n'atteint pas sa cible)
Fait : après un export du mix, quand le volume reste sous la cible de plus de 0,5 LU **parce que** la chaîne a atteint son plafond de tassement (6 LU sur le morceau, 3 sur le Drop), la carte d'export l'écrit en orange : « −10,6 LUFS au lieu de −9 : plus fort, le son s'écraserait. », puis « Le master s'arrête avant d'écraser les coups : le morceau garde son punch, un peu moins fort que la cible. » (`export/TargetReach.kt`, `TargetReachTest` sur les chiffres d'`enorme`). Le niveau n'est jamais forcé. Le « ? » de la Cible de volume le dit aussi.

**Pas fait : le bouton « Couper les infra-basses ».** Mesuré sur `enorme` par le vrai moteur (`ps_render --whole-song --target -9`, construit depuis cette branche), un passe-haut posé sur le master avant le limiteur :
| Coupe | Volume atteint | Tassement | Infra < 30 Hz |
|---|---|---|---|
| aucune | −10,57 LUFS | 6,12 dB | −11,3 dB |
| 25 Hz (12 dB/oct) | −10,63 | 6,11 | |
| 30 Hz | −10,66 | 6,10 | |
| 35 Hz | −10,71 | 6,09 | −15,9 dB |
| 40 Hz | −10,73 | 6,10 | |
| 30 Hz deux fois (24 dB/oct) | −10,88 | 6,07 | |
| 40 Hz deux fois | −10,74 | 6,09 | |
Les infra-basses baissent bien (−4,6 dB) mais le volume atteint ne monte pas (il baisse même de 0,1 à 0,3 LU) : ce qui coûte au limiteur, ce sont les crêtes de la bande 40–60 Hz (sub et kick), pas l'infra. Un bouton qui promettrait « plus fort » mentirait : je ne l'ai pas mis (le code du passe-haut au master a été écrit puis retiré).

### Question pour Benjamin (lot suivant)
1. Pour aller plus fort sur `enorme` en Club :
   A (recommandé) : laisser tel quel, le message suffit ; conséquence : rien ne change au son, le texte dit vrai.
   B : proposer « Baisser un peu le sub et le kick » (−2 dB sur les pistes de basse et le kick, annulable) ; conséquence : à mesurer avant, change l'équilibre du mix.
   C : un compresseur multibande sur le grave au master Club ; conséquence : plus fort, mais le grave pompe un peu.

## Décisions à recopier dans DECISIONS.md
- 2026-11-03 · Aides des nouveaux écrans : un « ? » par groupe de réglages (pas un par bouton) là où il y a la place à 360 dp, textes dans `ScreenHelp` (hors Lexique) ; le `HelpDot` nomme les réglages qu'il explique et `HelpCoverageTest` refuse un réglage sans « ? » sur ces écrans. Retour arrière : enlever les `HelpDot` et le test.
- 2026-11-03 · Décision 1A : message « −10,6 LUFS au lieu de −9 : plus fort, le son s'écraserait » sous l'export ; le bouton « Couper les infra-basses » n'est pas mis : mesuré sur `enorme`, un passe-haut de 25 à 40 Hz au master ne rend aucun volume (−10,57 → −10,66 LUFS à 30 Hz). Retour arrière : retirer `TargetReach`.
- 2026-11-03 · « Volume visé » Auto et Club : « l'export y est exactement » → « l'export s'en approche autant qu'il peut sans écraser le son ». Retour arrière : l'ancien texte.

## Pour REVIEW.md
- Restes : la feuille Partager (Mode simple) ne dit pas encore quand la cible n'est pas atteinte (elle montre seulement « les crêtes sont tassées de 3 dB : c'est beaucoup »). La barre de Pad Live (titre, tempo, REC) n'a pas de « ? » (pas la place à 360 dp) : le tempo est expliqué dans sa feuille, REC dans « Enregistrer ce que je joue ».
