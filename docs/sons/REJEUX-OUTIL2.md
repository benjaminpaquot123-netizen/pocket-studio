# Rejeux b1 à b8 de l'outil 2 (`tout.py`)

Les rejeux de `AUDIT-TOUT-2.md` sont devenus de vrais tests : chacun vérifie ses valeurs attendues
et sort en erreur sur une vraie faute.

## Lancer

```
python3 -I scripts/sons/audit2/run_all.py [dossier_de_travail]
```

Une ligne par rejeu (`ok` ou `ÉCHEC`), puis le total. Packs fabriqués et tables vont dans le
dossier de travail (par défaut `AUDIT2_WORK`, sinon un dossier temporaire), jamais dans le dépôt :
`run_all.py` vérifie à la fin que `git status` est propre (chaque rejeu le vérifie aussi sous
`scripts/sons/`) et que `scripts/sons/pack/` n'existe pas.
Un rejeu se lance aussi seul : `python3 -I scripts/sons/audit2/b6_misc.py`.

## Lire un verdict

Chaque rejeu finit par `<nom>: ok N faux N vide N erreur N` (`lib.Check`) :

- **faux** : une valeur mesurée ne vaut pas l'attendu (ligne `FAUX <nom> | <contrôle> | détail`) ;
- **vide** : l'outil laisse la case vide là où il ne sait pas ; c'est permis seulement quand le
  contrôle le dit (b4 et b8 : tempo ou tonalité sans indice écrit), sinon c'est un faux ;
- **erreur** : plantage de `tout.py`, aucune ligne, ou code de sortie inattendu.

Un faux ou une erreur donne un code de sortie 1. b8 n'imprime plus « WRONG » et b4 plus
« TEMPO wrong » : ce qui était laissé vide à raison compte en « vide ».

`run_all.py` est en échec si : une faute n'est pas dans la liste ci-dessous, une erreur apparaît,
un rejeu n'a pas de verdict, le verdict ne compte pas autant de faux et d'erreurs que de lignes
imprimées, un rejeu dépasse son plafond de vides (`VIDE_MAX` : b4 92, b6 17, b8 98 ; un outil qui
cesserait d'estimer ne passe donc pas en vert), une faute de la liste ne se montre plus (réparée : la retirer de
`EXPECTED_RED`), ou le dépôt a changé.

## Rouges attendus (`EXPECTED_RED`)

Défauts connus de l'outil, attrapés par les rejeux, gardés rouges jusqu'à leur réparation :

| Rejeu | Contrôle | Défaut |
|---|---|---|
| b1 | `FX/Reverse Crash 01.wav role` | 9 : un son `fx` garde le rôle `hats` |
| b1 | `Drums/808s/808 Long 140bpm C.wav end_cut` | 18, 25 : un coup seul nommé avec un tempo |
| b2 | `Synth/Bass 120 C.wav type` | 25 : un coup seul de 2 s avec « 120 » rangé en boucle |
| b2 | `Drum Loops/Kick Loop 125 Gm.wav key_tag` | 8 : une boucle de batterie reçoit une tonalité |
| b6 | `kit2 kit bars`, `kit2 kit_stem 1/2 bars` | mesures absurdes : kit écrit « 100 bpm », son à 80, le tempo écrit est gardé et donne 5 mesures au lieu de 4 |
| b6 | `root4 style not from 'Souls'` | style trop gourmand sur le nom du pack : « Trapped Souls Vol 1 » donne `rnb` |

Non reproduit : « un kit MIDI reçoit une tonalité devinée ». Essayé sur un kit MIDI avec tonalité
écrite, un dossier `MIDI Kits` sans nom, et une batterie MIDI : `key_est` reste toujours vide,
seule la tonalité écrite (`key_tag`) est reprise. Pas de contrôle rouge pour ce point.

## Relecture neuve

Une faute grave trouvée et corrigée : `may_be_empty` n'avait pas de plancher, un outil qui
cesserait d'estimer passait en vert en laissant tout vide (d'où `VIDE_MAX`). Mineurs corrigés :
une erreur imprimée sous un contrôle déjà rouge attendu était masquée ; le verdict n'était pas
recompté ; un rejeu seul ne regardait pas `git status` ; acc1 et kit2 ne comptaient pas leurs lignes.
Restes mineurs, non corrigés : b3 et b3b ont des cas `want=None` qui ne vérifient rien ; b1 vérifie
seulement que le crash n'est pas `hats` (le rôle juste reste à trancher avec la réparation du défaut 9).
