# Tour 5 : synthèse des défauts (méthode v2)

Sources : 24 rapports `sieges/tour-5/lot{A..F}/{a..d}/rapport.md`, `mesures.json`, `clusters-complet.md`, `promesses.tsv`, `incidents-banc.tsv`, `tableau-de-bord.md`, rapports du tour 4, `git log`. Notation : « B c #23 » = lot B, siège c, geste n° 23. [T4 …] = déjà vu au tour 4 (siège et ligne vérifiés dans les rapports du tour 4).

## 1. En bref
- 24 sièges joués, 0 coupé par le banc, 0 plantage ; 20 missions sur 24 réussies (partielles : B a, B c, C a, D b).
- 190 fiches (7,9 par siège, contre 9,3 au tour 4) ; 3 bloquants, 5 graves, le reste moyen ou mineur.
- Bloquants : en Simple, impossible de changer de partie (A a #27) ; une mélodie perdue après un passage par Pad Live (E a #117) ; étape Swing du tuto Pro introuvable (B c #23 ; « bloquant » non vérifié : le siège n'a fait défiler « Sounds » qu'une fois, voir Contre-vérification).
- Graves : gaucher d'une main gêné 3 fois, contourné en 8, 11 et 5 gestes (F d #2, #34, #51 ; #34 discutable, même écran que #2) ; pas d'étoile ☆ sur les sons (D b #39) ; « Mes exports » inatteignable en Simple (C d #50).
- Attention : entre le tour 4 et le tour 5, l'appli n'a changé que sur un point visible (la prise au clavier). Le reste qui « tient » maintenant tient sans doute grâce au banc réparé ou à un autre chemin, pas à une correction.
- Une quarantaine de défauts du tour 5 étaient déjà au tour 4 (marqués [T4]) ; la fusion par programme n'en a vu aucun (sa base ne connaît que le tour 0).
- La fusion par programme a trouvé 5 groupes de plusieurs sièges (3 justes, 1 faux, 1 lâche) ; en lisant, j'en trouve 25 (24 en §3, 1 en §5).
- Promesses : 90 lignes, 38 tenues, 45 non tenues, 7 pas vues. Pire zone : langues (0 sur 8).

## 2. Ce qui tient maintenant
Rappel : seul le commit `d3eccff` change l'appli de façon visible entre les deux tours (après une prise au clavier, l'écran montre la partie qui a reçu les notes). `350079e` touche aussi 3 fichiers de l'appli (`GuideListen.kt`, `SeparationService.kt`, `RhythmViewModel.kt`), mais seulement pour le faux moteur du banc. Les autres lignes ne sont pas des corrections : soit le défaut du tour 4 venait du banc, soit le siège n'a pas refait le même chemin. À confirmer par un rejeu avant de les rayer.
- **Prise au clavier gardée** (correction réelle) : A a écran 40 (« Ma mélodie » garde 3 notes dans la partie regardée), E a écrans 20 et 141 (Retour pendant la prise, notes gardées). [T4 : « Rien n'a été joué » chez A b, D c, D d.] Limite : E a #117 (bloquant ci-dessous).
- **Notifications après la prise** : A a écran 35. [T4 D d : fenêtre au milieu de la prise.]
- **Renommer ne fige plus** : A c écrans 8-12, 24-28, 50-54 (champ, ✕ Effacer, taper, OK, chaque fois tout de suite). [T4 C a #63, E a #7 : 5 min figé.] Le banc a été réparé (`d3eccff` : « Effacer répond en 11 s, pas 240 s »), l'appli non : le gel du tour 4 venait très probablement du banc, mais `verdicts.tsv` (T4-19) le garde « non tranché » tant que le téléphone de Benjamin n'a pas confirmé.
- **Mes projets liste le projet** : A c écran 67 (nom changé visible), D c écran 62 (« Salvato proprio ora » listé). [T4 D b #33 : seul « Trap » listé.] Pas partout : A d #21 (voir Pad Live).
- **Piano roll** : une note posée s'allonge en tirant son bord, et sonne quand on la pose (D a écran 6) ; « Chords » et « Scale » restent dans la gamme (D a écran 60). [T4 B b : « impossible de changer la durée », accord posé sur C#.]
- **Vue Live** : « ■ Tout » arrête tous les blocs pendant une prise, « ■ batterie » sa seule piste (A d écrans 27-28) ; « lancer la ligne 1 à la mesure » part même sans boucle (A d écran 3, E d écran 4). [T4 B c #12, #18.] Effets, tempo et REC visibles sans « Gros pads » (E d écran 1). [T4 B c #24.]
- **Défi** : pads et cadre remplis seuls après quelques secondes (C a écrans 2-3, 39). [T4 A a : pads vides « + ».] Doute : le banc v2 fabrique lui-même les pads prêts quand l'appli n'y arrive pas (`73e50d0`) ; je ne sais pas si c'est le cas ici.
- **Remix** : « Cancelar » n'ajoute rien au projet (C b écrans 15 et 63). [T4 A c #21.] Découpe « par phrases » en arabe : toucher le numéro coche, l'envoi s'allume, « Un bout » revient (D d écran 35), miroir droite-à-gauche juste (D d écran 34). [Rejeu T3 B c, jamais confirmé avant.]
- **Tuto Pro « je vérifie »** : après la démo remise à zéro rien n'est validé ; 12 frappes en trop sont signalées (B c écrans 9, 49, 50). [T4 B d #12, #15.] Sauf la force des frappes (voir Tutos).
- **Exports** : l'aide ne cite que des cibles qui existent (C c écran 45, « Apple Music » et « Free » présents) [T4 E b #18] ; « piste par piste » d'un remix donne 4 fichiers (E c écran 52) ; nom coréen intact et projet non renommé (E c écrans 20, 25).
- **Autres promesses tenues** : Retour après suppression du projet ouvert mène à l'accueil, jamais d'écran vide en 25 Retour comptés par `mesures.json` (le siège dit ~40) (E a écrans 27, 55, 103, 149) ; style en une touche puis partage au geste 11 selon le siège, 9 gestes selon `mesures.json` (F a écrans 6, 12) ; effet limité à certaines parties et écoute avec/sans master (F b écrans 41, 52) ; pastille Simple/Pro et tout en Simple (F c écrans 2, 14) ; un bout de voix gardé puis posé sur une piste (F d écrans 67-68) ; ▶ depuis une partie (A b écran 21) ; une étape du Guide ne se coche pas sans geste (B b écran 11).

## 3. Défauts par zone
Gravité : **bloquant** = mission impossible ; **grave** = contourné en plus de 3 gestes et revu ; **moyen** = contourné ; **mineur** = finition. « ? » = fiche marquée `banc?` (temps, son, clavier, moteur, rythme) : ne compte ni grave ni banc tant qu'elle n'a pas ses preuves (§12.3). « ×N » = vu par N sièges ; (fusion) = groupe trouvé aussi par le programme, (sens) = groupe que j'ai fait en lisant.

### Mode simple & parties (projets, lecture, navigation)
- **BLOQUANT** ×1 — A a #27 (revu écran 50, aussi #49, #53) : en Simple, choisir la partie C, B ou une nouvelle partie dans le menu ne change rien, l'éditeur reste sur A. Impossible de mettre sa mélodie dans le refrain.
- moyen ×2 (fusion) — A c #12, E a #105 : Retour depuis Mes projets (ouvert par le nom du projet) ramène à l'accueil, pas au morceau. [T4 C a, ligne 44.]
- moyen ×1 — A c #18 (revu) : pas de « Renommer » dans les réglages de la piste ; il est caché dans l'onglet Clavier.
- moyen ×1 — D c #34 (revu) : « Duplica » ajoute une copie liée sans prévenir, avec le message d'une action d'avant.
- moyen ×1 — D c #51 (revu) : « Voir tout le morceau » s'arrête vers la mesure 85 sur 90. [T4 D b, ligne 21.]
- moyen ×1 — D a #30 (revu) : « Continuer » rouvre sur la batterie, pas sur la piste et la vue où l'on était.
- moyen ×1 — F a #20 : la question Simple ou Pro revient à chaque nouveau morceau. [T4 D d, ligne 15.]
- moyen ×2 (sens) — E b #24 (contourné en 3) : le « + » d'une partie n'ajoute pas de partie ; F c écran 10 cherche aussi le bouton « ajouter ».
- moyen ×1 — F c #15 (revu) : « Écouter la démo » disparaît de l'accueil après le premier projet.
- mineur ×3 (+1 incident) (sens), ? — compteur du haut figé pendant la lecture : A b #14 (revu, banc? non), F a #5, F c #8 (banc? son), incident F b #63. [T4 A d, ligne 21 ; D b, ligne 18.]
- mineur ×3 (sens) — liste des parties déroutante : trois lignes « C · Refrain » marquées « Mix » (B d #18, F c #9, revus), « 19 parties » contre 6 et ordre E, F, G avant A (F a #8, #26). [T4 A d, ligne 39.]
- mineur ×2 (sens) — une nouvelle partie s'appelle seulement « M » : A a #51, F c #12.
- mineur ×2 (fusion) — « copie de A » écrit à l'envers : D c #32, F b #30 (revu).
- mineur ×3 (sens) — un message d'avant reste affiché : D c #34, A d #58, F d #83.
- mineur ×1 — E a #77 (carte « Continuer » dit Pro), D a #72 (deux Retour), F c #7 et #10 (gamme et nombre de lectures qui se contredisent).

### Clavier, mélodie (et voix)
- **BLOQUANT** ×1 — E a #117 (revu écran 149) : Retour pendant « Enregistrer ma mélodie », passage par Pad Live, puis réouverture du projet : la prise de 4 notes a disparu, piste vide. Sans Pad Live (écrans 20, 141), la prise est gardée. C'est une perte de travail.
- moyen ×1 — A a #30 : « Ma mélodie » prend le son FM piano, pas le Marimba choisi. [T4 D d, ligne 18 : « FM bell ».]
- moyen ×1 — A b #31 (revu) : toucher une prise de voix lance une boucle d'une mesure au lieu du morceau. [T4 A d, lignes 15 et 33.]
- mineur ×2 (sens) — noms de notes qui ne collent pas : A a #39 (« E♭4 » dans la grille, « D# 4 » au clavier), E a #12 (revu, « Octave 3 » sur des touches Do 4 à Si 5). La fusion a groupé E a #12 avec B d #33 (« Loop »), à tort.
- mineur ×1 — A b #5 (revu) : l'aide de la page voix parle d'une grille absente [T4 A d, ligne 27] ; A b #20, #26 : étiquettes qui ne suivent pas ; A b #14 et #27, ? (temps, son) : fin de morceau et fin de prise.

### Piano roll & synthé (et Mix)
- **GRAVE** ×1 — D b #39 (revu écran 4) : le texte dit « touche la ☆ d'un son », aucune ☆ sur les tuiles. [T4 E d, ligne 15.]
- moyen ×1 — D b #34, #35, #52 : garder un son sous le même nom est refusé (pas de « mettre à jour »), le nom tapé est perdu, le champ propose « Mon son 1 ». [T4 E d, ligne 12.]
- moyen ×1 — D b #7 (revu) : transport en double, l'éditeur de synthé est à l'étroit sur le pliable. [T4 E d, ligne 21 ; D b, ligne 36.] D b #56 : la puce garde l'ancien nom [T4 E d, ligne 18].
- moyen ×1 — D a #6 (revu) : en « Draw », glisser ne trace rien. [T4 B b, ligne 16.]
- moyen ×1 — D a #40, #43 (revus) : le collage se pose sur des notes et en efface une sans rien dire. [T4 B b, ligne 20.]
- moyen ×1 — D a #78 (revu) : « Écris-moi une mélodie » change la gamme de do majeur à mi majeur sans le dire.
- moyen ×1, ? (moteur) — F b #13 : la courbe d'automatisation se dessine mais le réglage ne bouge pas pendant la lecture ; F b #73 : choisir la partie B pendant la lecture ne marche pas.
- mineur ×1 — D a #26 (pas de choix de tonalité) [T4 B b, ligne 32], D a #3 (grille petite) [T4 B b, ligne 24], D a #9 (16 mesures = 15 touchers), D a #38, F b #40.

### Pad Live & Live
- moyen ×2 (sens) — A d #10, E d #11 (revus) : « Tout couper » coupe les pads mais la musique du projet continue. [T4 A c, ligne 27.] La fusion ne les a pas rapprochés (japonais et français).
- moyen ×2 (sens) — E d #10 (revu), A d écran 6 : « Tout couper » caché dans « Plus », libellé coupé « Tout cou… ».
- moyen ×1 — A d #16, #31 (revus) : « Annuler » efface la prise de la performance sans question. [T4 B c, ligne 15.]
- moyen ×2 (sens) — A d #49 (revu), E d #32 : l'enregistrement « prêt, part à la mesure » ne part pas en tapant un pad, rien ne dit d'appuyer sur Jouer.
- moyen ×1 — E d #17 (revu) : toucher le haut d'un pad ouvre sa fiche au lieu de le jouer (rejeu T1-07).
- moyen ×1 — A d #33 (revu) : au REC tout descend, le toucher tombe dans le vide. E d #45 (contourné en 4, non revu) : couché, le pavé du tempo est coupé.
- mineur ×1 — A d #21 : le projet annoncé « attend dans Mes projets » n'y est pas. [T4 B c, ligne 30, identique.]
- mineur ×2 (sens) — bouton du pack sans nom ou coupé : A d #0, E d #54. [T4 B c, ligne 39.]
- mineur ×1, ? (son) — A d #43 : un pad sonne tout seul en rouvrant Pad Live. [T4 A a, ligne 36.] E d #8, #39 : textes rognés.

### Remix & découpe
- moyen ×1 — C b #32 (+ #67, ? son) : la voix envoyée au Live est dans une colonne coupée d'office, la case dit « suena ». [T4 A c, ligne 12.]
- moyen ×1 — C b #35 (revu) : le mode « Frases » est perdu au retour. [T4 A c, ligne 18.]
- moyen ×3 (fusion pour 2, sens pour D d) — C b #22, F d #58 (revu), D d #19 (contourné en 5, non revu) : les outils et la liste des bouts sont cachés sous la forme d'onde ou le panneau d'envoi. [T4 A c, ligne 30.]
- moyen ×1 — D d #26 (revu) : toucher le bloc d'une phrase ne la coche pas ; seul le petit rond numéroté coche.
- moyen ×1 — D d #39 (revu) : depuis Mon morceau, rien ne ramène au Remix en cours.
- moyen ×1 — F d #55 : « Annuler » défait tout l'import de la chanson, pas le dernier réglage ; F d #52 : « Fin ici » fait aussi sauter le début.
- mineur ×1 — C b #43 : renvoyer le même bout crée « Voz 3 » sans prévenir [T4 A c, ligne 24] ; C b #30, #31 ; D d #39 (« ta chanson est vide » juste après avoir posé 2 bouts), D d #4 (le Guide parle de rythme pendant le remix).

### Exports & partage
- **GRAVE** ×1 — C d #50 (revu écran 102) : en Simple, l'onglet Envoyer ne défile pas, « Mes exports » est inatteignable ; il faut passer en Pro.
- moyen ×2 (sens) — C c #41, C d #68 (revus) : « Mes exports » ne liste jamais les .zip du projet. [T4 C a, ligne 24.]
- mineur ×2 (sens) — C c #30, E c #61 (revu) (deux fiches « finition ») : alerte « le morceau a changé » juste après un export, sans aucun changement. [T4 E b, ligne 12.]
- moyen ×1 — C c #20 (revu) : Annuler juste après l'export Club remet la cible sur Off. [T4 E b, ligne 12.]
- moyen ×1 — C c #27 (revu) : « Prêt à partager ? » exporte toujours 8 mesures, quoi que dise Mix. [T4 E b, ligne 15.]
- moyen ×2 (sens) — C d #49 (contourné en 4), F a #8 (revu) : la carte « Envoyer le projet » est cachée ou coupée.
- moyen ×1, ? (son) — E c #28 : remix placé en mesure 2, la fin est coupée et « Prêt » ne le dit pas. C d #83, ? (temps) : liste vide une fois.
- mineur ×2 (sens), ? (son) — A a #43, F a #11 : messages de niveau qui se contredisent. [T4 D d, ligne 30 ; D c, ligne 35.]
- mineur ×1 — C d #46 (revu) : « ne gère pas le MIDI » juste au-dessus de « reconnu tout seul » [T4 C a, ligne 36] ; C d #69 : « Partager · 2 » au lieu du nom [T4 E b, ligne 33] ; C c #41 : 1 KB et aucune marque de version [T4 E b, ligne 24] ; C c #23 ; C d #31.

### Tutos & Guide
- **BLOQUANT** ×1 — B c #23 (revu écran 76) : tuto Pro en allemand, chapitre 1 : « Ich mach's » montre la grille de pas, le réglage Swing reste introuvable, chapitre impossible à finir. Doute : au tour 4, B d l'avait trouvé dans « Sounds », deux défilements plus bas (ligne 21) ; c'est peut-être « trop caché » plutôt qu'« absent ». Contre-vérification : ses deux glissés dans Sounds (#28, #40) n'ont descendu que d'un cran (réglages du pad, Panorama) ; aucun écran ne montre le bas de Sounds. Gravité « bloquant » non vérifiée.
- moyen ×2 (sens) — le tuto ou l'aide cite ce qui n'existe pas : B a #43 (pad « Perc », revu), B b #37 (« Struktura › Części » en Simple), B b #50 (« Pad Live, 4 pages »), B b #13 (le lien « Son de la mélodie » ouvre « Synth » et change la mauvaise piste). [T4-30, cinq sièges.]
- moyen ×3 (sens) — le panneau du tuto ou le Guide cache le contenu : B a #72 (contourné en 5, non revu), #83 (revu), #89 à 200 % ; F a #6 (revu) ; A a #25.
- moyen ×1 — B a #80 (revu) : la grille s'ouvre à la mesure 4 au lieu de la 1.
- mineur ×1 — B b #41, #11 (revus) : étiquettes « ? » empilées loin de leurs boutons. [T4 C c, ligne 13.]
- mineur ×1 — B c #66 : coup fantôme laissé à 100 % et pourtant « Bravo » [T4 B d, ligne 15] ; B c #77 : la 303 reste en do [T4 B d, ligne 12] ; B c #21, #26 ; B a #31, #96 ; B b #30, #54.

### Langues & lisibilité
- mineur ×4 (sens) — mots anglais et jargon en Simple : A a #9 et promesse Aa-4 (SNARE, Kick, Octave, Crash, Solo), B d #2, #13, #33 (Sub bass, Loop, Bridge, Groove), D d (Loop, Transients, REC), F c promesse (Mix, Fill). Chaque fiche est une finition, mais la promesse « mots de tous les jours » est cassée chez les 4 sièges qui la vérifiaient. [T4-42, six sièges.] La fusion a fait un groupe lâche (A a #9, #20, B d #13, #17) : même écran de l'assistant, défauts différents.
- moyen ×1 — B d #24 (revu) : en arabe, « Rétablir » prend la place exacte d'« Annuler » ; le 2e appui rétablit. [T4 D c, ligne 23.]
- mineur ×1 — B d #17, #27, #37 : flèches →, signe moins après le chiffre, grille dans l'autre sens. [T4 D c, lignes 17 et 20.]
- mineur ×1 — B c #16 (revu), #78 : notation anglaise (B♭, B) au lieu de l'allemande (B, H). [T4 B d, ligne 27.]
- moyen ×1 — F b #1 (revu) : à 200 %, la grande carte « Continuer » est vide.
- mineur ×2 (sens) — nom du projet coupé en haut : A a #21 (« L… »), A c #23. [T4 A b, ligne 45.]
- mineur ×2 (sens) — première lettre rognée au bord gauche : E d #39, F c #5. [T4 B d, ligne 36.]
- mineur ×1 — textes coupés à 130 et 200 % : B a #4 [T4 A b, ligne 45], F a #6, #26, F b #2, #6, #32, #46, D c #51 ; D c #38 (« Refrain » en italien) [T4 D b, ligne 12].

### Accessibilité & une main
- **GRAVE** ×1 (3 fiches, même siège) — F d gaucher, pouce gauche : #2 (Simple et Pro tout en haut, 8 gestes, revu), #34 (« Dans un nouveau projet » retombe sur ce même choix, 11 gestes ; son « revu » renvoie à l'écran 10, celui de #2, vu avant : même cause que #2, grave discutable), #51 (poignées de découpe hors du pouce, 5 gestes, revu). Plus en moyen : #33 (11 gestes), #74 (menu « Plus » en haut à droite, 10 gestes), #37, non revus.
- moyen ×4 (sens) — glisser pour faire défiler déclenche un réglage : A c #39 (pad d'effet), E d #40 (effet, ? son) et #46 (touche 5 du tempo), F b #44 (courbe redessinée), D b #48 (la liste remonte).
- moyen ×1 — C a #5 (revu) : le gros bouton jaune après l'exemple termine l'essai sans jouer [T4 A a, ligne 27] ; C a #14, ? (temps) : au niveau 2 on ne voit pas quel pad taper [T4 A a, ligne 18].
- mineur ×4 (sens) — boutons sans nom : E b #4 (▶ du style), #25 (⋮ d'une partie), C b #28 et D d #27 (rond numéroté de la découpe), E d #54.
- mineur ×1 — E b #3 (styles en gris très pâle, contraste 1,7 à 2,2), #7, #13 (listes en texte seul), #34 ; A c #23 (deux ✕ collées).

## 4. Promesses
| Siège | Zone | Promesses (id : tenue) |
|---|---|---|
| A a | Clavier ; Langues | Aa-1 oui · Aa-2 oui · Aa-3 non · Aa-4 non |
| A b | Clavier (voix) | Ab-1 oui · Ab-2 non · Ab-3 non |
| A c | Mode simple | Ac-1 oui · Ac-2 oui · Ac-3 oui · accueil-mes-projets oui |
| A d | Pad Live | Ad-1 oui · Ad-2 oui · Ad-3 non · Ad-4 non |
| B a | Tutos | Ba-1 non · Ba-2 non · Ba-3 pas vue |
| B b | Tutos | Bb-1 non · Bb-2 oui · Bb-3 non · Bb-4 pas vue |
| B c | Tutos ; Langues | Bc-1 oui · Bc-2 oui · Bc-3 non · ecran-pruefe non |
| B d | Langues | Bd-1 non · Bd-2 non · Bd-3 non |
| C a | Accessibilité | Ca-1 oui · Ca-2 non · Ca-3 pas vue · Ca-4 pas vue |
| C b | Remix | Cb-1 non · Cb-2 oui · Cb-3 non · Cb-4 non |
| C c | Exports | Cc-1 non · Cc-2 non · Cc-3 non · Cc-4 oui |
| C d | Exports | Cd-1 oui · Cd-2 non · Cd-3 non · « - » oui |
| D a | Piano roll | Da-1 ×5 : oui (allonger) · non (Draw) · non (coller) · oui (Scale/Chords) · non (16 mesures) |
| D b | Synthé | Db-1 non · Db-2 non · Db-3 non · Db-4 non |
| D c | Mode simple | Dc-1 oui · Dc-2 non · Dc-3 non · Dc-4 non |
| D d | Remix | Dd-1 oui · Dd-2 non · Dd-3 oui |
| E a | Mode simple ; Clavier | Ea-1 oui · Ea-2 oui (écran 20) · Ea-2 non (écran 125) · Ea-3 oui |
| E b | Accessibilité | Eb-1 non · Eb-2 oui |
| E c | Exports | Ec-1 oui · Ec-2 non · Ec-3 pas vue · Ec-4 oui · Ec-5 oui |
| E d | Pad Live | Ed-1 non · Ed-2 oui · Ed-3 non · Ed-4 oui |
| F a | Mode simple ; Exports ; Langues | Fa-1 oui · Fa-2 oui · Fa-3 non |
| F b | Mix ; Langues | Fb-1 non · Fb-2 oui · Fb-3 oui · Fb-4 non |
| F c | Mode simple ; Langues | Fc-1 pas vue · Fc-2 oui · Fc-3 oui · Fc-ecran-1 non |
| F d | Une main ; Remix | Fd-1 non · Fd-2 pas vue · Fd-3 oui |

| Zone | Oui | Non | Pas vue | Total |
|---|---|---|---|---|
| Mode simple & parties | 10 | 3 | 1 | 14 |
| Clavier, mélodie, voix | 4 | 4 | 0 | 8 |
| Piano roll, synthé, Mix | 4 | 8 | 0 | 12 |
| Pad Live & Live | 4 | 4 | 0 | 8 |
| Remix & découpe | 4 | 4 | 1 | 9 |
| Exports & partage | 7 | 6 | 1 | 14 |
| Tutos & Guide | 3 | 5 | 2 | 10 |
| Langues & lisibilité | 0 | 8 | 0 | 8 |
| Accessibilité & une main | 2 | 3 | 2 | 7 |
| **Total** | **38** | **45** | **7** | **90** |

Le classement par zone est le mien (une promesse par zone). Défauts de saisie des ids : le brief de D a ne donnait qu'un id (t5-Da-1) pour 5 points, que le siège a répété 5 fois, E a a utilisé Ea-2 deux fois, et quatre ids sortent du modèle (`accueil-mes-projets`, `ecran-pruefe`, `Fc-ecran-1`, « - » chez C d). Presque aucune promesse n'a été donnée à deux sièges : le tableau de bord ne peut compter que 2 promesses vues par au moins 2 sièges (0 tenue).

## 5. Incidents du banc
**Contamination entre sièges E a et E c.** Le raccourci de commande de E a, rangé dans un fichier partagé du dossier de travail, a été réécrit par le testeur de E c. Trois gestes de E a sont partis dans E c ; E a a vu des écrans coréens à son geste 2, puis a pris un raccourci privé. Dans le journal de E c :
- #1 « retour » (E c l'a signalé : « un retour que je n'ai pas fait », il a dû rouvrir le Remix) ;
- #6 « attends 30 » (attente en plus pendant le téléchargement du modèle, sans effet visible) ;
- #13 « retour » pendant la séparation (signalé par E c : retour à l'accueil ; la séparation a fini quand même).
Ce qui est touché chez E c : ses deux « retours » comptés dans `mesures.json` sont les deux gestes étrangers ; ses 61 gestes en comptent 2 qui ne sont pas à lui (les deux retours ; les « attends » ne comptent pas comme gestes) ; l'attente #6 tombe pendant le téléchargement du modèle (2 %), pas pendant la séparation. Ses deux défauts (#28 fin coupée non signalée, #61 fausse alerte « changé ») et ses promesses viennent de ses propres gestes après #14, mais son chemin a été dévié : il a rouvert le Remix puis créé un nouveau projet après le retour forcé. À garder, avec la mention « à rejouer sans contamination ». La fausse alerte #61 est aussi vue par C c #30, ce qui la rend crédible.

**Lignes `incidents-banc` (14 dans le fichier, dont une vide : C d avait écrit « aucun »).**
- Écran lu seulement en image : B a #19 (le texte ne donnait que la barre du bas) ; B a #75 : plafond d'images atteint, note posée à l'aveugle (#77). Les fiches de B a après #75 (#80, #83, #89, #96) sont à lire avec prudence.
- Attente au démarrage : F a (premier écran en 120 s), F c (plusieurs minutes), D b #7 et #24 (écran presque vide, l'éditeur arrive après une attente).
- Séparation qui avance d'environ 15 % par geste, quelle que soit l'attente : C b #5, E c #12, F d #26.
- Défi : pads vides puis remplis après 5 s (C a #1).
- Compteur bloqué sur « A, mesure 1/2 » (F b #63) : classé incident par le siège, mais A b #14 (banc? non), F a #5 et F c #8 voient le même compteur figé, et c'était déjà au tour 4. À ne pas classer « banc » sans test (§12.3).

**Fiches `banc?` (« ? »).** son : A a #43, A b #14, A d #43, B b #58, C b #67, C c #41, E c #28, E d #40, F a #5, F a #11, F c #8, F c #31 ; temps : A b #27, A d #13, C a #1, C a #14, C d #83 ; clavier : A c #61, D b #16, D c #4 ; moteur : F b #13, F b #73 ; rythme : C a #32.
Les trois « clavier » sont le même geste : après ✕ Effacer, le champ perd le curseur et il faut le retoucher avant d'écrire (A c #61, D b #16, D c #4). C'est le même bouton que le gel du renommage du tour 4 ; un test sans banc dirait si c'est l'appli.

## 6. Limites
- **Son** : le banc ne fait qu'écrire « ça joue » ou « rien ne joue ». Personne n'a entendu une voix, un Marimba, un effet ou un master (A b promesse Ab-1, F b Fb-3 « son non jugé ») ; 12 fiches « son » restent ouvertes.
- **Rythme** : le banc ne tape pas en vrai rythme ; les étoiles du défi (C a, Ca-4) ne sont pas jugeables.
- **Horloge** : le banc n'avance pas le temps réel. Le profil A c a été adapté (pas de « 3 jours après ») ; la séparation avance au geste et pas à l'attente ; le compteur et l'automatisation figés (F b #13, #73) peuvent venir de là.
- **Import de fichiers** : pas de sélecteur de fichiers hors Remix. C d adapté (export .zip et MIDI, sans import) ; ouvrir un .zip ou un MIDI reçu n'a été vérifié par personne, ni au tour 4 ni au tour 5.
- **TalkBack** : non simulé. E b adapté (petits textes en barres grises, noms lus dans l'arbre de l'écran, sans voix) ; un « sans nom » ici est un indice, pas une preuve.
- **Téléphone réel** : rien n'a été joué sur appareil (tableau de bord : CI et recette « ? »). Le verdict du renommage (T4-19) attend toujours le téléphone de Benjamin.
- **Ce que je n'ai pas pu vérifier** : les lignes du tour 1 et du tour 3 citées par les profils (T1-07, T3-09, T3-18) ; si les pads du défi ont été posés par l'appli ou par le banc ; les 186 fiches du tour 4 une par une (seules celles citées ici ont été relues).

## 7. Contre-vérification
Relecture adverse de `synthese.md` et `plus-value.md` : chaque affirmation rouverte dans le rapport, `mesures.json`, le journal ou l'écran texte du siège (copie de travail du scratchpad), `promesses.tsv`, `incidents-banc.tsv`, `tableau-de-bord.md`, `verdicts.tsv`, `base.tsv`, `META-AUDIT.md`, les rapports du tour 4 et `git log`. Les deux fichiers ont été corrigés sur place. Bilan : **20 confirmées, 13 corrigées, 2 non vérifiées** (35 points).

| n° | Affirmation | Verdict | Preuve |
|---|---|---|---|
| 1 | 24 sièges, 0 coupé, 0 plantage, 20 réussies (partielles B a, B c, C a, D b) | confirmé | 24 `mesures.json` : `fin: fin`, `plantage: false`, 4 « partielle » exactement sur ces sièges |
| 2 | 190 fiches (7,9 par siège) : 3 mission-impossible, 72 contourné, 115 finition | confirmé | comptage des 24 rapports ; courbe du tableau de bord (9,3 au tour 4) |
| 3 | Bloquant A a #27 (impossible de changer de partie en Simple) | confirmé | journal : #27 touche « C », #49 « B », #53 « M » ; écrans 28, 50 et 54 : en-tête toujours « पार्ट: A · इंट्रो » |
| 4 | Bloquant E a #117 (prise perdue après Pad Live) | confirmé, avec une nuance | écran 116 « Ta prise : 4 notes », #117 = Pad Live, écran 149 « Grille des notes : 0 notes » et « Vider » grisé ; refait #141-#142. Nuance : la mission (libre) est « réussie » ; c'est une perte de travail plutôt qu'une mission impossible |
| 5 | Bloquant B c #23 (Swing introuvable) | non vérifié | les deux glissés de B c dans Sounds (#28, #40) ne descendent que d'un cran (réglages du pad jusqu'à Panorama) ; aucun écran ne montre le bas de Sounds ; au tour 4, B d trouvait Swing deux défilements plus bas (T4 B d ligne 21). « Trop caché » reste possible |
| 6 | Graves F d #2, #34, #51 : « gaucher bloqué 3 fois » | corrigé | rapport F d : contourné en 8, 11 et 5 gestes, pas bloqué. #34 a « revu : écran 10 », un écran vu avant le défaut (écran 35) et qui est celui de #2 : même cause, grave discutable |
| 7 | Grave D b #39 (pas d'étoile ☆) | confirmé | contourné en 4, revu écran 4 (dossier Basse, vu avant mais autre dossier que Lead) ; déjà au tour 4 (E d ligne 15) |
| 8 | Grave C d #50 (« Mes exports » inatteignable en Simple) | confirmé | journal #50-#51 : deux glissés, empreinte d'écran inchangée en Simple ; atteint en Pro par #64-#68 |
| 9 | 3 bloquants + 5 graves = 8 (condition A du tableau de bord) | confirmé (chiffre) | tableau de bord « 8 » ; mais #5 et la moitié de #6 sont douteux |
| 10 | « Seul `d3eccff` touche l'appli entre les deux tours » | corrigé | `git show --stat 350079e` : 3 fichiers de l'appli changés (`GuideListen.kt`, `SeparationService.kt`, `RhythmViewModel.kt`, `isAvailable` → `isNative`), effet seulement sous le faux moteur du banc. `d3eccff` reste le seul changement visible |
| 11 | Prise au clavier gardée (A a écran 40, E a écrans 20 et 141) | confirmé | promesses t5-Aa-1 et t5-Ea-2 ; écran 141 « Ta prise : 3 notes » ; tour 4 « rien n'a été joué » chez 6 sièges dont A b, D c, D d (META-AUDIT l. 54) |
| 12 | Renommer ne fige plus (A c) ; T4-19 toujours « non tranché » | confirmé | t5-Ac-1 ; tour 4 C a ligne 11 et E a ligne 10 (5 min) ; `verdicts.tsv` T4-19 |
| 13 | Mes projets liste le projet (A c écran 67, D c écran 62) ; tour 4 « seul Trap » | confirmé | t5-Ac-2, t5-Dc-1 ; tour 4 D b lignes 32-33 (écran 59) |
| 14 | Les autres lignes « ce qui tient » (Vue Live, piano roll, Défi, Remix, tuto « je vérifie », exports) | confirmé pour le tour 5 | promesses t5-Ad-1, Ad-2, Ed-2, Ed-4, Da-1 (×2 « oui »), Ca-1, Cb-2, Dd-1, Dd-3, Bc-1, Bc-2, Cc-4, Ec-1, Ec-4, Ec-5 aux écrans cités. Les renvois au tour 4 de B b, B c, A a, B d et E b n'ont pas été relus |
| 15 | E a « ~40 Retour » ; F a « partage en 11 gestes » | corrigé | `mesures.json` : E a `retours: 25` ; F a `gestes_premier_partage: 9` (le siège écrit « geste #11 ») |
| 16 | C c #30 + E c #61 « moyen ×2 » | corrigé | les deux fiches sont « finition » (C c #30 non revue) : mineur ×2 |
| 17 | Contamination E c : « 3 gestes étrangers dans ses 61 », « l'attente #6 compte dans la séparation » | corrigé | journal E c : 61 = 54 touches + 3 glissés + 2 écrits + 2 retours ; les « attends » ne comptent pas, donc 2 gestes étrangers. #6 tombe pendant le téléchargement du modèle (« 2 % »), pas pendant la séparation |
| 18 | Incidents : 14 lignes dont 1 vide (C d) ; 13 lignes dans 10 sièges | confirmé | `incidents-banc.tsv` |
| 19 | Fiches « banc? » : 23 (synthèse) contre « 30, 16 % » (plus-value) | corrigé (plus-value) | `grep` des 24 rapports : 23 fiches (12 son, 5 temps, 3 clavier, 2 moteur, 1 rythme), plus 2 lignes d'incident (C b, E c). 23/190 = 12 % |
| 20 | Promesses 90 : 38 / 45 / 7 ; tableau de bord D « 0/2 » et « 85/120 » ; tableau par siège | confirmé | `promesses.tsv` (comptes identiques ligne à ligne) ; `tableau-de-bord.md` |
| 21 | « D a a donné le même id à 5 promesses » ; « ids inventés par siège » | corrigé | `briefs/Da.txt` ne donne qu'un id (t5-Da-1) pour 5 points ; les ids « t5-Aa-1 » viennent des briefs (`fabrique.py`), pas des testeurs |
| 22 | Fusion v2 : 176 grappes, 5 groupes de plusieurs sièges (3 justes, 1 lâche, 1 faux) ; v1 : 230/1, 186/1, 2 faux sur 4 | confirmé | `clusters-complet.md` ligne 2 et les 5 groupes ; META-AUDIT l. 1098 |
| 23 | Paire manquée « compteur figé (A b, F b, F c 8) » | corrigé | F b 63 est un incident, pas une fiche ; les fiches sont A b #14, F a #5, F c #8 |
| 24 | Graves déclarés par siège 2,25 / 1,35 / 0,33 | confirmé | courbe de `tableau-de-bord.md` |
| 25 | « E a 117 : une prise de 4 notes revient après Pad Live » (plus-value §4) | corrigé | le rapport dit l'inverse : la prise disparaît |
| 26 | « Trois verdicts banc démentis (T3-26c, T3-15, T3-02) » | corrigé | seul T3-26c est démenti (CI 243, T4-26 « appli ») ; T4-16 et T4-01 sont « non tranché » dans `verdicts.tsv` |
| 27 | « Le vrai gel du renommage était le banc » ; « vraie cause enfin trouvée » | corrigé | `d3eccff` répare bien le banc (240 s → 11 s), mais `verdicts.tsv` garde T4-19 « non tranché » jusqu'au téléphone |
| 28 | « Auto-tests 116 sur 117 verts » ; rouge dû à l'ordre des tests | corrigé ; ordre non vérifié | `selftest-gradle.log` : « 117 tests completed, 1 failed, 38 skipped » (78 verts). « Dépend de l'ordre » n'est que la note de `selftest-skipped.txt`. Les 6 lots ont bien `PS_SKIP_SELFTEST` |
| 29 | Séparation « environ 13 min » pour C b, E c, F d | non vérifié | seul C b écrit « ≈ 13 min » ; F d indique 1 100 s d'attente, E c ne chiffre pas |
| 30 | « Un tour de 24 sièges sans gel » ; 0 « repos non venu » ; coupés : T3 = 3, T4 = 3 + lot F | corrigé (précision) | B a a `gels: 1` (« GEL 815 ms » au geste 0) ; aucune ligne « repos non venu » ; META-AUDIT C12 ; `tour-4/lotF` n'a que des `start.txt` |
| 31 | `base.tsv` : 29 clés, toutes du tour 0 (31 lignes) ; 0 « revient » | confirmé | `base.tsv` ; tableau de bord |
| 32 | 0 `mesures.json` aux tours 3-4, 24/24 au tour 5 ; B a 2 chapitres sur 15, 108 gestes, 200 % | confirmé | condition E « 0/0 » ; `lotB/a/mesures.json` et profil |
| 33 | Coût : 15:10 → 17:14, 17:14 → 20:45, `350079e` 62 fichiers et 1 431 lignes, tour 20:57 → 21:52 | confirmé | `git log` et `git show --stat` ; heure de `lance-lot.sh` |
| 34 | Gabarit v2 : n° de geste sur les 190 fiches, « incidents-banc » 24/24, A a pile 60 lignes, au plus 10 défauts, 2 à 5 promesses par siège | confirmé | lecture des 24 rapports |
| 35 | Détail du gel : 4 × 60 s = 240 s, garde `renameAfterTheClearButton`, 11 s après `d3eccff` | confirmé | message et commentaires de `d3eccff` ; garde ajoutée dans `350079e` |

Ce qui reste fragile après contre-vérification : deux des huit « graves ou bloquants » (B c #23, F d #34) tiennent à une lecture du siège et non à une preuve ; les renvois au tour 4 n'ont été relus que pour les lignes citées ici.
