siege: b | profil: t6-Cb | lang: fr | device: phone | font: 100 | mode: pro
mission: libre | buts: son, trace, partage
oeil-neuf: 16 écrans, 4 graves
non-vus: Écouter une démo, J'apprends, Remix (Extraire la voix), Enregistrer ma voix, Bibliothèque de sons, Aide, Réglages en Simple
hesitations:
  - ecran: 10 | cherchais: changer le son de la piste | regarde: icône ♪ seule, sans mot, à côté des pistes
  - ecran: 26 | cherchais: ce que veut dire « 22.3 » | regarde: un chiffre dans un cercle, qui avance et ne se touche pas
  - ecran: 31 | cherchais: les curseurs de volume après avoir touché « Mix » | regarde: des réglages d'export
abandon: jamais
incidents-banc:
  - geste: 0 | attente: 900 | vu: premier « regarde » lancé avant le banc : « L'appli ne s'est pas ouverte (15 min) » ; banc lancé (human-tester.sh), puis écran 1
  - geste: 58 | attente: 0 | vu: « tape » de 4 coups compté comme 1 seul (« 0 sur 4 ») : banc?: rythme
  - geste: 80 | attente: 0 | vu: outils hors human-act.py : Read des images du siège ; grep de journal.txt et images.txt du siège pour les numéros de gestes ; avant la partie, mise en route de la machine (git, outils Android, selftest, journal Gradle de /tmp)
defauts:
  - geste: 8 | ecran.vu: 9 | cible: bouton + «  Piste » [coupé] @ 522,186 | geste.type: touche
    attendu: le bouton « + Piste » se lit en entier | obtenu: « + Pis… » coupé au bord droit (image 8)
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 12 | banc?: non | promesse: -
  - geste: 27 | ecran.vu: 28 | cible: bouton (horloge et flèche) «  Garder ce que je viens de jouer » @ 418,489 | geste.type: touche
    attendu: le bouton se lit en entier en Pro | obtenu: « Garder ce que je viens… » coupé (image 28)
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 22 | ecran.vu: 23 | cible: bouton « Boucle » (flèches en boucle) [coupé] @ 500,183 | geste.type: touche
    attendu: le bouton « Boucle » visible en entier | obtenu: coupé au bord droit, rien n'indique la suite (image 23)
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 19 | ecran.vu: 20 | cible: bouton « + Nouvelle partie vide » [coupé] @ 190,934 | geste.type: touche
    attendu: le bouton se lit en entier | obtenu: coupé à moitié par la barre du Guide (image 20)
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 19 | ecran.vu: 20 | cible: texte « Ton morceau est vide : ajoute tes parties dans l'ordre où elles jouent (Intro, Couplet, Refrain…). » @ 294,752 | geste.type: touche
    attendu: un message cohérent avec « le morceau » qui joue depuis l'écran 4 | obtenu: « Ton morceau est vide » pendant la lecture en boucle (image 20)
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 13 | ecran.vu: 14 | cible: bouton (dossier) « Mes fichiers Retrouver et renvoyer : M4A, WAV, vidéo, .zip » › @ 272,894 | geste.type: touche
    attendu: « Mes fichiers » visible | obtenu: caché sous le bandeau « Nouveau son : Basse ronde » (image 14)
    symptome: cache
    gravite: contourne-en-1-gestes | revu: non | banc?: temps | promesse: -
  - geste: 30 | ecran.vu: 31 | cible: onglet (curseurs) « Mix » @ 294,1158 | geste.type: touche
    attendu: l'onglet Mix s'ouvre sur les curseurs de volume, comme en Simple | obtenu: il s'ouvre au milieu de la page (export, puis automation)
    symptome: mauvais-ecran
    gravite: contourne-en-1-gestes | revu: oui: ecran 71 | banc?: non | promesse: -
  - geste: 40 | ecran.vu: 41 | cible: icône ← @ 54,54 | geste.type: retour
    attendu: Retour depuis « Mes projets » (ouvert par Plus) ramène au studio | obtenu: l'Accueil, et la musique s'arrête sans le dire
    symptome: mauvais-ecran
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 38 | ecran.vu: 39 | cible: bouton (dossier ouvert) « Mes projets Ouvrir, créer, dupliquer, partager un projet » › @ 294,315 | geste.type: touche
    attendu: Plus s'ouvre en haut de sa liste et j'ouvre « Mes projets » | obtenu: la liste avait gardé son défilement : « Passer en mode Simple » s'applique sans confirmation ni message
    symptome: perdu
    gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: 36 | ecran.vu: 37 | cible: bouton « Appliquer à tout le morceau » @ 211,312 | geste.type: touche
    attendu: la carte des accords montre la nouvelle suite | obtenu: « Fait : accords du morceau Do Sol La m Fa » mais la carte reste « A Do »
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
