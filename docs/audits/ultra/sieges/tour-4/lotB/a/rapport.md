siege: a | profil: Ingénieur du son qui mixe (volumes, panoramiques, envois, EQ, compresseur, master) | lang: fr | device: phone | font: 100 | mode: pro
mission: libre | buts atteints: 2 sur 3
hesitations:
  - ecran: 4 | cherchais: le panoramique et les envois sur la tranche de console | regarde: les tranches du Mix (fader, muet, solo seulement), trouvés dans la fiche piste puis tout en bas (écran 34)
  - ecran: 9 | cherchais: l'égaliseur dans la liste d'effets | regarde: rangé sous « Distorsion & couleur », pas sous « Dynamique » ni une rubrique EQ
  - ecran: 37 | cherchais: le niveau de retour de la réverbe commune | regarde: section « RÉVERBE ET ÉCHO » en bas du Mix : Taille, Amorti, Largeur, Pré-délai, mais aucun volume de retour
promesse: Le Mix et les effets font ce qu'ils annoncent, chaque réglage a un nom, une unité, et s'entend | tenue: en partie | pourquoi: noms et unités presque partout (Hz, dB, Q, ms, LUFS), mais les indicateurs (réduction, LUFS, analyseur, vumètres) ne bougent pas avec les réglages et « Ce qui joue » ignore le muet
surprise: une ambiance toute prête (« Punch ») remplace sans prévenir l'EQ réglé à la main (Annuler le rattrape)
abandon: jamais
defauts:
  - ecran: 26 | cible: « Réduction » du Compresseur (master puis piste, écran 82) | geste: glisse
    attendu: seuil -36 dB, ratio 14,6:1, musique en lecture : la réduction de gain bouge | obtenu: reste « 0 dB » en permanence
    symptome: promesse | gravite: moyen | bloque: non
  - ecran: 49 | cible: « Volume −14,0 · comme la musique en streaming » / « Maintenant −13,6 » | geste: glisse
    attendu: Volume général à +6,0 dB puis cible « Club −8 LUFS » (« la sortie monte doucement ») : la mesure LUFS change | obtenu: reste −14,0 LUFS après 10 s
    symptome: promesse | gravite: moyen | bloque: non
  - ecran: 43 | cible: « Analyseur de spectre » | geste: touche
    attendu: une courbe Graves · Médiums · Aigus pendant la lecture | obtenu: cadre vide avec la seule ligne pointillée, même après 3 s
    symptome: rien | gravite: moyen | bloque: non
  - ecran: 95 | cible: vumètres des tranches Batterie et Acid 303 | geste: touche
    attendu: Batterie muette (et à -6,5 dB) : son vumètre à zéro | obtenu: même niveau vert que Acid 303 à +0,0
    symptome: promesse | gravite: moyen | bloque: non
  - ecran: 89 | cible: « Ce qui joue » (carte Courbes et effets) | geste: touche
    attendu: indiquer que Batterie est muette, donc que ses effets ne s'entendent pas | obtenu: liste « EQ paramétrique · Compresseur » sur Batterie comme si elle jouait (pareil écran 12 avec le solo sur Acid 303)
    symptome: promesse | gravite: mineur | bloque: non
  - ecran: 68 | cible: ambiance « Punch » | geste: touche
    attendu: avertissement avant d'écraser ma chaîne réglée (EQ +11 dB) | obtenu: EQ remplacé par Compresseur + Saturation sans prévenir
    symptome: perdu | gravite: moyen | bloque: non
  - ecran: 8 | cible: « Place » « Gauche 63 » | geste: glisse
    attendu: une unité (% ou L/R sur 100) | obtenu: « 63 » sans unité, et l'aide « ? » ne l'explique pas (écran 63)
    symptome: sans-aide | gravite: mineur | bloque: non
  - ecran: 33 | cible: macros « Saleté », « Mouvement », « Espace » | geste: touche
    attendu: valeur avec unité ou échelle | obtenu: « 0 » sans unité ni maximum
    symptome: sans-aide | gravite: mineur | bloque: non
  - ecran: 4 | cible: valeurs sous les faders « +0,0 » / « -6,5 » | geste: glisse
    attendu: « dB » affiché | obtenu: chiffre seul (dB seulement dans la fiche et pour TalkBack) ; de même « TP −1,2 » sans dBTP (écran 50)
    symptome: sans-nom | gravite: mineur | bloque: non
  - ecran: 90 | cible: « Réverbe commune » / « Envoi réverbe » | geste: touche
    attendu: un seul nom pour le même réglage | obtenu: « Envoi réverbe » (fiche piste), « Réverbe commune » (Effets), « voies » vs « pistes » ; « Relâche » (comp.) vs « Relâchement » (limiteur) ; « Compresseur de bus (glue) » devient « Compresseur »
    symptome: incompris | gravite: mineur | bloque: non
  - ecran: 6 | cible: Guide « À corriger : Un solo est resté allumé » | geste: touche
    attendu: un solo voulu n'est pas une erreur | obtenu: affiché « À corriger » dès l'activation ; le pourcentage du Guide recule (23 % → 21 %) sans raison visible
    symptome: incompris | gravite: mineur | bloque: non
