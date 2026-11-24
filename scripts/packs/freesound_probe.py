#!/usr/bin/env python3
"""Freesound witness run: proves that this machine can search, fetch a preview and (with a fresh
OAuth2 code) fetch an original. Nothing is kept: every file is measured, identified and deleted.

Secrets come from the environment only and are never printed:
  FREESOUND_API_KEY   the API key, which is also the OAuth2 client secret
  FREESOUND_CLIENT_ID the public client id
  OAUTH_CODE, PKCE_VERIFIER  optional; a code lives 10 minutes and works once
Standard library only.
"""
import json
import os
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request

BASE = os.environ.get("FREESOUND_BASE", "https://freesound.org")
AGENT = "pocket-studio-packs/1.0"

MAGIC = [(b"RIFF", "wav"), (b"fLaC", "flac"), (b"OggS", "ogg"), (b"FORM", "aiff"),
         (b"ID3", "mp3"), (b"\xff\xfb", "mp3"), (b"\xff\xf3", "mp3")]


def mask(value):
    """Tells GitHub Actions to hide this value in every later log line."""
    if value and os.environ.get("GITHUB_ACTIONS"):
        print(f"::add-mask::{value}", flush=True)


def call(url, headers=None, data=None):
    """Returns (status, headers, body). An HTTP error is a result here, not an exception."""
    request = urllib.request.Request(url, data=data, headers={"User-Agent": AGENT, **(headers or {})})
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            return response.status, response.headers, response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.headers, error.read()


def fail(step, status, headers):
    # Never the body: a refused token exchange could echo what was sent.
    print(f"FAIL {step}: HTTP {status}, server={headers.get('server', '-')}")
    sys.exit(1)


def measure(step, body):
    """Writes the bytes to a temporary file, reports size and format, deletes the file."""
    with tempfile.NamedTemporaryFile(delete=False) as handle:
        handle.write(body)
        path = handle.name
    size = os.path.getsize(path)
    with open(path, "rb") as handle:
        head = handle.read(4)
    os.remove(path)
    kind = next((name for magic, name in MAGIC if head.startswith(magic)), "unknown")
    print(f"OK {step}: {size} bytes, format {kind}, deleted")
    if size < 1000 or kind == "unknown":
        print(f"FAIL {step}: not a sound file")
        sys.exit(1)


def main():
    key = os.environ.get("FREESOUND_API_KEY", "").strip()
    if not key:
        print("FAIL: secret FREESOUND_API_KEY is not set")
        return 1
    mask(key)
    code = os.environ.get("OAUTH_CODE", "").strip()
    verifier = os.environ.get("PKCE_VERIFIER", "").strip()
    mask(code)
    mask(verifier)
    query = os.environ.get("FREESOUND_QUERY", "").strip() or "clap"
    api_auth = {"Authorization": f"Token {key}"}

    params = urllib.parse.urlencode({
        "query": query, "filter": 'license:"Creative Commons 0" duration:[0.05 TO 3]',
        "fields": "id,name,license,type,filesize,previews", "page_size": 5})
    status, headers, body = call(f"{BASE}/apiv2/search/text/?{params}", api_auth)
    if status != 200:
        fail("search", status, headers)
    results = json.loads(body).get("results", [])
    if not results:
        print(f"FAIL search: no CC0 result for '{query}'")
        return 1
    sound = results[0]
    print(f"OK search: {len(results)} results, first is sound {sound['id']} ({sound['type']}, "
          f"{sound['filesize']} bytes, {sound['license']})")

    status, headers, body = call(sound["previews"]["preview-hq-ogg"])
    if status != 200:
        fail("preview", status, headers)
    measure("preview", body)

    if not code:
        print("No oauth_code given: originals not tried.")
        return 0
    fields = {"client_id": os.environ.get("FREESOUND_CLIENT_ID", ""), "client_secret": key,
              "grant_type": "authorization_code", "code": code}
    if verifier:
        fields["code_verifier"] = verifier
    status, headers, body = call(f"{BASE}/apiv2/oauth2/access_token/",
                                 data=urllib.parse.urlencode(fields).encode())
    if status != 200:
        fail("token exchange", status, headers)
    token = json.loads(body).get("access_token", "")
    mask(token)
    mask(json.loads(body).get("refresh_token", ""))
    print("OK token exchange (token used for this run only, never stored)")

    status, headers, body = call(f"{BASE}/apiv2/sounds/{sound['id']}/download/",
                                 {"Authorization": f"Bearer {token}"})
    if status != 200:
        fail("original", status, headers)
    measure("original", body)
    if len(body) != sound["filesize"]:
        print(f"FAIL original: {len(body)} bytes, Freesound announced {sound['filesize']}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
