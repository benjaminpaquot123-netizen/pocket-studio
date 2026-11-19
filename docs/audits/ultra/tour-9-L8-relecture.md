# Relecture neuve, tour 9, lot L8

relu : 8ce4a1bf7

Relecteur : sous-agent neuf (n'a pas écrit le code). Portée : 7455749a5..HEAD (eq/tour-9-L8), sans captures ni docs.
Une première relecture (7455749a5..f603c4b94) avait donné 0 bloquant, 0 grave, 12 mineurs ; celle-ci couvre tout le lot, F125/F122/F121 compris.

1. mineur, ShareSheet.kt:890/911 : le renommage ne se fait jamais interrupteur éteint ni sur un projet nommé (shareRenamesProject exige un nom par défaut). Aucun test de la route elle-même (renameProject non appelé quand éteint).
2. mineur, ShareSheet.kt:445 : condition redondante ; l'étiquette « (et du projet) » peut rester si le nom tapé égale le nom par défaut (défaut antérieur au lot).
3. mineur, TrackBar.kt:175 : soundBeside peut rendre « » (808 / 808) : vérifier qu'aucune ligne vide n'apparaît.
4. mineur, KitAndPadSheets.kt:111-117 : deux KDoc d'affilée, le premier mal placé.
5. mineur, KitAndPadSheets.kt:256 : en étroit, le titre du pad n'a plus maxLines = 2.
6. mineur, TempoSheet.kt : « Fermer » de Tempo pas encadré comme Kit/Pad (raccord DESIGN).
7. mineur, ShareSheet.kt:652 : en RTL, l'isolation bidi touche aussi un tiret de nom (« Beat-2 »).
8. mineur, i18n hi/partage.json : « आपकी » vouvoie, l'appli tutoie en hindi.

Vérifié sans défaut : rows ≥ parts, 0 partie ; FormatChips une seule fois ; interrupteur gardé par projet ; « Annuler » encadré ; 14 langues × 5 clés, vraies traductions, aucune clé existante modifiée ; test soundBeside toutes langues avec garde before > 0.

Ajout après relecture : commit a078458a2 (correction du selftest du banc), relu par un sous-agent neuf. La phrase de renommage devient l'étiquette de l'interrupteur ; seul « Tout est prêt » fusionne son rond, les ⚠ restent à part. Rien perdu, rien en double, balises gardées. 0 bloquant, 0 grave. Deux mineurs :
9. mineur, ShareSheet.kt : éteint, l'étiquette dit un état (« Le projet garde son nom… »), en petit gris.
10. mineur, Tour9L8ShareWordsTest l.86 : cherchait share_renames sans l'arbre non fusionné. Corrigé (commit suivant, test vert, 7/7).
Selftest du banc vert ensuite : 388 verts, 0 rouge.

Verdict : 0 bloquant, 0 grave
