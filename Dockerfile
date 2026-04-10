# --- [Stage 1: 빌드 환경] 무거운 JDK를 써서 소스코드를 jar로 압축합니다. ---
    FROM eclipse-temurin:21-jdk-alpine AS builder

    WORKDIR /app
    COPY . .
    
    # 권한 부여 및 빌드 (테스트 제외)
    RUN chmod +x ./gradlew
    RUN ./gradlew build -x test
    
    
    # --- [Stage 2: 실행 환경] 기획자님의 최적화 코드! 가벼운 JRE만 사용합니다. ---
    FROM eclipse-temurin:21-jre-alpine
    
    WORKDIR /app
    
    # 인프라 가시성을 위한 포트 명시
    EXPOSE 8080
    
    # Stage 1(builder)에서 완성된 jar 파일만 쏙 빼서 가져옵니다.
    COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar
    
    # 서버 구동
    ENTRYPOINT ["java", "-jar", "app.jar"]