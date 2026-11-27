# Contre-mesure des tables de sons (passe 2)

Outil indépendant de `scripts/sons/tout.py` : `mesure.py` (numpy seul) relit chaque son et
compare ses mesures aux tables `docs/sons/passe2/tout/`. Tolérances fixées avant toute mesure :
`TOLERANCES.md`. Ne pas modifier ce dossier depuis une autre discussion : s'en servir tel quel.

## 1. Vérifier l'outil d'abord

```sh
cd /chemin/du/depot
python3 -I scripts/sons/passe2/verif/selftest.py   # doit finir par « mesure.py misses: 0 »
```

## 2. Une archive (RAR N), du téléchargement à l'effacement

Le lien Drive est donné dans la fiche ; il ne s'écrit jamais dans le dépôt. On en garde
seulement l'identifiant (la partie entre `/d/` et `/view`).

```sh
REPO=/chemin/du/depot
N=1                      # numéro du RAR
ID=identifiant_drive     # depuis le lien de la fiche, jamais commité
OUT=$REPO/docs/sons/passe2/verif           # résultats (sans aucun nom de fichier)
df -BG --output=avail /tmp                 # garder plus de 5 Go libres

W=$(mktemp -d /tmp/rar$N.XXXXXX)           # dossier neuf et vide pour cette archive
mkdir "$W/x"
curl -L --fail -sS --retry 3 -o "$W/a.rar" \
  "https://drive.usercontent.google.com/download?id=$ID&export=download&confirm=t"
unrar t -idq -- "$W/a.rar"
unrar x -idq -o- -y -- "$W/a.rar" "$W/x/"
rm -f -- "${W:?}/a.rar"
find "$W/x" -type l -delete                # aucun lien symbolique suivi

cd "$REPO"                                 # jamais depuis le dossier extrait
python3 -I scripts/sons/passe2/verif/compare.py "$W/x" --rar $N \
  --tables docs/sons/passe2/tout --out "$OUT"

rm -rf -- "${W:?}"                         # effacer avant l'archive suivante
```

Sorties :
- `docs/sons/passe2/verif/rarN.csv` : clé de table, type, sous-type, style, puis pour chaque
  colonne la valeur de la table, la mienne et l'accord (`yes`, `no`, `one-empty`, `octave`,
  `nc` = hauteur non comparée car son pas assez tonal). Jamais de nom de fichier.
- `docs/sons/passe2/verif/rarN-resume.md` : accord par type et colonne (⚠ sous 98 %), fichiers
  sans ligne et lignes sans fichier par pack, lecture indépendante des étiquettes (confusions
  kick/808, snare/clap, hat fermé/ouvert d'abord), nombre de « 808 » par pack et type.

## 3. Règles

- `python3 -I` pour tout script qui lit les sons ; scripts lancés depuis le dépôt.
- Une archive à la fois, chacune dans son dossier neuf ; effacée avant la suivante.
- Rien d'extrait, aucun lien, aucun nom de son ou de pack dans un commit.
