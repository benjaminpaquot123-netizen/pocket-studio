siege: c | profil: t5-Dc beatmaker-italien-pliable | lang: it | device: foldable | font: 100 | mode: pro
mission: libre | buts: but1, but2, but3
promesse: id=t5-Dc-1 | texte: Salvato proprio ora | ecran: 62 | tenue: oui | pourquoi: l'accueil dit « Salvato proprio ora » et I miei progetti liste « Notte a Napoli APERTO proprio ora · 90 batt. · 3:00 · 5 parti » (aussi la copie, écran 82)
promesse: id=t5-Dc-2 | texte: Duplica | ecran: 35 | tenue: non | pourquoi: « Duplica » d'une parte ajoute une 2e « C · Ritornello » sans aucun avertissement de copie liée ; seul le toast périmé « Fatto: nuova lunghezza per B · Strofa » s'affiche (refait écran 37 et depuis la timeline écran 67 : rien non plus)
promesse: id=t5-Dc-3 | texte: Vedi tutto il brano | ecran: 52 | tenue: non | pourquoi: brano de 90 battute, la règle s'arrête vers 81-85, la dernière « C · Ritornello » sort à droite ; « A · Int » coupé, la parte de 2 batt. sans étiquette (refait écrans 54 et 56)
promesse: id=t5-Dc-4 | texte: Schermo intero | ecran: 56 | tenue: non | pourquoi: en plein écran la timeline laisse ~280 px vides sous les pistes (y 900-1180) alors que le brano ne tient pas en largeur ; ailleurs (Suoni, Mix, Parti) les deux colonnes servent bien
hesitations:
  - ecran: 13 | cherchais: comment faire « une partie » | regarde: l'appli mélange « parte » et « blocco » (« Rinomina » = « Dai un nome al blocco A », dialogue « Dai un nome alla parte A »)
  - ecran: 22 | cherchais: comment allonger une parte pour viser 3 min | regarde: « Una volta in più » +1 seulement, l'appui long n'accélère pas ; trouvé le champ « battute » dans « Altre azioni »
  - ecran: 31 | cherchais: pourquoi « B · Strofa 32 batt. » n'a plus « × 32 » et ses +/− sont grisés | regarde: l'Intro à 8 garde « × 8 · 8 batt. »
abandon: jamais
incidents-banc:
defauts:
  - geste: 34 | ecran.vu: 35 | cible: Duplica | geste.type: touche
    attendu: un mot qui prévient que la copie est liée (modifier l'une change l'autre) | obtenu: copie ajoutée sans message, toast d'une action précédente « Fatto: nuova lunghezza per B · Strofa »
    symptome: sans-aide | gravite: contourne-en-1-gestes | revu: oui: ecran 37 | banc?: non | promesse: t5-Dc-2
  - geste: 34 | ecran.vu: 35 | cible: Fatto: nuova lunghezza per B · Strofa | geste.type: touche
    attendu: toast qui parle de la duplication qu'on vient de faire | obtenu: toast périmé de l'action d'avant, qui en plus cache « + Nuova parte (copia di C) »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 51 | ecran.vu: 52 | cible: Vedi tutto il brano | geste.type: touche
    attendu: tout le brano de 90 battute visible jusqu'à la fin | obtenu: la règle s'arrête vers la battuta 85, fin du dernier Ritornello hors écran ; « Riduci zoom » ne fait rien de plus
    symptome: cache | gravite: contourne-en-1-gestes | revu: oui: ecran 56 | banc?: non | promesse: t5-Dc-3
  - geste: 51 | ecran.vu: 52 | cible: A · Int | geste.type: touche
    attendu: étiquettes de parti lisibles | obtenu: « A · Int » et « C · Ritornello : » coupés dans la ligne Parti, la parte C de 2 batt. sans aucun nom
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 78 | banc?: non | promesse: t5-Dc-3
  - geste: 54 | ecran.vu: 56 | cible: Schermo intero | geste.type: touche
    attendu: le plein écran du pliable sert à montrer plus de brano ou des pistes plus hautes | obtenu: bande vide de ~280 px sous les pistes, le brano reste coupé à droite
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: t5-Dc-4
  - geste: 32 | ecran.vu: 33 | cible: Parte C: copia della parte A. Cambiala per farne una variante, o “Svuota”. | geste.type: touche
    attendu: « copia della parte B » (le bouton disait « + Nuova parte (copia di B · Strofa) ») | obtenu: le texte dit copie de A
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 38 | ecran.vu: 39 | cible: champ « Oppure un altro nome 7/20 » contenu « Refrain » | geste.type: touche
    attendu: en choisissant « Ritornello », le champ affiche « Ritornello » | obtenu: le champ affiche « Refrain » (le nom final devient bien « Ritornello »)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 71 | banc?: non | promesse: -
  - geste: 20 | ecran.vu: 21 | cible: + A · Intro | geste.type: touche
    attendu: rester dans « Parti » pour enchaîner Intro, Strofa, Ritornello | obtenu: bascule seule sur « Timeline », il faut retoucher « Parti » (seulement au premier ajout)
    symptome: mauvais-ecran | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 5 | cible: Cancella | geste.type: touche
    attendu: après avoir vidé le nom, on peut taper directement | obtenu: le champ perd le focus, il faut le retoucher avant d'écrire
    symptome: incompris | gravite: contourne-en-1-gestes | revu: non | banc?: clavier | promesse: -
mots-faux:
  - ecran: 39 | vu: Refrain | attendu: Ritornello
  - ecran: 71 | vu: Couplet | attendu: Strofa
  - ecran: 13 | vu: Dai un nome al blocco A | attendu: Dai un nome alla parte A (même mot que le dialogue)
