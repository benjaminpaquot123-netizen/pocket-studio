# Relecture des traductions : tuto_techno (ja, ko, ar, hi)

- Périmètre : `project-model/src/main/resources/i18n/{ja,ko,ar,hi}/tuto_techno.json`, seulement les valeurs modifiées depuis la base `3cd69a96` (branche `eq/tour-7-p67-techno`).
- Date : 2026-11-18. Relecteur neuf, qui n'a pas écrit ces traductions.
- Ce qui a été vérifié : le sens, le registre, les noms d'interface entre guillemets (comparés aux autres fichiers i18n de la même langue) et la grammaire.
- Les raccourcis voulus ne comptent pas comme des fautes : ja sans « あなたの番 », hi « व ».

## ja : 0 grave, 1 léger

- Léger : grammaire maladroite.
  - Clé : « À toi : touche la note du bas de chaque accord en pointillé de la mesure 1 : tout l'accord se pose. C'est une note longue, jouée grave et aiguë. »
  - Actuel : « …低くて高い、長い音だよ。 »
  - Proposé : « …低い音と高い音を重ねた長い音だよ。 »
  - Facultatif, dans la même entrée : « コード全体が置ける » devient « コード全体が置かれる ».
- Bon : « Drop B » et « Mini-break » restent en lettres latines, comme dans le Mode pro.

## ko : 0 grave, 1 léger

- Léger : la phrase n'a pas de verbe pour « le tic, le rim et l'accord ».
  - Clé : « À toi : crée la Montée : sans kick ni basse, le tic, le rim et l'accord, et le dernier temps se tait. »
  - Actuel : « 직접 해 보세요: 빌드업을 만드세요. 킥과 베이스 없이 틱, 림, 코드, 그리고 마지막 박은 쉬어요. »
  - Proposé : « 직접 해 보세요: 빌드업을 만드세요. 킥과 베이스 없이 틱, 림, 코드만 울리고, 마지막 박은 쉬어요. »
- Remarque hors périmètre (déjà là avant cette branche, dans tous les tutos ko) : les textes écrivent la partie « Fin » 엔딩, mais le bouton affiche 끝 (clé « Fin (partie) »). C'est au chef de décider.

## ar : 0 grave, 4 légers

- Léger (a) : « الكلاب » pour clap se lit « les chiens ».
  - Entrées touchées : les 3 « crée la Montée » (la 303 continue, avec la 303, sans kick plain) et « La tension monte… » (« والكلاب يجنّ »).
  - Proposé : « التصفيق », déjà utilisé dans l'entrée de la partie « Montée ».
  - Ce choix existait déjà : 33 emplois à la base, aussi dans d'autres tutos ar. Il vaut mieux trancher pour tout le projet.
- Léger (b) : « الريف » pour rave se lit « la campagne » et se confond avec ريف, qui veut dire riff.
  - Clé « À toi : choisis la couleur du morceau, rave : Ré mineur… » : actuel « على طريقة الريف », proposé « على طريقة الرايف ».
  - Clé « Mineur : … la grille de toutes les raves. » : actuel « تسلسل كل حفلات الريف », proposé « تسلسل كل حفلات الرايف ».
  - Ce choix existait déjà, par exemple « صفارات ريف ».
- Léger (c) : grammaire des entrées « choisis la couleur ».
  - Après « اختر لون المقطوعة، », l'adjectif devrait être à l'accusatif (ḥāl).
  - Exemples : « داكنًا », « داكنًا جدًا », « هادئًا », « حزينًا لكن مضيئًا ».
- Léger (d) : deux « » se suivent sans verbe qui les lie.
  - Clé : « À toi : mets en « Doux » ce dernier coup du pad « Clap ». »
  - Actuel : « دورك: اجعل هذه الضربة الأخيرة على باد «تصفيق» «ناعم». »
  - Proposé : « دورك: اضبط هذه الضربة الأخيرة على باد «تصفيق» على «ناعم». »

## hi : 0 grave, 1 léger

- Léger : « बार » est ambigu ici, car le mot veut dire à la fois « mesure » et « fois ».
  - Clé : « À toi : sur le pad du « Zap » (un bruit de laser), touche la case en pointillé, une fois toutes les 2 mesures. »
  - Actuel : « …डॉटेड खाना दबाओ, हर 2 बार में एक बार। »
  - Proposé : « …डॉटेड खाना दबाओ: हर 2 बार में एक खाना। »
- Le registre (तुम) et les noms d'interface sont cohérents partout.

## Noms d'interface

Tous les noms entre guillemets correspondent aux autres fichiers i18n de chaque langue :

- les pistes et pads (Mix, Clap, Rim, Caisse, Kick, Charley, Charley ouvert, Rumble, Zap) ;
- les écrans et boutons (Mon morceau, Ajouter, Je le fais, Sortie générale, Volume visé, Club) ;
- les réglages de jeu (Normal, Doux, Force, Roulement, Triolet).

Graves (première passe, relu à 0f06b0cc) : 0

## Deuxième passe : les « Pourquoi ? » ajoutés après (0f06b0cc..3af46263)

Relecteur neuf. 12 valeurs par langue : les 7 « Créer la partie », Rim, Cymbale, Clap, ref, gap. Sens juste, noms d'interface conformes, ja jamais 終了 pour la Fin, ko au style poli.
- ja : 0 grave, 0 léger.
- ko : 0 grave ; léger, make_fin : le bouton dit 끝, la phrase suivante 엔딩 (même remarque projet que plus haut).
- ar : 0 grave ; légers : accord de مقدمة (خفيفة ومنتظمة / يستخدمها), « لوحة » au lieu de « باد » pour Cymbale, espace dans « للـ DJ ».
- hi : 0 grave ; légers : tournure de Cymbale (« कहीं और रखो तो ये हर बार में दोहराया जाता »), « Riff » latin contre « रिफ़ ».

relu : 3af46263

Verdict : 0 bloquant, 0 grave
