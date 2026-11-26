# Batterie du matin : avant / après

Pour chaque famille de batterie et chaque style, une nouvelle version synthétisée, réglée sur des **mesures** de coups de référence (les chiffres sont dans `CIBLES-BATTERIE.md`). Aucun son acheté n'est copié ni rangé dans le dépôt : seulement des chiffres. C'est **à écouter seulement** : l'appli ne joue pas encore ces nouvelles versions, rien n'a changé dans ses sons ni dans son interface.

## Écouter

```
scripts/son/matin_batterie.sh /tmp/matin
```

Le dossier doit être hors du dépôt. Le script fabrique tout seul ce qu'il lui faut (il faut g++ et ffmpeg), dure une trentaine de secondes et écrit **60 MP3** :

- `famille_style.mp3` (54 fichiers) : le son actuel de l'appli 4 fois, 1 seconde de silence, puis la nouvelle version 4 fois. Les deux moitiés ont le même volume ressenti (même loudness), pour juger le son et pas le niveau.
- `boucle_style.mp3` (6 fichiers) : 8 mesures du style avec la batterie actuelle, 1 seconde, les mêmes 8 mesures avec les nouvelles versions.

À la fin, le script vérifie chaque fichier (longueur, pas de silence, pas de saturation) et dit combien il en a écrit.

## Ce qui a changé, fichier par fichier

« Clarté » : le centre de gravité du spectre, plus c'est haut, plus c'est brillant. Les chiffres « après » viennent du test `matin_measured_drums_in_range` (rendu hors-ligne, un coup seul).

| Fichier | Famille | Style | Ce qui a été réglé | Mesure après (cible : médiane, fourchette) |
|---|---|---|---|---|
| `kick_house.mp3` | Kick | house | la note finale, la chute de note (départ et vitesse), la longueur | longueur −40 dB 263 ms (cible 268, 223–326) ; note 41.7 Hz (cible 42.5) ; clarté 68 Hz (cible 94, 63–122) |
| `kick_techhouse.mp3` | Kick | techhouse | la note finale, la chute de note (départ et vitesse), la longueur | longueur −40 dB 310 ms (cible 313, 223–396) ; note 49.7 Hz (cible 47.5) ; clarté 81 Hz (cible 114, 86–168) |
| `kick_techno.mp3` | Kick | techno | la note finale, la chute de note (départ et vitesse), la longueur | longueur −40 dB 314 ms (cible 394, 240–398) ; note 49.5 Hz (cible 50.6) ; clarté 94 Hz (cible 140, 86–182) |
| `kick_trap.mp3` | Kick | trap | la note finale, la chute de note (départ et vitesse), la longueur | longueur −40 dB 121 ms (cible 129, 96–156) ; note 44.5 Hz (cible 46.5) ; clarté 82 Hz (cible 67, 56–80) |
| `kick_dnb.mp3` | Kick | dnb | la note finale, la chute de note (départ et vitesse), la longueur | longueur −40 dB 173 ms (cible 178, 153–261) ; note 50.2 Hz (cible 51.9) ; clarté 111 Hz (cible 100, 85–218) |
| `kick_dubstep.mp3` | Kick | dubstep | la note finale, la chute de note (départ et vitesse), la longueur | longueur −40 dB 275 ms (cible 284, 233–347) ; note 44.2 Hz (cible 47.7) ; clarté 81 Hz (cible 104, 64–132) |
| `808_house.mp3` | 808 | house | la note de la tonique, une chute courte au départ, une queue longue | longueur −40 dB 1793 ms (cible 1766, 893–2781) ; note 55.3 Hz (cible 41.3) ; clarté 78 Hz (cible 46, 30–73) ; cible de la famille entière (peu de coups dans ce style) |
| `808_techhouse.mp3` | 808 | techhouse | la note de la tonique, une chute courte au départ, une queue longue | longueur −40 dB 1796 ms (cible 1766, 893–2781) ; note 49.2 Hz (cible 41.3) ; clarté 70 Hz (cible 46, 30–73) ; cible de la famille entière (peu de coups dans ce style) |
| `808_techno.mp3` | 808 | techno | la note de la tonique, une chute courte au départ, une queue longue | longueur −40 dB 1797 ms (cible 1766, 893–2781) ; note 43.8 Hz (cible 41.3) ; clarté 62 Hz (cible 46, 30–73) ; cible de la famille entière (peu de coups dans ce style) |
| `808_trap.mp3` | 808 | trap | la note de la tonique, une chute courte au départ, une queue longue | longueur −40 dB 1797 ms (cible 1766, 893–2781) ; note 43.8 Hz (cible 41.3) ; clarté 62 Hz (cible 46, 30–73) ; cible de la famille entière (peu de coups dans ce style) |
| `808_dnb.mp3` | 808 | dnb | la note de la tonique, une chute courte au départ, une queue longue | longueur −40 dB 1797 ms (cible 1766, 893–2781) ; note 43.8 Hz (cible 41.3) ; clarté 62 Hz (cible 46, 30–73) ; cible de la famille entière (peu de coups dans ce style) |
| `808_dubstep.mp3` | 808 | dubstep | la note de la tonique, une chute courte au départ, une queue longue | longueur −40 dB 1794 ms (cible 1766, 893–2781) ; note 41.3 Hz (cible 41.3) ; clarté 59 Hz (cible 46, 30–73) ; cible de la famille entière (peu de coups dans ce style) |
| `clap_house.mp3` | Clap | house | trois mains de 9 ms puis une queue, le filtre qui règle la clarté | longueur −40 dB 156 ms (cible 142, 84–214) ; clarté 3897 Hz (cible 2690, 1826–4092) |
| `clap_techhouse.mp3` | Clap | techhouse | trois mains de 9 ms puis une queue, le filtre qui règle la clarté | longueur −40 dB 197 ms (cible 168, 114–228) ; clarté 4535 Hz (cible 3392, 2292–4726) |
| `clap_techno.mp3` | Clap | techno | trois mains de 9 ms puis une queue, le filtre qui règle la clarté | longueur −40 dB 220 ms (cible 175, 142–240) ; clarté 4948 Hz (cible 3738, 2410–5452) |
| `clap_trap.mp3` | Clap | trap | trois mains de 9 ms puis une queue, le filtre qui règle la clarté | longueur −40 dB 173 ms (cible 136, 81–221) ; clarté 3962 Hz (cible 2764, 2562–3383) |
| `clap_dnb.mp3` | Clap | dnb | trois mains de 9 ms puis une queue, le filtre qui règle la clarté | longueur −40 dB 141 ms (cible 129, 82–183) ; clarté 4567 Hz (cible 3266, 2714–4205) |
| `clap_dubstep.mp3` | Clap | dubstep | trois mains de 9 ms puis une queue, le filtre qui règle la clarté | longueur −40 dB 338 ms (cible 266, 151–463) ; clarté 3986 Hz (cible 2581, 1985–3844) |
| `caisse_claire_house.mp3` | Caisse claire | house | le corps (une note qui chute) et le bruit, sa longueur et sa clarté | longueur −40 dB 112 ms (cible 114, 80–142) ; clarté 823 Hz (cible 1180, 667–1605) |
| `caisse_claire_techhouse.mp3` | Caisse claire | techhouse | le corps (une note qui chute) et le bruit, sa longueur et sa clarté | longueur −40 dB 131 ms (cible 135, 94–174) ; clarté 802 Hz (cible 798, 634–1391) |
| `caisse_claire_techno.mp3` | Caisse claire | techno | le corps (une note qui chute) et le bruit, sa longueur et sa clarté | longueur −40 dB 131 ms (cible 135, 94–174) ; clarté 802 Hz (cible 798, 634–1391) |
| `caisse_claire_trap.mp3` | Caisse claire | trap | le corps (une note qui chute) et le bruit, sa longueur et sa clarté | longueur −40 dB 191 ms (cible 196, 127–243) ; clarté 2669 Hz (cible 2536, 1592–3237) |
| `caisse_claire_dnb.mp3` | Caisse claire | dnb | le corps (une note qui chute) et le bruit, sa longueur et sa clarté | longueur −40 dB 96 ms (cible 100, 76–124) ; clarté 4344 Hz (cible 3465, 2754–4240) |
| `caisse_claire_dubstep.mp3` | Caisse claire | dubstep | le corps (une note qui chute) et le bruit, sa longueur et sa clarté | longueur −40 dB 504 ms (cible 546, 294–709) ; clarté 2601 Hz (cible 1159, 614–2929) |
| `charley_ferme_house.mp3` | Charley fermé | house | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 82 ms (cible 85, 47–114) ; clarté 11074 Hz (cible 7745, 6611–9909) |
| `charley_ferme_techhouse.mp3` | Charley fermé | techhouse | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 88 ms (cible 92, 59–129) ; clarté 11321 Hz (cible 7899, 6853–10194) |
| `charley_ferme_techno.mp3` | Charley fermé | techno | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 90 ms (cible 96, 87–147) ; clarté 11333 Hz (cible 7963, 6856–10257) |
| `charley_ferme_trap.mp3` | Charley fermé | trap | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 67 ms (cible 71, 61–101) ; clarté 10610 Hz (cible 7825, 4784–11948) |
| `charley_ferme_dnb.mp3` | Charley fermé | dnb | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 74 ms (cible 77, 44–176) ; clarté 11689 Hz (cible 10307, 6139–12163) |
| `charley_ferme_dubstep.mp3` | Charley fermé | dubstep | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 78 ms (cible 82, 60–98) ; clarté 12040 Hz (cible 9334, 7497–11034) |
| `charley_ouvert_house.mp3` | Charley ouvert | house | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 150 ms (cible 154, 120–179) ; clarté 7925 Hz (cible 5996, 5197–7036) |
| `charley_ouvert_techhouse.mp3` | Charley ouvert | techhouse | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 206 ms (cible 210, 107–1006) ; clarté 11233 Hz (cible 7924, 6023–11659) ; cible de la famille entière (peu de coups dans ce style) |
| `charley_ouvert_techno.mp3` | Charley ouvert | techno | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 206 ms (cible 210, 107–1006) ; clarté 11233 Hz (cible 7924, 6023–11659) ; cible de la famille entière (peu de coups dans ce style) |
| `charley_ouvert_trap.mp3` | Charley ouvert | trap | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 942 ms (cible 951, 623–1937) ; clarté 13308 Hz (cible 10514, 8420–15545) |
| `charley_ouvert_dnb.mp3` | Charley ouvert | dnb | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 206 ms (cible 210, 107–1006) ; clarté 11233 Hz (cible 7924, 6023–11659) ; cible de la famille entière (peu de coups dans ce style) |
| `charley_ouvert_dubstep.mp3` | Charley ouvert | dubstep | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 134 ms (cible 138, 109–226) ; clarté 11383 Hz (cible 9367, 6574–11297) |
| `rim_house.mp3` | Rim | house | un coup court et sec, note aiguë qui chute vite | longueur −40 dB 109 ms (cible 163, 79–273) ; clarté 2258 Hz (cible 1479, 953–2443) ; cible de la famille entière (peu de coups dans ce style) |
| `rim_techhouse.mp3` | Rim | techhouse | un coup court et sec, note aiguë qui chute vite | longueur −40 dB 109 ms (cible 163, 79–273) ; clarté 2258 Hz (cible 1479, 953–2443) ; cible de la famille entière (peu de coups dans ce style) |
| `rim_techno.mp3` | Rim | techno | un coup court et sec, note aiguë qui chute vite | longueur −40 dB 109 ms (cible 163, 79–273) ; clarté 2258 Hz (cible 1479, 953–2443) ; cible de la famille entière (peu de coups dans ce style) |
| `rim_trap.mp3` | Rim | trap | un coup court et sec, note aiguë qui chute vite | longueur −40 dB 109 ms (cible 163, 79–273) ; clarté 2258 Hz (cible 1479, 953–2443) ; cible de la famille entière (peu de coups dans ce style) |
| `rim_dnb.mp3` | Rim | dnb | un coup court et sec, note aiguë qui chute vite | longueur −40 dB 109 ms (cible 163, 79–273) ; clarté 2258 Hz (cible 1479, 953–2443) ; cible de la famille entière (peu de coups dans ce style) |
| `rim_dubstep.mp3` | Rim | dubstep | un coup court et sec, note aiguë qui chute vite | longueur −40 dB 109 ms (cible 163, 79–273) ; clarté 2258 Hz (cible 1479, 953–2443) ; cible de la famille entière (peu de coups dans ce style) |
| `tom_house.mp3` | Tom | house | la note, sa chute et la longueur | longueur −40 dB 225 ms (cible 229, 153–330) ; note 117.1 Hz (cible 116.8) ; clarté 158 Hz (cible 148, 106–599) |
| `tom_techhouse.mp3` | Tom | techhouse | la note, sa chute et la longueur | longueur −40 dB 506 ms (cible 514, 216–1031) ; note 95.3 Hz (cible 94.8) ; clarté 131 Hz (cible 566, 119–1472) ; cible de la famille entière (peu de coups dans ce style) |
| `tom_techno.mp3` | Tom | techno | la note, sa chute et la longueur | longueur −40 dB 506 ms (cible 514, 216–1031) ; note 95.3 Hz (cible 94.8) ; clarté 131 Hz (cible 566, 119–1472) ; cible de la famille entière (peu de coups dans ce style) |
| `tom_trap.mp3` | Tom | trap | la note, sa chute et la longueur | longueur −40 dB 506 ms (cible 514, 216–1031) ; note 95.3 Hz (cible 94.8) ; clarté 131 Hz (cible 566, 119–1472) ; cible de la famille entière (peu de coups dans ce style) |
| `tom_dnb.mp3` | Tom | dnb | la note, sa chute et la longueur | longueur −40 dB 506 ms (cible 514, 216–1031) ; note 95.3 Hz (cible 94.8) ; clarté 131 Hz (cible 566, 119–1472) ; cible de la famille entière (peu de coups dans ce style) |
| `tom_dubstep.mp3` | Tom | dubstep | la note, sa chute et la longueur | longueur −40 dB 506 ms (cible 514, 216–1031) ; note 95.3 Hz (cible 94.8) ; clarté 131 Hz (cible 566, 119–1472) ; cible de la famille entière (peu de coups dans ce style) |
| `crash_house.mp3` | Crash | house | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 1669 ms (cible 1693, 836–2630) ; clarté 8308 Hz (cible 5946, 4051–7577) ; cible de la famille entière (peu de coups dans ce style) |
| `crash_techhouse.mp3` | Crash | techhouse | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 1669 ms (cible 1693, 836–2630) ; clarté 8308 Hz (cible 5946, 4051–7577) ; cible de la famille entière (peu de coups dans ce style) |
| `crash_techno.mp3` | Crash | techno | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 1669 ms (cible 1693, 836–2630) ; clarté 8308 Hz (cible 5946, 4051–7577) ; cible de la famille entière (peu de coups dans ce style) |
| `crash_trap.mp3` | Crash | trap | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 1669 ms (cible 1693, 836–2630) ; clarté 8308 Hz (cible 5946, 4051–7577) ; cible de la famille entière (peu de coups dans ce style) |
| `crash_dnb.mp3` | Crash | dnb | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 1074 ms (cible 1075, 710–1424) ; clarté 11302 Hz (cible 8366, 7436–11312) |
| `crash_dubstep.mp3` | Crash | dubstep | la longueur et la clarté (bruit + métal filtrés) | longueur −40 dB 1669 ms (cible 1693, 1342–2320) ; clarté 7580 Hz (cible 4946, 3965–6094) |
| `boucle_house.mp3` | Boucle | house | kick à chaque temps, clap sur 2 et 4, charleys | toutes les familles house ci-dessus ensemble |
| `boucle_techhouse.mp3` | Boucle | techhouse | idem house, charleys fermés en croches | toutes les familles techhouse |
| `boucle_techno.mp3` | Boucle | techno | idem techhouse, un rim toutes les deux mesures | toutes les familles techno |
| `boucle_trap.mp3` | Boucle | trap | kick + 808, clap + caisse claire sur 3, charleys en croches | toutes les familles trap |
| `boucle_dnb.mp3` | Boucle | dnb | kick sur 1 et le « et » de 3, caisse claire sur 2 et 4, toms | toutes les familles dnb |
| `boucle_dubstep.mp3` | Boucle | dubstep | demi-temps : kick + 808 sur 1, caisse claire + clap sur 3 | toutes les familles dubstep |

## Comment le test décide

Pour chaque famille et style, le test joue un coup de la nouvelle version et vérifie qu'il tombe dans la fourchette des mesures (du 10e au 90e centile), avec une marge : 10 % sur la longueur, 8 % sur la note, 25 % sur le départ de la chute et sur la clarté. Il vérifie aussi : attaque en moins de 5 ms, ni NaN ni saturation, aucune allocation de mémoire pendant le rendu, et qu'un coup qui en relance un autre ne casse rien.

**Resserrage (point 3, 26 novembre)** : les voix qui ne passaient que grâce à la marge (charleys fermés et ouverts, crash, claps, kick techhouse, caisse claire dubstep) passent maintenant un test plus dur, sans marge : la longueur à −20 dB, la longueur à −60 dB et la clarté doivent tomber entre le 25e et le 75e centile des coups de référence du style (pas « médiane ± 10 % »). Pour ces voix, l'ancien test ne garde que ses contrôles de sûreté (attaque, saturation, NaN, allocation). Les charleys sont classés fermés ou ouverts d'après leur longueur mesurée, plus d'après leur nom. Les chiffres du tableau plus haut, pour ces voix-là, sont ceux de la première version.

**Ce qui sert de cible, et ce qui n'en sert pas** (après une relecture à l'aveugle des mesures) : la longueur, la crête et le volume sont fiables ; la note n'est vérifiée que pour le kick (≈ 48 Hz, à un demi-ton près), le 808 et les toms. La note des claps, charleys, crash et caisses claires n'est pas une cible. La clarté sert d'indication, pas de vérité. **Le 808** : 21 des 23 coups mesurés viennent d'un seul pack, donc ses chiffres disent « ce pack-là », pas « le 808 » en général.

## Ce qui n'est pas fait (à dire franchement)

- **Les boucles ne passent pas par `ps_live`** (le moteur temps réel de l'appli) : elles sont jouées par un petit séquenceur du script, sans la chaîne de mixage de l'appli ni le mixage par piste. Les niveaux des familles dans la boucle « après » sont posés à la main. Les coups seuls « avant », eux, sortent de la vraie batterie de l'appli (kit v3, mêmes réglages de kick et de 808 que les styles), sans sa table de mixage.
- **Le 808 « avant »** de house, techhouse, techno, dnb et dubstep est le 808 de la trap posé sur la tonique du style : l'appli n'a un 808 que pour la trap.
- **Les voix qui ne passaient que grâce à la marge** ont été resserrées (voir plus haut) ; la caisse claire dnb et le kick trap, à 3 % de leur fourchette large, n'ont pas été retouchés.
- **Quand un style avait trop peu de coups mesurés** (808, rim, la plupart des toms, crash dans quatre styles, charley ouvert dans trois), la cible est celle de la famille entière : la nouvelle version est alors la même dans ces styles (seule la note du 808 suit la tonique).
- **Pas encore dans l'appli** : aucun pad, style ni projet ne joue ces versions. Les y mettre sera un autre lot, après écoute de Benjamin.
- Pas de cowbell, shaker, conga ni zap dans ce lot.
