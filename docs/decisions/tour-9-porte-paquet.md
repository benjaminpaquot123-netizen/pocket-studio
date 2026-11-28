# Décisions du lot « porte-paquet » (tour 9)

- 2026-11-24 : le paquet passe par des étapes du portail (`PS_PAQUET_ETAPE` = controle, fusion, traces) au lieu d'un second portail. Raison : une seule copie des contrôles et des traces, que le juge lit déjà. Retour arrière : supprimer `porte-paquet.sh` et les blocs `etape` de `fusion-equipes.sh`.
- 2026-11-24 : ces étapes ne valent qu'à partir de `from_paquet 10`. Raison : le chef seul fait avancer `portail-paquet.txt` ; les paquets en cours ne changent pas. Retour arrière : retirer la ligne `from_paquet 10` (le mode paquet est alors refusé partout).
- 2026-11-24 : la limite de temps se lit dans `--limite`, `PS_PAQUET_LIMITE_MIN` et `BASH_DEFAULT_TIMEOUT_MS` / `BASH_MAX_TIMEOUT_MS` ; une limite absente n'est pas un refus. Raison : `PS_STALL_MIN` mesure un blocage, pas une durée totale. Retour arrière : retirer la boucle des limites.
- 2026-11-24 : `verifie-chantier.py` n'est pas modifié. Raison : chaque zone garde son dossier de traces (« fusion: <sha> » et ses trois fichiers) ; le test avec le vrai juge ne trouve aucun manque. Retour arrière : sans objet.
- 2026-11-24 : un rouge après le pré-contrôle revient au commit de départ puis rejoue le portail zone par zone. Raison : nommer la zone fautive sans deviner. Retour arrière : retirer la fonction `fallback`.
