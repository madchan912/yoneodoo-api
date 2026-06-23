-- recipes 테이블에 updated_at 컬럼 추가
-- 대상 환경: 운영 RDS (로컬은 Spring이 DDL 자동 처리)
-- 실행 전 반드시 백업(pg_dump) 후 진행할 것

ALTER TABLE recipes
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- 기존 행은 created_at 값으로 초기화 (최선 근사치)
UPDATE recipes
SET updated_at = created_at
WHERE updated_at IS NULL;
