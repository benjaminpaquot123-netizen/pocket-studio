# Recherche : flûte, voix et sifflement pour le style trap

Date : 2026-11-24. Rôle : chercheur (aucun code touché, aucun son commité).
Modèle donné par Benjamin : Future, « Mask Off » (prod. Metro Boomin). On s'en sert comme **référence d'oreille** seulement : rien n'en est tiré, ni son ni mélodie.

Méthode : chaque licence ci-dessous a été lue sur la page citée le 2026-11-24, sauf mention « non vérifié ». **Rien n'a été écouté** (pas d'oreille ici) : les « qualités estimées » viennent des descriptions, des noms de fichiers et de ce que l'appli fait déjà. Une écoute par `./scripts/live-bench.sh` reste à faire avant tout choix définitif.

## 0. Ce qu'on a déjà (lu dans le dépôt)

- La flûte actuelle n'est **pas un instrument à elle** : c'est le haut de l'instrument « Bois » (`scripts/packs/instruments.json`), `FluteSusVib.sfz` de VSCO 2 CE, **seulement à partir du sol 4 (note 67)**, 2 couches, tenue **bouclée sur 3 s**, relâchement 0,4 s, 32 Mo pour trois instruments. Dessous, on tombe sur la clarinette puis le basson.
- VSCO 2 CE a pourtant quatre jeux de flûte (noms de fichiers lus, dépôt `sgossner/VSCO-2-CE`, commit 4403009 du 2020-08-04) : `susvib` (13 notes), `expvib` (14 notes, vibrato expressif), `susNV` (19 notes, sans vibrato), `stac` (48). On n'en utilise qu'un.
- Les voix « Go ! », « Ready »… des pads Fête viennent du Kenney Voiceover Pack (CC0), déjà dans `scripts/party/synth_party.py`.
- Le sifflet du pack Fête est un sifflet à roulette synthétisé, pas un sifflement humain.

Cause probable du « pas agréable » (à confirmer à l'oreille) : boucle courte qui fige le vibrato, registre coupé en bas, aucune réverbe ni filtre propres au lead, et un relâchement long qui bave à 150 BPM.

## 1. Le son visé (repères)

- « Mask Off » : ré mineur, environ 150 BPM (Hooktheory, Medium « Weekly Billboard Theory »). La flûte du disque est un échantillon de « Prison Song » (Tommy Butler, comédie musicale *Selma*, années 70 ; Vice, Wikipédia). **Donc interdit pour nous** : c'est un son de disque.
- Ce qui fait ce timbre (connaissance générale, non sourcée par une page) : flûte traversière seule, registre médium (à peu près ré 4 à la 5, pas l'aigu perçant), souffle audible, vibrato naturel qui n'arrive que sur les notes tenues, phrases courtes liées, couleur de vieille bande (aigus adoucis, légère saturation), réverbe de salle.

## 2. Flûte lead : trois options classées

| Rang | Option | Lien | Licence (lue) | Qualité estimée | Taille | Effort |
|---|---|---|---|---|---|---|
| **1 (recommandée)** | **Vraie « Flûte solo » à partir de VSCO 2 CE déjà en place** : jeux `susvib` + `expvib` + `susNV`, toute la tessiture, sans clarinette ni basson dessous | https://github.com/sgossner/VSCO-2-CE (dossier `Woodwinds/Flute/`) | CC0-1.0 (fichier LICENSE ; déjà dans `THIRD_PARTY.md`) | Bonne : vraie flûte, vibrato joué par le musicien ; c'est la matière la plus proche du modèle | environ 10 à 20 Mo en OGG pour 46 notes (estimation) | **Faible** : une entrée de plus dans `instruments.json`, même outil `instrument_pack.py`, zéro nouvelle licence |
| 2 | Flûte (et flûte alto, plus chaude) de l'université de l'Iowa, enregistrements d'après 2012 | https://theremin.music.uiowa.edu/MIS.html | « may be downloaded and used for any projects, without restrictions » (pas une licence CC formelle) | Très bonne prise (24 bits / 96 kHz, stéréo, chambre sourde donc son sec, pp/mf/ff avant 2011) ; vibrato de la flûte **non précisé sur la page** | gros à la source (zips 24/96), 15 à 30 Mo une fois réduit | Moyen : découper, accorder, boucler, écrire le SFZ nous-mêmes |
| 3 | Synthèse (sinus + souffle filtré + vibrato retardé) ou ocarina / flûtes à bec `SusVib` de VCSL | https://github.com/sgossner/VCSL (`Aerophones/Edge-blown Aerophones/`) | CC0-1.0 (VCSL, commit c1ea7bc du 2026-01-14) ; synthèse : à nous | Moyenne : joli et pur mais ce n'est plus une flûte traversière | 0 Mo (synthèse) ou 5 à 10 Mo | Faible à moyen |

Écartés :
- **Philharmonia** (https://philharmonia.co.uk/resources/sound-samples/) : « must not be sold or made available “as is” (i.e. as samples or as a sampler instrument) ». Un pack d'instrument dans une appli au dépôt public, c'est exactement ça : refus.
- **Sonatina Symphonic Orchestra** : licence non relue aujourd'hui (non vérifié), et VSCO fait déjà mieux pour la flûte : pas retenue.
- Boucles « type Mask Off » de Looperman : rejouent la mélodie du disque : refus.

Réglages recommandés pour l'option 1 (points de départ, à régler à l'oreille) :
- Tessiture utile do 4 à do 6 ; mélodies écrites autour de ré 4 à la 5.
- Vélocité douce = `susNV` (sans vibrato), vélocité forte = `susvib` ; `expvib` pour les notes longues de fin de phrase. Mieux qu'un vibrato ajouté par LFO ; s'il en faut un : 5 à 5,5 Hz, ±10 à 15 centièmes, qui n'arrive qu'après 250 à 400 ms.
- Ne plus boucler à 3 s : garder la note entière (ou boucle longue, 2 s au moins de fondu), sinon le vibrato tourne en rond et ça s'entend.
- Attaque 30 à 50 ms, relâchement 150 à 250 ms (0,4 s bave à 150 BPM).
- Filtres : coupe-bas vers 250 Hz, passe-bas doux vers 6 à 8 kHz (c'est lui qui enlève le côté « perçant »), petit creux vers 2,5 à 3,5 kHz si ça agresse.
- Réverbe en envoi : plaque ou salle, 1,8 à 2,5 s, pré-délai 20 à 40 ms, 20 à 25 % ; coupe-bas avant la réverbe.
- Delay : croche pointée ou noire, niveau bas (10 à 15 %), filtré ; rien sur les notes rapides.
- Couleur « vieille bande » : saturation légère, un rien de pleurage (0,3 à 0,6 Hz, quelques centièmes). Mono ou presque : le lead reste au centre.

## 3. Voix de rappeur (ad-libs, phrases) : trois options classées

| Rang | Option | Lien | Licence (lue) | Qualité estimée | Taille | Effort |
|---|---|---|---|---|---|---|
| **1 (recommandée)** | **La voix de Benjamin, enregistrée avec REC**, plus un préréglage « Ad-lib trap » (doublage, accordage, delay, réverbe) et une courte liste de mots à dire | dans l'appli | À lui : aucun risque, aucune mention | La seule qui sonne « vrai rappeur sur son beat » ; en français si on veut ; plusieurs voix = plusieurs prises décalées et transposées | 0 Mo dans l'appli | Moyen : une chaîne d'effets et un petit guide, pas de nouveau contenu |
| 2 | **Banque d'ad-libs en vraies voix CC0**, choisie pièce par pièce | Freesound, filtre « Creative Commons 0 » : `unfa` 155893 « Yeah 5 » (pack « Short vocal samples », FLAC, 2012), `riippumattog` 872174 « oh yeah 2 (Deep Male voice) » (2026-09-28), `melisandepope` 572602 « brrrt » (pack FunVox, 2021), `elliottdj` 685910 « Skrrt », `bogenseeberg` 475444, `Artmasterrich` 345424, `JapanYoshiTheGamer` 384981, `jackieAZ` 400630 « Kick it! » ; plus Kenney Voiceover Pack https://kenney.nl/assets/voiceover-pack (CC0, v1.0, 90 fichiers, déjà chez nous) | CC0 lue sur les pages 155893, 872174, 572602 et Kenney ; les autres sont sorties sous le filtre CC0 mais leur page n'a pas été ouverte | Inégale : voix d'amateurs, micros variés ; bien une fois traitées (hauteur, saturation, delay). Peu de vraies phrases rappées en CC0 : surtout des cris et des mots | 20 à 40 sons courts : 1 à 2 Mo | Moyen : écouter et vérifier chaque page, un par un (pas de téléchargement en masse) |
| 3 | **Voix de synthèse libre**, fabriquée une fois par un script, pas dans le téléphone | Kokoro-82M https://huggingface.co/hexgrad/Kokoro-82M ; Chatterbox https://huggingface.co/ResembleAI/chatterbox | Kokoro : Apache-2.0, poids compris (« This is an Apache-licensed model »), v1.0 du 2025-01-27, 54 voix, 8 langues. Chatterbox : MIT, Multilingual V3 (date non indiquée), 23 langues dont le français, réglage d'émotion, filigrane inaudible dans chaque fichier | Voix nette mais **parlée** : ça ne rappe pas et ça n'a pas l'énergie d'un ad-lib. Utile en effet « voix robot » découpée sur la grille | sons générés : 1 à 2 Mo ; modèles hors appli | Moyen à fort : script de génération, tri à l'oreille |

Comparaison courte : vraie voix libre = bon grain mais choix maigre et origine à contrôler ; synthèse = licence nette mais sans âme ; voix de Benjamin = la meilleure et la plus sûre. **Conseil : 1 en premier, complétée par une petite banque 2 pour que le style sonne dès l'ouverture, sans micro.**

À éviter (juridique) :
- Voix clonée ou imitée d'un vrai rappeur (droit à la voix et à l'image, même sans extrait). Le clonage de Chatterbox ne sert **que** sur la voix de Benjamin.
- Tout extrait de chanson, a cappella ou « tag » de producteur, même court ; tout son dont la page dit venir d'un disque. Exemple vu : Freesound `Bronxio` 456121, marqué CC0, contient un « ah yeah » tiré d'un disque de Run-DMC : refus. Une étiquette CC0 posée par un inconnu ne suffit pas : lire la description.
- Toute licence NC ; les packs « royalty-free » payants ou gratuits (Splice, Cymatics, Looperman…) : ce n'est pas une licence libre et la redistribution dans un dépôt public est interdite.
- Piper (https://github.com/OHF-Voice/piper1-gpl) : moteur GPL-3.0, et **chaque voix a sa propre licence** selon ses données d'entraînement (page non lue en détail) : pas par défaut.
- Sons Freesound à l'origine douteuse : `qubodup` 67672 (tiré d'une vidéo de l'armée américaine, domaine public annoncé) et `pbrechler` 250097 (remontage du son d'un autre) : à laisser de côté, il y a mieux.

Réglages d'ad-libs (pratique courante, points de départ) : coupe-bas 150 à 200 Hz, compression forte, saturation légère ; prise doublée ou triplée, les doubles écartés à gauche et à droite et 5 à 8 dB sous la voix principale ; une copie une octave plus bas pour l'effet « voix grave » ; accordage rapide sur la gamme du morceau pour les phrases chantonnées ; delay noire ou croche lancé seulement sur le dernier mot ; réverbe courte (0,8 à 1,2 s) ; placement dans les trous du beat, souvent sur le 4ᵉ temps ou juste après la caisse claire.

## 4. Sifflement : trois options classées

| Rang | Option | Lien | Licence | Qualité estimée | Taille | Effort |
|---|---|---|---|---|---|---|
| **1 (recommandée)** | **Sifflement de synthèse jouable** : un sifflement humain est presque un sinus pur ; on ajoute un filet de souffle, un vibrato 5 à 6 Hz qui arrive tard, un glissé de 30 à 60 ms entre les notes | à nous (même famille que `scripts/party/synth_party.py`, ou un préréglage du synthé) | À nous | Bonne et régulière sur toutes les notes ; c'est ce que font beaucoup de prods | 0 Mo | Faible |
| 2 | Benjamin siffle dans REC | dans l'appli | À lui | La plus vivante, dépend du siffleur et du micro | 0 Mo | Nul côté contenu |
| 3 | Sifflements enregistrés CC0 (mélodies, pas des notes séparées), ou l'ocarina `SusVib` de VCSL comme « sifflement jouable » | Freesound sous filtre CC0 : `PhonosUPF` 494518, `oscarboyle` 790454, `createwithfreecopyright` 869386 ; VCSL `Ocarina, Typical/Sustains/SusVib` | CC0 (filtre Freesound, pages non ouvertes ; VCSL : fichier LICENSE) | Moyenne : mélodies figées, prises d'amateurs ; l'ocarina est propre mais plus « flûte » | 1 à 8 Mo | Moyen |

Écarté : Pianobook « Human Whistle » (https://www.pianobook.co.uk/packs/human-whistle/) : format Kontakt et aucune licence de réutilisation écrite sur la page. Aucun jeu de notes sifflées en CC0 n'a été trouvé (ni VCSL ni Freesound).

## 5. Ce que font les producteurs trap (repères)

- Flûte : souvent un instrument échantillonné ou une boucle achetée, puis vieillie (bande, bit-crusher léger selon Soundtrap), passe-bas, réverbe d'envoi avec coupe-bas avant elle (SoundOracle), delay croche pointée ramené à la noire quand la phrase est chargée (Native Instruments). Chez Travis Scott, la flûte est courte et se fond derrière le couplet (LANDR).
- La mélodie tient en 2 ou 4 mesures, en mineur, peu de notes, beaucoup de silences : c'est la 808 et les charlestons qui remplissent.
- Voix : ad-libs doublés et écartés, delays lancés sur un mot, voix grave à l'octave, accordage audible.
- Sifflement : sinus ou vraie prise, très réverbéré, souvent en réponse à la mélodie principale.

Sources lues : https://www.native-instruments.com/blogs/blog/how-to-make-a-trap-beat ; https://blog.landr.com/how-to-make-a-travis-scott-type-beat ; https://blog.soundtrap.com/recording-woodwinds/ ; https://soundoracle.net/blogs/soundoracle-net-blog/5-essential-tips-for-better-reverb ; https://www.vice.com/en/article/a-brief-history-of-that-kickass-flute-sample-on-futures-mask-off ; https://hooktheory.com/theorytab/view/future/mask-off. Aucune ne donne de chiffres de vibrato : ceux de ce document sont des points de départ d'usage, pas des citations.

## 6. Ce qui n'est pas vert

- Rien n'a été écouté : toutes les qualités sont estimées.
- Freesound : 3 pages ouvertes sur une quinzaine citées ; chaque son retenu doit être relu (licence, description, origine) avant d'entrer.
- Non vérifiés : la licence de Sonatina, la date de Chatterbox Multilingual V3 (un guide le dit « retiré en août 2026 », non confirmé), la dernière version de Piper et la licence de ses voix, le vibrato des flûtes de l'Iowa, la taille exacte des jeux de flûte.
- La description du timbre de « Mask Off » (§1) et les réglages viennent de la pratique courante, pas d'une page citée.
- Incident : en listant les noms de fichiers de VSCO 2 CE, une commande a rapatrié par erreur une partie de ses sons (350 Mo) dans le dossier temporaire de la session ; supprimé, rien n'est entré dans le dépôt.
