# Essai IA : Stable Audio Open Small contre la synthèse (sub-basses)

Modèle : Stable Audio Open Small (Stability AI), export ONNX atauser001 rev 8f9544d, fp32, 8 pas, sur CPU (~3,75 s par son).
Licence : Stability AI Community License. Les sons produits sont utilisables, avec la mention « Powered by Stability AI » (déjà notée dans THIRD_PARTY.md).

Méthode : 48 candidats par famille (12 prompts × notes C1…B0, graine 1000+i), nettoyés puis coupés à plusieurs longueurs.
Deux variantes : « raw » et « lp » (mono, passe-bas 160 Hz). On garde les 8 meilleurs de chaque variante.
Distance : celle de la production (zb.py d'eq/son-cible-b, préférences sub de cible.py). Cibles : eq/son-fin-essentia-01 @ faa70cb3a.
Le score retenu est remesuré sur le MP3 exporté.
Kicks DnB : abandonnés (décision de Benjamin), donc absents ici.

| Famille | Production (Surge), médiane | IA raw : meilleur / médiane des 8 | IA lp : meilleur / médiane des 8 |
|---|---|---|---|
| Sub techno | 1,59 (1,44–1,82) | 1,17 / 1,48 | 0,96 / 1,20 |
| Sub DnB | 1,90 (1,65–2,32) | 0,47 / 0,55 | 0,56 / 0,60 |

À retenir :
- Sub DnB : l'IA est environ 3,4 fois plus proche de la cible que la synthèse actuelle.
- Sub techno : le gain est réel mais modeste (−25 % en lp). Le défaut restant est toujours le même : « lowflux » à environ −3, c'est-à-dire un grave trop stable, sans mouvement. Viennent ensuite crest_db et decay20_s.
- Limite de la comparaison : les distances de production ont été mesurées sur le tableau audio avant encodage, celles de l'IA sur le MP3. L'écart entre les deux est faible (voir score_array et score_mp3 dans index.json).
- Personne n'a écouté ces sons ; c'est une mesure seulement.

Fichiers : `techno-sub-{raw,lp}-01..08.mp3`, `dnb-sub-{raw,lp}-01..08.mp3`, plus `index.json` (prompt, graine, coupe, scores, descripteurs).
Script : `scripts/son/ia-essai/ia_essai.py`.
