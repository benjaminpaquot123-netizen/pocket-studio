# Tour 9, lot « maven » (le gros fichier Android de Robolectric et le délai du job) : rapport

Branche : eq/tour-9-maven (venue de a1a447c90, puis l'intégration 05e3de98a fusionnée dedans).
Tout est « tenté » : aucun « corrigé » tant qu'un testeur neuf ne l'a pas constaté.

tests: *FirstLaunchTest

## Chemins

chemin: premier lancement de l'appli (classe Robolectric, qui a besoin du fichier Android de Robolectric) | test: FirstLaunchTest#theFirstScreenIsInThePhonesLanguage

## Ce qui change (« tenté »)
- CI : le fichier Android de Robolectric (213 Mo) est récupéré avant les tests depuis le miroir Google de Maven Central, avec trois miroirs de secours, vérifié par sha1, puis gardé en cache (`scripts/ci/robolectric-jar.sh`, étape dans deux jobs de `ci.yml`, propriété `robolectric.dependency.repo.url` dans `app/build.gradle.kts`). Raison : repo1.maven.org répondait 429 et toutes les classes Robolectric tombaient.
- Si aucun miroir ne répond, le script le dit et Robolectric va chercher le fichier lui-même, comme avant (jamais pire qu'avant).
- Le job « Build, unit tests, screenshots » passe de 75 à 100 minutes : des CI sans test rouge ont été coupées à 75 min (audio1, moteur2), et une CI verte a mis 66,5 min.
- Appli inchangée : rien sous `app/src/main`, rien sur le moteur.

## Tests lancés
- Sur la tête du lot (intégration fusionnée) : `*FirstLaunchTest`, vert (BUILD SUCCESSFUL). En local, aucun miroir n'est joignable : le script a pris le repli prévu (« Robolectric will fetch it itself »).
- CI de 1787de0b4 : plus aucune erreur Maven ; seul rouge, `Tour5BloquantsTest#simplePartsPicked…`, le bug de base corrigé par fable5 (déjà dans l'intégration).

## Ce qui n'est pas vert
- La CI complète de cette tête est à relancer (elle part au push).

## Demandes (hors de mes fichiers)
Aucune.
