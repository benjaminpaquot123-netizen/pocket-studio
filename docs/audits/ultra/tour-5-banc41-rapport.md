# Tour 5, banc 4.1 : P3 « tape », les vraies causes du 0-1 étoile au défi

Branche `eq/tour-5-banc41`. Ligne 4.1 de `PLAN-RESTE.md`. Aucun code principal de l'appli modifié.

## En bref

- Le **1 étoile du banc** venait surtout **du banc lui-même** : `tape … 500` tapait en réalité toutes les **590 ms** environ
  (horloge du banc qui glisse). Corrigé : 500 ms demandées = 500 ms mesurées, **3 étoiles**.
- **(d) confirmée** (le défi bat au tempo du projet, 80 à 120 BPM) mais **l'appli a raison** : 500 ms sur un défi à 90 BPM
  donnent 3 sur 4, 2 étoiles. Au passage, un **vrai défaut** : ce joueur trop rapide s'entend dire « Un peu trop **lent** ».
- **(c') confirmée** : un toucher pendant « Écoute » ne compte pas (voulu), mais le toucher qui tombe pile sur le temps
  après l'exemple se perd aussi (défaut).
- **La remarque de Benjamin** vient du **moteur**, pas du défi : un pad « coup » touché pendant que son clap sonne encore
  attend la prochaine ligne de temps du projet, et un deuxième toucher pendant cette attente l'annule.

## Causes

| Cause | Verdict | Preuve |
|---|---|---|
| (d) le défi compte en temps du projet (80-120 BPM) | **confirmée, l'appli a raison.** 4 touchers à 500 ms : 3 étoiles à 120 BPM, **3/4, 2 étoiles** à 90 BPM (et non « 2 sur 4 » comme le disait le plan : le 2e toucher tombe pile sur la tolérance de 1/4 de temps). L'exemple et la lumière de tempo battent 90 : c'est au testeur de connaître la pulsation, d'où la ligne de son du banc. | `ChallengeControllerTest#fiveHundredMsTapsAreScoredOnTheProjectsTempo` (normal) |
| (d) bis : tendance inversée | **défaut de l'appli.** Même essai : « Bravo ! 3 sur 4. Un peu trop lent. » alors que chaque écart est 33 % trop court. `PadChallenge.score` compare l'empan des coups appariés (coups 1 à 3 = 2 temps) à celui des touchers appariés (touchers 1 et 4 = 2,25 temps) : le 4e toucher, pris pour le 3e coup, fait passer un essai pressé pour un essai traînant. | `ChallengeControllerTest#touchesTooCloseAreSaidTooFast` **@KnownDefect T5-DEFI-TENDANCE** |
| (c') touchers pendant « Écoute » ignorés | **confirmée, voulue** : pendant l'exemple, `tapped()` sort tout de suite (`_ui` n'est pas `YourTurn`) ; le pad sonne quand même. | `ChallengeControllerTest#touchesWhileTheExampleIsHeardAreNotCounted` (normal) |
| (c') bord : le temps juste après l'exemple | **défaut de l'appli.** L'exemple finit par un temps de résonance après son dernier coup (`PadChallenge.exampleFrames`) ; un joueur qui répond en rythme (le temps suivant, un poil en avance) touche alors que l'écran dit encore « Écoute » : le toucher sonne, ne compte pas, l'essai démarre décalé d'un coup. | `ChallengeControllerTest#aTouchOnTheBeatAfterTheExampleCounts` **@KnownDefect T5-DEFI-ECOUTE** |
| Banc : `tape` ne tenait pas le tempo | **cause réelle du 1 étoile du banc.** `tape` faisait `pass(60)` puis `pass(every - 60)` ; `pass()` laisse filer plus que demandé (pas de 50 ms + horloge des images Compose). Mesuré au toucher (`ChallengeController.now`) : 23460, 24055, 24645, 25235 ms, soit **~590 ms** pour 500 demandées (projet à 120 BPM, défi à 500 ms) : 2/4, 1 étoile. | `BenchAmeneesTest#aTappedRhythmIsScored` (rouge puis vert) |
| Touchers plus rapprochés que le clap refusés ? | **réfutée côté défi** : chaque toucher est compté et envoyé au moteur, pad en train de sonner ou non. Le galop (croches, 333 ms à 90 BPM) : 6/6, 3 étoiles, 6 coups envoyés. | `ChallengeControllerTest#touchesCloserThanTheClapAreAllCountedAndSent`, `#aHumanHandOnASoundingPadStillGetsThreeStars` (normaux) |

Le cas témoin qui marche : `ChallengeControllerTest#tapsAtTheChallengesBeatAfterAMoiGiveThreeStars` (90 BPM, touchers à
`c.beatMs` = 666,7 ms après « À moi ! » : 4/4, 3 étoiles).

## @KnownDefect posés

| Id | Message attendu (le test dit le bon comportement) | Test |
|---|---|---|
| `T5-DEFI-TENDANCE` | `four touches 500 ms apart on a 90 BPM challenge are said too slow` | `ChallengeControllerTest#touchesTooCloseAreSaidTooFast` |
| `T5-DEFI-ECOUTE` | `a touch on the beat after the example's last hit was not counted` | `ChallengeControllerTest#aTouchOnTheBeatAfterTheExampleCounts` |

Les deux sont verts tant que l'appli échoue avec exactement ce message (`KnownDefectRule`). `verdicts.tsv` non touché.

## Ce que veut dire la remarque de Benjamin (téléphone, 2026-11-08)

« J'arrive pas à tenir le rythme, comme si tant que le clap n'était pas fini il n'acceptait pas mon appui suivant. »

Côté défi, rien ne refuse le toucher (`PadLiveController.touch` appelle `challenge.tapped(pad)` à chaque appui, avant tout le
reste). C'est le **son** qui ment, dans le moteur, `engine/src/main/cpp/core/ClipLauncher.cpp`, `ClipLauncher::toggle` :

- un pad « coup » a `beats = 1`, donc sa grille (`gridOf`) est **un temps du tempo du projet** ;
- lecture arrêtée et **plus rien qui sonne** : l'horloge repart à zéro, le coup part **tout de suite** ;
- mais si le clap précédent **sonne encore** (« A one-shot that is still sounding is simply hit again on the grid »), le
  nouveau coup passe en `Waiting` jusqu'à la **prochaine ligne de temps** de l'horloge lancée au premier toucher ;
- et un toucher pendant ce `Waiting` **l'annule** (`Waiting -> Off`) : pas de son du tout.

Sonde hors dépôt (ClipLauncher seul, 48 kHz, lecture arrêtée, 4 touchers, source ci-dessous) :

| Projet | Clap | Touchers toutes les | Sons entendus à (ms) |
|---|---|---|---|
| 90 BPM | 400 ms | 500 ms | 0, 500, 1000, 1500 (le clap est fini à chaque fois : tout de suite) |
| 90 BPM | 400 ms | 333 ms (croches) | 0, (le 3e annule le 2e qui attendait), 999 : **2 sons pour 4** |
| 90 BPM | 800 ms | 650 ms (un poil en avance) | 0, 666, 1333, 2000 : le son suit la grille, pas le doigt |
| 90 BPM | 800 ms | 667 ms (pile, 1 ms après le temps) | 0, **1333**, 2000, 2666 : le 2e coup arrive **un temps entier en retard** |
| 120 BPM | 400 ms | 333 ms | 0, 500, (le 4e annule le 3e) : **2 sons pour 4** |

C'est exactement « tant que le clap n'est pas fini, mon appui n'est pas pris » : l'appui est compté par le défi, mais on
l'entend en retard ou pas du tout, donc on ne peut pas tenir le rythme à l'oreille. Pas de garde Kotlin possible
(Robolectric n'a pas le moteur natif). À faire (moteur, hors de cette ligne) : un « coup » touché lecture arrêtée devrait
repartir **tout de suite** même s'il sonne encore (ou au moins ne pas être annulé par le toucher suivant), avec une garde
`engine/host-tests` rouge avant / verte après.

<details><summary>Source de la sonde (compilée contre <code>ClipLauncher.cpp</code> + <code>Sample.cpp</code> + libsamplerate)</summary>

```cpp
#include "ClipLauncher.h"
#include <cstdio>
#include <set>
#include <vector>
int main() {
    const int rate = 48000;
    for (int clapMs : {400, 800}) for (double bpm : {90.0, 120.0}) for (int every : {250, 333, 500, 650, 667, 690}) {
        ps::ClipLauncher clips;
        clips.prepare(rate);
        clips.setBeatFrames(rate * 60.0 / bpm);
        std::vector<float> clap(size_t(rate) * clapMs / 1000, 0.5f);
        clips.install(0, ps::Sample::create(clap.data(), int64_t(clap.size()), 1, rate, rate));
        clips.setParam(0, ps::ClipLauncher::Beats, 1.0f);
        clips.setParam(0, ps::ClipLauncher::OneShot, 1.0f);
        std::set<int> taps; for (int i = 0; i < 4; ++i) taps.insert(i * every);
        std::vector<float> l(48), r(48);
        std::printf("%.0f BPM, every %d ms, clap %d ms: sounds at", bpm, every, clapMs);
        for (int ms = 0; ms < 3000; ++ms) {  // 1 ms blocks, transport stopped (idleClock = true)
            const int before = clips.state(0);
            if (taps.count(ms)) clips.toggle(0, true);
            const int now = clips.state(0);
            clips.render(l.data(), r.data(), 48);
            if (clips.state(0) == ps::ClipLauncher::Playing && now != ps::ClipLauncher::Playing) std::printf(" %d", ms);
            if (taps.count(ms) && before == ps::ClipLauncher::Waiting) std::printf(" [tap %d cancels]", ms);
        }
        std::printf("\n");
        clips.collectGarbage();
    }
}
```
</details>

## Côté banc (`HumanTester.kt`, `scripts/human-act.py`)

- La ligne « Son : » dit, tant qu'un défi est ouvert : « défi « Boum boum » : pulsation 120 BPM = 500 ms entre deux temps ».
- `tape X Y N tempo` : tape à la pulsation du défi ouvert (sinon le banc le dit et ne tape rien).
- `tape` donné pendant l'exemple : le banc le dit et attend « À toi ! » (20 s au plus), puis tape.
- `tape` place chaque toucher à `début + i × intervalle` (temps passé par pas de 8 ms) : 10810, 11310, 11810, 12310 ms mesurés.
- Effet de bord à surveiller : `pass(ms)` laisse filer plus que `ms` ailleurs aussi (`attends`, `touche`) ; sans effet connu
  sur les autres verdicts, non corrigé ici.

## Tests et commits

| Commit | Contenu |
|---|---|
| `31a4690` | `ChallengeControllerTest` (7 tests, dont 2 @KnownDefect) |
| `d00af4c` | **rouge** : `BenchAmeneesTest#aTappedRhythmIsScored` (lit la pulsation, tape après « À moi ! », exige 3 étoiles) et `#aTapRightAfterListeningWaitsForTheTurn` (« tape … tempo » juste après « Écouter », 3 étoiles). Rouges : pas de pulsation sur la ligne de son ; « tape … tempo » = geste mal écrit. |
| `0f5efde` | **vert** : changement du banc ci-dessus (+ le test « après Écouter » vérifie que l'exemple joue bien d'abord). |

Dernier passage : `ChallengeControllerTest` 7/7, `BenchAmeneesTest` 8/8 (+1 `drive` ignoré, comme avant),
`HumanPadLivePersonasTest` 3/3 (+1 `drive`), `PadLiveChallengeTest` 3/3.

## T5-Ca-32 tranché

« 0 ou 1 étoile en tapant en rythme » : **surtout le banc** (tape à ~590 ms au lieu de 500), plus deux défauts réels de
l'appli (T5-DEFI-TENDANCE, T5-DEFI-ECOUTE) et, sur téléphone, le retour sonore du moteur décrit plus haut.

## Suite de la relecture (c034963)

- Fusion de `fx-int` : `63a2f8f` (sans conflit).
- **Grave, suivi des défauts** : garde moteur `engine/host-tests/padlive_tests.inc` `padlive_known_defect_t5_padretrig`
  (`b91b3a8`) : elle vérifie le comportement d'aujourd'hui (attente sur la grille du projet, annulation par le toucher
  suivant, toucher d'après qui repart tout de suite, grille à 429 ms sur un projet à 140 BPM), verte ; une correction du
  moteur (coup relancé tout de suite) la fait passer au rouge (vérifié par mutation, 3 échecs) : il faudra alors l'inverser.
  `verdicts.tsv` : T5-DEFI-TENDANCE, T5-DEFI-ECOUTE, T5-PADRETRIG ajoutés, T5-Ca-32 tranché ; `PLAN-RESTE.md` §1 bis R.7
  (`eb14aaa`).
- **Banc moins gentil** (`bc02b31`) : `tape` = une main (±30 ms, tirage fixé par le siège et le numéro du geste) ; plus
  d'attente de « À toi ! » à la place du testeur (pendant l'exemple, il tape tout de suite et rien ne compte) ; la ligne de
  son dit « pulsation ~N BPM » (et « exemple en cours »), plus de millisecondes. Les paragraphes « Côté banc » ci-dessus
  sur l'attente et les ms sont donc remplacés. `BenchAmeneesTest` : 120 BPM (3 étoiles avec la gigue), **90 BPM** avec
  `tempo` (cause d, 3 étoiles), touchers pendant l'exemple non comptés puis 3 étoiles une fois « À toi ! ».
- **Trou de mutation** (`f7f5002`) : `ChallengeControllerTest#aTouchFarFromItsPlaceIsNotCountedANearOneIs` (0,4 temps
  de retard : pas compté ; 0,1 : compté) ; une tolérance élargie à 0,5 temps le fait rougir sans dépendre de la garde.
- Derniers passages : host-test `padlive_known_defect_t5_padretrig` OK ; `ChallengeControllerTest` 8/8,
  `BenchAmeneesTest` 9/9 (+`drive` ignoré), `HumanPadLivePersonasTest` 3/3 (+`drive`), `PadLiveChallengeTest` 3/3.
