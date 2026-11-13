code testé : 4cb88f552031889ab2b4854b9ddd285f2abb6250
joué par : sous-agent neuf
siege: a | profil: t6-Ha-controle | lang: fr | device: phone | font: 100 | mode: simple
mission: tuto-facile | buts: chap1, chap3, bravo
promesse: id=t6-Ha-1 | texte: J'apprends Tu fais tout le morceau toi-même, étape par étape : je t'explique et je vérifie. | ecran: 4 | tenue: oui | pourquoi: J'apprends → Simple (« Avec le tuto Facile ») → House → chapitre 1/15 Facile, en 3 gestes, sans impasse
promesse: id=t6-Ha-2 | texte: Bravo, ton morceau est fini ! Tu peux tout réécouter et continuer à le changer. | ecran: 236 | tenue: oui | pourquoi: les 15 chapitres se suivent jusqu'au Bravo final sans impasse ni « Suivant » forcé
promesse: id=t6-Ha-3 | texte: Réussi Étape suivante | ecran: 236 | tenue: oui | pourquoi: chaque geste demandé a validé son étape (cases, notes, accords, parties, curseurs, export)
promesse: id=t6-Ha-4 | texte: touche « Dorienne » (une gamme mineure avec une note claire qui donne le sourire), puis « La ». | ecran: 13 | tenue: non | pourquoi: « mineure », « relances de fin de phrase » (189), « couche » (214), « Niveau … (−14) » (231) ne sont jamais expliqués
promesse: id=t6-Ha-5 | texte: Télécharger le kit House (4 Mo) | ecran: 165 | tenue: non | pourquoi: tout est en français, mais textes coupés : bouton du kit recouvert par « ▸ Pourquoi ? » (165), champ « Mon morceau » coupé sous la barre (236)
oeil-neuf: q1 | ecran: 236 | image: 236 | boutons orange/contour clairs ; « Intro mesure 1/96 » ▾ en haut a l'air touchable mais ne dit pas à quoi il sert
oeil-neuf: q6 | ecran: 236 | image: 236 | preuve : coupe + « Bravo, ton morceau est fini ! » ; suite claire : Écouter / Recommencer / Fermer le tuto
oeil-neuf: q9 | ecran: 236 | image: 236 | le champ « Nom du morceau » est coupé : « Mon morceau » à moitié caché sous la barre du haut
chapitre: 1/15 « La vitesse et la couleur du morceau » | ecran: 4 | hésitation : « gamme mineure » et « Dorienne » sans explication du mot « mineure »
chapitre: 2/15 « Le kick » | ecran: 19 | compris du premier coup
chapitre: 3/15 « Le clap » | ecran: 33 | compris du premier coup
chapitre: 4/15 « Les charleys et le tambourin » | ecran: 41 | hésitation : le pad « Bord de caisse » joue le tambourin, et « les 4 cases » en demandait 8
chapitre: 5/15 « La basse » | ecran: 84 | hésitation : consigne très longue (Ré en mesure 3) alors que la copie fait la mesure 3 toute seule
chapitre: 6/15 « Le piano » | ecran: 107 | hésitation : « Encore 9 notes » alors que je vois 12 pointillés et que chaque toucher n'en enlève qu'une
chapitre: 7/15 « La petite boucle » | ecran: 136 | compris du premier coup
chapitre: 8/15 « Ta première partie : le Groove » | ecran: 161 | compris du premier coup
chapitre: 9/15 « Comprendre les parties » | ecran: 167 | compris du premier coup
chapitre: 10/15 « Construis tes parties » | ecran: 181 | hésitation : « relances de fin de phrase » ; consigne « piano, boucle », résultat « Accords, Mélodie »
chapitre: 11/15 « Range tes parties » | ecran: 193 | compris du premier coup
chapitre: 12/15 « Les transitions » | ecran: 204 | hésitation : la Montée « se tait » déjà (écran 184) mais il reste des coups barrés à enlever
chapitre: 13/15 « Les effets » | ecran: 219 | compris du premier coup
chapitre: 14/15 « Le mix : les volumes » | ecran: 225 | compris du premier coup
chapitre: 15/15 « Exporte ton morceau » | ecran: 230 | hésitation : « Niveau : comme la musique en streaming (−14) » non expliqué
defauts-avant:
  - T2 | corrigé (écran 156)
  - T7a | corrigé (écran 7)
  - T7c | corrigé (écran 117)
  - T8 | autre : aucune étape ne m'a dit « Déjà fait » ; à l'écran 205 la Montée annoncée « son dernier temps se tait » (184) demandait encore d'effacer des coups barrés (autres pads)
hesitations:
  - ecran: 6 | cherchais: quel bouton pour avancer | regarde: « Étape suivante » et « Suivant » à l'écran en même temps, je ne sais pas lequel prendre
  - ecran: 113 | cherchais: revenir sur la piste « Accords » | regarde: les pistes ont bougé, j'ai touché la Basse et une fiche de sons s'est ouverte
  - ecran: 3 | cherchais: ce que veut dire « 122 BPM » | regarde: aucune explication sur la liste des styles (expliqué seulement à l'écran 7)
abandon: jamais
incidents-banc:
  - geste: 33 | attente: 0 | vu: le texte de l'écran est vide (écrans 34, 35, 42, 53, 69, 194, 195, 226, 231) ; l'image, elle, montrait tout
  - geste: 0 | attente: 0 | vu: outils hors human-act : Read des images de mon siège, cat de rapport.md, ls et grep du journal de mon siège (pour les n° de geste)
defauts:
  - geste: 113 | ecran.vu: 114 | cible: bouton « Basse Basse ronde » ⋮ @ 323,186 | geste.type: touche
    attendu: revenir sur la piste Accords là où elle était | obtenu: les pistes se décalent quand on en choisit une ; j'ouvre la fiche des sons de Basse, 3 gestes pour revenir
    symptome: mauvais-ecran
    gravite: contourne-en-3-gestes | revu: non | banc?: non | promesse: -
  - geste: 12 | ecran.vu: 13 | cible: texte « À toi : choisis la couleur du morceau. « Je le fais » ouvre la fiche Tonalité : touche « Dorienne » (une gamme mineure avec une note claire qui donne le sourire), puis « La ». » | geste.type: touche
    attendu: chaque mot de musicien expliqué | obtenu: « mineure » jamais expliqué (seuls Gamme et Tonalité le sont)
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t6-Ha-4
  - geste: 188 | ecran.vu: 189 | cible: texte « À toi : crée le Drop 2 : tout ton Groove, avec la batterie de l'exemple et ses relances de fin de phrase (« Montre-moi » les pose). » | geste.type: touche
    attendu: des mots simples | obtenu: « relances de fin de phrase » non expliqué ; et le résultat dit seulement « tout le Groove y joue »
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t6-Ha-4
  - geste: 213 | ecran.vu: 214 | cible: texte « À toi : dans la partie « Drop 2 », ajoute quelques coups sur le pad « Maracas » (ou le charley ouvert) : une couche de plus que dans le Drop. » | geste.type: touche
    attendu: des mots simples | obtenu: « couche » non expliqué
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t6-Ha-4
  - geste: 230 | ecran.vu: 231 | cible: texte « Niveau : comme la musique en streaming (−14) » | geste.type: touche
    attendu: savoir ce que veut dire (−14) | obtenu: chiffre sans explication
    symptome: incompris
    gravite: finition | revu: non | banc?: son | promesse: t6-Ha-4
  - geste: 164 | ecran.vu: 165 | cible: bouton « Télécharger le kit House (4 Mo) » [coupé] | geste.type: touche
    attendu: le bouton du kit entier et lisible | obtenu: « ▸ Pourquoi ? » est posé par-dessus le bouton, son texte est coupé
    symptome: texte-coupe
    gravite: finition | revu: oui: ecran 166 (même bouton entier ailleurs) | banc?: non | promesse: t6-Ha-5
  - geste: 235 | ecran.vu: 236 | cible: champ « Nom du morceau (et du projet) » contenu « Mon morceau » [coupé] | geste.type: touche
    attendu: le nom du morceau lisible avec le Bravo final | obtenu: le champ est à moitié caché sous la barre du haut
    symptome: texte-coupe
    gravite: finition | revu: non | banc?: non | promesse: t6-Ha-5
  - geste: 68 | ecran.vu: 73 | cible: texte « À toi : sur le pad « Bord de caisse » (il joue le tambourin), touche les 4 cases en pointillé, entre les temps. » | geste.type: touche
    attendu: 4 cases comme écrit | obtenu: après 4 cases, il en faut encore 4 dans la mesure 2 (8 en tout) ; et le pad s'appelle « Bord de caisse » mais joue le tambourin
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t6-Ha-4
  - geste: 122 | ecran.vu: 123 | cible: bouton « Accords · mesure 3/8 Encore 9 notes en pointillé à toucher » | geste.type: touche
    attendu: un compte qui correspond à ce que je vois | obtenu: 12 pointillés visibles, chaque accord touché n'enlève qu'une « note » ; je ne sais pas combien il en reste vraiment
    symptome: incompris
    gravite: finition | revu: oui: ecran 128 | banc?: non | promesse: t6-Ha-4
compte: hesitations=10
