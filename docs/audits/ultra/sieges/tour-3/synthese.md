# Audit ultra, tour 3 : synthèse des 24 sièges (lots A à F) — « l'appli tient-elle ses promesses ? »
Sièges : scratchpad/ultra/tour-3/lot{A..F}/{a..d}/ (rapport.md avec champ « promesse », journal.txt, screen-N). Profils et promesses : sieges.tsv. Notes banc : notes-banc.txt (LIRE D'ABORD).
Banc à part : un agent « banc » (docs/audits/ultra/tour-3-banc.md quand il aura fini) tranche appli/banc pour : export 3 s, Pad Live stop/ligne/Tout couper, lecture du morceau entier, gel en tapant un nom. Ses verdicts priment.
×N = sièges. « revient » = déjà signalé à un tour précédent : trouver POURQUOI la correction n'a pas tenu et corriger à la racine, avec un test qui rejoue les gestes exacts du siège.

## Zone 1 — Pad Live, défis, pads, remix vers pads
1. ×4 Quitter Pad Live (Retour) laisse des boucles jouer sur l'accueil/Défis/Remix, aucun bouton pour les arrêter hors Pad Live (D a 57, D b 39, F c 47, C b).
2. ×4 « Tout couper » / ■ de ligne / « Lancer la ligne à la mesure » / ■ Wszystko des blocs n'arrêtent ni ne lancent (C b 13-57, D d 61, E d 51, revient tour 1 n° 6) — verdict banc d'abord.
3. Bloquant : pad live « Voix · prêt » ne joue jamais, même lecture lancée (E d 49-50) ; la voix importée « en silence » reste muette dans Pad Live sans le dire (E d 36).
4. revient : « Sur les pads » remplace encore Tom médium/aigu au lieu des pads libres (E d 16) ; promesse « rien d'écrasé sans prévenir ». Annuler laisse le message affiché (E d 22). « En el Live » écrase une case déjà prête sans avertir (E d 33).
5. Défis : impossible sans lire (A a : aucun modèle visible du rythme à refaire, ni tempo ; « coche » saute au niveau 5 sans étoiles ; aide texte seulement) ; défi qui finit tout seul au 4e coup et le bouton sous le doigt devient « suivant » (F a 41) ; une boucle touchée par erreur joue sous l'exemple (A a 3).
6. ×3 Pads affichés « + » vides à l'ouverture alors que le pack est chargé (F c 6, D b 6, A a 2) ; à 200 % noms coupés (« Claque ment… », « Klaxo n d… ») et « Gros pads » caché dans Plus (F c) ; pad qui joue à peine visible (F c 50).
7. ×4 Petits boutons au coin des pads sans nom TalkBack (D a 52, D c 4, F a 4, F c 6).
8. Changer de pack remplace la page sans « Annuler » alors que l'aide le promet (D c 9, C b 48) ; colonne Remix promise par l'aide absente du Live (D d 59) ; « Klaxon de DJ » : « n'a pas pu être lu » (F c 29-32, vérifier banc/mémoire).
9. Bloquant (B c, ar) : découpe « par phrases » : toucher une tranche ne la choisit pas, boutons d'envoi grisés, plus de retour au mode « Un bout » ; aide qui cite « Couper ici / Supprimer / Loop » absents. Découpe : la poignée Fin déplace le Début, la règle zoome seule (E d 17-18). Remix en cours introuvable depuis Mon morceau (B c 11).

## Zone 2 — Exports, partage, reprendre ailleurs
10. ×3 Club/streaming : le choix est loin de l'export (milieu de Mix), absent de « Prêt à partager ? », éteint par défaut (A d 3-4, F b 26) ; aide club qui parle du « Drop » d'un morceau sans Drop (A d 12).
11. Aucune liste de mes exports dans l'appli ; la carte « Sauvegardé dans… » disparaît après Annuler (A d 45).
12. Ancien export proposé au partage après modification du morceau (F d 48) ; « Partager les 2 fichiers » n'en envoie qu'un (F d 56) ; export piste par piste : Synthé manquant (F d 54), pistes du remix non séparées (un seul fichier « remix », F b 53) ; « Prêt : aucune piste muette » alors que la voix du remix est coupée (F b 58).
13. Envoyer le projet (.zip) pour le reprendre sur un autre appareil : caché dans Mes projets › ⋯, absent de « Envoyer » et de l'aide (F d 22, 61). Le champ « Nom du morceau » du partage renomme le projet à chaque export (F b 41). « (copie) » en français dans l'appli anglaise (F d 71).
14. ×3 Mode Simple : fenêtre de fin d'export pleine de LUFS/dBTP/kHz/kb/s (A c 47, C c 46, D c 60). Durée annoncée qui diffère entre Partager et Mix (A d 3, F d 7 : 8 mesures/0:16 vs 1 mesure).

## Zone 3 — Lecture, navigation, sauvegarde, prise en main
15. ×4 Grave : impossible d'écouter tout le morceau : « Jouer » ne joue qu'une boucle de 1-4 mesures ; toucher une partie (« écoute à partir d'elle ») ou « A partir do meu take » ne joue rien ; « La musique entière, du début » s'arrête (A b 17-33, C a 40-50, E a 87, F a) — verdict banc d'abord. Compteur « Intro mes. 1 » figé pendant la lecture (E a 44, D c 57).
16. ×2 Grave : gel en tapant un nom (nom du projet B a 9, nom de piste E c 6/24) — suspect SelectAllTextField (tour 2 n° 26). Renommer le projet depuis Pad Live colle le texte derrière l'ancien nom (D b 60, revient).
17. Grave (B a) : en revenant, l'accueil ne propose pas « Reprendre mon morceau » ; « Rétablir » introuvable ; Mes projets dit « ouvert à l'instant » au lieu de « modifié il y a 3 jours ».
18. Grave (D a) : après avoir supprimé le projet ouvert depuis Mes projets, Retour ne fait plus rien ; (D a 16) Retour pendant « Enregistrer ma mélodie » jette les notes (le micro, lui, garde la prise).
19. ×2 La carte « Enregistrer ma voix » ouvre le projet Live ou « Mon premier beat » au lieu du morceau en cours (D a 75, E c 3). Grave (E b) : revenir par « J'apprends » crée un nouveau projet et repart au chapitre 1 sans prévenir.
20. Une main (A b) : Mes projets, OK de Renommer, choix de boucle hors pouce ; carte Simple trop haute.
21. Paysage téléphone (D b) : zone utile ~190 px entre barres ; rail d'onglets coupé (Mix, Envoyer invisibles) ; carte Continuer vide ; boutons sous la barre du bas. Tablette 130 % (D c 44) : Guide couvre la piste Basse.
22. Promesses de l'accueil : « Un style en une touche » = 4 écrans (F a 58) ; « ⋮ › Aide » cité sur l'accueil qui n'a pas de ⋮ (D c 17) ; Guide qui saute de % sans action (D c, F d 60) ; bandeaux « Nouveau son… / Annuler » qui restent sur tous les onglets et couvrent le contenu (C c 19, C d 42, D a 15).

## Zone 4 — Enregistrement
23. ×2 Le rond rouge du haut « Enregistrer le micro » → « Le moteur audio n'a pas démarré » alors que « Enregistrer » de la carte marche (E c 27, F a 75) ; (C a 24) il crée une piste « Voz » fantôme sans prise — verdict banc d'abord.
24. Pendant la prise, décompte/niveau/« Arrêter et garder » tout en haut, loin de la piste (E c 29) ; décompte « 2 mesures » affiché « 1… » (E c 73).
25. Impossible d'écouter une prise seule pour comparer (C a 13-15) ; « Prise précédente » = bascule (E c) ; deux durées pour la même prise (E c 16).
26. Demande système de notifications au milieu de la première prise, en anglais, prise perdue (C c 22) ; mélodie mise sur une nouvelle piste avec un autre son que celui choisi (C c 22) ; touches du clavier non enregistrées sauf appuis longs (C c 23 — banc ?).
27. « Muet » + « désactivé » se lit « pas coupé » sur une piste coupée (E c 37, revient tour 2 n° 19 pour une autre langue).

## Zone 5 — Tutos, aides, sons, parties, synthé
28. ×4 revient (3e fois) : une fenêtre « Télécharger le kit » remplace le tuto sous le doigt : « Lire la suite » (A c 14), « Écouter l'exemple » (E b 5). La protection d'1 s du tour 2 (ArrivalGuard) ne suffit pas : RACINE (la carte kit ne doit jamais remplacer le panneau du tuto).
29. ×3 revient : le tuto nomme « Grosse caisse 1 » / « Basdrum 1 » alors que le pad s'appelle « Grosse caisse studio » (A c 58, E b 21, revient tour 2 n° 28) : un seul nom partout.
30. ×2 Progression du tuto fausse : « Bravo, chapitre fini » mais 2/3 (A c 19-31, E b 35) ; fin de chapitre sans « Écouter mon morceau » (A c 36) ; nouvelle étape ouverte au milieu du texte (A c 20).
31. ×3 Jargon en Facile/Simple : Dorien, Phrygien, mineur harmonique, 909, 707, kick, FX, BPM, groove (A c, E b, C c).
32. Tuto Pro (B d, de) : les noms du tuto ≠ l'écran (Acid 303/Cutoff vs Synth/Filter Sweep ; « Drop », « Intro 2 » inexistants) ; validation qui échoue sans dire quoi, message en français dans l'appli allemande ; « Montre-moi » écrit mais n'est pas validé ; kit 909 et effets promis absents ; « B♭ » vs « A# ».
33. ×2 Grave : la recherche de sons ne trouve pas ses propres exemples (« verre », « conga », « caisse », « davul », « trampet ») (F a 21-27, D b 22) ; l'onglet « Caisses claires » montre aussi grosses caisses, charleys, toms (F a 29).
34. Aides « ? » qui mentent (D d) : ■ Tout arrêter, colonne Remix, climat « plusieurs effets » = 0 effet, geste du pan faux et curseur caché sous le Guide, aide Pas en Simple qui cite Roulements/Pan ; « la cible » au lieu de l'étoile (F a 34, F c 24) ; ×3 étiquettes « ? » mal posées (« Annuler » sur Cofnij/Undo, « Écouter » sur Stop, étiquettes collées en haut) (D c 48, D d 17, E c 65).
35. Parties (E a) : impossible de renommer une partie (Drop, Outro) alors que l'appli invite à « ajouter tes parties » ; liste de 27 lignes d'1 mesure, trois parties du même nom ; « Seulement celle-ci » ferme la feuille et copie sans demander ; champ « mesures » au comportement différent selon la partie. (D c 41) blocs A-D vs ordre à 5 parties.
36. Synthé sur pliable (C d) : éditeur ~300 px, clavier qui se rouvre, pas de bouton fermer (Retour quitte le projet) ; mettre à jour un preset refusé (« change le nom ») et nom perdu ; ajouter une piste avec un son favori impossible directement.

## Zone 6 — Langues et accessibilité
37. TalkBack (B b, à VÉRIFIER sans le banc, lit=non grisait le texte) : boutons du haut, ▶/réglages de piste, touches du clavier sans nom ; rien ne dit qu'une note est enregistrée ; bulle d'aide qui cache Enregistrer et Annuler.
38. Langues : ×2 « 1 parts » (en, it « 1 parti ») ; hi (tu/vous mêlés, anglais Verse/Chorus/bar) ; ko (particules « 란 », « 을(를) », « streaming/remix » dans les noms de fichiers) ; pl (grammaire « Dla pad 1 », « co kroki: 16 », « Odpal na ») ; it (« Ré♭ », « Le sue prime carte ») ; nl (« Delen » ambigu, « Stap » pour deux choses, « Halve tempo ») ; de (« Zurück zu Leicht » vs Einfach, Verbreiterer…) ; id (calques « dia » pour un objet, « min ») ; tr (« alttaki çubuğunda ») ; zh (复音 pour unisson, Chorus/Bitcrusher non traduits) ; ar (Bass/Snare/Hi-hat/Live, titre en majuscules) ; pt-BR (« em a parte » → « na parte »). Balayer chaque catalogue pour ces mêmes fautes, pas seulement les lignes vues.
