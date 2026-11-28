# Targeted sounds, zone A: kicks and drum loops

For each style of the "Nouveaux sons" page (trap, drill, boom-bap, house, techno, drum-and-bass, edm, reggaeton-afro, lo-fi), this folder holds:
- 8 kicks: `<style>-kick-NN.mp3`;
- 8 four-bar drum loops: `<style>-boucle-NN.mp3`.

All of them are synthesised from scratch with numpy and pedalboard: no sample and no recording. License: CC0 (own work).

How they were made:
- Parameters are tuned with CMA-ES towards the per-style targets in `zone-a-kicks-boucles.tsv` (branch eq/son-fin-essentia-01 @ faa70cb3a).
- Each sound is scored by distance to its target: the weighted RMS of clipped z-scores. Lower means closer.
- `index.json` lists, for each file, its score, its measured descriptors, the share of descriptors inside p25–p75 and inside p10–p90, and the worst descriptors.
- Snare, clap and hat are used inside the loops only (kit in `scripts/son/cible-a/kit.json`). No isolated snare, clap or hat is delivered.

Known weak spot: most drum-and-bass kicks stay far from their target (scores around 11; the best one is 4.3). The drum-and-bass loops are fine (0.5 to 1.2).

Rebuild (scripts in `scripts/son/cible-a/`):

    A="--targets zone-a-kicks-boucles.tsv --desc <folder with cibles_desc.py> --out out"
    python3 -I kmake3.py opt $A && python3 -I loops3.py opt $A --kit kit.json
    python3 -I kmake3.py export $A && python3 -I loops3.py export $A --kit kit.json
