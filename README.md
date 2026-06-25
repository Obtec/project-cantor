# 🍎 Cantor Journal

오픈액세스 **다학제(multidisciplinary)** 연구 플랫폼 — 모든 학문 분야의 논문을
**제출·심사(peer review)·게재**할 수 있는 저널 웹 애플리케이션. (투고 시 분야 선택 지원)

- **프론트엔드**: React + Vite (사과나무 로고)
- **백엔드**: Spring Boot 3 (Java 21) + JWT 인증
- **DB**: MySQL 8
- **인프라**: nginx(리버스 프록시) + certbot(TLS)
- **구동/배포**: Docker + docker-compose

## 워크플로우

1. 저자가 회원가입 후 논문(메타 + PDF)을 **제출**한다.
2. 편집자가 **리뷰어를 배정**한다(논문 상태 → 심사중).
3. 리뷰어가 추천 의견·점수·코멘트로 **심사**를 제출한다.
4. 편집자가 심사를 보고 **게재/수정요청/반려**를 결정한다.

역할: `AUTHOR`(기본) · `REVIEWER` · `EDITOR` · `ADMIN`

## 빠른 시작 (로컬, Docker)

```bash
cp .env.example .env      # 값(비밀번호/시크릿) 채우기
docker compose up --build
```

- 앱: <http://localhost>
- API 헬스체크: <http://localhost/api/health>
- 시드 편집자 계정: `.env`의 `SEED_EDITOR_EMAIL` / `SEED_EDITOR_PASSWORD` (기본 `editor@cantor.org` / `editor1234`)

> 처음에는 회원가입으로 일반(저자) 계정을 만들고, 편집 기능은 시드 편집자 계정으로 로그인해 확인하세요.

## 개발 모드 (Docker 없이)

백엔드:
```bash
cd backend
./gradlew bootRun      # MySQL이 localhost:3306 에 떠 있어야 함 (또는 docker compose up db)
```
프론트엔드:
```bash
cd frontend
npm install
npm run dev            # http://localhost:5173 (/api 는 localhost:8080 으로 프록시)
```

> 로컬 JDK가 21보다 높으면 Gradle 빌드가 실패할 수 있습니다. 그 경우
> `docker compose build backend` 처럼 컨테이너 빌드(JDK 21 고정)를 사용하세요.

## 운영 배포 (HTTPS)

1. 서버에 도메인 DNS A 레코드를 연결한다.
2. `.env`에 `DOMAIN`, `LETSENCRYPT_EMAIL`을 채운다.
3. `docker compose up -d --build` 로 기동(이 시점엔 HTTP로 동작).
4. 인증서 발급:
   ```bash
   ./nginx/init-letsencrypt.sh
   ```
5. `nginx/conf.d/app.conf`의 HTTPS server 블록 주석을 해제하고 `example.com`을 실제 도메인으로 바꾼 뒤:
   ```bash
   docker compose restart proxy
   ```
   `certbot` 서비스가 12시간마다 자동 갱신한다.

## 구성

```
project-cantor/
├── docker-compose.yml      # db / backend(SPA 포함) / proxy / certbot
├── .env.example
├── backend/                # Spring Boot (com.cantor.journal) — 빌드된 프론트엔드를 정적 리소스로 서빙
├── frontend/               # React + Vite (빌드 산출물이 backend jar에 포함됨)
└── nginx/                  # edge nginx conf + certbot init 스크립트
```

## API 개요

| 메서드 | 경로 | 설명 | 권한 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 회원가입 | 공개 |
| POST | `/api/auth/login` | 로그인(JWT 발급) | 공개 |
| GET | `/api/auth/me` | 내 정보 | 인증 |
| GET | `/api/papers` | 논문 목록(`?status=`, `?mine=true`) | 공개 |
| GET | `/api/papers/{id}` | 논문 상세 | 공개 |
| POST | `/api/papers` | 논문 제출(multipart) | 인증 |
| PUT | `/api/papers/{id}` | 논문 수정/재업로드 | 저자 |
| GET | `/api/papers/{id}/file` | PDF 다운로드 | 공개 |
| POST | `/api/papers/{id}/assign` | 리뷰어 배정 | 편집자 |
| POST | `/api/papers/{id}/decision` | 게재 결정 | 편집자 |
| GET | `/api/papers/{id}/reviews` | 심사 의견 조회 | 편집자 |
| GET | `/api/reviews/assigned` | 배정된 심사 | 리뷰어 |
| POST | `/api/reviews` | 심사 제출 | 리뷰어 |
| GET | `/api/editor/reviewers` | 리뷰어 후보 | 편집자 |

## 추가된 저널 기능

- **알림**: 인앱 알림(🔔) — 투고/배정/심사/결정/발행 시 발송. `MAIL_ENABLED=true` + `spring.mail.*` 설정 시 이메일 동시 발송.
- **심사 결과 공개**: 편집 결정 후 저자가 익명 심사 의견(저자용 코멘트)을 열람.
- **수정 라운드·버전관리**: 수정요청 → 저자 재제출(응답서 + 새 PDF) → `v1, v2…` 이력. `GET /api/papers/{id}/versions`.
- **블라인드 테스트**: 리뷰어에게 게재 전 논문의 저자 정보를 숨김(`REVIEW_DOUBLE_BLIND`).
- **권/호 발행**: `Issue`(권·호·연도), 논문 호 배정·페이지 부여·발행, 호 목차(TOC). `/issues`, `/issues/{id}`.
- **영구 식별자**: 투고 시 `CJ-{연도}-{번호}` 부여.
- **피인용·지표**: 저널 내 인용 그래프 기반 피인용수, 확장형 저자 지표(h/g/i10-index 등 — `metric/` 빈 추가로 확장).
- **검색/색인**: 서버 검색+페이지네이션(`/api/papers/search`), Google Scholar용 `citation_*` 메타 서버 렌더(`/papers/{id}`), **RIS** 내보내기(`/api/papers/{id}/cite.ris`), BibTeX.
- **참고문헌 자동 추출**: 업로드 PDF(PDFBox)에서 저널 내 논문 제목을 매칭해 인용 관계 생성.

## 메모

- 초기 단순화를 위해 JPA `ddl-auto=update`를 사용합니다. 운영 전에는 Flyway 등으로
  스키마 마이그레이션을 관리하는 것을 권장합니다.
- 업로드 파일은 `uploads` Docker 볼륨에, DB 데이터는 `db_data` 볼륨에 영속됩니다.
