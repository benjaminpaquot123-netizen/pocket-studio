# Des sons modernes sans toucher aux morceaux : étude et plan d'essai (2026-11-12)

Demande de Benjamin : « les sons sonnent années 90-2000, même dans les styles modernes, surtout les synthés ». Son idée : garder les morceaux tels qu'ils sont (notes, structure, recettes) et ne changer que les sons, pour comparer. Ajouts : « il n'y a pas des gens qui publient gratuitement de super basses et mélodies de tous les styles ? », puis « vérifie aussi les options payantes, si ça en vaut la peine ».

Étude seulement : rien n'a changé dans l'appli, le moteur, le banc ou les tests. Aucun son n'a été téléchargé dans le dépôt. Seul ajout technique : un petit outil de mesure des voix et son test, dans `docs/sons/outils/`.

## En bref (à lire à voix haute)

1. **Les causes du côté rétro.**
   - La batterie est entièrement synthétisée, à la manière des boîtes à rythmes des années 80.
   - Certaines recettes visent exprès l'ancien. La « House » de l'appli est la house classique de Chicago de 1986 à 1992, avec les packs TR-909 et Juno. Le jump up joue un lead « Hoover » de 1991.
   - Les synthés sont les sons stars de 1983 à 2000, sans tables d'ondes.
   - La compression « OTT » qui fait le son d'aujourd'hui existe dans l'appli, mais elle ne sert que sur le master de quelques styles et sur les basses grasses, jamais sur les nappes et les leads.
2. **On peut changer les sons sans toucher aux notes.** L'appli choisit ses sons par rôle (kick, basse, lead…). Il suffit de donner d'autres sons aux mêmes rôles, derrière un interrupteur d'essai.
3. **Il existe des sons modernes qu'on a vraiment le droit de mettre gratuitement dans une appli payante.** Ce sont Signature Sounds (CC0), les boucles WaivOps (avec crédit), Freesound en CC0, et surtout des sons qu'on fabrique nous-mêmes avec le synthé gratuit Surge XT. Presque tous les packs « gratuits » connus l'interdisent (Splice, Cymatics, 99Sounds…).
4. **Des basses et des mélodies toutes faites et gratuites, il y en a plein.** Mais elles sont pour tes propres morceaux, jamais pour les mettre dans l'appli. Deux exceptions utiles : des accords (licence MIT) et des rythmes de batterie joués par de vrais batteurs (Google, avec crédit).
5. **Plan conseillé : un essai à 0 € sur le jump up, la tech house et la techno.**
   - On commence par une batterie moderne et par les réglages déjà dans l'appli (OTT, sons modernes inutilisés).
   - Viennent ensuite des tables d'ondes pour le synthé.
   - Tu écoutes l'avant / après en extraits de 30 secondes, avec les mesures et la charge du processeur.
   - Payer un sound designer (environ 500 à 1 500 $, estimation) ne vaut le coup qu'après cet essai.

---

## 1. D'où viennent les sons aujourd'hui

Un morceau fait par l'assistant (« Partir d'un style »), sans pack téléchargé, ne contient **aucun échantillon** : tout est calculé en direct par le moteur. L'appli a trois natures de sons, qui ne se remplacent pas de la même façon :

- **synthétisé** : le moteur fabrique le son note par note. Pour le changer, on change ses réglages ou on change de moteur.
- **SoundFont** : une banque d'instruments General MIDI des années 90-2000 (GeneralUser GS, FluidR3, MuseScore General). **L'assistant ne s'en sert jamais.** Elles servent seulement quand tu choisis toi-même un instrument dans Synthé › Source du son.
- **échantillon** : un enregistrement court, joué tel quel. Ce sont les packs téléchargés, les instruments réels et les sons de fête.

| Famille | Sans pack (par défaut) | Avec les packs téléchargés |
|---|---|---|
| Kick | synthétisé : « kick studio » (accordé à la tonalité en house et en techno), kick dur écrêté en hard techno | échantillon, qui remplace le kick studio |
| Caisse claire, clap | synthétisés (DaisySP, clap maison) | échantillon |
| Charleys, cymbales | synthétisés (DaisySP) ; la ride est la cymbale du kit raccourcie, le crash est cette cymbale telle quelle | échantillon (aucune ride en pack) |
| Percussions | synthétisées (rim, cloche, shaker, conga) | échantillon |
| 808, sub | voix 808 dédiée (trap, drill, phonk, jersey, reggaeton), « rumble » en techno, « Sub pur » en bass music | la 808 synthétisée reste devant |
| Basse | synthé maison ou Plaits (modèles « basse analogique », « log drum »…) | échantillon de basse joué au clavier, en premier dans 7 styles (dont house avec le pack Juno, tech house et techno) |
| Lead | synthé maison (Supersaw, Hoover, Arpège) ou Plaits (« Lead filtré », « Voix robot », voix FM façon DX7) | presque jamais remplacé (la tech house prend des voix découpées) |
| Nappe, accords, pluck | synthé maison (Nappe, Pluck, Orgue) ou Plaits (pianos FM DX7) | vrais instruments (piano, cordes) seulement en lo-fi, pop, boom bap, liquid DnB, amapiano |
| Montée, effets | bruit filtré du synthé | les sons d'effets des packs ne sont pas branchés |
| Voix | Plaits « voix robot » | les voix des packs ne sont pas branchées |

**Quel échantillon gagne** (`soundCategories`, `app/…/library/SoundLibraryViewModel.kt`) : il y a un ordre de priorité.
1. D'abord le pack de **machine** que la recette nomme (par exemple « Roland TR-909 » pour la batterie de la house).
2. Ensuite le pack du style.
3. Enfin le kit synthétisé.

Pour la basse, le pack nommé par la recette passe aussi en premier (par exemple « Juno » en house).

Sources : `engine/src/main/cpp/core/DrumKit.h` (« 16 synthesized drum voices (808/909 flavour), no samples ») ; `project-model/…/StarterWizard.kt` (`soundsFor`, `withStudioVoices`) ; `project-model/…/StyleRecipes.kt` (37 styles, leurs `kit` et `packSounds`) ; `scripts/packs/` (les packs).

**Les styles de l'essai, piste par piste, aujourd'hui** :
- **Jump up** (174 BPM, références de 2017 à 2023)
  - Batterie synthétisée : kick non accordé, caisse DaisySP, clap maison. Avec les packs, elle vient du pack « dnb ».
  - Basse « growl » : scie et carré désaccordés, balayage de filtre calé sur la double croche, couche médium saturée, effet « growl » avec distorsion et OTT. Il y a aussi un sub.
  - Lead « Hoover » à 7 voix, nappe à 2 scies, voix Plaits, OTT sur le master.
- **Tech house** (126 BPM, références de 2014 à 2022) : batterie du kit synthétisé, « Basse roulante », orgue, « Voix robot » Plaits. Avec les packs, la basse vient de `tech_house` et les voix découpées de `vocal_chops`.
- **Techno** (132 BPM, références de 1997 à 2019) : kick studio accordé (~44 Hz) et rumble, basse Plaits « analogique », accords « Pluck » en carré, lead Plaits « filtré ».
- **À part, la House** (122 BPM) : c'est **exprès la house classique de Chicago**.
  - Ses références vont de 1986 à 1992.
  - Sa batterie prend les packs « TR-909 » et « TR-707 », sa basse le pack « Juno ».
  - Ses accords sont le piano FM du DX7 (1983).
  - Son côté rétro est voulu par la recette (voir la question 3).
- **Témoins pour le processeur** : le jump up, le style le plus lourd (60 % de charge en moyenne, 74 % au pire sur le téléphone Honor), et la hard techno, qui y a eu des décrochages.

**Les notes** viennent de cellules écrites à la main dans les recettes, développées par l'appli (`MelodyBuilder`, `BassPatterns`, `AcidGenerator`). Elles ne dépendent pas des sons : on peut changer l'un sans l'autre.

## 2. Pourquoi ça sonne rétro, concrètement

### 2.1 Ce qu'on entend, et pourquoi

1. **La batterie est une boîte à rythmes des années 80 recréée.**
   - Les voix de DaisySP imitent la TR-808 et la TR-909 : c'est leur raison d'être.
   - Une production d'aujourd'hui empile des échantillons : un kick avec un claquement et un sub, des claps en plusieurs couches, des charleys enregistrés, souvent saturés et compressés.
   - Nos batteries sont aussi **presque mono** (§ 2.2) : les productions récentes étalent charleys et percussions.
2. **Certaines recettes visent exprès des sons d'époque.**
   - La « House » est la house classique de Chicago : elle va chercher les packs « Roland TR-909 », « TR-707 » et « Juno-106 », et le piano FM du DX7.
   - Le jump up, style des années 2020, joue un lead « Hoover » de 1991.
   - Côté packs, le vocabulaire d'époque se limite aux packs de machines (TR-909, Juno-106, DX7), qui portent ces noms, et au pack « Rétro techno » (« early 90s rave »).
   - Les packs des styles de l'essai ont été demandés sans mot d'époque (« drum and bass », « tech house tight », « techno Berlin »).
   - Le générateur léger qui les a faits (Stable Audio Open Small) sort du 44,1 kHz d'une qualité inégale.
3. **Les synthés sont les sons vedettes de 1983 à 2000.**
   - Le Hoover date de 1991, la supersaw de la JP-8000 de 1997, le piano FM du DX7 de 1983.
   - Le travail de fin octobre (« Son 8 ») les a rendus fidèles et propres (supersaw façon JP-8000, filtre qui suit la note, attaques, largeur), mais fidèles à leur époque.
   - Il manque **les tables d'ondes** (Serum en 2014, Vital en 2020) : des formes d'onde riches qui se transforment pendant la note. Notre synthé n'a que scie, carré, triangle et sinus. Le modèle « table d'ondes » de Plaits est dans l'appli, mais aucun son ne s'en sert.
4. **La compression « OTT » est là, mais pas où il faut.**
   - L'OTT est une compression en trois bandes qui remonte les détails faibles et les aigus : le son paraît « devant, brillant ».
   - L'appli l'a (effet « OTT multibande », `Ott` dans `engine/src/main/cpp/core/Effects.h`). L'assistant la pose sur le master de la bass music, de la trance et des styles durs (hard techno, gabber, acidcore), et dans les réglages de basse « Growl » et « Basse grasse ».
   - Elle n'est **jamais** posée sur les nappes et les leads, ni en house, ni en tech house, ni en techno, alors que la production d'aujourd'hui la met sur presque tout.
5. **Plusieurs voix de house, tech house et techno sont filtrées sombres.** C'est ce que montrent les mesures ci-dessous.

### 2.2 Les mesures (2026-11-12, chaque voix rendue seule)

**Méthode** :
- Les morceaux de l'assistant sont rendus par le banc (`scripts/song-bench.sh`).
- Chaque voix est rendue seule avant le master (`ps_render --no-limiter --solo`), puis mesurée par `docs/sons/outils/voix.py`.
- Cet outil est vérifié par `test_voix.py`, sur une dent de scie dont on connaît les chiffres exacts. Une version cassée de l'outil est refusée.
- Les commandes et les chiffres complets sont dans `docs/sons/outils/mesures-2026-11-12.md`. Les rendus ont été effacés, rien de sonore n'est commité.

**Ce que veulent dire les chiffres** :
- « **Air** » : la part du son au-dessus de 10 kHz, ce qui donne le brillant. L'« air » du banc, lui, commence à 6 kHz.
- **Repère** : une dent de scie brute, sans filtre, a un air de −18 à −24 dB selon la note (calcul exact sur ses harmoniques).
- **Limite** : nous n'avons mesuré aucun morceau récent de référence. « Sombre » veut dire ici « bien plus sombre qu'une scie brute ». L'essai mesurera, avec le même outil, des sons modernes fabriqués par nous (§ 6).

| Voix | Air au-dessus de 10 kHz | Grave-médium 150–500 Hz face à 1–4 kHz | Lecture |
|---|---|---|---|
| Tech house, lead (« Voix robot ») | **−57 dB** | +8,7 dB | la voix la plus sombre ; sur un téléphone, il en reste peu (présence 1–4 kHz à −13,7 dB) |
| Tech house, basse (« Basse roulante ») | −60 dB | +26,3 dB | un sub très rond, peu audible sur un téléphone (présence −18 dB) |
| Tech house, nappe (orgue) | −34 dB | −2,8 dB | assez sombre |
| Techno, nappe (Pluck) | **−39 dB** | **+10,5 dB** | sombre et boueuse : 15 à 20 dB plus sombre qu'une scie brute |
| Techno, lead (« Lead filtré ») | **−39 dB** | −2,2 dB | sombre |
| Jump up, lead (Hoover) | −20 dB | −2,3 dB | déjà brillant : son côté rétro vient du timbre du Hoover, pas du filtre |
| Jump up, nappe | −32 dB | +3,4 dB | assez sombre |
| House, nappe (piano FM) | **−42 dB** | +4,5 dB | sombre |
| House, lead (Arpège) | −24 dB | −1,3 dB | correct |
| Hard techno, lead (Supersaw) | −19 dB | +2,7 dB | brillant |
| Batteries (les 5 styles) | −16 à −21 dB | — | correct, mais presque mono (côté/centre de −19 à −24 dB) |

Le mix entier tient ses cibles :
- sonie de la partie forte à −8,4 LUFS ;
- crête à −1,2 dBTP ;
- côté/centre à −9,8 dB ;
- mouvement du lead à ±30 %.

Le problème n'est donc **ni le mixage ni le volume : c'est le timbre des sons**.

## 3. Ce qu'on a le droit d'utiliser, gratuitement

**La règle qui revient partout.** « Gratuit » ou « royalty-free » veut dire « tu peux t'en servir dans **tes** morceaux ». Ça ne donne **pas** le droit de **redistribuer** les sons, c'est-à-dire de les mettre dans une appli que d'autres téléchargent, encore moins si elle est payante. Seules ces licences le permettent :
- **CC0** ou domaine public ;
- **CC BY**, avec le crédit affiché dans l'appli ;
- **MIT** ;
- un **contrat écrit**.

Les licences ont été lues le 2026-11-12, puis relues par une deuxième personne pour les plus importantes.

### 3.1 Échantillons modernes : utilisables

| Source | Licence | Ce qu'il y a | Limites |
|---|---|---|---|
| [Signature Sounds](https://www.signaturesounds.org/) | 34 packs annoncent du CC0, dont 18 avec la ligne « License: CC0 (Free to use, no restrictions) » | house (deep, mélodique), UK garage, trap, drill, phonk, lo-fi | ni techno ni drum and bass ; beaucoup de bruitages ; un pack payant ; deux packs sans « CC0 » : vérifier pack par pack et garder une capture datée |
| WaivOps / Patchbanks sur Zenodo : [EDM-HSE](https://zenodo.org/records/13769544), [EDM-TECH](https://zenodo.org/records/17584890), [HH-TRP](https://zenodo.org/records/15734094), [EDM-TR8](https://zenodo.org/records/13257814) | CC BY 4.0 (crédit à Patchbanks obligatoire) | des dizaines de milliers de boucles de batterie : house, techno, trap, 808 | ce sont des boucles, pas des sons isolés : il faut les découper ; la techno est surtout faite de sons de boîtes à rythmes, avec un sous-style « 90s » à éviter ; « may contain minor … audio defects » |
| [Freesound](https://freesound.org/help/faq/), filtre CC0 (ou CC BY avec crédit) | CC0 : « You could even sell the sound » | énorme, de tout | qualité très inégale, tri à l'oreille ; **exclure** CC BY-NC et « Sampling+ » |
| SHLD Music (Drolez), Karoryfer | CC0 | nappes et leads de synthés récents (MiniFreak, Wavestate…), basses | peu de sons, peu électro |
| **Sons fabriqués par nous avec [Surge XT](https://surge-synthesizer.github.io/faq)** | « The output of Surge XT is yours … whether your sounds are given away for free, or … commercially licensed or sold … regardless if you … used one of the factory presets » | kicks, basses, leads, nappes, effets modernes, à volonté, pour chaque style | Surge XT reste un **outil hors appli** : on n'embarque pas son code (GPL), seulement les sons qu'on a fabriqués |
| Sons fabriqués avec Vital, **à partir de nos propres réglages** | [licence Vital](https://vital.audio/eula) : autorise de « create samples for commercial products such as sample packs » | même idée | interdit de redistribuer ce qui sort de son contenu d'usine non modifié |

### 3.2 Échantillons : interdits ou trop flous (à écarter)

- **Redistribution interdite** : Cymatics, Black Octopus, Zenhiser, Loopmasters, ADSR, Looperman, 99Sounds, MusicRadar SampleRadar, Philharmonia.
  - Exemple, 99Sounds : « You do not have the right to sell or redistribute (even for free) the audio files ».
- **Splice** : interdit lui aussi pour notre usage. Ses conditions défendent de « redistribute Sounds in new sample packs » (article 2.3.1.4 d) et de se servir d'un son isolé comme matière pour un autre échantillon. Elles acceptent un son de Splice dans un jeu ou une appli seulement s'il est fondu dans une œuvre, jamais jouable tel quel.
- **Sans licence, donc sans droit** : Dirt-Samples et tidal-drum-machines (des « TR-808 » enregistrées on ne sait par qui).
- **Trop flous** : Ghosthack, Samples From Mars, Reverb Drum Machines, SampleSwap, Wavs.
- **Trop pauvre** : NSynth (16 kHz, mono).

### 3.3 Formes d'onde pour un synthé moderne

- **Utilisable** : [AKWF](https://www.adventurekid.se/akrt/waveforms/adventure-kid-waveforms/) (Adventure Kid Waveforms), en **CC0**. La page de l'auteur dit « Kristoffer Ekstrand has waived all copyright », et son dépôt GitHub est marqué CC0-1.0. Ce sont des milliers de formes d'onde d'un cycle : le matériau idéal pour ajouter des tables d'ondes à notre synthé.
- **Interdit** : les tables et presets d'usine de Vital (« redistribute presets, samples, wavetables … created from any unaltered VITAL AUDIO content »), et les packs de presets de la communauté.
- **Trop flou** : les cartouches DX7 de Yamaha. Si besoin, on te laisse plutôt importer tes propres fichiers.

### 3.4 Ta question : des basses et mélodies toutes faites, gratuites ?

**Oui, il y en a énormément.** Des producteurs et des marques publient des « packs MIDI » gratuits par style : lignes de basse, mélodies, accords (Cymatics, Unison, Producer Spot, Hyperbits, Splice…). **Mais leurs licences disent toutes la même chose** : tu peux t'en servir **dans tes morceaux**, pas les **redistribuer**. Or les mettre dans l'appli pour d'autres utilisateurs, c'est les redistribuer.

**Pour ta musique à toi, aucun problème.** L'appli sait déjà importer un fichier MIDI (`MidiFile.importInto` : la batterie va sur les pads, le reste dans le piano roll). Tu peux donc prendre un pack MIDI gratuit et l'utiliser dans tes propres morceaux.

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

**Conclusion.** Pour les basses et mélodies « de tous les styles », la voie solide reste nos générateurs par style, enrichis par ces deux sources libres et par les traits des morceaux à succès (`docs/hits/`). L'étude des mélodies note encore qu'elles sont « trop sages » : 91 à 100 % de notes de l'accord, contre 55 à 85 % dans les tubes (`docs/son/MELODIE.md`). Mais c'est un autre chantier que les sons.

### 3.5 Stable Audio (nos packs actuels)

La licence est la « Stability AI Community License », dans sa version du 5 juillet 2024. Ce qu'elle dit :
- gratuite sous 1 million de dollars de revenus par an ;
- inscription obligatoire pour un usage commercial ;
- « You own any outputs generated from the Models » : les sons générés nous appartiennent ;
- « Powered by Stability AI » doit être affiché.

Nos packs générés sont donc redistribuables, **à condition de faire l'inscription** (voir § 7).

## 4. Les options payantes : est-ce que ça vaut le coup ?

Les prix marqués « estimation » ne sont pas des tarifs publiés.

| Option | Prix | Droits pour l'appli | Qualité | Vaut le coup ? |
|---|---|---|---|---|
| Sons fabriqués par nous (Surge XT, nos réglages Vital) + sources CC0 / CC BY | 0 € | oui | bonne, moderne | **Oui, d'abord** |
| Pack exclusif commandé à un sound designer sur Fiverr, avec cession écrite des droits | estimation : 500 à 1 500 $ pour ~300 sons (sur Fiverr, un kit sur mesure coûte 50 à 200 $) | oui, avec contrat | variable selon le vendeur | **Oui, après l'essai**, si le gratuit ne suffit pas (hard techno, jump up sur mesure) |
| Sound designer confirmé, en exclusivité | estimation : 3 000 à 10 000 $ | oui | haute | **Plus tard**, si l'appli se vend |
| [Soundtrack Loops](https://soundtrackloops.com/license-sounds/), offre faite pour les « developers … building music apps » | sur devis | oui, par contrat | gros catalogue, modernité à vérifier | **Peut-être** : un devis ne coûte rien |
| Prime Loops, Loopmasters, Ueberschall : licences « appli » sur demande | sur devis, sans doute cher | oui, par contrat | très moderne | **Peut-être**, plus tard |
| Licences de moteurs (Vital hors GPL, JUCE, Tracktion, RNBO) | 0 à quelques centaines de $, ou sur devis | le moteur seulement | — | **Non** : ça n'apporte pas de sons, et notre moteur existe |
| Abonnements de sons (Splice, Cymatics, Envato, Zapsplat, Big Fish) | abonnement | **non**, la licence interdit l'appli | haute | **Non** |

Les grandes applis (BandLab, Groovepad) font produire leurs packs sur commande par des artistes. C'est l'option « sound designer », pas un catalogue acheté.

## 5. Les synthés libres modernes à embarquer

| Moteur | Licence | Dans une appli payante fermée | Ce qu'il apporte |
|---|---|---|---|
| Vital / Vitalium | GPL-3 (Vital : licence hors GPL sur devis) | **seulement si toute l'appli devient GPL** (code source publié), ou avec un contrat Vital | la référence actuelle (tables d'ondes, 16 voix d'unisson) ; lourd, lié à JUCE |
| Surge XT et ses briques sst-* | GPL-3 (sauf 3 petits fichiers MIT) | seulement si l'appli devient GPL | tout ; jugé trop lourd sur un Raspberry Pi 4, donc à risque sur téléphone |
| Odin 2, OB-Xf, ZynAddSubFX, Helm | GPL | seulement si l'appli devient GPL | rien que Vital n'ait déjà |
| msfa (cœur de Dexed) | Apache-2.0 | oui | un DX7 fidèle : années 80, doublon de Plaits |
| Faust `co.xfer_ott` | MIT (octobre 2026) | oui | un modèle de l'OTT de Xfer à 1 dB près ; **facultatif** : notre OTT existe, au plus une comparaison |
| Faust, filtres de caractère (diode, Korg35, Oberheim) | STK-4.3 (type MIT) | oui | des couleurs de filtre modernes |
| Signalsmith elliptic-blep | MIT | oui | des dents de scie plus propres pour l'unisson |
| Airwindows (déjà présent) | MIT | oui | saturations et largeurs |
| Mutable Braids, Rings, Clouds | MIT | oui | textures : essaim de scies, résonateurs, granulaire |

**Ce qu'on a déjà sans s'en servir** :
- **Plaits a un modèle « table d'ondes »** (4 banques de 8 × 8 ondes figées). Aucun son de la bibliothèque ne l'utilise. Son imitation d'un vieux convertisseur 5 bits ne concerne que sa sortie secondaire, et l'appli joue sa sortie principale. Ses tables sont figées, mais on peut l'essayer à 0 €.
- **La bibliothèque contient déjà des sons plus récents** qu'aucune recette n'utilise : « Reese moderne », « Accords future bass », « Pluck future ».

**Avis : on n'embarque aucun code GPL.** Sinon, toute l'appli devrait publier son code source. On garde notre synthé et on lui ajoute ce qui fait le son d'aujourd'hui, dans cet ordre :
1. **Mieux se servir de ce qu'on a**, sans toucher au moteur : l'OTT existant sur les nappes et les leads, des filtres plus ouverts, les sons modernes inutilisés, le modèle table d'ondes de Plaits.
2. **Des tables d'ondes** (AKWF + tables à nous) qui se transforment pendant la note. Les tables sont préparées **hors du fil audio** ; le fil audio ne fait que les lire, sans allocation.
3. **Des scies plus propres** (elliptic-blep, MIT) et plus de voix d'unisson.
4. **Des filtres de caractère** (Faust, type MIT).

Surge XT et Vital servent **hors de l'appli**, comme des instruments, pour fabriquer des échantillons qui sont à nous.

## 6. Le plan d'essai

**But** : mêmes morceaux, mêmes notes, même structure, mais des sons remplacés. On compare l'avant et l'après à l'oreille et à la mesure, sur **jump up, tech house et techno**. **Jump up** (le plus lourd) et **hard techno** (des décrochages sur téléphone) servent de témoins pour le processeur.

**Comment on branche, sans risque.** Tout passe par un interrupteur d'essai. Il fait nommer aux recettes de l'essai des packs d'essai à la place de leurs packs de machines (TR-909, Juno…) et de leurs sons de bibliothèque. Au banc, seuls les packs d'essai sont installés. Les notes et la structure ne changent pas, et l'appli de ton téléphone ne change pas tant que tu n'as pas dit « mieux ».

### Essai 1 : une batterie moderne en échantillons

**Effort** (estimation) : moyen, environ 2 à 3 jours d'agent.

**D'où viennent les sons** :
- **tech house** : Signature Sounds (CC0) ;
- **techno** : boucles WaivOps (CC BY) découpées en sons isolés, en évitant le sous-style « 90s » ;
- **tous les styles** : Freesound CC0 trié, plus des kicks, claps et percussions fabriqués avec Surge XT par un script refait à chaque fois.

**Comment on fait le pack** :
- environ 40 à 60 sons par style (estimation), choisis à l'oreille et à la mesure : attaque, air, largeur ;
- la licence de chaque son est notée dans le `pack.json` du pack, avec une capture datée de la page ;
- les crédits sont affichés dans l'appli.

**Attention aux kicks** : le kick studio de la techno est accordé à la tonalité. Un kick échantillonné doit l'être aussi (transposé hors du fil audio), sinon on perd ce gain.

### Essai 2 : de meilleurs packs générés

**Effort** (estimation) : petit, environ 1 jour.

- **Ce qu'on fait** : régénérer les packs des styles de l'essai avec Stable Audio, avec des mots qui décrivent précisément le son d'aujourd'hui (par exemple « modern tech house kick, punchy, clean sub, 2020s club »), et refaire leur classement.
- **Avantages** : ça ne coûte rien et les sons générés nous appartiennent.
- **Limite** : la qualité du modèle léger, incertaine. Cet essai se fait en parallèle de l'essai 1.

### Essai 3 : des synthés des années 2020

**Effort** (estimation) : de quelques jours pour la partie A à environ une semaine d'agent pour la partie B.

**A. Sans toucher au moteur (recettes seules)** :
- l'OTT existant sur les nappes et les leads des trois styles ;
- des filtres plus ouverts sur les voix sombres du § 2.2 ;
- les sons modernes déjà dans la bibliothèque (« Reese moderne », « Accords future bass », « Pluck future ») ;
- le modèle table d'ondes de Plaits ;
- pour la tech house, un vrai lead à la place de la « Voix robot » à −57 dB d'air.

**B. Dans le moteur** :
- un oscillateur à tables d'ondes (tables préparées hors du fil audio) ;
- puis les scies plus propres et l'unisson élargi.

**Ce qui reste synthétisé** : les basses de bass music (wobble calé sur le tempo, cycles de filtre). Un échantillon ne sait pas faire ça.

### Ce qu'on mesure (avant / après, mêmes morceaux)

- **Banc d'export** (`scripts/song-bench.sh`) :
  - sonie et crête, qui doivent rester dans les cibles ;
  - largeur ;
  - basse au téléphone.
- **Voix seules** (`docs/sons/outils/voix.py`, la nappe comprise) :
  - air au-dessus de 10 kHz ;
  - grave-médium face au médium-aigu ;
  - présence au téléphone.
  - Les chiffres « avant » sont dans `docs/sons/outils/mesures-2026-11-12.md`.
  - Pour avoir un repère moderne, on mesure aussi, avec le même outil, des sons modernes fabriqués avec Surge XT.
- **Écoute en direct** (`scripts/live-bench.sh`) : aucune alerte (fondu, silence, clic, allocation).
- **Processeur** (banc « petit téléphone ») :
  - **pas plus de +10 % de charge sur aucun style, et rien de plus en jump up ni en hard techno** ;
  - sur le téléphone Honor, le jump up fait déjà 60 % en moyenne (74 % au pire moment), et la hard techno a eu des décrochages ;
  - remplacer une batterie synthétisée par des échantillons devrait alléger le calcul (estimation, à mesurer) ;
  - ce sont l'OTT sur plus de pistes et les tables d'ondes qu'il faudra surveiller.
- **Ton oreille** : pour chaque style, 30 secondes du Drop, avant puis après, envoyées ici dans la conversation. Pas d'APK, sauf si tu en demandes une. Tu réponds « mieux / pareil / pire », et ce qui cloche.

### Taille de l'appli

Toutes les tailles ci-dessous sont des estimations, sauf l'actuelle.

- **Aujourd'hui** : 47,8 Mo.
- **Essais 1 et 2** : les sons sont téléchargés à part comme les packs actuels (environ 2 à 5 Mo par style). L'appli ne grossit pas.
- **Essai 3** : un peu de code et quelques centaines de formes d'onde, moins de 1 Mo.
- **Option** : une batterie moderne de base intégrée à l'appli, pour qu'elle sonne bien dès l'installation sans téléchargement. Environ +5 à 10 Mo (question 2).

### Les risques

- **Licences** : un petit site « CC0 » peut avoir repris des sons qui ne sont pas à lui. Parade : des sources fiables d'abord, une capture datée de chaque licence, des crédits affichés, et nos propres sons Surge XT en priorité.
- **L'équilibre du mix** : chaque son a son volume calibré. Des sons nouveaux demandent de refaire ces calibrations, sinon le mix bouge. C'est ce que le banc vérifie.
- **Le processeur sur téléphone**, surtout pour l'essai 3 (voir la règle plus haut).
- **Les anciens projets** doivent garder leurs sons. Les nouveaux sons ne vont qu'aux nouveaux morceaux, comme pour les versions de mix précédentes.

### Ce qui est impossible ou déconseillé

- **Impossible** :
  - mettre dans l'appli des sons de Splice, Cymatics, 99Sounds, MusicRadar, Loopmasters, Envato, Zapsplat… : leurs licences l'interdisent ;
  - mettre dans l'appli des packs MIDI gratuits, ou des mélodies de chansons connues.
- **Interdit** : les tables et presets d'usine de Vital, même dans notre synthé.
- **Déconseillé** :
  - embarquer Vital ou Surge dans l'appli : toute l'appli devrait devenir libre (GPL), avec son code publié ;
  - remplacer par des échantillons les basses qui bougent avec le tempo (wobble, growl) : on perdrait ce qui les fait vivre ;
  - compter sur les SoundFonts General MIDI pour un son « moderne » : ce sont des banques des années 90, et l'assistant ne s'en sert déjà pas.

## 7. Ce que toi seul peux faire, et les questions

**À faire par toi, quand tu peux** : t'inscrire (gratuit) à la licence « Community » de Stability AI. C'est obligatoire pour vendre l'appli avec ses packs générés et sa génération de sons, que l'essai se fasse ou non.

**Questions** (réponds par exemple « 1A, 2A, 3A, 4A ») :

1. **Budget pour les sons**
   - A) 0 € pour l'essai : sources gratuites et sons fabriqués par nous. *(conseillé)* Conséquence : on ne décide d'un achat qu'après ton écoute.
   - B) Jusqu'à environ 1 500 $ tout de suite pour un pack exclusif commandé à un sound designer. Conséquence : plus rapide pour la hard techno et le jump up, mais de l'argent engagé avant de savoir si le gratuit suffit.
   - C) Demander des devis aux catalogues pro (Soundtrack Loops, Prime Loops). Conséquence : sans doute cher, et des semaines d'échanges.
2. **Taille de l'appli**
   - A) Les sons modernes téléchargés à part, comme les packs actuels. *(conseillé)* Conséquence : l'appli ne grossit pas, mais il faut du réseau une fois pour avoir les nouveaux sons.
   - B) Une batterie moderne de base intégrée à l'appli. Conséquence : +5 à 10 Mo (estimation), mais un bon son dès l'installation, même sans réseau.
3. **La « House » de l'appli** (c'est une affaire de goût : la recette vise exprès la house de Chicago de 1986 à 1992)
   - A) La garder classique : l'essai se fait sur la tech house, qui est la house d'aujourd'hui. *(conseillé)* Conséquence : rien ne change pour qui aime le son d'origine.
   - B) La moderniser aussi : nouveaux sons et nouvelles références. Conséquence : on perd la house « à l'ancienne » de l'appli.
   - C) Faire les deux : une « House classique » et une « House moderne ». Conséquence : un style de plus dans la liste, déjà longue (37).
4. **Comment tu écoutes**
   - A) Des extraits de 30 secondes avant / après, envoyés ici. *(conseillé)* Conséquence : rien à installer, réponse rapide.
   - B) Une APK d'essai avec un bouton « sons modernes ». Conséquence : plus parlant sur ton téléphone, mais une APK à construire et à installer.

## Sources (lues le 2026-11-12)

- **Dépôt** : `THIRD_PARTY.md`, `docs/PLAN-SON-PRO.md`, `docs/hits/leads-pros.md`, `docs/son/banc/apres-son8.md`, `docs/son/MELODIE.md`, `docs/son/banc/petit-telephone.md`, `docs/audits/ultra/tour-5-hardtechno-rapport.md`, `engine/src/main/cpp/core/Synth.h`, `engine/src/main/cpp/core/DrumKit.h`, `engine/src/main/cpp/core/Effects.h` (`Ott`), `project-model/…/StyleRecipes.kt`, `project-model/…/WizardMix.kt`, `scripts/packs/styles.json`, `scripts/packs/generate_pack.py`.
- **Échantillons** :
  - signaturesounds.org ; freesound.org/help/faq ; drolez.com/blog/music/ableton-free-sound-packs.php ; sfzinstruments.github.io ;
  - Zenodo : zenodo.org/records/13769544, 17584890, 15734094, 13257814 ;
  - licences des packs écartés : 99sounds.org/license ; splice.com/terms ; cymatics.fm/pages/contact ; blackoctopus-sound.com/terms-of-service ; zenhiser.com/pages/license-agreement ; loopmasters.com/cloud/terms_of_service ; looperman.com/help/terms ; philharmonia.co.uk/audio-sound-samples ;
  - sons de TidalCycles : github.com/tidalcycles/Clean-Samples ; uzu.lurk.org/t/re-distribute-tidalcycles-dirt-samples/5547 ; github.com/ritchse/tidal-drum-machines.
- **Synthés et formes d'onde** :
  - surge-synthesizer.github.io/faq ; vital.audio/eula ; github.com/mtytel/vital ;
  - adventurekid.se (AKWF) ; github.com/KristofferKarlAxelEkstrand/AKWF-FREE ;
  - github.com/grame-cncm/faustlibraries (`compressors.lib`, `co.xfer_ott`) ;
  - github.com/surge-synthesizer (surge, sst-*, OB-Xf) ; github.com/asb2m10/dexed ; github.com/pichenettes/eurorack ; github.com/Signalsmith-Audio/elliptic-blep ; github.com/airwindows/airwindows ;
  - discourse.zynthian.org/t/surge-xt/8764.
- **MIDI** : magenta.tensorflow.org/datasets/groove ; github.com/ldrolez/free-midi-chords ; colinraffel.com/projects/lmd ; zenodo.org/records/5142664 ; huggingface.co/datasets/Metacreation/GigaMIDI.
- **Stable Audio** : stability.ai/community-license-agreement ; huggingface.co/stabilityai/stable-audio-open-small.
- **Payant** : soundtrackloops.com/license-sounds ; support.primeloops.com (article 11000036557) ; ueberschall.com/about-us ; twine.net/blog/sound-designer-hourly-rates ; engine.tracktion.com ; forum.juce.com/t/revenue-limits-for-juce-tiers/61058.
