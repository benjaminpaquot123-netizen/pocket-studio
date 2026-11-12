siege: d | profil: t6-Ad | lang: de | device: phone | font: 200 | mode: simple
mission: libre | buts: son, trace, partage
promesse: id=P022 | texte: ▶ joue tout le morceau ; ■ « der Song » arrête | ecran: 8 | tenue: oui | pourquoi: le ▶/■ « der Song » lance (écran 8) puis arrête (écran 11) le morceau, vu sur l'onglet Mein Song
promesse: id=P025 | texte: Einfach: « 5 Tabs, große Knöpfe und Alltagswörter » | ecran: 8 | tenue: non | pourquoi: la barre du bas montre 5 onglets (Mein Song, Rhythmus, Tastatur, Mix, Teilen) au lieu de quatre
promesse: id=P118 | texte: ? au bout du bandeau (Beschriftungen ausblenden) | ecran: 9 | tenue: oui | pourquoi: un toucher affiche les étiquettes (9), « Beschriftungen ausblenden » les cache (10)
hesitations:
  - ecran: 7 | cherchais: comment valider le style choisi | regarde: texte coupé, bouton en bas après défilement
abandon: jamais
incidents-banc:
defauts:
  - geste: #7 | ecran.vu: 8 | cible: onglet (curseurs) « Mix » | geste.type: touche
    attendu: Mode simple ne garde que quatre onglets (Mein Song, Rhythmus, Tastatur, Teilen) | obtenu: cinq onglets dont « Mix »
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: P025
  - geste: #5 | ecran.vu: 7 | cible: bouton (lune) « Ruhig (Chill, Lo-Fi) 6 Stile » ⌃ [coupé, état « offen »] | geste.type: glisse
    attendu: bouton de validation visible sans défiler (police 200) | obtenu: « Diesen Stil wählen und weiter » seulement après défilement, textes coupés
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
