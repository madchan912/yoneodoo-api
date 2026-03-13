# 1. [운영 최적화] 무거운 JDK(개발용) 대신 가벼운 JRE(실행용) alpine 버전을 사용해 이미지 용량을 최소화합니다.
FROM eclipse-temurin:21-jre-alpine

# 2. 컨테이너 내부의 작업 디렉토리 설정
WORKDIR /app

# 3. [인프라 가시성] 이 컨테이너가 8080 포트를 쓴다는 것을 명시적으로 선언합니다.
# (실제 포트를 여는 기능은 아니지만, 인프라 담당자나 다른 개발자가 구조를 파악하기 좋습니다)
EXPOSE 8080

# 4. 빌드된 jar 파일을 컨테이너 내부로 복사
COPY build/libs/*-SNAPSHOT.jar app.jar

# 5. 컨테이너 실행 시 자바 서버 구동
ENTRYPOINT ["java", "-jar", "app.jar"]