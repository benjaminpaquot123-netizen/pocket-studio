# Aide : téléchargements refusés et contrôles capricieux

Écrit le 2026-11-25 à la demande de Benjamin : « quand je te dis "va voir ailleurs, trouve une solution", tu trouves toujours. Que ce soit inscrit dans le marbre. »
À lire dès qu'un téléchargement, une installation ou un contrôle fait perdre du temps. Décisions du lot : `docs/decisions/tour-9-ci-solide.md`.

## La règle, en quatre lignes

1. **Un refus n'est pas une fin.** « Trop de demandes », « rejeté », « délai dépassé » : on change de source tout de suite, sans attendre que Benjamin le dise.
2. **On lit le refus avant d'agir** (tableau ci-dessous) : le bon geste dépend du code, pas de l'humeur.
3. **Deux essais au même endroit, pas trois.** Au deuxième refus identique, on monte d'un barreau sur l'échelle.
4. **On écrit la solution trouvée ici**, dans « Recettes vécues », pour ne jamais la rechercher deux fois.

On ne dit « bloqué » à Benjamin qu'après le dernier barreau, et seulement pour ce qu'il est seul à pouvoir faire (approuver un hôte, un compte, un secret).

## Lire le refus

Dans une discussion du cloud, tout passe par un portier (le proxy, expliqué dans `/root/.ccr/README.md`). Les lignes `[agent-proxy]` à la fin d'une commande ratée donnent l'hôte et la raison.

| Ce qu'on lit | Ce que ça veut dire | Le geste |
|---|---|---|
| 429, 503, « too many requests », « too_many_connections » | Trop de demandes en même temps, chez le portier ou chez le site | Moins de téléchargements à la fois (un seul Gradle, `--max-workers` réduit), attendre une minute, puis **un miroir** |
| 403 ou 407 du portier | L'hôte n'est pas sur la liste autorisée de l'environnement | Ne pas insister, ne pas contourner. Chercher le même fichier sur un hôte autorisé ; sinon demander l'hôte (barreau 5) |
| 403 ou 404 du site lui-même | Mauvaise adresse, version retirée, fichier privé | Vérifier la version et l'adresse, puis un miroir |
| 408, délai dépassé sans message | Connexion ouverte mais rien envoyé, ou outil qui ignore le portier | Réessayer une fois ; puis voir « outil qui ignore le proxy » dans le README du portier |
| « connection reset », « RPC failed » en plein transfert | Tunnel coupé | Réessayer une fois, en reprenant le fichier (`curl -C -`) ; puis un miroir |
| « certificate verify failed », PKIX | L'outil ne connaît pas le certificat du portier | Lui donner `/root/.ccr/ca-bundle.crt`. **Jamais** couper la vérification |

Interdits, toujours : couper TLS, retirer `HTTPS_PROXY`, marteler un hôte refusé, prendre un fichier sans vérifier son empreinte quand une empreinte existe.

## L'échelle « va voir ailleurs »

On monte un barreau à chaque échec, sans demander.

1. **C'est peut-être déjà là.** `~/.m2`, `~/.gradle/caches`, `/opt/android-sdk`, `build/*-cache`, `~/.cache/ps-apt`, un autre dossier de la machine. `find / -name '<fichier>*' 2>/dev/null` coûte dix secondes.
2. **Un miroir du même fichier.** Liste par famille ci-dessous. Toujours comparer l'empreinte (sha1 ou sha256) à celle publiée par la source d'origine.
3. **Un autre chemin vers le même contenu.** Les gestionnaires de paquets sont autorisés dans notre environnement : `apt`, `pip`, `npm`, Maven, Gradle. Un outil introuvable en téléchargement direct existe souvent en paquet (`apt-get install`, `pip install`). Pour GitHub : `git clone` passe par le portier git ; une « release » GitHub se lit avec les outils GitHub de la discussion.
4. **Poser le fichier à la main à l'endroit où l'outil le cherche.** Exemple vécu : le gros fichier de Robolectric posé dans `~/.m2/repository/...`, et l'outil ne télécharge plus rien.
5. **Faire autoriser l'hôte.** `get_environment`, puis `update_environment` avec le nom exact de l'hôte : Benjamin voit une fenêtre et approuve. C'est la voie propre pour un 403 du portier ; une seule demande, avec la preuve (le refus lu).
6. **Faire faire le travail ailleurs.** La CI de GitHub a un réseau libre : un fichier qu'on ne peut pas lire d'ici (émulateur, artefact) se produit ou se lit là-bas, et son journal se lit avec `get_job_logs`.
7. **Seulement alors : « bloqué »**, en une ligne, avec l'hôte, le code lu et les barreaux déjà essayés.

### Miroirs par famille

| Famille | Sources, dans l'ordre |
|---|---|
| Maven Central | `maven-central.storage-download.googleapis.com/maven2`, `repo.maven.apache.org/maven2`, `maven-central-eu.storage-download.googleapis.com/maven2`, `maven.aliyun.com/repository/central` |
| Android (SDK, NDK, émulateur) | `dl.google.com` (autorisé dans l'environnement) par `sdkmanager` ; quatre essais, et on contrôle que les fichiers sont là (`sdkmanager` ne fait qu'avertir quand l'archive est cassée) |
| Paquets système | `apt` avec `-o Acquire::Retries=5` et un délai de 20 s ; les `.deb` gardés dans `~/.cache/ps-apt` (`scripts/ci/apt-install.sh`) |
| Python | `pip install` (index autorisé) ; sinon le paquet système `python3-<nom>` |
| Code source | GitHub par le portier git ; GitLab est parfois surchargé : 10 essais espacés de 30 s (`scripts/ci/submodules.sh`) |

## Recettes vécues

| Date | Le mur | La sortie |
|---|---|---|
| 12-13 nov. | Maven répond 429 en local, Robolectric ne démarre pas | Fichier posé à la main dans `~/.m2` (`docs/decisions/tour-5-ecoute.md`, `tour-6-portail3.md`) |
| 24 nov. | `repo1.maven.org` répond 429 dans la CI, toutes les classes Robolectric tombent | Miroir Google + trois secours, empreinte, cache : `scripts/ci/robolectric-jar.sh` (`docs/decisions/tour-9-maven.md`) |
| 24 nov. | `apt-get install ffmpeg` : 18,5 minutes sur un miroir lent | `scripts/ci/apt-install.sh` : cache de `.deb`, réessais, délai court |
| 25 nov. | Test de nuit rouge : le fichier Robolectric non téléchargé, trois étapes perdues | Le test de nuit prend le même cache et les mêmes miroirs que la CI (`.github/workflows/nightly.yml`) |
| 25 nov. | Artefacts de CI illisibles d'ici (hôte de stockage refusé, 403 du portier) | Lire le journal du job (`get_job_logs`), où les tests écrivent leur verdict ; ne pas insister sur l'artefact |
| oct. | GitLab surchargé pour `eigen` | 10 essais espacés ; depuis le 25 nov. tous les sous-modules ont leurs essais |
| oct. | Image d'émulateur téléchargée à moitié | Boucle de 4 essais avec contrôle du fichier, et cache (`ci.yml`, `nightly.yml`) |
| oct. | 30 jobs de packs téléchargent le modèle (858 Mo) en même temps : erreurs 500 | Un seul téléchargement, partagé par le cache (`packs.yml`, job `model`) |
| 5 nov. | Le portier git refuse de pousser les étiquettes (tags) | Ne pas insister : l'étiquette reste locale, on note le commit (`docs/DECISIONS.md`, 2026-11-05) ; les étiquettes de version viennent de la CI |

## Dans la CI : tout téléchargement est protégé

Règle pour tout nouveau job : un téléchargement a **un cache, des réessais, un secours et une empreinte** quand elle existe. Un `curl` ou un `apt-get` nu ne passe pas la relecture.

| Téléchargement | Protection |
|---|---|
| Fichier Android de Robolectric (213 Mo) | cache + 4 miroirs + sha1, dans la CI **et** le test de nuit |
| Sous-modules (7 GitHub, 1 GitLab) | `scripts/ci/submodules.sh` : 4 essais GitHub, 10 essais GitLab, contrôle final |
| Paquets système (`ffmpeg`, `fdkaac`, `python3-pil`) | `scripts/ci/apt-install.sh` |
| Émulateur et son image | cache + 4 essais + contrôle du fichier |
| Bibliothèque de séparation (onnxruntime) et son modèle | fichier entier d'abord, 5 essais, sha256 pour le modèle, cache |
| Licence du modèle des packs | 5 essais |
| Dépendances Gradle | cache de `setup-gradle` |
| SDK et NDK (`setup-android`) | pas de cache à nous : **reste fragile**, voir plus bas |
| `git clone` et `pip install` des packs | un seul essai : **reste fragile**, lot à part (job hors CI principale) |

## Sur la machine d'une discussion

- L'environnement cloud s'appelle `pocket-studio-android` ; réseau limité, gestionnaires de paquets autorisés, `dl.google.com` autorisé.
- Le SDK Android est dans `/opt/android-sdk` (`local.properties` : `sdk.dir=/opt/android-sdk`). S'il manque dans une discussion neuve, on l'installe par `sdkmanager` depuis `dl.google.com`, un paquet à la fois.
- Un seul Gradle à la fois quand ça télécharge : deux Gradle qui remplissent leur cache ensemble, c'est le chemin le plus court vers un 429.
- Le fichier Robolectric : `scripts/ci/robolectric-jar.sh` marche aussi ici, avant le premier test.

## Contrôles capricieux : les règles

Un contrôle « capricieux » est rouge une fois sur dix sans que l'appli ait changé. Chacun coûte une CI entière (une heure). Les causes vues, et la parade à appliquer partout :

1. **Un test n'appelle jamais le vrai réseau.** Vu : `TutoKickSoundTest` (catalogue des packs), réparé en injectant le catalogue par le crochet de test, un test par issue du réseau (`docs/decisions/tour-9-kick-ci.md`). Encore ouverts : `PackStore.fetchCatalogue` et `UpdateChecker.check` peuvent partir sur Internet dans les tests de la CI.
2. **« La machine était trop lente pour mesurer » n'est pas « l'appli a échoué ».** Vu : `ScenariosTest` écoutait par fenêtres de 60 ms et criait au silence quand la machine dormait. Un test qui mesure dit d'abord s'il a pu mesurer.
3. **Un chronomètre se compare à un témoin.** Vu : `EnduranceTest` (5043 ms pour une limite de 5000 sur un émulateur figé). Le test pose la même question à un fil témoin : ce que la machine a perdu est retiré, la limite de l'appli ne bouge pas.
4. **Aucun seuil n'est élargi pour faire passer** (`docs/DECISIONS.md`). On répare la mesure, pas la limite.
5. **Un test ne laisse rien au suivant.** Vu : `Tour5BloquantsTest` et `TutoScreenTest` rouges selon l'ordre. Chaque test range son état ; le garde `BenchPristine` le dit quand ce n'est pas fait.
6. **« Réparé » se prouve par plusieurs passages forcés** (`--rerun`), dont au moins deux avec la machine chargée, jamais par un seul vert.
7. **Devant un rouge : lire le nom du test avant de relancer.** Un même rouge deux fois n'est pas un caprice. Ne jamais relancer un commit déjà rouge sans rien changer.
8. **Aucun test sauté ni mis de côté pour avoir du vert.**

## Ce qui reste fragile (au 2026-11-25)

- Le correctif du test de nuit n'est sur `main` qu'à la fusion du tour 9 : d'ici là, le test de nuit sur `main` peut rester rouge pour la même raison.
- `PackStore.fetchCatalogue` et `UpdateChecker.check` : réseau possible dans les tests (fichiers de l'appli, lot à ouvrir par le chef).
- La fuite d'état de `Tour5BloquantsTest` (cause non prouvée, lot `tests-fragiles`).
- `setup-android` sans cache, et les `git clone` / `pip install` des packs avec un seul essai.
- Les artefacts de CI ne se lisent pas depuis une discussion : seuls les journaux se lisent.
