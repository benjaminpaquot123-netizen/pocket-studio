# L'Audit de BenStudio (version unique, 2026-11-01)

Ce document **remplace et réunit** :
- le plan d'origine `PLAN-AUDITS-FINAUX.md` §1 à §9 (axes, testeurs, parcours, langues, gravité, boucle, Master pro) ;
- la boîte à astuces §10 ;
- « tout est connecté » §11 ;
- les ajouts smartphone/tablette de `docs/etudes/USAGE-TABLETTE-SMARTPHONE.md` §6.

Les sections de l'ancien plan restent la référence pour le détail des rôles (§2, §2 bis), du lot Master pro (§1 bis) et des parcours (§3). Ce document fixe l'ordre, les règles, les critères d'arrêt et ajoute ce que la revue par experts a trouvé.

**Quand :** à la toute fin des modifications, seulement sur le signal de Benjamin.
**Qui décide de l'ordre :** l'agent (décision de Benjamin du 2026-11-01).
**Le mot « Audit »** désigne tout ce document.

---

## 0. Revue par experts : ce qui a changé
Les trois plans ont été relus sous l'angle de 9 métiers. C'est une relecture faite par l'agent en se mettant tour à tour dans la peau de chaque expert, pas par de vraies personnes. Les vrais humains arrivent à l'étage 7.

| Expert | Ce qui manquait dans les plans | Ajouté ici |
|---|---|---|
| **Responsable qualité (QA lead)** | Pas de priorité par le risque ; pas de critères d'entrée ; pas de jeu de données de référence ; arrêt jugé « au ressenti » ; pas de règle pour les tests instables | Carte des risques (§2), gel des fonctions pendant l'Audit, projets et sons de référence, courbe des découvertes pour savoir quand s'arrêter, quarantaine interdite des tests instables (on les répare) |
| **Ingénieur audio temps réel** | Rien sur les tailles de tampon, le changement de sortie en pleine lecture, la régression sonore d'une version à l'autre, la chauffe | Rendu « maître » comparé à la version précédente, tampons de 64 à 1024, bascule haut-parleur ↔ casque ↔ Bluetooth en direct, coupure et reprise d'AAudio, charge de l'interface pendant le son, chauffe sur 30 min |
| **Ingénieur plateforme Android** | Les tests tournaient sur la version de debug ; rien sur les tueurs d'applis des constructeurs, les permissions récentes, le démarrage, les « Android Vitals » | Tout l'Audit sur la **version release** (R8), matrice d'appareils, MagicOS (Honor) et autres tueurs d'arrière-plan, service de premier plan et session média, permissions Android 13/14, démarrage à froid, taux d'ANR et de plantage, mise à jour depuis l'APK précédente |
| **Chercheur en expérience utilisateur** | Mesures sans taux de réussite ; un seul évaluateur ; pas d'apprentissage dans le temps ; le plaisir non mesuré | Taux de réussite par tâche, note de facilité après chaque tâche (SEQ), questionnaire SUS ; 3 évaluateurs indépendants (Nielsen : 3 à 5 en trouvent environ 75 %) ; jour 1 / jour 3 ; « temps jusqu'au premier waouh » |
| **Expert accessibilité** | WCAG cité sans liste ; TalkBack seulement sur des écrans isolés | Parcours complets à TalkBack et à l'accès par contacteur ; police à 200 % ; simulation des daltonismes ; vibration en plus du visuel ; tableau WCAG 2.2 AA critère par critère |
| **Expert localisation** | Contrôle seulement une fois les traductions faites | **Pseudo-traduction** (textes +40 %, accents, droite à gauche) pour trouver les débordements avant les vraies langues ; formats de nombres et pluriels ; relecture « locuteur natif » des écrans clés |
| **Expert données et sécurité** | Rien sur l'écriture atomique, les zips piégés, la vie privée | Coupure pendant l'écriture, zip malveillant (chemins « ../ », fichiers énormes), migration de chaque ancienne version, vie privée (rien n'est envoyé), formulaire « sécurité des données » du Play Store, licences |
| **Expert performance** | Rien sur la fluidité, la mémoire, la batterie, la taille | Images à 120 Hz sans saccade, fuites mémoire, batterie par heure de Live, taille de l'APK et du stockage, téléphone à 3 Go de mémoire |
| **Producteur / DJ (métier)** | Le Live, le Pad Live et la découpe n'étaient pas dans les parcours ; pas de vraie séance de scène | Parcours Pad Live, découpe du Remix, séance de scène de 45 min au MiniLab, comparaison minutée avec Koala et Groovepad |

---

## 1. Principes
1. **Le risque d'abord.** On audite d'abord là où une panne coûte le plus et arrive le plus souvent (§2).
2. **Tout est reproductible.** Graine du hasard notée, projet de départ joint, vidéo ou son à l'appui ; un défaut non reproduit n'est pas corrigé au hasard, il est d'abord reproduit par un test.
3. **Chaque défaut a son oracle**, c'est-à-dire sa façon de savoir que c'est faux : une règle, une mesure, une comparaison ou une référence.
4. **Règle « partout » de Benjamin** : chaque défaut est cherché partout où il peut se reproduire, et corrigé à la racine, avec un test qui couvre tous les cas. Chaque changement de design s'applique partout.
5. **Version release.** On audite l'APK signée comme celle de Benjamin (R8 actif), pas la version de développement.
6. **Gel.** Pendant un tour, on ne développe aucune nouvelle fonction : seulement des corrections, sur des branches à part.
7. **Économie.** On commence par ce qui ne coûte presque rien (automatique), et on finit par ce qui coûte le plus (équipes IA, humains). On ne relance que ce que les corrections ont touché, sauf au tour final où l'on relance tout.

## 2. Carte des risques (où chercher en premier)
| Zone | Gravité si ça casse | Probabilité | Priorité |
|---|---|---|---|
| Sauvegarde, réouverture, migration, import de zip | Perte du travail | Moyenne | **1** |
| Moteur audio en direct (Live, tempo, Remix étiré) | Craquements sur scène | Moyenne | **1** |
| Export et master | Fichier faux ou abîmé | Faible | **1** |
| Remix (séparation, 4 pistes, découpe, notes) | Fonction phare cassée | Haute (récent) | **1** |
| Live et Pad Live (cases, pads, enregistrement, MIDI) | Fonction phare cassée | Haute (récent) | **1** |
| Tutos, Guide, « Montre-moi » | Débutant perdu | Haute (défauts n° 19 et 20) | **2** |
| Navigation, Mode simple, accueil | Débutant perdu | Moyenne | **2** |
| 15 langues, tailles d'écran, tablette | Écran illisible | Moyenne | **2** |
| Réglages, aide, rapport de bug | Gêne | Faible | **3** |

## 3. Préparation (une fois, avant le tour 1)
- **Projets de référence**, figés et versionnés hors du dépôt public quand ils contiennent de la musique protégée :
  - un projet vide ;
  - un petit projet ;
  - un projet énorme (100 mesures, 16 pistes, 16 pads, chanson séparée) ;
  - un projet de chaque ancienne version de format ;
  - l'Acidcore de Benjamin ;
  - le projet Remix « ADÉLA - Boys » (chanson de test locale, jamais commitée).
- **Sons « maîtres »** : l'export de chaque projet de référence avec la version de départ ; chaque tour compare au maître (test de nullité, tolérance chiffrée).
- **Matrice d'appareils** (émulateurs et profils) :
  - petit téléphone 360 dp à 3 Go de mémoire ;
  - téléphone standard ;
  - le Honor de Benjamin (MagicOS 11) ;
  - grand téléphone ;
  - tablette 8" ;
  - Galaxy Tab A11 ;
  - un pliable (ouvert et fermé) ;
  - chacun en portrait et en paysage, en écran partagé, à 100 %, 130 % et 200 %.
- **Instruments de mesure** : LeakCanary, StrictMode, détecteur d'ANR, traces Perfetto, compteur d'images, ASan/UBSan/TSan pour le moteur, couverture Jacoco, PIT (mutations), générateur de paires (type NIST ACTS), carte des fonctions (§5, étage 2).
- **Pseudo-traduction** : une 16e « langue » de test, avec des textes plus longs, des accents et une écriture de droite à gauche.
- **Gabarits** : fiche de défaut, tableau de bord du tour, briefs par rôle.

---

## 4. Les étages de l'Audit (dans cet ordre)
Chaque étage doit être vert avant de passer au suivant, sauf les découvertes d'étages plus tardifs, qui reviennent dans la boucle de correction.

### Étage 0 : santé du code et de la version release
- Compilation de la version release, signée, avec R8 ; démarrage, écran par écran, en release (les bugs propres à R8 apparaissent ici).
- Lint Android, detekt, avertissements du compilateur ; ressources et textes inutilisés.
- Tests moteur sous ASan, UBSan et TSan.
- Taille de l'APK, temps de démarrage à froid (objectif : moins de 1,5 s sur le téléphone standard).
- Licences tierces à jour (`THIRD_PARTY.md`), aucun secret dans le dépôt, aucune chanson protégée dans le dépôt.

### Étage 1 : contrôles automatiques massifs
- **Captures** : chaque écran × 16 langues (dont la pseudo-traduction) × matrice d'appareils. On détecte les textes coupés, les chevauchements et ce qui sort de l'écran, puis on fait relire chaque capture par un « œil » IA.
- **Accessibilité** : cibles d'au moins 48 dp, contrastes, libellés, ordre de lecture TalkBack (Accessibility Test Framework).
- **Son** : banc des 37 styles, kick dans le mix, sonie et true peak, comparaison au son maître, direct = export.
- **Exports** : matrice format × fréquence × bits × cible × plafond, nullité des pistes séparées, allers-retours MIDI et projet.
- **Performance** : saccades par écran (images à plus de 16 ms, puis 8 ms à 120 Hz), mémoire, fuites.
- **Tous les tests existants** (plus de 6 300), à la recherche des tests instables (relancés 3 fois ; un test instable est réparé, jamais mis de côté).

### Étage 2 : tout est connecté et prévu comme il faut
1. **Carte des fonctions** (matrice de traçabilité). Une ligne par fonction (Pad Live, cases, briques, Remix et découpe, tempo, effets, 303, notes, accords, enregistrement, bibliothèque, export, tutos, Guide…). Une colonne par branchement attendu :
   - visible en Simple et en Pro ;
   - sur chaque classe de taille d'écran ;
   - dans les 15 langues ;
   - annulable ;
   - sauvegardée et rouverte ;
   - exportée ;
   - direct = export ;
   - pilotable au MiniLab et au MIDI ;
   - prise dans l'enregistrement de la performance ;
   - accessible ;
   - expliquée dans l'aide et couverte par un tuto ou le Guide ;
   - mêmes mots et mêmes icônes qu'ailleurs.

   Chaque case pointe vers un test marqué. **Une case vide est un oubli à corriger.**
2. **Modèle en machine à états** de l'appli (écrans, onglets, modes, lecture, enregistrement, Live). Parcours de **toutes les transitions** : aucun cul-de-sac ; retour arrière toujours possible ; chaque fonction atteignable depuis l'accueil en 3 touchers au plus en Mode simple.
3. **Interactions deux à deux.** Exemples :
   - tempo changé pendant un enregistrement ;
   - Remix et Live ensemble ;
   - annuler pendant un « Montre-moi » ;
   - MiniLab débranché en boucle ;
   - passage Simple ↔ Pro en lecture ;
   - langue changée projet ouvert ;
   - rotation pendant la découpe ;
   - appel pendant l'export.

   Le générateur de paires choisit le minimum de tests.
4. **Allers-retours de toutes les données**, avec la liste des champs lue dans le code : sauvegarder puis rouvrir, exporter puis importer, annuler puis rétablir, copier une brique, migrer depuis chaque ancienne version.
5. **Contrats interface ↔ moteur** : chaque commande a son effet sonore vérifié, et chaque capacité du moteur a son bouton.
6. **Règles d'architecture** (Konsist) : un seul navigateur de sons, un seul muet/solo (icônes haut-parleur et casque), un seul réglage du tempo, un seul chemin d'annulation, un seul style de pad et de case.
7. **Rien de mort ni d'orphelin** : code jamais appelé, textes jamais affichés, réglages sans effet, écrans inaccessibles.
8. **Test « une semaine dans la vie »**, enchaîné d'un bout à l'autre :
   - installer, faire un tuto, faire un morceau ;
   - remixer ADÉLA - Boys, découper la voix, jouer au Pad Live et au MiniLab, enregistrer ;
   - rouvrir 3 jours après, exporter ;
   - mettre à jour l'appli.

   À chaque étape, tout ce qui a été fait avant doit être encore là.
9. **Cohérence** (oracles FEW HICCUPPS) : ce qu'on voit = ce qu'on entend ; l'aide et les tutos décrivent l'appli telle qu'elle est.

### Étage 3 : robustesse et chaos
- **Chaos Android** :
  - l'appli tuée en arrière-plan ;
  - rotation, écran partagé, pliable ouvert ou fermé en pleine lecture ;
  - mémoire basse, stockage plein ;
  - permission micro retirée en enregistrement ;
  - appel et perte du focus audio ;
  - casque filaire, Bluetooth et USB branchés ou débranchés en direct ;
  - MiniLab à chaud ;
  - économie de batterie ;
  - changement de fréquence d'échantillonnage ;
  - mode sombre, police à 200 %, droite à gauche.
- **Constructeurs** : tueurs d'arrière-plan (MagicOS, MIUI…), lecture qui continue écran éteint, session média et notification, mise en veille.
- **Fuzzing et propriétés** :
  - milliers de gestes au hasard rejouables (Android `monkey` et notre générateur à graine) ;
  - suites d'éditions aléatoires sur le modèle, avec des invariants : pas de plantage, pas de NaN, rien de perdu, l'annulation revient toujours.
- **Fichiers piégés** : projets tronqués, valeurs folles, zips malveillants (chemins « ../ », bombe de compression), sons corrompus ou énormes.
- **Écriture atomique** : coupure pendant la sauvegarde, pendant l'export et pendant la séparation ; tout reprend proprement.
- **Mutations** (PIT) sur la sauvegarde, l'annulation, le tempo et l'export : les tests doivent tomber quand on casse le code.

### Étage 4 : audio expert
- **Sur ordinateur** :
  - tailles de tampon de 64 à 1024 ;
  - 44,1 et 48 kHz ;
  - pire cas (16 pads, 8 boucles, chanson séparée étirée, effets, master) ;
  - changement de tempo en direct ;
  - 10 minutes de calage à l'échantillon près ;
  - zéro allocation, verrou ou log dans le rappel audio ;
  - charge de l'interface (défilement, animations) pendant le son.
- **Sur le téléphone de Benjamin**, liste à lui donner :
  - OboeTester (latence aller-retour, test de glitchs, compteur XRuns) ;
  - trace Perfetto pendant le pire cas ;
  - 30 minutes de Live pour la chauffe ;
  - casque Bluetooth (message et compensation) ;
  - MiniLab 3.
- **Écoute** : l'ingénieur du son écoute à l'aveugle (test ABX) les exports contre le son maître, et juge le master Spotify et le master club.

### Étage 5 : utilisabilité experte
- **3 évaluateurs indépendants**, chacun avec les 10 heuristiques de Nielsen et les heuristiques mobiles, écran par écran ; on fusionne ensuite les trouvailles.
- **Parcours cognitif** de chaque tâche clé : à chaque pas, la personne sait-elle quoi faire, voit-elle le bouton, comprend-elle le résultat ?
- **Visites exploratoires** en séances de 90 minutes, avec des notes (tours de Whittaker, FCC CUTS VIDS, SFDIPOT).
- **Designers seniors**, notés à 8/10 minimum sur 6 critères (ancien §2 bis), avec la règle du design partout identique.
- **Accessibilité** : tableau WCAG 2.2 AA critère par critère, parcours complets à TalkBack et à l'accès par contacteur, daltonismes simulés.
- **Localisation** : la pseudo-traduction d'abord, puis une relecture « locuteur natif » des écrans clés dans chaque langue.
- **Smartphone et tablette** :
  - pads à une main (zone du pouce) et à deux mains ;
  - tablette à dix doigts ;
  - clavier physique, souris et stylet ;
  - les 5 classes de taille d'écran.
- **Chasse au jargon** : chaque texte noté par un « débutant » ; même mot pour la même idée, dans les 15 langues.

### Étage 6 : testeurs IA à personnalités (tours)
- **Chaque tour : 6 à 8 sièges neufs.** Personnalités :
  - enfant de 8 ans ;
  - ado trap ;
  - débutant adulte ;
  - DJ en soirée ;
  - producteur FL Studio ;
  - ingénieur du son ;
  - senior qui découvre ;
  - daltonien ou malvoyant ;
  - non francophone.
- Un parcours, une langue et un appareil par siège ; un « œil » compare l'écran attendu à l'écran obtenu (bugs qui ne plantent pas).
- **Parcours** : ceux de l'ancien §3, plus :
  - le Pad Live depuis l'accueil (moins de 5 s jusqu'au premier son) ;
  - la découpe du Remix ;
  - « Piocher dans la chanson » ;
  - les tutos avec « Montre-moi » ;
  - le travail sur tablette ;
  - une séance de scène de 45 min.
- **Mesures** :
  - taux de réussite, temps et nombre de touchers par tâche ;
  - note de facilité (SEQ) après chaque tâche, questionnaire SUS à la fin ;
  - erreurs et retours en arrière ;
  - « temps jusqu'au premier waouh ».
- **Comparaison minutée** avec Koala et Groovepad pour les mêmes tâches (d'après leurs parcours connus).
- **Langues par tour** : ancien §4 (les 15 passent).

### Étage 7 : les vrais humains (en dernier)
- **Testeurs réels externes (décision de Benjamin, 2026-11-01 : « pour la fin »)** : test fermé du Google Play (au moins 12 testeurs pendant 14 jours d'affilée, exigé pour un compte personnel récent avant la publication), recrutés dans l'entourage puis chez des musiciens (fils de retours de r/WeAreTheMusicMakers, r/edmproduction, r/makinghiphop, Audiofanzine, Discord de producteurs ; règles lues avant de poster ; rien posté sans l'accord de Benjamin) ; message prêt en français et en anglais.
- **Bug bash d'une heure** : Benjamin, sa fille et l'ami de WhatsApp. Chacun a une mission ; l'écran est filmé et chacun pense à voix haute.
- **Journal sur 3 jours** : ce qu'on a fait, ce qui a coincé, ce qui a fait plaisir (apprentissage dans le temps).
- Les retours vont dans `docs/RETOURS.md`, puis passent par la règle « partout ».

---

## 5. Gravité et tri
- **Niveaux** (ancien §6) : Bloquant, Grave, Moyen, Mineur, Idée.
- **Priorité** = gravité × fréquence (combien de sièges ou de cas) × persistance (contournable ou non), et ×2 dans les zones de priorité 1 de la carte des risques.
- **Chaque défaut reçoit une cause racine** (oubli de branchement, état non sauvegardé, texte, mise en page, son, performance, plateforme…). Trois défauts de la même cause = une correction de fond, pas trois rustines.

## 6. La boucle et les critères d'arrêt
1. Les étages 0 à 5 sont verts.
2. Tour de l'étage 6, puis synthèse : dédoublonnage, tri, reproduction par un test.
3. Corrections du plus grave au plus léger (une équipe à la fois, en économie), avec la règle « partout ».
4. Revue neuve, tests complets, relance ciblée des étages touchés.
5. Tour suivant, avec des sièges neufs, d'autres langues, et un test des zones corrigées.

**On s'arrête quand tout est vrai en même temps :**
- **2 tours de suite** sans Bloquant ni Grave, et au plus 5 Moyens ;
- **courbe des découvertes aplatie** : le dernier tour trouve moins de 20 % des défauts du premier ;
- **carte des fonctions** sans aucune case vide ;
- **captures** propres dans les 15 langues et la matrice d'appareils ;
- **accessibilité** sans aucune erreur automatique et tous les critères WCAG AA tenus ;
- **son** : le banc dans les cibles, le rendu maître identique, direct = export, zéro XRun dans le pire cas sur ordinateur ;
- **performance** : démarrage en moins de 1,5 s, aucune saccade au-delà du seuil, aucune fuite ;
- **utilisabilité** :
  - au moins 90 % de réussite sur chaque tâche clé ;
  - SUS d'au moins 80 ;
  - premier son du Pad Live en moins de 5 s ;
  - chaque écran à 8/10 minimum.

Ensuite seulement, l'étage 7 (Benjamin et ses proches), puis une dernière relance complète.

## 7. Ce que chaque tour livre
- Un **tableau de bord** d'une page : les compteurs par gravité, la courbe des découvertes, les cases vides de la carte, les mesures son et performance, les notes des designers.
- Pour chaque défaut : une fiche avec la capture annotée ou le son, la cause racine, le test qui le reproduit, et la vérification « partout ».
- Les captures « après » sans la pastille de retours, et les extraits sonores avant/après.

## 8. Coût et économie
- **Étages 0 à 4** : surtout automatiques, peu coûteux, relancés seulement là où l'on a corrigé.
- **Étage 5** : quelques équipes, une à la fois.
- **Étage 6** : de 3 tours au minimum (pour que les 15 langues passent) jusqu'aux critères d'arrêt.
- **Étage 7** : le temps de Benjamin.
- Les équipes travaillent une à la fois (consigne de Benjamin), avec une consigne courte, et ne lancent que les tests utiles.

## 9. Ce que Benjamin fera (une seule liste, le moment venu)
1. Installer l'APK d'audit.
2. Lancer OboeTester : latence et glitchs.
3. Faire 30 minutes de Live au MiniLab.
4. Essayer un casque Bluetooth.
5. Faire une trace Perfetto (guide pas à pas fourni).
6. Faire le bug bash d'une heure avec sa fille et son ami.
7. Tenir le journal sur 3 jours.

Sources (en plus de celles des §10 et §11 de l'ancien plan et de l'étude smartphone/tablette) : [Android Vitals (Google)](https://developer.android.com/topic/performance/vitals) · [Qualité des applis grand écran (Android)](https://developer.android.com/docs/quality-guidelines/large-screens-app-quality) · [Nielsen : combien d'évaluateurs](https://www.nngroup.com/articles/how-to-conduct-a-heuristic-evaluation/) · [WCAG 2.2](https://www.w3.org/TR/WCAG22/) · [OboeTester](https://github.com/google/oboe/blob/master/apps/OboeTester/docs/Usage.md) · [Don't kill my app](https://dontkillmyapp.com/)
