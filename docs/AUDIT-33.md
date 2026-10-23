# Audit « morceau complet » (2026-10-23, 5 amateurs + 3 pros)

Rapports détaillés : `docs/audit-33/` (amateur1-5, pro1-3). Lecture du code et des captures, version 0.22.173.

## Ce qui rend un morceau complet difficile (regroupé)
1. **Trop de notions et de vues** : scène / partie / section / morceau mélangés ; trois vues d'arrangement (liste Morceau, grille de scènes, Ligne du temps) ; Live, pads live et pads plein écran mêlés à la création ; 7 onglets. (amateurs 1, 2, 3 ; pro 3)
2. **Après l'assistant, on est seul** : ▶ joue la section affichée et pas tout le morceau ; pas d'écran « Ton morceau est prêt » ; Partager caché dans le Mixeur ; pas de vraie fin (fondu seulement à l'export). (amateurs 1, 4 ; pro 3)
3. **Modifier le morceau entier est lourd** : changer un son ou les accords ne touche qu'une scène ou une piste ; pas de « drop plus fort », « ajouter une fin », « faire un break » en un geste ; variations / fills = dupliquer une scène ; batterie = une seule rangée dans la ligne du temps. (amateurs 1, 3 ; pro 1)
4. **Enregistrer est caché ou risqué** : pas de gros « ● Enregistrer ma mélodie » avec décompte puis « Remettre en rythme / Refaire / Garder » ; voix sans repère de mesure ni recalage ; prise Live : pads non enregistrés, pas de notes jouables en live, « Remplacer le morceau » par défaut. (amateurs 2, 4, 5)
5. **Son et mixage** : pas de LUFS en direct ; le compresseur ajoute un gain fixe même sans compresser (Effects.cpp:176, le limiteur travaille ~5 dB de trop) ; « Pompe » limitée au kick du pad 1 ; master figé ; pas de « rééquilibrer » automatique. (pro 2)
6. **Beats** : pas de piste 808 avec glide en un geste ; hi-hats case par case (pas de glisser) ; roulements seulement décroissants, pas de 1/32 ; une seule longueur pour tous les pads. (amateur 2 ; pro 1)
7. **Automation encore technique** : pas de noms de parties sur les courbes ; pas de boutons « fondu d'entrée » / « montée avant le drop » ; filtre du synthé non automatisable. (amateur 3 ; pro 1)
8. **Texte** : coupures restantes à 130 % (« Co… », « Da F… ») ; jargon anglais (Lead, Pad, 303, Quantifier). (amateur 4)
