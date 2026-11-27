# Empreinte des instruments à note : l'outil

Outil de banc, pas dans l'appli : `scripts/son/timbre_instr/` (lancement : `run.sh`, environ 1 min 30).
Il donne la carte d'identité d'une note tenue (basse, 808 longue, lead, nappe, clavier, pluck).
Chaque chiffre est mesuré deux fois, par deux chemins différents. Si les deux ne tombent pas
d'accord, le chiffre est marqué « DÉCROCHE » : on ne s'en sert pas.
Ce lot n'a utilisé aucun son acheté : seulement des sons factices et nos propres instruments.

## Les mesures

| Mesure | À l'oreille | Mesure / contre-mesure | Tolérance |
|---|---|---|---|
| Note | la hauteur | auto-corrélation (YIN) / pics des harmoniques | 0,5 % |
| Richesse (dB/octave) | sourd ou brillant | amplitudes FFT / hétérodyne | 0,5 dB |
| Pairs/impairs | creux (carré) ou plein (scie) | idem | 0,5 dB |
| Inharmonicité | côté cloche, piano, corde | position des pics / phase qui glisse | 10 % |
| Désaccord (cents) | gras, chorus, reese | largeur des pics 2 à 8 / battements | 15 % ou 1 c |
| Vibrato (Hz, cents) | la note qui ondule | suivi par auto-corrélation / phase d'une harmonique | 5 % / 10 % |
| Largeur stéréo | son large ou au centre | côté/milieu global / harmonique par harmonique | 1 dB |
| Souffle (HNR) | propre ou soufflé | creux entre harmoniques / résidu de peigne | 2 dB |
| Attaque, chute, relâchement | le « tac » ou la montée douce, la queue | RMS glissante / enveloppe analytique + Schroeder | 10 %, au moins 1 période de la note |
| Brillance attaque / tenue, chute du filtre | le « wouah » du filtre qui se ferme | centre spectral FFT / hétérodyne | 5 % (15 % la chute) |
| Évolution (nappes) | la nappe qui bouge | ampleur et période de la brillance, deux suivis | 15 % / 10 % |

Hors outil, écrit comme tel : les **accords** (on refuse d'y lire une note : seule la largeur stéréo
est donnée, sans contre-mesure) et les **boucles** (plusieurs notes à la suite : à couper note par note avant).
Un désaccord qui fait battre le son n'est pas pris pour un vibrato : l'outil l'écrit « hors mesure ».

## Le self-test (sons factices, vérité connue)

12 sons fabriqués : scie avec enveloppe connue, carré, 808 à 55 Hz, corde inharmonique, deux et
trois voix désaccordées (dont une reese à 65 Hz), vibrato, stéréo, souffle à 15 dB, filtre qui se
ferme, nappe qui ondule, accord et bruit. Chaque mesure ET sa contre-mesure doivent retrouver la
vérité dans la tolérance. Vu rouge avant les réparations (6 échecs ; puis 2 sur la reese grave, 3 sans le détecteur de battements),
vert après. `run.sh` refuse de mesurer si le self-test n'est pas vert.

## Essai sur 13 de nos instruments (moteur sur l'ordinateur)

Accord mesure / contre-mesure (sons où il y avait quelque chose à mesurer) :

| Tient | Fragile | Décroche souvent |
|---|---|---|
| richesse 11/11, pairs/impairs 11/11, attaque 13/13, inharmonicité 2/2, évolution 2/2, vibrato 1/1 | note 11/13, largeur 11/13, brillance 11/13 et 10/13, désaccord 10/11, relâchement 10/12, chute du filtre 9/10 | chute 5/9, souffle 4/11 |

- Lead et Lead carré : « pas de note sûre » (les deux chemins ne trouvent pas la même note).
- Souffle : sur les sons désaccordés, le contre-chemin compte le désaccord comme du souffle.
- Chute : décroche sur Basse ronde, Reese, Supersaw et Pluck (chute lente ou noyée dans les battements).

## La recherche CMA vise bien l'empreinte

Démo : un instrument factice à 5 réglages cachés (richesse, pairs, attaque, relâchement,
désaccord). La recherche CMA de l'étude (`timbre.cma`) part au hasard et cherche les réglages
dont l'empreinte colle à la cible. Distance en « tolérances » (1 = une tolérance en moyenne) :

| Départ | Optimum trouvé | Plancher (même son, autres phases) |
|---|---|---|
| 11,13 | **0,41** | 1,33 |

Réglages retrouvés : richesse 1,32 (caché 1,3), pairs 0,345 (0,35), attaque 27 ms (25),
relâchement 0,109 s (0,12), désaccord 6,55 c (7). L'optimum passe sous le plancher : la
recherche a fait tout ce que la mesure permet.

## Verdict

**Prêt** pour : richesse, pairs/impairs, attaque, inharmonicité, note des basses, nappes, claviers
et plucks, et pour viser une empreinte avec CMA.
**Pas prêt** pour : la note des leads, le souffle et la chute des sons désaccordés.

### Ce qui n'est pas vert

- Lead et Lead carré : aucune note sûre.
- Souffle : 4 sons sur 11 seulement.
- Chute : 5 sur 9.
- Relâchement et chute d'un son désaccordé : ils changent d'un tirage de phases à l'autre (le
  plancher CMA de 1,33 vient de là) ; il faut comparer plusieurs rendus, pas un seul.
- Limites de l'essai : patches V2 sans les retouches ProVoices, GAIN et SUB_FLOOR ignorés,
  vélocité 0,8 ; la note entendue est souvent l'octave du dessous (le SUB du moteur), c'est normal.
