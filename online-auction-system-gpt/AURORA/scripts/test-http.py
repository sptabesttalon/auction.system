#!/usr/bin/env python3
"""Local end-to-end REST smoke test, requires Python3 + JDK21 only.
Starts its own HTTP server on an available loopback port and uses temporary data.
"""
import json
import os
from pathlib import Path
import socket
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor

PROJECT = Path(__file__).resolve().parent.parent


def request(base, path, method="GET", data=None, token=None):
    raw = None if data is None else json.dumps(data).encode()
    req = urllib.request.Request(base + path, data=raw, method=method)
    if data is not None:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=5) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as exc:
        return exc.code, json.loads(exc.read().decode())


def main():
    with tempfile.TemporaryDirectory(prefix="aurora-http-test-") as directory:
        cls = Path(directory) / "classes"
        cls.mkdir()
        source_paths = [str(p) for p in (PROJECT / "server/src/main/java").rglob("*.java")]
        subprocess.run(["javac", "--add-modules", "jdk.httpserver", "-encoding", "UTF-8", "-d", str(cls), *source_paths], check=True)
        with socket.socket() as sock:
            sock.bind(("127.0.0.1", 0))
            port = sock.getsockname()[1]
        env = os.environ.copy()
        env.update(AUCTION_PORT=str(port), AUCTION_HOST="127.0.0.1", AUCTION_DATA=str(Path(directory) / "db.dat"))
        # Resources are separate classpath root, no need to copy into build directory.
        cp = os.pathsep.join([str(cls), str(PROJECT / "server/src/main/resources")])
        proc = subprocess.Popen(["java", "--add-modules", "jdk.httpserver", "-cp", cp,
                                 "vn.uet.auction.Main"], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        base = f"http://127.0.0.1:{port}"
        try:
            for _ in range(40):
                try:
                    status, _ = request(base, "/api/health")
                    if status == 200:
                        break
                except OSError:
                    time.sleep(.15)
            else:
                raise AssertionError("Server did not start")
            for username, role in [("seller_test", "SELLER"), ("alice_test", "BIDDER"), ("bob_test", "BIDDER")]:
                status, user = request(base, "/api/auth/register", "POST", dict(username=username, password="password123", role=role))
                assert status == 200, user
            tokens = {}
            for name in ["seller_test", "alice_test", "bob_test"]:
                status, obj = request(base, "/api/auth/login", "POST", dict(username=name, password="password123"))
                assert status == 200
                tokens[name] = obj["token"]
            start = int(time.time() * 1000)
            status, auction = request(base, "/api/auctions", "POST",
                dict(category="ELECTRONICS", title="Laptop HTTP e2e", description="Đấu giá song song", startPrice=1000,
                     startTime=start, endTime=start + 180000), tokens["seller_test"])
            assert status == 200, auction
            key = auction["id"]
            stream = urllib.request.urlopen(base + "/api/events", timeout=6)
            assert stream.readline().startswith(b": connected")
            status, _ = request(base, f"/api/auctions/{key}/bid", "POST", dict(amount=1500), tokens["seller_test"])
            assert status == 403
            status, _ = request(base, f"/api/auctions/{key}/bid", "POST", dict(amount=2000), tokens["alice_test"])
            assert status == 200
            event_line = stream.readline().decode().strip()  # empty line after : connected
            if not event_line:
                event_line = stream.readline().decode().strip()
            assert event_line.startswith("data: ") and "bid_placed" in event_line, event_line
            stream.close()
            status, _ = request(base, f"/api/auctions/{key}/bid", "POST", dict(amount=1000), tokens["bob_test"])
            assert status == 422
            with ThreadPoolExecutor(max_workers=2) as executor:
                tasks = [executor.submit(request, base, f"/api/auctions/{key}/bid", "POST", dict(amount=3000), tokens[name])
                         for name in ("alice_test", "bob_test")]
                results = [task.result() for task in tasks]
            assert sorted(code for code, _ in results) == [200, 422], results
            status, detail = request(base, f"/api/auctions/{key}")
            assert status == 200 and detail["currentPrice"] == 3000 and len(detail["history"]) == 2, detail
            status, _ = request(base, f"/api/auctions/{key}/auto", "POST", dict(maxAmount=8000, increment=1000), tokens["alice_test"])
            assert status == 200
            status, current = request(base, f"/api/auctions/{key}/bid", "POST", dict(amount=5000), tokens["bob_test"])
            assert status == 200 and current["currentPrice"] == 6000, current
            with urllib.request.urlopen(base + "/", timeout=4) as html:
                assert html.status == 200 and b"AURORA" in html.read()
            print("PASS: HTTP register/login, seller creation, RBAC, invalid bid, concurrent equal bids, auto-bid, SSE push")
        finally:
            proc.terminate()
            try:
                proc.wait(timeout=4)
            except subprocess.TimeoutExpired:
                proc.kill()


if __name__ == "__main__":
    main()
