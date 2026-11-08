# Relecture neuve et audits extérieurs : chantier « banc fiable », séance 3 (branche eq/tour-5-banc3)

Trois lectures indépendantes, aucune n'ayant écrit de code de la branche (2026-11-08) : la relecture neuve de la séance 3, un audit extérieur « code du banc » et un audit extérieur « preuves et affirmations » sur tout le chantier (dff04ad..).

**Corrigé avant fusion** (détail : `banc-fiable-journal.md`, 3.5a) :

| Gravité | Constat | Suite |
|---|---|---|
| grave | position du morceau du faux moteur trop naïve (décompte, relais remplacé, fin du morceau, annulation, tempo) | **corrigé** : `SongClock` sur les règles d'Engine.cpp ; `BenchSongClockTest` (5 cas) ; garde du compteur durcie |
| grave | sélecteur : projet .zip donné à l'import d'un pack de sons (faux défaut) | **corrigé** |
| grave | tampon : moteur, modèle, Gradle hors empreinte ; tampon copié accepté ; code changé pendant le selftest | **corrigé** et testé |
| grave | « banc contredit » au tour même du verdict | **corrigé** |
| grave | preuves « appli » en @Ignore, jamais rejouées | **corrigé** : gardes `@KnownDefect` (message exact), 4 vertes |
| grave | verdicts sans preuve (Ca-1, Cb-67, Fa-11, Aa-43, Cc-23, Fa-5, Cc-41 b/c) ; essais téléphone manquants ; T4-01 confondu avec Pad Live | **corrigé** : repassés « à trancher » ; 18 essais ; tel-12 pour la vue Live ; E d #11 a sa ligne |
| grave | journal : chiffres faux, 3.2 coché à 2/14, P3 annoncé prouvé | **corrigé** dans le journal (affirmations retirées ou chiffrées) |
| moyen | tableau de bord (ordre des tours, chiffre manuel prioritaire, sièges coupés, preuve Bench*) ; geste mal écrit lu comme plantage ; rejeu à moitié créé | **corrigé** |

**Restent** (non bloquants pour la fusion du banc, listés dans `docs/REVIEW.md`) : 12 contrôles sur 14, P3 non prouvé, .mid sans test, coupable de la quarantaine, deux selftests concurrents, sortie de siège non détectée, 43 citations ambiguës, candidats « revient ? » non lus, verdicts à prouver sans banc.

Verdict des trois lectures après corrections : fusion possible à condition que le portail soit vert sur ce code (vérifié par `fusion-equipes.sh`) ; **chantier non terminé** : la liste ci-dessus en est le reste.
