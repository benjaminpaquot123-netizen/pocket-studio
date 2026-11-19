code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf

siege: a | profil: pro | lang: id | device: tablet8 | font: 200 | mode: pro
mission: tuto-pro (retro_techno, chapitres 7 à 13) | buts: - (pas de liste donnée par le brief)
chapitres:
  - ch7 Bangun bagian-bagianmu : compris du premier coup
  - ch8 Susun bagian-bagianmu : hésitation (boutons + G / + H cachés sous le panneau, longueur Mini-break par ⋮)
  - ch9 Transisi : compris du premier coup
  - ch10 Efek : hésitation (Send reverb du pad Clap introuvable dans Mix, trait de courbe qui fait défiler la page)
  - ch11 Mix : compris du premier coup
  - ch12 Mastering : compris du premier coup
  - ch13 Ekspor lagumu : compris du premier coup (WAV exporté, fin du tuto atteinte écran 89)
promesse: - (aucune donnée par le brief)
hesitations:
  - ecran: 18 | cherchais: + G · Drop 2 et + H · Akhir | regarde: replié le panneau du tuto pour les voir
  - ecran: 50 | cherchais: Send reverb du pad Clap | regarde: fiche de la tranche Drum, puis « Tunjukkan »
  - ecran: 31 | cherchais: taper 2 dans le champ birama | regarde: le ✕ vide le champ mais lui retire le focus, re-touché le champ
abandon: jamais
incidents-banc:
  - geste: 85 | attente: 40 | vu: export WAV bloqué à « 20% » pendant ~40 s puis feuille de partage (banc, export non représentatif)
defauts:
  - geste: 17 | ecran.vu: 18 | cible: Masih kurang: Intro 2, Drop, Mini-break, Drop, Break, Drop 2, Akhir. | geste.type: touche
    attendu: voir tous les boutons « + » de la liste des parties | obtenu: « + G · Drop 2 » et « + H · Akhir » coupés sous le panneau du tuto
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 19 | banc?: non | promesse: -
  - geste: 46 | ecran.vu: 47 | cible: bouton « 1 » [coupé, état « pukulan, kekuatan 100 % »] @ 90,575 | geste.type: touche
    attendu: voir la grille de pas pour mettre le coup du Drop | obtenu: seule une fine tranche de la 1re rangée visible, le reste sous le panneau
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 49 | ecran.vu: 50 | cible: Giliranmu: di Mix, naikkan “Send reverb” di channel pad “Clap”. | geste.type: touche
    attendu: une tranche « Clap » avec Send reverb dans Mix | obtenu: Mix ne montre que les pistes (Drum…), la fiche Drum n'a pas de Send reverb ; trouvé seulement via « Tunjukkan »
    symptome: sans-aide | gravite: contourne-en-2-gestes | revu: oui: ecran 53 | banc?: non | promesse: -
  - geste: 53 | ecran.vu: 54 | cible: Nah. Aku kembalikan seperti semula… | geste.type: touche
    attendu: savoir si mon réglage à 67 % est gardé | obtenu: la démo dit « je remets comme avant » alors que j'avais réglé moi-même ; l'étape passe quand même Berhasil
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 64 | ecran.vu: 66 | cible: Gambar dengan jarimu; ketuk dua kali untuk menghapus titik. | geste.type: glisse
    attendu: dessiner la courbe de Cutoff | obtenu: le trait fait défiler la page (~110 px), courbe fausse ; redessinée en 2 traits
    symptome: perdu | gravite: contourne-en-2-gestes | revu: oui: ecran 68 | banc?: non | promesse: -
  - geste: 65 | ecran.vu: 66 | cible: texte « 18,0 kHz » @ 81,616 | geste.type: glisse
    attendu: lire les axes de la courbe | obtenu: à 200 % les libellés sont rognés à l'image (« 18,0 », « 40 H », « ket »)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 9 | ecran.vu: 10 | cible: Bagian “Mini-break” dibuat (salinan Groove): tanpa Kick studio, Snare, Clap, Rim. | geste.type: touche
    attendu: les mêmes noms que la consigne (« hoover », « zap ») | obtenu: la confirmation parle de Melodi et Rim ; il faut deviner que zap = Rim, hoover = Melodi (aussi écran 14)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 14 | banc?: non | promesse: -
  - geste: 15 | ecran.vu: 16 | cible: Bagian “Akhir” dibuat (salinan Groove): tanpa Snare, Clap, Bass, Melodi. | geste.type: touche
    attendu: une fin différente de l'intro (909 seul) | obtenu: exactement le même contenu que l'Intro (écran 2)
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 37 | ecran.vu: 38 | cible: Selesai: panjang baru untuk E · Mini-break | geste.type: touche
    attendu: un écran propre au début du chapitre 9 | obtenu: le message du chapitre 8 reste affiché par-dessus la liste
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 91 | ecran.vu: 92 | cible: −14,4 LUFS · puncak −9,5 dBTP | geste.type: touche
    attendu: le même chiffre que « Volume: −8,0 » juste au-dessus et que le bilan d'export (−8,0 LUFS, −3,6 dBTP) | obtenu: « detail » affiche −14,4 LUFS, sans dire que c'est avant mastering ; et pas de « trek terpisah » (promis au ch13 étape 1) dans la feuille de partage
    symptome: incompris | gravite: finition | revu: oui: ecran 94 | banc?: non | promesse: -
mots-faux:
  - ecran: 10 | vu: Rim / Melodi | attendu: zap / hoover (mots de la consigne)
defauts-avant:
  - sans objet | pas atteint | ecran: - | aucun défaut visé donné par le brief
autres-vus: panneau replié (⌄) qui se rouvre seul à l'étape suivante (écran 6) ; bannière « Unduh kit Techno retro (6 MB) » répétée à chaque étape jusqu'à « Nanti » (écran 9) ; noms des tranches Mix rognés en haut (écran 70) ; titre « Proy… » tronqué (écran 1).
gestes: 96 sur 153 (fin du tuto au geste 88)

fin-tranche: oui (ROUTE ligne 564)
