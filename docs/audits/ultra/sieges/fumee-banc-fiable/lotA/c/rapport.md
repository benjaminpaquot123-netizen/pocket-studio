siege: c | profil: t5-Cd | lang: fr | device: phone | font: 100 | mode: pro
mission: exports | buts: spotify, club, retrouve
promesse: id=t5-Cd-1 | texte: Envoyer le projet Pour le reprendre sur un autre appareil : un fichier .zip avec tous ses sons, à ouvrir là-bas dans Mes projets › Importer. / Autres exports Piste par piste, MIDI, FLAC… dans Mix | ecran: 16 | tenue: oui | pourquoi: le .zip part depuis « Envoyer le projet » (écran 21), « Importer un projet (.zip) » est bien dans Mes projets (écran 27), le MIDI est dans Mix (sous « Avancé », écran 16-17)
promesse: id=t5-Cd-2 | texte: Mes exports : retrouver et renvoyer un fichier | ecran: 15 | tenue: pas vue | pourquoi: bouton vu sous l'export dans Mix, mais pas ouvert : budget de gestes épuisé après un détour (Défis), je ne l'ai pas retrouvé en redescendant dans Mix
promesse: id=t5-Cd-3 | texte: Cet appareil ne gère pas le MIDI. / Aucun contrôleur branché. Branche ton clavier en USB : il est reconnu tout seul. | ecran: 25 | tenue: non | pourquoi: les deux phrases sont sur la même page MIDI, l'une juste au-dessus de l'autre, et se contredisent
hesitations:
  - ecran: 16 | cherchais: l'export MIDI annoncé « dans Mix » | regarde: rien de visible dans Mix avant d'ouvrir « Avancé »
  - ecran: 28 | cherchais: Mes exports depuis Mes projets (là où un musicien range ses fichiers) | regarde: seulement Renommer, Dupliquer, Envoyer (.zip)… pas de Mes exports
  - ecran: 39 | cherchais: le bloc d'export et « Mes exports » en redescendant dans Mix | regarde: page Mix très longue (courbes, sortie générale, master, réverbe), le bloc export n'est plus là où je l'avais vu
abandon: ecran 40 | parce que budget de 40 gestes atteint avant d'ouvrir « Mes exports »
incidents-banc:
  - geste: 36 | attente: 0 | vu: un « BrokenPipeError » du script de banc, provoqué par mon propre filtrage de sortie ; l'écran 37 est bien arrivé
  - geste: 38 | attente: 0 | vu: j'ai redirigé par erreur une sortie hors de mon siège (lotA/c-out.txt), fichier supprimé aussitôt
defauts:
  - geste: 24 | ecran.vu: 25 | cible: Cet appareil ne gère pas le MIDI. | geste.type: touche
    attendu: une page cohérente : soit le MIDI marche et le clavier USB est reconnu, soit pas de promesse « reconnu tout seul » | obtenu: « ne gère pas le MIDI » juste au-dessus de « Branche ton clavier en USB : il est reconnu tout seul. »
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t5-Cd-3
  - geste: 10 | ecran.vu: 11 | cible: Le morceau a changé depuis cet export : exporte à nouveau pour envoyer la nouvelle version. | geste.type: touche
    attendu: aucun avertissement : je n'ai rien touché entre l'export streaming (écran 9) et l'ouverture de Mix | obtenu: l'appli dit que le morceau a changé, alors que seul le choix « Streaming » avait été fait avant l'export
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 30 | ecran.vu: 32 | cible: Continuer Pro Live Boucle de 1 mes. Sauvegardé à l'instant | geste.type: touche
    attendu: toucher « Défis » sur l'accueil puis Retour ne change pas mon projet en cours | obtenu: un nouveau projet « Live » est créé et remplace ma démo dans « Continuer » ; il faut passer par Mes projets pour la rouvrir
    symptome: perdu
    gravite: contourne-en-2-gestes | revu: oui: ecran 36 | banc?: non | promesse: -
  - geste: 29 | ecran.vu: 30 | cible: Retour | geste.type: touche
    attendu: Retour depuis Mes projets (ouvert depuis Plus) me ramène dans mon projet | obtenu: retour à l'accueil « Que veux-tu faire ? »
    symptome: mauvais-ecran
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
