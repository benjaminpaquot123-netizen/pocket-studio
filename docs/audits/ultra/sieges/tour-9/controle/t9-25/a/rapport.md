code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: debutant-motive | lang: tr | device: phone360 | font: 130 | mode: simple
mission: tuto-dubstep-ch1-8 (tuto-facile) | buts: -
promesse: id=- | texte: Bravo, bölüm bitti! | ecran: 117 | tenue: oui | pourquoi: chapitres 1 à 8 finis, « İlk bölümün: Groove Aşama 8/15 · Tamam » affiché
chapitres:
  - ch1 Parçanın hızı ve rengi : compris du premier coup
  - ch2 Kick : hésitation (consigne cachée sous « Devamını oku », sélecteur d'ölçü à moitié sous l'en-tête)
  - ch3 Trampet ve clap : hésitation (il fallait l'ölçü 2, la consigne ne le disait pas)
  - ch4 Hi-hat'ler : hésitation (« perküsyon pad'i » = « Davul kenarı », swing sans repère visible)
  - ch5 Bas : hésitation (compteur de notes qui saute, bouton de copie sans effet)
  - ch6 Pad : hésitation (piano roll écrasé, bouton de copie à toucher deux fois)
  - ch7 Çan : compris du premier coup
  - ch8 İlk bölümün: Groove : compris du premier coup
  - budget : non épuisé (environ 116 gestes sur 174)
hesitations:
  - ecran: 28 | cherchais: comment aller à l'ölçü 2 pour le trampet | regarde: le sélecteur d'ölçü caché sous « Bölüm: A », trouvé en glissant la grille
  - ecran: 58 | cherchais: le « perküsyon pad'i » demandé | regarde: aucun pad de ce nom, c'était « Davul kenarı »
  - ecran: 85 | cherchais: combien de notes de basse il restait | regarde: le compteur passait de 2 à 1, puis « kaldı: 3, daha yukarıda », puis 1 et « 2 … ileride → »
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: outils hors human-act.py : cat (consignes, gabarit, fichiers du siège), tail/grep sur journal.txt du siège ; aucun gel, aucun écran vide
defauts:
  - geste: 90 | ecran.vu: 91 | cible: Sonraki ölçülere kopyala | geste.type: touche
    attendu: les notes de basse copiées dans les ölçü suivants | obtenu: rien ne bouge, j'ai dû passer par « Kopyala » de l'éditeur
    symptome: rien
    gravite: contourne-en-1-gestes | revu: oui: ecran 99 (#98 sans effet, #99 marche) | banc?: non | promesse: -
  - geste: 27 | ecran.vu: 28 | cible: Sıra sende: “Trampet” pad'inde noktalı kutuya dokun: ölçüde yalnızca bir tane, ortada. | geste.type: touche
    attendu: trouver la case pointillée dans l'ölçü visible | obtenu: la case était dans l'ölçü 2, et le sélecteur d'ölçü était caché sous l'en-tête
    symptome: cache
    gravite: contourne-en-2-gestes | revu: oui: ecran 37 | banc?: non | promesse: -
  - geste: 15 | ecran.vu: 16 | cible: Örnek dinlenmeden geçildi. | geste.type: touche
    attendu: rien, j'avais écouté l'exemple | obtenu: message disant que j'ai passé l'exemple sans l'écouter, et la consigne repliée sous « Devamını oku »
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 19 | ecran.vu: 24 | cible: Ölçü / Takip et | geste.type: touche
    attendu: voir le sélecteur d'ölçü en entier | obtenu: la rangée est à moitié sous l'en-tête, « Takip et » coupé
    symptome: cache
    gravite: finition | revu: oui: ecran 28 | banc?: non | promesse: -
  - geste: 70 | ecran.vu: 70 | cible: Swing'i işaretli değere getir | geste.type: glisse
    attendu: une marque visible sur le réglage de swing | obtenu: aucune marque, j'ai tâtonné jusqu'à 20 %
    symptome: sans-aide
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 84 | ecran.vu: 85 | cible: Noktalı nota kaldı: 3, daha yukarıda ↑ | geste.type: touche
    attendu: un compteur qui descend d'une note à chaque note posée | obtenu: le compteur remonte et redescend (2, 1, 3, 1, 2)
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 2 | ecran.vu: 3 | cible: Kit indiriliyor… %0 | geste.type: touche
    attendu: le kit téléchargé puis affiché | obtenu: la barre disparaît à %0 et l'écran montre toujours « Kit: Stüdyo »
    symptome: incompris
    gravite: finition | revu: non | banc?: temps | promesse: -
  - geste: 96 | ecran.vu: 96 | cible: piano roll de l'akor (canal Pad) | geste.type: touche
    attendu: voir les notes à toucher | obtenu: le piano roll est écrasé sous le panneau du tuto, on voit à peine la note du bas
    symptome: cache
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 115 | ecran.vu: 116 | cible: Bölüme “Groove” adını ver | geste.type: touche
    attendu: un texte propre | obtenu: guillemets imbriqués « Bölüme “Groove” adını ver »
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 5 | ecran.vu: 6 | cible: Pad'ler ses çıkarır… / Kit: Stüdyo | geste.type: touche
    attendu: lire ces textes | obtenu: cachés derrière le panneau du tuto
    symptome: cache
    gravite: finition | revu: oui: ecran 11 | banc?: non | promesse: -
mots-faux:
  - ecran: 58 | vu: perküsyon pad'i | attendu: Davul kenarı
  - ecran: 94 | vu: Pad (titre du chapitre des accords) | attendu: Akorlar
defauts-avant:
