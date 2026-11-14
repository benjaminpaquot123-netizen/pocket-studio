siege: b | profil: t6-Fb | lang: pl | device: phone360 | font: 100 | mode: pro
mission: libre | buts: son, trace, partage
promesse: id=t6-Fb-1 | texte: Przesuń fader, żeby zmienić głośność | ecran: 7 | tenue: oui | pourquoi: Acid 303 réglé à -10,5 au geste #5, toujours -10,5 après Struktura puis retour (écran 7) et après changement de langue (écran 26)
promesse: id=t6-Fb-2 | texte: ✏ Rysuj | ecran: 18 | tenue: oui | pourquoi: note posée sur C5 (écran 13, le synthé joue 1 note), glissée plus haut et à droite, l'image écran 18 la montre déplacée
promesse: id=t6-Fb-3 | texte: Język | ecran: 25 | tenue: oui | pourquoi: Polski → English (écran 22) puis retour à Polski (écran 25), tout repasse en polonais
promesse: id=t6-Fb-4 | texte: Przewodnik  · Zrób drugą część 30 % | ecran: 22 | tenue: non | pourquoi: en anglais l'étape du Guide reste en polonais ; en polonais, « Prosty: 5 zakładki » est faux et des textes sont coupés (écran 2)
oeil-neuf: q1 | ecran: 42 | image: 42 | tout ce qui a un contour est touchable (pistes, curseurs, Klawiatura, onglets) ; le « 303 basu » coupé à gauche ne dit pas qu'on peut faire défiler
oeil-neuf: q6 | ecran: 42 | image: 42 | le réglage se voit sur la valeur (146 Hz), mais sur ce petit écran la zone de travail ne fait que ~400 px entre les barres fixes
oeil-neuf: q9 | ecran: 42 | image: 42 | rien de coupé en largeur ; la liste coupe ses lignes haut/bas (normal en défilement)
hesitations:
  - ecran: 9 | cherchais: le synthé (piano roll) | regarde: il faut faire glisser la rangée de pistes, seul « Remix Rozdziel [coupé] » le laisse deviner
  - ecran: 23 | cherchais: revenir au polonais | regarde: la rubrique « langue du téléphone » a disparu, il a fallu faire défiler jusqu'à « Polski Polish »
  - ecran: 41 | cherchais: quoi faire dans le Guide | regarde: « Twoja kolej: dotknij „Później”. » demande de toucher « Plus tard » comme si c'était la tâche
abandon: jamais
incidents-banc:
  - geste: 14 | attente: 0 | vu: la note posée n'apparaît pas dans l'écran en texte, seule l'image la montre ; plafond d'images atteint, 4 gestes « attends » pour pouvoir regarder
  - geste: 30 | attente: 3 | vu: écran vide après « Ustaw brzmienie », revenu au geste suivant
defauts:
  - geste: 22 | ecran.vu: 22 | cible: bouton (boussole) « Guide  · Zrób drugą część 30 % » ⌃ [état « folded »] | geste.type: touche
    attendu: après passage en English, l'étape du Guide en anglais | obtenu: « Zrób drugą część » reste en polonais au milieu d'un écran anglais
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t6-Fb-4
  - geste: 19 | ecran.vu: 20 | cible: bouton « Prosty: 5 zakładki » | geste.type: touche
    attendu: « 5 zakładek » (génitif pluriel après 5) | obtenu: « 5 zakładki », faute de grammaire visible (revue écran 25)
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 25 | banc?: non | promesse: t6-Fb-4
  - geste: 1 | ecran.vu: 2 | cible: texte « Pady grają dźwięk, komórki trzymają uderzenia. Przytrzymaj: zmień dźwięk. » | geste.type: touche
    attendu: phrase d'aide lisible en entier | obtenu: coupée « Przytrzym… » sur l'image, la consigne de l'appui long est perdue
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 8 | banc?: non | promesse: t6-Fb-4
  - geste: 1 | ecran.vu: 2 | cible: bouton « Wszystkie pady » [coupé] | geste.type: touche
    attendu: bouton visible au-dessus du Guide | obtenu: caché derrière la barre « Przewodnik », seule une bordure dépasse (image 2)
    symptome: cache
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 8 | ecran.vu: 9 | cible: bouton « Synth Synth » | geste.type: glisse
    attendu: un sous-titre comme les autres pistes (« Linia basu », « Mikrofon ») | obtenu: « Synth » répété, rien de traduit ni d'utile
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 42 | banc?: non | promesse: t6-Fb-4
  - geste: 31 | ecran.vu: 31 | cible: bouton (créneaux) « Kwadrat » [coupé] | geste.type: touche
    attendu: réglages du son lisibles sur un petit téléphone | obtenu: le clavier ouvert d'office ne laisse qu'une rangée de réglages (boutons de forme d'onde coupés), il faut le replier (⌄) pour voir les curseurs
    symptome: cache
    gravite: contourne-en-1-gestes | revu: oui: ecran 35 | banc?: non | promesse: -
  - geste: 10 | ecran.vu: 10 | cible: bouton « C » [coupé] | geste.type: touche
    attendu: le clavier du synthé entier | obtenu: les touches du clavier sont coupées par la barre du Guide, seule une bande est touchable
    symptome: cache
    gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 20 | vu: 5 zakładki | attendu: 5 zakładek
  - ecran: 9 | vu: Synth (sous-titre) | attendu: Syntezator
