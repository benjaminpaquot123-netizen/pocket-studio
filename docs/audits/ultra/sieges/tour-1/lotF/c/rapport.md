siege: c | profil: ingénieur mastering | lang: pl | device: foldable | font: 100 | mode: pro
mission: exports | buts atteints: 2 sur 3
hesitations:
  - ecran: 34 | cherchais: le résultat après « Eksportuj takty: 8 » (Miks) | regarde: sous le bouton, en haut de la section « EKSPORTUJ », onglet « Więcej »
  - ecran: 21 | cherchais: quelle cible de loudness s'applique vraiment | regarde: « Głośność docelowa » (Wył.) puis « Docelowa głośność (cały utwór) » (Spotify −14)
  - ecran: 43 | cherchais: le rapport LUFS / true peak de l'export partagé | regarde: feuille « Gotowe do udostępnienia? », onglet « Więcej », enfin bas de « EKSPORTUJ » (trouvé écran 63)
surprise: les versions club et streaming affichent exactement le même rapport : « Poziom: jak muzyka w streamingu (−14) », « −12,9 LUFS · szczyt — dBTP »
abandon: j'aurais abandonné à l'écran 83 parce que rien ne prouve que la version club diffère de la version streaming, et le true peak n'est jamais mesuré
defauts:
  - ecran: 34 | cible: « Eksportuj takty: 8 » | geste: touche
    attendu: export du fichier puis rapport | obtenu: aucun effet ni message, 3 essais (écrans 31, 34, 64) ; seul « Udostępnij » exporte
    symptome: rien | gravite: grave | bloque: non
  - ecran: 63 | cible: « Poziom: jak muzyka w streamingu (−14) » | geste: touche
    attendu: niveau club (Klub −8 / −9 choisis, plafond −0,3 dBTP) | obtenu: rapport streaming −14, identique à l'export streaming (écran 83)
    symptome: incompris | gravite: grave | bloque: oui
  - ecran: 63 | cible: « −12,9 LUFS · szczyt — dBTP » | geste: touche
    attendu: true peak chiffré, LUFS proche de la cible annoncée | obtenu: true peak « — » ; −12,9 LUFS pour une cible −14 (1,1 LU d'écart, sans explication)
    symptome: incompris | gravite: grave | bloque: oui
  - ecran: 48 | cible: « Klub · −8 » et « Spotify, YouTube · −14 » | geste: touche
    attendu: une seule cible active, les deux sélecteurs synchronisés | obtenu: deux cibles opposées actives en même temps ; « Klub » vaut −8 ici, −9 dans « master pro »
    symptome: incompris | gravite: grave | bloque: non
  - ecran: 7 | cible: « — LUFS · TP — » / « Teraz — · przez 3 s — · od ▶ — LUFS » | geste: touche
    attendu: mesures en direct pendant la lecture | obtenu: que des tirets après 4 s de lecture ; « Głośność: — » sans unité
    symptome: rien | gravite: moyen | bloque: non
  - ecran: 23 | cible: « Część w pętli » / « Eksportuj 1 takt » | geste: touche
    attendu: mêmes réglages que la feuille de partage (0:16, takty: 8, M4A) | obtenu: 1 takt et WAV par défaut dans Miks ; deux réglages de fondu (« Fade-out » et « Początek i koniec »)
    symptome: incompris | gravite: moyen | bloque: non
  - ecran: 83 | cible: rapport sous « Eksportuj takty: 8 » | geste: glisse
    attendu: durée, plafond appliqué, historique des exports pour comparer | obtenu: seul le dernier export affiché, pas de durée ; nom de fichier sans version (club/streaming)
    symptome: perdu | gravite: moyen | bloque: non
  - ecran: 12 | cible: « Zwolnienie: ×1,0 » / « Atak: 100 % look-ahead » / « Głośność główna » | geste: glisse
    attendu: release en ms, réduction de gain du limiteur visible, curseurs nommés | obtenu: multiplicateur « ×2,0 » sans unité, aucun GR, curseurs « sans nom » (TalkBack)
    symptome: sans-nom | gravite: moyen | bloque: non
  - ecran: 42 | cible: « M?j-pierwszy-bit-p?tla-20261104-202801.wav » | geste: touche
    attendu: nom avec ó/ę (comme dans le message « Gotowe: ») ou translittéré | obtenu: « ? » à la place des lettres polonaises dans la feuille de partage
    symptome: coupe | gravite: mineur | bloque: non
  - ecran: 25 | cible: « 24 bitów », « Eksportuj takty: 8 », « Szeroko », « na miejscu », « Kompresor szyny », « mierzony po kompresji » | geste: touche
    attendu: « 24 bity », « Eksportuj 8 taktów », « Szerokość », « aktywny », « kompresor sumy », « po kodowaniu stratnym » | obtenu: fautes d'accord, mot tronqué, calques, « kompresja » ambiguë (aide écran 17)
    symptome: incompris | gravite: mineur | bloque: non
