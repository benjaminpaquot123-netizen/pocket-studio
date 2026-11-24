# Décisions du lot « maven » (tour 9)

- 2026-11-24 : CI : le gros fichier Android de Robolectric (213 Mo) vient du miroir Google de Maven Central, avec trois miroirs de secours et un cache (`scripts/ci/robolectric-jar.sh`). Raison : repo1.maven.org répondait 429 (trop de demandes), toutes les classes Robolectric tombaient (robot1, fable5). Retour arrière : retirer l'étape et la propriété `robolectric.dependency.repo.url`.
- 2026-11-24 : le job « Build, unit tests, screenshots » passe de 75 à 100 minutes. Raison : la CI verte de robot1 a mis 66,5 min, audio1 et moteur2 ont été coupés à 75 min sans test rouge (machines GitHub lentes). Retour arrière : remettre `timeout-minutes: 75` dans `.github/workflows/ci.yml`.
