# ⚙️ YoNeoDoo - Backend (API)

요너두 서비스의 비즈니스 로직 처리, AI 크롤링 데이터 적재, 그리고 프론트엔드에 레시피 데이터를 제공하는 핵심 백엔드 애플리케이션입니다.

## 🛠 Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot
- **Data Access**: Spring Data JPA, Hibernate
- **Database**: PostgreSQL (Neon Cloud DB / Local Docker DB)
- **Deployment**: Render

## 📁 환경 분리 (Local vs Prod)
안전한 데이터 관리와 개발 편의성을 위해 로컬과 운영 환경이 완벽하게 분리되어 있습니다.
- `application-local.yml` (기본값): 내 컴퓨터의 Docker PostgreSQL 연결
- `application-prod.yml`: Render 배포 시 활성화되며, 실제 서비스용 Neon DB 연결

## 🚀 How to Run (Local)
1. 루트 디렉토리의 `docker-compose.yml`을 활용하여 로컬 테스트용 DB를 띄웁니다.
   ```bash
   docker-compose up -d db
   ```
2. IDE(IntelliJ, VS Code 등)에서 Spring Boot 애플리케이션을 실행합니다. 
   - 💡 별도의 설정이 없다면 자동으로 `local` 프로필로 실행됩니다.

## 🔐 Environment Variables (운영 환경)
Render 서버에 배포할 때 다음 환경변수(Environment Variables) 설정이 필수적으로 요구됩니다. (보안상 실제 값은 깃허브에 올리지 않습니다.)
- `SPRING_PROFILES_ACTIVE=prod` (운영 설정 파일 강제 읽기)
- `DB_URL` (Neon DB JDBC 주소)
- `DB_USER` (운영 DB 계정명)
- `DB_PASSWORD` (운영 DB 비밀번호)
