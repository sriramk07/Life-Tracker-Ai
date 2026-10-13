import http.server
import socketserver
import webbrowser
import os
import sys

PORT = 8000
WEB_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "web")

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=WEB_DIR, **kwargs)

def run():
    os.chdir(WEB_DIR)
    url = f"http://localhost:{PORT}"
    print("=======================================================")
    print("  LifeOS AI Web Dashboard Running Locally!")
    print(f"  Opening browser at: {url}")
    print("  Press Ctrl+C in this terminal to stop the server.")
    print("=======================================================")
    sys.stdout.flush()
    try:
        webbrowser.open(url)
    except Exception:
        pass

    with socketserver.TCPServer(("", PORT), Handler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nShutting down server...")

if __name__ == "__main__":
    run()
