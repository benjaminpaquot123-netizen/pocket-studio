# Tour 8, lot restes8 : rapport

Correcteur, branche `eq/tour-8-restes8` (base 91ae5964, intégration `ccr-80af1a1b-mn0wec`). Zones : la découpe (`remix/CutScreen.kt`), le Guide (`guide/GuidePanel.kt`), la page Plus (`ui/AppSpaces.kt`) ; avec l'accord du chef pour le siège 3a : `export/ShareSheet.kt` (rangée « Écouter », nombre de parties) et `session/SessionScreen.kt` (nouvelle partie).

tests: :app:testDebugUnitTest --tests '*Cut*' --tests '*Guide*' --tests '*Plus*' --tests '*AppSpaces*' --tests '*Tour8NewPart*' --tests '*Tour8Share*' --tests '*ShareScreenshot*'

chemin: Un trozo (es, main gauche, phone 100 %, Simple), message resté d'avant | test: CutStaleMessageTest#staleMessageKeepsTheInstructionOnTop
chemin: Guide, carte sans étape en cours | test: Tour8GuideNoEmptyStepTest#settledCardHasNoEmptyStepBlock
chemin: Guide, carte à faire (l'étape reste) | test: Tour8GuideNoEmptyStepTest#cardToDoKeepsItsStep
chemin: page Plus, phone360 à 200 % | test: Tour8PlusRowsFitTest#french200
chemin: page Plus, phone360 hi à 160 % | test: Tour8PlusRowsFitTest#hindi160
chemin: page Plus, phone de allemand à 200 % | test: Tour8PlusRowsFitTest#phoneGerman200
chemin: Morceau, « + Nouvelle partie vide » (hi, phone360 130 %) | test: Tour8NewPartShownTest#hindiSmallPhoneAt130
chemin: Morceau, « + Nouvelle partie vide » (fr, phone360 200 %) | test: Tour8NewPartShownTest#frenchSmallPhoneAt200
chemin: Partager, « ▶ सुनो » (hi, phone360 130 %) | test: Tour8ShareListenAndPartsTest#listenHindiSmallPhoneAt130
chemin: Partager, nombre de parties (hi, phone360 130 %) | test: Tour8ShareListenAndPartsTest#partsHindiSmallPhoneAt130

corrige: decoupe2 | gravité: moyen | rejeu impossible : moyen, pas de rejeu demandé par la fiche ; preuve par CutStaleMessageTest
corrige: guide2-bloc-vide | gravité: mineur | rejeu impossible : mineur ; preuve par Tour8GuideNoEmptyStepTest
corrige: guide2-plus | gravité: mineur | rejeu impossible : mineur ; preuve par Tour8PlusRowsFitTest
corrige: 3a-23 | gravité: mineur | rejeu impossible : siège 3a non localisé au tri ; preuve par Tour8NewPartShownTest
corrige: 3a-64 | gravité: mineur | rejeu impossible : siège 3a non localisé au tri ; preuve par Tour8ShareListenAndPartsTest
corrige: 3a-65 | gravité: mineur | rejeu impossible : siège 3a non localisé au tri ; preuve par Tour8ShareListenAndPartsTest

## Ce qui a changé
- decoupe2 : en arrivant dans « Un trozo » avec un message resté d'avant, la colonne ne descend plus ; la consigne reste en haut. Un message qui arrive pendant que l'écran est ouvert fait toujours descendre vers les quatre boutons (tour 7, lot 2 D3, gardé dans le même test).
- guide2, bloc vide : le bloc d'étape n'est posé que s'il a quelque chose à montrer (une étape, « Écouter seul » ou un nom) ; plus de 6 dp de blanc sous le titre.
- guide2, page Plus : l'estimation `58 + 36 × taille du texte` est remplacée par une vraie mesure, invisible, des rangées de tuiles (la sonde ne porte ni étiquette ni toucher). Le choix compact / complet suit cette mesure.
- 3a #23 : après « + Nouvelle partie vide », la nouvelle partie est amenée en vue en entier (en plus du bouton).
- 3a #64 : la rangée horloge / « Écouter » de Partager se garde entière ou se cache entière quand la zone défilante se coupe aux lignes ; en hi à 130 %, le bouton n'est plus coupé à mi-hauteur (dessiné 39 px sur 72 avant).
- 3a #65 : Partager dit le nombre de parties différentes du morceau, pas le nombre de cases de la liste.

## À dire franchement
- Page Plus : sur le code d'avant, les rouges french160/200, hindi160 et phoneGerman200 venaient d'abord d'un tag faux (la liste à une tuile par rangée portait `plus_tiles_full`), pas de l'estimation elle-même. Le tag est corrigé (`plus_tiles_list`) et la mesure remplace quand même l'estimation, qui sous-estimait les tuiles de deux à trois lignes au-delà de 130 %.
- « Écouter » en fr et de : le premier test était un faux rouge (paragraphe mesuré plus large que le libellé alors que les lettres tiennent) ; le test regarde maintenant les lettres. Le vrai défaut n'existait qu'en hi.

## Laissés de côté
- 3a #14 (−/+ grisés sans mot) : chez T0c, d'après le chef.

## Recette pour le téléphone de Benjamin
En hindi, texte à 130 %, Mode Simple, onglet Partager : le bouton « ▶ सुनो » se lit en entier et le nombre de parties est celui du morceau.
