code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf

siege: a | profil: moyen | lang: de | device: foldable | font: 200 | mode: simple
mission: tuto-facile (neurofunk, chapitres 1 à 8) | buts: pas donnés par le brief
promesse: id=- | texte: - | ecran: - | tenue: pas vue | pourquoi: le brief ne donnait aucune promesse
chapitres:
  - 1 « Tempo und Farbe » : compris du premier coup
  - 2 « Die Kick » : hésitation (« Kick-Pad » introuvable, le pad s'appelle « Große Trommel Studio »)
  - 3 « Die Snare » : compris du premier coup
  - 4 « Die Hi-Hats » : hésitation (« Percussion-Pad » introuvable)
  - 5 « Der Bass » : hésitation (nombres de notes qui ne collent pas entre consigne et compteur)
  - 6 « Das Pad » : hésitation (le bouton orange du tuto « Auf die nächsten Takte kopieren » ne fait rien)
  - 7 « Der antwortende Stab » : hésitation (compteur de notes qui remonte de 1 à 2)
  - 8 « Dein erster Teil: der Groove » : compris du premier coup
hesitations:
  - ecran: 18 | cherchais: le Kick-Pad demandé | regarde: les noms des pads, aucun ne s'appelle Kick
  - ecran: 74 | cherchais: pourquoi la copie ne se fait pas | regarde: j'ai replié le panneau et trouvé le bouton « Kopieren » dessous
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: le texte de l'écran oublie parfois ce que l'image montre (puce « Acid 303 », bouton « Meinen Song anhören », bandeau « Kopieren »)
defauts:
  - geste: 74 | ecran.vu: 74 | cible: bouton ⊕ «   Auf die nächsten Takte kopieren » @ 517,839 | geste.type: touche
    attendu: les notes de la mesure 1 sont copiées et l'étape se valide | obtenu: rien ne change, même écran ; il a fallu replier le panneau et toucher « Kopieren » dessous
    symptome: rien
    gravite: contourne-en-2-gestes | revu: oui: ecran 76 | banc?: non | promesse: -
  - geste: 58 | ecran.vu: 58 | cible: Du bist dran: Tippe auf die 3 gestrichelten Noten in Takt 1 | geste.type: touche
    attendu: la consigne et le compteur disent le même nombre de notes | obtenu: consigne 3 notes, compteur « Noch 4 » ; l'étape se valide alors que le compteur dit encore « Noch 1 gestrichelte Note » (pareil écran 60 : 5 contre 4, et écran 85 au chapitre 7)
    symptome: incompris
    gravite: finition | revu: oui: ecran 85 | banc?: non | promesse: -
  - geste: 83 | ecran.vu: 83 | cible: Melodie · Takt 1/8 Noch 2 gestrichelte Noten, weiter oben ↑ Hier tippen | geste.type: touche
    attendu: après une note posée, le compteur descend | obtenu: il passe de « Noch 1 » à « Noch 2 », je ne sais plus où j'en suis
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 18 | ecran.vu: 18 | cible: Große Trommel Studio | geste.type: touche
    attendu: un pad nommé « Kick » comme le dit la consigne « Kick-Pad » | obtenu: le pad s'appelle « Große Trommel Studio », il faut deviner
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 41 | ecran.vu: 41 | cible: Tippe beim Percussion-Pad | geste.type: touche
    attendu: un pad « Percussion » dans la grille | obtenu: aucun pad de ce nom, le pad choisi s'appelle « Neurofunk · Trommelrand »
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 6 | ecran.vu: 6 | cible: Händeklatschen | geste.type: touche
    attendu: les noms des pads lisibles en entier | obtenu: « Händeklatsche / n » coupé en plein mot, les pastilles « R 30 » / « R 45 » recouvrent « Offene » / « Mittleres », « FX R 4 » coupé
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 17 | ecran.vu: 17 | cible: Noch 2 gestrichelte… | geste.type: touche
    attendu: voir la grille de pas et l'aide en entier | obtenu: le panneau du tuto recouvre la grille et l'aide est coupée par « … »
    symptome: cache
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 95 | ecran.vu: 95 | cible: Du bist dran: Tippe auf „Teil „Groove“ benennen“. | geste.type: touche
    attendu: des guillemets propres | obtenu: guillemets imbriqués « „Teil „Groove“ benennen“ », ça se lit mal
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 70 | vu: Akkorde-Spur | attendu: Akkord-Spur
  - ecran: 70 | vu: A♯ (clavier) | attendu: B (comme dans la feuille de tonalité allemande)
defauts-avant:
  - aucun défaut visé donné par le brief
gestes: 97 sur 160 | fin: chapitres 1 à 8 finis (écran 97, « Kapitel 8/15 · Erledigt »)
fin-tranche: oui (ROUTE ligne 533)
