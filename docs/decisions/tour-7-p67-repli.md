# Tour 7, lot p67-repli (bloc 0b de P6 et P7) : décisions

Plan : `docs/audits/ultra/tour-7-plan-p6-p7.md`. Chaque ligne : choix, raison, retour arrière.

- `TutoReadability.layout` : quand les mots sont coupés malgré tous les blocs repliés, la carte du kit et « Pourquoi ? » reviennent, seuls les exemples restent repliés ; les lignes montrées sont celles qui tiennent sous le kit, « Pourquoi ? » et « Lire la suite » (une au moins). Raison : les replier n'épargnait rien aux mots et cachait le kit ou « Pourquoi ? » (arabe à 130 %). Retour arrière : rendre `kitCard`/`why` à leur valeur après repli.
- Dans le panneau, la carte du kit et « ▸ Pourquoi ? » restent épinglés dans `WordsRoom` et ne se replient jamais ; seuls les exemples (Mode simple, `wizard_plain_words`) se replient, quand les mots défilent et que `layout` le dit, sur les hauteurs mesurées. Raison : le kit doit rester visible quand l'étape en a besoin. Retour arrière : retirer le `SideEffect` de repli dans `TutoPanel`.
- Le défilement des mots est remis à zéro quand les exemples se replient. Raison : sans ça, « Lire la suite » gardait sa place sous des mots qui tiennent désormais, et « Réduire » ne ramenait plus « Lire la suite » (`Tour7TutoReduceTest`). Retour arrière : retirer `examplesFolded` de la clé du `ScrollState`.
