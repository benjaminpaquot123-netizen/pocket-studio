# Rapport d'un testeur IA (gabarit, Audit ultra §3.4)

Un bloc par siège, **40 lignes au plus**, à champs fermés : le script de fusion le lit tel quel. Pas de récit. Les chiffres (gestes, retours, « ? », premier son, abandon, mission réussie) sont écrits par le banc dans `mesures.json`, pas par le testeur. **Aucun jugement sur le son** (le banc n'a pas de moteur) : un rapport qui en contient est rejeté. Pas de note sur 10.

```
siege: <lettre> | profil: <un des 16 profils> | lang: <code> | device: <profil> | font: <100|130|200> | mode: <simple|pro>
mission: <nom de la mission> | buts atteints: <n sur m>
hesitations:
  - ecran: <n°> | cherchais: <une ligne> | regarde: <où j'ai regardé, une ligne>
  - ecran: <n°> | cherchais: … | regarde: …
  - ecran: <n°> | cherchais: … | regarde: …
surprise: <une ligne, ou « aucune »>
abandon: j'aurais abandonné à l'écran <n°> parce que <une ligne>   (ou « jamais »)
defauts:   (12 au plus, une entrée chacun)
  - ecran: <n°> | cible: <texte ou nom TalkBack vu> | geste: <touche|appui-long|glisse|ecris|retour>
    attendu: <une ligne> | obtenu: <une ligne>
    symptome: <rien|mauvais-ecran|plantage|incompris|coupe|lent|perdu|bloque|sans-nom|sans-aide>
    gravite: <bloquant|grave|moyen|mineur> | bloque: <oui|non>
```

Clé de fusion calculée par le script : route de l'écran (lue dans le journal au n°) + cible + symptôme. Une grappe vue par deux sièges est un défaut confirmé sans juge.
