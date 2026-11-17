code testé : 11ca024b94945ffae964263f27e8342aa3770316
joué par : sous-agent neuf

siege: e | profil: produzent-de-pro | lang: de | device: tablette-8 | font: 130 | mode: pro
mission: tuto-pro ch. 8-13 | buts: ch8-12-finis, ch13-commence, mots-allemands
defauts-avant:
  - e-V1: autre | ecran: 85 | Kap. 8-12: jeder Schritt hat einen Knopf, der es wirklich tut (erstellen/gehen/Empfohlene Längen) ; Kap. 13: „Ich mach's“ öffnet nur die Stelle (Reverb-Send bleibt 0 %, Kurve wird nicht gezeichnet)
  - e-V2: corrigé | ecran: 67, 79 | Kreuze/gestrichelte Kästchen nur im Build-up bzw. Drop, in Drop 2 keine
  - e-V3: corrigé | ecran: 62, 77 | Build-up ohne Roll, erstes Becken-Kästchen im Drop wirklich leer
  - e-V4: autre | ecran: 1, 4 | Banner unter dem Schritttext, Teile-Karte sichtbar ; aber während „Kit wird geladen… 0 %“ wird der Schritttext gekürzt („Weiterlesen“)
promesse: id=pro-boutons | texte: Ich mach's / Zeig's mir | ecran: 85 | tenue: non | pourquoi: Kap. 8-12 ok mit Geschafft-Haken, aber in Kap. 13 öffnet „Ich mach's“ nur das Effekt-Blatt, Reverb-Send bleibt 0 %
promesse: id=partie-etape | texte: Noch 1 gestricheltes Kästchen hier | ecran: 79 | tenue: oui | pourquoi: Kästchen/Kreuze nur im richtigen Teil, in Drop 2 Knopf „Zum Teil … gehen“
promesse: id=a-faire | texte: es ist noch leer | ecran: 77 | tenue: oui | pourquoi: erstes Becken-Kästchen leer und gepunktet ; Build-up hatte keinen Roll (ecran 62)
promesse: id=kit-banniere | texte: Kit Techno laden (4 MB) | ecran: 1 | tenue: oui | pourquoi: Banner unter dem Schritttext, Teile-Karte bleibt sichtbar
oeil-neuf: q1 | ecran: 89 | image: 89 | Karten-Knöpfe, Aufnehmen, Was spielt, Regler, Details, Tuto-Leiste klar ; „Lesen ⌃“ und Kapitel-Liste gut erkennbar
oeil-neuf: q6 | ecran: 89 | image: 89 | nach „Ich mach's“ kein Hinweis, was als Nächstes zu tippen ist (Karte nur umrandet), Kurve nicht gezeichnet
oeil-neuf: q9 | ecran: 89 | image: 89 | Schritttext auf 2 Zeilen gekürzt („„…“), Kopf zeigt „Drop 2 Takt 81/128“ statt Drop
hesitations:
  - ecran: 69 | cherchais: Kreuze löschen | regarde: Raster noch im Modus „Rolls“ von Schritt 1, Tippen setzte ×2
  - ecran: 84 | cherchais: ob „Ich mach's“ den Send schon gedreht hat | regarde: 0 %, selbst geschoben
abandon: jamais
incidents-banc:
  - geste: 53 | attente: 0 | vu: „ecris 12“ nach ✕ füllte das Feld nicht, erst nach Tippen ins Feld (#55) ; banc? clavier
  - geste: - | attente: 0 | vu: outils hors human-act : Read (consignes + images de mon siège), grep/sed/head pour filtrer la sortie du banc
defauts:
  - geste: 84 | ecran.vu: 85 | cible: bouton (doigt qui touche) «   Ich mach's » | geste.type: touche
    attendu: Reverb-Send des Clap wird aufgedreht, Haken Geschafft | obtenu: nur Effekt-Blatt geöffnet, Send 0 %, selbst schieben nötig
    symptome: incompris | gravite: contourne-en-2-gestes | revu: oui: ecran 89 (Schritt 2 gleich: nur Karte „Kurven“) | banc?: non | promesse: pro-boutons
  - geste: 69 | ecran.vu: 71 | cible: bouton [état « Schlag, Stärke 100 % »] (rotes Kreuz, Build-up) | geste.type: touche
    attendu: Schlag wird gelöscht | obtenu: Editor noch im Tab „Rolls“ (aus Schritt 1), Tippen setzt ×2-Rolls ; erst nach „Schläge“ (#72) löschen
    symptome: incompris | gravite: contourne-en-1-gestes | revu: oui: ecran 75 | banc?: non | promesse: partie-etape
  - geste: 57 | ecran.vu: 60 | cible: texte « Erledigt: neue Länge für E · Break » | geste.type: touche
    attendu: Meldung verschwindet | obtenu: deckt Zeile F · Build-up, bleibt auch nach „Empfohlene Längen setzen“ und auf Sounds stehen
    symptome: cache | gravite: finition | revu: oui: ecran 59 | banc?: temps | promesse: -
  - geste: 43 | ecran.vu: 43 | cible: texte « B · Intro × 1 · 8 Takte » [coupé] | geste.type: touche
    attendu: neue Zeile sichtbar | obtenu: Zeile unter der oberen Leiste, Schritttext gekürzt mit großer Lücke
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 60 | ecran.vu: 76 | cible: icône « Drop 2 Takt 81/128 » ▾ | geste.type: touche
    attendu: Kopf zeigt den bearbeiteten Teil (Build-up/Drop) | obtenu: „Intro Takt 1/128“ bzw. „Drop 2 Takt 81/128“, auch auf ecran 89
    symptome: texte-faux | gravite: finition | revu: oui: ecran 89 | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 4 | cible: texte « Kit wird geladen… 0 % » | geste.type: touche
    attendu: Schritttext bleibt lesbar | obtenu: Text [coupé] mit „Weiterlesen“, nach Laden keine Bestätigung
    symptome: texte-coupe | gravite: finition | revu: non | banc?: temps | promesse: kit-banniere
mots-faux:
  - ecran: 26 | vu: Tippe auf „Teil „Intro“ erstellen“ | attendu: Tippe auf „Teil ‚Intro' erstellen“
  - ecran: 33 | vu: der ganze Groove spielt darin. Darin: | attendu: der ganze Groove spielt darin:
  - ecran: 15 | vu: dem „Techno · Rim“ | attendu: der Rimshot
  - ecran: 59 | vu: auf den letzten Schlag vor der letzten Zählzeit | attendu: auf den letzten Schlag vor Zählzeit 4
  - ecran: 64 | vu: dann wieder auf einen. | attendu: dann wieder auf ×1.
  - ecran: 80 | vu: gestrichelt / gepunktet | attendu: ein Wort (gestrichelt)
  - ecran: 30 | vu: Riff (Schritt) / Melodie (Ergebnis) | attendu: ein Wort
  - ecran: 49 | vu: wann du willst | attendu: wenn du so weit bist
  - ecran: 84 | vu: Akkorde ✓ · Snare: danach | attendu: unklar, was gemeint ist
  - ecran: 88 | vu: Automatisiere den „Cutoff“ … „Filter (Sweep)“ | attendu: ein Name für denselben Regler
fin
