siege: c | profil: t6-Bc arabophone-rtl-connait-les-applis | lang: ar | device: phone | font: 100 | mode: pro
mission: libre | buts: son, trace, partage
promesse: id=t6-Bc-1 | texte: « محترف للذهاب أبعد: كل الأدوات، البنية والأصوات والمزج والعزف المباشر. » | ecran: 3 | tenue: oui | pourquoi: accueil → « البدء من الصفر » → « بسيط أم محترف؟ » → « محترف » : projet ouvert en Pro en 2 gestes, sans impasse, tout en arabe et de droite à gauche
promesse: id=t6-Bc-2 | texte: « مرّر Fader للمستوى؛ اضغط على اسم مسار لمؤثراته ومنحنياته. » | ecran: 34 | tenue: oui | pourquoi: Fader d'Acid 303 à « -10.5 », effet « 1. Distortion » ajouté (Drive 74%) ; après fermeture, puis après Réglages et retour au Mix, « -10.5 » et le badge ✨ « 1 » restent affichés
promesse: id=t6-Bc-3 | texte: « ✏ رسم » | ecran: 33 | tenue: oui | pourquoi: note posée sur la ligne « دو4 » (#16, « 1 note du synthé »), glissée (#17) : sur l'image 33 elle est plus haut et plus à droite
promesse: id=t6-Bc-4 | texte: « اللغة » | ecran: 32 | tenue: oui | pourquoi: passage à English (écran 29), puis retour à « العربية » (écran 32) : tout l'écran revient en arabe
oeil-neuf: q1 | ecran: 41 | image: 41 | touchables d'aspect : REC, + / −, ▶, →, ?, les clips, A à H, ■, la barre du bas ; écart : le rond gris « – » en haut a l'air d'un bouton mais ne fait rien (#35)
oeil-neuf: q6 | ecran: 41 | image: 41 | après ▶ puis ■ : « A المازورة 1/1 » et la ligne « التسجيل: نسخة جديدة / إضافة بعده » restent ; la partie ajoutée dans البنية ne se voit pas ici
oeil-neuf: q9 | ecran: 41 | image: 41 | rien de coupé dans les textes ; la ligne « G » est coupée par le bas, rien n'annonce H en dessous
hesitations:
  - ecran: 30 | cherchais: revenir à l'arabe depuis l'anglais | regarde: la liste « Choose your language » n'a plus la section « langue du téléphone » ; « العربية » n'apparaît qu'après un défilement
  - ecran: 4 | cherchais: poser un coup de Kick dans les pas | regarde: les cases des pas sont coupées par la barre « المرشد », mon toucher n'a rien posé
  - ecran: 35 | cherchais: à quoi sert le rond gris « – » en haut du Live | regarde: le toucher ne change rien
abandon: jamais
incidents-banc:
  - geste: 0 | attente: 0 | vu: banc et selftest du lot B lancés par moi sur consigne (selftest vert, 173/0) ; outils hors human-act : bash (grep, sed, diff sur les sorties du banc et mon dossier de siège), Read sur mes images
  - geste: 21 | attente: 0 | vu: quatre « attends 1 » (#21 à #24) faits pour débloquer une image ; le banc ne les compte pas comme gestes, plafond d'images atteint
defauts:
  - geste: 28 | ecran.vu: 29 | cible: bouton (boussole) « Guide  · ضع إيقاعًا 9 % » ⌃ [état « folded »] | geste.type: touche
    attendu: après le choix « English », toute l'interface en anglais | obtenu: la barre du guide reste à moitié en arabe : « Guide · ضع إيقاعًا 9 % »
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t6-Bc-4
  - geste: 29 | ecran.vu: 30 | cible: texte « Choose your language » | geste.type: touche
    attendu: retrouver tout de suite ma langue (en arabe, la liste commençait par « لغة الهاتف ») | obtenu: en anglais, plus de section langue du téléphone ; « العربية » cachée plus bas, il faut faire défiler
    symptome: cache
    gravite: contourne-en-1-gestes | revu: non | banc?: non | promesse: t6-Bc-4
  - geste: 35 | ecran.vu: 36 | cible: texte « – » @ 135,42 | geste.type: touche
    attendu: un rond dans la barre du haut, à côté du tempo, doit faire quelque chose ou dire ce qu'il est | obtenu: rien ne change à l'écran ni au son
    symptome: incompris
    gravite: finition | revu: non | banc?: non | promesse: -
mots-faux:
  - ecran: 16 | vu: Ghost bass | attendu: باص شبحي
  - ecran: 38 | vu: Loop | attendu: تكرار
  - ecran: 3 | vu: كل الـPads | attendu: كل الوسادات (mélange arabe et anglais ; « هاي هات » traduit mais « Kick », « Snare », « Clap » non)