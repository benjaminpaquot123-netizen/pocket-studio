# Outils existants pour fabriquer des sons neufs à partir de l'« ADN » Essentia

Date : 2026-11-27. Recherche seule : rien n'a été installé ni essayé, aucun son écouté.
Base de départ : `ESSENTIA-BASE.md` (10 414 sons, 88 familles) et `RECHERCHE-TIMBRE.md`
(recherche automatique d'une recette sur notre propre voix de batterie).
Le juge reste l'oreille : sur 30 essais, l'auditeur ne repère pas notre son parmi les vrais plus de
14 fois. Les pourcentages ci-dessous sont la **chance estimée que le son soit indiscernable** ; la
critique (§7) montre que la règle « ≤ 14 sur 30 » doit être revue avant la première écoute.

## En bref

1. **Rien de prêt à l'emploi ne fait « ADN de famille → son neuf ».** Les outils d'« appariement »
   (sound matching) visent à recopier *un* son précis, et la plupart sont des codes de recherche
   figés, à entraîner sur GPU. On n'en prend pas un tel quel.
2. **Ce qui existe et sert tout de suite** : un optimiseur boîte noire éprouvé (pycma, déjà notre
   méthode), un synthé libre bien plus riche que notre voix et pilotable en Python (Surge XT par
   `surgepy`), un hôte de rendu et d'effets (pedalboard, DawDreamer), et une mesure de
   ressemblance perceptive (CLAP + distance de Fréchet / KAD).
3. **Le plus gros gain ne vient pas d'un outil mais d'une matière première** : pour le piano, les
   cordes, les cuivres et la batterie acoustique, il existe des enregistrements **CC0** de bonne
   qualité. Partir d'un vrai enregistrement libre, puis le façonner vers l'ADN de la famille
   (effets réglés par l'optimiseur), bat de loin toute synthèse pure. C'est là que l'estimation
   bouge le plus : de 40 % à environ 60 % pour le piano.
4. **La génération par modèle (texte → audio)** est utile pour les effets et les textures, faible
   pour la voix (les modèles le disent eux-mêmes), et ses poids ne sont jamais vraiment libres
   (licence « communauté » ou non commerciale). À garder en appoint, pas en base.
5. **Ce qui n'est pas prouvé** : aucune étude publiée ne fait notre test exact (un son isolé neuf
   contre de vrais sons de packs). Les nouvelles estimations sont donc un jugement appuyé sur des
   preuves voisines, pas une mesure. La première écoute à l'aveugle tranchera.

## 1. Tableau des outils

Vérifié le 2026-11-27 sur la page officielle (PyPI, dépôt du projet ou carte du modèle).
« CPU » = tourne dans notre conteneur Linux sans GPU. « Vivant » = activité dans les 12 derniers mois.

### 1.1 Recherche de réglages (optimisation, appariement)

| Outil | Ce qu'il fait | Licence | Dernière version | CPU | Vivant | Verdict |
| --- | --- | --- | --- | --- | --- | --- |
| pycma (`cma`) | CMA-ES, optimiseur boîte noire de référence | BSD-3-Clause | 4.6.0, 2026-11-15 | oui | oui | **à prendre** (c'est déjà la méthode de `RECHERCHE-TIMBRE.md`) |
| Nevergrad | boîte à optimiseurs (dont CMA, et des choix mixtes continus + discrets) | MIT | 1.0.12, 2025-04-23 (dépôt actif 2026-11) | oui | oui | **appoint** : utile quand la recette mélange des curseurs et des choix (forme d'onde, type de filtre) |
| Syntheon | inférence de réglages pour Vital (Dexed en cours) | Apache-2.0 | aucune version publiée, dépôt actif 2026-06 | oui (inférence) | à peine | non : pas de résultats chiffrés publiés, vise un son précis |
| SpiegeLib | banc d'appariement sur VST | MIT | 0.0.4, 2020-07-09 | oui | non | non : abandonné |
| synth-permutations (article ISMIR 2025 sur Surge XT) | inversion de Surge XT par « flow matching » | code de recherche | 2025 | entraînement GPU | recherche | non pour l'instant ; preuve que Surge XT est un terrain d'appariement sérieux |
| DiffMoog | synthé soustractif différentiable + encodeur d'appariement | code de recherche (article CC BY 4.0) | 2024 | entraînement GPU conseillé | recherche | non : l'article lui-même présente l'appariement par gradient comme un problème encore ouvert |
| InverSynth, FlowSynth | inférence de réglages par réseau | recherche (FlowSynth : MIT, figé 2021) | 2019-2021 | — | non | non |

### 1.2 Synthés et hôtes de rendu pilotables en Python

| Outil | Ce qu'il fait | Licence | Dernière version | CPU | Vivant | Verdict |
| --- | --- | --- | --- | --- | --- | --- |
| Surge XT + `surgepy` | synthé hybride complet (onde, table d'ondes, FM, filtres, effets), liaison Python dans le dépôt (`src/surge-python`, à compiler) | GPL-3.0 | stable 1.3.4 ; nightly 2026-11-27 | oui, plus rapide que le temps réel | oui | **à prendre** pour basses, synthés, pads, effets |
| Dexed | modèle FM 6 opérateurs (type DX7) | GPL-3.0 | 1.1.1 (journal du dépôt), nightly, dépôt actif 2026-11 | oui (par hôte VST3) | oui | **appoint** : cloches, pianos électriques, basses FM |
| Vital | synthé à tables d'ondes | GPL-3.0 | dépôt figé 2023-05 | oui (par hôte VST3) | non | non : Surge couvre le même terrain et vit |
| DawDreamer | hôte Python : VST, Faust, automation, MIDI, rendu hors ligne | GPL-3.0 | 0.9.0, 2026-08-12 (Linux x86_64 / aarch64, Python 3.11-3.14) | oui | oui | **à prendre** si on charge Dexed ou des modèles Faust |
| pedalboard | effets (réverbe, compresseur, distorsion, chorus, filtres, hauteur) + chargement VST3 | GPL-3.0 | 0.9.26, 2026-10-07 | oui | oui | **à prendre** pour la chaîne d'effets réglée par l'optimiseur |
| sfizz | lecteur d'instruments SFZ (formats des banques libres ci-dessous) | BSD-2-Clause | dépôt actif 2025-03 | oui | moyen | **à prendre** pour lire les banques CC0 |
| Faust | langage DSP, bibliothèques de modèles physiques (cordes, percussions) | licence du compilateur (dépôt « NOASSERTION ») | dépôt actif 2026-11 | oui | oui | appoint, via DawDreamer |

### 1.3 Synthèse différentiable et neuronale

| Outil | Ce qu'il fait | Licence | Dernière version | CPU | Vivant | Verdict |
| --- | --- | --- | --- | --- | --- | --- |
| DDSP | synthé harmonique + bruit différentiable, transfert de timbre | Apache-2.0 | 3.7.0, 2023-05-25 (dépôt touché 2026-10) | inférence oui, entraînement lent | faible | plus tard, pour cordes / cuivres monophoniques |
| torchsynth | synthé modulaire sur GPU, génération massive | Apache-2.0 | 1.0.2, 2022-08-19 | oui mais pensé GPU | non | non |
| RAVE | encodeur-décodeur audio temps réel | **CC BY-NC 4.0** (fichier LICENSE) | v2.4 bêta (2.3.1 sur PyPI, 2023-12) | inférence oui ; entraînement 8 à 32 Go de GPU | oui | non : entraînement impossible ici, licence non commerciale |

### 1.4 Génération à partir de texte ou d'un exemple

| Outil | Ce qu'il fait | Licence du code / des poids | Dernière version | CPU | Verdict |
| --- | --- | --- | --- | --- | --- |
| Stable Audio Open 1.0 (stable-audio-tools) | texte → audio stéréo 44,1 kHz, jusqu'à 47 s ; entraîné sur des sons Freesound et FMA sous CC0, CC BY, CC Sampling+ | code MIT / poids « Stability AI Community License » (accès après acceptation sur Hugging Face) | outils 0.0.20, 2026-05-20 ; modèle 2024 | possible mais lent (≈ 1 milliard de paramètres) | **appoint** pour effets et textures ; la carte du modèle dit : meilleur en effets qu'en musique, **pas de voix réaliste** |
| Stable Audio Open Small | même famille, jusqu'à 11 s, conçu pour CPU Arm (annonce : 11 s générées en moins de 8 s sur un téléphone) | poids « Stability AI Community License » | 2025 | oui | **appoint** pour boucles de batterie et riffs courts ; mêmes limites |
| MusicGen (audiocraft) | texte → musique | code MIT / poids **CC BY-NC 4.0** | 1.3.0, 2024-06-03 | lent | non : vise des morceaux, pas des sons isolés |
| AudioLDM / AudioLDM 2 | texte → audio | poids non commerciaux (CC BY-NC-SA selon une source secondaire, à vérifier) | 0.1.1, 2023-04 | lent | non : dépassé par Stable Audio Open |

### 1.5 Mesure de ressemblance perceptive

| Outil | Ce qu'il fait | Licence | Dernière version | CPU | Verdict |
| --- | --- | --- | --- | --- | --- |
| CLAP (modèle `larger_clap_music_and_speech`) | « empreinte » d'un son ; distance entre sons, tri par texte | poids Apache-2.0 ; code LAION CC0-1.0 | `laion-clap` 1.1.7, 2025-05-04 | oui | **à prendre** : juge automatique de « ça sonne comme la famille » |
| fadtk | distance de Fréchet (FAD) entre deux lots de sons, avec plusieurs empreintes (CLAP, MERT, EnCodec…) | MIT | 1.1.0, 2025-05-23 | oui | **à prendre** au niveau famille (notre lot contre la famille) |
| kadtk (Kernel Audio Distance) | variante sans hypothèse gaussienne, plus stable sur petits lots | MIT | 1.1.0, 2025-03-16 (dépôt actif 2026-09) | oui | **à prendre** : nos familles ont souvent 20 à 400 sons, trop peu pour une FAD fiable |
| auraloss | pertes spectrales multi-résolutions | Apache-2.0 | 0.4.0, 2023-04-21 | oui | appoint (stable, pas besoin de plus récent) |
| pyloudnorm | sonie EBU R128 | MIT | 0.2.0, 2026-01-04 | oui | appoint (contre-mesure du LUFS, absente aujourd'hui) |
| matchering | « mastering par référence » : aligne niveau, courbe de fréquences, crête et largeur stéréo sur une référence | GPL-3.0 | 2.0.6, 2022-10 (dépôt actif 2026-10) | oui | appoint pour les boucles, avec une référence fabriquée par nous |

### 1.6 Banques d'enregistrements libres (matière première)

| Banque | Contenu | Licence | État | Verdict |
| --- | --- | --- | --- | --- |
| VCSL (Versilian Community Sample Library) | instruments variés : claviers, percussions, cordes, vents, effets | **CC0** | dépôt actif 2026-01, SFZ fourni | **à prendre** |
| VSCO 2 Community Edition | orchestre de chambre (cordes, bois, cuivres, percussions) | **CC0** | 1.1.0, figé 2021 (stable) | **à prendre** |
| Virtuosity Drums (sfzinstruments) | batterie acoustique multi-couches | **CC0** | dépôt actif 2026-04 | **à prendre** |
| Splendid Grand Piano (index sfzinstruments) | piano à queue | domaine public (selon l'index) | — | à prendre, licence à relire au téléchargement |
| Salamander Grand Piano | piano à queue, 16 couches de vélocité | **CC BY 3.0** (attribution obligatoire) | figé 2022 | à prendre, crédit dans `THIRD_PARTY.md` |
| VocalSet | 10,1 h de chant, 20 chanteurs, 5 voyelles, techniques variées | **CC BY 4.0** | 2018, stable | **à prendre** pour voix (voyelles, tenues, « ooh / aah ») |
| NSynth | 305 979 notes de 1 006 instruments | CC BY 4.0 | stable | non comme matière : **16 kHz seulement**, trop sourd pour des échantillons |
| Freesound (filtre CC0) | sons isolés de toute sorte | CC0 son par son | vivant | appoint, licence à garder son par son |

## 2. Ce que disent les preuves publiées

- **Optimisation boîte noire sur un vrai synthé** : l'étude de Yee-King, Fedden et d'Inverno
  (IEEE TETCI, 2018) compare escalade, algorithme génétique et réseaux sur Dexed : le meilleur
  réseau retrouve de près le son visé dans **25 % des cas**, et les recherches heuristiques sont
  plus lentes. Leçon : recopier *un* son précis est dur ; viser la *fourchette d'une famille*
  (notre cas) est bien plus facile, et c'est ce que montrait déjà `RECHERCHE-TIMBRE.md` (de 3 à 19
  mesures sur 19 dans la fourchette sur des familles inventées).
- **Surge XT comme terrain d'appariement** : Hayes, Saitis et Fazekas (ISMIR 2025) inversent Surge
  XT et battent les méthodes de régression sur les mesures de reconstruction audio. Pas de test
  d'écoute publié. Leçon : Surge XT est assez riche pour qu'on y cherche des sons, et sa liaison
  Python est déjà utilisée en recherche.
- **Mesure perceptive** : Gui et coll. (ICASSP 2024, outil fadtk) montrent qu'avec une bonne
  empreinte (dont CLAP) et une bonne référence, la FAD suit mieux les notes d'écoute humaines.
  Limite publiée en 2025 : sur la musique libre, la FAD peut mal suivre les préférences. Leçon :
  CLAP + FAD/KAD sert de **filtre** (écarter les ratés), pas de juge final.
- **Génération par modèle** : les cartes de Stable Audio Open (1.0 et Small) disent elles-mêmes
  « meilleur en effets et enregistrements de terrain qu'en musique » et « pas de voix réaliste ».
  Aucun chiffre d'écoute réel contre généré n'est publié sur la carte.
- **Batterie générée** : DrumGAN puis StyleWaveGAN (DAFx 2022) publient des notes d'écoute (MOS)
  en progrès, mais **aucun test « vrai contre généré » qui conclut à l'indiscernable** n'a été
  trouvé pour des coups isolés.
- **Enregistrements libres** : pas besoin de preuve d'écoute : ce sont de vrais instruments. Le
  risque restant est la **chaîne de production** (égalisation, compression, réverbe, saturation)
  des sons de packs, que l'ADN Essentia mesure justement (brillance, parts de bande, platitude,
  sonie, attaque).

## 3. Chaîne recommandée, famille par famille

Socle commun à toutes les familles :

1. **Cible** : les percentiles p10 à p90 de `essentia-familles.tsv` (mêmes réglages 2048 / 512 /
   44,1 kHz) + l'empreinte CLAP moyenne de la famille, calculée une fois sur la machine qui a les
   packs. On garde des nombres, jamais d'audio.
2. **Source** : selon la famille, notre voix maison, Surge XT, Dexed, ou un enregistrement CC0.
3. **Recherche** : pycma règle la recette (et la chaîne d'effets pedalboard) pour entrer dans la
   fourchette de la famille ; Nevergrad seulement s'il y a des choix discrets.
4. **Filtre** : un classifieur « vrai contre fabriqué » (C2ST, §7) sur les finalistes ; on rejette tout son hors fourchette Essentia ou trop loin en CLAP ; on mesure le lot
   entier contre la famille avec kadtk (KAD) ; on rejette aussi tout son *trop proche* d'un son
   précis de la famille (garde-fou anti-copie de `RECHERCHE-TIMBRE.md`).
5. **Juge** : écoute à l'aveugle (30 essais).

| Famille | Source | Recherche et façonnage | Pourquoi |
| --- | --- | --- | --- |
| Batterie électronique (kick, caisse claire, clap, charleys, percussions, 808) | notre voix `MeasuredDrums` | pycma + filtre CLAP / KAD ; couche de transitoire tirée de VCSL / Virtuosity (CC0) pour les caisses et claps | la voix tient déjà la fourchette ; le gain vient du tri perceptif et d'attaques plus vraies |
| Batterie acoustique (toms, ride, crash, shakers, percussions jouées) | Virtuosity Drums, VCSL percussions (CC0) via sfizz | égalisation, compression, saturation, réverbe réglées par pycma vers l'ADN | vrais enregistrements ; seul le traitement est à ajuster |
| Basses (one-shots, 808, basses de kit) | Surge XT ; 808 par notre voix | pycma sur ~20 à 40 curseurs de Surge (onde, filtre, enveloppes, saturation) | Surge a les filtres et saturations des basses de packs, notre voix non |
| Synthés (lead, pluck, arp, accords, synth) | Surge XT ; Dexed pour les timbres FM | pycma + Nevergrad (choix de table d'ondes, d'algorithme FM) ; effets Surge / pedalboard | même raison, et les accords demandent la polyphonie que Surge a |
| Pads, nappes | Surge XT | pycma, avec chorus et réverbe longue | sons lents et flous : les plus faciles à faire passer |
| Cloches, claviers électriques | Dexed (FM) | pycma | la FM est la source naturelle de ces timbres |
| Effets (montées, impacts, souffles) | Surge XT ; Stable Audio Open en appoint | pycma ; tri CLAP | c'est le point fort reconnu de Stable Audio Open |
| Boucles de batterie | nos coups validés + motifs MIDI ; groove humain appris du Groove MIDI Dataset (CC BY 4.0) | rendu hors ligne ; niveau et courbe alignés par pedalboard / matchering sur une référence fabriquée | un motif juste + des coups qui passent = une boucle qui passe ; le tempo est connu par construction |
| Boucles mélodiques et pistes de kit | les sources ci-dessus + motifs MIDI | idem | elles héritent de la qualité de leurs sons ; plusieurs couches à réussir à la fois |
| Piano et claviers acoustiques | VCSL Keys, Splendid Grand (CC0 / domaine public), Salamander (CC BY) via sfizz | chaîne d'effets réglée vers l'ADN (filtre « lo-fi », saturation, réverbe, sonie) | vrais pianos ; les packs les traitent souvent fort, ce que l'ADN mesure |
| Cordes | VSCO 2 CE, VCSL (CC0) | idem ; superposition de pupitres pour les ensembles | vrais enregistrements, mais peu de pupitres et d'articulations |
| Cuivres, guitares | VCSL, VSCO 2 CE (CC0) | idem | matière plus maigre (peu de guitares) |
| Voix (voyelles, tenues, « chops ») | VocalSet (CC BY 4.0), Freesound CC0 | découpe, hauteur et formants (pedalboard ; WORLD par `pyworld` en appoint), effets réglés vers l'ADN | vraies voix ; on fabrique des chops neufs, pas des phrases |
| Voix (phrases chantées avec paroles) | aucune bonne source libre | — | les générateurs le disent : pas de voix réaliste ; la synthèse de chant libre demande une banque de voix et un GPU |

Ce qu'on **ne fait pas** :
- entraîner ou affiner un modèle (Stable Audio Open, RAVE, DDSP) sur les sons achetés : risque de
  mémoriser et de recracher des morceaux de sons, ce qui casse la règle « jamais un son copié » ;
- utiliser des poids non commerciaux (MusicGen, RAVE, AudioLDM) comme base : possible en usage
  personnel, mais ça bloque toute sortie publique de l'appli ;
- installer un outil d'appariement de recherche figé (SpiegeLib, Syntheon, InverSynth).

## 4. Nouvelle estimation par famille

**Chance que le son soit indiscernable** (repérage proche du hasard), après critique (§7). Attention :
en choix à deux, même un son indiscernable ne réussit « ≤ 14 sur 30 » que 43 % du temps (§7, point 1).
Jugement appuyé sur les preuves de la section 2, **pas une mesure**.

| Famille | Avant | Après | Fourchette | Justification courte |
| --- | ---: | ---: | --- | --- |
| Batterie électronique | 85 % | **88 %** | 85-92 % | la voix tient déjà la fourchette ; gain modeste par tri CLAP / KAD et attaques CC0 |
| Batterie acoustique | 85 % (comptée avec la batterie) | **85 %** | 75-90 % | vrais enregistrements CC0, mais un seul kit libre multi-couches : peu de variété |
| Basses | 75 % | **82 %** | 75-88 % | Surge apporte filtres et saturations ; les basses se jugent surtout au grave et à l'attaque, que l'ADN mesure |
| Synthés (lead, pluck, arp, accords) | 75 % | **80 %** | 72-86 % | Surge + Dexed couvrent les timbres de packs ; risque : effets maison des packs difficiles à retrouver |
| Pads | 75 % | **83 %** | 75-88 % | sons lents et texturés, les plus tolérants |
| Effets | non estimé | **78 %** | 70-85 % | Surge d'abord ; Stable Audio Open seulement si Benjamin crée le compte (son point fort déclaré) |
| Boucles de batterie | 70 % | **78 %** | 70-85 % | coups validés + groove humain ; tempo exact par construction |
| Boucles mélodiques, pistes de kit | 70 % | **55 %** | 45-65 % | plusieurs couches à réussir à la fois ; écrire une phrase musicale crédible reste le maillon faible, et aucun outil de composition n'est prévu |
| Piano, claviers acoustiques | 40 % | **62 %** | 50-72 % | vrais pianos libres, mais un piano par banque, très répandus donc reconnaissables ; l'ADN ne mesure ni stéréo, ni queue de réverbe, ni bruits de relâchement |
| Cordes | 40 % | **68 %** | 55-78 % | vrais enregistrements CC0, mais pupitres et articulations limités |
| Cuivres, guitares | 40 % | **40 %** | 30-50 % | presque aucune guitare libre de qualité ; cuivres VSCO / VCSL seulement |
| Voix : voyelles, tenues, chops | 40 % | **50 %** | 40-60 % | vraies voix CC BY, mais VocalSet est du chant classique sec, loin des chops pop traités |
| Voix : phrases avec paroles | 40 % | **35 %** | 25-45 % | aucune source libre crédible ; on conseille de ne pas en livrer |

Ce qui ferait baisser ces chiffres : un traitement « signature » des packs que l'ADN ne capte pas
(stéréo, réverbes longues, automation), ou un auditeur qui juge à la phrase musicale plutôt qu'au
timbre (boucles).

## 5. Coût de mise en place (estimé, rien n'a été lancé)

| Brique | Travail | Disque | Calcul |
| --- | --- | --- | --- |
| pycma, Nevergrad, pyloudnorm, auraloss | `pip` dans un venv isolé | < 50 Mo | négligeable |
| Surge XT + `surgepy` | compilation CMake depuis le dépôt (`pip install src/surge-python`) | ≈ 1 Go pendant la compilation | 20 à 40 min une fois ; rendu plus rapide que le temps réel |
| pedalboard | `pip` (roue binaire) | ≈ 50 Mo | négligeable |
| DawDreamer + Dexed (VST3 Linux) | `pip` + binaire Dexed | ≈ 200 Mo | négligeable |
| sfizz | paquet système ou compilation | ≈ 100 Mo | négligeable |
| CLAP + kadtk / fadtk | PyTorch CPU + poids | ≈ 2 à 3 Go | quelques secondes par son sur CPU (à mesurer) |
| Banques CC0 / CC BY | téléchargement | de quelques centaines de Mo à quelques Go (à mesurer) | — |
| Surge XT, Python récent | les roues prévues ne couvrent que CPython 3.7-3.11 (manylinux x86_64) : sinon compilation, à essayer | — | — |
| Stable Audio Open (appoint) | `stable-audio-tools` + poids | 3 à 5 Go | **compte Hugging Face et acceptation de licence nécessaires** (action de Benjamin) ; lent sur CPU |

Ordre conseillé : socle de mesure (CLAP + kadtk) d'abord, puis banques CC0 pour piano et cordes
(plus gros gain), puis Surge XT pour basses et synthés, enfin Stable Audio Open seulement pour les
effets si le reste plafonne.

## 6. Points de licence à respecter

- Les sons **produits** par Surge, Dexed, pedalboard ou DawDreamer ne sont pas couverts par leur GPL ;
  la GPL ne concerne que le code, utilisé seulement hors ligne.
- CC0 : rien à faire. CC BY (Salamander, VocalSet, Groove MIDI Dataset) : crédit dans
  `THIRD_PARTY.md` si un son dérivé est livré.
- Poids « Stability AI Community License » : gratuits pour l'usage non commercial, et aussi commercial
  sous 1 M$ de chiffre d'affaires annuel (stability.ai/license) ; relire avant toute sortie publique.
- Aucun son acheté ne sort de la machine qui les a ; seuls des nombres (percentiles, empreintes
  moyennes) entrent dans le dépôt.

## 7. Critique à l'aveugle (sous-agent neuf, 2026-11-27)

Relecture par un sous-agent qui n'a pas écrit ce document, chargé de le contredire.

**Licences revérifiées (3/3 justes)** : poids de Stable Audio Open Small sous « Stability AI Community
License » ; CLAP `larger_clap_music_and_speech` en Apache-2.0 ; VCSL en CC0 1.0 avec SFZ. Erreur
relevée et corrigée au §6 : la licence Stability permet aussi l'usage commercial sous 1 M$ de revenu.

**Affirmations revérifiées (3/3 justes)** : `surgepy` existe et se construit depuis `src/surge-python`
(scikit-build, CMake) ; les cartes Stable Audio Open disent mot pour mot « not able to generate
realistic vocals » et « better at sound effects and field recordings than music » ; Yee-King et coll.
2018 : un BLSTM retrouve de près le son visé dans 25 % des cas sur Dexed. Risque ajouté au §5 : roues
Python de Surge seulement jusqu'à 3.11.

**Critiques retenues**

1. **Plafond statistique (la plus grave).** Si le test fait choisir entre deux sons (un vrai, le
   nôtre), un son *parfaitement* indiscernable n'obtient 14 repérages ou moins sur 30 que dans
   **42,8 %** des cas (loi binomiale 30 ; 0,5 ; recalculé). Les chiffres du §4 ne peuvent donc pas
   se lire comme « chance de réussir ce test » : ils se lisent comme **« chance que le son soit
   indiscernable »** (repérage proche du hasard). Avant la première écoute, il faut fixer la forme
   du test : soit plusieurs vrais sons à côté du nôtre, soit un seuil « pas mieux que le hasard »
   (par exemple ≤ 20/30 en choix à deux : au-delà, p ≈ 0,02), sinon même la perfection échoue une fois sur
   deux.
2. **Cible mesure par mesure.** Entrer dans chaque fourchette p10-p90 n'empêche pas une combinaison
   atypique, et viser l'empreinte CLAP *moyenne* donne des sons fades. L'optimiseur exploitera les
   failles de la mesure. Correction de la chaîne : viser la distance au plus proche *groupe* de vrais
   sons (pas à la moyenne), et ajouter l'outil du point 3.
3. **Outil manquant : test de discrimination automatique (C2ST).** Un petit classifieur « vrai
   contre fabriqué » sur les empreintes CLAP et Essentia, entraîné sur la machine qui a les packs
   (seuls des nombres sortent), est l'équivalent direct de l'écoute à l'aveugle : précision proche
   de 50 % = indiscernable pour la machine. À placer à l'étape 4 du socle, avant KAD.
4. **Familles surestimées** : piano, cuivres et guitares, chops de voix, boucles mélodiques, effets
   (dépend d'un compte pas encore créé). **Corrigé au §4.**
5. **Coût de calcul** : une empreinte CLAP à chaque essai de CMA-ES = des milliers de passages CPU
   par son. Parade : l'optimiseur vise d'abord les mesures Essentia (rapides), CLAP et le C2ST ne
   trient que les finalistes.

**Non retenu** : recaler toute la colonne sur 43 %. On a préféré changer le sens de la colonne
(point 1) : mélanger qualité du son et forme du test rendrait les familles incomparables.
