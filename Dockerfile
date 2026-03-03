# JDK 21 경량화 이미지 사용
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# 빌드된 jar 파일을 컨테이너 내부로 복사
COPY build/libs/*-SNAPSHOT.jar app.jar

# 컨테이너 실행 시 자바 서버 구동
ENTRYPOINT ["java", "-jar", "app.jar"]