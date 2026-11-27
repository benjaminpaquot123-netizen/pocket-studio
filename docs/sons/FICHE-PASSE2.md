# Fiche type : une discussion de passe profonde (un RAR)

Rôle : banc. Équipe muette : aucun texte dans la discussion (fin de tour « ok »), un seul message au chef, puis archivage.
Remplacer `N` par le numéro du RAR et `LIEN` par le lien Google Drive donné par le chef (le lien ne s'écrit nulle part dans le dépôt).

## Étapes
Toutes les commandes se lancent depuis la racine du dépôt (`cd` dans le dossier cloné).

1. Disque : `df -h /tmp` ; il faut plus de 5 Go libres (archive + extraction). Sinon « bloqué : disque ».
2. Outil : `apt-get update && apt-get install -y unrar` (unrar de RARLAB ; jamais unar, qui donne de fausses erreurs).
3. Banc : `python3 -I scripts/sons/passe2/test_lance.py` doit finir par « 0 failure(s) ».
4. Dossier de sortie hors dépôt : `OUT=/tmp/passe2-out-rarN; mkdir -p "$OUT"`.
5. Plan d'abord : `scripts/sons/passe2/lance.sh --rar N --url 'LIEN' --out "$OUT" --plan`.
   Le plan télécharge et extrait aussi (puis efface tout) : il affiche « rar N: P pack(s) » puis une ligne par pack (`pN`, nom du dossier, nombre de fichiers, style). Une archive d'un seul pack donne 1 pack, au nom du dossier racine.
   Comparer chaque pack et son style à `docs/sons/STYLES-PACKS.md`. Un style faux non couvert par la table : relancer avec `--force-style 'REGEX=style'`. Un pack manquant ou un dossier pris à tort pour un pack : `--pack 'chemin/relatif'` (répétable, chemin depuis la racine de l'archive). Refus « sounds at the top of the archive » (sons posés à la racine, sans dossier) : « bloqué : sons à la racine du RAR N ».
6. Passe complète : la même commande sans `--plan` (avec les mêmes `--force-style` / `--pack`).
   Le lanceur télécharge, teste (`unrar t`), extrait, lance `tout.py` sur chaque dossier de pack, fusionne les CSV et efface téléchargement et extraction.
   Pour chaque pack, il recopie la ligne de `tout.py` « pN: style du pack : STYLE, posé sur X lignes » (ou « style du pack : aucun ») : la garder pour le rendu.

## Interdits
- Dépôt PUBLIC : aucun son, aucune archive, aucun audio, aucun lien Drive, aucun nom de fichier de son dans un commit.
- `--out` et le dossier de travail toujours hors dépôt (le lanceur refuse sinon).
- Ne pas toucher `scripts/sons/tout.py`, les selftests, `scripts/sons/audit2/`, `scripts/sons/pack/`.
- `python3 -I` pour tout script qui lit les données ; aucun lien symbolique ; `git add` par chemin, jamais `-A` ; pas de PR ; aucun identifiant de modèle dans les commits.

## Contrôle de fin
Lire `$OUT/rarN_controle.csv` (une ligne par pack) :
- `count_ok` = `yes` partout : lignes (hors lignes de kit) = fichiers audio + midi lus ;
- `errors` = 0 partout, sinon les erreurs sont listées dans `$OUT/rarN_erreurs.txt` ;
- `pack_line_ok` = `yes` partout (la ligne « style du pack » de `tout.py` est là et compte les mêmes lignes que `rows_pack_styled`) et `unresolved` = 0 ;
- code de sortie du lanceur : 0 bon, 3 erreurs listées, 4 écart de compte, ligne « style du pack » absente ou fausse, ligne sans fichier retrouvé (`unresolved`) ou audio apparu dans le dépôt, 2 refus (dont unrar absent).
Vérifier aussi `git status --short` : rien de neuf hors des CSV voulus.

## Rendu
Copier les CSV (`rarN_*.csv`, sans `rarN_packs.txt`, qui nomme les dossiers) dans `docs/sons/passe2/rarN/` (`mkdir -p docs/sons/passe2/rarN`), `git add` de chaque CSV par son chemin, commit sur la branche donnée par le chef, push.
Message au chef (send_message), puis s'archiver :
```
fini <sha>
RAR N : P packs, F fichiers lus, L lignes, K lignes de kit
erreurs : 0 (ou E, listées dans rarN_erreurs.txt)
styles forcés : <pN → style_forced, rows_forced du contrôle> ou aucun
style du pack : <une ligne « pN: style du pack : … » par pack>
écarts au tableau STYLES-PACKS : <liste> ou aucun
archives imbriquées / fichiers sautés : <résumé>
```
Si un contrôle échoue : « bloqué : raison » au lieu de « fini ».
