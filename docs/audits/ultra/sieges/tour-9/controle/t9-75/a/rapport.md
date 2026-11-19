code testé : 2b18313c4
joué par : sous-agent neuf

siege: a | profil: confirmé | lang: nl | device: tablet8 | font: 100 | mode: pro
mission: tuto-pro (uk_garage, hoofdstukken 1-4) | buts: hoofdstuk-1, hoofdstuk-2, hoofdstuk-3/4
promesse: id=- | texte: - | ecran: - | tenue: pas vue | pourquoi: aucune promesse fournie
chapitres:
  - 1 Tempo en toonsoort : compris du premier coup
  - 2 De 2-stepbeat : bloqué (stap 9 overgeslagen, hoofdstuk blijft « niet klaar »)
  - 3 De bas : compris du premier coup
  - 4 De akkoorden : hésitation (stap 3, grote knop deed niets)
hesitations:
  - ecran: 78-93 | cherchais: ‘Verschuiving van slag’ van de pad Rim | regarde: Padinstellingen, lang drukken, Pad-effecten, Laat zien
  - ecran: 26 | cherchais: de rim | regarde: raster toont « Snare »
  - ecran: 134 | cherchais: waarom kopiëren niets deed | regarde: kleine knop Kopiëren in de balk
abandon: jamais (stap 9 van hoofdstuk 2 overgeslagen met 2× Volgende)
incidents-banc:
  - geen
defauts:
  - geste: #75-93 | ecran.vu: 78 | cible: « zet op de pad ‘Rim’ de ‘Verschuiving van slag’ » | geste.type: touche
    attendu: een regelaar voor verschuiving vinden | obtenu: nergens te vinden (instellingen, lang drukken, effecten, Laat zien)
    symptome: stap onhaalbaar, hoofdstuk 2 blijft onaf (« stap 9 blijft over »)
    gravite: bloquant | revu: oui: ecran 84, 88, 93 | banc?: non | promesse: -
  - geste: #3 | ecran.vu: 3 | cible: « Kit UK Garage downloaden (3 MB) » | geste.type: touche
    attendu: kit geladen | obtenu: « Kit downloaden… 0 % » verdwijnt zonder bericht, kit blijft « Studio »
    symptome: hoofdstuk 2 zegt « de kit van de stijl staat klaar », padnamen kloppen niet met de tekst
    gravite: grave | revu: oui: ecran 18 | banc?: fichier/temps (onzeker) | promesse: -
  - geste: #26 | ecran.vu: 26 | cible: « Nu jij: de rim op 2 en 4 » | geste.type: touche
    attendu: raster van de Rim | obtenu: « Maat 1/2 · Snare »; ook stap 7 « shaker » toont « Rim » (ecran 70)
    symptome: tekst en pad lopen door elkaar, ik weet niet welke pad ik bewerk
    gravite: finition | revu: oui: ecran 70 | banc?: non | promesse: -
  - geste: #27-30 | ecran.vu: 36 | cible: « met het zachte slagje » | geste.type: touche
    attendu: zachte slag | obtenu: alle slagen op Kracht 100%
    symptome: « zacht » in de tekst, niets zachts in het resultaat
    gravite: finition | revu: oui: ecran 36 | banc?: non | promesse: -
  - geste: #92 | ecran.vu: 92 | cible: « ▾ Waarom? » (stap 9) | geste.type: touche
    attendu: uitleg over de verschuiving | obtenu: tekst over de kick die tel 3 overslaat
    symptome: uitleg hoort bij een andere stap
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #101-103 | ecran.vu: 103 | cible: « Luister naar het voorbeeld » | geste.type: touche
    attendu: geluisterd erkend | obtenu: « Voorbeeld overgeslagen zonder te luisteren. » na enkele seconden luisteren
    symptome: verwijt terwijl ik wel luisterde (wel gestopt voor het einde)
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: #117 | ecran.vu: 119 | cible: « Bas een octaaf lager kopiëren » | geste.type: touche
    attendu: hele baslijn op Sub | obtenu: octaafnoot ontbreekt, « Nog 1 gestippelde noot » terwijl hoofdstuk « Klaar » zegt
    symptome: kopie onvolledig en tegenstrijdige meldingen
    gravite: finition | revu: oui: ecran 119 | banc?: non | promesse: -
  - geste: #133 | ecran.vu: 134 | cible: « Kopiëren naar de volgende maten » | geste.type: touche
    attendu: akkoorden gekopieerd, stap klaar | obtenu: niets; pas de kleine knop « Kopiëren » (balk) werkte
    symptome: de grote knop van de les doet niets
    gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 14 | vu: « De kleur van het nummer is gekozen » | attendu: toonsoort

fin-tranche: non (dernier step=pad_end, done=18, chapters=1,3,4,7)
BENCH-VERDICT: #135 BENCH-VERDICT ok where=seat.fin
