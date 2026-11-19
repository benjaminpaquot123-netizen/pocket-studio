code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf
siege: a | profil: confirmé | lang: pl | device: tablet8 | font: 130 | mode: pro
mission: tuto-pro (finir chapitres 9-16 du tuto pop) | buts: -
promesse: id=- | texte: Przycisk „Ustaw zalecane długości” robi to za jednym razem | ecran: 55 | tenue: oui | pourquoi: toutes les parties ont pris leur longueur (100 taktów) en un geste
chapitres:
  - ch9 Zrozumieć części : compris du premier coup
  - ch10 Zbuduj swoje części : compris du premier coup
  - ch11 Ułóż swoje części : compris du premier coup
  - ch12 Przejścia : hésitation (panneau replié, message de réussite caché)
  - ch13 Efekty : hésitation (« Zrobię to » a fait le réglage à ma place ; glisser sur la fiche a bougé le pad XY)
  - ch14 Miks : compris du premier coup
  - ch15 Mastering : compris du premier coup
  - ch16 Wyeksportuj swój utwór : compris du premier coup (tuto fini, « Brawo, twój utwór jest gotowy! »)
hesitations:
  - ecran: 59 | cherchais: le retour du tuto après le roll ×4 | regarde: panneau replié, il a fallu toucher ⌃ pour voir « Brawo »
  - ecran: 84 | cherchais: faire moi-même la wysyłka du Werbel | regarde: « Zrobię to » l'a faite seul (Akordy ✓ · Werbel ✓)
  - ecran: 74 | cherchais: la piste Akordy | regarde: la fiche s'appelle « Dla: Pluck », deux noms pour une piste
abandon: jamais
incidents-banc:
defauts:
  - geste: 23 | ecran.vu: 24 | cible: Cofnij | geste.type: touche
    attendu: annuler la création de la partie, le tuto revient à « crée Zwrotka » | obtenu: la partie C · Zwrotka disparaît mais le tuto dit encore « Brawo, udało się! Utworzono część „Zwrotka” »
    symptome: texte-faux | gravite: finition | revu: oui: ecran 25 (Ponów remet la partie) | banc?: non | promesse: -
  - geste: 77 | ecran.vu: 78 | cible: Przestrzeń | geste.type: glisse
    attendu: faire défiler la fiche de la piste Perkusja | obtenu: le doigt a déplacé le pad XY, « Przestrzeń » passe en silence à 100 %
    symptome: perdu | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 102 | ecran.vu: 103 | cible: Filtr Odcięcie | geste.type: retour
    attendu: fermer l'éditeur de courbe et revenir au Miks | obtenu: Retour sort du projet vers l'accueil « Co chcesz zrobić? »
    symptome: mauvais-ecran | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: 90 | ecran.vu: 91 | cible: Przejdź do części „Pre-chorus” | geste.type: touche
    attendu: le haut affiche Pre-chorus | obtenu: l'en-tête affiche « Refren takt 21/100 » (l'éditeur dit ensuite « Kroki części D »)
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 13 | ecran.vu: 14 | cible: Dalej | geste.type: touche
    attendu: passer l'exemple sans éloge | obtenu: « Brawo, udało się! » avec « Przykład pominięty bez słuchania. »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 6 | ecran.vu: 7 | cible: Intro / Zwrotka (bande d'énergie des parties) | geste.type: touche
    attendu: lire les noms des parties | obtenu: quand la consigne fait 2 lignes, noms des parties et pastilles coupés en bas
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 115 | ecran.vu: 116 | cible: 6 części | geste.type: touche
    attendu: 11 parties comme sur l'accueil (« 100 taktów · 3:27 · 11 części ») | obtenu: la fenêtre de partage dit « 6 części »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 54 | ecran.vu: 55 | cible: Gotowe: nowa długość dla B · Intro | geste.type: touche
    attendu: message qui dit que toutes les longueurs sont réglées | obtenu: l'ancien message « nowa długość dla B · Intro » reste affiché
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 60 | vu: bez Akordy | attendu: bez Akordów
  - ecran: 60 | vu: bez Kick, Werbel | attendu: noms déclinés (bez Kicka, Werbla)
  - ecran: 57 | vu: „Roll” | attendu: „Rolki” (nom de l'onglet)
  - ecran: 73 | vu: „Wysyłka na pogłos” | attendu: „Wysyłka pogłos” (nom du réglage)
defauts-avant:
  - -
fin-tranche: oui (ROUTE ligne 724)
