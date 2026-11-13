# Tour 6 — lot « emulateur » : relecture neuve
Relu : 4e31f98cd4c87215ab9c143b0c24c21686db4c1a

Relecture par la discussion « Testeur 3 », qui n'a pas écrit le lot : le diff complet, vérifié contre la source d'ICU (`regexcmp.cpp`) et contre les quelque 60 motifs des quatre dossiers livrés.

## Bloquant
Aucun.

## Grave
Aucun. Le nouveau motif passe sous ICU (drapeaux i et u acceptés, regard en avant et `\p{L}` / `\p{N}` standard). Aucun autre motif à risque dans `app`, `project-model`, `ui-components`, `engine` : pas de drapeau hors `(?i)`, regards en arrière tous bornés, seule option `RegexOption.IGNORE_CASE`.

## Moyen
- La garde ne voit pas un drapeau seulement négatif (`(?-U)`).
- La garde ne refuse pas `CANON_EQ`, non pris en charge sur Android.
- Sur le téléphone, `\b` et `(?i)` sont Unicode ; sur la JVM, ASCII : les tests JVM ne prouvent rien pour un mot collé à une lettre accentuée.
- Portée : `src/main` seulement.

Verdict : 0 bloquant, 0 grave — fusion possible
