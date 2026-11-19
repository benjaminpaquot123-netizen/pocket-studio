code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf
siege: a | profil: confirmé | lang: nl | device: foldable | font: 200 | mode: pro
mission: tuto-pro | buts: - | - | -
promesse: id=- | texte: « Nu jij: zet in ‘Hoofduitgang’ het ‘Doelvolume’ op ‘Auto (stijl)’: het mikt op ongeveer −10 LUFS » | ecran: 95 | tenue: non | pourquoi: pas de Doelvolume dans Hoofduitgang ; trouvé seulement dans la feuille Delen (écran 109), et Auto y donne −9 LUFS « luid, zoals clubmuziek »
hesitations:
  - ecran: 38 | cherchais: les boutons « + » pour ajouter le part suivant | regarde: ils sautent sous le pli après chaque ajout, il faut refaire défiler
  - ecran: 68 | cherchais: les effets du Snare en touchant le nom de la piste (comme dit l'aide) | regarde: rien ne s'ouvre, pas de canal par pad
  - ecran: 97 | cherchais: « Doelvolume » dans Hoofduitgang | regarde: seulement Hoofdvolume, Details (LUFS), aucun choix Auto
abandon: ecran 122 | parce que aucun fader du Mix ne bouge (6 essais), chapitre 12 impossible à finir
incidents-banc:
  - geste: 74-77, 80-81, 87, 119-121 | attente: 0 | vu: le banc refuse chaque glisse sur le mixeur « doigt couvert : Drums couvre Volume van Akkoorden » ; je ne sais pas dire si c'est l'appli ou le banc
defauts:
  - geste: #74 | ecran.vu: 75 | cible: curseur Akkoorden « +0,0 dB, à 80 % » | geste.type: glisse
    attendu: baisser Akkoorden puis Melodie (chapitre 12) | obtenu: le fader ne bouge jamais (6 essais, 3 positions de défilement) ; Melodie introuvable ; chapitre 12 reste 0/2
    symptome: rien | gravite: mission-impossible | revu: oui: ecran 121 | banc?: peut-être (refus « doigt couvert ») | promesse: -
  - geste: #96 | ecran.vu: 97 | cible: « zet in ‘Hoofduitgang’ het ‘Doelvolume’ op ‘Auto (stijl)’ » | geste.type: touche
    attendu: un réglage Doelvolume dans Hoofduitgang | obtenu: il n'existe que dans la feuille Delen du chapitre 14 ; chapitre 13 coché seulement après l'export
    symptome: perdu | gravite: contourne-en-14-gestes | revu: oui: ecran 109 | banc?: non | promesse: -
  - geste: #73 | ecran.vu: 74 | cible: « Ik doe het » (chapitre 12) | geste.type: touche
    attendu: que l'appli baisse les deux pistes ou me montre où | obtenu: le Mix défile un peu, rien ne change (idem #79, #95)
    symptome: rien | gravite: mission-impossible | revu: oui: ecran 80 | banc?: non | promesse: -
  - geste: #109 | ecran.vu: 110 | cible: « Auto (stijl) » | geste.type: touche
    attendu: environ −10 LUFS, moins fort que trap (texte du chapitre 13) | obtenu: « Volume: −9,0, luid, zoals clubmuziek »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 114 | banc?: non | promesse: -
  - geste: #67 | ecran.vu: 68 | cible: « Drums » (nom de piste du Mix) | geste.type: touche
    attendu: ouvrir effets et courbes (« tik op de naam van een spoor ») | obtenu: rien ; il a fallu repasser par le panneau du tuto
    symptome: rien | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: #38 | ecran.vu: 38 | cible: boutons « + » d'ajout de part (chapitre 9) | geste.type: touche
    attendu: ajouter plusieurs parts à la suite | obtenu: après chaque ajout les « + » descendent sous le pli (y 671, coupé), re-défiler à chaque fois
    symptome: cache | gravite: contourne-en-6-gestes | revu: oui: ecran 50 | banc?: non | promesse: -
  - geste: #55 | ecran.vu: 56 | cible: grille de pas du kick (chapitre 10) | geste.type: touche
    attendu: voir la case à barrer | obtenu: la grille est sous le panneau du tuto, seul un contour jaune dépasse
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: #8 | ecran.vu: 8 | cible: « Kopiëren naar de volgende maten » | geste.type: touche
    attendu: copier la mélodie sur les mesures suivantes | obtenu: rien de visible ; le « Kopiëren » du piano-roll a marché
    symptome: rien | gravite: contourne-en-1-gestes | revu: oui: ecran 10 | banc?: non | promesse: -
  - geste: #64 | ecran.vu: 65 | cible: « Ik doe het » (chapitre 11) | geste.type: touche
    attendu: « ik doe het » = c'est moi qui le fais | obtenu: l'appli pose elle-même le Bitcrusher sur le Kick
    symptome: incompris | gravite: finition | revu: oui: ecran 70 | banc?: non | promesse: -
  - geste: #66 | ecran.vu: 76 | cible: noms des pistes du Mix | geste.type: touche
    attendu: noms lisibles en police 200 % | obtenu: « Drum s », « Akko ord… », « CURVE N » coupés au milieu
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 121 | banc?: non | promesse: -
mots-faux:
  - ecran: 31 | vu: Acid 303 Baslijn (onglet et piste, projet boom bap) | attendu: Bas / Baslijn
  - ecran: 2 | vu: FM-piano | attendu: elektrische piano (mot du tuto)
  - ecran: 109 | vu: 3 delen | attendu: 8 delen (écran d'accueil, écran 84)
chapitres:
  - ch5 De melodie : hésitation (bouton du panneau sans effet, #8)
  - ch6 Refrein : compris du premier coup
  - ch7 De delen begrijpen : compris du premier coup (« Goed gedaan » + « Voorbeeld overgeslagen » en même temps)
  - ch8 Bouw je delen : compris du premier coup
  - ch9 Volgorde : hésitation (« + » qui sautent)
  - ch10 Overgangen : hésitation (grille cachée)
  - ch11 Effecten : hésitation (nom de piste sans effet)
  - ch12 De mix : bloqué (faders immobiles)
  - ch13 De mastering : bloqué dans Hoofduitgang, coché via l'export du ch14
  - ch14 Exporteer : compris du premier coup (M4A partagé, −9 LUFS)
gestes utilisés : 121 sur 153 (+ fin)

fin-tranche: non (dernier step=levels, done=30)
