# Fiche : sons neufs (2026-11-27)

Fiche du chef d'orchestre (session_01NqonxZBBysqqWBbnsC2VTP). Rôle : producteur de sons, zone indiquée dans ton message d'ouverture (ZONE, SLUG, FAMILLES, STYLES ci-dessous viennent de ce tableau). Tu es une discussion muette : tu écris « ok » en fin de tour, et tu n'envoies qu'un seul message au chef, par send_message à @parent, à la fin : « fini <url artefact> <sha> » suivi de 3 lignes, ou « bloqué <raison> ». Ensuite, archive-toi.

But : Benjamin veut voir DEMAIN MATIN une page d'exemples de NOUVEAUX sons, fabriqués par nous selon l'« ADN » mesuré de ses packs, pour tous les gros styles, avec l'original à côté. Ta part, c'est les sons neufs de ta zone. Le chef assemble la page et les originaux viennent d'ailleurs : tu n'as pas les packs et tu ne les cherches pas.

À lire :
- docs/sons/fin/OUTILS-EXISTANTS.md (git fetch origin eq/son-fin-outils-existants), c'est le plan d'outils choisi.
- L'ADN : docs/sons/fin/ESSENTIA-BASE.md et les tsv de familles sur eq/son-fin-essentia-01. Une version corrigée arrive cette nuit : refais un fetch avant le calage final et prends la plus récente.

Travail :
1. Installe les outils libres dans un venv de ton scratchpad : Surge XT avec surgepy (ou le binaire Surge XT en ligne de commande), pycma, pedalboard, essentia, librosa, et laion-clap si ça s'installe en moins de 15 min. Pour les enregistrements libres CC0 ou CC BY (VCSL, VSCO 2 CE, Virtuosity Drums, Salamander, VocalSet, Freesound CC0), télécharge chacun dans un dossier neuf et lance Python avec -I.
2. Fabrique des sons NEUFS pour les familles de ta zone : FAMILLES. Pour chaque gros style concerné (STYLES), fais 3 à 4 exemples par famille utile au style. Une boucle d'optimisation pycma règle Surge XT ou le traitement pedalboard pour rapprocher chaque son de la médiane de l'ADN de sa famille (mesures Essentia), avec librosa comme contre-mesure. Ne garde que les meilleurs.
3. Note chaque son avec sa distance Essentia à l'ADN de sa famille, en écarts-types (0 = parfait), plus CLAP si tu l'as.
4. Sortie : MP3 192 kb/s, crête −1 dBFS. Nom : <style>-<famille>-<nn>.mp3, sans aucun nom de vendeur ni de pack.
5. Publie un artefact privé neuf, « Nouveaux sons ZONE » : une simple liste de lecteurs par style puis par famille, avec la note sous chaque son. Charge d'abord les skills artifact-design et artifact-capabilities, puis mets les MP3 en assets.
6. Sur une branche neuve eq/son-neuf-SLUG, pousse :
   - docs/sons/neuf/SLUG.json, le manifeste : fichier, style, famille, outil, source et licence, note Essentia, url de l'asset ;
   - tes scripts dans scripts/son/neuf/SLUG/ ;
   - une ligne THIRD_PARTY.md par source libre utilisée.
   Commite par chemin (jamais git add -A ni .). JAMAIS d'audio dans git. 0 lien symbolique (`git ls-tree -r HEAD | grep -c '^120000'` = 0). Pas de PR, rien sur main ni sur ccr-80af1a1b-mn0wec.
7. Délai : envoie « fini » avant 5 h 00 UTC demain (6 h belge), même incomplet, avec ce qui est prêt. La qualité d'abord, mais au moins 2 styles complets plutôt que tout à moitié.


## Tableau des zones

| ZONE | SLUG | FAMILLES | STYLES |
|---|---|---|---|
| Batterie électronique | batterie-elec | kick, snare, clap, charleston fermé et ouvert, percussions électroniques, plus une boucle de batterie de 4 mesures par style assemblée avec tes coups | trap, drill, hip-hop boom bap, house, techno, drum & bass, EDM/future, reggaeton/afro, lo-fi |
| Batterie acoustique | batterie-acou | kick, caisse claire, toms, charleston, cymbales (crash, ride) acoustiques, à partir d'enregistrements libres façonnés vers l'ADN, plus une boucle de 4 mesures par style | rock, pop, funk, jazz/soul, lo-fi, boom bap acoustique |
| Basses | basses | 808 (dans sa propre famille), basse synthé, sub, reese, basse acoustique ou électrique (enregistrements libres), en notes tenues et une phrase de 2 mesures par style | trap, drill, house, techno, drum & bass, EDM/future, pop, lo-fi, reggaeton/afro |
| Synthés et nappes | synthes | leads, plucks, nappes (pads), arpèges, stabs et accords de synthé, en une phrase ou un accord de 2 à 4 mesures par son | trap, house, techno, EDM/future, pop, lo-fi, synthwave, drum & bass |
| Claviers, effets et voix | claviers-fx | piano (Salamander), piano électrique et orgue, effets (riser, impact, downlifter, sweep), coupures de voix (vocal chops, pas de paroles, à partir de VocalSet), en phrases courtes | pop, lo-fi, house, trap, EDM/future, jazz/soul, R&B |
