#!/usr/bin/env python3
"""
 Sunday burst (Python, stdlib only).

Models the Sunday load:
  - 200 matches, one league
  - arrive in clumps of 30 (within a few seconds each)
  - ~12% corrections (same match id, new scores) after the burst

Run: python3 loadgen/load_burst.py
"""

import json
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed

BASE = "http://localhost:8080"
TOKEN = "dev-organiser-token"
LEAGUE = "sunday-league"
MATCHES = 200
CLUMP = 30          # clumps of 20–30
PAUSE_SEC = 1.0     # gap between clumps (demo-scale "Sunday window")
CORRECTIONS = 24    # ~12% — same match id, different score
TEAMS = [
    "Alpha", "Beta", "Gamma", "Delta", "Epsilon",
    "Zeta", "Eta", "Theta", "Iota", "Kappa",
]

URL = f"{BASE}/api/leagues/{LEAGUE}/results"


def send(method: str, url: str, body: dict) -> None:
    req = urllib.request.Request(
        url,
        data=json.dumps(body).encode(),
        method=method,
        headers={
            "Content-Type": "application/json",
            "X-Organiser-Token": TOKEN,
        },
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        resp.read()


def main() -> None:
    stamp = int(time.time())
    jobs = []
    for i in range(MATCHES):
        home = TEAMS[i % len(TEAMS)]
        away = TEAMS[(i + 3) % len(TEAMS)]
        jobs.append({
            "matchId": f"b-{stamp}-{i:04d}",
            "homeTeam": home,
            "awayTeam": away,
            "homeScore": i % 4,
            "awayScore": (i * 3) % 4,
        })

    print(f"league={LEAGUE} matches={MATCHES} clump={CLUMP} corrections={CORRECTIONS}")
    print("submitting clumps through REST → Kafka…")

    for start in range(0, MATCHES, CLUMP):
        batch = jobs[start : start + CLUMP]
        print(f"  clump {start + 1}-{start + len(batch)} ({len(batch)} in parallel)")
        with ThreadPoolExecutor(max_workers=len(batch)) as pool:
            futs = [pool.submit(send, "POST", URL, body) for body in batch]
            for f in as_completed(futs):
                f.result()
        if start + CLUMP < MATCHES:
            time.sleep(PAUSE_SEC)

    #  corrections minutes later — short pause then same match ids, new scores
    print("waiting before corrections…")
    time.sleep(2)
    print(f"correcting {CORRECTIONS} (same match id, new scores)…")
    with ThreadPoolExecutor(max_workers=CLUMP) as pool:
        futs = []
        for i, body in enumerate(jobs[:CORRECTIONS]):
            corr = {
                "homeTeam": body["homeTeam"],
                "awayTeam": body["awayTeam"],
                "homeScore": (i + 1) % 5,
                "awayScore": i % 3,
            }
            futs.append(pool.submit(send, "PUT", f"{URL}/{body['matchId']}", corr))
        for f in as_completed(futs):
            f.result()

    print(f"done — watch http://localhost:3000 (league={LEAGUE})")


if __name__ == "__main__":
    main()
