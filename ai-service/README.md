# Python AI 서비스 자리

임수종 담당 서비스의 디렉터리다. 현재 `app/`, `tests/` 자리만 있으며 Python 기능·의존성·실행 서버는 아직 구현하지 않았다.

주차별 개인 계획이 들어 있는 루트 `AI/`는 로컬 전용이다. Windows는 `AI`와 `ai`를 같은 이름으로 취급하므로 실행 서비스는 반드시 `ai-service/`에 둔다.

첫 구현에서는 FastAPI와 요청/응답 스키마, readiness, 고정 응답 Mock을 준비한다. Spring과의 합의 기준은 [내부 API 개요](../docs/api/internal-api.md)다. DB와 RabbitMQ의 쓰기는 Spring이 담당한다.

`requirements.txt`와 실행 가능한 테스트가 추가되면 기존 CI의 `ai-check`가 동작한다. 실제 의존성 버전은 담당자가 실행환경을 확인한 뒤 고정한다.
