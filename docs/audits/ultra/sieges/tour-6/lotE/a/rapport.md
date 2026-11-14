siege: a | profil: t6-Ea | lang: en | device: phone | font: 130 | mode: pro
mission: padlive | buts: son, stop, prise
promesse: id=t6-Ea-1 | texte: Pad Live NEW Pads ready to play, your sounds, live effects. It sounds right away. | ecran: 4 | tenue: oui | pourquoi: premier toucher sur « Clap » : le pad s'allume et le son part tout de suite (après 1 attente de « Preparing the ready-to-play pads… »).
promesse: id=t6-Ea-2 | texte: Stop all | ecran: 18 | tenue: oui | pourquoi: projet en lecture + boucle « My take 1 » : « Stop all » coupe tout (aussi écrans 12, 33 en pleine prise, 41 pendant « Listen »).
promesse: id=t6-Ea-3 | texte: Set kept · 0:09 | ecran: 40 | tenue: oui | pourquoi: après REC/End, « Listen » rejoue la performance ; la prise « My take 1 » rejoue aussi sur le pad B1 (écran 14).
promesse: id=t6-Ea-4 | texte: Effects · Remix | ecran: 24 | tenue: oui | pourquoi: curseurs Speed/Depth/Shape/Left-right entiers et réglables (Depth 60 % → 23 %) ; seules les rangées de puces défilantes sont coupées au bord, et elles défilent.
promesse: id=t6-Ea-5 | texte: Pac… | ecran: 19 | tenue: non | pourquoi: tout est en anglais, mais des textes sont coupés à 130 % : « Pac… » (écran 19) et « drums, bass, vocals a… » (écran 47).
oeil-neuf: q1 | ecran: 47 | image: 47 | les cartes et leurs flèches sont touchables, rien de trompeur ; le bandeau feedback du bas est à part.
oeil-neuf: q6 | ecran: 47 | image: 47 | la carte « Keep going » montre « Live 5 bars · 0:10 · Saved just now » : preuve que la perf est gardée, je sais où revenir.
oeil-neuf: q9 | ecran: 47 | image: 47 | « Track separation: drums, bass, vocals a… » coupé dans la carte Remix ; le reste tient.
hesitations:
  - ecran: 19 | cherchais: où régler les effets live (Filter, Echo, Roll n'ont pas de réglage visible) | regarde: j'ai ouvert « More » puis « Effects », et l'aide « ? » explique de glisser le doigt vers le haut.
  - ecran: 35 | cherchais: la différence entre « REC » en haut et « Record what I play » | regarde: deux boutons d'enregistrement ; REC enregistre tout le set, l'autre une prise pour un pad, il faut essayer pour comprendre.
  - ecran: 20 | cherchais: pourquoi la feuille d'effets s'appelle « Remix » alors que je suis dans Pad Live | regarde: le titre « Effects · Remix », j'ai cru ouvrir les effets de la page Remix.
abandon: jamais
incidents-banc:
  - geste: 10 | attente: 3 | vu: « attends 3 » a renvoyé un écran vide en texte, le « regarde » suivant a montré la feuille « Your take: 5 bars ».
defauts:
  - geste: 13 | ecran.vu: 19 | cible: bouton « Pack… » ▾ @ 349,138 | geste.type: touche
    attendu: le nom du choix de pack lisible en entier à 130 % | obtenu: l'image montre « Pac… » sur la banque B (vide), mot coupé
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 31 | banc?: non | promesse: t6-Ea-5
  - geste: 47 | ecran.vu: 47 | cible: bouton (quatre vagues) « Remix Track separation: drums, bass, vocals and the rest. » → @ 294,274 | geste.type: glisse
    attendu: la description complète | obtenu: l'image montre « drums, bass, vocals a… » coupé (déjà sur l'écran 1)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 1 | banc?: non | promesse: t6-Ea-5
  - geste: 20 | ecran.vu: 20 | cible: texte « Effects · Remix » @ 190,62 | geste.type: touche
    attendu: un titre lié à Pad Live (ou à la piste « Pads live ») | obtenu: « Effects · Remix » ouvert depuis Pad Live, on croit régler la page Remix
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 39 | ecran.vu: 39 | cible: texte « Listen » @ 315,228 | geste.type: touche
    attendu: « Listen » et « Share » dessinés comme des boutons | obtenu: sur l'image, simples mots colorés dans le bandeau « Set kept · 0:09 », on ne devine pas qu'on peut les toucher (et « Share » existe aussi plus bas)
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: t6-Ea-3
  - geste: 39 | ecran.vu: 39 | cible: bouton [allumé] @ 87,319 | geste.type: touche
    attendu: la grille de pads garde sa taille et son alignement | obtenu: avec le bandeau « Set kept », les pads rétrécissent et se décalent (marge gauche plus grande, colonne ▶ plus petite) par rapport aux écrans 31 et 40
    symptome: cache | gravite: finition | revu: oui: ecran 40 | banc?: non | promesse: -
  - geste: 47 | ecran.vu: 47 | cible: bouton ★ ★ ★ « Challenges Listen to a rhythm, play it back on the pads: win stars. » → @ 294,722 | geste.type: glisse
    attendu: icônes et textes alignés comme les cartes voisines | obtenu: icônes carrées pleines (Pad Live, Challenges) contre icônes rondes ailleurs ; texte de « Challenges » décalé à droite, icône plus grosse
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 38 | ecran.vu: 38 | cible: bouton « End » @ 528,47 | geste.type: touche
    attendu: « End » termine l'enregistrement du set et arrête la lecture | obtenu: le set est gardé, mais le projet et la boucle continuent de jouer ; il faut « Stop all » en plus
    symptome: incompris | gravite: contourne-en-1-gestes | revu: non | banc?: son | promesse: -
mots-faux:
  - ecran: 19 | vu: Pac… | attendu: Pack…
