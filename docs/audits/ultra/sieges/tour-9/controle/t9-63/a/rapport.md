code testé : 2b18313c4779235edb463fc138d7edc96310c025
joué par : sous-agent neuf

siege: a | profil: curieux | lang: ar | device: tablet8 | font: 100 | mode: simple
mission: finir les chapitres 7-13 du tuto speed_garage | buts: chapitre-7-a-13 (fiche du chef, pas d'ids donnés)
promesse: id=- | texte: أحسنت، مقطوعتك اكتملت! يمكنك الاستماع إليها كلها من جديد ومواصلة تغييرها. | ecran: 65 | tenue: oui | pourquoi: les 7 chapitres finis, partage M4A fait, écran de fin du tuto affiché
chapitres:
  - 7 فهم الأجزاء : hésitation (définition du « فاصل » contradictoire, voir défaut 2)
  - 8 ابنِ أجزاءك : compris du premier coup
  - 9 رتّب أجزاءك : compris du premier coup (liste « ما زال ناقصًا » très utile)
  - 10 الانتقالات : compris du premier coup (cases barrées et case pointillée guident bien)
  - 11 المؤثرات : compris du premier coup
  - 12 المزج: المستويات : hésitation (la consigne parle de l'onglet «مزج», l'onglet s'appelle «المكس»)
  - 13 صدّر مقطوعتك : compris du premier coup
hesitations:
  - ecran: 56 | cherchais: l'onglet «مزج» demandé par la consigne | regarde: barre du bas, seul «المكس» ressemble ; deviné
  - ecran: 10 | cherchais: si le basse reste ou se tait dans le «فاصل» | regarde: consigne, puce et «لماذا؟» se contredisent
abandon: jamais
incidents-banc:
  - geste: 61 | attente: 0 | vu: ligne « image (rond coché) @ 0,0 » sur l'écran Partage, rien de tel sur l'image
defauts:
  - geste: 55 | ecran.vu: 56 | cible: دورك: في تبويب «مزج»، اخفض قليلًا مساري «الكوردات» و«اللحن». | geste.type: touche
    attendu: la consigne nomme l'onglet comme il est écrit en bas («المكس») | obtenu: elle dit «مزج», aucun onglet ne porte ce nom ; le titre du chapitre dit aussi «المزج»
    symptome: texte-faux | gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: -
  - geste: 9 | ecran.vu: 10 | cible: • الاستراحة: لحظة أهدأ، يصمت الطبل الكبير والباص. | geste.type: touche
    attendu: une définition du «فاصل» qui colle à la consigne | obtenu: la consigne dit «والبيس يبقى وحده مع الأورغن», la puce dit que le basse se tait, et le mot «الاستراحة» n'est pas le nom de la partie «فاصل» ; au chapitre 8 la partie «فاصل» est créée « بدون طبل كبير للاستوديو, سنير » (le basse reste) : la puce a tort
    symptome: incompris | gravite: finition | revu: oui: ecran 28 | banc?: non | promesse: -
  - geste: 8 | ecran.vu: 9 | cible: icône ← | geste.type: touche
    attendu: le bouton «السابق» garde son nom quand l'étape est réussie | obtenu: il ne reste qu'une flèche → seule à droite de « استمع إلى المثال », sans mot
    symptome: sans-nom | gravite: finition | revu: oui: ecran 35 | banc?: non | promesse: -
  - geste: 56 | ecran.vu: 57 | cible: texte « اضغط ▶ » | geste.type: touche
    attendu: les graduations des curseurs lues «−12», «−24», «−∞» | obtenu: écrites «12−», «24−», «∞−» (signe moins collé à droite) ; même chose au Partage «(14−)»
    symptome: texte-faux | gravite: finition | revu: oui: ecran 61 | banc?: non | promesse: -
  - geste: 56 | ecran.vu: 57 | cible: bouton (haut-parleur et ondes) [coupé, allumé, état « يعمل »] | geste.type: touche
    attendu: voir les boutons son / ▶ sous chaque curseur | obtenu: le panneau du tuto les coupe à moitié tant qu'il est ouvert
    symptome: cache | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 64 | ecran.vu: 65 | cible: صدّر مقطوعتك الفصل 13/13 · الخطوة 1/1 · سهل | geste.type: touche
    attendu: « تم » comme à la fin des autres chapitres | obtenu: l'en-tête garde « الخطوة 1/1 » sur l'écran de fin
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 64 | vu: OK | attendu: حسنًا
  - ecran: 61 | vu: مثل موسيقى الـStreaming | attendu: مثل موسيقى البث
  - ecran: 15 | vu: بدون باص, اللحن (virgule latine dans les listes, écrans 15 à 27) | attendu: بدون باص، اللحن
  - ecran: 41 | vu: ضربات زائدة هنا (مشطوبة): 1 | attendu: ضربة زائدة واحدة هنا (مشطوبة)
  - ecran: 10 | vu: البيس / الباص (deux orthographes du même mot) | attendu: une seule, «الباص»

fin-tranche: oui (ROUTE ligne 291)
