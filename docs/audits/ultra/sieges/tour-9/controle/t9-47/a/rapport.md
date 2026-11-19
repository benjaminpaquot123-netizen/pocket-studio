code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf

siege: a | profil: debutant-motive | lang: es | device: tablet8 | font: 200 | mode: simple
mission: tuto-facile (tuto lofi, chapitres 6 à 13) | buts: -
promesse: id=- | texte: « Bajo el fader, “Normal” pasa a “−2” » | ecran: 57 | tenue: pas vue | pourquoi: le texte sous le fader est caché par le panneau du tuto à 200 %, le chapitre est pourtant validé
chapitres:
  - 6 : compris du premier coup
  - 7 : compris du premier coup
  - 8 : compris du premier coup (un geste pour déplier le panneau après la démo)
  - 9 : hésitation (boutons « + » de « Añadir al final: » recachés à chaque ajout ; pas de partie « Outro »)
  - 10 : compris du premier coup
  - 11 : compris du premier coup
  - 12 : hésitation (faders cachés par le panneau, il a fallu le replier)
  - 13 : compris du premier coup (export M4A, partage, « ¡Bravo, tu tema está terminado! » écran 67)
hesitations:
  - ecran: 28 | cherchais: les boutons pour ajouter les parties | regarde: le panneau du tuto qui les recouvre
  - ecran: 41 | cherchais: une partie « Outro » demandée par « Faltan: … Outro » | regarde: les 4 boutons « + » sous « Añadir al final: »
  - ecran: 54 | cherchais: les faders du mixage | regarde: le panneau du tuto déplié par-dessus
abandon: jamais
incidents-banc:
defauts:
  - geste: 29 | ecran.vu: 28 | cible: texte « Añadir al final: » [coupé] | geste.type: touche
    attendu: après un « + X », les boutons restent visibles pour ajouter la partie suivante | obtenu: les boutons repassent sous le panneau du tuto après chaque ajout, il faut re-glisser à chaque fois (#29, #31, #33, #35, #37, #39, #41)
    symptome: cache | gravite: contourne-en-7-gestes | revu: oui: ecran 41 | banc?: non | promesse: -
  - geste: 28 | ecran.vu: 28 | cible: bouton « + B · Intro » | geste.type: touche
    attendu: la liste « Faltan » correspond aux parties proposées | obtenu: « Faltan: … Estribillo, Outro. » alors que la consigne dit « y la Intro al final » et qu'aucune partie « Outro » n'existe
    symptome: texte-faux | gravite: finition | revu: oui: ecran 41 | banc?: non | promesse: -
  - geste: 56 | ecran.vu: 55 | cible: texte « Bajo el fader, “Normal” pasa a “−2” » | geste.type: glisse
    attendu: voir « Normal » devenir « −2 » sous le fader | obtenu: le texte sous le fader est caché par le panneau du tuto (font 200 %)
    symptome: cache | gravite: finition | revu: oui: ecran 58 | banc?: non | promesse: -
  - geste: 18 | ecran.vu: 19 | cible: bouton « Muéstramelo » | geste.type: touche
    attendu: après la démo, le panneau revient avec l'action à faire visible | obtenu: le panneau reste replié, « Crear la parte “Intro” » caché ; un geste pour le déplier
    symptome: cache | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 48 | ecran.vu: 48 | cible: texte « 4 » [tachado] | geste.type: touche
    attendu: la consigne demande d'effacer les coups de bombo ET de caja du dernier temps ; le chapitre attend les deux | obtenu: le chapitre s'est validé après la seule caja
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 4 | cible: bouton « Siguiente » | geste.type: touche
    attendu: passer à la suite après avoir écouté l'exemple (il jouait) | obtenu: « Ejemplo saltado sin escucharlo. » alors que je l'écoutais
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 47 | ecran.vu: 47 | cible: titre « PASOS: LO-FI HIP-HOP · CAJA · ESTROFA » | geste.type: touche
    attendu: titre lisible en entier | obtenu: titre coupé en haut à 200 %
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 55 | ecran.vu: 55 | cible: textes « Acordes », « Melodía » | geste.type: touche
    attendu: noms des pistes à la même taille | obtenu: « Acordes » et « Melodía » bien plus petits que « Bajo » et « Salida »
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 4 | cible: texte « Te toca: toca “Nombrar la parte “Estribillo””. » | geste.type: touche
    attendu: guillemets lisibles | obtenu: guillemets imbriqués du même type, difficile à lire (aussi écrans 17, 22, 24, 45)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 45 | banc?: non | promesse: -
  - geste: 2 | ecran.vu: 2 | cible: boutons « Detener » / « Dejar de escuchar » | geste.type: touche
    attendu: un seul mot pour arrêter le son | obtenu: deux libellés différents pour le même geste (écrans 2 et 3)
    symptome: incompris | gravite: finition | revu: oui: ecran 3 | banc?: non | promesse: -
mots-faux:
  - ecran: 6 | vu: kick (« el kick, el bajo ») | attendu: bombo
  - ecran: 14 | vu: « sin kick ni caja » | attendu: « sin bombo ni caja »
  - ecran: 47 | vu: « golpes de kick » | attendu: « golpes de bombo »
  - ecran: 18 | vu: « Añádelos » (les parties) | attendu: « Añádelas »
defauts-avant:

résumé : 0 bloquant, 1 grave (contourne-en-7-gestes), 9 finition ; chapitres finis : 6 à 13 (8/8) ; gestes utilisés : 68 / 150.

fin-tranche: oui (ROUTE ligne 294)
BENCH-VERDICT: #67 BENCH-VERDICT ok where=seat.fin
