siege: c | profil: t6-Ac lyceen-envoie-a-un-ami | lang: fr | device: phone | font: 100 | mode: simple
mission: exports | buts: spotify, club, retrouve
promesse: id=t6-Ac-1 | texte: 1. Partager un morceau se fait en 3 gestes au plus depuis le studio | ecran: 10 | tenue: oui | pourquoi: onglet « Partager » (#6), bouton « Partager » (#7), « WhatsApp » (#9) : 3 gestes, « C'est prêt » à l'écran 10
promesse: id=t6-Ac-2 | texte: 2. « Mes exports » s'atteint en Mode simple (grave C d #50) | ecran: 27 | tenue: oui | pourquoi: bouton « Mes fichiers » visible dans l'onglet Partager en Simple, il ouvre la liste (vide aux écrans 28 et 31, pleine à l'écran 43 : voir défaut)
promesse: id=t6-Ac-3 | texte: 3. Le projet envoyé en .zip est listé dans « Mes exports » | ecran: 38 | tenue: oui | pourquoi: « Trap-20261112-195658.pocketstudio.zip » en tête de la liste, aussi à l'écran 43
promesse: id=t6-Ac-4 | texte: 4. La longueur annoncée avant l'export est celle du fichier exporté | ecran: 10 | tenue: oui | pourquoi: « 2:58 » annoncé (écran 7), « Durée : 2 min 58 s » au bilan (écran 10) ; chiffre du banc, rien jugé de plus
oeil-neuf: q1 | ecran: 43 | image: 43 | les 4 « Renvoyer » et « Fermer » se voient touchables (orange) ; les noms de fichiers non, et ne le sont pas : pas d'écart
oeil-neuf: q6 | ecran: 43 | image: 43 | la preuve est là (mes 4 fichiers, les plus récents d'abord), suite claire : Renvoyer ou Fermer ; mais rien ne dit « aucun fichier » quand la liste est vide (écrans 28, 31)
oeil-neuf: q9 | ecran: 43 | image: 43 | noms de fichiers coupés au milieu d'un mot : « pocketstudio.z / ip », « 19 / 5558.wav », « 195612. / wav »
hesitations:
  - ecran: 12 | cherchais: où choisir « club » ou « streaming » en Simple | regarde: j'ai touché « Niveau : comme la musique en streaming (−14) », rien ne se passe
  - ecran: 13 | cherchais: le volume club | regarde: « Autres fichiers … (passe en mode Pro) », je bascule en Pro sans vraiment le vouloir
  - ecran: 26 | cherchais: le Mix (passage demandé) | regarde: vu, rien à juger
abandon: jamais
incidents-banc:
  - geste: 0 | attente: 600 | vu: premier « regarde » sans banc lancé (je ne savais pas qu'il fallait lancer scripts/human-tester.sh) ; outils hors human-act.py pour préparer la machine : selftest.sh, human-tester.sh (en-tête lu), lecture de journal.txt et images de mon siège
defauts:
  - geste: 27 | ecran.vu: 28 | cible: bouton (dossier) « Mes fichiers Retrouver et renvoyer : M4A, WAV, vidéo, .zip » › | geste.type: touche
    attendu: mes 3 fichiers déjà faits (m4a, streaming, club) | obtenu: liste vide, sans même « aucun fichier » ; pareil à l'écran 31 ; pleine plus tard (écran 43)
    symptome: perdu | gravite: contourne-en-6-gestes | revu: oui: ecran 31 | banc?: temps | promesse: t6-Ac-2
  - geste: 13 | ecran.vu: 14 | cible: bouton (notes sur une pile) › « Autres fichiers Piste par piste, MIDI, FLAC… (passe en mode Pro) » [coupé] | geste.type: touche
    attendu: en Simple, l'export annonçait « Niveau : comme la musique en streaming (−14) » | obtenu: en Pro le volume choisi est « Auto (Trap) · −8,5 LUFS » et le bilan dit « à la cible (Auto (style)) » : mon m4a n'était pas « streaming »
    symptome: texte-faux | gravite: contourne-en-3-gestes | revu: oui: ecran 14 | banc?: non | promesse: -
  - geste: 18 | ecran.vu: 20 | cible: bouton « Créer le fichier du morceau » | geste.type: touche
    attendu: juste après l'export, rien à refaire | obtenu: « Le morceau a changé depuis cet export : exporte à nouveau pour envoyer la nouvelle version. » alors que je n'ai rien changé (vu aussi écrans 14, 24)
    symptome: incompris | gravite: finition | revu: oui: ecran 24 | banc?: non | promesse: -
  - geste: 7 | ecran.vu: 7 | cible: bouton (notes sur une pile) › « Autres fichiers Piste par piste, MIDI, FLAC… (passe en mode Pro) » [coupé] | geste.type: touche
    attendu: la ligne « Autres fichiers » lisible en entier | obtenu: les boutons « M4A · pour partager » / « WAV · qualité max » recouvrent sa 2e ligne et son « ? » (image 7)
    symptome: cache | gravite: finition | revu: oui: ecran 12 | banc?: non | promesse: -
  - geste: 13 | ecran.vu: 17 | cible: bouton « WAV · qualité max » [sélectionné] | geste.type: touche
    attendu: le même format qu'en Simple (« M4A · pour partager » [sélectionné], écran 7) | obtenu: en Pro c'est « WAV » qui est choisi : mes fichiers club et streaming sont partis en WAV sans que je le décide
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 43 | ecran.vu: 43 | cible: texte « Trap-20261112-195658.pocketstudio.zip » | geste.type: touche
    attendu: nom de fichier lisible | obtenu: coupé au milieu du mot : « pocketstudio.z » / « ip », « 19 » / « 5558.wav » (image 43)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 38 | banc?: non | promesse: -
  - geste: 21 | ecran.vu: 23 | cible: bouton « Annuler » [coupé] | geste.type: touche
    attendu: le bouton « Annuler » de la fabrication visible | obtenu: coupé sous la barre « Guide » pendant « Fabrication du son · 75 % »
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
