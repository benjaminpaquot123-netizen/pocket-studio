# Plan des audits finaux (préparé le 2026-10-29, pas encore lancé)

Demande de Benjamin : « des audits finaux pour contrôler que tout fonctionne nickel » ; des débutants, des pros et des ingénieurs du son ; les sons récupérés et analysés ; chaque tour dans des langues différentes jusqu'à ce que toutes y passent ; on recommence en corrigeant le grave puis le moins grave ; des audits d'ergonomie et de design aussi : « il faut tout penser ».

## 1. Ce qu'on vérifie (8 axes)
1. **Ça marche** : chaque parcours va au bout, sans plantage, sans perte de projet, sans écran bloqué.
2. **Le son** : rendu par le vrai moteur et mesuré.
   - Export à −14 LUFS ±1, true peak ≤ −1 dBTP, aucune saturation, aucun NaN, aucun clic.
   - Parties contrastées (Drop au moins +2 LU sur le Groove), grave présent sans étouffer le médium.
   - Tempo et tonalité justes.
3. **L'ergonomie** :
   - nombre de gestes par tâche ;
   - découvrabilité (trouver sans aide) ;
   - « une action principale évidente par écran » ;
   - retour immédiat de chaque geste ;
   - retour arrière qui fait ce qu'on attend ;
   - pas de cul-de-sac ;
   - textes compris du premier coup.
4. **Le design** (`docs/DESIGN.md`) :
   - cohérence des composants et des couleurs (vert = ça joue) ;
   - hiérarchie et alignements ;
   - espaces, états vides et états d'erreur ;
   - animations utiles de 150 à 250 ms ;
   - rien qui déborde ni ne se chevauche.
5. **L'accessibilité** :
   - cibles d'au moins 48 dp (pads 64 dp) ;
   - texte d'au moins 14 sp, contraste ≥ 4,5:1 (3:1 pour les gros textes et les icônes) ;
   - libellés lisibles par TalkBack ;
   - animations coupées respectées ;
   - taille d'interface à 130 % ;
   - daltonisme : jamais la couleur seule pour dire quelque chose.
6. **Les langues** :
   - les 15 langues passent : textes coupés, mots restés en français, sens faux, guillemets ;
   - notes Do Ré Mi ou C D E selon la langue ;
   - arabe de droite à gauche ;
   - chiffres et pourcentages.
7. **Les écrans** : petit téléphone, téléphone standard (392 dp, celui du banc), grand téléphone, tablette 8" et Galaxy Tab A11, paysage, 130 %.
8. **La robustesse** :
   - interruptions : appel, mise en arrière-plan, rotation, casque débranché ;
   - double toucher, annuler et rétablir partout ;
   - hors ligne ;
   - projet ancien ;
   - disque presque plein.

## 1 bis. Exports et mastering (demande de Benjamin : « il faut que la masterisation soit nickel, avec les options qu'on doit proposer pour les pros »)

### Ce qui existe aujourd'hui
- Formats : WAV (16/24 bits, 32 bits flottants sans limiteur), FLAC, M4A, 44,1 ou 48 kHz.
- Piste par piste (stems), MIDI.
- « Volume visé » : streaming −14 LUFS, volume du style, club, ou libre (protection seule).
- Rapport LUFS / crête, avertissement si la crête est trop haute ou si le limiteur tasse trop.

### Lot « Master pro » à faire AVANT le tour 1 (préparé, pas lancé)
Les options que propose un outil pro, groupées derrière « Avancé » en Pro. Le Mode simple garde ses 3 choix clairs.
1. **Cible de volume** :
   - Spotify / YouTube −14, Apple Music −16, club −9, « comme le style », et une valeur libre (−6 à −23 LUFS) ;
   - plafond de crête au choix : −1, −0,5 ou −0,3 dBTP, mesuré en true peak après l'encodage ;
   - sans limiteur (32 bits flottants).
2. **Chaîne du master visible et réglable** :
   - EQ, compresseur de bus (« glue »), largeur stéréo avec grave en mono sous X Hz, saturation douce, limiteur (attaque, relâchement, look-ahead) ;
   - **écoute A/B** avec et sans master, au même volume (gain compensé) ;
   - **morceau de référence** importé, comparé au volume égal.
3. **Mesures de pro** :
   - LUFS intégré, court terme max et momentané max, LRA (dynamique), true peak, PLR ;
   - corrélation et compatibilité mono, spectre comparé à la référence.
   - Un rapport exporté avec le fichier.
4. **Fichier final** :
   - 16, 24 ou 32 bits flottants, 44,1, 48 ou 96 kHz ;
   - **dither** au passage en 16 bits (TPDF, mise en forme du bruit en option) ;
   - fondu de début et de fin, silence de fin réglable (queue de réverbe gardée), début et fin coupés au premier et au dernier son ;
   - plage exportée : morceau, partie, boucle ou sélection ;
   - métadonnées : titre, artiste, BPM, tonalité, pochette.
5. **Stems pour pros** :
   - piste par piste, avec ou sans le traitement du master, avec ou sans les envois (réverbe, écho) ;
   - tous de la même longueur, depuis 0 ;
   - noms propres.
   - **Test de nullité** : la somme des stems doit égaler le mix.
6. **Autres sorties** :
   - MIDI (toutes les pistes, tempo, signature) ;
   - projet complet (partage et réouverture identique) ;
   - vidéo de partage.

### Contrôles automatiques des exports (banc, à chaque tour)
- Une **matrice** : chaque format × fréquence × bits × cible × plafond, sur 5 styles. On mesure LUFS ±0,5 de la cible, true peak sous le plafond après l'encodage (M4A et FLAC compris), aucun NaN, aucun clic, et la bonne durée (fin et queue de réverbe).
- **Aller-retour** :
  - le projet exporté puis réouvert est identique ;
  - le MIDI exporté puis réimporté donne les mêmes notes ;
  - la somme des stems égale le mix (nullité sous −60 dB).
- **Fichiers lisibles** : les fichiers sont lus par ffmpeg sans erreur, avec les bonnes métadonnées.
- **Comparaison** : écoute contre la sortie en direct (« ce que j'entends = ce que j'exporte »).

### Dans les tours
- L'ingénieur du son et un pro masterisent un morceau pour Spotify, puis pour le club. Ils exportent WAV 24 bits, FLAC, M4A et les stems, puis jugent le résultat au rapport et au spectrogramme.
- Un débutant partage son morceau en deux gestes et doit obtenir un fichier propre sans rien régler.

## 2. Les testeurs (toujours neufs, un tour = 6 sièges)

| Rôle | Ce qu'il fait | Ce qu'il juge |
|---|---|---|
| **Débutant** (2 par tour : âges, aisance et patience variés) | J'apprends (tuto Facile), Fais-moi un morceau, Mode simple | Comprendre, se perdre, nombre de gestes, envie de continuer |
| **Producteur pro** (1 à 2) : un habitué de FL Studio Mobile, un DJ ou bootlegger, un beatmaker hip-hop | Je pars de zéro, tuto Pro, Remix, Live, export | Vitesse, outils qui manquent, réalisme du style |
| **Ingénieur du son** (1) | Écoute tout avec la commande `ecoute` (rapport et spectrogramme), mixe, masterise, exporte | Niveaux, équilibre, saturation, réalisme sonore, export |
| **Ergonome UX senior** (à chaque tour, sur le banc) | Les parcours clés, avec le compte des gestes et du temps | Découvrabilité, charge mentale, cohérence, retours arrière (voir §2 bis) |
| **Designer UI senior** (à chaque tour, sur les captures) | Passe en revue les captures de tous les écrans × langues × tailles | Cohérence, alignements, débordements, hiérarchie, finition (voir §2 bis) |
| **Designer motion** (tours 1 et dernier) | Les transitions, l'ouverture, les retours au toucher, image par image | Utile, 150 à 250 ms, sans saccade, coupé si les animations sont désactivées |
| **Expert accessibilité** (à chaque tour, hors banc et sur le banc) | Captures à 130 %, contrastes, cibles, libellés TalkBack, daltonisme | Règles du §1.5 |

## 2 bis. Ergonomie et visuel : des pros, très exigeants
Benjamin le veut ainsi : les auditeurs d'ergonomie et de design sont des professionnels **seniors et très exigeants**. Ils jugent l'appli comme une app vendue sur le Play Store face aux meilleures, pas comme un projet amateur.
- **Leurs références** :
  - FL Studio Mobile, Koala Sampler, BandLab, Ableton Note, Groovebox, Roland Zenbeats ;
  - les guides Material 3 et Apple HIG ;
  - les 10 heuristiques de Nielsen ;
  - les WCAG 2.2 niveau AA.
- **Leur note** : chaque écran est noté de 1 à 10 sur 6 critères. Un écran est validé à **8/10 minimum sur chaque critère**.
  1. Clarté : l'action principale se voit en moins de 2 s.
  2. Efficacité : nombre de gestes, comparé à la meilleure appli de référence.
  3. Cohérence : composants, mots et couleurs.
  4. Finition visuelle : grille, alignements au dp près, espaces, typographie, icônes.
  5. Retour et états : vide, chargement, erreur, succès.
  6. Accessibilité.
- **Ils ne laissent rien passer** :
  - un alignement décalé de 2 dp, une icône d'un autre style ;
  - un texte coupé dans une seule des 15 langues ;
  - un bouton qui change de place d'un écran à l'autre ;
  - un mot technique non expliqué en Mode simple ;
  - une animation qui saccade ;
  - un écran sans état vide soigné.
- **Ce que chaque défaut contient** :
  - la capture annotée, avec la zone entourée et la cote mesurée ;
  - la règle enfreinte ;
  - ce que fait la référence ;
  - une correction précise.
- **Leur gravité compte autant que celle des testeurs** : un écran sous 8/10 bloque la fin des audits comme un Grave.
- **Ils sont présents à chaque tour**, et toujours neufs, comme les autres testeurs. Le tour suivant vérifie que ce qu'ils ont signalé est vraiment corrigé.

## 3. Les parcours (chaque tour en couvre une partie, tous passent au moins une fois)
1. **Premier lancement** : langue, ouverture avec son, accueil, choix Simple ou Pro.
2. **J'apprends** : tuto complet d'un style par famille, en Facile et en Pro, avec kit à télécharger ou « Plus tard ».
3. **Fais-moi un morceau** : assistant, écoute des styles, Guide jusqu'au morceau fini.
4. **Je pars de zéro** : batterie à la main (roulements, triolets, décalage), notes, accords, 303, structure, mix.
5. **Remix** : carte de l'accueil, séparation, tableau des pistes, phrases de voix sur les pads, voix dans le morceau.
6. **Live** : pads, effets, enregistrement de la performance.
7. **Mix et master** : Mode simple et Pro, effets, automation, vumètres.
8. **Partager et exporter** : WAV, M4A, MIDI, projet. Le fichier est récupéré et analysé.
9. **Projets** : ouvrir, copier, renommer, supprimer, reprendre (« Te revoilà »).
10. **Réglages et aide** : langue, notation, son d'ouverture, vibrations, Guide, rapport de bug.
11. **Enregistrer au micro** et **clavier MIDI**. Sur le banc, seulement ce qui est simulable ; le reste se teste sur téléphone par Benjamin.

## 4. Les langues par tour (les 15 y passent)
- **Tour 1** : FR, EN, JA, AR, DE, ES.
- **Tour 2** : NL, PT-BR, IT, PL, TR, ZH-CN.
- **Tour 3** : ID, HI, KO, plus les langues où il restait un défaut.
- **Tours suivants** : des langues tirées au sort parmi celles qui ont eu des défauts.
- Chaque siège a une langue, un appareil et un parcours, répartis pour que chaque langue croise au moins un débutant et une fois le son.
- Le rapport est toujours rédigé en français.

## 5. Les audits automatiques (sans testeur, avant chaque tour)
- **Matrice de captures** : chaque écran × 15 langues × 4 tailles. Elle détecte les textes coupés (`hasVisualOverflow`), les chevauchements et ce qui sort de l'écran.
- **Cibles tactiles** : tout élément cliquable de moins de 48 dp dans l'arbre sémantique. **Contrastes** : texte et fond mesurés sur les captures.
- **Libellés** : tout bouton à icône sans description (TalkBack).
- **Banc des morceaux** (`scripts/song-bench.sh`) : les 37 styles de l'assistant et les 37 morceaux cibles des tutos, rendus et mesurés selon le §1.2.
- **Parcours automatiques** : `TutoJourneyTest` sur les 37 cours, et les tests existants (≈ 4 800).

## 6. La gravité
- **Bloquant** : on ne peut pas finir, plantage, perte de projet, son cassé.
- **Grave** : faux, trompeur ou très pénible. Un débutant abandonne, un pro n'utiliserait pas.
- **Moyen** : gêne réelle avec un contournement.
- **Mineur** : finition (texte, alignement, mot).
- **Idée** : pas un défaut, à garder pour plus tard.

## 7. La boucle
1. Audits automatiques (§5), puis un tour de 6 sièges.
2. Synthèse : dédoublonner, classer, séparer banc et appli (chaque défaut est reproduit avant d'être corrigé).
3. Corrections par lots en parallèle :
   - tout Bloquant, Grave et Moyen ;
   - tout ce que ≥ 2 testeurs ont dit ;
   - les Mineurs groupés.
4. Revue neuve, tests complets, captures regardées.
5. Tour suivant : testeurs neufs, autres langues, parcours qui couvrent aussi les zones corrigées.
6. **On s'arrête** quand ces 4 conditions sont réunies :
   - un tour complet ne trouve ni Bloquant ni Grave, et les Moyens se comptent sur les doigts d'une main ; chaque écran a au moins 8/10 sur chaque critère des auditeurs d'ergonomie et de design (§2 bis) ;
   - les 15 langues sont passées ;
   - le banc des morceaux est dans les cibles ;
   - la matrice de captures est propre.
7. Ensuite, Benjamin teste sur son téléphone et sa tablette : micro, MIDI, haut-parleur, ressenti.

## 8. Ce qui est prêt et ce qui reste à préparer
- **Prêt** : le banc testeur humain (6 sièges), le banc des morceaux, les briefs des tours précédents, les tests automatiques.
- **En préparation** (équipe Banc 2) : la langue par siège, la commande `ecoute` (rendu par le vrai moteur, rapport et spectrogramme), la conservation et l'analyse des fichiers exportés, les sons des packs lisibles au rendu.
- **À faire avant le tour 1** : le lot « Master pro » (§1 bis).
- **À préparer avant le tour 1** :
  - les profils d'appareil par siège (petit téléphone, tablette, paysage, 130 %) ;
  - la matrice de captures × 15 langues avec détection des débordements ;
  - les détecteurs de cibles tactiles, de contraste et de libellés ;
  - les briefs par rôle (débutant, pro, ingénieur du son, ergonome, designer, accessibilité) ;
  - les gabarits de rapport.

## 9. Le coût
- **Par tour** : 6 testeurs, 1 synthèse, 3 à 5 équipes de correction, 1 revue neuve. C'est l'ordre de grandeur des tours du tuto.
- **Au total** : 3 tours au minimum pour faire passer les 15 langues, plus ceux qu'il faut pour qu'il ne reste plus de grave.
- **Les audits automatiques** coûtent peu : ils passent avant chaque tour pour économiser les testeurs.
