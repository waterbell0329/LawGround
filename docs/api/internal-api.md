# Spring ↔ Python API 합의 초안

현재 문서는 계획서 기반 초안이며 전체 OpenAPI와 실제 Python 서버가 구현된 상태는 아니다. Y01/S01에서 양측이 필드·오류·인증·샘플을 합의한 뒤 스키마를 고정한다.

Base path는 `/internal/v1`. Spring이 데이터 저장·검색 SQL·RRF·작업 상태를 담당하고 Python이 전달된 텍스트를 처리해 결과를 반환한다. 브라우저는 내부 API를 직접 호출하지 않는다.

| API | 요청 핵심 | 응답 핵심 | 상한 / 타임아웃 초안 |
|---|---|---|---|
| POST /embed | texts, modelVersion | vectors, dim, modelVersion | 32개 / 30초, 차원 1024 |
| POST /tokenize | texts, tokenizerVersion | tokens, tokenizerVersion | 32개 / 30초 |
| POST /hyde | taskType, 문제 전체, referenceDate | hypotheticalText, searchTerms, modelVersion, promptVersion | 60초 |
| POST /rerank | query, candidates, topK | ranked(articleId, score), modelVersion | 후보 30개 / 60초 |
| POST /verify | taskType, question, candidates, referenceDate | status, reasonCode, reason, evidences, 버전 | 후보 5개 / 120초 |
| POST /answer | message, history, context, referenceDate | answerText, citations, usage, 버전 | 확장 전용 / 120초 |
| GET /health/ready | 없음 | 준비 여부·모델 버전 | 준비 안 된 모델이면 실패 |

연결 타임아웃 초안은 3초다. 입력·출력 배열 순서를 유지하고 요청 후보 밖 articleId를 성공 결과로 저장하지 않는다. Spring에서도 JSON 타입·enum·숫자·인용 원문을 검증한다.

확정할 사항: 서비스 인증 헤더, 오류 JSON, 계약 버전, 멱등키 지원 여부, 각 필드 필수 여부, ID 문자열 규칙, 총 입력 크기·토큰 예산. `X-Request-Id`, `X-Job-Id`, `Idempotency-Key` 전달을 계획한다. 비밀값은 커밋하지 않는다.

검증 상태 SUPPORTED/HOLD/REJECTED는 작업 성공/실패와 다른 축이다. HTTP 어댑터 자체에서 자동 재시도하지 않고 작업 단계가 하나의 재시도 정책을 관리한다. 기본 제안은 최초 포함 최대 3회, HOLD 의미 재검색은 별도 최대 1회다.

Java 문자열 인덱스는 UTF-16 코드 유닛, Python 인덱스는 유니코드 코드 포인트다. 인용 오프셋은 합의한 하나의 기준으로 변환하고 한글·이모지·줄바꿈·반복 구절로 양쪽을 검증한다. 가상 조문을 실제 법령 원문으로 제시하지 않는다.
