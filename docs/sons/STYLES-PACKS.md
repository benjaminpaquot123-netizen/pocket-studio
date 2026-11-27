# Styles des packs (passe profonde)

Règle de `scripts/sons/tout.py` (`style_of`) : les dossiers sont lus du plus intérieur vers l'extérieur, le nom du dossier du pack en dernier. Un nom de pack connu (`mine_or.STYLES`) gagne, sinon le mot de genre qui finit le plus tard. Le style du pack ne remplit que les lignes restées vides ou « unlabelled ».

Le lanceur `scripts/sons/passe2/lance.py` ajoute un style forcé par pack (table `FORCES`, ou `--force-style REGEX=STYLE`). Il ne s'applique qu'aux lignes dont le style vaut le style posé par le nom du pack, « unlabelled » ou vide, ET dont les dossiers intérieurs ne nomment aucun style.

**Les noms des packs ne figurent pas ici (dépôt public)** ; les titres commerciaux sont reconnus par empreinte dans `mine_or.STYLES` et `FORCES`. **Les noms exacts des dossiers ne sont pas connus** (les CSV des passes précédentes sont anonymisés, seuls leurs en-têtes nomment les packs). Les repères ci-dessous (rNpN) décrivent les noms probables. Chaque discussion lance d'abord `--plan` et compare la colonne « style » à ce tableau ; un nom différent qui tombe faux = `--force-style` à ajouter.

| RAR | Dossier de pack (nom probable) | Style posé par tout.py | Juste ? | Style à forcer |
|---|---|---|---|---|
| 1 | r1p1 (multi-genres) | multi | oui (les sous-dossiers portent les genres) | — |
| 2 | r2p1 (titre + « Drift ») | drift-phonk | oui | — |
| 2 | r2p1 (si le nom dit « Drift Phonk ») | phonk | faux | drift-phonk (table) |
| 3 | r3p1 (titre + « Melodic Techno ») | techno | oui | — |
| 3 | r3p1 (titre seul) | unlabelled | faux | techno (table) |
| 4 | r4p1 (titre + « Chillhop ») | lofi | oui | — |
| 4 | r4p1 (titre seul) | unlabelled | faux | lofi (table) |
| 5 | r5p1 (titre + « Acid Techno ») | techno | oui | — |
| 5 | r5p1 (titre seul, contient « Rave ») | rave | faux | techno (table) |
| 6 | r6p1 (titre + « Amapiano ») | amapiano | oui | — |
| 7 | r7p1 (voix, MIDI, kits mélodiques) | unlabelled | acceptable (multi-styles, les sous-dossiers décident) | — |
| 8 | r8p1 (titre dubstep) | dubstep | oui | — |
| 8 | r8p2 (titre + « Afro House ») | afro-house | oui | — |
| 8 | r8p3 (atmosphères) | unlabelled | acceptable | — |
| 9 | r9p2 (titre + « Deathstep ») | dubstep | oui | — |
| 9 | r9p1 (kits mélodiques synthwave) | unlabelled | faux | synthwave (table ; absent du vocabulaire de tout.py) |
| 9 | r9p3 (kits mélodiques trap) | trap | oui | — |
| 10 | r10p1 (titre + « Liquid DnB ») | dnb | oui | — |
| 10 | r10p2 (kits mélodiques deep house) | house | oui | — |
| 10 | r10p3 (kits mélodiques hardstyle) | hardstyle | oui | — |
| 11 | r11p1 (claviers et cordes) | unlabelled | acceptable | — |
| 11 | r11p2 (titre DnB) | dnb | oui | — |
| 11 | r11p3 (titre rave) | rave | oui | — |
| 12 | r12p1 (effets EDM) | edm | oui | — |
| 12 | r12p2 (titre deep house) | house | oui | — |
| 12 | r12p3 (presets rave) | rave | oui | — |

Pièges connus (mot de genre pris dans un nom qui n'est pas un genre) : « Jungle Animals FX » → dnb (faux), « House of Loops » → house (faux). Si `--plan` montre un pack de ce genre : `--force-style 'jungle\W*animals=fx'` ou le style juste, et le noter dans le message au chef.
