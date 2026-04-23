#!/usr/bin/env python3
"""
운영(Neon 등) PostgreSQL에서 pg_dump로 백업한 뒤, 로컬 Docker PostgreSQL에 pg_restore로 덮어쓴다.

- 접속 정보는 환경 변수 또는 --env-file 로만 주입한다. (하드코딩 없음)
- TARGET 호스트는 localhost 계열만 허용해, 실수로 운영 DB를 restore 대상으로 쓰는 것을 방지한다.
- SOURCE 호스트가 로컬로 보이면 중단한다 (역방향 실수 방지).

필수: PATH에 pg_dump, pg_restore (PostgreSQL client)가 있어야 한다.
"""

from __future__ import annotations

import argparse
import os
import subprocess
import sys
import tempfile
from pathlib import Path


ALLOWED_LOCAL_HOSTS = frozenset(
    {
        "localhost",
        "127.0.0.1",
        "::1",
        "host.docker.internal",
    }
)

REQUIRED_SOURCE_KEYS = (
    "SYNC_SOURCE_HOST",
    "SYNC_SOURCE_PORT",
    "SYNC_SOURCE_USER",
    "SYNC_SOURCE_PASSWORD",
    "SYNC_SOURCE_DB",
)

REQUIRED_TARGET_KEYS = (
    "SYNC_TARGET_HOST",
    "SYNC_TARGET_PORT",
    "SYNC_TARGET_USER",
    "SYNC_TARGET_PASSWORD",
    "SYNC_TARGET_DB",
)


def load_env_file(path: Path) -> None:
    if not path.is_file():
        print(f"ERROR: env file not found: {path}", file=sys.stderr)
        sys.exit(1)
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        key, _, val = line.partition("=")
        key = key.strip()
        val = val.strip().strip('"').strip("'")
        if key and key not in os.environ:
            os.environ[key] = val


def getenv_required(key: str) -> str:
    v = os.environ.get(key, "").strip()
    if not v:
        print(f"ERROR: missing or empty environment variable: {key}", file=sys.stderr)
        sys.exit(1)
    return v


def mask(s: str, keep: int = 2) -> str:
    if len(s) <= keep * 2:
        return "***"
    return s[:keep] + "…" + s[-keep:]


def run(cmd: list[str], extra_env: dict[str, str]) -> None:
    env = {**os.environ, **extra_env}
    print("+", " ".join(cmd[:6]), "..." if len(cmd) > 6 else "")
    r = subprocess.run(cmd, env=env)
    if r.returncode != 0:
        sys.exit(r.returncode)


def main() -> None:
    script_dir = Path(__file__).resolve().parent
    default_env = script_dir / ".env.sync"

    parser = argparse.ArgumentParser(
        description="Dump production PostgreSQL and restore into local Docker DB (overwrite local only)."
    )
    parser.add_argument(
        "--env-file",
        type=Path,
        default=default_env,
        help=f"Path to env file (default: {default_env})",
    )
    args = parser.parse_args()

    load_env_file(args.env_file.resolve())

    for k in REQUIRED_SOURCE_KEYS + REQUIRED_TARGET_KEYS:
        getenv_required(k)

    src_host = getenv_required("SYNC_SOURCE_HOST").lower()
    src_port = getenv_required("SYNC_SOURCE_PORT")
    src_user = getenv_required("SYNC_SOURCE_USER")
    src_pass = getenv_required("SYNC_SOURCE_PASSWORD")
    src_db = getenv_required("SYNC_SOURCE_DB")

    tgt_host = getenv_required("SYNC_TARGET_HOST").lower()
    tgt_port = getenv_required("SYNC_TARGET_PORT")
    tgt_user = getenv_required("SYNC_TARGET_USER")
    tgt_pass = getenv_required("SYNC_TARGET_PASSWORD")
    tgt_db = getenv_required("SYNC_TARGET_DB")

    src_ssl = os.environ.get("SYNC_SOURCE_SSLMODE", "require").strip() or "require"

    if src_host in ALLOWED_LOCAL_HOSTS or src_host.startswith("127."):
        print(
            "ERROR: SYNC_SOURCE_HOST looks like a LOCAL machine. "
            "This script only dumps from PRODUCTION. Fix your .env.sync (역방향 방지).",
            file=sys.stderr,
        )
        sys.exit(1)

    if tgt_host not in ALLOWED_LOCAL_HOSTS and not tgt_host.startswith("127."):
        print(
            f"ERROR: SYNC_TARGET_HOST must be one of {sorted(ALLOWED_LOCAL_HOSTS)} "
            f"or 127.x.x.x (got {tgt_host!r}). 운영 DB를 restore 대상으로 지정할 수 없습니다.",
            file=sys.stderr,
        )
        sys.exit(1)

    if (src_host, src_port, src_db) == (tgt_host, tgt_port, tgt_db):
        print("ERROR: SOURCE and TARGET are identical. Abort.", file=sys.stderr)
        sys.exit(1)

    print()
    print("=" * 72)
    print("  경고: 운영(또는 SOURCE) DB에서 읽어와 로컬(TARGET) DB를 통째로 덮어씁니다.")
    print("  로컬 DB의 기존 데이터는 pg_restore --clean 으로 삭제·재적재됩니다.")
    print("  이 스크립트는 운영 DB에 쓰기(write)하지 않습니다. (dump만)")
    print("=" * 72)
    print()
    print(f"  SOURCE  {src_host}:{src_port}  db={src_db}  user={src_user}  password={mask(src_pass)}")
    print(f"  TARGET  {tgt_host}:{tgt_port}  db={tgt_db}  user={tgt_user}  password={mask(tgt_pass)}")
    print()

    a = input("SOURCE가 운영(Neon 등)이 맞습니까? [yes/NO]: ").strip().lower()
    if a != "yes":
        print("중단했습니다.")
        sys.exit(0)

    b = input("TARGET이 로컬 Docker PostgreSQL(덮어써도 되는 곳)이 맞습니까? [yes/NO]: ").strip().lower()
    if b != "yes":
        print("중단했습니다.")
        sys.exit(0)

    token = input("계속하려면 정확히 다음을 입력하세요 → SYNC-PROD-TO-LOCAL : ").strip()
    if token != "SYNC-PROD-TO-LOCAL":
        print("확인 문구가 일치하지 않아 중단했습니다.")
        sys.exit(0)

    with tempfile.NamedTemporaryFile(prefix="yoneodoo_prod_", suffix=".dump", delete=False) as tmp:
        dump_path = Path(tmp.name)

    try:
        print("\n[1/2] pg_dump (SOURCE → custom format file) ...")
        dump_cmd = [
            "pg_dump",
            "-h",
            src_host,
            "-p",
            src_port,
            "-U",
            src_user,
            "-d",
            src_db,
            "-Fc",
            "--no-owner",
            "-f",
            str(dump_path),
        ]
        run(
            dump_cmd,
            {
                "PGPASSWORD": src_pass,
                "PGSSLMODE": src_ssl,
            },
        )

        print("\n[2/2] pg_restore (dump file → TARGET, --clean) ...")
        restore_cmd = [
            "pg_restore",
            "-h",
            tgt_host,
            "-p",
            tgt_port,
            "-U",
            tgt_user,
            "-d",
            tgt_db,
            "--clean",
            "--if-exists",
            "--no-owner",
            "--no-acl",
            "-v",
            str(dump_path),
        ]
        run(
            restore_cmd,
            {"PGPASSWORD": tgt_pass},
        )

        print("\n완료: 로컬 DB가 SOURCE 스냅샷으로 갱신되었습니다.")
    finally:
        if dump_path.is_file():
            dump_path.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
