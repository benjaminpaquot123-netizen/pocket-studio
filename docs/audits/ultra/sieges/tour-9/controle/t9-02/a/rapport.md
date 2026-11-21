code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: curieux | lang: ar | device: foldable | font: 200 | mode: simple
mission: tuto-facile (acid_house, chapitres 4-9) | buts: -
promesse: id=- | texte: «M4A» يصنع ملفًا خفيفًا لأصدقائك | ecran: 55 | tenue: oui | pourquoi: le bilan dit « ملف M4A: خفيف، يعمل على كل الأجهزة » et le fichier part vers Files
hesitations:
  - ecran: 19 | cherchais: le bouton « أنشئ الجزء «مقدمة» » après « أرني » | regarde: la carte du tuto était repliée, seul un bandeau « ها هو! دورك الآن. » avec « إيقاف »
  - ecran: 31 | cherchais: la liste « إضافة » pour ajouter la partie suivante | regarde: la colonne étroite revient sur la ligne ajoutée, il faut glisser deux fois
  - ecran: 57 | cherchais: la fin du tuto | regarde: « بقيت لك 4 خطوات » (chapitres 1-3 que je n'ai pas joués : amenée au chapitre 4)
abandon: jamais
incidents-banc:
  - geste: - | attente: 0 | vu: aucun outil hors human-act.py ; seul le dossier du siège a été lu (start.txt, journal.txt pour les numéros de gestes)
defauts:
  - geste: 32 | ecran.vu: 33 | cible: bouton « + C · غروف » | geste.type: touche
    attendu: après un ajout, la liste « إضافة » reste sous le doigt pour ajouter la partie suivante | obtenu: la colonne remonte sur la ligne ajoutée, les boutons « + » sortent de l'écran ; 6 parties = 11 glissés en plus (#30-#43)
    symptome: cache | gravite: contourne-en-11-gestes | revu: oui: ecran 40 | banc?: non | promesse: -
  - geste: 19 | ecran.vu: 20 | cible: texte « إيقاف » | geste.type: touche
    attendu: arrêter la démo « أرني » | obtenu: le doigt a touché « التالي » : message « هذه الخطوة لم تُنجز بعد. المس «التالي» مرة أخرى لتخطيها على أي حال. »
    symptome: mauvais-ecran | gravite: finition | revu: non | banc?: temps | promesse: -
  - geste: 18 | ecran.vu: 19 | cible: bouton « + جزء جديد (نسخة من A · دروب) » | geste.type: touche
    attendu: le nom du bouton en entier | obtenu: on lit « يد. (نسخة من A · دروب) », le début « + جزء جد » est mangé ; « إضافة: » coupé en haut aussi
    symptome: texte-coupe | gravite: finition | revu: oui: ecran 44 | banc?: non | promesse: -
  - geste: 9 | ecran.vu: 10 | cible: texte «  مقدمة » | geste.type: touche
    attendu: reconnaître chaque partie à sa couleur | obtenu: مقدمة, غروف et النهاية ont le même gris dans la légende et les cases « 16 »
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 55 | ecran.vu: 56 | cible: icône (dossier ouvert) « أغنيتي تم الحفظ » ✓ | geste.type: touche
    attendu: mon projet garde son nom « مشروعي 1 » | obtenu: après l'export, le projet s'appelle « أغنيتي » sans que je l'aie renommé
    symptome: incompris | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 55 | vu: OK | attendu: حسنًا
  - ecran: 55 | vu: مثل موسيقى الـStreaming | attendu: مثل منصات البث
  - ecran: 25 | vu: طبل كبير للاستوديو, تصفيق, هاي هات مفتوح (virgules latines) | attendu: «،»
  - ecran: 17 | vu: المس «أنشئ الجزء «مقدمة»». (guillemets imbriqués) | attendu: المس «أنشئ الجزء مقدمة».
  - ecran: 48 | vu: الاستراحة | attendu: الفاصل (nom de la partie partout ailleurs)
  - ecran: 51 | vu: مقطوعتك | attendu: أغنيتك (mot utilisé dans le reste du tuto)
chapitres:
  - 4 | compris du premier coup | 1 geste
  - 5 | compris du premier coup | écoute seulement
  - 6 | hésitation | « أرني » puis « إيقاف » a sauté vers « التالي » (écran 20)
  - 7 | hésitation | ajouts en colonne étroite, 11 glissés en plus
  - 8 | compris du premier coup | écoute seulement (aucun bouton à tourner malgré le titre)
  - 9 | compris du premier coup | export M4A vers Files réussi
fin: mission finie au chapitre 9/9, 56 gestes sur 150

fin-tranche: non (dernier step=share, done=20 sur 24, journal ligne 272)
