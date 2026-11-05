siege: c | profil: t5-Cc-dj-club-anglais | lang: en | device: phone | font: 100 | mode: pro
mission: exports | buts: spotify, club, retrouve
promesse: id=t5-Cc-1 | texte: The song has changed since this export: export again to send the new version. | ecran: 68 | tenue: non | pourquoi: Undo juste après l'export Club remet la cible sur Off (écran 22) puis Streaming (écran 68) ; et l'alerte « changed » est sortie une fois juste après un export Share sans aucun changement (écran 31)
promesse: id=t5-Cc-2 | texte: No song yet: your part goes out as a loop (8 bars). | ecran: 27 | tenue: non | pourquoi: format M4A et cible bien repris de Mix, mais la longueur reste 8 mesures (0:16, fichier 16 s) alors que Mix est sur 16 puis sur 4 mesures
promesse: id=t5-Cc-3 | texte: In Music › BenStudio on this phone. Newest first. | ecran: 54 | tenue: non | pourquoi: les deux .pocketstudio.zip envoyés n'y sont pas, aucune marque de version, chaque fichier affiche 1 KB (WAV de 33 s)
promesse: id=t5-Cc-4 | texte: To pick up your song on another device: “Share” › “Send the project” (a .zip with all its sounds), then, on the other device, My projects › “Import a project (.zip)”. | ecran: 45 | tenue: oui | pourquoi: « ? » du bandeau, Share › Send the project, My projects › Import a project (.zip), Apple Music et Free dans Mix : tout existe
hesitations:
  - ecran: 27 | cherchais: où régler la longueur de la boucle dans « Ready to share? » | regarde: seulement « (8 bars) » en texte, aucun réglage, et Mix dit 16
  - ecran: 11 | cherchais: exporter le morceau entier pour mon set | regarde: « The song is empty: add parts in Structure. », je suis revenu à la boucle
  - ecran: 42 | cherchais: le .zip du projet que je venais d'envoyer | regarde: seulement le .m4a et le .wav
abandon: jamais
incidents-banc:
defauts:
  - geste: 20 | ecran.vu: 22 | cible: case « Club · −8 LUFS » | geste.type: touche
    attendu: juste après l'export Club, Undo ne touche pas au choix Club (promesse) | obtenu: la cible repasse sur Off, et l'alerte « The song has changed since this export » apparaît ; refait écran 68 (Club → Streaming)
    symptome: perdu
    gravite: contourne-en-1-gestes | revu: oui: ecran 68 | banc?: non | promesse: t5-Cc-1
  - geste: 30 | ecran.vu: 31 | cible: The song has changed since this export: export again to send the new version. | geste.type: touche
    attendu: juste après l'export Share (OK sur « It's ready »), pas d'alerte « changed » | obtenu: alerte affichée alors que rien n'a changé (le Guide passe de 27 % à 31 %) ; pas revu au 2e essai écran 62
    symptome: texte-faux
    gravite: finition | revu: non | banc?: non | promesse: t5-Cc-1
  - geste: 27 | ecran.vu: 30 | cible: M4A, 48 kHz, 256 kb/s · 16 s | geste.type: touche
    attendu: « Ready to share? » exporte la longueur choisie dans Mix (16 mesures = ~32 s, puis 4 mesures = ~8 s) | obtenu: toujours 8 mesures / 16 s, la fiche dit « (8 bars) » ; le bouton « Export 16 bars » de Mix, lui, respecte
    symptome: perdu
    gravite: contourne-en-2-gestes | revu: oui: ecran 61 | banc?: non | promesse: t5-Cc-2
  - geste: 41 | ecran.vu: 42 | cible: texte « My exports » | geste.type: touche
    attendu: le .zip envoyé (« My-first-beat-20261105-211648.pocketstudio.zip ») listé dans My exports | obtenu: seuls le .m4a et le .wav ; 2e zip envoyé par My projects › Options, toujours absent écran 54
    symptome: perdu
    gravite: contourne-en-3-gestes | revu: oui: ecran 54 | banc?: non | promesse: t5-Cc-3
  - geste: 41 | ecran.vu: 42 | cible: Nov 5, 2026, 9:15 PM · 1 KB | geste.type: touche
    attendu: taille vraie du fichier (un WAV 24 bits de 33 s pèse plusieurs Mo) | obtenu: 1 KB pour le WAV comme pour le M4A
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 54 | banc?: son | promesse: t5-Cc-3
  - geste: 41 | ecran.vu: 42 | cible: My-first-beat-loop-streaming-20261105-211622.m4a | geste.type: touche
    attendu: les versions marquées (laquelle est à jour, laquelle date d'avant le changement signalé par « The song has changed… ») | obtenu: liste brute nom + date, aucune marque
    symptome: sans-aide
    gravite: finition | revu: oui: ecran 54 | banc?: non | promesse: t5-Cc-3
  - geste: 23 | ecran.vu: 24 | cible: −14 LUFS over the whole song: the loudness Spotify and YouTube bring every song to. | geste.type: touche
    attendu: un texte qui colle à ce qu'on exporte (ici une boucle, la chanson est vide) | obtenu: « over the whole song » alors que l'export est « The looping part »
    symptome: texte-faux
    gravite: finition | revu: oui: ecran 52 | banc?: non | promesse: -
mots-faux:
  - ecran: 34 | vu: My-first-beat-20261105-211648.pocketstudio.zip | attendu: un nom d'appli cohérent (l'appli s'appelle BenStudio, dossier Music › BenStudio)
  - ecran: 47 | vu: Loop of 1 bar | attendu: la longueur réelle de la boucle (Mix dit « played over 8 bars », puis 16 ou 4)
