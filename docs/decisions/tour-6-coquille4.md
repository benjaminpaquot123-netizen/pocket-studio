# Décisions du lot « coquille4 » (tour 6, 2026-11-14)

- 2026-11-14 · **Écrire sur le fil principal** — Les effets d'écran qui gardent une lecture faite hors du fil principal l'écrivent sur le fil principal (`withContext(Dispatchers.Main)`) : retours, taille des packs, chanson venue de l'accueil ; un test de garde le vérifie dans toute l'appli · raison : « Mes fichiers » restait sur « Chargement » quand l'écriture partait du fil de la lecture · retour arrière : retirer les `withContext(Dispatchers.Main)` et le test.
