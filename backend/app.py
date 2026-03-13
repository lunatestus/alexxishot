import asyncio
import fcntl
import json
import mimetypes
import os
import pty
import re
import select
import signal
import struct
import subprocess
import termios
import time
from pathlib import Path
from typing import Iterator, Tuple
from urllib.parse import quote

import modal

APP_NAME = "vibe-backend"
VOLUME_NAME = "vibe-media"
MOUNT_PATH = "/vol"
MEDIA_ROOT = Path(MOUNT_PATH).resolve()
SERVER_PORT = 8080
HEALTH_URL = f"http://localhost:{SERVER_PORT}/health"
RUN_HEARTBEAT_TTL_SECONDS = 15
START_LOCK_TTL_SECONDS = 20 * 60

image = (
    modal.Image.debian_slim()
    .apt_install(
        "ca-certificates",
        "curl",
        "ffmpeg",
        "aria2",
        "sudo",
        "python3",
        "python3-pip",
    )
    .run_commands(
        "pip3 install -U pip",
        "pip3 install fastapi uvicorn[standard]",
        "curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp",
        "chmod a+rx /usr/local/bin/yt-dlp",
    )
    .run_commands(
        "curl -fsSL https://deb.nodesource.com/setup_22.x | bash -",
        "apt-get install -y nodejs",
        "npm install -g @google/gemini-cli",
    )
    .run_commands(
        "curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o /usr/local/bin/cloudflared",
        "chmod +x /usr/local/bin/cloudflared",
    )
    .add_local_file(
        os.path.join(os.path.dirname(__file__), "terminal.html"),
        "/app/terminal.html",
        copy=True,
    )
)

app = modal.App(APP_NAME, image=image)
media_volume = modal.Volume.from_name(VOLUME_NAME, create_if_missing=True)
cf_url_store = modal.Dict.from_name("vibe-backend-cf", create_if_missing=True)


def _resolve_path(raw_path: str) -> Path:
    if not raw_path:
        raw_path = "/"
    if not raw_path.startswith("/"):
        raw_path = f"/{raw_path}"
    rel = raw_path.lstrip("/")
    full = (MEDIA_ROOT / rel).resolve()
    if full != MEDIA_ROOT and MEDIA_ROOT not in full.parents:
        raise ValueError("Invalid path")
    return full


def _iter_file(
    path: Path, start: int, length: int, chunk_size: int = 1024 * 1024
) -> Iterator[bytes]:
    with path.open("rb") as f:
        f.seek(start)
        remaining = length
        while remaining > 0:
            chunk = f.read(min(chunk_size, remaining))
            if not chunk:
                break
            remaining -= len(chunk)
            yield chunk


def _read_runtime_state() -> Tuple[dict | None, Exception | None]:
    try:
        return {
            "url": cf_url_store.get("url"),
            "status": cf_url_store.get("status"),
            "heartbeat": cf_url_store.get("heartbeat"),
            "launching": cf_url_store.get("launching"),
        }, None
    except Exception as e:
        return None, e


def _is_run_alive(state: dict, now_ts: float | None = None) -> bool:
    heartbeat = state.get("heartbeat")
    if not isinstance(heartbeat, (int, float)):
        return False
    now_ts = now_ts if now_ts is not None else time.time()
    return (now_ts - heartbeat) <= RUN_HEARTBEAT_TTL_SECONDS


def _has_live_url(state: dict, now_ts: float | None = None) -> bool:
    return bool(state.get("url")) and _is_run_alive(state, now_ts)


def _no_cache_headers() -> dict:
    return {
        "Cache-Control": "no-store, no-cache, must-revalidate, max-age=0",
        "Pragma": "no-cache",
        "Expires": "0",
    }


def create_api_app():
    from fastapi import FastAPI, HTTPException, Request, WebSocket, WebSocketDisconnect
    from fastapi.responses import HTMLResponse, JSONResponse, StreamingResponse

    api_app = FastAPI()

    @api_app.get("/health")
    def health() -> dict:
        return {"ok": True}

    @api_app.get("/list")
    def list_items(path: str = "/") -> JSONResponse:
        try:
            target = _resolve_path(path)
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc
        if not target.exists():
            raise HTTPException(status_code=404, detail="Path not found")
        if not target.is_dir():
            raise HTTPException(status_code=400, detail="Path is not a directory")

        items = []
        for entry in sorted(
            target.iterdir(), key=lambda e: (not e.is_dir(), e.name.lower())
        ):
            rel_path = "/" + entry.relative_to(MEDIA_ROOT).as_posix()
            items.append(
                {
                    "type": "folder" if entry.is_dir() else "file",
                    "name": entry.name,
                    "path": rel_path,
                    "thumb": "",
                    "size": entry.stat().st_size,
                }
            )
        return JSONResponse({"path": path, "items": items})

    @api_app.get("/stream")
    def stream_file(request: Request, path: str):
        try:
            target = _resolve_path(path)
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc
        if not target.exists() or not target.is_file():
            raise HTTPException(status_code=404, detail="File not found")

        file_size = target.stat().st_size
        range_header = request.headers.get("range")
        mime_type, _ = mimetypes.guess_type(target.name)
        media_type = mime_type or "application/octet-stream"

        if range_header:
            match = re.match(r"bytes=(\d*)-(\d*)", range_header)
            if not match:
                raise HTTPException(status_code=416, detail="Invalid Range header")
            start_str, end_str = match.groups()
            start = int(start_str) if start_str else 0
            end = int(end_str) if end_str else file_size - 1
            if start >= file_size:
                raise HTTPException(status_code=416, detail="Range out of bounds")
            end = min(end, file_size - 1)
            length = end - start + 1
            headers = {
                "Content-Range": f"bytes {start}-{end}/{file_size}",
                "Accept-Ranges": "bytes",
                "Content-Length": str(length),
            }
            return StreamingResponse(
                _iter_file(target, start, length),
                status_code=206,
                media_type=media_type,
                headers=headers,
            )

        headers = {
            "Accept-Ranges": "bytes",
            "Content-Length": str(file_size),
        }
        return StreamingResponse(
            _iter_file(target, 0, file_size),
            media_type=media_type,
            headers=headers,
        )

    @api_app.get("/download-url")
    def download_url(request: Request, path: str):
        try:
            target = _resolve_path(path)
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc
        if not target.exists() or not target.is_file():
            raise HTTPException(status_code=404, detail="File not found")
        base = str(request.base_url).rstrip("/")
        return {"url": f"{base}/stream?path={quote(path)}"}

    @api_app.get("/terminal")
    def serve_terminal():
        for p in [
            os.path.join(os.path.dirname(os.path.abspath(__file__)), "terminal.html"),
            "/app/terminal.html",
        ]:
            if os.path.exists(p):
                with open(p, "r") as f:
                    return HTMLResponse(content=f.read())
        raise HTTPException(status_code=404, detail="terminal.html not found")

    @api_app.websocket("/ws/terminal")
    async def terminal_ws(websocket: WebSocket):
        await websocket.accept()

        pid, fd = pty.fork()
        if pid == 0:
            # Child process
            os.chdir(MOUNT_PATH if os.path.exists(MOUNT_PATH) else "/")
            env = os.environ.copy()
            env["TERM"] = "xterm-256color"
            env["COLORTERM"] = "truecolor"
            env["LANG"] = os.environ.get("LANG", "en_US.UTF-8")
            os.execvpe("/bin/bash", ["/bin/bash"], env)

        # Parent process
        loop = asyncio.get_event_loop()

        async def read_pty():
            try:
                while True:
                    await asyncio.sleep(0.01)
                    if select.select([fd], [], [], 0)[0]:
                        try:
                            data = os.read(fd, 4096)
                            if data:
                                await websocket.send_json(
                                    {
                                        "type": "output",
                                        "data": data.decode("utf-8", errors="replace"),
                                    }
                                )
                            else:
                                break
                        except OSError:
                            break
            except Exception:
                pass

        read_task = asyncio.create_task(read_pty())

        try:
            while True:
                raw = await websocket.receive_text()
                try:
                    msg = json.loads(raw)
                except json.JSONDecodeError:
                    continue

                msg_type = msg.get("type")
                if msg_type == "input":
                    data = msg.get("data", "")
                    if data:
                        os.write(fd, data.encode("utf-8"))
                elif msg_type == "resize":
                    cols = max(1, min(500, int(msg.get("cols", 80))))
                    rows = max(1, min(200, int(msg.get("rows", 24))))
                    winsize = struct.pack("HHHH", rows, cols, 0, 0)
                    fcntl.ioctl(fd, termios.TIOCSWINSZ, winsize)
                elif msg_type == "ping":
                    await websocket.send_json({"type": "pong"})
        except WebSocketDisconnect:
            pass
        except Exception:
            pass
        finally:
            read_task.cancel()
            try:
                os.kill(pid, signal.SIGTERM)
                os.waitpid(pid, 0)
            except Exception:
                pass
            try:
                os.close(fd)
            except Exception:
                pass

    return api_app


@app.function(
    timeout=86400,
    volumes={MOUNT_PATH: media_volume},
    env={"HOME": f"{MOUNT_PATH}/.home"},
)
def run():
    import threading
    import urllib.request

    def wait_for_health(url: str, timeout: int = 30, interval: float = 0.5) -> bool:
        deadline = time.time() + timeout
        while time.time() < deadline:
            try:
                with urllib.request.urlopen(url, timeout=3) as resp:
                    if resp.status == 200:
                        return True
            except Exception:
                time.sleep(interval)
        return False

    def terminate_process(proc: subprocess.Popen | None):
        if not proc or proc.poll() is not None:
            return
        try:
            proc.terminate()
            proc.wait(timeout=5)
        except Exception:
            proc.kill()

    cf_url_store.pop("url", None)
    start_ts = time.time()
    cf_url_store["status"] = "starting"
    cf_url_store["heartbeat"] = start_ts
    if not isinstance(cf_url_store.get("launching"), (int, float)):
        cf_url_store["launching"] = start_ts

    os.makedirs(f"{MOUNT_PATH}/.home", exist_ok=True)
    os.makedirs(f"{MOUNT_PATH}/media", exist_ok=True)

    import uvicorn

    api_app = create_api_app()
    config = uvicorn.Config(api_app, host="0.0.0.0", port=SERVER_PORT, log_level="info")
    server = uvicorn.Server(config)

    def run_server():
        server.run()

    server_thread = threading.Thread(target=run_server, daemon=True)
    server_thread.start()

    if not wait_for_health(HEALTH_URL, timeout=45):
        cf_url_store["status"] = "failed"
        cf_url_store.pop("launching", None)
        server.should_exit = True
        server_thread.join(timeout=5)
        raise RuntimeError("API server did not become healthy in time.")

    cf_proc = subprocess.Popen(
        ["cloudflared", "tunnel", "--url", f"http://localhost:{SERVER_PORT}"],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )

    tunnel_ready = threading.Event()

    def capture_cf_url(proc: subprocess.Popen):
        url_found = False
        for line in iter(proc.stdout.readline, ""):
            match = re.search(r"https://[a-zA-Z0-9-]+\.trycloudflare\.com", line)
            if match and not url_found:
                url_found = True
                cf_url_store["url"] = match.group(0)
                tunnel_ready.set()
        if not url_found:
            tunnel_ready.set()

    capture_thread = threading.Thread(
        target=capture_cf_url, args=(cf_proc,), daemon=True
    )
    capture_thread.start()

    tunnel_ready.wait(timeout=20)
    cf_url_store["status"] = "running"
    cf_url_store["heartbeat"] = time.time()
    cf_url_store.pop("launching", None)

    try:
        while True:
            if cf_proc.poll() is not None:
                break
            cf_url_store["heartbeat"] = time.time()
            time.sleep(1)
    finally:
        server.should_exit = True
        server_thread.join(timeout=5)
        terminate_process(cf_proc)
        cf_url_store.pop("url", None)
        cf_url_store.pop("heartbeat", None)
        cf_url_store.pop("launching", None)
        cf_url_store["status"] = "stopped"


@app.function()
@modal.fastapi_endpoint(method="GET")
def launch():
    from fastapi.responses import JSONResponse

    state, err = _read_runtime_state()
    if err:
        return JSONResponse(
            {"error": str(err)}, status_code=500, headers=_no_cache_headers()
        )

    now_ts = time.time()
    launch_ts = state.get("launching")
    launch_lock_active = (
        isinstance(launch_ts, (int, float))
        and (now_ts - launch_ts) < START_LOCK_TTL_SECONDS
    )
    should_spawn = not launch_lock_active and not _is_run_alive(state, now_ts)

    if should_spawn:
        cf_url_store["launching"] = now_ts
        cf_url_store["status"] = "starting"
        cf_url_store.pop("url", None)
        try:
            run.spawn()
        except Exception as e:
            cf_url_store["status"] = "failed"
            cf_url_store.pop("launching", None)
            return JSONResponse(
                {"error": f"Failed to launch: {e}"},
                status_code=500,
                headers=_no_cache_headers(),
            )

    return JSONResponse(
        {"status": cf_url_store.get("status", "starting")},
        status_code=202 if should_spawn else 200,
        headers=_no_cache_headers(),
    )


@app.function()
@modal.fastapi_endpoint(method="GET")
def tunnel():
    from fastapi.responses import JSONResponse

    state, err = _read_runtime_state()
    if err:
        return JSONResponse(
            {"error": str(err)}, status_code=500, headers=_no_cache_headers()
        )

    now_ts = time.time()
    if _has_live_url(state, now_ts) and state.get("status") == "running":
        return JSONResponse(
            {"url": state["url"], "status": "running"}, headers=_no_cache_headers()
        )

    if state.get("url") and not _has_live_url(state, now_ts):
        cf_url_store.pop("url", None)

    return JSONResponse(
        {"status": state.get("status", "starting")},
        status_code=404,
        headers=_no_cache_headers(),
    )


@app.function()
@modal.fastapi_endpoint(method="GET")
def terminal_redirect():
    from fastapi.responses import JSONResponse, RedirectResponse

    state, err = _read_runtime_state()
    if err:
        return JSONResponse(
            {"error": str(err)}, status_code=500, headers=_no_cache_headers()
        )

    now_ts = time.time()
    if _has_live_url(state, now_ts) and state.get("status") == "running":
        return RedirectResponse(url=f"{state['url']}/terminal")

    return JSONResponse(
        {"error": "Server not running", "status": state.get("status", "unknown")},
        status_code=503,
        headers=_no_cache_headers(),
    )


@app.local_entrypoint()
def main():
    run.remote()
