siege: d | profil: t5-Dd fan-rap-marocain-appli-arabe | lang: ar | device: phone | font: 100 | mode: simple
mission: remix-voix | buts: separe, decoupe, pose
promesse: id=t5-Dd-1 | texte: « الجُمل » / « قطعتان مختارتان · المجموع 0:05 » / « قطعة واحدة » | ecran: 35 | tenue: oui | pourquoi: en mode الجُمل, toucher la tuile « 2 » la coche (#34), « على مسار » s'allume et pose 2 bouts ; repasser sur « قطعة واحدة » (#38) ramène début/fin et l'envoi
promesse: id=t5-Dd-2 | texte: « أغنيتي » (fil « أغنيتي ريمكس » en haut du Remix) | ecran: 40 | tenue: non | pourquoi: dans l'onglet أغنيتي, rien ne mène au Remix (vu pendant la séparation, écran 11, et après la découpe, écrans 40 et 43) ; il faut passer par « المزيد » puis « ريمكس »
promesse: id=t5-Dd-3 | texte: « →  أغنيتي  ريمكس » | ecran: 34 | tenue: oui | pourquoi: tout est en miroir (onglets, flèche retour à droite, tuiles 1→6 de droite à gauche, OK à gauche) ; seule la forme d'onde reste de gauche à droite, ce qui est normal pour une timeline
hesitations:
  - ecran: 11 | cherchais: où suivre la séparation en cours depuis أغنيتي | regarde: les pistes et les parties, aucun lien Remix
  - ecran: 19 | cherchais: la liste des phrases après avoir choisi الجُمل | regarde: la forme d'onde, la liste est cachée dessous et le glisser ne fait rien
  - ecran: 27 | cherchais: comment cocher une phrase | regarde: le bloc vert de la forme d'onde, puis le petit chiffre « 1 » en haut
abandon: jamais
incidents-banc:
defauts:
  - geste: 39 | ecran.vu: 40 | cible: onglet « أغنيتي » [sélectionné] | geste.type: touche
    attendu: depuis أغنيتي, une carte ou un bouton qui ramène au Remix en cours | obtenu: aucune trace du Remix, seulement مساراتي et الأغنية ; il faut المزيد → ريمكس
    symptome: perdu
    gravite: contourne-en-2-gestes | revu: oui: ecran 43 | banc?: non | promesse: t5-Dd-2
  - geste: 19 | ecran.vu: 26 | cible: texte « حدّد قطعًا لإرسالها. » | geste.type: glisse
    attendu: après الجُمل, voir la liste des phrases (« قطعك · 6 ») et les outils | obtenu: liste et ligne de mode cachées sous la forme d'onde ; glisser dessus ne fait rien (#19, #22, #25, #31), seul un glisser parti du titre « المازورات 2 إلى 5 » marche (#32)
    symptome: cache
    gravite: contourne-en-5-gestes | revu: non | banc?: non | promesse: t5-Dd-1
  - geste: 26 | ecran.vu: 27 | cible: titre « المازورات 2 إلى 5 » | geste.type: touche
    attendu: toucher le bloc vert d'une phrase dans la forme d'onde la coche | obtenu: rien ; seul le petit rond numéroté au-dessus coche (#27)
    symptome: rien
    gravite: contourne-en-1-gestes | revu: oui: ecran 31 | banc?: non | promesse: -
  - geste: 27 | ecran.vu: 28 | cible: texte « قطعة واحدة مختارة · المجموع 0:03 » | geste.type: touche
    attendu: le rond « 1 » qui coche la phrase a un nom lisible | obtenu: il coche bien mais n'a aucun nom (le banc ne voit rien sous le doigt)
    symptome: sans-nom
    gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 39 | ecran.vu: 40 | cible: texte « أغنيتك فارغة: أضف أجزاءك بترتيب عزفها (Intro، مقطع، لازمة…). » | geste.type: touche
    attendu: cohérent avec le message « 2 قطع على المسار الصوتي الجديد « الغناء 1 »، في مكانها من الأغنية. » | obtenu: « أغنيتك فارغة » alors que je viens de poser 2 bouts dans la chanson
    symptome: incompris
    gravite: finition | revu: oui: ecran 43 | banc?: non | promesse: -
  - geste: 4 | ecran.vu: 5 | cible: bouton « المرشد  · ضع إيقاعًا 0 % » [état « مطويّ »] | geste.type: touche
    attendu: un guide qui parle du remix (séparer, découper) | obtenu: le guide dit « ضع إيقاعًا » (mets un rythme) pendant tout le remix
    symptome: incompris
    gravite: finition | revu: oui: ecran 40 | banc?: non | promesse: -
mots-faux:
  - ecran: 3 | vu: Bass (« إلى طبول وBass وغناء ») | attendu: باص (comme le bouton « باص » de l'écran 17)
  - ecran: 17 | vu: على الـPads | attendu: على اللوحات / على الأزرار
  - ecran: 18 | vu: Transients | attendu: الضربات / بدايات الأصوات
  - ecran: 34 | vu: Loop | attendu: حلقة / تكرار
  - ecran: 36 | vu: 2 قطع | attendu: قطعتان
  - ecran: 13 | vu: مستوى الـMaster | attendu: مستوى الصوت الرئيسي
  - ecran: 44 | vu: Snare / Tom منخفض / Maracas / Conga | attendu: سنير / طبلة توم منخفضة / ماراكاس / كونغا (en lettres arabes)
  - ecran: 40 | vu: Intro | attendu: مقدمة
  - ecran: 47 | vu: REC | attendu: تسجيل
