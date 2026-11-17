code testé : ede109d8ada92d293e0cfd3b777f53b38e2e3b68
joué par : sous-agent neuf

siege: a | profil: adulte-fr-tuto-house-ch3 | lang: fr | device: telephone | font: 100 | mode: simple
mission: tuto-facile | buts: chapitre-3, chapitre-4, chapitre-5
promesse: id=consigne-kit | texte: À toi : sur le pad « Claquement de mains », … / Télécharger le kit House (4 Mo) | ecran: 1 | tenue: oui | pourquoi: la consigne est juste au-dessus de la carte du kit, visible sans rien ouvrir (image 1)
promesse: id=touche-ici | texte: Basse · mesure 1/8 ↑ Encore 6 notes en pointillé à toucher Touche ici | ecran: 67 | tenue: non | pourquoi: notes La grave + La aigu : le 1er toucher laisse les aigus hors écran (il en faut 2) ; idem écran 85 en mesure 4 (1 note haute restée hors écran)
promesse: id=pointilles-partie | texte: Toutes les notes en pointillé sont posées | ecran: 78 | tenue: oui | pourquoi: étapes faites en B (basse) ou en A (charley) ne redemandent aucun pointillé dans l'autre partie (écrans 24 et 78, image 78)
oeil-neuf: q1 | ecran: 93 | image: 93 | touchables clairs (barre du bas, pistes, Partie : B, Touche ici disparu) ; « mes. 4 » et la bande « Toutes les notes… » ne disent pas si on peut les toucher
oeil-neuf: q6 | ecran: 93 | image: 93 | preuve nette : « Bravo, chapitre fini ! » + « Chapitre 5/15 · Fait » ; mais rien ne dit où aller ensuite (pas de « chapitre suivant » visible, il faut deviner ⌃ ou Chapitres)
oeil-neuf: q9 | ecran: 93 | image: 93 | « En rythme » coupé au bord droit ; « Mon morceau » sur 2 lignes ; reste lisible
hesitations:
  - ecran: 67 | cherchais: les notes aiguës après « Touche ici » | regarde: rien en haut, les graves à moitié sous le panneau ; j'ai retouché
  - ecran: 80 | cherchais: l'effet de « Copier sur les mesures suivantes » | regarde: rien ne bouge, j'étais en Partie A sans l'avoir choisie
  - ecran: 92 | cherchais: pourquoi on me redemande de copier après la mesure 4 | regarde: 2e copie, puis « Bravo »
abandon: jamais
incidents-banc:
  - geste: 28 | attente: 0 | vu: sortie du banc coupée par un tuyau « head » (BrokenPipe côté outil), le geste est passé ; outils hors human-act.py : grep/sed sur la sortie, Read des images et cat du rapport dans mon dossier
defauts:
  - geste: 67 | ecran.vu: 67 | cible: Basse · mesure 1/8 ↑ Encore 6 notes en pointillé à toucher Touche ici | geste.type: touche
    attendu: les notes pointillées visées (La aigu) à l'écran | obtenu: aigus toujours hors écran, graves à moitié cachées sous le panneau du tuto ; 2e toucher nécessaire
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 85 | banc?: non | promesse: touche-ici
  - geste: 85 | ecran.vu: 90 | cible: Basse · mesure 4/8 ↑ Encore 6 notes en pointillé à toucher Touche ici | geste.type: touche
    attendu: toutes les notes visées visibles, flèche à jour | obtenu: 5 notes visibles (graves coupées au bord bas), 1 aiguë hors écran, flèche ↑ restée affichée ; « plus haut ↑ » seulement après les 5
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 91 | banc?: non | promesse: touche-ici
  - geste: 58 | ecran.vu: 62 | cible: le morceau | geste.type: touche
    attendu: après l'écoute, rester sur la partie où je travaillais (A) | obtenu: l'éditeur a suivi la lecture jusqu'en Partie B et y est resté ; chapitre 5 commencé en B sans le savoir
    symptome: perdu | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 80 | ecran.vu: 81 | cible: Copier sur les mesures suivantes | geste.type: touche
    attendu: la mesure 1 copiée, ou un message « tes notes sont en Partie B » | obtenu: rien ne change en Partie A ; écran 81 dit même « Motif posé ! » et « mesure 1/1 » ; il faut repasser en B (3 gestes)
    symptome: rien | gravite: contourne-en-3-gestes | revu: oui: ecran 84 | banc?: non | promesse: -
  - geste: 84 | ecran.vu: 85 | cible: ↑ Encore 6 notes en pointillé à ... | geste.type: touche
    attendu: phrase entière dans la bande | obtenu: « à toucher » coupé en « à ... » (image 85)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
defauts-avant:
  - a-V1 | encore là | ecran: 67 | 1er « Touche ici » laisse les La aigus hors écran (2 touches) ; revu écran 85 (mesure 4, 1 note haute manquante)
  - a-V2 | corrigé | ecran: 1 | consigne « À toi : sur le pad « Claquement de mains », … » juste au-dessus de la carte du kit
  - a-V4 | corrigé | ecran: 78 | Partie A sans pointillés de basse après l'étape faite en B ; charley fait en A sans pointillés en B (écran 24)
