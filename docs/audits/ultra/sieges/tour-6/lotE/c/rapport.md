siege: c | profil: t6-Ec-malvoyante-br-200 | lang: pt-BR | device: phone | font: 200 | mode: simple
mission: remix-voix | buts: separe, decoupe, pose
promesse: id=t6-Ec-1 | texte: Tout le Remix se fait sans bouton hors de l'écran ni texte coupé avec la police 200 % | ecran: 18 | tenue: non | pourquoi: dans « Cortar a música » la courbe et les barres Início/Fim n'apparaissent jamais ; « Baixar o modelo (174 », « Cortar a mú… », « Bateri », « Guard ar » coupés
promesse: id=t6-Ec-2 | texte: La séparation de la chanson se lance et se termine | ecran: 16 | tenue: oui | pourquoi: modèle téléchargé, chanson lue, puis puces Bateria / Baixo / Resto / Voz
promesse: id=t6-Ec-3 | texte: Un bout de voix se pose sur un pad ou une piste | ecran: 25 | tenue: oui | pourquoi: « “Voz 1” está no pad live 1. » (écran 21) et « “Voz 1” está numa nova faixa de áudio, no compasso 3. » (écran 25) ; mais c'est la voix entière, pas un bout choisi
promesse: id=t6-Ec-4 | texte: tous les textes vus sont dans ma langue, aucun en français ni coupé | ecran: 13 | tenue: non | pourquoi: « La menor » au lieu de « Lá menor », et beaucoup de mots coupés (« Des… », « Crie uma bat… », « Bateri »)
oeil-neuf: q1 | ecran: 32 | image: 32 | pads, onglets, Kit, Todos os pads touchables ; « Des… » se lit à peine comme un bouton, la phrase « Os pads soam… » coupée en « quadrados… »
oeil-neuf: q6 | ecran: 32 | image: 32 | la voix posée sur le pad live 1 n'est pas visible ici (pads de batterie seulement) : pas de preuve sur cet écran
oeil-neuf: q9 | ecran: 32 | image: 32 | « Des… », « Crie uma bat… », « PASSOS: BUMBO » coupé, « Os pads soam, os quadrados… », onglet « Minha música » sur deux lignes qui touche le bord
hesitations:
  - ecran: 11 | cherchais: si la séparation avait marché après « Lendo “Ma chanson préférée”… » | regarde: il a fallu faire défiler 4 fois la petite fenêtre pour voir les puces Voz/Bateria
  - ecran: 18 | cherchais: les barres Início et Fim que le texte demande de placer | regarde: rien à l'écran, même l'aide (écran 22) parle d'une « curva » et de « Ouvir o trecho » invisibles
abandon: jamais
incidents-banc:
defauts:
  - geste: 17 | ecran.vu: 18 | cible: texte « Posicione Início e Fim, depois coloque este trecho num pad ou faixa. » | geste.type: touche
    attendu: la courbe de la voix avec les barres Início/Fim pour choisir un bout | obtenu: aucune courbe ni barre à 200 %, seulement les 4 boutons d'envoi : impossible de découper
    symptome: cache | gravite: mission-impossible | revu: oui: ecran 27 | banc?: non | promesse: t6-Ec-1
  - geste: 24 | ecran.vu: 27 | cible: texte « “Voz 1” está numa nova faixa de áudio, no compasso 3. » | geste.type: touche
    attendu: le message s'ajoute sans rien cacher | obtenu: les puces Voz/Bateria disparaissent et les 4 boutons d'envoi perdent leur nom (icônes seules)
    symptome: sans-nom | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: t6-Ec-1
  - geste: 5 | ecran.vu: 6 | cible: bouton « Baixar o modelo (174 MB) » | geste.type: touche
    attendu: le bouton lisible en entier | obtenu: « Baixar o modelo (174 » coupé
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t6-Ec-1
  - geste: 5 | ecran.vu: 6 | cible: texte « Primeiro é preciso o modelo de separação (174 MB, uma vez só, Wi-Fi recomendado). » [coupé] | geste.type: touche
    attendu: la phrase visible | obtenu: cachée sous le bouton orange, illisible
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: t6-Ec-1
  - geste: 17 | ecran.vu: 18 | cible: texte « Cortar a música » | geste.type: touche
    attendu: titre et nom de la chanson entiers | obtenu: « Cortar a mú… » et « Ma chanson préféré… » coupés
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 27 | banc?: non | promesse: t6-Ec-1
  - geste: 17 | ecran.vu: 18 | cible: bouton (marque-page et +) « Guardar » | geste.type: touche
    attendu: « Guardar » sur une ligne | obtenu: mot cassé en « Guard / ar »
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t6-Ec-4
  - geste: 15 | ecran.vu: 16 | cible: bouton « Bateria » | geste.type: glisse
    attendu: « Bateria » entier | obtenu: « Bateri » coupé dans la puce
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: t6-Ec-1
  - geste: 5 | ecran.vu: 6 | cible: icône ↶ « Desfazer » [désactivé] | geste.type: touche
    attendu: les mots de la barre du bas lisibles | obtenu: « Des… », « Guia · Crie uma bat… », onglet « Minha música » qui déborde sur deux lignes
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 32 | banc?: non | promesse: t6-Ec-4
  - geste: 12 | ecran.vu: 9 | cible: texte « Pronto: o modelo está no seu celular. Escolha uma música. » [coupé] | geste.type: glisse
    attendu: tout le Remix visible en grand | obtenu: la fenêtre du Remix fait ~4 lignes, il faut faire défiler sans cesse ; « Escolher uma música » apparaît deux fois (écran 11)
    symptome: hors-pouce | gravite: contourne-en-3-gestes | revu: non | banc?: non | promesse: t6-Ec-1
  - geste: 27 | ecran.vu: 28 | cible: texte « Sua música está vazia: adicione suas partes na ordem em que tocam (Intro, Verso, Refrão…). » | geste.type: touche
    attendu: voir que la voix est posée au compasso 3 | obtenu: « Sua música está vazia » alors que la piste « Voz 1 » vient d'être créée
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 13 | vu: La menor | attendu: Lá menor
  - ecran: 29 | vu: A compasso 1/1 | attendu: (peu clair : « Parte A, compasso 1/1 » ?)
