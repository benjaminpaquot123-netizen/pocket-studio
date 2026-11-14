siege: d | profil: DJ club | lang: ja | device: phone411 | font: 100 | mode: pro
mission: exports | buts atteints: 2 sur 3
hesitations:
  - ecran: 9 | cherchais: la section master (cible, limiteur) | regarde: tranches du mixeur glissées à droite, onglet その他 (14), feuille シェア (15), trouvée seulement via 「ほかの書き出し」
  - ecran: 40 | cherchais: un retour après 「曲を書き出す」 | regarde: le bouton, le bas de page (44), le haut de la section 書き出す (45)
  - ecran: 57 | cherchais: quelle cible sera vraiment appliquée à l'export | regarde: les deux rangées 「目標ラウドネス」 et 「音量の目標（曲全体）」
surprise: deux sélecteurs de cible contradictoires sur le même écran, et aucun LUFS/TP mesuré après export
abandon: j'aurais abandonné à l'écran 43 parce que 「曲を書き出す」 ne réagit pas et rien n'indique que l'export passe par シェア
defauts:
  - ecran: 57 | cible: 「ストリーミング · −14」 et 「クラブ · −9」 | geste: touche
    attendu: un seul choix de cible, les deux rangées synchronisées | obtenu: en haut −14 coché + texte « −14：Spotify… », en dessous クラブ −9 coché
    symptome: incompris | gravite: grave | bloque: non
  - ecran: 40 | cible: 「曲を書き出す」 (onglet ミックス) | geste: touche
    attendu: progression puis fichier | obtenu: 2 appuis + 45 s d'attente, rien ne change, aucun message
    symptome: rien | gravite: grave | bloque: non
  - ecran: 62 | cible: toast 「準備完了：テクノ-曲-20261104-202122.wav」 | geste: touche
    attendu: bilan de l'export (LUFS intégré, true peak, format) pour streaming et club | obtenu: seulement le nom du fichier, les deux versions indiscernables
    symptome: sans-aide | gravite: grave | bloque: non
  - ecran: 20 | cible: 「クラブ · −7.5」 / 「クラブ · −9」 | geste: touche
    attendu: une seule valeur club, avec unité LUFS | obtenu: −7.5 en haut, −9 en dessous, aide (24) dit « −9 くらい » ; aucun « LUFS » sur les puces
    symptome: incompris | gravite: moyen | bloque: non
  - ecran: 49 | cible: 「Partager « ???-?-20261104-202008.wav »」 | geste: touche
    attendu: nom lisible « テクノ-曲-… » et distinct selon la cible | obtenu: caractères japonais remplacés par ???, même nom pour streaming et club
    symptome: coupe | gravite: moyen | bloque: non
  - ecran: 22 | cible: curseurs sous 「マスター音量」, 「アタック」, 「リリース」 | geste: glisse
    attendu: un nom TalkBack par curseur | obtenu: « curseur sans nom » (aussi au 19)
    symptome: sans-nom | gravite: moyen | bloque: non
  - ecran: 35 | cible: 「周波数」 (fréquence d'échantillonnage) | geste: touche
    attendu: « サンプルレート » ; états TalkBack cohérents | obtenu: 「周波数」 ; 「クローズ/オープン」 (4) vs 「開いた状態」 (7) ; 「早く」 pour « plus tôt » (12) ; 「Mixeur」 non traduit (55)
    symptome: incompris | gravite: mineur | bloque: non
