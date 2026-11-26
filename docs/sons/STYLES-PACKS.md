# Styles des packs (passe profonde)

Règle de `scripts/sons/tout.py` (`style_of`) : les dossiers sont lus du plus intérieur vers l'extérieur, le nom du dossier du pack en dernier. Un nom de pack connu (`mine_or.STYLES`) gagne, sinon le mot de genre qui finit le plus tard. Le style du pack ne remplit que les lignes restées vides ou « unlabelled ».

Le lanceur `scripts/sons/passe2/lance.py` ajoute un style forcé par pack (table `FORCES`, ou `--force-style REGEX=STYLE`). Il ne s'applique qu'aux lignes dont le style vaut le style posé par le nom du pack, « unlabelled » ou vide, ET dont les dossiers intérieurs ne nomment aucun style.

**Les noms exacts des dossiers ne sont pas connus** (les CSV des passes précédentes sont anonymisés, seuls leurs en-têtes nomment les packs). Les noms ci-dessous sont les noms probables. Chaque discussion lance d'abord `--plan` et compare la colonne « style » à ce tableau ; un nom différent qui tombe faux = `--force-style` à ajouter.

| RAR | Dossier de pack (nom probable) | Style posé par tout.py | Juste ? | Style à forcer |
|---|---|---|---|---|
| 1 | Drum Hero 6 | multi | oui (les sous-dossiers portent les genres) | — |
| 2 | Downtown Drift | drift-phonk | oui | — |
| 2 | Drift Phonk (si c'est le nom) | phonk | faux | drift-phonk (table) |
| 3 | Consciousness - Melodic Techno | techno | oui | — |
| 3 | Consciousness (seul) | unlabelled | faux | techno (table) |
| 4 | Cherry Blossom - Chillhop | lofi | oui | — |
| 4 | Cherry Blossom (seul) | unlabelled | faux | lofi (table) |
| 5 | Blood Rave - Acid Techno | techno | oui | — |
| 5 | Blood Rave (seul) | rave | faux | techno (table) |
| 6 | 24K Golden Amapiano | amapiano | oui | — |
| 7 | UPB2026 Exclusive Vocals / Exclusive MIDI Kits / Melody Kits | unlabelled | acceptable (multi-styles, les sous-dossiers décident) | — |
| 8 | Riddim Riot | dubstep | oui | — |
| 8 | Roots - Afro House | afro-house | oui | — |
| 8 | UPB2026 Exclusive Atmospheres | unlabelled | acceptable | — |
| 9 | Reaper's Abyss - Deathstep | dubstep | oui | — |
| 9 | Producer Essentials - Melody Kits Synthwave | unlabelled | faux | synthwave (table ; absent du vocabulaire de tout.py) |
| 9 | Trap Melody Kits | trap | oui | — |
| 10 | Palmtrees - Liquid DnB | dnb | oui | — |
| 10 | Melody Kits - Deep House | house | oui | — |
| 10 | Melody Kits - Hardstyle | hardstyle | oui | — |
| 10 | Mainstage DnB | dnb | oui | — |
| 11 | Keys & Strings Vol. 3 | unlabelled | acceptable | — |
| 11 | Neo Rave 2026 | rave | oui | — |
| 12 | EDM Sound Effects Volume 2 | edm | oui | — |
| 12 | Essence Deep House | house | oui | — |
| 12 | Keta Serum 2 Rave Presets | rave | oui | — |

Pièges connus (mot de genre pris dans un nom qui n'est pas un genre) : « Jungle Animals FX » → dnb (faux), « House of Loops » → house (faux). Si `--plan` montre un pack de ce genre : `--force-style 'jungle\W*animals=fx'` ou le style juste, et le noter dans le message au chef.
