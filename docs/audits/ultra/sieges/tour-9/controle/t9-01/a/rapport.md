code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: curieux | lang: ar | device: phone360 | font: 130 | mode: simple
mission: tuto-facile (acid_house, chapitres 1 à 3) | buts: aucun donné par le brief
promesse: id=- | texte: «سأفعلها» يفتحها، و«أرني» يكتب الخط فيها: شاهد نوتاتها الـ13 توضع. | ecran: 17 | tenue: non | pourquoi: les deux boutons ouvrent la feuille « صوت: Acid 303 », jamais l'écran de la 303
chapitres:
  - chapitre 1 « الأسيد هاوس » : hésitation (étape 3 : « A » demandé, la carte de tonalité dit « لا »)
  - chapitre 2 « الـ303 أولًا » : bloqué (étape 2 jamais validée, sautée par « التالي » deux fois)
  - chapitre 3 « آلة الإيقاع » : compris du premier coup (7/7, écran 79 « تم »)
hesitations:
  - ecran: 8 | cherchais: la tonalité « A » demandée | regarde: les noms de notes (لا، دو…) et « الجزء: A » ; c'est le pointillé autour de « لا » qui m'a guidé
  - ecran: 25 | cherchais: quoi faire après « ها هو! دورك الآن. » | regarde: « سأفعلها » grisé, « إيقاف » qui rouvre la feuille de son
  - ecran: 37 | cherchais: l'écran de la 303 | regarde: trouvé seul par l'onglet « Acid 303 خط باص »
abandon: jamais (étape 2 du chapitre 2 sautée, pas abandonnée)
incidents-banc: aucun
defauts:
  - geste: 17 | ecran.vu: 18 | cible: bouton (œil) «  أرني » | geste.type: touche
    attendu: l'écran de la 303 s'ouvre et le tuto y écrit 13 notes | obtenu: la feuille « صوت: Acid 303 » (choix de son) s'ouvre, aucune note écrite, l'étape ne se valide jamais
    symptome: mauvais-ecran | gravite: mission-impossible | revu: oui: ecran 22, 42 (أرني) et 31, 35 (سأفعلها) | banc?: non | promesse: -
  - geste: 44 | ecran.vu: 47 | cible: icône « التالي  » → | geste.type: touche
    attendu: une ligne 303 existe (écrite par « اكتب لي خطًا », écran 40), l'étape devrait passer | obtenu: « بلا 303، لا أسيد هاوس. المس «التالي» مرة أخرى لتخطيها على أي حال. »
    symptome: texte-faux | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 25 | ecran.vu: 25 | cible: bouton (doigt qui touche) «   سأفعلها » [désactivé] | geste.type: touche
    attendu: après « ها هو! دورك الآن. » je peux agir | obtenu: « سأفعلها » grisé et « إيقاف » rouvre la feuille de son ; on ne sait pas quoi faire
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 38 | ecran.vu: 38 | cible: bouton « اكتب لي خطًا » | geste.type: glisse
    attendu: la grille de la 303 visible | obtenu: la carte du tuto couvre la grille, il faut faire défiler
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 44 | ecran.vu: 45 | cible: case de la grille 303 | geste.type: touche
    attendu: « اضغط خانة لتغيير نوتتها » : la note change | obtenu: la case est seulement entourée de vert, rien ne change
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 11 | ecran.vu: 11 | cible: باد « طبل كبير للاستوديو » | geste.type: touche
    attendu: texte de même taille que les pads voisins | obtenu: texte nettement plus petit ; et « L 30 », « R 45 » en lettres latines
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 16 | ecran.vu: 16 | cible: texte « ▾ لماذا؟ » | geste.type: touche
    attendu: l'explication ouverte reste lisible | obtenu: elle disparaît quand l'étape se valide, la flèche reste ▾
    symptome: perdu | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 18 | ecran.vu: 18 | cible: « إغلاق » | geste.type: touche
    attendu: bouton séparé du texte | obtenu: « إغلاق » collé à la ligne de description de la feuille de son
    symptome: hors-pouce | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 13 | ecran.vu: 13 | cible: « الـ303 » | geste.type: touche
    attendu: un mot expliqué pour un curieux | obtenu: « الـ303 » jamais expliqué ; « الـaccents »، « الـslides »، « Phuture » laissés en anglais
    symptome: sans-aide | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 50 | ecran.vu: 50 | cible: grille de la boîte à rythmes | geste.type: touche
    attendu: dans l'appli en arabe, la case 1 à droite | obtenu: case 1 et « ملء » à gauche (sens gauche-droite)
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 53 | vu: بقيت هنا خانة منقّطة واحدة (1) | attendu: بقيت هنا خانة منقّطة واحدة
  - ecran: 62 | vu: باد «جرس» (جرس) | attendu: باد «جرس»
  - ecran: 48 | vu: الطقم هو طقم أولى أسطوانات الأسيد | attendu: طقم أولى أسطوانات الأسيد
  - ecran: 14 | vu: Dorian / Phrygian / Blues / Squelchy | attendu: mots traduits ou expliqués
defauts-avant:
  - aucun défaut visé donné par le brief
gestes joués: 79 / 150 (fin envoyée)

fin-tranche: non (dernier step=drums_hear, done=11)
