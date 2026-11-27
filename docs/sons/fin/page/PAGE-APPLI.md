# Sons de l'appli : ce qu'elle joue aujourd'hui

But : écouter le son que l'appli joue aujourd'hui pour chaque ligne de
`docs/sons/fin/cibles-autres-definitives.tsv`, à côté des sons achetés.
Analyse seulement : rien ne change dans l'appli. La page est privée ; son adresse
n'est pas écrite ici.

Rejouer : `./scripts/sons/fin/page/rendu_appli.sh` (live-bench puis ps_live,
sans changer le volume ni normaliser). Chiffres : `appli-autres.csv`.
Écart = valeur de l'appli moins la médiane de la cible ; « dans la cible » si la
valeur est entre p25 et p75.

## Bilan
38 lignes : 29 rendues (dont pluck_tous sans cible définitive), 9 « l'appli n'a pas ce son » ; 0 dans la cible, 28 hors cible.

## Dans la cible
Aucune.

## Hors cible (durée ; rms ; crête)
- basse_tous : rms trop faible de 8.4 dB ; crête trop faible de 2.7 dB
- basse_dnb : crête trop faible de 1.1 dB
- basse_dubstep : rms -3.1 dB ; crête -1.1 dB
- basse_house : crête -1.8 dB
- basse_techno : trop long de 0.47 s ; rms -2.3 dB ; crête -2.7 dB
- basse_trap : rms -1.8 dB ; crête -1.2 dB
- pad_tous : trop court de 11.78 s ; crête -2.7 dB
- chords_tous : trop court de 4.12 s ; crête -0.9 dB
- keys_lofi : trop long de 3.99 s ; rms -14.8 dB ; crête -14.9 dB
- lead_tous : rms -3.9 dB ; crête -7.4 dB
- lead_dubstep : trop long de 1.16 s ; rms -4.6 dB ; crête -7.0 dB
- synth_tous : trop long de 1.49 s ; rms -3.8 dB ; crête -7.4 dB
- synth_house : crête -1.2 dB
- synth_techno : crête -7.4 dB
- bouclemelodique_tous / _dubstep / _techno : trop long d'environ 6.9 s ; crête -0.2 à -1.1 dB (dubstep : rms -5.2 dB)
- bouclemelodique_dnb : trop long de 0.005 s ; crête -1.1 dB
- bouclebatterie_tous : trop long de 7.69 s ; rms trop fort de 6.7 dB ; crête -1.1 dB
- bouclebatterie_dnb / _dubstep / _house : trop long de 5.5 à 7.7 s ; crête -0.9 à -1.1 dB
- bouclebatterie_techno : crête -1.1 dB
- voix_dnb : crête -4.8 dB
- effet_tous : trop long de 9.01 s ; rms -3.5 dB ; crête -1.1 dB
- effet_dnb : trop long de 1.87 s ; rms -5.7 dB ; crête -1.1 dB
- effet_dubstep : trop long de 12.32 s ; rms -7.9 dB ; crête -1.1 dB
- effet_techno : trop long de 10.29 s ; crête -0.6 dB

Les trois plus gros écarts : keys_lofi (-14.9 dB), effet_dubstep (+12.3 s), pad_tous (-11.8 s).

## L'appli n'a pas ce son
coupdivers_tous, arp_tous, strings_tous, bell_tous, guitar_tous, brass_tous,
keys_tous, voix_tous, effet_rave.

## Limites
- Chaîne du live complète (drive, limiteur, master) : les crêtes plafonnent vers -1.2 dB.
- Coup seul : le morceau continue autour, la note est posée là où la piste s'entend
  (repérée par une sonde, car les leads techno et house ne s'entendent que tard).
- La montée et la voix sont prises dans le morceau ; la 808 garde sa longueur naturelle.
- Le Pluck techno sert pour pad, pluck et accords : il s'éteint avant la fin des longues tenues.
- Boucles de 8 mesures (dnb 174, dubstep 140, house 125, techno et autres au tempo du style),
  les cibles en font environ 4.
- Les touches lofi gardent le souffle de leur piste en dessous.
- Aucun pack n'est livré avec l'appli : seuls les sons intégrés sont comparés.
