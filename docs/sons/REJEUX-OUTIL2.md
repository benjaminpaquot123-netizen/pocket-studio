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
imprimées, un rejeu dépasse son plafond de vides (`VIDE_MAX` : b4 92, b6 21, b8 98 ; un outil qui
cesserait d'estimer ne passe donc pas en vert), une faute de la liste ne se montre plus (réparée : la retirer de
`EXPECTED_RED`), ou le dépôt a changé.

## Rouges attendus (`EXPECTED_RED`)

Aucun depuis le raccord à l'outil réparé (passe 2, 2026-11-26). Ce que chaque ancien rouge est devenu :

| Rejeu | Contrôle | Qui avait tort | Raison |
|---|---|---|---|
| b1 | `FX/Reverse Crash 01.wav role` | outil | un crash inversé rangé en `fx` prend le rôle `fx` (rouge/vert dans `selftest_tout_repare.py`) ; le rejeu attend `role=fx` |
| b1 | `Drums/808s/808 Long 140bpm C.wav end_cut` | rejeu | réparation 18 : sans verdict sûr, `end_cut` reste vide ; vide vaut mieux que deviné |
| b1 | `Trap Kit` stem de batterie, `key_tag` | rejeu | réparation 7 : une piste de batterie ne porte pas de tonalité |
| b2 | `Synth/Bass 120 C.wav` | rejeu | coup seul avec tempo : `oneshot_melodic`, note mesurée A3, ni tonalité ni `note_tag` (une lettre seule peut être une lettre de variante) |
| b2 | `Drum Loops/Kick Loop 125 Gm.wav key_tag` | rejeu | une tonalité complète écrite dans le nom est gardée ; le défaut 8 visait la lettre seule de variante |
| b5 | silence, RF64, RIFX | rejeu | textes d'erreur nouveaux : « digital silence, no sound », « RF64/RIFX/BW64 non pris en charge » ; le cas « 11.5 MB » pèse en vrai 0,96 Mo, renommé |
| b6 | `kit2 kit bars`, `kit2 kit_stem 1/2 bars` | outil (réparé avant) | tempo écrit 100 contre 80 entendu : la case reste vide au lieu de 5 mesures ; compte dans les vides |
| b6 | `root4 style not from 'Souls'` | outil | « Souls » au pluriel ne nomme pas la soul (rouge/vert dans `selftest_tout_repare.py`) |
| b6 | `acc2 0 key_est`, `acc2 accuracy key 12/12` | rejeu | porte de Wilson à 95 % (audit 4, A7) : 12/12 ne suffit pas, la tonalité estimée d'une boucle sans tag reste vide ; le rejeu vérifie 60/60 ouvert, 59/60 et 12/12 fermés |

Vides voulus de b6 (21) : 4 pads sans tag de acc1 (tempo et mesures barrés), 1 tonalité estimée de la
boucle sans tag de acc2, 13 mesures de acc2 (aucun tempo écrit : « 106 » est un modèle de synthé),
3 mesures de kit2 (tempo écrit et entendu en désaccord).

Non reproduit : « un kit MIDI reçoit une tonalité devinée ». Essayé sur un kit MIDI avec tonalité
écrite, un dossier `MIDI Kits` sans nom, et une batterie MIDI : `key_est` reste toujours vide,
seule la tonalité écrite (`key_tag`) est reprise. Pas de contrôle rouge pour ce point.

## Relecture neuve

Une faute grave trouvée et corrigée : `may_be_empty` n'avait pas de plancher, un outil qui
cesserait d'estimer passait en vert en laissant tout vide (d'où `VIDE_MAX`). Mineurs corrigés :
une erreur imprimée sous un contrôle déjà rouge attendu était masquée ; le verdict n'était pas
recompté ; un rejeu seul ne regardait pas `git status` ; acc1 et kit2 ne comptaient pas leurs lignes.
Restes mineurs, non corrigés : b3 et b3b ont des cas `want=None` qui ne vérifient rien ; b1 attend
désormais le rôle `fx` pour le crash inversé.
