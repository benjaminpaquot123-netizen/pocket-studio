# Tour 9, lot ci-outils : décisions

- Défaut 1 : la ligne de verdict s'appelle « verdict » et non « ci_verdict », pour que les lecteurs des lignes « ci_ » (tableau de bord, tests du banc) gardent exactement leurs deux clés. Retour arrière : ebd448e5^.
- Défaut 1 : un run annulé ou sauté (« annulé ») ne compte ni pour le verdict ni pour ci_build / ci_emulateur : un run remplacé dit rien du commit. Un échec, un délai dépassé ou une action requise comptent rouge.
- Défaut 1 : ci_build et ci_emulateur prennent le pire de tous les runs du commit (rouge, puis en cours, puis vert) au lieu du dernier lu.
- Défaut 2 : chemin de secours par curl seulement quand gh échoue ; l'artefact le plus récent non expiré nommé « unit-test-reports » ; dézippé par Python -I avec refus de tout membre qui sort du dossier. Retour arrière : 74e0841e.
- Tests des deux scripts dans `test_stable.py` (déjà lancé par le self-test et le pré-vol) plutôt qu'un fichier neuf, pour ne pas toucher `selftest.sh`.
