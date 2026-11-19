code testé : 2b18313c4
joué par : sous-agent neuf
siege: a | profil: curieux | lang: it | device: phone360 | font: 200 | mode: simple
mission: tuto-facile (tuto phonk, chapitres 5 à 13) | buts: - (aucun id donné par le brief)
promesse: id=- | texte: Tutorial finito: bravo! | ecran: 92 | tenue: oui | pourquoi: les 13 chapitres finis en 91 gestes sur 150
chapitres:
  - ch5 Il pad : hésitation (grille cachée sous le panneau, premier « Copia » sans effet)
  - ch6 Ritornello : compris du premier coup
  - ch7 Capire le parti : hésitation (le Ritornello « tutto cade di colpo » contredit le ch6)
  - ch8 Costruisci le parti : compris du premier coup
  - ch9 Ordina le parti : hésitation forte, presque bloqué (environ 25 gestes pour 9 ajouts)
  - ch10 Le transizioni : hésitation (quel onglet ? pad « Piatto » caché)
  - ch11 Saturare tutto : compris du premier coup (on écoute seulement)
  - ch12 Mix volumi : hésitation (panneau sur les curseurs)
  - ch13 Esporta : compris du premier coup
hesitations:
  - ecran: 2 | cherchais: la note tratteggiata de la battuta 1 | regarde: seul « Accordi · battuta 1/8 » visible, la grille est sous le panneau du tuto
  - ecran: 40 | cherchais: les boutons « + » sous « Aggiungi » | regarde: la liste saute sur la nouvelle ligne et les boutons passent sous le panneau
  - ecran: 67 | cherchais: le pad « Piatto » | regarde: l'onglet n'est pas nommé et le pad est caché sous le panneau
abandon: jamais
incidents-banc:
defauts:
  - geste: 37 | ecran.vu: 38 | cible: icône ▶ « B · Intro × 1  ·  8 batt.  ·  suona » | geste.type: touche
    attendu: ajouter les 9 parties à la suite sous « Aggiungi » | obtenu: après chaque ajout la liste saute, les boutons + passent sous le panneau, j'ai lancé la lecture par erreur (#38) et la lecture ramène la liste en arrière
    symptome: cache | gravite: contourne-en-20-gestes | revu: oui: ecran 62 | banc?: non | promesse: -
  - geste: 47 | ecran.vu: 49 | cible: icône « Tocca a te: sotto “Aggiungi”, tocca in ordine: Intro, Ritornello, Strofa, Pre-ritornello, Ritornello, Strofa, Pre-ritornello, Ritornello, poi Intro per finire. Leggi » ⌃ | geste.type: glisse
    attendu: faire défiler la liste des parties en glissant au milieu | obtenu: rien ne bouge (#47, #48) ; il faut glisser tout à gauche (x=100)
    symptome: rien | gravite: contourne-en-2-gestes | revu: oui: ecran 50 | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 2 | cible: bouton « Accordi · battuta 1/8 Accordi tratteggiati da toccare: 1 » [coupé] | geste.type: touche
    attendu: voir la note tratteggiata à toucher | obtenu: la grille est sous le panneau du tuto ; ⌃ (#2) agrandit le panneau au lieu de le replier ; Mostrami + glisse pour la trouver
    symptome: cache | gravite: contourne-en-4-gestes | revu: non | banc?: non | promesse: -
  - geste: 8 | ecran.vu: 9 | cible: bouton ⊕ «   Copia sulle battute successive » @ 270,636 | geste.type: touche
    attendu: les accordi copiés sur les battute suivantes | obtenu: rien ne change ; le même bouton retouché (#10) marche
    symptome: rien | gravite: contourne-en-2-gestes | revu: oui: ecran 11 | banc?: temps | promesse: -
  - geste: 67 | ecran.vu: 68 | cible: icône « Tocca a te: sul pad “Piatto”, tocca la primissima casella. Leggi » ⌃ | geste.type: touche
    attendu: trouver le pad « Piatto » | obtenu: la consigne ne dit pas d'aller dans « Ritmo », le pad est sous le panneau, il faut glisser tout à gauche pour voir les cases ; étiquettes des pads couvertes par des pastilles (« D 30 », « FX »)
    symptome: sans-aide | gravite: contourne-en-4-gestes | revu: non | banc?: non | promesse: -
  - geste: 81 | ecran.vu: 82 | cible: icône « Tocca a te: nella scheda “Mix”, abbassa un po' gli slider “Accordi” e “Melodia”. Sotto lo slider, “Normale” diventa “−2”. Leggi » ⌃ | geste.type: glisse
    attendu: voir les curseurs « Accordi » et « Melodia » avec leur nom | obtenu: quand les curseurs sont enfin visibles sous le panneau, leurs noms sont sortis de l'écran par le haut
    symptome: cache | gravite: contourne-en-3-gestes | revu: non | banc?: non | promesse: -
  - geste: 22 | ecran.vu: 23 | cible: texte « Ascolta il Ritornello: tutto cade di colpo. » | geste.type: touche
    attendu: le Ritornello décrit comme au ch6 (la parte più piena) | obtenu: « tutto cade di colpo », je ne sais plus ce qu'est un Ritornello
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 21 | ecran.vu: 22 | cible: texte « Energia » [coupé] | geste.type: touche
    attendu: lire « Energia » et le bouton « Indietro » | obtenu: « Energia » coupé à moitié, le bouton retour n'est plus qu'une flèche ←
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 11 | ecran.vu: 12 | cible: onglet (quatre carrés) « Ritmo » | geste.type: touche
    attendu: voir les onglets du bas | obtenu: les onglets sont écrasés sous la barre « Modalità feedback » (5 dp de haut)
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 33 | ecran.vu: 34 | cible: « Più tardi » | geste.type: touche
    attendu: le bandeau « Scarica il kit Phonk (3 MB) » une seule fois | obtenu: il revient à chaque passo jusqu'à « Più tardi »
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 17 | vu: Dai il nome “Ritornello” alla parte (guillemets dans le bouton, mal fermés à l'œil) | attendu: Dai il nome Ritornello alla parte
  - ecran: 29 | vu: Crea la parte “Intro” | attendu: Crea la parte Intro
  - ecran: 75 | vu: Saturare tutto (titre) | attendu: un titre d'écoute : on ne sature rien soi-même
defauts-avant:
  - aucun défaut visé donné par le brief
fin-tranche: oui (ROUTE ligne 531)
