code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf

siege: a | profil: moyen | lang: de | device: tablet8 | font: 100 | mode: simple
mission: tuto-facile (neurofunk, chapitres 9-15) | buts: - - -
promesse: id=- | texte: Nicht in der Reihenfolge: … Das ⋮-Menü eines Teils entfernt ihn | ecran: 38 | tenue: oui | pourquoi: mon Drop 2 mal placé a été signalé, Rückgängig l'a enlevé
promesse: id=- | texte: Tippe ▶, um eine Spur allein zu hören. | ecran: 62 | tenue: non | pourquoi: les boutons ▶ de solo sont cachés sous le panneau du tuto pendant tout le chapitre 14
promesse: id=- | texte: eine Kopie liegt auch in Musik › BenStudio | ecran: 74 | tenue: pas vue | pourquoi: pas allé vérifier le dossier
chapitres:
  - ch 9 « Die Teile verstehen » : compris du premier coup (fini écran 16)
  - ch 10 « Bau deine Teile » : compris du premier coup (fini écran 32)
  - ch 11 « Ordne deine Teile » : hésitation (erreur volontaire Drop 2, message clair, boutons qui sautent après Rückgängig ; fini écran 45)
  - ch 12 « Die Übergänge » : hésitation (consigne mal tournée, Roll/Wirbel ; fini écran 53)
  - ch 13 « Die Effekte » : compris du premier coup (fini écran 60)
  - ch 14 « Der Mix » : compris du premier coup pour les faders, hésitation sur le solo ▶ caché (fini écran 65)
  - ch 15 « Exportiere deinen Track » : compris du premier coup (« Bravo, dein Track ist fertig! » écran 75)
  - mission terminée au chapitre 15, 75 gestes sur 167
hesitations:
  - ecran: 47 | cherchais: le dernier Snare-Schlag « du dernier temps » | regarde: les boutons de page 1-4 au-dessus de la grille
  - ecran: 65 | cherchais: le bouton ▶ pour écouter la basse seule | regarde: les noms de pistes, toucher « Bass » ne fait rien
  - ecran: 39 | cherchais: le bouton « + Break » | regarde: les boutons « + X » qui ont sauté de place après Rückgängig
abandon: jamais
incidents-banc: aucun
defauts:
  - geste: 2 | ecran.vu: 3 | cible: 24 | geste.type: touche
    attendu: la carte des parties lisible (nom + nombre de mesures) | obtenu: chiffres de mesures coupés et « Ende… » tronqué, carte poussée par la bannière jaune du kit
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 4 | banc?: non | promesse: -
  - geste: 3 | ecran.vu: 4 | cible: Beispiel übersprungen, ohne es anzuhören. | geste.type: touche
    attendu: le message disparaît quand j'écoute l'exemple de l'étape 3 | obtenu: le message « übersprungen » reste affiché pendant que l'exemple joue
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 16 | ecran.vu: 17 | cible: Teil „Intro“ erstellen | geste.type: touche
    attendu: des guillemets propres | obtenu: guillemets imbriqués „Teil „Intro“ erstellen“ partout (Build-up, Drop, Break, Ende, Zum Teil „Build-up“ gehen)
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 46 | banc?: non | promesse: -
  - geste: 46 | ecran.vu: 47 | cible: Drück … lange auf den letzten „Snare“-Schlag, und „×4“ unter „Roll“. | geste.type: touche
    attendu: une consigne complète | obtenu: il manque le verbe (« und tippe ×4 ») ; le glossaire dit « Wirbel », le panneau dit « Roll »
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 50 | ecran.vu: 51 | cible: Lösch … die Schläge des letzten Schlags | geste.type: touche
    attendu: comprendre quoi effacer | obtenu: « Schläge des letzten Schlags » ne veut rien dire ; seules les croix dans la grille m'ont aidé
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 38 | ecran.vu: 39 | cible: Am Ende hinzufügen: | geste.type: touche
    attendu: les boutons « + X » restent en place et s'appellent comme dans le tuto (« Hinzufügen ») | obtenu: après Rückgängig les boutons sautent de y≈312 à y≈438 (risque de toucher le mauvais) et l'étiquette diffère du tuto
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 64 | ecran.vu: 65 | cible: Du bist dran: lauter, was man hören soll, leiser, was stört. Tippe ▶, um eine Spur allein zu hören. | geste.type: touche
    attendu: voir les boutons ▶ de solo sur chaque piste | obtenu: les ▶ (et « Normal » de Drums) sont sous le panneau du tuto ; toucher « Bass » ne fait rien ; ils réapparaissent seulement au chapitre 15
    symptome: cache
    gravite: contourne-en-1-gestes | revu: oui: ecran 66 | banc?: non | promesse: -
  - geste: 45 | ecran.vu: 46 | cible: Ende Takt 3/8 | geste.type: touche
    attendu: le compteur suit la nouvelle longueur (16 mesures) | obtenu: la puce affiche encore « /8 » après « Empfohlene Längen setzen »
    symptome: texte-faux
    gravite: finition | revu: non | banc?: son | promesse: -
mots-faux:
  - ecran: 49 | vu: Velocity | attendu: Stärke (le reste de l'appli dit Stärke)
  - ecran: 54 | vu: Reverb | attendu: Hall
defauts-avant:
  - aucun défaut visé dans le brief | pas atteint | ecran: - | rien à constater
fin-tranche: oui (ROUTE ligne 349)
