# Lecture « promesses » : ce que l'appli promet à l'écran, et qui l'a vérifié

Méta-audit de l'audit ultra, lecteur « promesses ». Dépôt `/home/user/fx-int` (branche `fx-int`, dernier commit `20ea449`, 2026-11-05). Lecture seule.

## 1. Comment j'ai cherché

- **Textes de l'appli** : toutes les chaînes passées à `tr("…")` dans `app/src/main/java` (4 436 chaînes, y compris celles écrites sur plusieurs lignes), plus les clés françaises de `project-model/src/main/resources/i18n/en/*.json` (7 759 clés : elles couvrent aussi les textes du Guide, des tutos et des recettes de style qui vivent dans `project-model`).
- **Mots qui promettent** : « en un toucher », « une touche », « tout de suite », « gestes », « en moins de », « ça sonne », « sans », « jamais », « toujours », « aucun », « automatique », « tout seul », « prêt », « facile », « rien à », « il suffit », « directement », « instantan », « gratuit », « sauvegard », « retrouv », plus « pile en rythme », « je vérifie », « pas à pas », « en rythme », « perdu ». Résultat brut : 267 chaînes `tr()` et 980 clés i18n ; après tri à la main (les « Aucun » de listes vides, les descriptions purement musicales, les doublons), **120 promesses** ci-dessous.
- **Qui les a vérifiées** : la colonne « promesse » de `docs/audits/ultra/sieges/tour-3/sieges.tsv` et `tour-4/sieges.tsv`, la ligne `promesse: … | tenue: …` en tête de chaque `rapport.md` des tours 3-4, et les défauts marqués `symptome: promesse` (44 rapports : 24 au tour 3, 20 au tour 4 ; le lot F du tour 4, dont le « chasseur de promesses » F c, n'a que son `start.txt` : jamais joué). Les tours 0-2 n'ont pas de colonne « promesse » dans leurs sièges ; je ne les compte pas.
- **Notation** : « T3 D d » = tour 3, lot D, siège d ; « écran 62 » = numéro d'écran du rapport. Verdict : **tenue / en partie / non / jamais vérifiée / non vérifiable au banc** (le banc ne peut pas juger un son à l'oreille).
- **Zones** (celles des équipes de correction, `docs/audits/ultra/tour-2-zone*.md`) : Z1 pads, Pad Live, défis, remix · Z2 exports et master · Z3 navigation, prise en main, sauvegarde · Z4 enregistrement · Z5 tutos, synthé, notes · Z6 textes, langues, accessibilité. La « fonction » est la ligne de `docs/audits/CARTE-FONCTIONS.md`.

## 2. Les 120 promesses

### A. Accueil et premier lancement (`Welcome.kt`, `LaunchChoice.kt`, `ModeChoice.kt`) : Z3

| n° | Promesse (texte à l'écran) | Fichier:ligne | Fonction (carte) · zone | Vérifiée ? |
|---|---|---|---|---|
| 1 | « Choisis un style, je t'aide pas à pas, du début à la fin du morceau. » | `onboarding/Welcome.kt:185` | Nouveau morceau (assistant) · Guide · Z3 | T3 D c : tenue (assistant 13 étapes) ; T3 A b : en partie ; T4 D a : en partie (mélodie jouée pas gardée, écran 20) |
| 2 | « Tu fais tout le morceau toi-même, étape par étape : je t'explique et **je vérifie**. » | `Welcome.kt:190` (aussi `LaunchChoice.kt:405`, `projects/NewProjectPage.kt:125`, `settings/SettingsScreen.kt:207`, `tuto/TutoPanel.kt:1044`) | Tutos · Z5 | T3 B d : en partie (validation échoue sans dire pourquoi) ; T4 B d : **non** (« Geschafft » après la démo remise à zéro, écran 29 ; 12 frappes à 100 % validées, écran 57) ; T4 C c : **non** (étape cochée sans rien faire, écran 40). **Non tenue deux tours de suite** |
| 3 | « Écouter une démo : un morceau déjà prêt, pour découvrir ce qu'on peut faire. » puis « Ta démo est prête. Écoute-la en entier, puis va voir dans Mon morceau comment elle est construite : tu peux tout changer. » | `Welcome.kt:195`, `:139-143` | Nouveau morceau · Z3 | **Jamais vérifiée** (T3 F a note seulement que la carte « Écouter une démo » disparaît après la première visite) |
| 4 | « Partir de zéro : aucune note : une batterie, une Acid 303 et un synthé attendent les tiennes. » | `Welcome.kt:200`, `NewProjectPage.kt:129` | Nouveau morceau · Z3 | T4 C c : en partie (Acid 303 absent de « Mój utwór » et de Miks, présent au clavier avec des notes, écran 25/43) |
| 5 | « Choisis une chanson du téléphone : l'appli sépare la voix, la batterie, la basse et le reste. » / « Remix : séparation des pistes : batterie, basse, voix et le reste. » | `Welcome.kt:206`, `LaunchChoice.kt:531` | Remix · Z1 | T3 B c : tenue ; T3 F b : en partie ; T4 A c : tenue |
| 6 | « Pad Live · NOUVEAU : des pads prêts à jouer, tes sons, des effets de scène. **Ça sonne tout de suite.** » | `LaunchChoice.kt:500` | Pad Live · Z1 | « sonne tout de suite » : T3 A a tenue, T3 F c tenue (un toucher = un son) ; « tes sons » : T3 F c **non** (« Poser » → « Ce son n'a pas pu être lu », pad B7 vide, écran 31) ; « effets de scène » : T3 C b en partie |
| 7 | « Enregistrer ma voix ou ma guitare : le micro enregistre par-dessus ta musique, **pile en rythme**. » | `Welcome.kt:214`, `LaunchChoice.kt:399` | Enregistrer (voix, micro) · Z4 | T3 F a : **non** (« Le moteur audio n'a pas démarré », écrans 75 et 79) ; T3 D a : non (ouvre le projet « Live », écran 75) ; T3 E c : en partie (ouvre « My first beat » avec le Guide « Add a melody », écran 3) ; T3 C a : en partie. « pile en rythme » : **non vérifiable au banc** |
| 8 | « Défis : écoute un rythme, refais-le sur les pads, gagne des étoiles. » | `LaunchChoice.kt:419`, `:433` | Pad Live (défis) · Z1 | T3 A a : en partie (0 étoile, rythme jamais montré, écran 26) ; T4 A a : en partie (quel pad taper invisible, écrans 14/19) |
| 9 | « Nouveau morceau : **un style en une touche**, ou pas à pas avec l'assistant. » | `LaunchChoice.kt:380` | Nouveau morceau · Z3 | T3 F a : **non** (4 écrans avant « Créer et écouter », écran 58, mineur) ; T4 D d : **non** (5 touchers, écran 12, grave). **Revient** |
| 10 | « Mes projets : ouvrir un autre morceau, le copier ou **le renommer**. » | `LaunchChoice.kt:384` | Sauvé (Mes projets) · Z3 | T3 B a : **non** (gel au renommage) ; T4 E a : **non** (écran figé 5 min sur « Effacer »). **Revient** |
| 11 | « Continuer » + « Sauvegardé il y a … » | `LaunchChoice.kt:182`, `:351` | Sauvé · Z3 | T3 B a : non (accueil sans « Reprendre », écran 1) ; T4 D a : tenue (« Doorgaan » retrouvé) ; T4 E a : « Sauvegardé » visible, tenue |
| 12 | « Pad Live a son propre projet « Live » : « %s » t'attend dans Mes projets. » (toast) | `ui/AppRoot.kt:1522` | Sauvé · Z3 | T4 B c : **non** (seule la carte « Live » est listée, écran 30) |
| 13 | « Le guide des premiers pas reste dans ⋮ › Aide et premiers pas. » | `Welcome.kt:227`, `settings/GuideDialog.kt:113` | Guide · Z3 | T3 D c : **non** (pas de ⋮ sur l'accueil, écran 17, mineur) |
| 14 | « Aucun projet pour l'instant : commence un nouveau morceau. » | `LaunchChoice.kt:313` | Sauvé · Z3 | **Jamais vérifiée** (aucun siège n'a supprimé tous ses projets) |
| 15 | « Simple : pour débuter : 4 onglets, de gros boutons et **des mots de tous les jours**. » | `ui/ModeChoice.kt:62` | S/P (Mode simple) · Z6 | T3 C c : **non** (Snare, groove, bar, LUFS…) ; T4 D d : **non** (anglais translittéré partout, écran 13) ; T4 D c : non ; T4 E c : non. **Revient (4 sièges, 2 tours)** |
| 16 | « Ton morceau ne change pas : seuls les écrans et les mots changent. Ce projet garde le mode choisi. » / « Changer de façon de travailler ne change rien à tes morceaux. » | `ModeChoice.kt:261`, `SettingsScreen.kt:67` | S/P · Z3 | T3 F a : en partie (en Pro le format passe en WAV et le Guide saute de 17 à 26 %, écran 70) ; T4 D d : en partie (« Simple ou Pro ? » revient au projet suivant, écran 54) |
| 17 | « Tu pourras changer de mode à tout moment avec la pastille en haut. » | `ModeChoice.kt:87-88` | S/P · Z3 | **Jamais vérifiée** |
| 18 | « Plus tard : voir le rythme de départ » → « Un rythme et une ligne d'acide prêts à tourner. » | `Welcome.kt:224`, `AppRoot.kt:286` | Nouveau morceau · Z3 | **Jamais vérifiée** (T3 B a : son projet « caché derrière Plus tard », écran 1) |

### B. « Aide et premiers pas » (`settings/GuideDialog.kt`) : Guide, Z3

| n° | Promesse | Fichier:ligne | Fonction · zone | Vérifiée ? |
|---|---|---|---|---|
| 19 | « Tout est sauvegardé tout seul, à chaque geste. » / « Pas besoin de sauvegarder : tout est gardé tout seul. » | `GuideDialog.kt:46`, `library/SongGuide.kt:70` | Sauvé · Z3 | T3 B a : en partie (projet retrouvé, mais gel) ; T3 D a : en partie (une mélodie perdue) ; T4 D a : en partie (mélodie jouée pas gardée) ; T4 E a : non |
| 20 | « ↶ ↷ Annuler et rétablir : on peut toujours revenir en arrière. » / « Une erreur ? ↶ répare tout, autant de fois que tu veux. » | `GuideDialog.kt:53`, `:76` | Annul. · Z3 | T3 B a : **non** (aucun « Rétablir » à l'écran, écran 4) ; T4 B b : tenue (Undo/Redo) ; T4 D c : en partie (« إعادة » prend la place exacte de « تراجع », écran 60) ; T4 B c : en partie (annuler efface la prise sans prévenir) |
| 21 | « ● Enregistrer : ce que tu joues (clavier, pads, clavier MIDI) s'écrit en rythme. » | `GuideDialog.kt:54` | Notes · Enregistrer la performance · Z4 | T4 A b : **non** (« Rien n'a été joué pendant la prise », écran 33) ; T4 C b : **non** (écran 39) ; T4 C d : **non** (écran 63) ; T3 D a : mélodie perdue. **Revient** (« la mélodie au clavier : rien n'a été joué ») |
| 22 | « ▶ Jouer et arrêter, sur tous les écrans. » / « ▶ joue tout le morceau ; « ⟳ Boucler » boucle la partie affichée. » / « ▶ joue tout le morceau, de l'Intro à la fin, sur tous les écrans. » | `GuideDialog.kt:55-56`, `SongGuide.kt:76` | Briques et parties · Z3 | T3 A b : **non** (« Jouer » = boucle de 4 mesures, écran 17) ; T4 A d : **non** (seule l'Intro tourne, le morceau entier par un menu caché, écran 23) ; T4 D b : en partie (double barre de transport). **Revient** |
| 23 | « Touche une partie pour écouter le morceau à partir d'elle. » | `session/SessionScreen.kt:385` | Briques et parties · Z3 | T3 A b : **non** (écran 31) ; T3 C a : **non** (écran 45) ; T3 E a : **non** (écran 87) : trois sièges, « rien ne joue » ; T4 A d : en partie (par « Tocar a partir de… ») |
| 24 | « Structure : les parties du morceau sur la ligne du temps ; deux touches sur un bloc l'ouvrent. » | `GuideDialog.kt:63` | Briques et parties · Z3 | T4 C c : en partie (pas d'axe du temps en mode Simple, écran 26) ; en Pro : **jamais vérifiée** |
| 25 | « Le Mode simple n'en garde que quatre : Mon morceau, Rythme, Clavier, Envoyer. » | `GuideDialog.kt:68` | S/P · Z3 | T3 C c : tenue ; T4 D c : tenue |
| 26 | « Appui long sur un pas ou une note : plus fort ou plus doux. » / « Sur un potard : glisse vers le haut ou le bas ; deux touchers le remettent à zéro. » | `GuideDialog.kt:74`, `:75` | Batterie · Notes · Z1/Z5 | **Jamais vérifiée** (T3 D d a testé le pan d'un pas, pas la force ni le potard) |
| 27 | « Pour partager : ⤴ tout en haut à droite, sur chaque écran. » | `GuideDialog.kt:83` | Export · Z2 | T3 F d : en partie (l'envoi du projet caché hors de « Send ») ; T4 E b : tenue |
| 28 | « Clavier : joue et enregistre ta mélodie, toujours dans la gamme. » / « Le clavier : joue, il reste dans la gamme du morceau : aucune fausse note possible. » | `GuideDialog.kt:102`, `SongGuide.kt:85` | Notes · Z5 | « dans la gamme » : T3 D c tenue (clavier verrouillé) ; « enregistre » : voir n° 21 (**non**) |
| 29 | « Ce guide se retrouve dans Plus › Aide. » (Pro) | `GuideDialog.kt:84` | Guide · Z3 | **Jamais vérifiée** en Pro |

### C. Le Guide (cartes `project-model/src/main/resources/guides/*.json`, `guide/*.kt`) : Z3/Z5

| n° | Promesse | Fichier:ligne | Fonction · zone | Vérifiée ? |
|---|---|---|---|---|
| 30 | « L'appli écrit tout le morceau pour toi : rythme, basse, accords et mélodie. Ensuite, tu le fais tien. » | `guides/common.json:13` | Nouveau morceau · Z3 | T3 E a : tenue (Trap 3:05) ; T4 D a : tenue |
| 31 | « Un style, une ambiance : le morceau est prêt en une minute. » | `guides/common.json:25` | Nouveau morceau · Z3 | T4 D d : en partie (5 touchers) ; T3 A b : en partie |
| 32 | « Écouter : juste avant le premier grand moment : le meilleur passage, tout de suite. » | `guides/common.json:58` | Guide · Z3 | T3 E a : **non** (lecture depuis « Intro batt. 1 », écran 9) |
| 33 | « Depuis le début : tout le morceau, de la première mesure. » | `guides/common.json:67` | Guide · Z3 | T4 A d : **non** (voir n° 22) |
| 34 | « Changer un son : le son de la mélodie, **en un toucher**. » / « Touche-en un : tu l'entends tout de suite. » | `guides/common.json:87`, `:133` | Bibliothèque de sons · Z1 | T3 D c : en partie (le panneau recouvre la carte, écran 44) ; T4 C c : **non** (aperçu : rien ne joue, écran 14) ; T3 F c : **non** (Poser échoue, écran 31) |
| 35 | « Enregistre une mélodie : touche « ● Enregistrer ma mélodie » et joue quelques notes : le clavier reste dans la gamme, aucune fausse note. » | `guides/common.json:158` | Notes · Z5 | T4 D a : **non** ; T4 C d : **non** (écran 63) ; T4 A b : **non** (écran 33). **Revient** |
| 36 | « Générer une ligne Acid 303 : une ligne 303, **en un toucher**. » | `guides/common.json:278-279` | Acid 303 · Z5 | T4 C c : **non** (aucun bouton de ce nom, le bouton s'appelle « Napisz mi linię » ; étape cochée sans rien faire, écran 40, grave) |
| 37 | « Arrange les parties : appui long sur une partie : ajoute un Fill, un Break ou une Montée. » | `guides/common.json:196` | Briques et parties · Z3 | T3 E a : en partie (appui long : aucune option de nom, écran 25) ; T4 D b : tenue |
| 38 | « Duplique ta partie et change-la : un couplet, un refrain. Donne-leur un nom pour t'y retrouver. » | `guides/common.json:342` | Briques et parties · Z3 | T3 E a : **non** (impossible de nommer ses parties) → T4 D b : tenue, mais « les 5 occurrences renommées sans avertissement » (écran 24) |
| 39 | « Partager : les vérifications, puis le fichier. » / « Tout est fait : ton morceau est prêt à partager. » | `guides/common.json:105`, `model/guide/GuideAnalyzer.kt:151` | Export · Z2 | T3 D c : **non** (3 dtk pour 0:58, écran 60) ; T4 E b : en partie |
| 40 | « Remixer un morceau : l'appli sépare une chanson… » / « Il t'attend dans Sons › Remix, quand tu veux. » | `guides/common.json:412`, `:429` | Remix · Z1 | séparation : tenue (n° 5) ; « Sons › Remix » : **jamais vérifiée** |
| 41 | « Compare avec [morceau du style], au même volume : plus fort paraît toujours mieux, alors on égalise d'abord. » → « les deux jouent maintenant au même volume » | `guides/techno.json:…` (36 styles), `guide/GuideListen.kt:307-309` | Export et Master pro · Z2 | **Non vérifiable au banc** (volume à l'oreille) ; T4 B a : les indicateurs LUFS ne bougent pas (écran 49) |
| 42 | « Ça sonne déjà juste » (carte du Guide) | `guides/acid_techno.json:1527`, `dnb.json:1605`, `jungle.json:1498`, `phonk.json:1515` | Guide · Z5 | **Non vérifiable au banc** |
| 43 | « ▶ Écouter l'exemple » / « Joue en boucle, tout seul : change le son pour comparer. » | `guide/GuidePanel.kt:383`, `rhythm/KitAndPadSheets.kt:231` | Bibliothèque de sons · Aide · Z1 | T3 D d : en partie (ouvre le glossaire, il faut re-toucher, écran 28) ; T4 C c : **non** (rien ne joue, écrans 45 et 14) ; T3 C d : **non** (ambiance : rien ne joue, écran 59). **Revient** |
| 44 | « Écouter sans puis avec » / « D'abord sans, puis avec l'écho. » | `GuidePanel.kt:514`, `:617`, `model/learn/SoundGlossary.kt:121` | Effets · Z2 | **Jamais vérifiée** (les écoutes d'exemple n'ont été touchées que par T4 C c, « rien ne joue ») |
| 45 | « Rien encore : chaque choix fait ici ou par le Guide s'inscrit ici, et se ramène d'un geste. » | `guide/TrackSheet.kt:361` | Guide · Annul. · Z3 | **Jamais vérifiée** |

### D. Nouveau morceau et assistant (`NewProjectPage.kt`, `WizardScreen.kt`, `SongGuide.kt`) : Z3/Z5

| n° | Promesse | Fichier:ligne | Fonction · zone | Vérifiée ? |
|---|---|---|---|---|
| 46 | « Une touche, un morceau complet de 2 à 6 minutes selon le style, prêt à écouter. » | `projects/NewProjectPage.kt:207` | Nouveau morceau · Z3 | « complet » : T3 E a tenue (Trap 3:05) ; « prêt à écouter » : T3 A b en partie (Jouer = boucle de 4 mesures) ; « une touche » : voir n° 9 (**non**) |
| 47 | « Créer tout de suite avec les choix conseillés » | `library/WizardScreen.kt:217` | Nouveau morceau · Z3 | **Jamais vérifiée** |
| 48 | « Pour que ça sonne vraiment [style] : [kit]. L'assistant prendra ses sons dedans. » | `WizardScreen.kt:462` | Nouveau morceau · Z5 | **Jamais vérifiée** directement ; par le tuto : T3 B d et T4 B d, le kit annoncé « 909 » n'est pas celui affiché « Kit: Studio » (écrans 26 et 41) |
| 49 | « Tes choix seront perdus. Tu pourras le relancer depuis Mes projets › « Nouveau morceau guidé ». » | `WizardScreen.kt:190` | Nouveau morceau · Z3 | T3 A b : tenue (« Nouveau morceau guidé » vu dans Mes projets, écran 15) |
| 50 | « Ton morceau [style] est prêt. Il est fait de quatre briques, comme un vrai morceau de club : la Montée fait monter la tension sans kick, tout tombe au Drop… » | `SongGuide.kt:58-68` | Briques et parties · Z3 | T3 D c : en partie (« EMPAT BLOK » A-D mais 5 parties, « Pre-chorus » en anglais, écran 41) ; T4 D c : en partie (ordre Intro → 3 refrains → 2 couplets → Intro, écran 24) |
| 51 | « Acide : ta ligne 303 joue dans … La coupure s'ouvre toute seule le long du morceau » / « Filtres : certains sons s'ouvrent et se ferment tout seuls » / « Souffle montant : on ne l'entend qu'en jouant tout le morceau » | `SongGuide.kt:89`, `:109`, `:98-99` | Acid 303 · Effets · Z5 | **Jamais vérifiée** (tout est à l'oreille ou sur une courbe que personne n'a ouverte) |
| 52 | « Ta voix : enregistre-la par-dessus le morceau (mets des écouteurs, sinon le micro entend la musique). » | `SongGuide.kt:110` | Enregistrer · Z4 | T3 C a : en partie ; T4 A d : en partie (choisir une prise coupe l'écoute du morceau entier, écran 40) |
| 53 | « Écris-moi une ligne » / « Génère-moi un pattern » / « Génère-moi une ligne » / « Écrire une mélodie pour moi » (boutons magiques) | `acid/AcidScreen.kt:160`, `WizardScreen.kt:653`, `synth/TrackAdd.kt:66` | Acid 303 · Notes · Z5 | Acid : T4 C c en partie (bouton trouvé sous un autre nom que le Guide, écran 38) ; « Écrire une mélodie pour moi » : **jamais vérifiée** |
| 54 | Descriptions des styles : ce que le morceau généré contient (« Le kick tape chaque temps sans jamais s'arrêter », « Peu de coups de kick, jamais au même endroit », ~20 phrases) | `model/StyleRecipes.kt:521`, `guides/techno.json:128`, i18n `model.json` | Nouveau morceau · Z5 | **Jamais vérifiées aux tours 3-4** (seul T3 E a a regardé la structure d'un Trap) |

### E. Tutos (`tuto/TutoPanel.kt`, `project-model/src/main/resources/tutos/*.json`) : Z5

| n° | Promesse | Fichier:ligne | Fonction · zone | Vérifiée ? |
|---|---|---|---|---|
| 55 | « Avant de commencer : ton kit est **déjà** celui de [style]… **Tu n'as rien à régler.** » (dans les 36 tutos) | `tutos/acid_techno.json:53`, `detroit.json:73`, `uk_garage.json:61`, `hard_techno.json:63`… | Tutos · Z5 | T3 A c : **non** (la fenêtre du kit remplace le tuto, écran 14) ; T3 B d : **non** (kit reste « Kit: Studio », écrans 8 et 26) ; T4 B d : **non** (écran 41) ; T4 E c : **non** (« Kit: Studio », téléchargement refusé, écran 29). **Revient (2 tours, 4 sièges)** |
| 56 | « Déjà prêt pour toi, pour que ça sonne drum & bass tout de suite : le kit, des effets sur plusieurs pads (badge FX)… » (tuto Pro) | `tutos/dnb.json:64` (et jump_up, jungle, liquid, neurofunk, dubstep, riddim) | Tutos · Effets · Z5 | T3 B d : **non** (effets annoncés absents : « ni Sättigung ni Pumpen », écran 52) |
| 57 | « Les vrais sons de batterie du style… Sans eux, le tuto marche avec les sons intégrés. » | `tuto/TutoPanel.kt:293` | Tutos · Z5 | T3 A c : **non** (le tuto disparaît derrière la fenêtre du kit) ; T4 A b : en partie (des bandeaux cachent le Guide) |
| 58 | « « Accords » pose la triade **d'un toucher** » / « « Copier sur les mesures suivantes » : les accords suivent **tout seuls** » | `tutos/dnb.json:228`, `:227`, `jungle.json:223`, `trap.json:194` | Accords · Z5 | **Jamais vérifiée** (T4 B d a vu l'étape Akkorde-Spur, écran 65, mais n'a jugé que l'allemand) |
| 59 | « Bravo, c'est réussi ! » / « Réussi » / « Fait » (le tuto dit que l'étape est bonne) | `TutoPanel.kt:506`, `:610` | Tutos · Z5 | T4 B d : **non** (écrans 29 et 57) ; T4 C c : **non** (écran 40) ; T3 B d : en partie. Voir n° 2 |
| 60 | « Écouter mon morceau » à la fin du tuto (ajouté au tour 3, n° 30, commit `b3ee0d6`) | `TutoPanel.kt:320` | Tutos · Z5 | T3 A c : **non** avant correction (« Presque fini ! 33 étapes », écran 36) ; T4 A b : **jamais atteinte** (banc arrêté à 32 %). Non re-vérifiée depuis la correction |
| 61 | « Bravo, ton morceau est fini ! Tu peux tout réécouter et continuer à le changer. » | `TutoPanel.kt:816` | Tutos · Z5 | **Jamais atteinte** par un siège (T3 A c, T3 E b, T4 A b, T4 E c ont tous arrêté avant la fin) |
| 62 | « Montre-moi » (l'appli fait le geste à ta place) | `TutoPanel.kt:755` | Tutos · Z5 | T4 B d : en partie (« validée après la démo remise à zéro », écran 29) ; sinon **jamais vérifiée** |
| 63 | « Le mode choisit aussi le niveau du tuto. » / « Avec le tuto Facile » | `ModeChoice.kt:87`, `:106` | Tutos · Z3 | T3 A c : tenue (Facile) ; T3 B d : tenue (Pro) |

### F. Pad Live (`help/ScreenHelp.kt`, `padlive/*`, `live/LivePad.kt`) : Z1

| n° | Promesse | Fichier:ligne | Fonction · zone | Vérifiée ? |
|---|---|---|---|---|
| 64 | « « Pack » remplit la page affichée avec les sons d'un style ; **« Annuler » la remet comme avant**. L'étoile « Défi » lance un défi. » | `help/ScreenHelp.kt:17` | Pad Live · Aide · Z1 | T3 D c : **non** (aucun « Batal », écran 9) ; T3 F c : en partie (« Annuler » caché dans « Plus », écran 24) ; T3 F a : en partie (l'aide dit « La cible », écran 34) ; T4 C c : **non** (aucun « Anuluj », bouton nommé « Impreza », écran 79). **Revient** |
| 65 | « Garde le doigt sur un effet : il joue tant que tu appuies, **puis tout revient**. » / « Lâche : tout revient. » | `ScreenHelp.kt:21`, `session/LivePad.kt:71` | Effets (live) · Z1 | T3 C b : en partie (effet tenu invisible après relâche) ; T3 F c : en partie (Filtre : rien ne change à l'écran, écran 10) ; T4 B c : tenue (effets tenus visibles). « tout revient » à l'oreille : **non vérifiable au banc** |
| 66 | « Enregistre ce que tu joues sur les pads, en mesures entières, puis pose-le sur un pad libre ou dans une brique du Live. REC enregistre toute ta perf pour en faire un morceau. » | `ScreenHelp.kt:25` | Enregistrer la performance · Z1/Z4 | T3 C b : en partie (clip vert vide, case « + », écran 42) ; T3 E d : **non** (pad rempli reste « listo », écran 49) ; T4 B c : tenue (prise retrouvée) ; T4 A c : tenue |
| 67 | « **Tout couper** arrête tous les pads. Briques ouvre le Live complet. Gros pads met les pads sur tout l'écran pour jouer à plusieurs. Partager envoie ta perf. » | `ScreenHelp.kt:29` | Pad Live · Z1 | T3 C b : **non** (« 全部止める » sans effet) ; T4 B c : en partie (Pad Live OK ; « ■ 全部 » du Live ne coupe pas la prise, écran 22 ; Gros pads : ni tout couper ni effets, écran 53) ; T4 A c : en partie (« Detener todo » : la musique du projet continue, écran 50). **Revient** ; « Partager envoie ta perf » : **jamais vérifiée** |
| 68 | « L'appli joue un petit rythme… écoute, puis refais-le. Tu gagnes jusqu'à 3 étoiles selon ta précision. » | `ScreenHelp.kt:33` | Pad Live (défis) · Z1 | T3 A a : en partie (0 étoile, vitesse jamais montrée, écran 26) ; T4 A a : en partie (écrans 14/19/29) ; T3 F a : en partie (score à peine vu, écran 41) |
| 69 | « Coup : le son joue une fois à chaque toucher. Boucle : un toucher le lance en rythme, un autre l'arrête. Tenir : il joue tant que ton doigt reste sur le pad. » | `ScreenHelp.kt:37`, `padlive/PadModes.kt:61` | Pad Live · Z1 | Coup : T3 F c tenue ; Boucle : T4 A c tenue, T3 E d **non** (« listo » ne joue jamais) ; Tenir : **jamais vérifiée** |
| 70 | « Un seul à la fois : les pads d'un même groupe se coupent entre eux » / « Attaque et fin : le son arrive en douceur… s'éteint doucement » | `ScreenHelp.kt:41`, `:45` | Pad Live · Z1 | **Jamais vérifiée** (groupes) ; **non vérifiable au banc** (attaque/fin) |
| 71 | « Prêt : ça part à la mesure » (prise posée sur un pad) | `padlive/PlayedTakeSheet.kt:45` | Pad Live · Z1 | T3 E d : **non** (écrans 49-50) ; T4 A c : tenue |
| 72 | « Glisse sur le pad : à gauche ça coupe les aigus, à droite les graves, en haut ça résonne. » / « Garde le doigt sur un pad : ça répète en rythme. » / « les boucles restent calées sur le tempo, comme chez un DJ » | `LivePad.kt:71`, `live/LiveFx.kt:432`, `help/Glossary.kt:27` | Effets (live) · Pad Live · Z1 | **Non vérifiable au banc** (à l'oreille) ; personne n'a même regardé un indicateur |
| 73 | « Préparation des pads prêts à jouer… » (puis les pads pleins) | `AppRoot.kt:2754`, `padlive/PadLiveRoute.kt:108` | Pad Live · Z1 | T3 F c : en partie (16 pads vides « + » à l'ouverture alors que le pack est chargé, écran 6) ; T4 A a : **non** (pads vides avec +, écran 2). **Revient** |
| 74 | « Un seul fichier avec tes effets, prêt à envoyer. » (partager la perf) | `padlive/PerfShare.kt:264` | Export · Z2 | **Jamais vérifiée** |

### G. Live (grille, briques, cases Remix) : Z1

| n° | Promesse | Fichier:ligne | Fonction · zone | Vérifiée ? |
|---|---|---|---|---|
| 75 | « Touche une case pour la lancer, la lettre pour lancer toute la ligne, **■ pour arrêter**. » | `ScreenHelp.kt:58` | Cases du Live · Z1 | T3 D d : **non** (■ n'arrête rien, écran 62) ; T3 C b : **non** (▶ de ligne : rien ; ligne 1 ne s'arrête pas, écrans 15/30) ; T4 C c : **non** (écran 63) ; T4 B c : tenue dans Pad Live. **Revient (2 tours)** |
| 76 | « La colonne Remix joue un passage de la chanson : appui long sur une de ses cases pour le choisir. » (depuis `b3ee0d6`, dite seulement si la colonne existe) | `ScreenHelp.kt:59`, `Glossary.kt:345` | Cases Remix du Live · Z1 | T3 D d : **non** (colonne absente, écran 59) ; après correction : **jamais re-vérifiée** |
| 77 | « Ce que tu lances part au prochain départ choisi… tout reste en rythme. » / option « Tout de suite » / « D'un coup » | `ScreenHelp.kt:63`, `LiveFx.kt:606`, `:614` | Cases du Live · Z1 | T3 C b : **non** (rien ne part, écrans 27-32) ; T4 B c : tenue ; « Tout de suite » : **jamais vérifiée** |
| 78 | « Case Remix : … Il part sur la mesure et suit le tempo. » | `ScreenHelp.kt:67` | Cases Remix du Live · Z1 | T3 E d : **non** (case « suena » mais piste Voz coupée, rien ne le dit, écran 36) ; T4 A c : **non** (colonne « Sonido de… » désactivée, rien ne s'entend, écran 45). **Revient** |
| 79 | « Ta première boucle tourne. Touche un point pour y ajouter un son. » | `live/LiveScreen.kt:971` | Cases du Live · Z1 | T3 E d : **non** (« Tu primer bucle está sonando » mais on n'entend pas la voix, écran 36) |
| 80 | « Prêt : l'enregistrement part au prochain lancement. » / « REC enregistre tout ce que tu joues… « Annuler » la retire d'un coup ; l'ancien morceau reste dans « Versions ». » | `LiveScreen.kt:818`, `Glossary.kt:349` | Enregistrer la performance · Z1 | T3 C b : tenue (enregistrement part) ; T4 B c : tenue, mais « Annuler » efface la prise sans prévenir (en partie) ; « Versions » : **jamais vérifiée** |
| 81 | « Joue aux notes : elles sonnent sur l'Acid 303. Retouche la case pour arrêter : la boucle se cale sur la mesure. » | `live/LiveCellMenu.kt:97` | Cases du Live · Acid 303 · Z1 | **Jamais vérifiée** |

### H. Rythme, batterie, sons (`rhythm/*`, `library/*`) : Z1/Z5

| n° | Promesse | Fichier:ligne | Fonction · zone | Vérifiée ? |
|---|---|---|---|---|
| 82 | « Les pads sonnent, les cases gardent les coups. Appui long : changer le son. » | `rhythm/RhythmScreen.kt:283` | Batterie · Z1 | cases : T3 D d tenue (aide Krok juste) ; « changer le son » : T3 F c **non** (Poser échoue, écran 31) ; T4 A b : en partie (pads nommés autrement que le Guide, écran 44) |
| 83 | « Couches : … La batterie sonne plus vivante. » | `ScreenHelp.kt:135` | Batterie · Z1 | **Jamais vérifiée** |
| 84 | « Calé au tempo : N temps à N BPM, sans changer la hauteur » / « la chanson s'y cale, sans changer de hauteur » / « suit le tempo du projet, sans changer de hauteur » | `RhythmScreen.kt:1249`, `AppRoot.kt:1304`, `remix/RemixScreen.kt:510` | Bibliothèque · Remix · Z1 | **Non vérifiable au banc** (hauteur à l'oreille) |
| 85 | « Chercher un son (caisse, conga, sirène…) » (le champ promet que ses exemples se trouvent ; au tour 3 il disait « doigts, verre, conga », corrigé `b3ee0d6`) | `library/SoundShelfBrowser.kt:341` | Bibliothèque de sons · Z1 | T3 F a : **non** (« verre », « conga », « caisse » : aucun son, écrans 21/27) ; T3 D b : **non** (« davul », écran 22) ; T4 C d : en partie (synonymes et mots sans accents non trouvés, écrans 13/25/83). **Revient** |
| 86 | « « %s » est dans ta bibliothèque : tu le retrouves dans « Changer le son », pour tous tes projets. » | `library/MySoundLibrary.kt:91` | Bibliothèque · Z5 | T3 C d : tenue (preset gardé avec ses effets, sur 2 pistes) ; « pour tous tes projets » : **jamais vérifiée** |
| 87 | « Touche ☆ sur un son pour le retrouver dans « Favoris ». » | `SoundShelfBrowser.kt:360` | Bibliothèque · Z5 | T4 E d : **non** (aucune ☆ sur les tuiles, écrans 33/74) |
| 88 | « À télécharger seulement si tu veux ; chaque pack se supprime d'un geste pour libérer la place. » | `library/LibraryScreen.kt:453` | Bibliothèque · Z1 | T3 D d : en partie (confirmation = 2 touchers, écran 114, mineur) |
| 89 | « C'est prêt : le pack « %s » est sur ton téléphone. » | `library/SoundLibraryViewModel.kt:416` | Bibliothèque · Z1 | T3 B d : **non** (chargement 0 % puis la carte disparaît, kit reste Studio, écran 8) ; T4 E c : téléchargement refusé par le banc : **non vérifiable** |
| 90 | « Le son est créé sur l'appareil, sans internet. » / « Elle glisse d'une note à l'autre, rien à télécharger » | `generate/GenerateScreen.kt:136`, `instruments/RealInstruments.kt:89` | Bibliothèque · Z5 | **Jamais vérifiée** (T4 D a : 7 instruments réels sur 8 « Binnenkort beschikbaar », écran 20) |
| 91 | « Aucun pack à télécharger : les sons intégrés sont tous là, prêts à servir. » | `LibraryScreen.kt:450` | Bibliothèque · Z1 | **Jamais vérifiée** |

### I. Mix, effets, automation, master (`mixer/*`, `help/ScreenHelp.kt`) : Z2

| n° | Promesse | Fichier:ligne | Fonction · zone | Vérifiée ? |
|---|---|---|---|---|
| 92 | « Ambiances toutes prêtes : touche une ambiance pour l'essayer : tu peux toujours revenir avec Annuler. » / « Un réglage tout prêt… certaines ajoutent aussi des effets à la liste » | `mixer/QuickFx.kt:102`, `:154`, `Glossary.kt:268` | Effets · Z2 | T3 D d : en partie (« Pod wodą » : 0 effet sur 8, écran 37) ; T4 B a : en partie (« Punch » remplace l'EQ sans prévenir, Annuler le rattrape, écran 68) ; T3 C d : **non** (appui long : rien ne joue, écran 59) |
| 93 | « Dessine la courbe du réglage au doigt… : il bouge tout seul pendant le morceau. » / « Lance la lecture et bouge un réglage : il s'enregistre pas à pas sur la boucle. » / « 1 réglage bouge tout seul. » | `mixer/AutomateSheet.kt:101`, `mixer/AutomationCard.kt:412-413`, `mixer/MixerScreen.kt:272` | Effets (automation) · Z2 | T3 B d : en partie (noms de piste et de réglage différents du tuto, écran 41) ; le mouvement lui-même : **jamais vérifié** (le siège T4 F d « automatisations » n'a pas été joué) |
| 94 | « Un effet branché directement sur une piste… Jusqu'à huit par piste, et chacun peut ne jouer que dans certaines parties. » / « Ne joue dans aucune partie : touche une lettre ou « Tout ». » | `Glossary.kt:235`, `mixer/EffectsBox.kt:254` | Effets · Z2 | « 8 » : vu affiché (T3 D d, écran 37) ; « que dans certaines parties » : **jamais vérifiée** |
| 95 | « Le volume visé… Une cible par projet, **la même dans Mix et au partage**… Si le son devait s'écraser, l'appli s'arrête avant et le dit. » | `ScreenHelp.kt:102` | Export et Master pro · Z2 | T3 F b : en partie (cible éteinte par défaut, cachée loin de l'export) ; T3 D c : **non** (−14,2 annoncé dans Envoyer, −9 obtenu, écran 60) ; T4 E b : en partie (l'aide cite « Apple Music » et « Libre », absentes, écran 51) ; T4 D d : en partie (deux lignes contradictoires, écran 30) ; « s'arrête avant et le dit » : **jamais vérifiée** |
| 96 | « Fort, comme en club (N LUFS)… La sortie monte doucement pour y arriver ; l'export s'en approche autant qu'il peut sans écraser le son. » | `mixer/MasterLoudness.kt:109-112` | Export et Master pro · Z2 | T3 A d : tenue (club −8 et streaming −14 : deux fichiers différents) ; T4 B a : **non** (la mesure reste −14,0 après +6 dB et cible Club, écran 49) |
| 97 | « Mesures pro : LUFS, LRA, crête vraie, PLR » (des mesures qui bougent) | `ScreenHelp.kt:121`, `export/ExportBilan.kt` | Export et Master pro · Z2 | T3 A d : **non** (chiffres identiques pour 4 mesures et 1 mesure, « Length: 3 s », écrans 32/60) ; T4 B a : **non** (réduction du compresseur « 0 dB », écran 26 ; vumètres d'une piste muette, écran 95 ; analyseur vide, écran 43). **Revient (mesures figées)** |
| 98 | « Plafond de crête : −1 dBTP garde une marge : le fichier ne grésille pas une fois compressé » / « A avec le master, B sans… « Au même volume » les met au même niveau » | `ScreenHelp.kt:109`, `:117`, `mixer/MasterPro.kt:404-405` | Export et Master pro · Z2 | **Non vérifiable au banc** (à l'oreille) ; A/B : **jamais vérifiée** même en surface |
| 99 | « Fichier final : … la partie exportée, les fondus et silences au début et à la fin… » | `ScreenHelp.kt:125` | Export · Z2 | T4 E b : **non** (la feuille Share ignore longueur et format choisis dans Mix) ; T3 F a : en partie (format M4A devenu WAV en Pro, écran 70) |
| 100 | « Avec les envois, chaque piste garde sa réverbe et son écho. Sans, ils sont dans des fichiers à part » / « remises ensemble, les pistes redonnent le mix » | `ScreenHelp.kt:129`, `MixerScreen.kt:566` | Export (pistes) · Z2 | T3 F b : **non** (export par pistes : un seul fichier remix, écran 53) ; T3 F d : en partie (« Share the 2 files » n'en cite qu'un, écran 56) ; « redonnent le mix » : **jamais vérifiée** (il existe `host::stems_add_up_to_the_mix`, pas de siège) |
| 101 | « En M4A et en FLAC, le limiteur reste en place : sans lui, les pointes seraient écrêtées. Choisis WAV pour t'en passer. » / « En WAV, fichier 32 bits flottants : rien n'est écrêté. » | `MixerScreen.kt:564`, `:567` | Export · Z2 | **Jamais vérifiée** |
| 102 | « Le Mix et ses effets : chaque réglage a un nom et une unité » (promesse implicite des aides « ? » du Mix) | `mixer/*` (aides) | Mix · Z2 | T4 B a : en partie (« Gauche 63 » sans unité, macros « 0 » sans échelle, faders sans « dB », écrans 8/33/4) |

### J. Export et partage (`export/*`, `projects/*`) : Z2

| n° | Promesse | Fichier:ligne | Fonction · zone | Vérifiée ? |
|---|---|---|---|---|
| 103 | « Prêt à partager » / « Tout est prêt : aucun solo oublié, aucune piste coupée. » | `export/ShareSheet.kt:412`, `export/ExportCheck.kt:300` | Export · Z2 | T3 F b : **non** (piste voix du remix coupée non signalée, écran 58) ; T4 E b : **non** (fausse alerte « song has changed » juste après l'export, écran 12) ; T4 D d : en partie. **Revient** |
| 104 | « C'est prêt : [fichier], une copie est aussi dans Musique › BenStudio. » | `export/ShareSongUi.kt:88`, `MixerScreen.kt:672` | Export · Z2 | T3 A d : tenue (fichiers trouvables dans Music › BenStudio) ; T3 D c, T3 C a, T3 C c : **non** (fichier annoncé « 3 s » pour 0:58 / 0:11 / 3:12, écrans 60/59/52) : classé « banc » après coup, donc **non tranché pour l'appli** |
| 105 | « Mes exports : retrouver et renvoyer un fichier » | `export/MyExports.kt:92` | Export · Z2 | T4 E b : en partie (tous « Send » sans marque, « 1 KB » pour un WAV de 16 s, écran 48) ; T3 F d : **non** (l'ancien export reste proposé, écran 48) |
| 106 | « Envoyer / sauvegarder (.zip) » puis « Projet importé : « %s » » | `projects/ProjectsScreen.kt:330`, `projects/ProjectsViewModel.kt:252` | Sauvé (projet) · Z3 | envoi : T3 F d tenue, T4 C a tenue ; **import : jamais vérifié** (T3 F d et T4 C a : « sélecteur de fichier du banc » : limite du banc, deux tours de suite) |
| 107 | « Vidéo prête : touche pour la partager. » | `export/video/VideoExport.kt:212` | Export · Z2 | T3 F d : tenue |
| 108 | « Rien ne part tout seul : « Partager » ouvre le menu d'Android et tu choisis à qui l'envoyer. » / « Ce fichier ne peut pas être relu. Envoie-le tel quel pour le faire réparer : il n'est jamais effacé tout seul. » | `report/BugReportSheet.kt:229`, `ProjectsScreen.kt:295` | Export · Sauvé · Z2/Z3 | **Jamais vérifiée** |
| 109 | « Projets retrouvés dans la sauvegarde du téléphone : N. Leurs sons n'y étaient pas… » | `rhythm/ProjectOpening.kt:208` | Sauvé · Z3 | **Jamais vérifiée** (hors banc) |

### K. Enregistrement (`record/*`) : Z4

| n° | Promesse | Fichier:ligne | Fonction · zone | Vérifiée ? |
|---|---|---|---|---|
| 110 | « Remettre en rythme » / « Remise en rythme » (recaler une prise) | `record/RecordBar.kt:219`, `synth/PianoRoll.kt:438` | Enregistrer · Notes · Z4/Z5 | T4 B b : tenue (« Put in time » et « As played ») ; T3 E c : en partie |
| 111 | « Décompte » (une mesure de clics avant le départ) / « Prêt ? » | `record/RecordChoice.kt:110`, `RecordBar.kt:151`, `Glossary.kt:61` | Enregistrer · Z4 | T3 E c : tenue ; T4 C b : tenue (décompte 2 mesures se retrouve) ; « on ne l'entend jamais dans l'export » (métronome) : **jamais vérifiée** |
| 112 | « Rien n'a été joué pendant la prise. » → « Garder ce que je viens de jouer » | `RecordBar.kt:104`, `:127`, `AppRoot.kt:2395` | Enregistrer · Z4 | T4 A b : en partie (les notes se récupèrent ainsi, écran 33) ; T4 C b et T4 C d : le bouton n'a pas été vu |

### L. MIDI et réglages (`midi/*`, `settings/*`) : Z3/Z6

| n° | Promesse | Fichier:ligne | Fonction · zone | Vérifiée ? |
|---|---|---|---|---|
| 113 | « Branche un clavier en USB (il se connecte tout seul) ou cherche-le en Bluetooth. » / « il est reconnu tout seul » | `midi/MidiScreen.kt:89`, `midi/ControllerUi.kt:107` | MIDI et MiniLab · Z3 | T4 C b : en partie (« cet appareil ne gère pas le MIDI » juste au-dessus ; l'apprentissage attend sans fin, écran 8) |
| 114 | « Profil : [contrôleur] — prêt à jouer, rien à régler » / « [clavier] connecté — prêt à jouer » | `ControllerUi.kt:114`, `midi/MidiViewModel.kt:610-612` | MIDI et MiniLab · Z3 | **Jamais vérifiée avec un profil connu** (T4 C b : clavier simulé, tableau des potards pas à jour, écran 21 : en partie) |
| 115 | « Potards automatiques : les 8 premiers potards règlent tout seuls l'écran affiché… » / « le son ne saute jamais » | `midi/KnobHintUi.kt:236`, `:156` | MIDI et MiniLab · Z3 | **Jamais vérifiée** |
| 116 | « Branché à une autre machine en MIDI, l'appli peut suivre son tempo : page MIDI, « Suivre l'horloge MIDI ». » / « Taper en rythme » | `ScreenHelp.kt:49`, `ui/TempoSheet.kt:215` | Tempo · Z3 | T4 C b : le réglage se retrouve (en partie) ; le suivi réel : **non vérifiable au banc** ; « Taper en rythme » : **jamais vérifiée** |
| 117 | « « Rapide » répond au doigt sans délai. Si le son craque, passe à « Sûr »… fini les craquements. » / « Écran toujours allumé pendant la lecture » | `SettingsScreen.kt:127`, `:94` | Réglages · Z3 | **Non vérifiable au banc** |
| 118 | « Le « ? » au bout du bandeau étiquette l'écran : un toucher pour voir, un autre pour cacher. » | `SettingsScreen.kt:219` | Aide · Z6 | T3 D d : en partie (étiquettes mal posées, écran 17) ; T3 D c : en partie (écran 48) ; T4 C c : **non** (étiquettes empilées, cachées, écran 53). **Revient** |
| 119 | « Une nouvelle version est prête. » / « Mise à jour prête : touche pour l'installer. » | `AppRoot.kt:3841`, `update/InstallResultReceiver.kt:67` | (hors carte) | **Jamais vérifiée** (hors banc) |

### M. Remix et découpe (`remix/*`) : Z1

| n° | Promesse | Fichier:ligne | Fonction · zone | Vérifiée ? |
|---|---|---|---|---|
| 120 | « « → Pad » copie les mesures choisies sur un pad libre (**jamais sur un son que ton rythme joue**), « → Live » sur un pad live libre… » / « Coche les bouts… envoie-les sur les pads (un par pad), dans le Live, dans « Garder » ou sur une piste » | `remix/RemixScreen.kt:604`, `ScreenHelp.kt:88`, `:78-80` | Découpe du Remix · Z1 | T3 E d : **non** (« Voz 1 » remplace « Tom medio », écran 16) ; T3 B c : **non** (mode phrases : aucune tranche sélectionnable, écran 22) ; T4 A c : tenue (Kick gardé, voix sur pads 2 à 9). Corrigée entre les tours ; c'est le « bouts qui écrasent un pad » de la liste du propriétaire. « Garder » et « sur une piste » : **jamais vérifiés** |

Trois promesses du Remix vérifiées et tenues, hors tableau pour rester à 120 : « C'est prêt : le modèle est sur ton téléphone. Choisis une chanson. » (`RemixScreen.kt:404`, T3 B c tenue) ; « Un nouveau projet à son tempo et dans sa tonalité, avec ses pistes prêtes. » (`remix/RemixFromHome.kt:135`, T3 F b en partie ; la tonalité : jamais vérifiée) ; « Les pistes séparées seront effacées. Pour les retrouver, il faudra séparer à nouveau. » (`RemixScreen.kt:769`, jamais vérifiée).

## 3. Bilan chiffré

Sur les 120 lignes (verdict le plus sévère vu aux tours 3-4 quand plusieurs sièges ont regardé) :
- **vérifiées au moins une fois aux tours 3-4 : 88** : tenue 11 (n° 11, 25, 30, 40, 49, 63, 86, 94, 106, 107, 111), en partie 24, **non tenue 53** ;
- **jamais vérifiées : 26** (n° 3, 14, 17, 18, 26, 29, 44, 45, 47, 48, 51, 54, 58, 61, 70, 74, 81, 83, 90, 91, 101, 108, 109, 114, 115, 119) ; et 20 autres lignes ont au moins un morceau jamais vérifié (par exemple n° 7 « pile en rythme », 60 après correction, 67 « Partager envoie ta perf », 76 après correction, 80 « Versions », 95 « l'appli s'arrête avant et le dit », 100 « redonnent le mix », 106 l'import, 120 « Garder » et « sur une piste ») ;
- **non vérifiables par le banc tel qu'il est (jugement à l'oreille, hors appareil) : 6 lignes entières** (n° 41, 42, 72, 84, 98, 117) plus des morceaux des n° 7, 65, 70, 116, 119, auxquelles s'ajoute l'import de projet (n° 106 : « sélecteur du banc », deux tours de suite).

Les promesses **non tenues qui reviennent** (même promesse, deux tours de suite ou plusieurs sièges du même tour) : 17 groupes : n° 2/59 (« je vérifie » du tuto), 9 (« un style en une touche »), 10 (renommer), 15 (« mots de tous les jours »), 21/35 (la mélodie enregistrée « rien n'a été joué »), 22/23/33 (écouter tout le morceau ou depuis une partie), 43 (« Écouter l'exemple » : rien ne joue), 55 (« ton kit est déjà chargé »), 64 (« Annuler » du pack), 67 (« Tout couper »), 73 (pads vides à l'ouverture), 75 (« ■ pour arrêter »), 78 (case Remix du Live muette), 85 (la recherche de sons et ses exemples), 97 (mesures du master figées), 103 (« Prêt à partager »), 118 (étiquettes « ? »).

Le taux « promesses tenues » du critère d'arrêt (≥ 90 %) ne peut pas se lire dans les rapports : chaque siège ne note qu'une promesse, la sienne (24 lignes `promesse:` au tour 3 : 1 « oui », 21 « en partie », 2 « non » ; 20 au tour 4 : 1 « oui », 18 « en partie », 1 « non »). Ramené aux 120 promesses écrites dans l'appli, on est à 11 tenues sur 88 regardées.

Remarque de méthode : le « chasseur de promesses » n'a été joué qu'une fois (T3 F a, 40 lignes, 10 défauts) ; sa reprise au tour 4 (F c) et le siège « automatisations, effets par piste » (F d) n'ont pas tourné. Les seuls sièges qui lisent les aides « ? » (T3 D d, T4 C c) sont ceux qui ont trouvé le plus de promesses fausses (6 et 7 défauts « promesse » chacun) : ce profil rapporte plus que la moyenne.

## 4. Promesses jamais vérifiées, par zone

**Z1 · pads, Pad Live, défis, remix**
- n° 69 « Tenir : il joue tant que ton doigt reste sur le pad » (`ScreenHelp.kt:37`).
- n° 70 « Un seul à la fois » (groupes de pads) (`ScreenHelp.kt:41`).
- n° 67 « Partager envoie ta perf » (`ScreenHelp.kt:29`) et n° 74 « Un seul fichier avec tes effets, prêt à envoyer » (`PerfShare.kt:264`).
- n° 76 « La colonne Remix joue un passage » : non re-vérifiée depuis la correction `b3ee0d6` (`ScreenHelp.kt:59`).
- n° 77 « Lancer à : Tout de suite » (`LiveFx.kt:606`).
- n° 80 « l'ancien morceau reste dans « Versions » » (`Glossary.kt:349`).
- n° 81 « Joue aux notes : elles sonnent sur l'Acid 303… la boucle se cale sur la mesure » (`LiveCellMenu.kt:97`).
- n° 83 « Couches : la batterie sonne plus vivante » (`ScreenHelp.kt:135`).
- n° 86 « tu le retrouves dans « Changer le son », pour tous tes projets » (`MySoundLibrary.kt:91`).
- n° 91 « les sons intégrés sont tous là, prêts à servir » (`LibraryScreen.kt:450`).
- n° 120 envoyer un bout dans « Garder » ou « sur une piste » (`ScreenHelp.kt:88`).
- À l'oreille (banc à équiper) : n° 72 (glisser sur le pad, roulement, boucles calées), n° 84 (« sans changer la hauteur »), n° 70 (attaque et fin), n° 65 (« tout revient »).

**Z2 · exports et master**
- n° 95 « Si le son devait s'écraser, l'appli s'arrête avant et le dit » (`ScreenHelp.kt:102`).
- n° 100 « remises ensemble, les pistes redonnent le mix » (`MixerScreen.kt:566`).
- n° 101 limiteur en M4A/FLAC, « rien n'est écrêté » en WAV (`MixerScreen.kt:564`, `:567`).
- n° 98 A/B « Au même volume » (`ScreenHelp.kt:117`, `MasterPro.kt:404`) : même pas vérifiée en surface.
- n° 108 « Rien ne part tout seul » (`BugReportSheet.kt:229`).
- n° 93 l'automation « bouge tout seul » (`AutomateSheet.kt:101`) ; n° 94 « ne jouer que dans certaines parties » (`Glossary.kt:235`) ; n° 44 « Écouter sans puis avec » (`GuidePanel.kt:514`).
- À l'oreille : n° 41 (« au même volume »), n° 98 (plafond de crête).

**Z3 · navigation, prise en main, sauvegarde**
- n° 3 « Écouter une démo » et « Ta démo est prête » (`Welcome.kt:195`, `:139-143`).
- n° 14 « Aucun projet pour l'instant » (`LaunchChoice.kt:313`).
- n° 17 « changer de mode à tout moment avec la pastille » (`ModeChoice.kt:87-88`).
- n° 18 « Rythme de départ : prêts à tourner » (`AppRoot.kt:286`).
- n° 24 « deux touches sur un bloc l'ouvrent » en Pro (`GuideDialog.kt:63`) ; n° 29 « Ce guide se retrouve dans Plus › Aide » (`GuideDialog.kt:84`).
- n° 26 « Appui long : plus fort ou plus doux » et « deux touchers remettent le potard à zéro » (`GuideDialog.kt:74-75`).
- n° 40 « Il t'attend dans Sons › Remix » (`guides/common.json:429`) ; n° 45 « se ramène d'un geste » (`TrackSheet.kt:361`).
- n° 47 « Créer tout de suite avec les choix conseillés » (`WizardScreen.kt:217`) ; n° 48 « L'assistant prendra ses sons dedans » (`WizardScreen.kt:462`).
- n° 106 import d'un projet .zip (`ProjectsViewModel.kt:252`) : limite du banc ; n° 108 « jamais effacé tout seul » (`ProjectsScreen.kt:295`) ; n° 109 projets retrouvés dans la sauvegarde du téléphone (`ProjectOpening.kt:208`) ; n° 119 mise à jour prête.
- MIDI : n° 114 « Profil : … prêt à jouer, rien à régler » (`ControllerUi.kt:114`), n° 115 « Potards automatiques » et « le son ne saute jamais » (`KnobHintUi.kt:236`, `:156`), n° 116 « Taper en rythme » (`TempoSheet.kt:215`). Hors banc : n° 117 (latence, écran allumé).

**Z4 · enregistrement**
- n° 7 « pile en rythme » (`Welcome.kt:214`) : à l'oreille, mais vérifiable par une mesure du banc (décalage de la prise).
- n° 111 « on ne l'entend jamais dans l'export » (métronome) (`Glossary.kt:61`).

**Z5 · tutos, synthé, notes**
- n° 51 « La coupure s'ouvre toute seule », « Filtres… tout seuls », « Souffle montant » (`SongGuide.kt:89`, `:109`, `:98-99`).
- n° 53 « Écrire une mélodie pour moi » (`TrackAdd.kt:66`).
- n° 54 les ~20 descriptions de styles « jamais / toujours » (`StyleRecipes.kt:521`, i18n `model.json`).
- n° 58 « « Accords » pose la triade d'un toucher », « les accords suivent tout seuls » (`tutos/dnb.json:227-228`).
- n° 60 « Écouter mon morceau » après sa correction (`TutoPanel.kt:320`) ; n° 61 « Bravo, ton morceau est fini ! » (`TutoPanel.kt:816`) ; n° 62 « Montre-moi » (`TutoPanel.kt:755`).
- n° 90 « créé sur l'appareil, sans internet », « rien à télécharger » (`GenerateScreen.kt:136`, `RealInstruments.kt:89`).
- À l'oreille : n° 42 « Ça sonne déjà juste ».

**Z6 · textes, langues, accessibilité**
- Rien de jamais vérifié : les promesses de cette zone (n° 15 « mots de tous les jours », n° 118 étiquettes « ? ») ont toutes été vérifiées, et trouvées fausses à chaque tour.
