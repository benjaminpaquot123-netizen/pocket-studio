# Tolérances d'accord (écrites avant la première mesure d'un vrai son)

Un chiffre de la table et le chiffre de `mesure.py` « s'accordent » quand l'écart
reste sous la tolérance ci-dessous. Une case vide d'un côté seulement compte
comme désaccord (« vide d'un côté ») ; vide des deux côtés : pas compté.

| colonne | tolérance | raison |
|---|---|---|
| dur_s | 0,01 s | nombre d'échantillons / fréquence, rien à interpréter |
| peak_dbfs | 0,1 dB | crête d'échantillon, tous canaux |
| rms_dbfs | 0,2 dB | puissance moyenne, tous canaux |
| t20_ms | 10 % ou 5 ms (le plus large) | intégrale de Schroeder ; le départ dépend du seuil d'attaque |
| t60_ms | 10 % ou 5 ms (le plus large) | idem |
| centroid_hz | 10 % | spectre d'une fenêtre attaque → point -60 dB |
| hf_db | 1,5 dB | part ≥ 10 kHz de la même fenêtre |
| transient_body_db | 1,0 dB | énergie 10 ms après l'attaque / énergie après |
| tonal_db | 1,5 dB | part de la bande autour du pic grave de la queue |
| pitch_steady_hz | 100 cents (un demi-ton) | fréquence de la partie stable d'un coup tonal ; la critique l'a trouvée tenue au demi-ton près |
| pitch_attack_hz | 200 cents | fréquence moyenne 2 à 12 ms après l'attaque : fenêtre courte, une seule période sous 100 Hz |
| f0_hz (one-shots mélodiques) | 50 cents ; une erreur d'octave est comptée à part | note tenue |

Hauteur des coups de batterie : comparée seulement quand la table donne
`tonal_db ≥ -10` (coup réellement tonal) ; sinon notée « non comparée ».
La hauteur d'une famille bruitée (charley, crash, shaker) n'a pas de sens
(critique aveugle) : elle est mesurée mais rangée à part dans le résumé.

Méthode de `mesure.py` (indépendante de `tout.py`, numpy seul) :
- lecture WAV / AIFF écrite à la main (PCM 8 à 32 bits, flottant 32/64, `sowt`) ;
- attaque : premier échantillon à -30 dB de la crête ; t20 / t60 : intégrale de Schroeder ;
- hauteur stable : passages par zéro d'un signal filtré par FFT autour du pic grave,
  médiane des périodes entre les points -10 dB et -40 dB de la décroissance ;
- hauteur d'attaque : phase du signal analytique (FFT) entre 2 et 12 ms ;
- f0 mélodique : spectre à somme d'harmoniques sur 30 ms → 1 s après l'attaque.
