siege: c | profil: t6-Ac | lang: en | device: tablet8 | font: 100 | mode: pro
mission: padlive | buts: son, stop, prise
promesse: id=P067 | texte: Stop all / Blocks / Big pads / Share (boutons du tiroir « More ») | ecran: 14 | tenue: oui | pourquoi: Stop all coupe la boucle, Big pads ouvre les pads plein écran (écran 15), Blocks et Share présents mais non touchés
promesse: id=P073 | texte: Préparation des pads prêts à jouer… | ecran: 15 | tenue: pas vue | pourquoi: les pads pleins s'affichent sans message de préparation visible, trop rapide ou absent
promesse: id=P082 | texte: Les pads sonnent, les cases gardent les coups. Appui long : changer le son. | ecran: 2 | tenue: pas vue | pourquoi: les pads sonnent au 1er toucher, mais le texte n'est pas vu et l'appui long (#20) n'a rien montré de net
hesitations:
  - ecran: 6 | cherchais: Share, Big pads, Blocks | regarde: barre du bas, caché derrière « More »
abandon: jamais
incidents-banc:
defauts:
  - geste: #13 | ecran.vu: 13 | cible: bouton ⋯ « More » @ 803,1398 | geste.type: touche
    attendu: le tiroir reste ouvert après l'enregistrement de la prise | obtenu: le tiroir était refermé, mon toucher suivant l'a rouvert puis refermé, Big pads manqué
    symptome: incompris | gravite: finition | revu: oui: ecran 15 | banc?: non | promesse: P067
mots-faux:
