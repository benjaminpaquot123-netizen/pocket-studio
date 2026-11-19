code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: curieux | lang: ar | device: tablet8 | font: 130 | mode: simple
mission: tuto hard_techno ch.1-7 | buts: - (aucun id donné par le brief)
promesse: id=- | texte: aucune promesse donnée par le brief | ecran: - | tenue: pas vue | pourquoi: brief sans promesse
chapitres:
  - ch1 (écrans 1-16) : compris du premier coup
  - ch2 (17-35) : compris du premier coup
  - ch3 (36-74) : compris du premier coup
  - ch4 (75-86) : hésitation (bouton du tuto « انسخ إلى المازورات التالية » sans effet, passé par la puce « نسخ »)
  - ch5 (87-98) : hésitation (compteur de notes pointillées incohérent, notes posées qui disparaissent de la vue)
  - ch6 (99-105) : compris du premier coup
  - ch7 (106-117) : compris du premier coup ; 117 gestes sur 181
hesitations:
  - ecran: 85 | cherchais: pourquoi le bouton orange du tuto ne fait rien | regarde: la puce verte « نسخ » dans l'éditeur
  - ecran: 88 | cherchais: combien de notes il reste vraiment (3, 2, 1, 4 ?) | regarde: la bannière « بقيت … نوتات منقّطة »
  - ecran: 91 | cherchais: mes deux notes déjà posées | regarde: la grille après « المس هنا »
abandon: jamais
incidents-banc:
  - geste: - | attente: - | vu: aucun écran vide ; hors banc j'ai seulement lu les PNG-grilles de mon siège et les deux gabarits
defauts:
  - geste: 85 | ecran.vu: 85 | cible: bouton ⊕ «   انسخ إلى المازورات التالية » | geste.type: touche
    attendu: le motif se copie et l'étape avance | obtenu: rien ne bouge ; seule la puce « نسخ » (geste 86) a marché. Au ch5 (écran 97-98) le même bouton a marché
    symptome: rien | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 88 | ecran.vu: 88 | cible: bouton « اللحن · المازورة … بقيت نوتتان منقّطتان » | geste.type: touche
    attendu: la consigne dit 3 notes, le compteur descend 3,2,1 | obtenu: 2, puis « (1) », puis « بقيت 4 نوتات منقّطة، أعلى ↑ »
    symptome: incompris | gravite: finition | revu: oui: ecran 94-96 (2/8 : 3,2,1 cohérent) | banc?: non | promesse: -
  - geste: 91 | ecran.vu: 91 | cible: « المس هنا » | geste.type: touche
    attendu: voir mes notes déjà posées et la suivante | obtenu: la grille a sauté, mes deux notes ne sont plus visibles, seule la note pointillée reste
    symptome: perdu | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 1 | ecran.vu: 22 | cible: « البادات تُصدر الصوت… ضغطة مطوّلة: تغيير الصوت. » | geste.type: touche
    attendu: texte d'aide et puce « الطقم: استوديو » lisibles | obtenu: coupés sous le panneau du tuto (police 130)
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 45 | ecran.vu: 45 | cible: « ▸ لماذا؟ » | geste.type: touche
    attendu: l'explication s'ouvre sous la consigne | obtenu: la consigne devient [coupée]
    symptome: texte-coupe | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 11 | ecran.vu: 11 | cible: « F♯ Phrygian » | geste.type: touche
    attendu: la tonalité écrite comme sur les boutons (« فا♯ ») | obtenu: lettres latines ; la carte de gamme écrit مي♭/لا♭/سي♭ et le clavier لا♯/صول♯/ري♯
    symptome: texte-faux | gravite: finition | revu: oui: ecran 94 (clavier en ♯) | banc?: non | promesse: -
  - geste: 17 | ecran.vu: 17 | cible: puce « • الكورد … » | geste.type: touche
    attendu: définitions des mots du chapitre (kick, rumble) | obtenu: une définition de l'accord alors qu'il n'y en a pas au ch2
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 35 | vu: الكلاب | attendu: التصفيق (aussi « الكلاب يفرقع كالسوط » écran 45)
  - ecran: 36 | vu: 2 مازورات | attendu: مازورتين
  - ecran: 17 | vu: الـrumble | attendu: الهدير (écran 116 dit « الهدير », écran 107 « الـrumble »)
  - ecran: 75 | vu: الـhoover / Hoover | attendu: الهوفر
  - ecran: 12 | vu: Phrygian, Dorian, Blues | attendu: فريجي، دوري، بلوز
  - ecran: 22 | vu: Hard techno · الطبل الكبير 1 | attendu: هارد تكنو · الطبل الكبير 1
  - ecran: 23 | vu: خانة منقّطة واحدة (1) | attendu: خانة منقّطة واحدة
  - ecran: 106 | vu: البيس | attendu: الباص (comme l'onglet « خط باص »)
  - ecran: 104 | vu: المس «سمِّ الجزء «غروف»» | attendu: guillemets imbriqués à simplifier
  - ecran: 116 | vu: بلا الهدير | attendu: بلا هدير
defauts-avant:
  - aucun défaut visé donné par le brief

fin-tranche: non (dernier step=map_phrases, done=23)
