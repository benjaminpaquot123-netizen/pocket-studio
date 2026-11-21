code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: debutant-motive | lang: tr | device: foldable | font: 200 | mode: simple
mission: tuto-facile (dubstep, chapitres 9 à 15) | buts: aucun id donné par le brief
chapitres: 9 hésitation | 10 compris du premier coup | 11 hésitation | 12 hésitation | 13 compris du premier coup | 14 compris du premier coup | 15 compris du premier coup — tuto 9→15 fini en 86 gestes + fin (87/167)
hesitations:
  - ecran: 8 | cherchais: les boîtes de la carte des parties (étape 7/7) | regarde: la bannière « Dubstep kitini indir » les couvrait, vues après « Sonra »
  - ecran: 40 | cherchais: le bouton « + » de la partie suivante | regarde: la liste avait sauté sous le panneau, il fallait glisser à chaque ajout
  - ecran: 59 | cherchais: le pas de kick à toucher | regarde: le panneau du tuto couvrait la grille, j'ai deviné qu'il fallait « Ben yaparım »
abandon: jamais
incidents-banc:
defauts:
  - geste: 39 | ecran.vu: 40 | cible: + C · Yükseliş | geste.type: touche
    attendu: ajouter les parties l'une après l'autre en touchant chaque « + » | obtenu: après chaque ajout la liste « Sona ekle » saute, les boutons passent sous le panneau, il faut glisser avant chaque ajout
    symptome: cache | gravite: contourne-en-6-gestes | revu: oui: ecran 42, 44, 46, 48 | banc?: non | promesse: -
  - geste: 7 | ecran.vu: 8 | cible: Dubstep kitini indir (3 MB) | geste.type: touche
    attendu: voir la carte « her kutu bir bölüm » que l'étape me demande de regarder | obtenu: la bannière de téléchargement couvre les boîtes 16/8/32 et coupe la courbe, visibles seulement après « Sonra »
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 17 | banc?: non | promesse: -
  - geste: 18 | ecran.vu: 19 | cible: Bravo, bölüm bitti! | geste.type: touche
    attendu: un mot qui dise « étape finie » comme l'en-tête « Aşama 9/15 » | obtenu: « bölüm bitti » / « Sonraki bölüm », alors que « bölüm » est aussi le nom d'une partie du morceau : on croit qu'une partie est finie
    symptome: texte-faux | gravite: finition | revu: oui: ecran 79 | banc?: non | promesse: -
  - geste: 19 | ecran.vu: 20 | cible: Sıra sende: ““Giriş” bölümünü oluştur” düğmesine dokun. | geste.type: touche
    attendu: des guillemets propres autour du nom du bouton | obtenu: guillemets doublés « ““Giriş” … ” », à chaque étape des chapitres 10 et 12
    symptome: texte-faux | gravite: finition | revu: oui: ecran 21 | banc?: non | promesse: -
  - geste: 50 | ecran.vu: 51 | cible: G · Bitiş × 1 · 8 ölçü | geste.type: touche
    attendu: la ligne de la partie lisible d'un coup | obtenu: coupée sur 3 lignes, avec un point tout seul sur une ligne
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 57 | ecran.vu: 58 | cible: Tiz tom | geste.type: touche
    attendu: lire le nom de chaque pad | obtenu: les pastilles « FX » / « R 45 » couvrent « Büyük », « Orta », « Davul », et « Tiz tom » est coupé
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 62 | ecran.vu: 63 | cible: Burada fazladan vuruş (üstü çizili): 3 | geste.type: touche
    attendu: lire l'indice en entier | obtenu: l'indice est coupé, tout comme « Ölçü 4/4 · Büyük dav… » sur l'écran 58
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 75 | ecran.vu: 76 | cible: Sıra sende: duyulması gerekeni aç, rahatsız edeni kıs. Bir parçayı tek başına dinlemek için ▶'ye dokun. | geste.type: touche
    attendu: lire la consigne du Mix et voir les boutons sous les curseurs | obtenu: la consigne du haut est coupée, et les boutons haut-parleur et ▶ sous les curseurs sont coupés par le panneau
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 77 | banc?: non | promesse: -
  - geste: 85 | ecran.vu: 86 | cible: Şarkım Kaydedildi | geste.type: touche
    attendu: mon projet garde son nom « Projem 1 » après le partage | obtenu: après l'export, l'en-tête dit « Şarkım » : le projet a changé de nom sans que je le demande
    symptome: texte-faux | gravite: finition | revu: oui: ecran 87 | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 1 | cible: Projem 1 Kaydedildi | geste.type: touche
    attendu: voir le nom du projet et « Kaydedildi » | obtenu: seul « Projem 1 » se voit, « Kaydedildi » est coupé dans l'en-tête
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 19 | vu: bölüm (fin d'étape du tuto) | attendu: aşama
  - ecran: 34 | vu: Ekle (consigne) | attendu: Sona ekle (texte du bouton)
defauts-avant:
  - aucun défaut visé donné par le brief | pas atteint | ecran: - | rien à contrôler

fin-tranche: non (dernier step=share, done=36 ; pas de « Bravo » final : l’écran 87 affiche « 7 adımın kaldı », 7 étapes des chapitres 1-8 que l’amenée n’a pas validées)
