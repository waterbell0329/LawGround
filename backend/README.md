# Spring 백엔드

Java 21, Spring Boot 3.5.16, Gradle Wrapper 8.14.3을 사용한다. IDE에서는 이 디렉터리를 Gradle 프로젝트로 연다.

저장소 루트의 [실행 안내](../README.md)와 [구조 설명](../docs/architecture/backend.md)을 먼저 확인한다.

```powershell
# 현재 디렉터리: backend
.\gradlew.bat test spotlessCheck bootJar --no-daemon
.\gradlew.bat integrationTest --no-daemon  # Docker 엔진 필요
.\gradlew.bat clean check bootJar --no-daemon  # CI와 동일한 전체 검사
.\gradlew.bat bootRun
```

macOS/Linux에서는 `.\gradlew.bat` 대신 `./gradlew`를 사용한다. Java 21이 없는 환경에서 다른 버전으로 몰래 빌드하지 않는다.

`src/main/java/com/lawground`에는 도메인별 디렉터리를 준비했다. 빈 `.gitkeep`은 해당 디렉터리에 첫 실제 파일을 넣을 때 제거한다. 기능이 구현된 것처럼 보이는 빈 Controller/Service 클래스는 만들지 않는다.
