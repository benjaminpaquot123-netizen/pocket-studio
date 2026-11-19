# Tour 9, lot L17 : décisions

- F039 : jouer le pad d'un son dans la grille rythme passe par un crochet « oreille » du tuto (`TutoPadEar`), qui coche l'étape d'écoute de ce son. Retour arrière : revert a77d9d13.
- F040 : toucher une case muette de l'éditeur Acid y pose une note (avant, la case était seulement choisie). Retour arrière : revert 9b33c7de.
- F044 : le contrôle d'un chapitre de notes compte aussi les notes fantômes de la piste copiée à l'octave ; plus de « Fini » avec une note en pointillé. Retour arrière : revert 7fbb6174.
- F042 : le conseil d'effet nomme les tranches réelles du style (liste des envois rendue publique). Retour arrière : revert be6f1050.
- F036 : toucher le bouton de l'étape pendant une démo arrête d'abord la démo (`endDemoNow`), puis fait le geste ; la partie créée est gardée. Retour arrière : revert d44b4466.
- F032 : triolets validés seulement sur les deux cases demandées ; crash validé seulement sur Drop et Drop 2. Retour arrière : revert 76d693c0, 518d76d0.
- F032 (synthwave, tom sur la ligne « Rim ») : pas corrigé ici. Le texte vit dans synthwave.json et 14 fichiers de traduction non accordés ; changer le français seul laisserait 14 langues sans traduction. Test rouge retiré (ebb65c3e). Retour arrière : sans objet.
- F034 : la démo d'un fill dont la consigne dit « Roulement » pose un roulement (`rolls()`). Retour arrière : revert e50d28b0.
- F034 (t9-08, t9-38, t9-65) : démo qui pointe une zone vide pour créer une partie, non reproduit dans mes fichiers ; laissé à un lot de la zone Structure. Retour arrière : sans objet.
- F046 : la ligne du compteur dit dès le départ les cases des autres mesures (`elsewhere != null`, plus seulement quand celle-ci est vide). Retour arrière : revert e3383914.
