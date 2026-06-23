#!/usr/bin/env python3
"""
운영(RDS 등) PostgreSQL에서 pg_dump로 백업한 뒤, 로컬 Docker PostgreSQL에 pg_restore로 덮어쓴다.

동작 방식
----------
  [1/2] dump   : docker run --rm <SYNC_PG_IMAGE> pg_dump   SOURCE → stdout → 호스트 임시 파일
  [2/2] restore: docker run --rm --network container:<local_pg> -v <dump>:/tmp/sync.dump <SYNC_PG_IMAGE> pg_restore
                 restore 컨테이너가 로컬 pg 컨테이너의 네트워크를 공유하므로 localhost:5432 로 접속

필수 환경 : Docker (pg_dump / pg_restore 로컬 설치 불필요, OS 무관)

안전장치
---------
- SOURCE 호스트가 로컬로 보이면 중단한다 (역방향 실수 방지).
- TARGET 호스트가 설정되어 있으면 로컬 주소인지 검증한다.
- 로컬 컨테이너를 명시하지 않으면 실행 중인 postgres 이미지를 자동 탐지한다.
- 실행 전 2단계 yes/no 확인 프롬프트 필수.

.env.sync 필수 키
------------------
  SYNC_SOURCE_HOST, SYNC_SOURCE_PORT, SYNC_SOURCE_USER, SYNC_SOURCE_PASSWORD, SYNC_SOURCE_DB
  SYNC_TARGET_USER, SYNC_TARGET_PASSWORD, SYNC_TARGET_DB

.env.sync 선택 키
------------------
  SYNC_SOURCE_SSLMODE      (기본: require)
  SYNC_TARGET_HOST         (기본값 없음 — 있으면 로컬 주소인지 검증)
  SYNC_DOCKER_CONTAINER    (기본: 자동 탐지)
  SYNC_PG_IMAGE            (기본: postgres:16)
"""

from __future__ import annotations

import argparse
import os
import subprocess
import sys
import tempfile
from pathlib import Path


ALLOWED_LOCAL_HOSTS = frozenset({"localhost", "127.0.0.1", "::1", "host.docker.internal"})

REQUIRED_SOURCE_KEYS = (
    "SYNC_SOURCE_HOST",
    "SYNC_SOURCE_PORT",
    "SYNC_SOURCE_USER",
    "SYNC_SOURCE_PASSWORD",
    "SYNC_SOURCE_DB",
)

REQUIRED_TARGET_KEYS = (
    "SYNC_TARGET_USER",
    "SYNC_TARGET_PASSWORD",
    "SYNC_TARGET_DB",
)

DEFAULT_PG_IMAGE = "postgres:16"


# ──────────────────────────────────────────────
# 헬퍼
# ──────────────────────────────────────────────

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


def check_docker() -> None:
    """Docker 데몬이 동작 중인지 확인한다."""
    try:
        r = subprocess.run(
            ["docker", "info"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            timeout=10,
        )
        if r.returncode != 0:
            raise RuntimeError
    except Exception:
        print(
            "ERROR: Docker가 실행 중이지 않거나 'docker' 명령을 찾을 수 없습니다.\n"
            "       Docker Desktop을 실행한 뒤 다시 시도하세요.",
            file=sys.stderr,
        )
        sys.exit(1)


def detect_pg_container() -> str | None:
    """
    실행 중인 컨테이너 중 이미지 이름에 'postgres'가 포함된 첫 번째 컨테이너 이름을 반환한다.
    """
    try:
        r = subprocess.run(
            ["docker", "ps", "--format", "{{.Names}}\t{{.Image}}"],
            capture_output=True,
            text=True,
            timeout=10,
        )
        if r.returncode != 0:
            return None
        for line in r.stdout.strip().splitlines():
            name, _, image = line.partition("\t")
            if "postgres" in image.lower():
                return name.strip()
    except Exception:
        pass
    return None


def run_dump(pg_image: str, env_file: str, pg_args: list[str], out_path: Path) -> None:
    """
    docker run --rm --env-file <env_file> <pg_image> pg_dump <pg_args>
    stdout → out_path 파일로 저장한다.
    패스워드를 커맨드라인 인수가 아닌 --env-file 로 전달해 프로세스 목록 노출을 방지한다.
    """
    cmd = ["docker", "run", "--rm", "--env-file", env_file, pg_image, "pg_dump"] + pg_args
    print("+", " ".join(cmd[:7]), "...")
    with open(out_path, "wb") as f:
        r = subprocess.run(cmd, stdout=f)
    if r.returncode != 0:
        print(f"ERROR: pg_dump 실패 (exit {r.returncode})", file=sys.stderr)
        sys.exit(r.returncode)


def run_restore(pg_image: str, container: str, env_file: str, pg_args: list[str], dump_path: Path) -> None:
    """
    SYNC_PG_IMAGE 이미지로 새 컨테이너를 띄워 pg_restore를 실행한다.

    --network container:<local_pg> 옵션으로 로컬 pg 컨테이너의 네트워크 네임스페이스를 공유한다.
    이 덕분에 restore 컨테이너 내부에서 localhost:5432 가 곧 로컬 PostgreSQL 서버다.
    dump 파일은 -v 볼륨 마운트로 컨테이너 안에 /tmp/sync.dump 로 전달한다.

    pg_restore는 경고 시 exit 1을 반환할 수 있으므로 치명적 오류(exit >= 2)만 실패로 처리한다.
    """
    # Windows 경로를 Docker가 인식하는 형식(슬래시)으로 변환
    host_mount = dump_path.as_posix()
    cmd = [
        "docker", "run", "--rm",
        "--network", f"container:{container}",
        "--env-file", env_file,
        "-v", f"{host_mount}:/tmp/sync.dump:ro",
        pg_image, "pg_restore",
    ] + pg_args + ["/tmp/sync.dump"]
    print("+", " ".join(cmd[:10]), "...")
    r = subprocess.run(cmd)
    if r.returncode == 1:
        print("  ⚠  pg_restore exit 1 — 일부 경고가 발생했습니다. 위 출력(-v)을 확인하세요.")
    elif r.returncode >= 2:
        print(f"ERROR: pg_restore 실패 (exit {r.returncode})", file=sys.stderr)
        sys.exit(r.returncode)


# ──────────────────────────────────────────────
# 메인
# ──────────────────────────────────────────────

def main() -> None:
    script_dir = Path(__file__).resolve().parent
    default_env = script_dir / ".env.sync"

    parser = argparse.ArgumentParser(
        description="Dump production PostgreSQL → restore into local Docker container (docker run/exec)."
    )
    parser.add_argument(
        "--env-file",
        type=Path,
        default=default_env,
        help=f"Path to env file (default: {default_env})",
    )
    args = parser.parse_args()
    load_env_file(args.env_file.resolve())

    # ── Docker 확인 ──
    check_docker()

    # ── 필수 환경변수 로드 ──
    for k in REQUIRED_SOURCE_KEYS + REQUIRED_TARGET_KEYS:
        getenv_required(k)

    src_host = getenv_required("SYNC_SOURCE_HOST").lower()
    src_port = getenv_required("SYNC_SOURCE_PORT")
    src_user = getenv_required("SYNC_SOURCE_USER")
    src_pass = getenv_required("SYNC_SOURCE_PASSWORD")
    src_db   = getenv_required("SYNC_SOURCE_DB")

    tgt_user = getenv_required("SYNC_TARGET_USER")
    tgt_pass = getenv_required("SYNC_TARGET_PASSWORD")
    tgt_db   = getenv_required("SYNC_TARGET_DB")

    src_ssl  = os.environ.get("SYNC_SOURCE_SSLMODE", "require").strip() or "require"
    pg_image = os.environ.get("SYNC_PG_IMAGE", DEFAULT_PG_IMAGE).strip() or DEFAULT_PG_IMAGE

    # ── 안전 검증 ──
    if src_host in ALLOWED_LOCAL_HOSTS or src_host.startswith("127."):
        print(
            "ERROR: SYNC_SOURCE_HOST 가 로컬 주소입니다. 운영 DB 호스트를 지정하세요 (역방향 방지).",
            file=sys.stderr,
        )
        sys.exit(1)

    tgt_host_raw = os.environ.get("SYNC_TARGET_HOST", "").strip().lower()
    if tgt_host_raw and tgt_host_raw not in ALLOWED_LOCAL_HOSTS and not tgt_host_raw.startswith("127."):
        print(
            f"ERROR: SYNC_TARGET_HOST={tgt_host_raw!r} 가 로컬 주소가 아닙니다. "
            "운영 DB를 restore 대상으로 지정할 수 없습니다.",
            file=sys.stderr,
        )
        sys.exit(1)

    # ── 로컬 pg 컨테이너 결정 ──
    container = os.environ.get("SYNC_DOCKER_CONTAINER", "").strip()
    if not container:
        print("SYNC_DOCKER_CONTAINER 미설정 — 실행 중인 postgres 컨테이너 자동 탐지 중...")
        container = detect_pg_container() or ""
    if not container:
        print(
            "ERROR: 로컬 PostgreSQL 컨테이너를 찾지 못했습니다.\n"
            "       .env.sync 에 SYNC_DOCKER_CONTAINER=<컨테이너명> 을 추가하거나\n"
            "       'docker ps' 로 postgres 컨테이너가 실행 중인지 확인하세요.",
            file=sys.stderr,
        )
        sys.exit(1)

    # ── 배너 출력 ──
    print()
    print("=" * 72)
    print("  경고: 운영(SOURCE) DB에서 읽어와 로컬(TARGET) 컨테이너 DB를 통째로 덮어씁니다.")
    print("  로컬 DB의 기존 데이터는 pg_restore --clean 으로 삭제·재적재됩니다.")
    print("  이 스크립트는 운영 DB에 쓰기(write)하지 않습니다. (dump 만)")
    print("=" * 72)
    print()
    print(f"  SOURCE     {src_host}:{src_port}  db={src_db}  user={src_user}  pw={mask(src_pass)}")
    print(f"  TARGET     컨테이너={container}  db={tgt_db}  user={tgt_user}  pw={mask(tgt_pass)}")
    print(f"  pg image   {pg_image}  (dump·restore 모두 이 이미지로 실행, 로컬 설치 불필요)")
    print()

    # ── 2단계 확인 ──
    a = input("SOURCE가 운영(RDS 등)이 맞습니까? [yes/NO]: ").strip().lower()
    if a != "yes":
        print("중단했습니다.")
        sys.exit(0)

    b = input("TARGET 컨테이너가 로컬 Docker PostgreSQL(덮어써도 되는 곳)이 맞습니까? [yes/NO]: ").strip().lower()
    if b != "yes":
        print("중단했습니다.")
        sys.exit(0)

    # ── 임시 파일 준비 ──
    with tempfile.NamedTemporaryFile(prefix="yoneodoo_prod_", suffix=".dump", delete=False) as tmp:
        dump_path = Path(tmp.name)

    # dump·restore 용 Docker --env-file (패스워드를 docker 인수에 노출하지 않기 위해)
    with tempfile.NamedTemporaryFile(
        mode="w", suffix=".env", delete=False, encoding="utf-8"
    ) as ef:
        ef.write(f"PGPASSWORD={src_pass}\n")
        ef.write(f"PGSSLMODE={src_ssl}\n")
        dump_env_path = ef.name

    with tempfile.NamedTemporaryFile(
        mode="w", suffix=".env", delete=False, encoding="utf-8"
    ) as ef:
        ef.write(f"PGPASSWORD={tgt_pass}\n")
        restore_env_path = ef.name

    try:
        # ── [1/2] dump ──
        print(f"\n[1/2] pg_dump via Docker (SOURCE → 임시 파일) ...")
        run_dump(
            pg_image=pg_image,
            env_file=dump_env_path,
            pg_args=[
                "-h", src_host,
                "-p", src_port,
                "-U", src_user,
                "-d", src_db,
                "-Fc",
                "--no-owner",
            ],
            out_path=dump_path,
        )
        dump_mb = dump_path.stat().st_size / 1024 / 1024
        print(f"   dump 완료: {dump_mb:.1f} MB → {dump_path}")

        # ── [2/2] restore ──
        print(f"\n[2/2] pg_restore via docker run (image={pg_image}, network=container:{container}) ...")
        # restore 컨테이너가 로컬 pg 컨테이너의 네트워크를 공유하므로 localhost:5432 로 접속한다.
        run_restore(
            pg_image=pg_image,
            container=container,
            env_file=restore_env_path,
            pg_args=[
                "-h", "localhost",
                "-U", tgt_user,
                "-d", tgt_db,
                "--clean",
                "--if-exists",
                "--no-owner",
                "--no-acl",
                "-v",
            ],
            dump_path=dump_path,
        )

        print("\n✓ 완료: 로컬 DB가 SOURCE 스냅샷으로 갱신되었습니다.")

    finally:
        if dump_path.is_file():
            dump_path.unlink(missing_ok=True)
        if Path(dump_env_path).is_file():
            Path(dump_env_path).unlink(missing_ok=True)
        if Path(restore_env_path).is_file():
            Path(restore_env_path).unlink(missing_ok=True)


if __name__ == "__main__":
    main()
