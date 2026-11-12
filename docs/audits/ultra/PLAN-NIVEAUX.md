# Règle des niveaux : des sièges par niveau, pour tester comme de vrais utilisateurs

Écrit le 2026-11-12, dans la discussion « Plan d'audit utilisateur », à la demande de Benjamin :
> « Fais un plan d'audit, car actuellement il n'est pas optimisé pour tester le programme comme de vrais utilisateurs. Débutant, moyen, pro, etc. »

> « J'aimerais que la règle générale de l'audit soit intégrée une fois que tu l'as terminée. Sois autonome. »

**Statut : règle générale de l'Audit** (principe 9 de `docs/audits/AUDIT.md`, rappelée dans `CLAUDE.md`), à partir du premier tour de testeurs lancé après le tour 6. **Le tour 6 en cours ne change pas** : son plan, ses huit sièges et sa règle de sortie restent ceux de `PLAN-TOUR-6.md`. Rien n'est lancé par ce fichier ; le lot du banc du § 3 est inscrit dans `PLAN-RESTE.md` § 6.

**La règle en une phrase** : tout tour de testeurs a au moins un siège par niveau (0 curieux, 1 débutant motivé, 2 moyen, 3 confirmé, 4 pro), chacun avec une mission de son niveau dont l'oracle lit un résultat précis ; `libre` ne compte pour aucun niveau ; le tableau de bord se lit par niveau. Un tour de contrôle ciblé (comme le tour 6) peut y déroger s'il l'écrit dans son plan.

Ce fichier remplace une première version (commit `47fe20d`), écrite sur une copie périmée du dépôt, qui ignorait le banc « testeur humain » et les tours 0 à 6.

## En trois lignes
1. Le banc teste déjà à l'aveugle, et les modes Simple et Pro sont couverts. Ce qui manque, c'est le **niveau de la mission** : un siège « Pro » réussit aujourd'hui avec un son et un changement gardé, comme un débutant.
2. On ajoute une échelle de cinq niveaux, du curieux au pro, avec des missions qui demandent un **résultat précis et vérifiable** (refaire une boucle imposée, partir d'un projet vide, sortir des pistes séparées, tenir un set).
3. Trois choses qu'un vrai utilisateur fait et que le banc ne sait pas restent nommées, avec où elles se testent : écouter, revenir trois jours après, être interrompu.

## 1. Le constat, compté dans les fiches de sièges
Source : `sieges/tour-3` à `tour-6` (`sieges.tsv`, colonnes `mode` et `mission`). Les tours 0 à 2 n'ont pas ce fichier : non comptés. Les rapports des sièges n'ont pas été relus pour ce constat.

| Tour | Sièges | Simple | Pro | Mode choisi par le siège | Mission « libre » |
|---|---|---|---|---|---|
| 3 | 24 | 10 | 8 | 6 | 6 |
| 4 | 24 | 10 | 10 | 4 | 10 |
| 5 | 24 | 9 | 10 | 5 | 6 |
| 6 | 8 | 5 | 1 | 2 | 3 |

- **Les modes sont couverts** aux tours 3 à 5 (8 à 10 sièges Pro sur 24). Le tour 6, tour de contrôle, n'a qu'un siège Pro.
- **Les missions ne montent pas en niveau.** Le banc connaît douze missions (`seat-metrics.py`, `MISSIONS`) : `premier-morceau`, `simple`, `tuto-facile`, `tuto-pro`, `defi`, `padlive`, `voix`, `remix-voix`, `exports`, `synthe`, `reprendre`, `libre`. Onze ont leurs trois buts dans `fabrique-sieges.py` (`GOALS`) ; **`reprendre` n'y est pas, et son oracle répond toujours « non »** (« oracle à écrire », faute d'horloge) : elle n'a été donnée qu'à un siège par tour aux tours 3 et 4, à aucun aux tours 5 et 6. Aucune ne demande de refaire un résultat imposé ; les oracles lisent peu (`padlive` : un son au premier toucher ; `voix` : une prise sur une piste).
- **`libre` est le filet des sièges Pro**, et son oracle dit peu : « un son a joué », plus « un changement gardé » ou « un fichier partagé ». Un producteur qui n'arrive pas à faire son morceau est quand même compté « a fini ». `PLAN-TOUR-6.md` § 4 le dit déjà : « a fini » n'est pas « a réussi ».
- **Il manque trois profils entiers** : le « moyen » qui arrive d'une autre appli avec ses mots, le « confirmé » qui refuse l'assistant et part d'un projet vide, le « pro » qui cherche les limites (gros projet, pistes séparées, set long).

## 2. L'échelle : cinq niveaux, seize sièges
Le tour complet ; le minimum de la règle est la version courte du § 7 (un siège par niveau et le casse-tout).
Colonnes comme `sieges-proposes.tsv`. « Mission du banc » : une mission qui existe, ou **une mission neuve** (§ 3). Le profil dit le niveau, le but dans les mots de la personne, et les maladresses à commettre (§ 4).

| Niveau | Siège | Profil et but | Mode | Mission du banc | Réussi si |
|---|---|---|---|---|---|
| 0 Curieux | N0 a | Lycéenne, jamais fait de musique : « un son pour ma story, vite » | Simple | `premier-morceau` | un fichier partagé, sans l'aide de l'appli |
| 0 | N0 b | Le pressé : ne lit rien, « Plus tard » partout, prend Pro par erreur | choisi par le siège | `libre` | un son en 5 gestes au plus ; jamais coincé |
| 0 | N0 c | 55 ans, police 130 %, néerlandais : « un morceau calme pour ma fille » | Simple | `simple` | morceau partagé ; aucun texte coupé |
| 1 Débutant motivé | N1 a | Fan de jump up, suit le Guide jusqu'au bout : « un morceau à moi » | Simple puis Pro | **`mon-morceau`** | sons, mélodie et structure différents de ceux de l'assistant |
| 1 | N1 b | Ado trap : « ma voix sur une instru » | Simple | `voix` (oracle à durcir, § 3) | la prise est sur une piste du projet sauvé, et un fichier est partagé |
| 1 | N1 c | « Enlever la voix d'une chanson et rejouer dessus » | Simple | `remix-voix` | les trois buts de la mission |
| 1 | N1 d | Revient sur son morceau, change un son, puis veut l'ancien | Simple | `reprendre` (oracle à écrire, § 3) | l'ancien son est revenu, rien d'autre n'a bougé |
| 2 Moyen | N2 a | Vient de BandLab ou FL Studio Mobile, cherche « piano roll », « quantize », « BPM » | Pro | **`boucle-imposee`** | la boucle de la fiche est dans le projet, note pour note |
| 2 | N2 b | Beatmaker : un sample à découper, charleston en roulements, 808 | Pro | **`beat-sample`** | sample découpé sur les pads, beat exporté |
| 2 | N2 c | Bidouilleur d'effets : « le filtre qui monte avant le Drop » | Pro | **`courbe`** | la courbe existe sur la bonne partie du projet sauvé, et un fichier est exporté |
| 3 Confirmé | N3 a | Producteur techno, **projet vide**, refuse l'assistant | Pro | **`projet-vide`** | 6 pistes et 8 parties au moins, fichier exporté |
| 3 | N3 b | Compositeur : accords, gamme, mélodie, export MIDI | Pro | **`midi`** | le fichier MIDI relu a les mêmes notes |
| 4 Pro | N4 a | Ingénieur du son : « les pistes séparées pour mon logiciel » | Pro | **`pistes-separees`** | une piste par instrument, même durée, bons formats |
| 4 | N4 b | Producteur live : un set long, pads, effets, performance gardée | Pro | `padlive`, budget long (oracle à durcir, § 3) | la prise est gardée, rien d'écrasé |
| 4 | N4 c | Le projet monstre : 8 pistes, 24 parties, des courbes partout | Pro | **`monstre`** (projet fourni) | ouvrir, jouer, modifier, sauver, exporter sans erreur |
| Hors niveau | X | Le casse-tout : touche à tout, double toucher, Retour au pire moment | choisi par le siège | `libre` | pas de gel, pas de projet perdu, « annuler » fidèle |

Appareils, langues et police se tirent comme aux tours 3 à 5 ; N0 a, N1 a, N2 a et N3 a sont doublés sur tablette.

## 3. Ce qu'il faut ajouter au banc
Règle de `CLAUDE.md` : l'outil de mesure se vérifie avant de mesurer. Chaque point est un lot du banc, avec un test rouge puis vert, et `selftest.sh` vert avant tout siège.

1. **Huit missions neuves avec leur oracle** : `mon-morceau`, `boucle-imposee`, `beat-sample`, `courbe`, `projet-vide`, `midi`, `pistes-separees`, `monstre`. L'oracle lit le projet sauvé et les fichiers exportés, pas l'écran. Pour `boucle-imposee`, la fiche du siège donne le rythme, la basse et les accords ; l'oracle compare les notes.
   - **Trois oracles de missions qui existent sont à écrire ou à durcir**, sinon ces sièges réussissent comme aujourd'hui : `reprendre` (N1 d : aujourd'hui toujours « non », et absente de `GOALS`), `voix` (N1 b : aujourd'hui « une prise sur une piste »), `padlive` (N4 b : aujourd'hui « un son au premier toucher »). Les « Réussi si » des sièges N0 b, N0 c et X (gestes, texte coupé, gel) se lisent dans les mesures et les fiches du siège, pas dans l'oracle de la mission.
   - **Limite du faux moteur** (consignes v2, familles `son` et `chiffres`) : sous le banc, le son d'un fichier exporté n'est pas celui du téléphone. L'oracle lit donc dans le projet sauvé ce qui a été fait (prise, courbe, notes, pads) et, pour un export, que le fichier existe, combien il y en a, leur format et leur longueur ; ce que le fichier fait entendre (la voix, la courbe) relève du rapport d'écoute au vrai moteur (§ 6). À prouver par le lot du banc, test rouge puis vert : rien de cela n'est mesuré ici.
2. **Deux fichiers de plus dans le sélecteur du banc** : un sample pour `beat-sample`, le projet monstre en .zip pour `monstre` (le banc sait déjà offrir une chanson, un .zip et un .mid).
3. **Un niveau par siège** dans `sieges-proposes.tsv` (colonne `niveau`, 0 à 4 ou X), recopié dans `mesures.json`.
4. **Le tableau de bord par niveau** (§ 5).

Pas besoin de nouveau pilote : `human-act.py` et les consignes v2 font déjà le test à l'aveugle.

## 4. Trois règles à ajouter aux consignes des testeurs (v3)
1. **Dire ce qu'on attend avant un geste qui compte** (ouvrir un écran neuf, valider, effacer) : une ligne « j'attends : … », puis « j'ai eu : … ». Un écart est un défaut `surprise`, même si la mission continue.
2. **La règle des trois essais** : trois tentatives ratées, puis l'aide de l'appli seulement (« ? », Guide, tutos). Si l'aide ne suffit pas : « bloqué » dans le rapport, puis on passe au but suivant. Le rapport compte les appels à l'aide.
3. **Deux maladresses imposées par fiche** (mauvais bouton, effacer par erreur, refuser le micro, revenir en arrière en pleine prise) : on regarde si l'appli aide à s'en sortir.

## 5. Ce qu'on lit à la fin : une ligne par niveau
| Niveau | Sièges | Réussi sans aide | Avec l'aide de l'appli | Bloqué | Gestes faits / chemin idéal | Gestes avant le premier son | Surprises | Mots incompris |
|---|---|---|---|---|---|---|---|---|

- Le chemin idéal de chaque mission neuve est rejoué une fois par étiquettes (un rejeu), pour avoir le dénominateur.
- **Gravité selon le niveau** : un « bloqué » aux niveaux 0 et 1 est bloquant ; aux niveaux 2 à 4 il est grave, sauf perte de données. La grille des défauts ne change pas.
- **Interdit**, comme au tour 6 : comparer ces chiffres à ceux des tours 3 à 5, dont les missions étaient autres.

## 6. Ce que ce tour ne saura toujours pas, et où ça se teste
| Ce que fait un vrai utilisateur | Le banc | Où le tester |
|---|---|---|
| **Écouter** et juger le son | faux moteur : dit si ça joue, pas comment | après le siège, son projet sauvé joué par le vrai moteur (`scripts/live-bench.sh`, `real-projects.sh`) : alertes fondu, silence, saut, volume ; puis « Questions à l'oreille » de BenStudio Test sur le téléphone |
| **Revenir trois jours après** | pas d'horloge | prévu au tour 7 (`PLAN-RESTE.md`, P2) ; d'ici là N1 d joue `reprendre` sans le saut de date, une fois son oracle écrit (§ 3) |
| **Être interrompu** (appel, appli tuée, rotation, casque) | aucun geste du téléphone | l'étage 3 « chaos » de `AUDIT.md`, l'émulateur (« Nightly real use »), la recette de Benjamin |
| Le toucher, la latence, la chaleur | non | le téléphone de Benjamin seulement |

Le premier point est le seul à construire : un petit script qui prend le dossier d'un siège fini et rend son rapport d'écoute. Il se range avec le point 1 du § 3.

## 7. Quand, et combien
- **Après le tour 6**, quand sa règle de sortie est tenue. Jamais pendant.
- Lot du banc d'abord (§ 3 et le script d'écoute), relu à neuf, par le portail.
- 16 sièges en lots de 3, plus 4 doublons tablette, un lecteur et un rédacteur neufs : à peu près le tour 5 en gestes (estimation, non mesurée).
- Version courte si le quota est serré : un siège par niveau (N0 a, N1 a, N2 a, N3 a, N4 a) et le casse-tout, soit 6 sièges et quatre missions neuves seulement (`mon-morceau`, `boucle-imposee`, `projet-vide`, `pistes-separees`).
- Décision de Benjamin du 2026-11-12 (« 1A ») : un passage « Nightly real use » sur émulateur à la fin de l'audit. À raccorder à la règle de CI de `PLAN-TOUR-6.md` § 2, point 7, qui demande déjà la fumée sur émulateur verte.
