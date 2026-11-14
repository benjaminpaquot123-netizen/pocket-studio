# Audit ultra, tour 1 : zones 5 et 6 (assistant, tutos, sons ; textes, langues, accessibilité)

Équipe 4. Commit principal `05801b8`. Gravités de la synthèse ; « appli » = vrai défaut corrigé, « banc » = vient du banc.

| N° | Verdict | Gravité | Test (échoue avant) | Fait | Reste |
|---|---|---|---|---|---|
| 30b, 39 | appli | Moyen ×5 | ScreenScanTest : tout curseur ou interrupteur sans nom refusé, sans plafond ; LayoutScan ne prend plus la valeur (« 0.0 ») pour un nom | 40 contrôles nommés (assistant, Swing, volumes, synthé, master, métronome, MIDI, Remix, export, Pad Live) ; `Modifier.named` | Les valeurs du synthé en % plutôt qu'en Hz/ms |
| 32 | appli | Moyen ×2 | WizardTracksOfferedTest | La ligne du temps ne montre plus une Acid 303 muette | — |
| 33 | appli | Moyen ×2 | StarterWizardTest.theShortVersion… | « Court » (20 à 75 s selon le tempo) / « Complet » au départ de l'assistant | — |
| 34 coup en trop | appli | Moyen | TutoCourseTest (un coup en trop n'est plus réussi) | Barré sur la grille, « 1 coup en trop ici (barré) » | — |
| 34 cases en pointillé | appli | Moyen | — (état TalkBack) | « vide, en pointillé : à toucher » / « barré : à enlever » | — |
| 34 Sol♭ / Fa♯ | appli | Moyen | — | Piano roll écrit les touches comme la tonalité | Test dédié à ajouter |
| 34 plus haut ↑ | appli | Moyen | — | « plus haut ↑ / plus bas ↓ » dans la ligne du tuto | Test dédié à ajouter |
| 34 Montre-moi | banc (le maintien de 3,2 s ne passe pas dans l'horloge du banc ; TutoTest prouve la remise) + clarté appli | Mineur | TutoTest vert | « Voilà. Je remets comme avant… » puis « À toi maintenant » | — |
| 34 consigne « t… », rangée sous « Partie : A », bravo de chapitre | appli | Grave | — | Non fait | Ligne repliée à 2 lignes, défilement sous l'en-tête collant, bravo + Écouter en fin de chapitre |
| 34 « Lire la suite » → kit | banc pour le toucher (le texte pris pour la cible) ; appli : la carte du kit arrive tard par-dessus la barre du haut à 200 % | Moyen | — | Non fait | Proposer le kit au début du tuto seulement |
| 35 fantôme | appli | Moyen | BassGhostTest | La piste Basse en fantôme sans ligne 303 | — |
| 35 famille | appli | Mineur | TrackSoundFamilyTest | « Changer le son » s'ouvre sur la famille de la piste | — |
| 35 effets / nom / doublon | appli | Grave | — | Non fait | Le favori garde ses effets, Renommer, « déjà dans tes favoris » |
| 36 | appli | Moyen | TrackSoundNameTest | La piste montre « Orgue », plus « Synthé » | « Octave 3 » / « Do 4 » |
| 37 | appli | Moyen | — | Mode simple : kick, charley, triolet, roulement, Drop, penta, phrygienne, contretemps, 808 expliqués sous les textes de l'assistant et du tuto | « FM 6 op. », « Modal (percussion) » à dire en mots simples |
| 38 | appli | Moyen | — | Non fait | Ajouter les écrans du tuto à 200 % à la matrice CUT |
| 40 | appli | Mineur | I18nCatalogTest vert pour mes clés | Undo ≠ Cancel (clé « Annuler (défaire) ») ; Stage effects dans 10 langues ; de, es, ar, nl (spoor/nummer), it/pt (Do Re Mi), pl, pt-BR, ja ; référence boom bap neutre ; BENSTUDIO sur la vidéo | Noms de sons du kit (« Caisse claire 1 ») ; « Fechar planilha » = texte de Material ; ja 周波数 pour la fréquence d'échantillonnage |
| 40 « Créer la vidéo » | banc (NativeEngine absent sous Robolectric, seul cas où le message sort) | — | — | — | Préparer un moteur factice pour la vidéo |
| 41 | appli | Mineur | — | Non fait | Accueil tablette paysage en deux colonnes, noms de pistes sur deux lignes |

Captures ré-enregistrées et regardées : `wizard_mood` (carte « Durée du morceau »), `tuto_demo_caption`.
`timeline` et `tuto_new_project` diffèrent aussi, à cause des zones 3 et 28 (pas les miennes).
Laissés aux autres équipes, non commités : les noms des curseurs de `MasterPro.kt` (fichier réécrit par l'équipe 2), « Annuler (défaire) » dans `ThumbBar.kt` (fichier neuf de l'équipe 3).
ScreenScanTest : HELP et CONTRAST dépassent leur plafond sur 54 écrans, à cause des contrôles ajoutés par d'autres équipes (barre du Guide, ThumbBar, Pad Live) ; aucun curseur ni interrupteur sans nom.
