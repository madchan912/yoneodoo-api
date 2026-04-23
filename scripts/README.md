# 스크립트 — DB 동기화 (운영 → 로컬)

## `sync_prod_to_local_db.py` / `sync_prod_to_local_db.sh`

Neon 등 **운영 PostgreSQL**에서 `pg_dump`로 받은 커스텀 포맷 덤프를, **로컬 Docker PostgreSQL**에 `pg_restore --clean`으로 덮어씁니다.

### 요구 사항

- `pg_dump`, `pg_restore`가 PATH에 있어야 합니다 (PostgreSQL 클라이언트).
- Python 3만 사용합니다 (추가 pip 패키지 없음).

### 설정

1. `.env.sync.example`을 복사해 `scripts/.env.sync`를 만듭니다.
2. `SYNC_SOURCE_*` = 운영 DB, `SYNC_TARGET_*` = 로컬 DB를 채웁니다.
3. TARGET 호스트는 `localhost`, `127.0.0.1`, `::1`, `host.docker.internal` 또는 `127.*` 만 허용됩니다.

### 실행

```bash
cd yoneodoo-api
chmod +x scripts/sync_prod_to_local_db.sh   # 최초 1회 (Unix)
./scripts/sync_prod_to_local_db.sh
# 또는
python3 scripts/sync_prod_to_local_db.py --env-file scripts/.env.sync
```

Windows에서는 Git Bash / WSL에서 위와 같이 실행하거나, `python3 scripts\sync_prod_to_local_db.py` 로 직접 실행합니다.

### 보안

- 확인 질문 2회 + `SYNC-PROD-TO-LOCAL` 문구 입력 없이는 진행되지 않습니다.
- SOURCE가 로컬로 보이면 즉시 종료합니다 (역방향 실수 방지).
