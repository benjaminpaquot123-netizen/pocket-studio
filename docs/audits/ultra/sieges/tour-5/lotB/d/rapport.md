siege: d | profil: t5-Bd etudiante-arabophone-tablette-couchee | lang: ar | device: tablet11Land | font: 100 | mode: simple
mission: simple | buts: son, partie, jamais-pro
promesse: id=t5-Bd-1 | texte: بسيط للبداية: 5 تبويبات وأزرار كبيرة وكلمات يومية. | ecran: 2 | tenue: non | pourquoi: en Simple restent « Sub bass », « Loop », « Pads », « Kick », « Snare », « Mix », « Bridge », « Groove ».
promesse: id=t5-Bd-2 | texte: الأغنية: Intro → لازمة → مقطع → لازمة → مقطع → لازمة → النهاية: نحو 3 د. | ecran: 18 | tenue: non | pourquoi: flèches → à l'envers dans la phrase arabe, moins collé après le chiffre au mixeur (« 12− »), grille de pas tantôt de droite à gauche tantôt de gauche à droite.
promesse: id=t5-Bd-3 | texte: إعادة | ecran: 24 | tenue: non | pourquoi: après un « تراجع », « إعادة » s'affiche pile à 261,42 et « تراجع » glisse à 333,42 : mon 2e appui a rétabli.
hesitations:
  - ecran: 19 | cherchais: où ajouter une 2e partie | regarde: la longue liste des parties ; deux glissés avant « إضافة في النهاية: »
  - ecran: 21 | cherchais: pourquoi trois lignes « C · لازمة » de suite | regarde: « مازورة واحدة · Mix », « 6 مازورات · Mix », sans aide
  - ecran: 29 | cherchais: le coup que je venais de poser | regarde: case « الخطوة 1 » encore « فارغ » pendant la lecture (le coup était en mesure 4)
abandon: jamais
incidents-banc:
defauts:
  - geste: 24 | ecran.vu: 24 | cible: bouton « إعادة » @ 261,42 | geste.type: touche
    attendu: « تراجع » reste sous le doigt pour annuler encore, « إعادة » à côté | obtenu: « إعادة » prend la place exacte de « تراجع » ; mon 2e appui remet la partie « D · Bridge »
    symptome: perdu
    gravite: contourne-en-2-gestes | revu: oui: ecran 26 | banc?: non | promesse: t5-Bd-3
  - geste: 17 | ecran.vu: 18 | cible: texte « الأغنية: Intro → لازمة → مقطع → لازمة → مقطع → لازمة → النهاية: نحو 3 د. » | geste.type: touche
    attendu: flèches ← qui suivent la lecture de droite à gauche | obtenu: flèches → qui pointent vers le début de la phrase
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t5-Bd-2
  - geste: 37 | ecran.vu: 38 | cible: curseur « مستوى الخرج » [état « −1.0 dB », à 76 %] @ 1209,513 | geste.type: touche
    attendu: graduations « −12 », « −24 », « −∞ », « +4 » | obtenu: graduations dessinées « 12− », « 24− », « ∞− », « 4+ », « 6+ » (signe après le chiffre)
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t5-Bd-2
  - geste: 27 | ecran.vu: 28 | cible: touchable « 1 » (nom « الخطوة 1 ») [état « فارغ »] @ 90,495 | geste.type: touche
    attendu: même sens que la grille de l'assistant (écran 18 : « 1 » à droite) | obtenu: pas 1 à gauche, mesures « 1 2 3 4 » et « الطول » de gauche à droite
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: t5-Bd-2
  - geste: 13 | ecran.vu: 14 | cible: bouton « Sub bass ناعم · ممدود » @ 1086,723 | geste.type: touche
    attendu: nom de son en arabe courant | obtenu: « Sub bass », et autour « Reese bass », « Wobble bass », « Growl bass », « Neuro bass »… en anglais
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 34 (piste « باص Sub bass ») | banc?: non | promesse: t5-Bd-1
  - geste: 33 | ecran.vu: 34 | cible: interrupteur « Loop » (nom « كرّر الجزء بدل تشغيل الأغنية ») [coché, état « يعمل »] @ 148,114 | geste.type: touche
    attendu: « تكرار » ou « حلقة » | obtenu: gros bouton orange « Loop » en anglais, aussi dans « في تكرار (⟳ Loop) »
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 38 | banc?: non | promesse: t5-Bd-1
  - geste: 13 | ecran.vu: 14 | cible: bouton « باص تناظري تناظري » @ 1338,1096 | geste.type: touche
    attendu: un nom puis une courte description | obtenu: mot répété ; idem « سرب سرب », « آلة وتريات آلة وتريات », « Pad كوردات الكوردات » (écran 16)
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 16 | banc?: non | promesse: -
  - geste: 2 | ecran.vu: 3 | cible: bouton « باص music الأنماط: 7 » [état « مغلق »] @ 960,545 | geste.type: touche
    attendu: nom de famille entièrement traduit | obtenu: moitié arabe, moitié anglais « باص music »
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 4 (« Liquid DnB باص music ») | banc?: non | promesse: t5-Bd-1
  - geste: 18 | ecran.vu: 19 | cible: bouton « C · لازمة مازورة واحدة  ·  Mix » @ 1618,873 | geste.type: touche
    attendu: une ligne « لازمة » comme sur le plan de l'écran 18 | obtenu: trois lignes « C · لازمة » de suite (1, 6, 1 mesures, « Mix »), +/− grisés, rien n'explique « Mix »
    symptome: incompris
    gravite: finition | revu: oui: ecran 22 (même chose pour « G · لازمة 2 ») | banc?: non | promesse: -
mots-faux:
  - ecran: 9 | vu: KICK (الطبلة الكبيرة) | attendu: الطبلة الكبيرة
  - ecran: 11 | vu: Snare / Snare 2 | attendu: سنير (le kick, lui, est traduit « الطبل الكبير 1 »)
  - ecran: 11 | vu: Groove | attendu: الإحساس الإيقاعي
  - ecran: 12 | vu: Swing | attendu: تأرجح
  - ecran: 4 | vu: Loop بيانو | attendu: حلقة بيانو
  - ecran: 18 | vu: Bridge | attendu: جسر
  - ecran: 28 | vu: Pads / L 30 / R 50 | attendu: الوسادات / يسار 30 / يمين 50
  - ecran: 23 | vu: + H · Crash / + J · Fill | attendu: صنج / حشوة
  - ecran: 39 | vu: Streaming / Solo | attendu: البث / منفردًا
