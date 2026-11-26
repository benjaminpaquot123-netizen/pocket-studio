# Batterie du matin : avant / après dans les morceaux

## La commande

```
scripts/son/matin_morceaux.sh /tmp/morceaux
```

- Le dossier de sortie doit être **hors du dépôt** (sinon le script refuse).
- Durée : environ 3 min 30 pour les 12 styles (4 processeurs).
- Résultat : **24 MP3** (2 par style).
- Pour quelques styles seulement : `--styles house,techno`.

## Ce qu'il y a dans chaque fichier

Pour chaque style (house, tech house, techno, trap, dnb, dubstep, deep house, minimal,
hard techno, drill, liquid dnb, riddim), le morceau fait par l'assistant est joué comme
sur le téléphone (« Écouter », moteur en direct), à partir du premier Drop :

- `morceau_<style>.mp3` : le morceau entier, 40 s avec la batterie de l'appli, 1 s de
  silence, puis 40 s avec la batterie mesurée.
- `batterie_<style>.mp3` : la même chose, mais la piste batterie seule.

Mêmes commandes, même gain : rien n'est remis au même volume entre les deux moitiés.

## Le niveau de la batterie

La batterie mesurée est réglée style par style pour sonner au même volume que celle de
l'appli : on juge le son, pas le volume. Écart mesuré sur la piste batterie seule :

| Style | Écart | Style | Écart |
|---|---|---|---|
| house | +0,6 dB | deep house | −0,1 dB |
| tech house | 0,0 dB | minimal | −1,1 dB |
| techno | −1,6 dB | hard techno | +1,6 dB |
| trap | −0,3 dB | drill | 0,0 dB |
| dnb | −0,3 dB | liquid dnb | −0,1 dB |
| dubstep | −1,0 dB | riddim | +1,4 dB |

Le script refuse un écart de plus de 2 dB. Techno, minimal et hard techno partagent le
même réglage, donc on ne peut pas descendre sous 1,5 dB pour les trois à la fois.

## Le drill : pourquoi ça change peu

Dans le morceau drill, la grosse basse (le 808) et deux sons du pack (une caisse claire et
un effet) restent les mêmes : seuls le kick, les charleys et les autres sons de batterie
synthétique sont remplacés. La différence existe, mais elle est discrète.

## Ce qui n'est pas fait

- Pas de shaker, cowbell, conga ni vrai 808 par style : les mesures les mélangent tous
  sous « percussions ».
- Les variantes (kick grave ou sec, clap long ou court…) ne sont pas dans ces morceaux :
  elles s'écoutent avec `scripts/son/matin_batterie.sh` (voir `BATTERIE.md`).
- La deuxième caisse claire (pad Snare2) n'est pas remplacée quand elle vient d'un pack.
- Rien de tout ça n'est dans l'appli : c'est une commande du banc d'écoute seulement.
