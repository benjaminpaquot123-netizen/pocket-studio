code testé : 7455749a516f5055df3dc5985ec8fc1061fdb25e
joué par : sous-agent neuf

siege: a | profil: confirme-niveau-3 | lang: nl | device: phone360 | font: 100 | mode: pro
mission: tuto liquid_dnb, hoofdstukken 12 tot 17 afmaken (vanaf 12) | buts: - (geen ids in de brief)
promesse: id=- | texte: Goed gedaan, je nummer is klaar! Je kunt alles opnieuw beluisteren en blijven aanpassen. | ecran: 101 | tenue: oui | pourquoi: 17/17 klaar, WAV Club −8,0 LUFS geëxporteerd en gedeeld (Drive)
chapitres:
  - 12 Zet je delen op volgorde : compris du premier coup (petit détour onglet Tijdlijn)
  - 13 De overgangen : compris du premier coup
  - 14 De effecten : hésitation (send Snare introuvable dans Mix, résolu par « Ik doe het »)
  - 15 De mix : compris du premier coup
  - 16 De mastering : hésitation (choix Doelvolume cachés sous la carte)
  - 17 Exporteer je nummer : compris du premier coup
hesitations:
  - ecran: 43 | cherchais: het kanaal van de pad ‘Snare’ in Mix | regarde: Mix heeft alleen sporen (Drums, Bas, Sub, Akkoorden…), geen pad-kanaal; Drums-blad doorzocht
  - ecran: 83 | cherchais: de keuze ‘Club’ onder Doelvolume | regarde: alleen « Auto (stijl) » zichtbaar, de rest verborgen onder de tuto-kaart
  - ecran: 16 | cherchais: lengte van Intro zetten (« Tik op een deel ») | regarde: tik op een deel speelt het af; Lengte zit onder ⋮
abandon: jamais
incidents-banc:
  - geste: 96 | attente: ~2 attends | vu: export blijft op 25 % hangen tot het deelvenster verschijnt (banc: duur export niet representatief)
defauts:
  - geste: 42 | ecran.vu: 43 | cible: Akkoorden ✓ · Snare: hier instellen | geste.type: touche
    attendu: « hier » brengt me naar de galm-send van de Snare-pad | obtenu: « hier » staat naast de Akkoorden-schuif; Mix heeft geen Snare-kanaal, 13 gestes gezocht (Drums-blad, scrollen) tot « Ik doe het » (#55) « Effecten · pad 3 (Snare) » opende
    symptome: incompris | gravite: contourne-en-13-gestes | revu: oui: ecran 56 | banc?: non | promesse: -
  - geste: 49 | ecran.vu: 50 | cible: Houd je vinger op een sfeer om hem te ho | geste.type: glisse
    attendu: het effectenblad van Drums scrollt omhoog | obtenu: de verticale veeg begon op de XY-pad en zette Ruimte (galm) op 100 %; dubbeltik nodig om terug te zetten
    symptome: perdu | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 82 | ecran.vu: 83 | cible: bouton « Auto (stijl) · −9 LUFS » | geste.type: touche
    attendu: de vier Doelvolume-keuzes (Club, Streaming, Apple Music, Vrij) zien die de stap vraagt | obtenu: alleen Auto zichtbaar, de rest onder de tuto-kaart; pas na inklappen + veeg (#84) verscheen ‘Club’
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 85 | banc?: non | promesse: -
  - geste: 90 | ecran.vu: 92 | cible: champ « Naam van het nummer (en van het project) » contenu «  » | geste.type: ecris
    attendu: na ✕ (wissen) direct de nieuwe naam typen | obtenu: het veld verliest de focus, er wordt niets getypt (klavier weg); opnieuw op het veld tikken nodig. Zelfde gedrag in het ‘maten’-veld (#15/#16)
    symptome: perdu | gravite: contourne-en-1-gestes | revu: oui: ecran 17 | banc?: clavier | promesse: -
  - geste: 21 | ecran.vu: 22 | cible: Klaar: nieuwe lengte voor B · Intro | geste.type: touche
    attendu: melding dat alle delen hun aanbevolen lengte kregen | obtenu: oude melding over alleen Intro blijft staan na « Aanbevolen lengtes invullen »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 3 | ecran.vu: 4 | cible: + B · Intro | geste.type: touche
    attendu: deel toevoegen en de knoppenrij blijft zichtbaar voor de volgende | obtenu: scherm springt naar Tijdlijn, knoppen weg; terug naar ‘Onderdelen’ + scrollen
    symptome: mauvais-ecran | gravite: contourne-en-2-gestes | revu: oui: ecran 6 | banc?: non | promesse: -
  - geste: 36 | ecran.vu: 36 | cible: Nog 1 gestippeld vakje hier | geste.type: touche
    attendu: banner volledig leesbaar met knop Wissen | obtenu: banner half bedekt, « Wissen » afgesneden; welk vakje/maat pas na tikken
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 66 | ecran.vu: 67 | cible: Lijn: tik het begin en dan het einde (of | geste.type: touche
    attendu: tweede tik zet het eindpunt van de lijn | obtenu: pagina scrolde tussen beide tikken, tweede tik viel naast de curve (stap toch geteld)
    symptome: perdu | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 75 | ecran.vu: 76 | cible: de Sub en de piano vooraan… de bel en de pad erachter | geste.type: touche
    attendu: uitleg die klopt met stap 1 | obtenu: stap 1 liet de piano (Akkoorden) zakken, stap 3 zegt piano « vooraan »; « pad » noemt geen enkel spoor
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 80 | ecran.vu: 81 | cible: Kit Liquid DnB downloaden (2 MB) | geste.type: touche
    attendu: aanbod één keer tonen | obtenu: banner staat op elke stap van 12 tot 17 (niet aangeraakt) en neemt plaats van de instructie
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 81 | vu: oversteuurt | attendu: overstuurt
  - ecran: 30 | vu: tik op elk rood kruisje | attendu: doorgestreept (zoals het scherm zegt)
defauts-avant:
  - (geen defecten opgegeven in de brief)
fin-tranche: oui (ROUTE ligne 645)
