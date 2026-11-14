# Une appli comme BenStudio sur smartphone et tablette : comment les gens l'utilisent (2026-11-01)

Recherche demandée par Benjamin. Les études publiques sur les applis musicales mobiles sont rares et souvent anciennes (2012 à 2014) ; il n'existe pas de chiffres publics fiables sur l'orientation ou la durée des séances. Le reste vient des règles officielles d'Android, des recherches en ergonomie du pouce et de la pratique des applis du même genre. Ce qui est déduit (et non mesuré) est signalé.

## 1. Deux appareils, deux façons de jouer
| | Smartphone | Tablette |
|---|---|---|
| **Quand** | Partout, par petits moments : transports, canapé, idée qui arrive, jouer avec les enfants (déduit) | Posée : bureau, studio, scène ; séances plus longues (déduit) |
| **Comment on le tient** | Souvent à une main (environ la moitié des usages selon les observations de Steven Hoober), le pouce fait tout ; à deux mains pour jouer des pads | À deux mains ou posée sur un support ; dix doigts possibles |
| **Orientation** | Portrait pour naviguer et choisir des sons ; paysage pour jouer des pads ou du clavier (déduit) | Paysage surtout ; écran partagé avec une autre appli |
| **Points forts** | Toujours dans la poche ; micro intégré pour enregistrer tout de suite ; vibration au toucher | Grand écran : grille, pistes et effets visibles ensemble ; meilleur pour monter un morceau et pour la scène |
| **Points faibles** | Petit écran, doigts qui cachent ce qu'on touche, coins durs à atteindre | Lourde, poignet fatigué en mains, souvent sans 4G |

Les études universitaires disent que la tablette est un très bon outil pour **apprendre** et pour la **production amateur** grâce à son grand écran. Sur téléphone, les capteurs (vibration, gyroscope) servent pour le jeu des pads. Android a longtemps été en retard sur iOS pour la musique, en partie à cause de la latence ; c'est un point où BenStudio doit être irréprochable.

## 2. Les règles officielles d'Android pour les grands écrans
- **Classes de taille de fenêtre** (largeur) : compacte (< 600 dp), moyenne (600–840), étendue (840–1200), large (1200–1600), très large (≥ 1600). Elles changent en direct (rotation, écran partagé, pliable qui s'ouvre). Il ne faut pas coder « si tablette » mais selon la place disponible.
- **Trois niveaux de qualité** : « prêt » (mises en page qui s'adaptent), « optimisé » (toutes les tailles et configurations, clavier, souris, stylet), « différencié » (l'appli tire parti du grand écran, y compris en écran partagé). Viser le niveau « différencié » pour le Live, le Remix et la Structure.
- **Conseil Google** : concevoir d'abord pour la largeur « étendue », puis simplifier pour le téléphone.

## 3. Le pouce et les doigts
- Zone confortable en éventail en bas de l'écran, du côté de la main ; coins du haut difficiles. Actions principales dans le tiers du bas, barres fixes en bas.
- Taille de cible : au moins 9 mm environ pour le pouce (≈ 48 dp, notre règle), avec de l'espace entre les cibles ; les pads plus grands (64 dp et plus).
- Ce qu'on touche ne doit pas être caché par le doigt : retour visuel au-dessus ou à côté (notre loupe de découpe va dans ce sens).

## 4. Le son et les branchements
- **Casques Bluetooth** : latence imprévisible sur Android, souvent 100 à 200 ms et plus selon le téléphone (environ 30 à 50 ms avec certains codecs aptX à faible latence). Inutilisable pour jouer des pads au doigt. Il faut le détecter, prévenir gentiment (« Avec un casque Bluetooth, le son arrive en retard : branche un casque à fil ou joue sur le haut-parleur ») et proposer la compensation de latence pour l'enregistrement.
- **Casque filaire, carte son USB, clavier MIDI USB** (MiniLab 3) : la configuration des musiciens qui s'y mettent sérieusement ; hot-plug, choix de l'entrée et de la sortie, mesure de latence.
- **Faible latence** : AAudio exclusif via Oboe (déjà fait) ; mode basse latence affiché ; test OboeTester sur le téléphone de Benjamin (plan d'Audit).

## 5. Ce que ça veut dire pour BenStudio (recommandations)
1. **Le smartphone d'abord pour jouer et capter** : Pad Live, Remix et enregistrement au micro en un toucher ; pads et boutons principaux dans la zone du pouce ; mode paysage pour les pads et le clavier ; vibration au toucher.
2. **La tablette pour construire** : en largeur « étendue », afficher côte à côte la grille des briques et les pads, ou la Structure et le Tableau des pistes ; panneau d'effets toujours visible ; dix doigts sur les pads ; support du clavier physique (raccourcis : espace = lecture, Ctrl+Z…), de la souris et du stylet.
3. **Écran partagé et pliables** : l'appli garde le son et son état quand la fenêtre change de taille ou que le pliable s'ouvre ; tester toutes les classes de taille (le plan d'Audit le prévoit).
4. **Bluetooth** : détection et message clair, compensation de latence pour les prises, conseil « casque à fil ».
5. **Continuité** : un projet commencé sur le téléphone se continue sur la tablette (export du projet, plus tard une synchro) ; export des pistes séparées et du MIDI vers un logiciel sur ordinateur.
6. **Séances courtes sur téléphone** : sauvegarde instantanée, reprise exacte (« Continuer »), rien de perdu si un appel arrive (déjà prévu dans le chaos Android de l'Audit).

## 6. Ajouts au plan d'Audit
- Chaque écran contrôlé dans les 5 classes de taille, en portrait et en paysage, en écran partagé, et sur un pliable qui s'ouvre en pleine lecture.
- Jeu des pads mesuré à une main (zone du pouce) et à deux mains ; tablette à dix doigts.
- Casque Bluetooth branché pendant le Live : message, compensation, pas de plantage.
- Clavier physique et souris sur tablette.

Sources : [Survol des interfaces musicales mobiles (Aalto)](https://sopi.aalto.fi/?p=668) · [Tablette et post-production audio (KTH, 2013)](https://www.csc.kth.se/utbildning/kandidatexjobb/medieteknik/2013/sammanfattning/tavakolian_poya_OCH_weinl_johan.html) · [Synthés sur iPad, enquête (TCD, 2014)](https://publications.scss.tcd.ie/theses/diss/2014/TCD-SCSS-DISSERTATION-2014-037-ABSTRACT.pdf) · [Comment les musiciens utilisent l'iPad (TechRadar)](https://techradar.com/news/mobile-computing/tablets/how-musicians-are-using-the-ipad-921391/2) · [Applis et musiciens (PRS for Music)](https://www.prsformusic.com/m-magazine/features/app-happy) · [Sondage plateformes mobiles (CDM)](https://cdm.link/mobile-music-platform-survey-results-plus-beatmaker-midi-export/) · [Qualité des applis grand écran (Android)](https://developer.android.com/docs/quality-guidelines/large-screens-app-quality) · [Classes de taille de fenêtre (Android)](https://developer.android.com/develop/ui/views/layout/use-window-size-classes) · [Grands écrans, design (Android)](https://developer.android.com/design/ui/large-screens?authuser=4) · [Zone du pouce (HCI International)](https://2014.hci.international/2013paperawards109.php) · [Taille des cibles au pouce (Springer)](https://link.springer.com/content/pdf/10.1007/978-3-642-39241-2_6.pdf) · [Coins difficiles (MobileHCI 2013)](https://mobilehci2013.medien.ifi.lmu.de/program/174.php) · [Latence Bluetooth sur Android (Android Authority)](https://androidauthority.com/smartphone-bluetooth-latency-973198/) · [Latence Bluetooth (SoundGuys)](https://www.soundguys.com/android-bluetooth-latency-22732/) · [Latence des applis musicales sur téléphone (Musicianwave)](https://www.musicianwave.com/?p=615239)
