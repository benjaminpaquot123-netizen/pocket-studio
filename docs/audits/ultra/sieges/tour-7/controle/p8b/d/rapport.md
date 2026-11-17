code testé : 11ca024b94945ffae964263f27e8342aa3770316
joué par : sous-agent neuf
siege: d | profil: adulte-fr-curieux | lang: fr | device: téléphone | font: 100 | mode: simple
mission: tuto-facile, chapitre 12 « Les transitions » (cours Techno) | buts: gap-mots, cymbale-mots, partie-posee
promesse: id=gap-mots | texte: efface les 4 coups du pad « Claquement de mains » sur le dernier temps (touche les 4 croix rouges) | ecran: 23 | tenue: oui | pourquoi: « Mesure 4/4 · Claquement de mains », 4 croix barrées exactement sur le temps 4 (cases 13-16), 4 touches → « Réussi » (écran 27)
promesse: id=cymbale-mots | texte: sur le pad « Cymbale », touche la toute première case, encore vide | ecran: 30 | tenue: oui | pourquoi: pad « Cymbale », case 1 « vide, en pointillé : à toucher », touchée → « Bravo, chapitre fini ! » (écran 31)
promesse: id=partie-posee | texte: Il n'y a pas de partie « Montée » dans ton morceau : cette étape se fait dedans. / Créer la partie « Montée » | ecran: 11 | tenue: oui | pourquoi: après renommage Montée→Pont la phrase et le bouton « Créer » apparaissent ; rejoué écran 42 : la partie créée « J · Montée 4 mes. » est dans la liste du morceau (écran 47, 132→136 mesures)
promesse: id=vide-pas-fait | texte: Il n'y a pas de partie « Montée » dans ton morceau : cette étape se fait dedans. | ecran: 22 | tenue: non | pourquoi: pas de « Déjà fait » (bien), mais la phrase dit qu'il n'y a pas de Montée alors que la liste juste au-dessus montre « I · Montée × 1 · 8 mes. » (partie vide que je venais de nommer) ; « Créer » a ensuite rempli cette partie au lieu d'en créer une 2e (écran 23)
oeil-neuf: q1 | ecran: 47 | image: 47 | parties (▶, nom, −, +, ⋮), onglets, Annuler, ▶ le morceau, Chapitres, ⌃, « Lire » : tout semble touchable ; − et + grisés sans dire pourquoi
oeil-neuf: q6 | ecran: 47 | image: 47 | preuve : « J · Montée 4 mes. » dans la liste et 1/136 en haut ; suite donnée par le tuto (« Je le fais »)
oeil-neuf: q9 | ecran: 47 | image: 47 | rien de coupé hormis la dernière partie au bord du panneau du tuto (normal, la liste défile)
defauts-avant:
  - d-V1 | corrigé | écran 11 puis 47 | après renommage, la phrase « Il n'y a pas de partie « Montée »… » s'affiche et la partie créée entre dans la liste du morceau (J · Montée).
  - d-V2 | autre | écran 22 | plus de « Déjà fait » avec une Montée vide, mais la phrase affirme « pas de partie Montée » alors qu'elle est dans la liste ; « Créer » remplit la vide (pas de doublon).
  - d-P1 | corrigé | écran 23 | exactement 4 croix, toutes sur « Claquement de mains », mesure 4/4, temps 4.
  - d-P2 | corrigé | écran 30 | pad « Cymbale », case 1 vide et en pointillé, seule case marquée.
hesitations:
  - ecran: 7 | cherchais: où renommer une partie | regarde: trouvé par ⋮ de la partie, « Renommer » tout en bas de la feuille
  - ecran: 17 | cherchais: le bouton du tuto | regarde: le bouton replié disait encore « Créer la partie « Montée » » alors que j'avais une Montée
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: aucun ; outils hors human-act.py : lecture de journal.txt et des images de mon dossier de siège seulement
defauts:
  - geste: 20 | ecran.vu: 22 | cible: texte « Il n'y a pas de partie « Montée » dans ton morceau : cette étape se fait dedans. » | geste.type: touche
    attendu: avec une partie vide nommée « Montée », le tuto dit qu'elle existe mais est vide (et propose de la remplir) | obtenu: il affirme qu'il n'y a pas de Montée alors que « I · Montée » est dans la liste
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: vide-pas-fait
  - geste: 42 | ecran.vu: 47 | cible: icône ▶ « J · Montée 4 mes. » | geste.type: touche
    attendu: la Montée créée se place juste avant le Drop 2 (« un temps de silence juste avant le Drop 2 ») | obtenu: elle est posée après « E · Break », avant « F · Pont » ; entre elle et G · Drop 2 il y a Pont et Fill, donc le silence n'est pas juste avant le Drop 2
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: partie-posee
fin
