# Audit ultra, tour 2 : zone 4 (Enregistrement)

Siège : lot B b (guitariste coréen, tablette 8 pouces, appli en coréen, mission « voix »). Items 18 à 21 de la synthèse.
Tous les défauts sont de l'appli (aucun ne vient du banc : écrans et modèle testés sans banc, moteur de remplacement seulement pour « Écouter seul »).

| N° | Verdict | Gravité | Correction | Test |
|---|---|---|---|---|
| 18 | appli | moyen | Prises numérotées une fois pour toutes (`AudioClip.take`, donné par `withTake` et un passage refait), ordre fixe (`AudioTrack.takesInOrder`, la plus ancienne d'abord, celle qui joue allumée) ; prises d'avant numérotées par leur nom ; croix « Supprimer » (« 삭제 », était « Retirer » = « 빼기 ») ; secondes via `tr("%s s")` dans les prises, le titre de la prise, les fondus, et le fondu d'export du mixeur (« 3초 »). | `TakeNumbersTest` (modèle), `RecordRound2Zone4Test` (écran, fr et ko) |
| 19a | appli | moyen | L'interrupteur de son (`MuteToggle`, partout : Mix simple, mixeur, Structure, Live, Remix) s'appelle « Son de X », coché quand la piste sonne ; son action dit « Couper le son de X » / « Remettre le son de X ». TalkBack lisait « 사운드, 꺼짐 » pendant que la piste jouait. Exclusivité muet/solo (tour 0) inchangée. | `MuteSoloEverywhereTest` (mis à jour), `RecordRound2Zone4Test` |
| 19b | appli | moyen | « Écouter seul » coupé (▶/■ d'une tranche) rend le mix entier et la musique continue si elle jouait avant (`AloneListen.resume`) ; partie du silence, il s'arrête comme avant ; quitter l'écran fait pareil ; le ■ du haut arrête tout. TalkBack de la tranche : « Réécouter toutes les pistes » dans ce cas. | `AloneListenVmTest.turningTheSoundAloneOffWhileTheMusicPlayedKeepsItPlaying` |
| 20a | appli | mineur | `InputLevel` : « Niveau du micro » (icône, nom), barre 18 dp avec sa zone rouge, mot « Trop faible / Bien / Trop fort » ; nom et état TalkBack. Pendant une prise micro et un son enregistré sur un pad. | `RecordRound2Zone4Test.theMicrophonesLevelHasANameAndSaysHowItIs` |
| 20b | appli | mineur | « Audio %d » ajouté aux noms de l'appli (`DefaultNames`) : « 오디오 1 » en coréen partout où le nom passe par `shown` ; sur la carte de piste aussi le nom TalkBack « 오디오 1 옵션 », la couleur et le renommage. | `TakeNumbersTest`, `RecordRound2Zone4Test` |
| 21 | appli | moyen | Carte « Enregistrer ma voix ou ma guitare » au premier lancement (rythme de départ, sans « Simple ou Pro ? ») et sur « Que veux-tu faire ? » (projet ouvert) : elle ouvre la page micro. Le ● du haut demande quoi enregistrer sur tous les onglets ; sur Rythme / Clavier / 303 la source de l'onglet est en premier, le micro toujours proposé. Coût assumé : un toucher de plus pour enregistrer les pads. | `RecordVoiceWaysTest` (4 cas) |

## Méthode
- Chaque test a été écrit contre le défaut ; `AloneListenVmTest.turningTheSoundAloneOff…` a été lancé sans la correction (rouge : la lecture s'arrêtait), puis avec (vert). Les autres tests vérifient ce qui n'existait pas avant (« Prise 1 », « Supprimer », « 7초 », « 오디오 1 », « Niveau du micro », état « activé » du son, choix micro sur Rythme, cartes d'accueil).
- Tests lancés (verts) : `:app` record.*, AloneListenVmTest, RecordVoiceWaysTest, MuteSoloEverywhereTest, NavigationMapProTest, SimpleMixTest, KeyActionsGestureTest ; `:project-model` TakeNumbersTest, i18n.* (catalogues, clés inutilisées), DefaultNamesTest, RecordTakesTest, AudioTracksTest.
- Nouvelles chaînes : zone `ultra2z4` dans les 14 catalogues.

## Captures (regardées)
- `take_picker` (+ 130 % fr/nl, tablette) : « Prise 1 · 28 s ✕ / Prise 2 · 26 s ✕ / Prise 3 · 27 s » sur plusieurs lignes (plus de défilement : la prise qui joue, souvent la dernière, reste visible ; une prise et sa croix ne sont jamais séparées), titre « Prise 3 · 27,4 s ».
- `audio_tracks` : carte d'enregistrement avec « 🎤 Niveau du micro … Bien » et la barre épaisse.
- `welcome` : inchangée dans le cadre (la carte micro est sous le pli, après Pad Live et Défis).

## Reste
- Le mot du niveau suit le crête instantané (pas de lissage) : il peut clignoter entre « Bien » et « Trop faible » entre deux phrases. À revoir sur un vrai téléphone.
- « Écouter seul » reprend la boucle de la partie, pas le morceau entier là où il était.
- Le bouton ■ lui-même garde son icône carrée quand la musique va continuer (seul TalkBack le dit).
- Jargon « Normaliser / Inverser » et 반말 (n° 28-29) : zone 6.
