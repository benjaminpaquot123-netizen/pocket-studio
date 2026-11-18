# Tour 8, lot K « clavier200 » (Clavier à grande police) : rapport

Branche : eq/tour-8-clavier200, partie de l'intégration ccr-80af1a1b-mn0wec (91ae5964).
Défaut visé : t4-26 (prise au clavier, téléphone à 200 %, Mode Simple).

Phrase d'acceptation (mot pour mot) : « en fr, deux mains, phone à 200 %, Mode Simple : depuis le Clavier, je touche les touches 62 et 65 puis « Enregistrer ma mélodie » et la prise démarre. »

Mesure sur la base (rouge) : phone 360 × 780, Mode Simple, Clavier d'un morceau neuf.
- 130 % : touches 62 et 65 coupées (dessinées de 653 à 834 px, barre du Guide à 834) ;
- 200 % : touches hors écran (place de la page : 392 à 751 px, soit ~240 dp ; carte de prise 91 dp + touches 150 dp + en-tête ne tiennent pas) ;
- 100 % et tablet8 à 200 % : déjà bons.

Ce qui change (`SynthScreen.kt`, Clavier debout, seulement Mode simple + téléphone + police ≥ 115 %) :
- la carte de prise vient en premier, les touches juste dessous, puis la carte « Clavier » (titre, octave, Accords, Pads, dispositions) et le choix Clavier / Notes ;
- les touches prennent la place qui reste au-dessus du bas de la page, entre 110 et 150 dp (≈ 122 dp à 200 % sur 360 dp, 150 dp à 130 %) ;
- 100 %, tablette, paysage et Mode Pro : inchangés. Aucun texte touché, `RecordBar.kt` non touché ;
- le nom d'une touche blanche (« Do ») rétrécit à la largeur de sa touche au lieu d'être coupé (LanguagesScanTest simple_keys_fr_2.0 le voyait coupé).

Décision : docs/decisions/tour-8-clavier200.md (à côté des touches, elles tombaient à ~14 dp de large : écarté).

Preuves :
- rouge puis vert : Tour8KeyboardBigFontTest (phone 360 à 100 / 130 / 200 %, tablet8 à 200 %) : touches 62, 65 et « Enregistrer ma mélodie » entières, au-dessus de la barre du Guide, sans défiler ;
- tests ciblés `*synth*`, `*Synth*`, `*RecordBar*` : verts ;
- captures ré-enregistrées : `scale130/fr_synth.png` et `scale130/fr_record_bar.png` (le « Do » tient dans sa touche) ; `fr_record_bar` est hors des noms `synth*` / `*keyboard*`, signalé au chef ;
- rejeu t4-26-prise-clavier : voir la dernière ligne.

Laissé de côté (pas pris) : Tour6GuideCoquilleLookTest audioPagesFrench100 et audioPagesTurkish130 rougissent aussi sur la base (écart = trace de toucher seulement), hors de mes fichiers.

tests: *Tour8KeyboardBigFontTest* *SynthLayoutTest* *RecordBar*

chemin: Clavier, Simple, phone 360 à 200 % | test: Tour8KeyboardBigFontTest#phone360At200
chemin: Clavier, Simple, phone 360 à 130 % | test: Tour8KeyboardBigFontTest#phone360At130
chemin: Clavier, Simple, phone 360 à 100 % | test: Tour8KeyboardBigFontTest#phone360At100
chemin: Clavier, Simple, tablet8 à 200 % | test: Tour8KeyboardBigFontTest#tablet8At200

État : tenté, prêt pour contrôle (pas « corrigé » : un siège neuf doit le constater).
