# 로컬 실행과 문제 해결

## 실행

1. Java/Javac 21과 Docker Desktop Linux 엔진을 준비한다.
2. 루트 `.env.example`을 `.env`로 복사하고 두 비밀번호를 채운다. `.env`는 커밋하지 않는다.
3. 루트에서 `docker compose config --quiet`, `docker compose up -d --wait`를 실행한다.
4. `cd backend` 후 Windows는 `.\gradlew.bat bootRun`, Linux/macOS는 `./gradlew bootRun`을 실행한다.
5. `http://127.0.0.1:8080/actuator/health/readiness`가 UP인지 확인한다. Swagger는 `/swagger-ui/index.html`에 있다.

기능 API는 아직 없으며 명시적 권한 규칙도 미구현이다. 상태 검사와 문서 확인까지만 실행한다. DB에는 vector 확장과 Flyway 이력만 생성된다.

## 확인 명령

```powershell
# 저장소 루트
python scripts/check_repository.py
docker compose config --quiet
docker compose ps

# backend
.\gradlew.bat test spotlessCheck bootJar --no-daemon
.\gradlew.bat integrationTest --no-daemon
.\gradlew.bat clean check bootJar --no-daemon
```

통합 테스트는 임시 컨테이너·임시 포트를 사용해 개발 DB와 분리한다. Docker가 없으면 integrationTest는 실패하며 검사 생략을 성공으로 표시하지 않는다. 결과는 `backend/build/reports/tests/test/`와 `backend/build/reports/tests/integrationTest/`에 있다.

## 오류 대응

- Docker named pipe/daemon 오류: Docker Desktop에서 Linux 엔진 실행 상태를 확인한다. `docker version`에 Server 정보가 있어야 한다.
- 5432·5672·15672 포트 충돌: `.env`의 포트만 변경하고 다시 실행한다. PostgreSQL/RabbitMQ 컨테이너 내부 포트는 바꾸지 않는다.
- 환경변수 누락: 루트 `.env`와 비밀번호 값을 확인한다. 값을 로그나 이슈에 붙이지 않는다.
- 기존 볼륨 비밀번호 불일치: 컨테이너는 초기 생성 후 `.env` 변경만으로 DB 계정 비밀번호를 바꾸지 않는다. 기존 데이터 보존 여부를 먼저 확인하고 비밀번호 변경 또는 새 볼륨을 선택한다.
- Flyway 체크섬 불일치: 적용된 V001을 수정하지 않는다. 새 버전 마이그레이션으로 변경한다. 무작정 repair/clean하지 않는다.
- Java 버전 불일치: IDE Gradle JVM과 `JAVA_HOME`을 JDK 21로 맞춘다.
- 한글 경로의 테스트 ClassNotFoundException: `gradle.properties`의 `file.encoding=COMPAT`을 유지한다. JVM 실행 전 인자 파일은 Windows의 기본 문자셋으로 읽힌다. 소스 컴파일과 테스트 실행은 별도로 UTF-8을 고정했다.
- 포맷 검사 실패: `.\gradlew.bat spotlessApply` 후 실제 변경을 확인하고 다시 검사한다.
- 401/403: 기반 설정에서 기능 경로를 열지 않은 정상 동작이다. Y04/Y11에서 정확한 권한·사용자 흐름과 테스트를 추가한다.

종료는 루트에서 `docker compose stop`. 데이터 삭제가 필요한 상황을 별도로 판단하기 전에는 `docker compose down -v`를 사용하지 않는다.
