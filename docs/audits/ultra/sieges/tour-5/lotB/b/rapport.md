siege: b | profil: t5-Bb musicien-polonais-lit-chaque-aide | lang: pl | device: phone | font: 100 | mode: pro
mission: libre | buts: but1, but2, but3
promesse: id=t5-Bb-1 | texte: « Jedno z pól A, B, C… w Struktura › Części: pętla perkusji, basu i nut. » | ecran: 37 | tenue: non | pourquoi: en mode Prosty il n'y a ni « Struktura » ni « Części » (onglets : Mój utwór, Rytm, Klawiatura, Miks, Wyślij) ; l'aide « Pady live » cite aussi « 4 strony po 16 padów w Pad Live » (ni pages ni bouton « Pad Live »)
promesse: id=t5-Bb-2 | texte: « Twoja kolej: dotknij „Dźwięk melodii”. » | ecran: 11 | tenue: oui | pourquoi: Słuchaj, Zmień dźwięk, Nagraj melodię n'ont avancé qu'après mon geste ; seul doute : passage en Pro, 27 % → 29 % sans avoir gardé le kick
promesse: id=t5-Bb-3 | texte: « „?” na końcu paska pod górnym paskiem podpisuje ekran: jedno dotknięcie pokazuje, drugie ukrywa. » | ecran: 42 | tenue: non | pourquoi: « Graj część » et « Zatrzymaj tę ścieżkę » posées l'une sur l'autre tout en haut, loin de leurs boutons ; « Więcej opcji » idem écran 12
promesse: id=t5-Bb-4 | texte: (pas trouvé de « Strony i paczki » / pages et packs) | ecran: - | tenue: pas vue | pourquoi: cherché dans Pomoc, Live, Pady, Biblioteka dźwięków (Prosty puis Pro) ; seule phrase vue sur « Anuluj » : « „Anuluj” usuwa je od razu; poprzedni utwór zostaje w „Wersje”. » (écran 37, fiche « Nagraj mój występ »)
hesitations:
  - ecran: 14 | cherchais: le son de ma mélodie (lien du Guide « Dźwięk melodii ») | regarde: « Dźwięk: Synth » alors que mes pistes sont Perkusja, Acid 303, Riser
  - ecran: 36 | cherchais: « pages et packs » dans l'aide | regarde: glossaire très long, défilé 4 fois sans le trouver
  - ecran: 75 | cherchais: pourquoi le Guide a sauté l'étape Kick | regarde: « Przewodnik · Długość i siatka 8 taktów 29 % » juste après « Przełącz na tryb Pro »
abandon: jamais
incidents-banc:
defauts:
  - geste: 41 | ecran.vu: 42 | cible: texte « Zatrzymaj tę ścieżkę » @ 99,13 | geste.type: touche
    attendu: chaque étiquette « ? » posée sur son bouton (les ■ de la ligne du bas, les cases de la grille) | obtenu: « Graj część » et « Zatrzymaj tę ścieżkę » empilées au bord haut, coupées, invisibles sur la capture, sur le bouton Stop
    symptome: cache | gravite: finition | revu: oui: ecran 12 | banc?: non | promesse: t5-Bb-3
  - geste: 11 | ecran.vu: 12 | cible: texte « Więcej opcji » @ 59,13 | geste.type: touche
    attendu: étiquette sur le bouton ⋮ en haut à droite | obtenu: étiquette jetée en haut à gauche hors de l'écran ; le ⋮ reste sans étiquette ; « Tempo » chevauche « Graj, nagrywaj »
    symptome: cache | gravite: finition | revu: oui: ecran 42 | banc?: non | promesse: t5-Bb-3
  - geste: 11 | ecran.vu: 12 | cible: texte « Przewodnik: na jakim etapie jest twój utwór » @ 297,617 | geste.type: touche
    attendu: étiquette lisible en entier | obtenu: « Przewodnik: na jakim etapie jest twój ut… » coupée
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t5-Bb-3
  - geste: 13 | ecran.vu: 20 | cible: bouton « ↗ Dźwięk melodii » (nom « Zobacz ustawienie: Dźwięk melodii ») @ 294,909 | geste.type: touche
    attendu: ouvrir le son de la piste mélodie (Acid 303) et cocher l'étape « Zmień dźwięk » | obtenu: feuille « Dźwięk: Synth » (aucune piste Synth) ; mon choix Bas reese est parti sur « Riser » (écran 20 : « Riser Bas reese ») et l'étape est restée ouverte
    symptome: mauvais-ecran | gravite: contourne-en-2-gestes | revu: oui: ecran 18 | banc?: non | promesse: -
  - geste: 37 | ecran.vu: 37 | cible: texte « Jedno z pól A, B, C… w Struktura › Części: pętla perkusji, basu i nut. » | geste.type: glisse
    attendu: l'aide cite des boutons du mode où je suis | obtenu: « Struktura › Części » n'existe qu'en Pro, rien sous ce nom en Prosty
    symptome: texte-faux | gravite: finition | revu: oui: ecran 75 | banc?: non | promesse: t5-Bb-1
  - geste: 50 | ecran.vu: 51 | cible: texte « Dźwięki (…): 4 strony po 16 padów w Pad Live. » | geste.type: touche
    attendu: retrouver « Pad Live » et ses 4 pages | obtenu: le bouton s'appelle « Pady », 8 pads au choix 4–16, aucune page (Prosty et Pro)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 79 | banc?: non | promesse: t5-Bb-1
  - geste: 54 | ecran.vu: 55 | cible: texte « Pady nagrane, M nie: do przerwy użyj ■. » | geste.type: touche
    attendu: une consigne claire pendant l'enregistrement live | obtenu: « M nie » incompréhensible pour un Polonais (abréviation non expliquée)
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 58 | ecran.vu: 65 | cible: touchable « Acid techno OTWARTE przed chwilą · 138 BPM · 572 takty · 16:35 · 33 części » | geste.type: touche
    attendu: après « Anuluj », le morceau comme avant (écran 9 : « 176 taktów · 5:06 ») | obtenu: 572 takty · 16:35, « Ponów » allumé ; je ne sais pas quel geste l'a allongé
    symptome: incompris | gravite: finition | revu: non | banc?: son | promesse: -
  - geste: 30 | ecran.vu: 31 | cible: texte « To łańcuch „Chrupiący”, jeden na cały utwór. » | geste.type: touche
    attendu: la fiche dit une seule chose sur les effets | obtenu: même fiche : « Brak efektu: dźwięk przechodzi bez zmian. » puis « To łańcuch „Chrupiący” »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 2 | vu: 5 karty | attendu: 5 kart
  - ecran: 1 | vu: czekają na twoje | attendu: czekają na ciebie
  - ecran: 41 | vu: Co to jest „„Start w rytmie” i pola”? | attendu: Co to jest „Start w rytmie” i pola?
