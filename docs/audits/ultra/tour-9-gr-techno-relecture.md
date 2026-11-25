# Tour 9, lot gr-techno : relecture neuve
Relu : f0e93a57

Relecture par un sous-agent neuf qui n'a pas écrit le code, après la fusion avec origin/eq/ci-int-styles
(`git diff origin/eq/ci-int-styles f0e93a57 -- . ':!docs/audits/ultra/sieges'`, 44 fichiers). Les deux derniers
commits (7dc046ec, f0e93a57) ne touchent pas au code : fichiers d'un ancien lot retirés, siège de contrôle et ligne
« controle: » du rapport. Le siège t9-77 a donc bien joué le code du lot (7dc046ec).

## Ce qui a été vérifié

1. Fusion des traductions : les 28 fichiers `i18n/*/guide.json` et `i18n/*/model.json` sont du JSON valide ; mêmes
   clés dans les 14 langues (669 pour guide, 891 pour model). Par rapport à l'intégration, aucune valeur changée et
   aucune clé perdue sauf les 6 attendues (1 dans guide, 5 dans model : anciens textes français remplacés par le lot).
   Aucune des 6 n'est utilisée ailleurs (la phrase « Un gros synthé épais… » du tuto hard techno est une phrase plus
   longue, traduite dans `tuto_techno.json`). Toutes les phrases des recettes techno et des guides techno ont leur
   traduction, sauf les titres de morceaux de référence et les notes en anglais, qui ne se traduisent pas.
2. `StyleRecipes.kt` : régénéré dans un dossier temporaire avec `recipes_to_kotlin.py` (37 styles) : identique.
   Les guides régénérés (`make_guides.py` dans un dossier temporaire) : les 40 fichiers `guides/*.json` identiques ;
   `en/guide.json` et `nl/guide.json` ont le même contenu, seul l'ordre d'une clé diffère (voir mineur 1).
3. `TutoChecks.withFilterAsked` : n'est appelé qu'à la création d'une piste par le tuto (TutoChecks.kt:1428), jamais au
   chargement d'un projet : les anciens projets ne passent pas par là. Si le son de la piste n'est pas un son de la
   bibliothèque, ou si sa famille n'a pas de son « Analogique + filtre », on retombe sur l'ancien comportement (même
   son, modèle changé) : pas de plantage possible. Seule la famille « Leads » a un tel son (« Lead filtré ») ; les
   basses, nappes et pluck gardent l'ancien chemin. La correction de gain est linéaire et bornée (SynthPatch.kt:167-170).
   Effet : le tuto techno garde le « Lead filtré » qu'il avait avant le lot, alors que la recette propose maintenant
   le « Stab métallique ». Le test `Tour6Tuto4TechnoTest.theTrackAndTheCurveAreSaidAsTheScreenShowsThem` exerce ce chemin
   (le son nommé par le texte Pro doit être celui de la barre de la piste, dans toutes les langues).
4. Tests changés (AcidcoreTest, AuditSonTest, ClubTransitionsTest, StarterWizardTest, GuideParcoursTest) : chacun
   affirme une valeur précise qui suit un son voulu (303 sur 32 pas, crash au Drop, lead absent du Break, basse de la
   hard techno cochée, courbes plus sombres, grondement acide hors du Mode simple). Aucun ne se contente de passer.
5. Défauts du siège t9-77 : aucun n'est causé par le lot. En particulier le n° 7 (« Mélodie filtrée » au Clavier,
   « Mélodie » au Mix et au ch. 14) existait déjà : avant le lot, la recette techno donnait déjà le « Lead filtré »,
   dont le nom simple est « Mélodie filtrée » (I18n.kt:207) ; la barre montre le nom du son, le Mix celui de la piste.
   Le lot garde ce même son dans le tuto (sinon la barre aurait montré « Plaits »). Les autres (compteurs, notes hors
   écran, « Encore 2 », ch. 11, « Bord de caisse ») portent sur le tuto, que le lot ne touche pas.

## Points relevés

Bloquant : aucun. Grave : aucun.

Moyen :
- (déjà noté, toujours vrai) Le tuto techno fait écrire un « Lead filtré » en riff d'octaves
  (tutos/techno.json:296), alors que le style propose maintenant un stab métallique sur une note, décoché par défaut.
  Rien ne casse ; à aligner dans un lot tuto.

Mineur :
1. `i18n/en/guide.json` et `i18n/nl/guide.json` : la nouvelle clé « Compare avec Charlotte de Witte… » est ajoutée en
   fin de fichier au lieu d'être triée ; la prochaine régénération par `make_guides.py` la déplacera (diff de bruit).
2. Siège n° 7 (antérieur au lot) : nom du son « Mélodie filtrée » au Clavier contre nom de piste « Mélodie » au Mix et
   au ch. 14. À traiter dans un lot tuto ou nommage, pas ici.

## Mineurs d'avant toujours valables

- AuditSonTest ne couvre plus « lead de hard techno coché → EQ présente ».
- Exception du Mode simple pour le grondement des styles acides (borne 10..22 cartes gardée).
- withFilterAsked remet `level` à celui de la bibliothèque (même convention que FxPresets et GuideSheets).
- guides/techno.json:502, carte lead, simpleWhy « qu'on chante » face à « pas de mélodie au départ ».
- Commentaire de `leadTrackId` (WideMix.kt:602-606) pas mis à jour pour les pistes de corps exclues.
- « Charlotte de Witte – Doppler » datée 2020 (house.json:903) et 2021 (techno.json:5).

Verdict : 0 bloquant, 0 grave
