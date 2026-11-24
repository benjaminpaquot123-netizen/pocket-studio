"""The repository is public: no audio file is added to git (only lists, sums and scripts).
The few sounds already tracked (party effects made in house, test fixtures) are frozen."""
import subprocess
import unittest

from helpers import REPO

AUDIO = ('.wav', '.flac', '.mp3', '.ogg', '.oga', '.opus', '.aif', '.aiff', '.m4a', '.aac', '.sfz', '.sf2')
# Already tracked before the packs went public (in-house party sounds, test fixtures): frozen, never more.
TRACKED_ALLOWED = (
    'app/src/main/assets/party/',
    'app/src/test/resources/real-projects/brouillon-jumpup/samples/',
    'app/src/test/resources/reference-projects/enorme/samples/',
    'docs/audits/ultra/sieges/fumee-finale/lotA/d/etat/partage/',
    'scripts/bench/oracles-niveaux/pistes-separees/ne-tient-pas/etat/partage/',
    'scripts/bench/oracles-niveaux/projet-vide/tient/etat/partage/',
)
TRACKED_MAX = 36


def git(*args):
    return subprocess.run(['git', '-C', str(REPO), *args], check=True, capture_output=True, text=True).stdout


def audio(paths):
    return [p for p in paths if p.lower().endswith(AUDIO)]


class NoAudioInGit(unittest.TestCase):
    def test_status_has_no_audio(self):
        # Untracked, staged or modified: anything git would pick up (ignored files stay out).
        paths = [line[3:].split(' -> ')[-1].strip('"') for line in git('status', '--porcelain', '-uall').splitlines()]
        self.assertEqual([], audio(paths))

    def test_tracked_audio_is_only_the_frozen_party_sounds(self):
        tracked = audio(git('ls-files').splitlines())
        self.assertEqual([], [p for p in tracked if not p.startswith(TRACKED_ALLOWED)])
        self.assertLessEqual(len(tracked), TRACKED_MAX)


if __name__ == '__main__':
    unittest.main()
