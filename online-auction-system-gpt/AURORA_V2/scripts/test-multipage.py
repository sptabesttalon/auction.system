#!/usr/bin/env python3
"""Verify the standalone Java JAR serves the real multi-page URLs.
Runs on a random loopback port without Maven and with a temporary demo database.
Browser navigation still needs a real browser smoke-test on the target machine.
"""
import os
from pathlib import Path
import socket
import subprocess
import tempfile
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parent.parent
JAR = ROOT / 'dist/AURORA-server.jar'
PAGES = {
    '/': ('AURORA', 'ĐĂNG NHẬP ĐỂ ĐẤU GIÁ', 'data-page="home"'),
    '/login': ('Đăng nhập', 'loginForm', 'data-page="login"'),
    '/register': ('Tạo tài khoản', 'registerForm', 'data-page="register"'),
    '/auctions': ('Danh sách đấu giá', 'auctionGrid', 'data-page="list"'),
    '/auctions/1': ('Chi tiết phiên đấu giá', 'detailContent', 'data-page="detail"'),
    '/sell/new': ('Đăng sản phẩm', 'createForm', 'data-page="create"'),
    '/auctions/1/edit': ('Sửa sản phẩm', 'editForm', 'data-page="edit"'),
}


def fetch(base, path):
    try:
        with urllib.request.urlopen(base + path, timeout=4) as response:
            return response.status, response.read().decode('utf-8'), response.headers
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode('utf-8', 'replace'), error.headers


def main():
    assert JAR.exists(), 'Build the jar first'
    with tempfile.TemporaryDirectory(prefix='aurora-multipage-') as temp:
        with socket.socket() as sock:
            sock.bind(('127.0.0.1', 0))
            port = sock.getsockname()[1]
        env = os.environ.copy()
        env.update(AUCTION_HOST='127.0.0.1', AUCTION_PORT=str(port), AUCTION_DEMO='1',
                   AUCTION_DATA=str(Path(temp) / 'fixtures.dat'))
        process = subprocess.Popen(['java', '--add-modules', 'jdk.httpserver', '-jar', str(JAR)],
                                   env=env, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        base = f'http://127.0.0.1:{port}'
        try:
            for _ in range(50):
                try:
                    code, _, _ = fetch(base, '/')
                    if code == 200:
                        break
                except OSError:
                    time.sleep(.1)
            else:
                raise AssertionError('Server did not start')
            for route, fragments in PAGES.items():
                code, html, headers = fetch(base, route)
                assert code == 200, (route, code)
                for fragment in fragments:
                    assert fragment in html, (route, fragment)
                assert headers['Content-Type'].startswith('text/html'), route
                assert 'modalLayer' not in html and 'modal-backdrop' not in html, route
                if route in ('/auctions', '/auctions/1', '/sell/new', '/auctions/1/edit'):
                    assert 'id="protectedMain" hidden' in html, route
            code, js, _ = fetch(base, '/app.js')
            assert code == 200 and 'location.assign' in js and 'href="/auctions/${a.id}"' in js
            assert 'showModal(' not in js and 'window.confirm' not in js and 'confirm(' not in js
            code, css, _ = fetch(base, '/style.css')
            assert code == 200 and '.btn-login-large' in css and '.auction-details-grid' in css
            for missing in ('/auctions/abc', '/sell/1', '/random.html'):
                assert fetch(base, missing)[0] == 404, missing
            print('PASS: seven real HTML routes, CSS/JS, protected hidden pages, no modal markup, unknown path 404')
        finally:
            process.terminate()
            try:
                process.wait(timeout=4)
            except subprocess.TimeoutExpired:
                process.kill()


if __name__ == '__main__':
    main()
