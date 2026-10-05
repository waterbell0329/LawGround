# 임수종 님께 요청하는 Python AI API 명세·협의 자료

작성: 윤영주 / 2026-10-05 / 계약 기준: 0.1.0-draft

수종 님, Spring의 문제 저장·단건 조회와 계약 초안을 준비했습니다. **첨부한 내부 계약을 그대로 구현 완료한 것으로 전제하지 않고**, Python 쪽에서 실제 가능한 형식·모델·제약을 받아 양쪽 계약을 맞추려고 합니다. 아래 각 항목에 `수락 / 수정 제안 / 미정 / 미구현`과 근거·준비 예정 시점을 적어 주세요.

기존1·2주차는 공유용 **통합1주차**로 묶었습니다. 아래 일정은 통합 주차를 사용하며, 기존 날짜를 앞당긴 것이 아닙니다. 실제 Python 진행 상태·납기는 수종 님의 답변을 기준으로 기록하겠습니다.

## 1. 먼저 보내 주실 자료

| 우선순위 | 요청 산출물 | 꼭 포함할 내용 | 필요한 이유 |
|---|---|---|---|
| P0 | Python 내부 OpenAPI YAML/JSON | 실제 경로·method·헤더·인증·요청/응답·required/nullable·enum·상한·상태코드, 계약 버전 | Spring DTO/HTTP 클라이언트/검사 기준 고정 |
| P0 | 초안 차이표 C01~C12 | 현재 초안 수락 여부, 바꾸려는 정확한 필드/값/이유, 결정 담당·예정 시점 | 말로 합의한 내용과 코드가 달라지는 것 방지 |
| P0 | 실행/접속 안내 | 저장소/브랜치/commit, Python/FastAPI 등 실제 버전, 실행 명령·컨테이너 여부, 포트/base URL, 필수 환경변수 **이름만** | 같은 환경에서 재현 |
| P0 | 엔드포인트별 정상·오류 JSON fixture | 요청과 대응 응답, 예상 HTTP 상태·헤더, Mock/실모델 여부 | 실제 모델 준비 전 계약 테스트 |
| P0 | 모델·전처리 manifest | 임베딩/리랭커/LLM 정확한 식별자·revision, tokenizer·prompt 버전, 차원·토큰 상한·정규화 | 검색 색인과 질의의 호환성 보장 |
| P1 | 실제 호출 증거 | 실행 commit·환경·시각, 단계별 성공/실패 로그 요약, 측정된 처리시간/동시성/배치 한도 | Mock와 실제 연결 구분 |
| P1 | 검증 판정/선지 태그 사전 | SUPPORTED/HOLD/REJECTED 기준, reasonCode, 선지 relation, 태그 정의·버전 | Spring 저장/화면/골든셋 평가 연결 |

아직 API가 없으면 먼저 합의용 스펙과 고정 JSON Mock를 주세요. 실행하지 않은 모델 성능·처리시간은 추정값 또는 미측정으로 적어 주세요. API 키·토큰 실값·개인 데이터는 명세나 노션에 쓰지 말고 별도 보안 경로로 전달해 주세요.

## 2. 역할·호출 방향 확인 — C01

현재 제안은 **Spring이 DB·RabbitMQ·작업 상태를 소유하고, Spring 워커가 별도 Python HTTP 서버를 호출**하는 구조입니다. Python은 입력을 받아 계산 결과를 반환합니다. Python이 PostgreSQL에 직접 결과를 저장하거나 RabbitMQ를 직접 소비하는 것은 현재 확정안에 포함되지 않습니다.

확인해 주세요:
- 별도 FastAPI 서버를 사용할지, 실제 실행 기술·포트·프로세스 구조는 무엇인지.
- Python 워커로 변경을 제안한다면 큐 소비·재시도·결과 저장·ACK·취소 책임을 누가 맡을지. HTTP 모델 서버와 큐 워커를 구분해 설명해 주세요.
- 로컬에서 두 프로세스가 같은 PC인지, 서로 다른 PC/컨테이너인지. 초안의 127.0.0.1은 같은 네트워크 공간에서만 통합니다. 실제 호출 가능한 주소를 알려 주세요.

## 3. 공통 통신 계약 — C02

초안 base URL: `http://127.0.0.1:8001/internal/v1`. 각 아래 경로에 이 prefix를 붙입니다.

| 항목 | Spring 측 현재 초안 | 받아야 할 답 |
|---|---|---|
| 형식 | UTF-8 application/json, UUID 식별자, 날짜 YYYY-MM-DD, 시각 UTC Z | Python 모델의 실제 타입/직렬화와 불일치 여부 |
| 서비스 인증 | X-Internal-Token, 환경변수 주입 | 헤더·검증 위치·토큰 교체/실패 응답, TLS/접근 범위 |
| 추적 | POST 헤더 X-Request-Id와 본문 requestId | 두 값 일치 강제 여부, 한 작업의 재시도마다 유지/변경 규칙 |
| 버전 | POST 헤더 X-Contract-Version: 0.1.0-draft | 합의 후 버전, 지원하지 않는 버전의 응답 |
| 작업 호출 | X-Job-Id와 Idempotency-Key(UUID) 추가 | 필수 호출 범위·키 재사용 규칙·보존기간 |
| 입력 | required/nullable/추가 필드 금지 구분 | 누락·null·빈 문자열·숫자 문자열·float→int·중복 JSON 키·후행 JSON 처리 |
| 배치 | ID/개수/순서 보존, 누락·중복 금지 | 일부 실패 시 전체 오류인지 항목별 결과인지 |
| 시간 | 연결3초, embed/tokenize30초, hyde/rerank60초, verify120초 | 실제 최악 지연·타임아웃·모델 로딩 시간을 고려한 지원값 |
| 크기 | 항목별 문자열/개수 상한은 YAML에 존재 | 전체 HTTP 요청/응답 바이트 상한, 프록시/서버 설정 |

**주의:** 외부 문제 입력 API의128KiB 제한을 내부 AI API에 그대로 적용하면 긴 법령 후보가 들어가지 않을 수 있습니다. 내부 본문 상한은 따로 정해야 합니다. 청크 문자열 상한과 실제 모델 토큰 상한도 서로 다릅니다.

## 4. 경로별 정확한 요청·응답

아래는 첨부 `docs/api/internal.openapi.yaml`의 **현재 제안**을 요약한 것입니다. 모든 listed 필드는 별도 표시가 없으면 required입니다. 실제 수락/수정 여부를 답해 주세요. 추가 속성은 초안에서 금지합니다. `[]`는 배열 원소, `?`는 필드 생략이 아닌 nullable 설명을 뜻합니다.

### /embed — C03

- POST 요청: `requestId: UUID`, `texts: [{id: UUID, text: string}]`, `modelRevision: string`.
- texts는1~32개, text는1~30,000 code points. 공백만 있는 문자열 금지.
- 200 응답: `items: [{id: UUID, vector: number[1024]}]`, `dim: 1024`, `modelRevision: string`.
- ID·순서·개수 보존, 요청 revision과 응답 revision 일치. NaN/Infinity·영벡터 금지.
- BGE-M3 dense/1024는 기준안입니다. 실제 모델 공급자·정확한 가중치 revision·라이선스/실행환경·dtype·정규화·출력 차원을 주세요.
- 문서/질의 전처리와 prefix가 같은지, query/document용 설정을 나눠야 하는지 알려 주세요. 현재 DTO에 inputType이 없으므로 필요하면 필드 추가를 제안해 주세요.
- 한 항목이 토큰 상한을 넘으면 오류인지, 재분할 요청인지 명시해 주세요. 묵시적 잘라내기로 법적 조건을 누락하지 않도록 합의가 필요합니다.
- 동일 차원이어도 revision이 다르면 기존 색인과 혼합할 수 없습니다. revision 선택 지원 또는 불일치 거절 규칙을 알려 주세요.

### /tokenize — C04

- POST 요청: `requestId: UUID`, `texts: [{id,text}]`, `tokenizerVersion: string`.
- 배치1~32개, text1~30,000 code points.
- 200 응답: `items: [{id: UUID, tokens: string[]}]`, `tokenizerVersion: string`.
- tokens는0~10,000개, 토큰당1~200 code points. 빈 토큰 배열은 유효한 결과와 장애를 구분해야 합니다.
- **PostgreSQL 단어 검색용 한국어 토큰화**입니다. 임베딩 모델의 subword 토큰 ID를 반환하는 것으로 오해하지 않도록 형태소 분석기/사전/버전을 알려 주세요.
- 조사·불용어·부정어·법령명·조문 번호·숫자/단위·반복 토큰·순서·Unicode 정규화 처리 규칙과 전후 예시를 주세요.
- 조문 적재와 사용자 질의에 같은 토큰화 규칙을 적용합니다. Spring 초기 계획은 simple tsvector/tsquery 및 ts_rank_cd입니다. 검색어 AND/OR 확장은 별도 합의입니다.

### /hyde — C05

- POST 요청: `requestId: UUID`, `questionSnapshot: QuestionSnapshot`, `referenceDate: date`.
- QuestionSnapshot: `subject`, `stem`, `questionType`, `choices[{number,text}]`, `providedAnswerNumber`, `referenceDate`, `revision`.
- subject=BROKER_LAW, 유형=SELECT_CORRECT/SELECT_INCORRECT, 선지1~5 각1회·총5개, 제공 정답1~5, revision≥1. 지문≤10,000/선지≤2,000 code points.
- 바깥 referenceDate와 snapshot.referenceDate는 같아야 합니다. 문제를 긍정형으로 바꾸거나 제공 정답을 임의 수정하지 않습니다.
- 200 응답: `hypotheticalText: string(1~20,000)`, `searchTerms: string[](0~20개, 각1~200)`, `modelRevision`, `promptVersion`.
- 실제 LLM 식별자, prompt 버전·입출력 token 상한·생성 파라미터·구조화 응답 실패 처리를 주세요.
- searchTerms는 HyDE 단계의 검색 보조어입니다. 법령 최초 적재 때 검색어를 생성해 저장한다는 뜻이 아닙니다. 최초 적재에서는 법령 청크를 /embed와 /tokenize에 보냅니다.
- 원문 질의와 HyDE를 각각 검색해 합칠지, 한 문장으로 조합할지 아직 구체 합의가 필요합니다. lexical 입력에 searchTerms를 어떻게 쓰는지, RRF에 동일 채널을 중복 가중하지 않도록 질의/채널 구성을 명시해 주세요.
- HyDE OFF 원문 검색을 baseline으로 유지합니다. 가상 문서는 근거 인용/DB 법령 원문이 아니며 기본 ON 여부는 골든셋 비교로 결정합니다.

### /rerank — C06

- POST 요청: `requestId: UUID`, `query: string(1~30,000)`, `candidates: [{chunkId: UUID, text: string(1~30,000)}]`, `topK: integer(1~5)`.
- 후보1~30개. 200 응답: `ranked: [{chunkId: UUID, score: number}]`, `modelRevision`.
- 반환1~5개이면서 topK 이하, 후보 ID만 사용·중복 금지·score 내림차순. 후보가 topK보다 적을 때와 동점 처리 규칙을 답해 주세요.
- 실제 reranker 모델/revision·cross-encoder 등 구조·점수 의미/범위/방향·유한 수 검사·입력 토큰 초과 처리를 주세요. score를 정답 확률 또는 법적 타당성으로 해석하지 않습니다.
- query를 `지문+문제 유형+선지+제공 정답`으로 어떻게 구성할지, HyDE 문구를 넣을지, 후보 text는 searchText인지 sourceText인지 확정해야 합니다.
- 모델별 점수는 직접 비교하지 않습니다. threshold를 사용한다면 근거/평가셋/버전까지 주세요.

### /verify — C07

- POST 요청: `requestId`, `questionSnapshot`, `referenceDate`, `candidates: VerifyCandidate[1~5]`.
- VerifyCandidate: `chunkId: UUID`, `sourceText: string(1~200,000)`, `metadata`.
- metadata: `articleId: UUID`, `lawVersionId: UUID`, `lawTitle: string(1~200)`, `articleLabel: string(1~200)`, `effectiveFrom: date`, `effectiveTo: date|null`.
- 200 응답 필수 필드: `verdict`, `reasonCode`, `summary`, `evidences`, `choiceAnalyses`, `tagSetVersion`, `modelRevision`, `promptVersion`.
- verdict=SUPPORTED/HOLD/REJECTED. reasonCode는1~80자 문자열로만 정의돼 있고 **코드 사전은 아직 미합의**입니다. summary는1~10,000자입니다.
- evidences는0~50개. 항목은 `evidenceKey: string(1~80)`, `chunkId: UUID`, `quote: string(1~20,000)`, `start: integer≥0`, `end: integer≥0`.
- SUPPORTED는 실제 근거가 필요합니다. HOLD·REJECTED의 근거 요구·부분 근거·상충 조문·버전 부족·모델 실패 처리를 예시와 함께 주세요. HTTP 성공의 판정과500/504 같은 기술 장애를 구분합니다.
- SELECT_CORRECT/SELECT_INCORRECT, 사용자 제공 정답과 법령이 충돌하는 경우, 다수 선지 판정이 가능한 경우를 어떻게 처리할지 알려 주세요. 임의로 정답을 바꿔 응답하지 않습니다.
- 후보가0개이면 현재 DTO로 /verify를 호출할 수 없습니다. Spring이 HOLD 처리할지, 계약을 확장할지 합의가 필요합니다.
- 긴 원문의 실제 토큰 상한·분할/재시도 방식·출력 JSON 파싱 실패 시 정책을 주세요. LLM이 출력한 법령명/URL을 새 공식 출처처럼 저장하지 않습니다.

### 인용·선지 상세 — C08

원문은 불변 sourceText이며 검색용 searchText/가상문서를 인용하지 않습니다. 후보 밖 chunkId 금지, `0 ≤ start < end ≤ 원문 code point 길이`, `sourceText[start:end] == quote`가 되어야 합니다.

```text
원문: 앞😀근거 문장\n뒤   (\n은 실제 줄바꿈 1자)
인용: 근거 문장
start=2, end=7   (Unicode code point, 시작 포함/끝 제외)
```

Python 인덱스와 Java UTF-16 인덱스를 섞지 않습니다. Java는 code point 인덱스를 변환해 검사합니다. 원문 재정규화·줄바꿈 변경·동일 문구가 두 번 있는 경우를 포함한 fixture를 주세요.

choiceAnalyses는 **필수 필드이며 값은 null 또는 정확히5개 배열**입니다. null은 아직 선지 분석을 제공하지 않는 뜻입니다. 제공 시 choiceNumber1~5가 각각 한 번 있어야 합니다.

| VerifyChoice 필드 | 현재 초안 |
|---|---|
| choiceNumber | integer1~5 |
| relation | SUPPORTS / CONTRADICTS / INSUFFICIENT |
| explanation | string1~10,000 |
| correctedText | string1~2,000 또는 null. 근거 없는 정정 금지 |
| tags | 배열0~20개, 각 `{code: string1~80}` |
| evidenceKeys | 문자열0~20개·중복 금지, 응답 evidences의 evidenceKey만 참조 |

위 필드는 모두 required입니다. tagSetVersion도 필수 필드이며 string1~80 또는 null입니다. 태그가 없으면 빈 배열, 아직 태그 사전이 미제공이면 버전 null 등의 조합을 양측에서 확정해야 합니다.

evidenceKey는 한 응답 안에서 유일한 임시 식별자입니다. Spring이 저장하면서 evidenceId(UUID)로 바꿉니다. Python은 DB PK를 만들지 않습니다. 외부 응답의 originalText는 Spring 입력 스냅샷에서 복원합니다.

태그명/정의/중복 허용·수치 오류 포함 여부·분류 불가·복수 인용·정정문 필요 조건을 알려 주세요. 제공 정답 번호와 선지 relation을 같은 값처럼 쓰지 않습니다. 틀린 설명을 고르는 문제의 제공 정답은 CONTRADICTS일 수 있습니다.

### /health/ready 및 /answer — C11 / C12

- GET /health/ready: 200 또는503, 본문은 `{status: READY|NOT_READY, models:[{name, revision:string|null, ready:boolean}]}`. 각 필드 required, models0~20개.
- readiness의503은 ReadyOutput입니다. 모델 실행 API의503은 Error DTO이므로 Spring 파싱에서 구분해야 합니다.
- 프로세스만 살아 있는지, 필수 모델 전체가 준비됐는지, GPU/외부 LLM 연결까지 확인하는지 알려 주세요. 선택 모델이 없을 때 전체 준비 실패로 볼지도 결정해야 합니다.
- POST /answer는 **후속 확장**입니다. 이번 핵심 연결 완료 조건에 넣지 않습니다. 지원 예정 여부만 답해도 됩니다. 나중에 다룰 때 message/history/context/referenceDate, answerText/citations/usage 및 보존/비용 정책을 별도 합의합니다.

## 5. 오류·시간 제한·멱등·취소 — C09

모델 실행 API 오류 본문 초안: `{code, message, requestId:UUID, timestamp:UTC Z, retryable:boolean}`. 모든 필드 required, message1~500자, 입력 원문/키/내부 stack trace 제외.

| HTTP | 현재 코드 | 현재 제안 | 답변할 사항 |
|---|---|---|---|
| 400/422 | INVALID_REQUEST | 재시도 안 함 | 두 상태의 구분, FastAPI 기본422를 공통 DTO로 감쌀지 |
| 429 | RATE_LIMITED | Retry-After를 고려한 제한 재시도 | 초/HTTP-date 형식, 누락 시 정책, 외부 공급자 제한 전파 |
| 503 | MODEL_NOT_READY | 준비 후 제한 재시도 | 로딩·GPU부족·외부 장애 구분 |
| 504 | MODEL_TIMEOUT | 제한 재시도 후보 | 서버 연산이 실제 중지되는지, 늦은 응답·중복 비용 처리 |
| 500 | INTERNAL_ERROR | 제한 재시도 후보 | retryable=false인 영구 오류 구분 |

Spring job 단계가 재시도를 관리하고 HTTP 클라이언트와 Python/모델 SDK에서 이중 자동 재시도하지 않는 방향입니다. 최초 포함 최대3회, HOLD 의미 재검색 최대1회는 서로 다른 횟수로 제안합니다. Python SDK에 기본 재시도가 있다면 횟수/비활성화 방법을 알려 주세요.

Idempotency-Key가 같고 payload가 같을 때 동작, 다른 payload일 때 오류, 처리 중 중복 요청, 결과 캐시 TTL/프로세스 재시작/모델 revision 변경 시 동작을 주세요. **현재 초안만으로 Python이 멱등성을 지원한다고 가정할 수 없습니다.** 미지원이면 비용·중복 호출 영향을 공동 결정해야 합니다.

사용자가 작업을 취소하거나 HTTP 연결이 끊겨도 LLM 연산이 계속되는지, 서버 측 deadline 전달/취소 API가 필요한지 답해 주세요. 현재 wire에는 deadline/cancel 경로가 없습니다. Spring의 늦은 결과 저장 차단과 Python 계산 중단은 별도 책임입니다.

## 6. 모델/평가·배포 — C10

단계별 manifest에 `역할 / 공급자·정확한 모델명 / revision 또는 배포 식별자 / tokenizer / promptVersion / 차원 / query·document 전처리 / token 상한 / dtype·정규화 / 배치·동시성 / CPU·GPU·VRAM / 라이선스 / 실행 commit`을 적어 주세요. API 제공자가 immutable revision을 주지 않으면 그 한계와 대체 식별 방법을 명시해 주세요.

골든셋은 사람이 검수한 기준으로 평가합니다. expectedArticleIds/기대 판정은 평가기에만 전달하고 /hyde·/rerank·/verify 입력에 정답 근거를 몰래 추가하지 않습니다. 사용자 제공 정답 번호는 정상 입력입니다. Mock fixture·개발 평가셋·최종 평가셋을 구분해 주세요.

측정값을 줄 때 데이터셋 버전·문항 수·모델/prompt 버전·실행환경·실패 포함 분모·지연 측정 조건을 붙여 주세요. 골든셋으로 모델을 미리 학습해야 한다거나 모델이 추가 기출을 생성한다는 전제는 없습니다.

## 7. 현재 초안에서 반드시 같이 고쳐야 할 부분

아래는 첨부 파일을 실제 대조해서 찾은 **협의 필요 지점**입니다. 명세 검증 통과가 이 의미적 불일치까지 해결해 주지는 않습니다.

| 항목 | 관찰 | 필요한 결정 |
|---|---|---|
| C02 requestId | POST 헤더는1~64자 일반 문자열, 본문/오류는UUID | 헤더도UUID로 통일할지·본문과 일치 규칙 |
| C11 readiness 헤더 | 설명에는 인증·request/contract 헤더 필수 표현, YAML GET에는 인증만 있고 parameters 없음 | GET에도 추적/버전 헤더를 요구할지 양 문서 동일화 |
| C09 인증/계약 오류 | 인증이 필요한데 실행 경로에401/403 응답 정의와 인증 실패 code가 없음 | HTTP·오류 코드·retryable 결정, 버전/멱등 충돌 코드도 추가 필요 여부 |
| C02 전체 크기 | 항목 수/문자열 길이는 있으나 내부 총 payload 바이트 제한 없음 | Python/프록시/Spring 클라이언트가 수용할 상한 |
| C05 질의 구성 | HyDE와 원문을 결합하는 방식이 wire로 정해지지 않음 | dense/lexical 입력과 합산 규칙·baseline·검색 버전 |
| C06/C07 버전 고정 | /embed·/tokenize는 요청에 버전 선택, /hyde·/rerank·/verify는 응답에 버전만 있음 | 실행 중 모델/prompt가 바뀌지 않도록 expected version 또는 고정 배포 manifest 사용 |
| C07 reasonCode/무후보 | 코드사전 없음, /verify 후보 최소1개 | 사전과0후보 처리 위치, REJECTED의 근거 요구 |
| C08 선지 상세 | null/5개 구조는 있으나 태그 사전·판정 기준·null 조합은 미합의 | 초기/확장 제공 시점과 fixture |
| C09 중복·취소 | 헤더는 있으나 TTL/처리 중 동작/deadline wire 없음 | 실제 지원 범위와 비용·늦은 결과 정책 |

`modelRevision`, `promptVersion`, `tokenizerVersion` 일반 문자열은 초안에서1~160자입니다. 수종 님의 실제 식별자가 범위를 넘거나 필드 구성이 다르면 정확한 변경안을 주세요. 소리 없이 필드 이름만 바꾸지 말고 OpenAPI·예시·검사기를 함께 갱신해야 합니다.

## 8. 이 형식으로 답변 부탁드립니다

| ID | 답변 주제 | 수락/수정/미정 | 실제 구현 상태(Mock/실모델/미구현) | 변경할 필드·제약·예시 | 근거 commit/실행 자료 | 준비 목표일 |
|---|---|---|---|---|---|---|
| C01 | 서버/워커·DB/큐 책임 |  |  |  |  |  |
| C02 | 공통 wire·ID·인증·상한 |  |  |  |  |  |
| C03 | /embed 모델/차원/전처리 |  |  |  |  |  |
| C04 | /tokenize 사전/버전/토큰 |  |  |  |  |  |
| C05 | /hyde 생성·검색 조합 |  |  |  |  |  |
| C06 | /rerank 입력/점수/모델 |  |  |  |  |  |
| C07 | /verify 판정/reasonCode |  |  |  |  |  |
| C08 | 인용 offset/선지/태그 |  |  |  |  |  |
| C09 | 오류/재시도/멱등/취소 |  |  |  |  |  |
| C10 | 모델 manifest/환경/평가 |  |  |  |  |  |
| C11 | 준비 상태와 헤더 |  |  |  |  |  |
| C12 | /answer 확장 여부 |  |  |  |  |  |

API별로 `정상1개 + 입력 오류1개 + 관련 운영 오류1개` 이상의 재현 fixture를 주세요. /verify는 SUPPORTED/HOLD/REJECTED, 부정형·근거 부족·인용 불일치·이모지/줄바꿈·반복 구절도 포함해 주세요. 미지원 조합은 가짜 성공 응답 대신 지원 범위를 적어 주세요.

## 9. 합의/연결 완료 체크

- [ ] C01~C11 핵심 항목에 양측 답변과 최종 합의 버전이 있다. C12는 확장으로 별도 관리한다.
- [ ] OpenAPI와 Pydantic 모델·실제 HTTP 응답의 required/null/type/enum/상한이 일치한다.
- [ ] 배치 ID/순서, 모델·기준일, 후보 ID·topK, 인용 code point, evidenceKey 참조를 왕복 검증한다.
- [ ] 오류·429/503/504·멱등 재전송·타임아웃 뒤 늦은 결과를 실제로 재현한다.
- [ ] Spring과 Python이 같은 fixture를 사용하고 실행 commit·환경·결과를 남긴다.
- [ ] 합의 완료 / Mock 연결 / 실제 모델 연결 / 평가 품질 통과를 각각 기록한다.

계약 참고 묶음에서 실행할 검사는 `python -m pip install -r scripts/requirements-contracts.txt`, `python scripts/check_contracts.py`, `python -m unittest discover -s scripts/tests -v`입니다. 이는 정적 계약·fixture 검사이며 실제 서버 호출/모델 품질을 대신하지 않습니다. 실제 내부 API가 준비되면 별도 HTTP 계약 테스트를 추가해야 합니다.

Spring 연동 목표: **통합2주차**에 Mock·기본 wire 협의, **통합3주차**에 /embed·/tokenize 기반 색인/검색 호환성, **통합5주차**에 준비된 실제 단계 연결, **통합6주차**에 /verify·골든셋 검증, **통합7주차**에 선지 분석 확장 연결. 수종 님의 가능한 제공 일정과 맞춰 최종 확정하겠습니다.
