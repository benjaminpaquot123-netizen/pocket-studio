code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf

siege: a | profil: debutant-motive | lang: tr | device: phone | font: 200 | mode: simple
mission: tuto-facile (tuto reggaeton chapitres 1-7) | buts: -
promesse: id=- | texte: « Bravo, bölüm bitti! » | ecran: 122 | tenue: oui | pourquoi: les 7 chapitres finis, 114 gestes sur 153
chapitres: ch1 Parçanın hızı ve rengi = compris du premier coup ; ch2 Kick = compris du premier coup ; ch3 Dembow = hésitation (mesure 2) ; ch4 Hi-hat'ler ve perküsyon = hésitation (▶ et « Tümünü doldur ») ; ch5 808 bas = hésitation (grille cachée, copier en 2 fois) ; ch6 Akorlar = hésitation (copier en 2 fois) ; ch7 Riff = hésitation (5e note hors de l'écran)
hesitations:
  - ecran: 43 | cherchais: comment aller à la mesure 2 | regarde: l'indice « Ölçü 2 · 4 kutu → » à moitié caché
  - ecran: 70 | cherchais: où sont les notes à pointillés du 808 | regarde: le panneau du tuto qui couvre la grille
  - ecran: 109 | cherchais: la 5e note de la mesure 1 du riff | regarde: 4 notes pleines, aucune 5e visible
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: une redirection mal écrite par le testeur a créé un fichier vide hors du siège (/home/user/tmp_null), effacé tout de suite ; aucun effet sur l'appli
defauts:
  - geste: 52 | ecran.vu: 52 | cible: tümü | geste.type: touche
    attendu: le tuto dit de toucher « Tümünü doldur » pour la güira, je pensais remplir juste ce qu'il faut | obtenu: les 2 mesures pleines, « Burada fazladan vuruş (üstü çizili): 4 », 8 cases à enlever une à une
    symptome: incompris | gravite: contourne-en-9-gestes | revu: non | banc?: non | promesse: -
  - geste: 104 | ecran.vu: 109 | cible: Sıra sende: 1. ölçünün 5 noktalı notasına dokun. Oku | geste.type: touche
    attendu: voir les 5 notes à pointillés de la mesure 1 | obtenu: 4 visibles seulement, la 5e est sous les lignes visibles ; glisser la grille n'aide pas, trouvée seulement avec « Ben yaparım »
    symptome: cache | gravite: contourne-en-10-gestes | revu: non | banc?: non | promesse: -
  - geste: 43 | ecran.vu: 44 | cible: ▶ | geste.type: touche
    attendu: passer à la mesure 2 | obtenu: tous les coups de hi-hat décalés d'une case (« üstü çizili: kaldır »), il a fallu Geri al
    symptome: incompris | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: 69 | ecran.vu: 70 | cible: 808 kanalını oluştur | geste.type: touche
    attendu: voir la grille du 808 avec les notes à toucher | obtenu: le panneau du tuto couvre la grille ; ⌃ l'agrandit au lieu de le replier, il faut glisser
    symptome: cache | gravite: contourne-en-3-gestes | revu: non | banc?: non | promesse: -
  - geste: 92 | ecran.vu: 93 | cible: Sonraki ölçülere kopyala | geste.type: touche
    attendu: les mesures copiées au premier toucher | obtenu: rien, même après 3 s ; le 2e toucher marche (pareil au chapitre 5, geste 78 ; au chapitre 7 le 1er toucher a marché)
    symptome: rien | gravite: contourne-en-1-gestes | revu: oui: ecran 94 | banc?: non | promesse: -
  - geste: 45 | ecran.vu: 43 | cible: Ölçü 2 · 4 kutu → | geste.type: touche
    attendu: un sélecteur de mesure bien visible | obtenu: seul l'indice à moitié caché sous l'en-tête permet d'y aller
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 70 | ecran.vu: 70 | cible: Sıra sende: 1. ölçünün 4 noktalı notasına dokun | geste.type: touche
    attendu: un message clair | obtenu: en même temps « Dokunulacak 1 noktalı nota daha var », je ne sais plus combien il en reste
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 97 | ecran.vu: 98 | cible: İleri | geste.type: touche
    attendu: passer, j'avais écouté l'exemple juste avant (geste 96) | obtenu: « Örnek dinlenmeden geçildi. » comme si je n'avais pas écouté
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 106 | ecran.vu: 107 | cible: Göster | geste.type: touche
    attendu: qu'on me montre la note qui manque | obtenu: mes notes déjà posées redeviennent en pointillés et la démo recommence tout lentement
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 8 | ecran.vu: 8 | cible: Reggaeton · Hi-hat L 30 | geste.type: touche
    attendu: des noms de pads en turc, lisibles | obtenu: « Stüdyo büyük davulu FX », « Zil FX R 40 » : mélange de langues et codes L 30 / R 40 incompris
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 52 | vu: tümü (bouton) | attendu: Tümünü doldur (mot du tuto)
  - ecran: 52 | vu: güira (tuto) | attendu: un nom de pad visible à l'écran
  - ecran: 70 | vu: 808 808 | attendu: 808
defauts-avant:
  - n/a | pas atteint | ecran: - | le brief ne listait aucun défaut visé

fin-tranche: oui (ROUTE ligne 685)
BENCH-VERDICT: #122 BENCH-VERDICT ok where=seat.fin
