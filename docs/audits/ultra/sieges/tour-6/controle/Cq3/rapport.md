code testé : fd8c57a68b841803aaeb3b56ff6bfd014e538a0d
joué par : sous-agent neuf
siege: a | profil: t6-Ac controle-lotCq3 | lang: fr | device: phone | font: 100 | mode: pro
mission: exports (contrôle ciblé F1-F3) | buts: spotify, club, retrouve
hesitations:
  - ecran: 7 | cherchais: « Autres fichiers » sur la page Partager en Mode simple | regarde: absent en Simple (image 7) ; trouvé seulement en Pro, dans la feuille « Prêt à partager ? » (écran 29)
  - ecran: 45 | cherchais: « Autres fichiers » au 2e passage dans la feuille Partager (Pro) | regarde: la ligne a disparu entre « Vidéo pour les réseaux » et « M4A » (écrans 41, 45, 46) ; rejoint par l'onglet Mix en faisant défiler (écran 53)
  - ecran: 14 | cherchais: le fichier juste envoyé | regarde: « Chargement de la liste… » qui ne finit pas ; rouvrir Mes fichiers montre le fichier
abandon: jamais
incidents-banc:
  - geste: 14-16 et 60-62 | attente: ~25 s puis ~95 s | vu: « Mes fichiers » reste sur « Chargement de la liste… » juste après un envoi (peut-être le banc : banc?: temps)
  - outils hors human-act.py : cat/heredoc sur mon seul dossier de siège (lecture start.txt, écriture rapport.md)
defauts:
verdicts:
F1: corrigé (écran 30 : M4A choisi en Simple reste M4A dans Autres fichiers en Pro ; écrans 53-54 : FLAC toujours choisi au retour) — note : en Simple aucune entrée « Autres fichiers » (passage en Pro par la pastille), et au 2e passage la ligne « Autres fichiers » manque dans la feuille Partager (écrans 41/45), retour par l'onglet Mix
F2: corrigé (écran 41, image 41 : « Volume : −14,0, comme la musique en streaming (−14) » entière, « détails » et « ? » dessous ; même chose écran 45)
F3: autre (liste plus vide, mais juste après l'envoi elle reste sur « Chargement de la liste… » écrans 14-16 et 60-62, 2 fois sur 2 ; en fermant puis rouvrant, le fichier envoyé est listé écrans 18 et 65 ; banc?: temps possible)
