"""The repository is public: no audio file is added to git (only lists, sums and scripts).
The few sounds already tracked (party effects made in house, test fixtures) are frozen."""
import subprocess
import unittest

from helpers import REPO

AUDIO = ('.wav', '.flac', '.mp3', '.ogg', '.oga', '.opus', '.aif', '.aiff', '.m4a', '.aac', '.sfz', '.sf2')
# Already tracked before the packs went public (in-house party sounds, test fixtures): frozen, never more.
FROZEN = {
    'app/src/main/assets/party/air_horn.ogg',
    'app/src/main/assets/party/air_horn_dancehall.ogg',
    'app/src/main/assets/party/air_horn_long.ogg',
    'app/src/main/assets/party/drop.ogg',
    'app/src/main/assets/party/impact.ogg',
    'app/src/main/assets/party/laser.ogg',
    'app/src/main/assets/party/laser_triple.ogg',
    'app/src/main/assets/party/riser.ogg',
    'app/src/main/assets/party/riser_short.ogg',
    'app/src/main/assets/party/siren_dub.ogg',
    'app/src/main/assets/party/siren_wail.ogg',
    'app/src/main/assets/party/siren_yelp.ogg',
    'app/src/main/assets/party/voice_fire.ogg',
    'app/src/main/assets/party/voice_go.ogg',
    'app/src/main/assets/party/voice_go_go_go.ogg',
    'app/src/main/assets/party/voice_hurry_up.ogg',
    'app/src/main/assets/party/voice_level_up.ogg',
    'app/src/main/assets/party/voice_power_up.ogg',
    'app/src/main/assets/party/voice_ready.ogg',
    'app/src/main/assets/party/voice_you_win.ogg',
    'app/src/main/assets/party/whistle.ogg',
    'app/src/main/assets/party/zap.ogg',
    'app/src/test/resources/real-projects/brouillon-jumpup/samples/05bf504a-b2f.wav',
    'app/src/test/resources/real-projects/brouillon-jumpup/samples/1ac2fbe3-3f6.wav',
    'app/src/test/resources/real-projects/brouillon-jumpup/samples/28456cfe-cf2.wav',
    'app/src/test/resources/real-projects/brouillon-jumpup/samples/7a33e3a2-a6d.wav',
    'app/src/test/resources/real-projects/brouillon-jumpup/samples/accd5484-e87.wav',
    'app/src/test/resources/real-projects/brouillon-jumpup/samples/af4f056e-c38.wav',
    'app/src/test/resources/reference-projects/enorme/samples/1ac2fbe3-3f6.wav',
    'app/src/test/resources/reference-projects/enorme/samples/28456cfe-cf2.wav',
    'app/src/test/resources/reference-projects/enorme/samples/7a33e3a2-a6d.wav',
    'app/src/test/resources/reference-projects/enorme/samples/accd5484-e87.wav',
    'app/src/test/resources/reference-projects/enorme/samples/af4f056e-c38.wav',
    'docs/audits/ultra/sieges/fumee-finale/lotA/d/etat/partage/Lo-Fi-Hip-Hop-song-20261112-043521.m4a',
    'scripts/bench/oracles-niveaux/pistes-separees/ne-tient-pas/etat/partage/Trois.flac',
    'scripts/bench/oracles-niveaux/projet-vide/tient/etat/partage/Vide.wav',
}


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
        self.assertEqual([], sorted(set(tracked) - FROZEN))


if __name__ == '__main__':
    unittest.main()
