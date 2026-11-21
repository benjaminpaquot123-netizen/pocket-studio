code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf
siege: a | profil: debutant-motive | lang: es | device: phone | font: 130 | mode: simple
mission: tuto-facile (deep_house ch.7-13) | buts: -
promesse: -
chapitres:
  - ch7 Entender las partes : hesitation (ecran 10, mapa fuera de vista)
  - ch8 Construye tus partes : compris du premier coup
  - ch9 Ordena tus partes : compris du premier coup
  - ch10 Las transiciones : hesitation (ecran 44, no sé qué pad borro ; « shaker » vs « Maracas »)
  - ch11 Los efectos : compris du premier coup
  - ch12 La mezcla : compris du premier coup
  - ch13 Exporta tu tema : compris du premier coup (ecran 74 « ¡Bravo, lo has conseguido! » Capítulo 13/13)
hesitations:
  - ecran: 10 | cherchais: el mapa que el texto me pide mirar | regarde: solo veía « Energía », he deslizado la tarjeta
  - ecran: 44 | cherchais: el nombre del pad que estaba borrando | regarde: « 4 golpes de más aquí » sin nombre de pad
  - ecran: 55 | cherchais: el pad « shaker » | regarde: en la lista solo hay « Maracas »
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: aucun gel ; lu les 2 gabarits hors du siège sur consigne du chef (pas de code de l'appli)
defauts:
  - geste: 7 | ecran.vu: 7 | cible: Intro | geste.type: touche
    attendu: leyenda del mapa de partes entera | obtenu: leyenda (Intro, Subida, Drop, Break, Drop 2) cortada a media altura
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 10 | ecran.vu: 10 | cible: Mira el mapa: la Intro (28 compases)… | geste.type: touche
    attendu: el mapa visible junto al texto | obtenu: mapa empujado fuera de vista, hay que deslizar la tarjeta
    symptome: cache
    gravite: contourne-en-1-gestes | revu: oui: ecran 11 | banc?: non | promesse: -
  - geste: 13 | ecran.vu: 13 | cible: Te toca: toca “Crear la parte “Intro””. | geste.type: touche
    attendu: comillas correctas (« Crear la parte "Intro" ») | obtenu: comillas anidadas iguales “…“Intro””, raro de leer (igual en todos los pasos Crear/Ir a la parte)
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 15 | banc?: non | promesse: -
  - geste: 38 | ecran.vu: 39 | cible: Subida compás 1/8 | geste.type: touche
    attendu: nombre de la parte entero en el chip de arriba | obtenu: « Su… compás » / « Dro… compás », nombre cortado a 130 %
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 42 | banc?: non | promesse: -
  - geste: 39 | ecran.vu: 39 | cible: Compás 4/4 · Tom medio | geste.type: touche
    attendu: cabecera de la rejilla (compás y pad) legible | obtenu: tapada por el mensaje « Se repite en bucle… »
    symptome: cache
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 42 | ecran.vu: 44 | cible: 4 golpes de más aquí (tachados) | geste.type: touche
    attendu: ver qué pad estoy editando (« → Palmada ») | obtenu: el nombre del pad desaparece, solo el aviso de golpes de más
    symptome: sans-nom
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 65 | ecran.vu: 67 | cible: Salida | geste.type: touche
    attendu: todas las tiras del Mix enteras y alineadas | obtenu: tira « Salida » cortada en el borde derecho, tira Batería desalineada
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 1 | cible: Descargar el kit Deep house (4 MB) | geste.type: touche
    attendu: ver el mapa de partes del capítulo 7 | obtenu: la tarjeta de descarga del kit tapa el mapa
    symptome: cache
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 40 | ecran.vu: 42 | cible: Subida compás 5/8 | geste.type: touche
    attendu: contador coherente con la Subida de 4 compases | obtenu: « compás 5/8 » mientras la Subida dura 4 compases
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 55 | vu: shaker | attendu: Maracas

fin-tranche: oui (Bravo « ¡Bravo, lo has conseguido! » screen-74 ligne 21, Capítulo 13/13 ; dernière ROUTE journal ligne 379 step=share done=31, attendu done=37 : écart à lire par le trieur)
incidents-banc: amenée tuto:deep_house:7 refusée au 1er lancement (« les chapitres n'est jamais venu », HumanTester.kt:845), passée au 2e lancement
