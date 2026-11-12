# Décisions du lot « sons-essai » (tour 5) : mêmes morceaux, sons modernes, derrière un interrupteur caché

- 2026-11-12 · Branche : `ccr-09a30009-jy4aua`, partie de la dernière `ccr-80af1a1b-mn0wec` mais en gardant le lot « sons-modernes », pas encore fusionné. C'est ma seule branche autorisée : la repartir de zéro l'aurait effacé. Retour arrière : sans objet.
- 2026-11-12 · Interrupteur : une propriété de la JVM (`ps.sons.essai=1`, `-Psons.essai=1` au banc), lue à chaque `StarterWizard.style`. Aucun réglage, écran ni fichier de l'appli ne l'allume ; il est donc invisible et coupé sur un téléphone. Raison : un seul point de branchement, et un rendu par défaut prouvé identique (le même objet recette, les empreintes des 37 morceaux prises sur la base). Retour arrière : supprimer `SoundTrial.kt` et le `.let(SoundTrial::styleOf)`.
- 2026-11-12 · Sources de la batterie : sons fabriqués par nous (`scripts/packs/trial_drums.py`), catégorie « sons à nous » de l'étude. Signature Sounds (passage en caisse avec une adresse e-mail : pas d'adresse de Benjamin donnée à un tiers), Freesound (clé) et Surge XT (GitHub bloqué par le réseau du conteneur) n'étaient pas atteignables par script ; les boucles WaivOps (7,6 et 12 Go, coups superposés) ont été écartées pour cet essai. Licence : originaux, notés dans `THIRD_PARTY.md`. Retour arrière : prendre ces sources plus tard, avec l'aide de Benjamin.
- 2026-11-12 · Packs d'essai en WAV 16 bits (et non en FLAC) : le banc les lit pour de vrai par `BenchFlac` (les FLAC n'y sont qu'une tonalité de remplacement). Ils ne sont publiés ni sur une Release ni dans l'APK. Retour arrière : supprimer `registerWavs`.
- 2026-11-12 · Ce que l'interrupteur change :
  - la batterie (packs `essai_<style>`) ;
  - l'OTT existant sur la nappe et le lead (profondeur 0,4) ;
  - en jump up, la nappe « Accords future bass » et le lead « Supersaw » (le hoover de 1991 part).
  - Pas en tech house (aucun son de la bibliothèque n'en remplace mieux les voix) ni en techno (le « Pluck future » mesurait plus sombre, −40,2 contre −38,9 dB d'air : retiré par un rouge et un vert).
  - Les basses ne changent pas : celles de la bass music bougent avec le tempo, et aucune basse de la bibliothèque n'est plus moderne pour la tech house et la techno.
  - Retour arrière : `SoundTrial.SOUNDS` et `VOICE_OTT`.
- 2026-11-12 · Ce qu'on garde du kit :
  - le shaker de la tech house (sa deuxième percussion) garde sa voix de kit, car les percussions du pack sont d'une seule sorte et la deuxième ligne aurait pris le même « toc » ;
  - la techno garde son rumble studio accordé.
  - Retour arrière : `SoundTrial.DRUMS`.
- 2026-11-12 · Extraits : 30 s depuis le premier Drop, lecture lancée comme « Écouter » (`-Pbench.listen=drop`), le plus fort baissé au niveau de l'autre (0,0 LU d'écart). Envoyés en WAV 16 bits, car l'AAC de ffmpeg dépassait 0 dBTP sur ces extraits denses. Retour arrière : sans objet.
- 2026-11-12 · Tables d'ondes (essai 3, partie B) non faites : on attend l'écoute de Benjamin avant de toucher au moteur. Retour arrière : sans objet.
