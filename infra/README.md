# 인프라 구조

- 개발 DB·큐: 루트 `docker-compose.yml`
- PostgreSQL 애플리케이션 마이그레이션: `backend/src/main/resources/db/migration/`
- `postgres/`: 추후 DB 운영 초기화·권한 설정
- `proxy/`: 추후 TLS·리버스 프록시 설정
- 백엔드 컨테이너 빌드: `backend/Dockerfile`, 저장소 루트를 build context로 사용

현재 Compose는 DB와 RabbitMQ 포트를 `127.0.0.1`에만 연다. 인터넷 공개용 구성은 아니다. 기능과 운영 조건을 갖춘 뒤 배포 설정을 추가한다. 영속 볼륨을 제거하는 `down -v`는 일반 종료 명령으로 사용하지 않는다.
