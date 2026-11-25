# Relecture tour-9 cat-400
Relu : b39f1d1f (relecture neuve par un sous-agent qui n'a pas écrit le code ; points mineurs corrigés dans 11d106aa)

Périmètre : ff60701b..b39f1d1f, fichiers du banc (`app/src/benTest/.../bentest/Scenarios*.kt`, `InAppDriver.kt`) et leurs tests (`app/src/test/.../bentest/`). Rien hors du banc.

Rien de grave.
- Les sept corrections de revue font ce qu'elles disent : effets et curseurs remis en `finally`, trois doublons retirés, écoutes d'une minute réparties ; les tests vérifient toujours un vrai résultat.
- Mineur, corrigé (11d106aa) : une remise de curseur ratée était muette ; elle laisse maintenant une note au carnet.
- Mineur, laissé : `ajouterOuvert` qui échoue sur « la fiche ne le montre pas » laisse l'effet sur la piste (défaut ancien, avant le `try`).
- Mineur, accepté : `refaire` laisse une partie qui n'apparaît qu'après 5 s.

Vérifié : suite bentest (232 tests, 0 sauté) verte ; `-PbenTest :app:assembleRelease` réussit. Thread audio : non concerné.
