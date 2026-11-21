# Relecture neuve : lot textes (tour 9), après fusion de l'intégration ceb482ad6

Relu : 7a48eb60

Relecteur : sous-agent neuf, n'a pas écrit le code. Diff relu : `git diff ceb482ad6 7a48eb60` (8 fichiers).

- corrections.tsv : conflit résolu en gardant les deux côtés, aucun marqueur, aucun doublon exact, 12 colonnes partout.
- GuideSheets.soundOf passe par tabSoundName : pas de plantage neuf ; piste avec instrument ou son exact inchangée.
- es « Detener la fila %d » : juste ; la règle « Arrêter » couvre 7 langues.
- Tests : le test du guide échouait avant le correctif ; le test du mot stop attrape le cas es.

Mineurs (notés, pas bloquants) :
- tabSoundName (GuideSheets.kt:160-185) copie trackSoundName (TrackBar.kt:74-98) ; à unifier plus tard (accord non gardé pour Plaits).
- Tour9TextesRowStopWordTest fait doublon avec Tour9TextesStopWordTest.
- aFreshTracksSoundIsNamedAsBefore ne vérifie pas le nom attendu.

Verdict : 0 bloquant, 0 grave
