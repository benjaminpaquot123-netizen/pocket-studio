# Des sons modernes sans toucher aux morceaux : étude et plan d'essai (2026-11-12)

Demande de Benjamin : « les sons sonnent années 90-2000, même dans les styles modernes, surtout les synthés ». Son idée : garder les morceaux tels qu'ils sont (notes, structure, recettes) et ne changer que les sons, pour comparer. Ajouts : « il n'y a pas des gens qui publient gratuitement de super basses et mélodies de tous les styles ? », puis « vérifie aussi les options payantes, si ça en vaut la peine ».

Étude seulement : rien n'a changé dans l'appli, le moteur, le banc ou les tests. Aucun son n'a été téléchargé dans le dépôt.

## En bref (à lire à voix haute)

1. Le côté « rétro » a trois causes précises. La batterie est entièrement synthétisée, à la manière des boîtes à rythmes des années 80. Les packs téléchargeables ont été générés en demandant littéralement « rave des années 90, TR-909, Juno-106 ». Et les synthés sont les sons stars de 1991 à 2000 (Hoover, supersaw, piano DX7), sans tables d'ondes ni compression multibande, qui font le son d'aujourd'hui.
2. On peut changer les sons sans toucher aux notes : l'appli choisit déjà ses sons par rôle (kick, basse, lead…), il suffit de lui donner d'autres sons pour les mêmes rôles.
3. Il existe des sons modernes qu'on a vraiment le droit de mettre dans une appli payante, gratuitement : Signature Sounds (CC0), les boucles WaivOps (crédit obligatoire), Freesound en CC0, et surtout des sons qu'on fabrique nous-mêmes avec le synthé gratuit Surge XT. Presque tous les packs « gratuits » connus l'interdisent (Splice, Cymatics, 99Sounds…).
4. Pour les mélodies et basses toutes faites : oui, il y en a plein en accès gratuit, mais seulement pour tes propres morceaux, jamais pour les mettre dans l'appli. Deux exceptions utiles : des accords (licence MIT) et des rythmes de batterie joués par des humains (Google, crédit obligatoire).
5. Plan conseillé : un essai à 0 € sur jump up, house et techno. On commence par une batterie moderne, puis on rend les synthés plus modernes. Tu écoutes l'avant / après en extraits de 30 secondes, avec les mesures et la charge du processeur à côté. Payer un sound designer (environ 500 à 1 500 $) ne vaut le coup qu'après cet essai, si le gratuit ne suffit pas.

---

## 1. D'où viennent les sons aujourd'hui

Un morceau fait par l'assistant (« Partir d'un style »), sans pack téléchargé, ne contient **aucun échantillon** : tout est calculé en direct par le moteur. Trois natures de sons existent dans l'appli, et elles ne se remplacent pas de la même façon :

- **synthétisé** : le moteur fabrique le son note par note (on change ses réglages, ou on le remplace par un autre moteur) ;
- **SoundFont** : une banque d'instruments General MIDI des années 90-2000 (GeneralUser GS, FluidR3, MuseScore General). **L'assistant ne s'en sert jamais** : elles servent seulement quand tu choisis toi-même un instrument dans Synthé › Source du son ;
- **échantillon** : un enregistrement court, joué tel quel (les packs téléchargés, les instruments réels, les sons de fête).

| Famille | Sans pack (par défaut) | Avec les packs téléchargés |
|---|---|---|
| Kick | synthétisé : « kick studio » (accordé à la tonalité en house et en techno), kick dur écrêté en hard techno | échantillon du pack du style, qui remplace le kick studio |
| Caisse claire, clap | synthétisés (DaisySP, clap maison) | échantillon du pack |
| Charleys, cymbales | synthétisés (DaisySP) ; la ride et le crash sont la cymbale du kit raccourcie | échantillon du pack (aucune ride en pack) |
| Percussions | synthétisées (rim, cloche, shaker, conga) | échantillon du pack |
| 808, sub | voix 808 dédiée (trap, drill, phonk, jersey, reggaeton), « rumble » en techno, « Sub pur » en bass music | la 808 synthétisée reste devant |
| Basse | synthé maison ou Plaits (modèles « basse analogique », « log drum »…) | échantillon de basse joué au clavier, seulement en house (Juno) et en techno |
| Lead | synthé maison (Supersaw, Hoover, Arpège) ou Plaits (« Lead filtré », voix FM façon DX7) | presque jamais remplacé |
| Nappe, accords, pluck | synthé maison (Nappe, Pluck) ou Plaits (pianos FM DX7) | vrais instruments (piano, cordes) seulement en lo-fi, pop, boom bap, liquid DnB, amapiano |
| Montée, effets | bruit filtré du synthé | les sons d'effets des packs ne sont pas branchés |
| Voix | Plaits « voix robot » | les voix des packs ne sont pas branchées |

Sources : `engine/src/main/cpp/core/DrumKit.h` (« 16 synthesized drum voices (808/909 flavour), no samples »), `project-model/…/StarterWizard.kt` (`soundsFor`, `withStudioVoices`), `app/…/library/SoundLibraryViewModel.kt` (`soundCategories`), `project-model/…/StyleRecipes.kt` (37 styles), `scripts/packs/` (packs).

**Les trois styles de l'essai, piste par piste (aujourd'hui)** :
- **Jump up** (174 BPM) : batterie synthétisée (kick non accordé, caisse DaisySP, clap maison) ; basse « growl » (scie + carré désaccordés, balayage de filtre calé sur la double croche, couche médium saturée) et un sub ; lead « Hoover » à 7 voix ; nappe à 2 scies ; voix Plaits.
- **House** : kick studio accordé, clap et charleys du kit, shaker et conga ; basse « ronde » (scie + sub) ; accords « Piano FM » (le E.PIANO 1 du DX7, 1983) ; lead « Arpège » en carré.
- **Techno** : kick studio accordé (~44 Hz) et rumble ; basse Plaits « analogique » ; accords « Pluck » en carré ; lead Plaits « filtré ».
- **Hard techno** (témoin pour le processeur) : kick dur, rumble, lead Supersaw 7 voix doublé d'un carré à l'octave, nappe Hoover en rafales.

**Les notes**, elles, viennent de cellules écrites à la main dans les recettes et développées par l'appli (`MelodyBuilder`, `BassPatterns`, `AcidGenerator`). Elles ne dépendent pas des sons : on peut changer l'un sans l'autre.

## 2. Pourquoi ça sonne rétro, concrètement

### 2.1 Ce qu'on entend, et pourquoi

1. **La batterie est une boîte à rythmes des années 80 recréée.** Les voix de DaisySP imitent la TR-808 et la TR-909 : c'est leur raison d'être. Une production d'aujourd'hui empile des échantillons : un kick avec un claquement et un sub, des claps en plusieurs couches, des charleys enregistrés, souvent saturés et « compressés à fond ». Nos batteries sont aussi **presque mono** : côté/centre de −19 à −24 dB sur la batterie seule, alors que les productions récentes étalent charleys et percussions.
2. **Les packs ont été demandés « rétro ».** Les mots donnés au générateur Stable Audio contiennent « early 90s Belgian and Dutch rave », « Roland TR-909 », « Juno-106 », « DX7 » (`scripts/packs/styles.json`). Le générateur a fait ce qu'on lui demandait. De plus, ce modèle léger sort du 44,1 kHz avec une qualité inégale.
3. **Les synthés sont les sons vedettes de 1983 à 2000.** Le Hoover vient de 1991, la supersaw de la JP-8000 de 1997, le piano FM du DX7 de 1983. Le travail de fin octobre (« Son 8 ») les a rendus fidèles et propres : supersaw façon JP-8000, filtre qui suit la note, attaques, largeur. Mais fidèles à leur époque. Ce qui fait le son des années 2010-2020 manque :
   - **les tables d'ondes** (Serum, 2014 ; Vital, 2020) : des formes d'onde riches qui se transforment pendant la note. Notre synthé n'a que scie, carré, triangle et sinus ;
   - **la compression multibande « OTT »**, utilisée sur presque toutes les basses, leads et nappes d'aujourd'hui. Elle remonte les détails faibles et les aigus : le son paraît « devant, brillant ». On ne l'a pas ;
   - **le travail du son en couches** : distorsion, puis OTT, puis réenregistrement. Nous avons une couche par piste au plus, pour épargner le processeur.
4. **Les nappes et les leads de house et de techno sont filtrés sombres.** C'est ce que montrent les mesures ci-dessous.

### 2.2 Les mesures (banc du 2026-11-12, chaque voix rendue seule)

Méthode : `scripts/song-bench.sh --styles jump_up,house,techno,hard_techno`, puis chaque voix rendue seule avant le master (`ps_render --no-limiter --solo`), mesurée sur toute sa durée de jeu. Rendus effacés après mesure, rien n'est commité. « Air » = la part du son au-dessus de 10 kHz : c'est ce qui donne le brillant. **Repère** : une dent de scie brute, sans filtre, a un air de −18 à −24 dB selon la note (calcul exact sur ses harmoniques).

| Voix | Air au-dessus de 10 kHz | Grave-médium 150–500 Hz face à 1–4 kHz | Lecture |
|---|---|---|---|
| Techno, nappe | **−39 dB** | **+10,5 dB** | sombre et boueuse : 15 à 20 dB plus sombre qu'une scie brute |
| Techno, lead | **−39 dB** | −2,2 dB | sombre |
| House, nappe (piano FM) | **−42 dB** | +4,5 dB | sombre |
| House, lead | −24 dB | −1,3 dB | correct |
| House, basse | −68 dB | +24,9 dB | basse « ronde » très sombre : on l'entend mal sur un téléphone (présence 1–4 kHz à −17 dB) |
| Jump up, lead (Hoover) | −20 dB | −2,3 dB | déjà brillant : le rétro y est le timbre du Hoover, pas le filtre |
| Jump up, nappe | −32 dB | +3,4 dB | assez sombre |
| Hard techno, lead (Supersaw) | −19 dB | +2,7 dB | brillant |
| Batteries (les 4 styles) | −16 à −21 dB | — | correct, mais presque mono (côté/centre −19 à −24 dB) |

Le mix entier, lui, tient ses cibles :
- sonie de la partie forte à −8,4 LUFS, crête à −1,2 dBTP ;
- côté/centre à −9,8 dB ;
- mouvement du lead de ±30 %.

Le problème n'est donc **pas le mixage ni le volume : c'est le timbre des sons**.

## 3. Ce qu'on a le droit d'utiliser, gratuitement

**La règle qui revient partout** : « gratuit » ou « royalty-free » veut dire « tu peux t'en servir dans **tes** morceaux ». Ça ne donne **pas** le droit de **redistribuer** les sons, c'est-à-dire de les mettre dans une appli que d'autres téléchargent, encore moins si elle est payante. Seules ces licences le permettent :
- **CC0** ou domaine public ;
- **CC BY**, avec le crédit affiché dans l'appli ;
- **MIT** ;
- un **contrat écrit**.

Licences lues le 2026-11-12. Les plus importantes ont été relues une deuxième fois par moi-même : Signature Sounds, WaivOps, Surge XT, AKWF.

### 3.1 Échantillons modernes : utilisables

| Source | Licence | Ce qu'il y a | Limites |
|---|---|---|---|
| [Signature Sounds](https://www.signaturesounds.org/) | « License: CC0 (Free to use, no restrictions) » sur 34 des 36 packs visibles | house (deep, mélodique), UK garage, trap, drill, phonk, lo-fi | ni techno ni drum and bass ; vérifier pack par pack (2 packs sans « CC0 ») ; garder une capture datée |
| WaivOps / Patchbanks, sur Zenodo : [EDM-HSE](https://zenodo.org/records/13769544), [EDM-TECH](https://zenodo.org/records/17584890), [HH-TRP](https://zenodo.org/records/15734094), [EDM-TR8](https://zenodo.org/records/13257814) | CC BY 4.0 (crédit à Patchbanks obligatoire) | des dizaines de milliers de boucles de batterie modernes : house, techno moderne, minimale, industrielle, trap, 808 | des boucles, pas des sons isolés : il faut les découper ; « may contain minor … audio defects » |
| [Freesound](https://freesound.org/help/faq/), filtre CC0 (ou CC BY avec crédit) | CC0 : « You could even sell the sound » | énorme, de tout | qualité très inégale, tri à l'oreille ; **exclure** CC BY-NC et « Sampling+ » |
| SHLD Music (Drolez), Karoryfer | CC0 | nappes et leads de synthés récents (MiniFreak, Wavestate…), basses | peu de sons, peu électro |
| **Sons fabriqués par nous avec [Surge XT](https://surge-synthesizer.github.io/faq)** | FAQ : « The output of Surge XT is yours … whether your sounds are given away for free, or … commercially licensed or sold … regardless if you … used one of the factory presets » | kicks, basses, leads, nappes, effets modernes, à volonté, pour chaque style | Surge XT reste un **outil hors appli** : on n'embarque pas son code (GPL), seulement les sons qu'on a fabriqués |
| Sons fabriqués avec Vital, **à partir de nos propres réglages** | [licence Vital](https://vital.audio/eula) : autorise de « create samples for commercial products such as sample packs » | même idée | interdit de redistribuer ce qui sort de son contenu d'usine non modifié |

### 3.2 Échantillons : interdits ou trop flous (à écarter)

- **Interdits pour une appli** : Splice, Cymatics, Black Octopus, Zenhiser, Loopmasters, ADSR, Looperman, 99Sounds, MusicRadar SampleRadar, Philharmonia. Exemples de textes :
  - 99Sounds : « You do not have the right to sell or redistribute (even for free) the audio files » ;
  - Splice : interdit de « redistribute Sounds in new sample packs ».
- **Sans licence, donc sans droit** : Dirt-Samples et tidal-drum-machines (des « TR-808 » enregistrées on ne sait par qui).
- **Trop flous** : Ghosthack, Samples From Mars, Reverb Drum Machines, SampleSwap, Wavs.
- **Trop pauvre** : NSynth (16 kHz, mono).

### 3.3 Formes d'onde pour un synthé moderne

- **[AKWF](https://www.adventurekid.se/akrt/waveforms/adventure-kid-waveforms/)** (Adventure Kid Waveforms) : **CC0**, vérifié deux fois. La page de l'auteur dit « Kristoffer Ekstrand has waived all copyright », et son dépôt GitHub est marqué CC0-1.0. Ce sont des milliers de formes d'onde d'un cycle : le matériau idéal pour ajouter des tables d'ondes à notre synthé.
- **Interdit** : les tables et presets d'usine de Vital (« redistribute presets, samples, wavetables … created from any unaltered VITAL AUDIO content » est exclu) et les packs de presets de la communauté.
- **Trop flou** : les cartouches DX7 de Yamaha. Si besoin, on te laisse plutôt importer tes propres fichiers.

### 3.4 Ta question : des basses et mélodies toutes faites, gratuites ?

**Oui, il y en a énormément.** Des producteurs et des marques publient des « packs MIDI » gratuits : lignes de basse, mélodies, accords, par style (Cymatics, Unison, Producer Spot, Hyperbits, Splice…). **Mais leurs licences disent toutes la même chose** : tu peux t'en servir **dans tes morceaux**, pas les **redistribuer**. Les mettre dans l'appli pour d'autres utilisateurs, c'est de la redistribution.

**Pour ta musique à toi, aucun problème.** L'appli sait déjà importer un fichier MIDI (`MidiFile.importInto` : la batterie va sur les pads, le reste dans le piano roll). Tu peux prendre un pack MIDI gratuit et l'utiliser dans tes propres morceaux.

Ce qu'on peut **mettre dans l'appli** :

| Source | Licence | Intérêt |
|---|---|---|
| [free-midi-chords](https://github.com/ldrolez/free-midi-chords) | MIT (« All the MIDI files are licensed under the MIT license ») | plus de 13 000 fichiers, 195 suites d'accords par tonalité : de quoi enrichir les accords des recettes |
| [Groove MIDI Dataset](https://magenta.tensorflow.org/datasets/groove) (Google Magenta) | CC BY 4.0 | 13,6 heures de batterie jouée par de vrais batteurs, dont dance et hip-hop : de quoi rendre nos rythmes plus humains |
| The Endless Acid Banger | CC BY 4.0 | déjà dans l'appli (générateur acide) |

**À écarter** :
- Lakh MIDI et BitMidi : ce sont des transcriptions de chansons connues, protégées par le droit d'auteur.
- MetaMIDI : réservé à la recherche.
- GigaMIDI : non commercial.

**Conclusion** : pour les basses et mélodies « de tous les styles », la voie solide reste nos générateurs par style. On les enrichit avec ces deux sources libres et avec les traits des morceaux à succès (`docs/hits/`). L'étude des mélodies note encore qu'elles sont « trop sages » : 91 à 100 % de notes de l'accord, contre 55 à 85 % dans les tubes (`docs/son/MELODIE.md`). C'est un autre chantier que les sons.

### 3.5 Stable Audio (nos packs actuels)

- **Licence** : Stability AI Community License, mise à jour du 5 juillet 2024.
- **Revenus** : gratuite sous 1 million de dollars de revenus par an.
- **Inscription** : obligatoire pour un usage commercial.
- **Propriété des sons générés** : « You own any outputs generated from the Models ».
- **Mention** : « Powered by Stability AI » à afficher.

Nos packs générés sont donc redistribuables, **à condition de faire l'inscription** (voir § 7).

## 4. Les options payantes : est-ce que ça vaut le coup ?

| Option | Prix | Droits pour l'appli | Qualité | Vaut le coup ? |
|---|---|---|---|---|
| Sons fabriqués par nous (Surge XT, nos réglages Vital) + sources CC0 / CC BY | 0 € | oui | bonne, moderne | **Oui, d'abord** |
| Pack exclusif commandé à un sound designer sur Fiverr, avec cession écrite des droits | environ 500 à 1 500 $ pour ~300 sons (estimation : sur Fiverr, un kit sur mesure coûte 50 à 200 $ ; pas de tarif publié pour ce volume) | oui, avec contrat | variable selon le vendeur | **Oui, après l'essai**, si le gratuit ne suffit pas (hard techno, jump up sur mesure) |
| Sound designer confirmé, en exclusivité | environ 3 000 à 10 000 $ (estimation) | oui | haute | **Plus tard**, si l'appli se vend |
| [Soundtrack Loops](https://soundtrackloops.com/license-sounds/) : offre faite pour les « developers … building music apps » | sur devis | oui, par contrat | gros catalogue, modernité à vérifier | **Peut-être** : un devis ne coûte rien |
| Prime Loops, Loopmasters, Ueberschall : licences « appli » sur demande | sur devis, sans doute cher | oui, par contrat | très moderne | **Peut-être**, plus tard |
| Licences de moteurs (Vital hors GPL, JUCE, Tracktion, RNBO) | 0 à quelques centaines de $, ou sur devis | le moteur seulement | — | **Non** : ça n'apporte pas de sons, et notre moteur existe |
| Abonnements de sons (Splice, Cymatics, Envato, Zapsplat, Big Fish) | abonnement | **non** : la licence interdit l'appli | haute | **Non** |

Les grandes applis (BandLab, Groovepad) font produire leurs packs sur commande par des artistes : c'est l'option « sound designer », pas un catalogue acheté.

## 5. Les synthés libres modernes à embarquer

| Moteur | Licence | Dans une appli payante fermée | Ce qu'il apporte |
|---|---|---|---|
| Vital / Vitalium | GPL-3 (Vital : licence hors GPL sur devis) | **seulement si toute l'appli devient GPL** (code source publié), ou contrat avec Vital | la référence actuelle (tables d'ondes, 16 voix d'unisson) ; lourd, lié à JUCE |
| Surge XT et ses briques sst-* | GPL-3 (sauf 3 petits fichiers MIT) | seulement si l'appli devient GPL | tout ; trop lourd sur un Raspberry Pi 4, donc à risque sur téléphone |
| Odin 2, OB-Xf, ZynAddSubFX, Helm | GPL | seulement si l'appli devient GPL | rien que Vital n'ait déjà |
| msfa (cœur de Dexed) | Apache-2.0 | oui | un DX7 fidèle : années 80, doublon de Plaits |
| **Faust, `co.xfer_ott`** | MIT (octobre 2026) | **oui** | **un OTT mesuré à 1 dB près de l'original** : le gain « son 2020 » le plus fort pour le moins de travail |
| Faust, filtres de caractère (diode, Korg35, Oberheim) | STK-4.3 (type MIT) | oui | des couleurs de filtre modernes |
| Signalsmith elliptic-blep | MIT | oui | des dents de scie plus propres pour l'unisson |
| Airwindows (déjà présent) | MIT | oui | saturations et largeurs |
| Mutable Braids, Rings, Clouds | MIT | oui | textures (essaim de scies, résonateurs, granulaire) |

**Ce qu'on a déjà sans le savoir** : Plaits (dans l'appli) a un modèle « table d'ondes », mais aucun son ne s'en sert. Il imite volontairement un convertisseur 5 bits : c'est un son lo-fi, pas un Serum.

**Avis** : on n'embarque **aucun code GPL**. Ce serait obliger toute l'appli à publier son code source. On garde notre synthé et on lui ajoute, avec des briques MIT, ce qui fait le son d'aujourd'hui, dans cet ordre :
1. un **OTT** (code Faust MIT, sans allocation dans le fil audio, testé sur Linux) ;
2. des **tables d'ondes** (AKWF + tables à nous) avec transformation pendant la note ;
3. des **scies plus propres** et plus de voix d'unisson ;
4. des **filtres de caractère**.

Surge XT et Vital servent **hors de l'appli**, comme des instruments, pour fabriquer des échantillons qui sont à nous.

## 6. Le plan d'essai

**But** : mêmes morceaux, mêmes notes, sons remplacés. On compare l'avant et l'après à l'oreille et à la mesure, sur trois styles : **jump up, house, techno**. **Hard techno** sert de témoin pour le processeur.

Le principe est sans risque pour l'appli : l'essai passe par le banc, pas par ton téléphone. Les nouveaux sons vivent dans des packs d'essai, comme les packs actuels. Les recettes ne changent pas tant que tu n'as pas dit « mieux ».

### Essai 1 : une batterie moderne (échantillons) — effort moyen, environ 2 à 3 jours d'agent

- **Matière** :
  - house : Signature Sounds (CC0) ;
  - techno : boucles WaivOps (CC BY), découpées en sons isolés ;
  - tous les styles : Freesound CC0 trié ;
  - kicks, claps et percussions qu'on fabrique avec Surge XT, par un script refait à chaque fois.
- **Volume** : environ 40 à 60 sons par style, choisis à l'oreille et à la mesure (attaque, air, largeur).
- **Licence** : la licence de chaque son est notée dans le `pack.json` du pack, avec une capture datée de la page, et les crédits sont affichés.
- **Branchement** : par le mécanisme qui existe déjà. Le « pack du style » passe avant le kit synthétisé, et chaque rôle (kick, clap, charley…) prend le meilleur son du pack. Les notes ne bougent pas.
- **Attention aux kicks** : le kick studio de house et de techno est accordé à la tonalité. Un kick échantillonné doit l'être aussi (transposé hors du fil audio), sinon on perd ce gain.

### Essai 2 : régénérer les packs Stable Audio avec des mots d'aujourd'hui — effort petit, environ 1 jour

- **Mots** : on retire « 90s », « TR-909 », « Juno-106 » des demandes, et on décrit le son voulu (par exemple « modern tech house kick, punchy, clean sub »).
- **Coût et droits** : ça ne coûte rien et les sons générés nous appartiennent.
- **Limite** : la qualité du modèle léger, qui reste incertaine. C'est le moins cher des essais : il se fait en parallèle de l'essai 1.

### Essai 3 : des synthés des années 2020 (moteur) — effort moyen à grand, environ 1 à 2 semaines d'agent

- **D'abord, sans toucher au moteur** :
  - ouvrir les nappes et leads trop sombres de house et de techno (−39 à −42 dB d'air aujourd'hui) ;
  - faire porter les accords de house par un vrai son de nappe moderne plutôt que par le piano DX7.
- **Ensuite, dans le moteur** :
  - l'OTT en effet (sur la basse, le lead et le bus musique) ;
  - un oscillateur à tables d'ondes ;
  - de nouveaux sons de bibliothèque : basse « reese » moderne, lead « Serum », nappe qui évolue.
- **Ce qui reste synthétisé** : les basses de bass music (wobble calé sur le tempo, cycles de filtre). Un échantillon ne sait pas faire ça.

### Ce qu'on mesure (avant / après, mêmes morceaux)

- **Banc d'export** (`scripts/song-bench.sh`) :
  - sonie et crête (doivent rester dans les cibles) ;
  - air au-dessus de 10 kHz ;
  - grave-médium face au médium-aigu ;
  - largeur ;
  - présence au téléphone ;
  - et chaque voix rendue seule (§ 2.2).
- **Écoute en direct** (`scripts/live-bench.sh`) : aucune alerte (fondu, silence, clic, allocation).
- **Processeur** :
  - banc « petit téléphone » ;
  - règle : **pas plus de +10 % de charge sur aucun style**, et **rien de plus en hard techno**. Sur le téléphone Honor, elle fait déjà 44 % en moyenne et 67 % au pire moment. Remplacer une batterie synthétisée par des échantillons **allège** plutôt le calcul ; c'est l'OTT et les tables d'ondes qu'il faudra surveiller.
- **Ton oreille** : pour chaque style, 30 secondes du Drop, avant puis après, envoyées ici dans la conversation. Pas d'APK, sauf si tu en demandes une. Tu réponds « mieux / pareil / pire » et ce qui cloche.

### Taille de l'appli

- Aujourd'hui : 47,8 Mo.
- **Essais 1 et 2** : les sons sont téléchargés à part comme les packs actuels (environ 2 à 5 Mo par style) : l'appli ne grossit pas.
- **Essai 3** : un peu de code et quelques centaines de formes d'onde, moins de 1 Mo.
- **Option** : une batterie moderne de base intégrée dans l'appli, pour qu'elle sonne bien dès l'installation sans téléchargement : environ +5 à 10 Mo (question 2).

### Les risques

- **Licences** : un petit site « CC0 » peut avoir repris des sons qui ne sont pas à lui. Parade : sources fiables d'abord, capture datée de chaque licence, crédits affichés, et nos propres sons Surge XT en priorité.
- **L'équilibre du mix** : chaque son a son réglage de volume calibré. Des sons nouveaux demandent de refaire ces calibrations, sinon le mix bouge. C'est ce que le banc vérifie.
- **Le processeur** sur téléphone, pour l'essai 3 (voir la règle ci-dessus).
- **Les anciens projets** : ils doivent garder leurs sons. Les nouveaux sons ne vont qu'aux nouveaux morceaux, comme pour les versions de mix précédentes.

### Ce qui est impossible ou déconseillé

- **Impossible** :
  - mettre dans l'appli des sons de Splice, Cymatics, 99Sounds, MusicRadar, Loopmasters, Envato, Zapsplat… : leurs licences l'interdisent ;
  - mettre dans l'appli des packs MIDI gratuits ou des mélodies de chansons connues.
- **Interdit** : les tables et presets d'usine de Vital, même dans notre synthé.
- **Déconseillé** :
  - embarquer Vital ou Surge dans l'appli : toute l'appli devrait devenir libre (GPL), avec son code publié ;
  - remplacer par des échantillons les basses qui bougent avec le tempo (wobble, growl) : on perdrait ce qui les fait vivre ;
  - les SoundFonts General MIDI comme son « moderne » : ce sont des banques des années 90, et l'assistant ne s'en sert déjà pas.

## 7. Ce que toi seul peux faire, et les questions

**À faire par toi, quand tu peux** : t'inscrire (gratuit) à la licence « Community » de Stability AI. C'est obligatoire pour vendre l'appli avec ses packs générés et sa génération de sons, que l'essai se fasse ou non.

**Questions** (réponds par exemple « 1A, 2A, 3A, 4A ») :

1. **Budget pour les sons**
   - A) 0 € pour l'essai : sources gratuites et sons fabriqués par nous ; on décide d'un achat seulement après ton écoute. *(conseillé)*
   - B) Jusqu'à environ 1 500 $ tout de suite pour un pack exclusif commandé à un sound designer. Conséquence : plus rapide pour la hard techno et le jump up, mais de l'argent engagé avant de savoir si le gratuit suffit.
   - C) Demander des devis aux catalogues pro (Soundtrack Loops, Prime Loops). Conséquence : sans doute cher, et des semaines d'échanges.
2. **Taille de l'appli**
   - A) Sons modernes téléchargés à part, comme les packs actuels : l'appli ne grossit pas. *(conseillé)*
   - B) Une batterie moderne de base intégrée : +5 à 10 Mo, mais un bon son dès l'installation, même sans réseau.
3. **Styles de l'essai**
   - A) Jump up, house, techno, avec la hard techno comme témoin du processeur. *(conseillé)*
   - B) Tu choisis trois autres styles (dis lesquels).
4. **Comment tu écoutes**
   - A) Extraits de 30 secondes avant / après, envoyés ici. *(conseillé : pas d'APK à installer)*
   - B) Une APK d'essai avec un bouton « sons modernes » pour comparer sur ton téléphone. Conséquence : plus parlant, mais une APK à construire et à installer.

## Sources (lues le 2026-11-12)

- Dépôt : `THIRD_PARTY.md`, `docs/PLAN-SON-PRO.md`, `docs/hits/leads-pros.md`, `docs/son/banc/apres-son8.md`, `docs/son/MELODIE.md`, `docs/son/banc/petit-telephone.md`, `docs/audits/ultra/tour-5-hardtechno-rapport.md`, `engine/src/main/cpp/core/Synth.h`, `engine/src/main/cpp/core/DrumKit.h`, `scripts/packs/styles.json`, `scripts/packs/generate_pack.py`.
- Échantillons : signaturesounds.org ; zenodo.org/records/13769544, 17584890, 15734094, 13257814 ; freesound.org/help/faq ; drolez.com/blog/music/ableton-free-sound-packs.php ; sfzinstruments.github.io ; 99sounds.org/license ; splice.com/terms ; cymatics.fm/pages/contact ; blackoctopus-sound.com/terms-of-service ; zenhiser.com/pages/license-agreement ; loopmasters.com/cloud/terms_of_service ; looperman.com/help/terms ; philharmonia.co.uk/audio-sound-samples ; github.com/tidalcycles/Clean-Samples ; uzu.lurk.org/t/re-distribute-tidalcycles-dirt-samples/5547 ; github.com/ritchse/tidal-drum-machines.
- Synthés et formes d'onde : surge-synthesizer.github.io/faq ; vital.audio/eula ; github.com/mtytel/vital ; adventurekid.se (AKWF) ; github.com/KristofferKarlAxelEkstrand/AKWF-FREE ; github.com/grame-cncm/faustlibraries (`compressors.lib`, `co.xfer_ott`) ; github.com/surge-synthesizer (surge, sst-*, OB-Xf) ; github.com/asb2m10/dexed ; github.com/pichenettes/eurorack ; github.com/Signalsmith-Audio/elliptic-blep ; github.com/airwindows/airwindows ; discourse.zynthian.org/t/surge-xt/8764.
- MIDI : magenta.tensorflow.org/datasets/groove ; github.com/ldrolez/free-midi-chords ; colinraffel.com/projects/lmd ; zenodo.org/records/5142664 ; huggingface.co/datasets/Metacreation/GigaMIDI.
- Stable Audio : stability.ai/community-license-agreement ; huggingface.co/stabilityai/stable-audio-open-small.
- Payant : soundtrackloops.com/license-sounds ; support.primeloops.com (article 11000036557) ; ueberschall.com/about-us ; twine.net/blog/sound-designer-hourly-rates ; engine.tracktion.com ; forum.juce.com/t/revenue-limits-for-juce-tiers/61058.
