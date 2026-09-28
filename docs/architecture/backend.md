# 백엔드 기반 설계

## 구현 범위

이번 변경은 Y02 개발 기반이다. Spring 기동, 설정, DB 확장 마이그레이션, 공통 오류, 요청 ID, 보안 기본값과 실제 인프라 테스트를 준비한다. 문제 CRUD·검색·LLM 호출·로그인 기능과 도메인 테이블은 아직 구현하지 않았다.

## 디렉터리

```text
backend/
  gradle/wrapper/       # 공식 Wrapper, 배포 ZIP과 JAR 체크섬 검증
  src/main/java/com/lawground/
    member/ auth/       # 회원, 인증
    law/                # 법령·버전·원문·수집·색인
    question/ trace/    # 문제, 분석 실행·근거
    recommendation/    # 유사문제
    evaluation/        # 골든셋·평가
    job/                # 작업·outbox·메시징
    conversation/ bookmark/ usage/  # 후속 확장 자리
    integration/ai/    # Python HTTP 계약·어댑터
    integration/law/   # 법령 API 어댑터
    global/config/     # 보안·OpenAPI·UTC Clock
    global/error/      # 오류 DTO와 예외 처리
    global/web/        # 요청 ID
  src/main/resources/
    application.yml
    application-local.yml
    application-prod.yml
    db/migration/
  src/test/java/        # 동일 도메인 경로 + bootstrap/ 실제 인프라 검사
  src/test/resources/application-test.yml
```

도메인 내부에서 controller → service → repository로 책임을 나눈다. DTO는 엔티티와 분리하고 생성자 주입을 사용한다. 외부 HTTP 대기 중 DB 트랜잭션을 유지하지 않는다. 비즈니스 기능 추가 전 상태·요청/응답·테이블 제약을 확정한다.

## 의존성 선택

| 항목 | 목적 |
|---|---|
| Spring MVC, Validation | HTTP API와 입력 검증 |
| Data JPA, PostgreSQL JDBC | 데이터 저장, SQL/벡터/FTS 쿼리 준비 |
| Security, OAuth2 Client | 보안 기본값과 추후 Google OIDC |
| AMQP | RabbitMQ 작업 큐 |
| Flyway Core + PostgreSQL 모듈 | 버전별 DB 변경 |
| Actuator | health/readiness |
| WebFlux | WebClient 사용 준비; 웹 서버는 MVC로 고정 |
| springdoc 2.8.17 | Boot 3.x용 OpenAPI·Swagger |
| Boot Test, Security Test, Testcontainers 1.21.4 | 단위·웹·실제 PostgreSQL/RabbitMQ 검사 |
| Spotless 8.10.3 + google-java-format 1.28.0 AOSP | 일관된 Java 포맷, 4칸 들여쓰기 |

Spring 의존성 버전은 Boot 3.5.16 BOM이 관리한다. Gradle은 호환 범위인 8.14.3으로 고정한다. 테스트용 H2로 PostgreSQL·pgvector·FTS를 대체하지 않는다. Spring Session JDBC는 Y11 세션 스키마 설계와 함께 도입한다.

## 설정과 보안

- 기본 프로파일은 local이며 루프백 주소에 바인딩한다. 운영은 명시적으로 prod를 선택한다.
- local만 `.env`를 읽는다. test 프로파일 단독 실행은 `.env`를 읽지 않으며 Testcontainers가 연결을 제공한다.
- 비밀번호와 운영 연결 정보에 기본 운영 값을 넣지 않는다. prod에서는 필수 값 누락 시 기동에 실패한다.
- health/readiness만 공통 공개한다. 문서 열람은 local 설정에서만 허용한다. 그 외 요청은 명시적인 권한 규칙이 생길 때까지 거부한다.
- CSRF를 유지한다. 임시 Basic 로그인이나 클라이언트 `memberId` 인증은 제공하지 않는다.
- 요청 ID는 제한된 64자 패턴으로 검증하고 없거나 잘못되면 UUID를 만든다. 요청이 끝나면 MDC를 정리한다.
- 오류 응답은 코드·메시지·requestId·UTC 시각·필드 오류를 제공한다. 입력 원문·rejectedValue·스택트레이스는 반환하지 않는다.
- `ddl-auto=validate`, `open-in-view=false`를 기본으로 둔다. DB 변경은 Flyway 파일로만 진행한다.
- V001은 vector 확장만 활성화한다. 확장 생성 권한이 제한된 운영 DB는 운영자가 먼저 확장을 준비해야 한다.

## 공식 참고

- [Spring Boot 3.5 시스템 요구사항](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [springdoc 호환성 안내](https://springdoc.org/)
- [Spring Boot Testcontainers 연동](https://docs.spring.io/spring-boot/3.5/reference/testing/testcontainers.html)
- [Gradle Wrapper 검증](https://docs.gradle.org/8.14.3/userguide/gradle_wrapper.html)
- [pgvector 연산자](https://github.com/pgvector/pgvector#querying)

pgvector의 코사인 거리 연산자는 `<=>`다. `<->`는 L2 거리이므로 향후 코사인 검색에 혼용하지 않는다. RRF는 거리 점수를 직접 더하지 않고 순위를 결합한다.
