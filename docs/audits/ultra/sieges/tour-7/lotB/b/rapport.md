code testé : 28f1260707fe98b9322b638b5eb0f4db44c66028
joué par : sous-agent neuf
siege: b | profil: t7-2b-bresilienne-malvoyante | lang: pt-BR | device: phone | font: 200 | mode: simple
mission: remix-voix | buts: separe, decoupe, pose
promesse: id=t7-2b-1 | texte: Início aqui / Fim aqui / Ouvir o trecho 6 tempos · 0:03.0 | ecran: 23 | tenue: oui | pourquoi: les trois se touchent après un défilement, aucun hors de l'écran (mais [coupé] tant qu'on voit la courbe, écran 19)
promesse: id=t7-2b-2 | texte: “Voz 1” está numa nova faixa de áudio, no compasso 2. | ecran: 24 | tenue: oui | pourquoi: le message dit où (nouvelle piste, mesure 2 ; « pad live 1 » à l'écran 26)
promesse: id=t7-2b-3 | texte: + Escolher uma música | ecran: 12 | tenue: non | pourquoi: deux boutons « Escolher uma música » l'un sous l'autre ; deux flèches ← et deux ▶ sur l'écran de découpe (écran 15)
promesse: id=t7-2b-4 | texte: Pince para ampliar | ecran: 15 | tenue: non | pourquoi: « Pince » n'est pas du portugais ; « La menor » au lieu de « Lá menor » ; « Crie uma bat… » coupé
oeil-neuf: q1 | ecran: 28 | image: 28 | piste « Voz 1 », ▶, curseurs, pliages et onglets se voient touchables ; « Trocar o som » ne dit pas qu'il ouvre un autre écran
oeil-neuf: q6 | ecran: 28 | image: 28 | la preuve (piste Voz 1) est là, mais « A MÚSICA » dit « Sua música está vazia » : je ne sais pas si mon bout compte
oeil-neuf: q9 | ecran: 28 | image: 28 | « Verso, Refrão…) » coupé par le bas du pliage, « Crie uma bat… » coupé, « Compartilhar » minuscule, « música » de l'onglet collé au bord
hesitations:
  - ecran: 16 | cherchais: comment placer le début | regarde: « Início aqui » touché, rien ne bouge ; j'ai fini par tirer la poignée de la courbe
  - ecran: 26 | cherchais: la différence entre « Nos pads » et « No Live » | regarde: « Nos pads » répond « pad live 1 », « No Live » répond « célula Remix do bloco A »
  - ecran: 12 | cherchais: le résultat de la séparation | regarde: deux « Escolher uma música », il faut défiler pour voir la chanson séparée
abandon: jamais
incidents-banc:
  - geste: 5 | attente: 30 | vu: le rejeu du tour 6 s'est arrêté au geste 4 (« main rendue au pas 5 », écran de choix Simples/Pro) au lieu du geste 17 ; j'ai fait moi-même le chemin jusqu'à la découpe
  - geste: 0 | attente: 0 | vu: outils hors human-act : Read des images de mon siège, cat de rejeu.txt/journal.txt/rapport.md de mon dossier de siège (pour comprendre l'arrêt du rejeu)
defauts:
  - geste: 11 | ecran.vu: 12 | cible: bouton + «  Escolher uma música » | geste.type: touche
    attendu: un seul bouton pour choisir une musique | obtenu: « Escolher uma música » en haut du pliage ET « + Escolher uma música » en bas
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t7-2b-3
  - geste: 18 | ecran.vu: 19 | cible: bouton « Início aqui » (barre, onglet à gauche) [coupé] | geste.type: glisse
    attendu: courbe (un tiers de l'écran) et Início/Fim/Ouvir visibles ensemble | obtenu: zone utile ~400 px entre l'en-tête fixe « Cortar a música » et les barres du bas ; courbe visible = boutons coupés, boutons entiers = courbe partie
    symptome: cache | gravite: contourne-en-2-gestes | revu: oui: ecran 23 | banc?: non | promesse: t7-2b-1
  - geste: 16 | ecran.vu: 17 | cible: bouton (barre, onglet à gauche) « Início aqui » | geste.type: touche
    attendu: le début bouge ou un message dit où il est | obtenu: rien ne change (toujours 0:04.00), aucun message
    symptome: sans-aide | gravite: contourne-en-1-gestes | revu: oui: ecran 20 | banc?: non | promesse: t7-2b-1
  - geste: 14 | ecran.vu: 15 | cible: texte « Pince para ampliar » | geste.type: touche
    attendu: « Use dois dedos para ampliar » | obtenu: « Pince » (mot français)
    symptome: texte-faux | gravite: finition | revu: oui: ecran 19 | banc?: non | promesse: t7-2b-4
  - geste: 14 | ecran.vu: 15 | cible: bouton « Resto » [coupé] | geste.type: touche
    attendu: les 4 puces lisibles ou un signe qu'on peut défiler | obtenu: « Resto » coupé au bord droit, rien n'indique la suite
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t7-2b-3
  - geste: 14 | ecran.vu: 15 | cible: icône ← @ 42,388 | geste.type: touche
    attendu: un seul retour et un seul ▶ pour le bout | obtenu: « ← Minha música » et « ← » l'un sous l'autre ; ▶ à côté du titre en plus de « Ouvir o trecho », « ▶ esta parte » et « ▶ a música »
    symptome: incompris | gravite: finition | revu: oui: ecran 22 | banc?: non | promesse: t7-2b-3
  - geste: 27 | ecran.vu: 28 | cible: texte « Sua música está vazia: adicione suas partes na ordem em que tocam (Intro, Verso, Refrão…). » | geste.type: touche
    attendu: pas de « vazia » juste après avoir posé « Voz 1 » sur une piste | obtenu: « Sua música está vazia » sous la piste Voz 1
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 25 | ecran.vu: 26 | cible: texte « “Voz 1” está no pad live 1. » [coupé] | geste.type: touche
    attendu: « Nos pads » pose sur un pad de Ritmo (« Bateria 16 pads ») | obtenu: le message parle de « pad live 1 », alors qu'un autre bouton s'appelle « No Live »
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: t7-2b-2
  - geste: 6 | ecran.vu: 7 | cible: bouton (boussole) « Guia  · Crie uma batida 0 % » ⌃ [état « recolhido »] | geste.type: touche
    attendu: texte entier à 200 % | obtenu: « Crie uma bat… » coupé sur tous les écrans ; « Ma chanson préféré… » coupé (écran 15)
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 28 | banc?: non | promesse: t7-2b-4
  - geste: 6 | ecran.vu: 7 | cible: texte « Escolha uma música do aparelho: o app a separa em bateria, baixo, voz e o resto. Leva alguns minutos; você pode apagar a tela. » | geste.type: touche
    attendu: phrase entière | obtenu: « a tela » à moitié caché sous le bouton « Baixar o modelo (174 MB) »
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t7-2b-4
mots-faux:
  - ecran: 13 | vu: La menor | attendu: Lá menor
  - ecran: 15 | vu: Pince para ampliar | attendu: Use dois dedos para ampliar
defauts-avant:
  - R3-EC17 | pas corrigé (écran 19) : courbe et Início/Fim/Ouvir jamais entiers ensemble, la zone de découpe fait ~1/3 de l'écran en tout ; boutons touchables après défilement (écran 23)
  - R6 | corrigé (écran 24) : « “Voz 1” está numa nova faixa de áudio, no compasso 2. » ; écran 26 « “Voz 1” está no pad live 1. »
  - U13 | pas corrigé (écran 15) : puce « Resto » coupée ; les 4 boutons d'envoi entiers seulement après défilement (écran 23)
  - R1 | corrigé (écran 15) : « Cortar a música », « Bateria » entiers ; « Baixar o modelo (174 MB) » entier sur 2 lignes (écran 7) ; « Guardar » entier (écran 22)
  - R5 | pas corrigé (écran 15) : deux flèches retour et plusieurs ▶ ; deux « Escolher uma música » (écran 12)
  - N14 | pas corrigé (écran 28) : « Sua música está vazia » juste après avoir posé « Voz 1 » sur une piste
