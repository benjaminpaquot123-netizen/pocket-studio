# Tour 9, lot « maven » : relecture neuve
Relu : 9408ec0b7

Lu : `git diff 05e3de98a..9408ec0b7` (1787de0b4, fusion 453509a34 de l'intégration, 9408ec0b7) : `scripts/ci/robolectric-jar.sh` (neuf), `.github/workflows/ci.yml` (deux étapes cache + script, job build à 100 min), `app/build.gradle.kts` (propriété `robolectric.dependency.repo.url`), rapport et décisions du lot. Aucun Gradle lancé : les verts sont ceux du rapport.

## Constats
- Appli inchangée : rien sous `app/src/main`, rien sous `engine/`. Aucun lien symbolique commité (aucune entrée 120000), aucun secret ; les deux jobs touchés n'ont toujours pas `SIGNING_PASSWORD`.
- Bon fichier : `16-robolectric-13921718-i7` est bien celui que Robolectric 4.17 (`gradle/libs.versions.toml`) prend pour `sdk=36` (`app/src/test/resources/robolectric.properties`) : c'est celui que Robolectric a lui-même rangé dans `~/.m2` sur cette machine.
- Le dépôt local est bien lu : `MavenArtifactFetcher` (4.17) prend le jar dès qu'il « existe » dans `~/.m2/repository` (« Found %s in local maven repository ») ; le script le pose exactement là, et seulement après contrôle sha1 (`.part` puis `mv`), donc pas de jar à moitié téléchargé laissé en place.
- Le repli tient : `set -uo pipefail` sans `-e`, chaque chemin d'échec finit par `exit 0` ou par un `echo` final (code 0). Si aucun miroir ne répond, Robolectric télécharge lui-même (avec son contrôle SHA-512), comme avant.
- Cache : chemin `~/.m2/repository/org/robolectric`, clé liée à la version de Robolectric et au sdk ; un jar en cache n'est gardé que si son sha1 correspond (quand un miroir donne le sha1).
- Tests locaux : la propriété se surcharge par `-Probolectric.repo=…` ; ici repo1, le miroir Google et repo.maven.apache.org répondent tous 200 ; le jar est déjà dans `~/.m2` local, donc rien ne change pour les tests locaux existants.
- 100 min pour le job build : justifié dans `docs/decisions/tour-9-maven.md` (66,5 min sur une CI verte, coupures à 75 sans rouge).

## Points mineurs (non bloquants)
- Sha1 contournable en théorie : l'empreinte n'est pas écrite dans le script, elle vient du premier miroir qui répond ; si les trois premiers échouent, sha1 et jar viennent tous deux de maven.aliyun.com (miroir tiers). Sha1 est aussi plus faible que le SHA-512 que Robolectric vérifie. À durcir plus tard : écrire l'empreinte (SHA-512 de préférence) en dur dans le script, ou retirer aliyun.
- Pas de plafond de durée sur l'étape : `--max-time 600` vaut par essai, avec `--retry 3 --retry-all-errors` et quatre miroirs, un miroir qui traîne pourrait en théorie manger tout le job au lieu de passer au repli. Un `timeout-minutes: 10` sur l'étape (ou `--retry-max-time`) suffirait.
- `actions/cache` n'enregistre qu'un job vert : tant que le job build est rouge, chaque CI retélécharge les 213 Mo (sans gravité, juste du temps).
- Sur une machine neuve où seul repo1 serait joignable, la propriété remplace repo1 (Robolectric n'a qu'une adresse) : il faudrait `-Probolectric.repo=https://repo1.maven.org/maven2`. Non constaté ici.
- Pas de CI complète sur cette tête (c'est au chef).

Verdict : 0 bloquant, 0 grave — fusion possible
