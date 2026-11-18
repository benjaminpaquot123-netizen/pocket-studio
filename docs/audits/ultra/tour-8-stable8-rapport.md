# Lot « stable8 » (tour 8) : `c_` rouge sous charge (« red itself, by name: null »)

Base : 91ae5964 · branche `eq/tour-8-stable8` · commits : rouge d7edc082, vert 855cb50a.
Fiche : `docs/audits/ultra/tour-8-fiches/fiche-S-stable8.md`. Un seul fichier de code touché : `app/src/test/java/com/pocketstudio/app/human/OrdinaryTestsLeaveNothingTest.kt`. Rien dans `app/src/main`, aucune phrase de l'appli, aucune capture.

tests: :app:testDebugUnitTest --tests 'com.pocketstudio.app.human.*'
chemin: banc, test ordinaire qui laisse du travail | test: OrdinaryTestsLeaveNothingTest#c_anOrdinaryTestThatLeavesTheAppsWorkRunningIsRedItself
chemin: banc, test ordinaire qui laisse du travail démarré en retard (machine chargée) | test: OrdinaryTestsLeaveNothingTest#c_anOrdinaryTestThatLeavesTheAppsWorkStartedLateIsRedItself
corrige: stable8-c-sous-charge | gravité: moyen

## Cause (mesurée)
`c_` lançait le travail laissé (`GlobalScope.launch(Dispatchers.Default)`) puis appelait tout de suite `afterTest`.
`afterTest` regarde les fils `DefaultDispatcher-worker` qui ont du code de l'appli dans leur pile. Il ne voit pas un travail mis en file mais pas encore démarré.
Sur une machine chargée, le fil démarre en retard. `afterTest` ne voit donc rien, ne lève rien, et `c_` échoue avec « red itself, by name: null ».

Preuve sans charge : le même test, avec un démarrage du fil retenu 300 ms (`lateDefault(300)`, la machine chargée sans la charge).
- Rouge à d7edc082 : `c_anOrdinaryTestThatLeavesTheAppsWorkStartedLateIsRedItself` échoue avec le même message, « red itself, by name: null ». 6 tests, 1 échec.
- Vert à 855cb50a : le test attend que le travail laissé tourne vraiment (verrou `CountDownLatch`, 60 s au plus) avant de demander la vérification. 6 tests, 0 échec.

L'assertion « red itself, by name » n'a pas changé. `a_`, `b_`, `d_` et `e_` n'ont pas changé. Pas de quarantaine, pas de `@Ignore`.

## 20 passages sous charge
Commande, sur 855cb50a :
`PS_GRADLE_FILE=1 ./gradlew :app:testDebugUnitTest --tests 'com.pocketstudio.app.human.*' --rerun`, 20 fois de suite.

À côté, en même temps : `scripts/bench/rejeu-defauts.sh t6-da15-ecoute-passee` en boucle, dans une copie jetable du dépôt (un rejeu Gradle complet, avec son propre verrou).

Résultat : en cours (2 sur 2 verts au moment de ce commit).

Pourquoi `./gradlew` direct et pas `gradle-file.sh` :
- la file `gradle-file.sh` arrête les démons Gradle des autres (`stop_daemons`) ;
- le selftest refuse plus d'un démon.
Deux Gradle en même temps sont donc impossibles par la file. La copie jetable neutralise ces deux gardes pour la charge seulement. Rien de cela n'est commis (voir `docs/decisions/tour-8-stable8.md`).

Note : le passage 1 a tourné en partie sans charge (le premier lancement de la charge a buté sur la garde des démons, puis a été relancé). Les passages suivants ont tous tourné avec la charge.

## Mis de côté (« pas pris », hors de la fiche)
`BenchPristine.appWorkEnded` (fichier hors liste) ne voit pas un travail mis en file sur `Dispatchers.Default` qui n'a pas encore démarré. Un vrai test ordinaire qui laisse du travail peut donc passer inaperçu sur une machine chargée. Signalé au chef, pas corrigé.
