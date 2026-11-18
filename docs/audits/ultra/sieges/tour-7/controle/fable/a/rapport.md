code testé : 258441802eda97ec42ca0967188dfc83c7aeb29f
joué par : sous-agent neuf

siege: a | profil: controle-fable-repar | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile (chapitres 3 à 5) | buts: -
promesse: id=touche-ici | texte: Basse · mesure 1/8 ↑ Encore 6 notes en pointillé à toucher Touche ici | ecran: 63 | tenue: non | pourquoi: le rouleau bouge un peu puis ne va plus vers les notes du haut, la flèche ↑ reste
promesse: id=pointilles-partie | texte: Basse · mesure 1/1 ↑ Encore 24 notes en pointillé à toucher Touche ici | ecran: 80 | tenue: non | pourquoi: la partie B (1 mesure) affiche les pointillés de l'étape faite en A
oeil-neuf: q1 | ecran: 89 | image: 89 | tout ce qui a l'air touchable l'est ; « Lire ^ » au bout de la consigne ressemble à un lien, pas à un bouton
oeil-neuf: q6 | ecran: 89 | image: 89 | après une note posée le compteur « Encore 1 note » baisse, la preuve est là ; mes notes déjà posées sortent de la vue
oeil-neuf: q9 | ecran: 89 | image: 89 | consigne du bas coupée en « … » sur deux lignes ; ligne orange coupée en « à … » sur l'écran 62
hesitations:
  - ecran: 1 | cherchais: ce qu'on me demande de faire au chapitre 3 | regarde: seule la carte « Télécharger le kit House » et « Lire la suite », pas de consigne
  - ecran: 63 | cherchais: la note du haut vers laquelle pointe ↑ | regarde: j'ai dû faire glisser le rouleau à la main (écran 65)
  - ecran: 53 | cherchais: si la partie B devait recevoir l'exercice | regarde: les mêmes cases en pointillé qu'en A, et elles comptent en B
abandon: jamais
incidents-banc:
  - geste: 1 | attente: 60 | vu: premier « regarde » : « Le banc ne tourne pas pour ce lot », reparti après une attente de 60 s
defauts:
  - geste: 63 | ecran.vu: 63 | cible: Basse · mesure 1/8 ↑ Encore 6 notes en pointillé à toucher Touche ici | geste.type: touche
    attendu: le rouleau monte jusqu'aux notes du haut (La aigu) et les montre | obtenu: le rouleau monte un peu, aucune note du haut visible, La grave à moitié coupée en bas ; retoucher ne fait plus rien
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 67 et 78 | banc?: non | promesse: touche-ici
  - geste: 80 | ecran.vu: 80 | cible: Basse · mesure 1/1 ↑ Encore 24 notes en pointillé à toucher Touche ici | geste.type: touche
    attendu: en partie B, pas de pointillés de l'étape de la basse faite en partie A | obtenu: 24 notes en pointillé (4 mesures) sur une partie B qui n'a qu'une mesure
    symptome: incompris | gravite: finition | revu: oui: ecran 53 (même chose sur la batterie, chapitre 4) | banc?: non | promesse: pointilles-partie
  - geste: 12 | ecran.vu: 12 | cible: Télécharger le kit House (4 Mo) | geste.type: touche
    attendu: la consigne de l'étape (même repliée) sous la carte du kit | obtenu: aucune phrase de consigne, seulement « Pourquoi ? » et « Lire la suite » ; la consigne n'arrive qu'après « Plus tard » (écran 25)
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 1 et 24 | banc?: non | promesse: -
  - geste: 2 | ecran.vu: 2 | cible: Lire la suite | geste.type: touche
    attendu: la consigne s'ouvre et les cases restent visibles | obtenu: la consigne s'affiche au-dessus du kit, le panneau couvre toute la grille ; « Lire la suite » et « Réduire » visibles en même temps
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 62 | ecran.vu: 62 | cible: Basse · mesure 1/8 ↑ Encore 6 notes en pointillé à toucher Touche ici | geste.type: touche
    attendu: la ligne orange entière | obtenu: « ↑ Encore 6 notes en pointillé à … » coupé à cause du bouton « Touche ici »
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 77 | banc?: non | promesse: -
defauts-avant:
  - V1 | autre | ecran: 63 | flèche présente et le toucher fait défiler ; quand il reste une seule rangée (écrans 70, 89) la note arrive bien en vue, mais quand les notes sont sur deux hauteurs éloignées (écrans 63, 66, 77) le rouleau s'arrête avant la note visée et la flèche reste
  - V2 | encore là | ecran: 12 | chapitres 3 et 4 : sous la carte du kit, aucune consigne repliée, juste « Pourquoi ? » et « Lire la suite » (écrans 1, 12, 24) ; ouverte, elle s'affiche au-dessus de la carte (écran 2)
  - V3 | corrigé | ecran: 92 | chaque étape vue avait « Je le fais », « Créer la piste Basse », « Copier… », « Écouter l'exemple » ou « Montre-moi » ; chapitre 3 « Fait » (écran 10), chapitre 5 coché en entier dans la liste (écran 92)
  - V4 | encore là | ecran: 80 | en partie B, les pointillés de l'étape faite en A apparaissent (basse écran 80, batterie écran 53) ; en changeant de chapitre ils partent bien (écrans 58, 95)
