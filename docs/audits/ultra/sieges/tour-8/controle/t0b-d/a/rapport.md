code testé : b49ce1e0096c95db1d73f3b345b8a862efaf27ec
joué par : sous-agent neuf
siege: d | profil: leo-debutant-presse | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile (chapitre 12) | buts: -
chapitres: 12=compris du premier coup, 3=compris du premier coup
chapitres bloqués: 0 | graves neufs: 0
defauts-avant:
  - d-montee: autre : projet 1 (amené), la Montée existait et se place juste avant le Drop, pas avant le Drop 2 (écran 25) ; projet 2 (vide), « Créer la partie « Montée » » la crée en partie B, il n'y a pas de Drop 2 où la placer (écrans 48-49)
  - d-V2: corrigé + écran 48 (Montée vide qui vient d'être créée : le message « Il n'y a pas de partie « Montée » » a disparu ; absent aussi au projet 1, écran 1)
  - a-V1-anneau: autre : au ch.3 (projet 2), aucun anneau « Touche ici » vu, même après attends 10 ; seules les cases à toucher sont en pointillé, et ça suffit à guider (images écrans 35, 37, 42)
  - c-croix: corrigé + écran 6 (croix rouges + bord en pointillé, vue d'un pad), écran 11 (pointillé au Drop), écran 18 (pointillé dans la vue « Tous les pads »)
promesse: id=- | texte: Bravo, chapitre fini ! | ecran: 23 | tenue: non | pourquoi: l'étape demandait 4 cases Maracas (une par temps), « Bravo » arrive après 1 seule case
promesse: id=- | texte: Réussi Étape suivante | ecran: 9 | tenue: oui | pourquoi: les croix effacées, l'étape est validée
promesse: id=- | texte: Bravo, chapitre fini ! | ecran: 44 | tenue: oui | pourquoi: ch.3 fini après les claquements sur les temps 2 et 4 des 2 mesures
oeil-neuf: q1 | ecran: 49 | image: 49 | boutons « + A », « + B · Montée », copie, vide, « Ce qui joue », onglets, ▶ : tout ce qui a un contour se touche ; « Chapitres » sans contour mais lisible
oeil-neuf: q6 | ecran: 49 | image: 49 | la Montée créée n'apparaît que dans les boutons « + B · Montée » ; la ligne du temps est vide, aucune preuve d'ordre, le tuto demande d'effacer des croix qu'on ne voit pas ici
oeil-neuf: q9 | ecran: 49 | image: 49 | la consigne du tuto est coupée (« … dernier temps (t… ») mais « Lire » permet de l'ouvrir ; rien d'autre coupé
hesitations:
  - ecran: 30 | cherchais: la liste des chapitres | regarde: « Chapitres » touché à côté, la liste s'ouvre au second essai (écran 33)
  - ecran: 36 | cherchais: où sont les cases de la mesure 2 au ch.3 | regarde: les pointillés une fois la mesure 2 choisie
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: outils hors human-act.py : Read sur les images du siège (screen-N-grille.png) ; lecture de la fiche et des consignes testeur au départ ; grep sur journal.txt du siège pour les numéros de gestes
  - geste: 30 | attente: 0 | vu: toucher sur « Chapitres » à 444,573 ne touche rien (panneau déplacé), pas une fiche
defauts:
  - geste: 22 | ecran.vu: 23 | cible: Bravo, chapitre fini ! | geste.type: touche
    attendu: 4 cases Maracas demandées (une par temps de la mesure 1), Bravo après la 4e | obtenu: Bravo et chapitre fini après la 1re case
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 47 | ecran.vu: 48 | cible: À toi : dans la partie « Montée », dernière mesure, efface les coups du dernier temps (touche chaque croix rouge) : un temps de silence juste avant le Drop. | geste.type: touche
    attendu: dans un projet vide, la Montée créée a des coups à effacer, ou une consigne adaptée | obtenu: Montée vide, morceau sans Drop (écran 49), consigne parle de croix et d'un Drop qui n'existent pas
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 1 | cible: Partie : G · Fin | geste.type: touche
    attendu: l'en-tête et le sélecteur disent la même partie | obtenu: l'en-tête dit « Intro mesure 1/96 » et le sélecteur « Partie : G · Fin »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
