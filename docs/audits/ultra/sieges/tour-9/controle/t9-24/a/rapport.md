code testé : ab7fbf4e83d955e08309d6bb77f1ce0cc65ce008
joué par : sous-agent neuf

siege: a | profil: confirme | lang: pl | device: phone | font: 100 | mode: pro
mission: tuto-pro (drill, chapitres 9-16) | buts: -
chapitres: 9 compris du premier coup | 10 hésitation | 11 hésitation | 12 hésitation | 13 hésitation (fini par « Zrobię to ») | 14 bloqué (krok 1 sauté) | 15 hésitation (fini par « Zrobię to ») | 16 compris du premier coup ; fin : chapitre 16 atteint, « Prawie koniec! Został ci 1 krok » (ch. 14)
hesitations:
  - ecran: 13 | cherchais: copier Refren dans une nouvelle partie | regarde: le menu A ▾ ne propose que « Nowa pusta część »
  - ecran: 37 | cherchais: « Długość » d'une partie, le tuto dit « dotknij części » | regarde: le ⋮ de la ligne
  - ecran: 58 | cherchais: ce qu'est « ostatnie uderzenie » | regarde: le dernier coup posé, puis la case vide 16
abandon: ecran 142 | parce que dans Miks plus aucun fader ne répond : krok 1 du chapitre 14 impossible
incidents-banc:
  - geste: 74 | attente: 0 | vu: le banc dit « doigt couvert : Perkusja couvre Akordy » ; même refus aux gestes 78, 94, 95, 101, 102, 104, 110, 119, 125, 139, 140 (voir défaut 1)
defauts:
  - geste: 110 | ecran.vu: 111 | cible: Akordy | geste.type: glisse
    attendu: baisser un peu le fader d'Akordy (ch. 14 « ścisz trochę ») | obtenu: rien ne bouge ; dans Miks plus rien ne répond (faders, « Automatyzuj ustawienie », « Auto (styl) ») ; le banc dit qu'un élément « Perkusja » invisible est par-dessus
    symptome: rien | gravite: mission-impossible | revu: oui: ecran 141 (fader Perkusja aussi, après changement d'onglet) | banc?: gel | promesse: -
  - geste: 60 | ecran.vu: 61 | cible: Twoja kolej: w części „Pre-chorus”, ostatni takt, pad „Hi-hat”: przytrzymaj ostatnie uderzenie, potem „Roll” ×4 … | geste.type: appui-long
    attendu: Roll ×4 sur le dernier coup de hi-hat accepté | obtenu: refusé ; seul un Roll sur la 16e case vide est accepté (4 gestes de plus)
    symptome: incompris | gravite: contourne-en-4-gestes | revu: oui: ecran 63 | banc?: non | promesse: -
  - geste: 62 | ecran.vu: 64 | cible: Pokaż mi | geste.type: touche
    attendu: voir faire le Roll ×4 du texte | obtenu: la démo pose des coups sur les cases 13-16 avec une force qui monte, pas de Roll
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 36 | ecran.vu: 37 | cible: B · Intro × 1 · 8 taktów | geste.type: touche
    attendu: le tuto dit « dotknij części, potem pod „Długość” » : ouvrir la longueur | obtenu: le morceau se met à jouer ; « Długość » est dans le ⋮
    symptome: mauvais-ecran | gravite: contourne-en-2-gestes | revu: non | banc?: non | promesse: -
  - geste: 40 | ecran.vu: 41 | cible: Wszystkie (3) | geste.type: touche
    attendu: éditer les 3 parties ensemble (rubrique « Edytuj: ») | obtenu: la feuille se ferme, « Pętla wł. » et lecture, aucun éditeur
    symptome: incompris | gravite: finition | revu: oui: ecran 43 | banc?: non | promesse: -
  - geste: 9 | ecran.vu: 10 | cible: Dobrze wiedzieć: … Jego zwrotka „16 taktów” ma więc tutaj 32 takty. | geste.type: touche
    attendu: un chiffre cohérent | obtenu: le schéma juste à côté et le chapitre 11 disent « Zwrotka 30 »
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 22 | ecran.vu: 23 | cible: Pre-chorus | geste.type: touche
    attendu: le texte dit « bez kicka i 808 » | obtenu: la partie enlève aussi Akordy (« Kick studyjny, 808, Akordy »)
    symptome: texte-faux | gravite: finition | revu: non | banc?: non | promesse: -
  - geste: 80 | ecran.vu: 81 | cible: Zrobię to | geste.type: touche
    attendu: qu'on m'ouvre le bon réglage et que je le fasse | obtenu: « Akordy ✓ · Werbel ✓ » déjà cochés à 30 % avant que je touche (pareil écran 76) ; au ch. 13 krok 3 (geste 97) toute l'automation est faite pour moi, portée « Utwór » au lieu de « Na część »
    symptome: incompris | gravite: finition | revu: oui: ecran 98 | banc?: non | promesse: -
  - geste: 83 | ecran.vu: 84 | cible: ⌃ | geste.type: touche
    attendu: déplier la carte du tuto | obtenu: une démo « Patrz, robię to… » part toute seule et ouvre « Efekty · Melodia » (encore au geste 89)
    symptome: mauvais-ecran | gravite: finition | revu: oui: ecran 90 | banc?: non | promesse: -
  - geste: 51 | ecran.vu: 52 | cible: Gotowe: nowa długość dla C · Zwrotka | geste.type: touche
    attendu: un message qui dit ce que « Ustaw zalecane długości » vient de faire | obtenu: l'ancien message (une seule partie) avec « Anuluj » reste affiché jusqu'au chapitre 12
    symptome: texte-faux | gravite: finition | revu: oui: ecran 54 | banc?: non | promesse: -
mots-faux:
  - ecran: 15 | vu: bez Kick studyjny, Werbel, Hi-hat, Rim, 808, Akordy | attendu: bez kicka studyjnego, werbla, hi-hatu, rimu, 808, akordów
  - ecran: 17 | vu: bez Akordy | attendu: bez akordów
  - ecran: 59 | vu: 3 uderzeń | attendu: 3 uderzenia
  - ecran: 60 | vu: 4 uderzeń | attendu: 4 uderzenia
  - ecran: 101 | vu: ścisz trochę „Akordy” i „Melodia”: najpierw 808 i kick, potem werbel, potem hi-haty | attendu: phrase incohérente (baisser accords/mélodie, puis une liste de la batterie) ; « „Akordy” i „Melodię” »
  - ecran: 87 | vu: Wysyłka na pogłos (carte) | attendu: Wysyłka pogłos (nom du réglage à l'écran)
  - ecran: 114 | vu: w Mikserze | attendu: w Miksie (nom de l'onglet)
  - ecran: 118 | vu: około −8,5 LUFS | attendu: −9 LUFS (bouton « Auto (styl) · −9 LUFS »)
defauts-avant: n/a

note-siege: le premier « regarde » (écran 1) a été fait par le siège pour vérifier l amenée, avant le sous-agent.
fin-tranche: non (dernier step=levels, done=33)
