# Décisions du lot « fuzz » (tour 5) : CI 43229551237 rouge sur GestureFuzzTest

- 2026-11-12 · Mixwake n'est pas la cause : ses boucles de 4 lectures par seconde laissent Compose au repos (sonde isolée), et le même blocage était noté le 10 novembre, avant mixwake. Mixwake reste tel quel. Retour arrière : sans objet.
- 2026-11-12 · La cause est dans le banc : Retour partait vers la fenêtre du dessus (une bulle) au lieu du dialogue à champ. Corrigé dans `GestureFuzzTest.closeTextWindows()` et `BenchWindows.textFieldDialog()`, pas dans l'appli (sur un téléphone, ce dialogue ne gèle rien). Retour arrière : revert de b934b4c2.
- 2026-11-12 · Preuve par une sonde biaisée non commitée (elle tourne 10 à 20 min et fuit de la mémoire en boucle) ; le test commité est le cas déterministe « dialogue à champ sous une bulle ». Retour arrière : sans objet.
