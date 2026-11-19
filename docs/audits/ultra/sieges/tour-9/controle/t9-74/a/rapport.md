code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf

```
siege: a | profil: curieux | lang: en | device: foldable | font: 200 | mode: simple
mission: tuto-facile (trap, chapitres 8 à 15) | buts: (aucun id donné par le brief) chapitres-8-15-finis
promesse: id=- | texte: Under the slider, “Normal” becomes “−2”. | ecran: 63 | tenue: non | pourquoi: le chapitre 14 s'est validé avec « −1 » sous Chords et Melody, jamais « −2 »
promesse: id=- | texte: Well done, your song is finished! You can listen to it all again and keep changing it. | ecran: 72 | tenue: oui | pourquoi: export M4A fait, fin du tuto affichée avec Listen / Restart / Close
hesitations:
  - ecran: 31 | cherchais: la fin de la liste à taper (Intro, Chorus, Verse…) | regarde: la consigne est coupée « Verse, P... », « then Intro to finish » n'est plus lisible pendant qu'on tape
  - ecran: 33 | cherchais: le bouton « + C · Verse » | regarde: après chaque ajout la liste remonte, seul « + A · Chorus » reste, il faut glisser pour revoir les autres
  - ecran: 59 | cherchais: les curseurs du Mix après avoir touché l'onglet | regarde: le panneau du tuto couvre tout, il faut d'abord toucher « I'll do it »
abandon: jamais
incidents-banc:
  - geste: 0 | attente: 0 | vu: outils hors human-act : lecture des deux gabarits imposés par le chef, `cat`/`ls` et lecture d'images dans mon dossier de siège seulement
defauts:
  - geste: 32 | ecran.vu: 33 | cible: bouton « + A · Chorus » [coupé] @ 197,671 | geste.type: touche
    attendu: après avoir ajouté une partie, les boutons « + C · Verse » et « + D · Pre-chorus » restent sous mon doigt pour la suite de la liste | obtenu: la liste remonte sur la dernière partie ajoutée, seul « + A · Chorus » (coupé) reste visible ; 5 glissés en plus pour finir les 9 ajouts
    symptome: cache | gravite: contourne-en-5-gestes | revu: oui: ecran 39 | banc?: non | promesse: -
  - geste: 30 | ecran.vu: 31 | cible: icône « Your turn: under “Add”, tap in order: Intro, Chorus, Verse, Pre-chorus, Chorus, Verse, Pre-chorus, Chorus, then Intro to finish. Read » ⌃ @ 517,886 | geste.type: touche
    attendu: garder sous les yeux toute la liste des 9 parties à taper | obtenu: la consigne est coupée « Verse, P... » ; la fin (« then Intro to finish ») est cachée pendant qu'on tape
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 62 | ecran.vu: 63 | cible: curseur [état « −1 », à 73 %] @ 462,438 | geste.type: glisse
    attendu: le tuto dit que « Normal » devient « −2 » | obtenu: le chapitre est validé avec « −1 », la consigne ne correspond pas à ce que je vois
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 59 | ecran.vu: 60 | cible: texte « Your turn: raise what should be heard, lower what gets in the way. Tap ▶ to hear a track on its own. » [coupé] @ 520,211 | geste.type: touche
    attendu: lire l'aide du Mix en haut | obtenu: la phrase est à moitié cachée sous la ligne « 140 Tempo / Intro bar 1/120 », on ne voit que le bas des lettres
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 66 | ecran.vu: 67 | cible: champ « Song name (and the project's) » contenu « My song » [coupé] @ 518,341 | geste.type: touche
    attendu: voir le nom de ma chanson avant de partager | obtenu: le nom « My song » dans le champ est caché sous les boutons M4A / WAV, seule l'étiquette se lit
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 49 | ecran.vu: 50 | cible: bouton « Open hi-hat R 30 » @ 325,448 | geste.type: touche
    attendu: lire le nom des pads | obtenu: « R 30 » est dessiné par-dessus « Open », et « R 50 » par-dessus « Drum » (Drum rim) : les mots se chevauchent
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 7 | ecran.vu: 8 | cible: texte « Energy » [coupé] @ 90,539 | geste.type: touche
    attendu: voir le schéma « Energy » du chapitre 9 | obtenu: seulement un petit trait vert et « ••• » ; il faut toucher « Read on » pour le voir
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 10 | banc?: non | promesse: -
  - geste: 49 | ecran.vu: 50 | cible: icône « Intro bar 1/120 » ▾ @ 388,144 | geste.type: touche
    attendu: en haut, le nom de la partie que je regarde (Chorus) | obtenu: « Intro bar 1/120 » alors que « Part: A · Chorus » est choisi juste dessous ; je ne sais pas où je suis
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 19 | ecran.vu: 20 | cible: bouton ✓ «  It's done » @ 788,911 | geste.type: touche
    attendu: un seul bouton clair pour passer au chapitre suivant | obtenu: « Next chapter » et « It's done » côte à côte sur l'écran de fin de chapitre, sans dire ce que fait « It's done »
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 5 | ecran.vu: 6 | cible: texte « Your turn: tap “Name the part “Chorus””. » @ 426,491 | geste.type: touche
    attendu: une consigne simple | obtenu: guillemets dans les guillemets (“Name the part “Chorus””), même chose aux chapitres 10 et 12 ; pas naturel en anglais
    symptome: texte-faux | gravite: finition | revu: oui: ecran 21 | banc?: non | promesse: -
mots-faux:
  - ecran: 25 | vu: Studio bass drum | attendu: Kick (le tuto dit « the kick » au chapitre 9)
chapitres:
  - 8 compris du premier coup | 9 compris du premier coup | 10 compris du premier coup | 11 hésitation (liste coupée, boutons qui remontent)
  - 12 compris du premier coup | 13 compris du premier coup | 14 hésitation (panneau sur le Mix, −1 au lieu de −2) | 15 compris du premier coup
  - tuto fini au geste 71 sur 150
defauts-avant:
  - (aucun défaut visé donné par le brief)
```
fin-tranche: oui (ROUTE ligne 331)
