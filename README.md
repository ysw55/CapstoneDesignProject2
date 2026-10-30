# 📖 Book Canvas

> 책을 읽고 마음에 남은 한 문장을 기록하고 나누는 **독서 기록 · 문장 큐레이션 커뮤니티**

화이트 · 블랙 · 딥그린(`#1B4D3E`) 테마의 모바일 우선(최대 폭 430px) 웹앱입니다. 추후 웹뷰로 감싸 하이브리드 앱으로 출시할 예정입니다.

| 탭 | 기능 |
|---|---|
| **커뮤니티** | 인상 깊은 문장 + 상황 태그(💧 위로가 필요할 때 등) 피드, 좋아요 · 댓글, 내 글 수정 · 삭제 |
| **랭킹** | 이달의 추천 도서, 주간 베스트 문장 *(현재 샘플 데이터)* |
| **내 책장** | 완독한 책을 서재처럼 진열, **완독 인증**(독후감 200자 이상) 시 ✓ 배지 + 100P + 프로필 스킨 |
| **프로필** | 포인트 · 보상 스킨 · 설정(로그인, 비밀번호 변경, 다크 모드, 알림) |

---

## 🛠 기술 스택

| 구분 | 사용 기술 |
|---|---|
| Backend | **Java 17**, **Spring Boot 3.3.5**, Spring Security + JWT(jjwt 0.12), Spring Data JPA(Hibernate 6), Bean Validation, Lombok |
| Database | **MySQL 8.0** (테스트는 H2 인메모리) |
| Frontend | **React 18**, **Vite 5**, React Router 6, styled-components 6, lucide-react |
| Build | Maven 3.9+, Node.js 20+ / npm 10+ |

---

## 📁 폴더 구조

```
capstone-design/
├─ backend/                        # Spring Boot
│  ├─ pom.xml                         # 백엔드 Maven 설정
│  ├─ application-local.example.yml   # 로컬 비밀 설정 예시 (복사해서 application-local.yml 로 사용)
│  ├─ src/main/java/com/bookcanvas/
│  │  ├─ domain/
│  │  │  ├─ user/     # 회원가입·로그인·JWT·비밀번호 변경·시연용 간편 로그인
│  │  │  ├─ book/     # 내 책장, 완독 인증
│  │  │  ├─ quote/    # 커뮤니티 글, 좋아요, 댓글
│  │  │  └─ tag/      # 상황 태그
│  │  └─ global/      # 보안 설정, 공통 예외 처리, 시연용 샘플 데이터
│  ├─ src/main/resources/application.yml
│  └─ src/test/                       # 단위 · 통합 테스트 (H2)
└─ frontend/                       # React + Vite
   └─ src/
      ├─ api/         # 백엔드 API 호출 (JWT 자동 첨부)
      ├─ context/     # 로그인 상태, 피드 · 책장 데이터
      ├─ pages/       # 탭 화면, 글쓰기 · 책장 · 완독 인증 폼
      └─ components/
```

---

## ✅ 실행 전 준비사항

### 1. 설치

- **JDK 17 이상** — `java -version`
- **Maven 3.9 이상** — `mvn -v`
- **Node.js 20 이상** — `node -v`
- **MySQL 8.0** — `localhost:3306` 에서 실행 중이어야 합니다

### 2. MySQL 데이터베이스

서버를 처음 실행할 때 `bookcanvas` DB가 **자동으로 생성**됩니다(`createDatabaseIfNotExist=true`).
직접 만들어 두고 싶다면 MySQL 에서 아래를 실행하세요.

```sql
CREATE DATABASE IF NOT EXISTS bookcanvas
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

> 테이블은 JPA가 자동으로 만듭니다(`ddl-auto: update`). SQL 스크립트를 따로 실행할 필요가 없습니다.

### 3. 비밀 설정 파일 만들기 ⚠️ 필수

DB 비밀번호와 JWT 키는 GitHub 에 올리지 않습니다. `backend/` 폴더에서 예시 파일을 복사한 뒤 **내 값으로 수정**하세요.

```bash
cd backend
# Windows
copy application-local.example.yml application-local.yml
# macOS / Linux
cp application-local.example.yml application-local.yml
```

```yaml
# application-local.yml
DB_PASSWORD: 내-MySQL-비밀번호
JWT_SECRET: Base64로-인코딩된-32바이트-이상-무작위-값
```

JWT 키 만드는 법:

```powershell
# Windows PowerShell
[Convert]::ToBase64String((1..32 | % { Get-Random -Max 256 }))
```
```bash
# macOS / Linux
openssl rand -base64 32
```

> `application-local.yml` 은 `.gitignore` 에 등록되어 있어 커밋되지 않습니다. 같은 이름의 **환경 변수**(`DB_PASSWORD`, `JWT_SECRET`)로 넘겨도 됩니다.

### 4. 도서 검색 키 (선택)

책 추가 화면(`+` 버튼)의 **책 검색**은 [카카오 책 검색 API](https://developers.kakao.com/docs/ko/daum-search/dev-guide)를 씁니다.
키가 없으면 검색만 꺼지고 **직접 입력**은 그대로 됩니다.

1. [카카오 디벨로퍼스 콘솔](https://developers.kakao.com/console/app)에 카카오 계정으로 로그인 → 앱 만들기 (메뉴 이름은 "내 애플리케이션 → 애플리케이션 추가하기" 또는 "앱 → 앱 생성"으로 보입니다)
2. 만든 앱을 열어 **앱 → 플랫폼 키 → REST API 키** (예전 화면은 "앱 설정 → 요약 정보") 에서 키 복사
3. `application-local.yml` 에 한 줄 추가 (또는 환경 변수 `KAKAO_REST_API_KEY`)

```yaml
KAKAO_REST_API_KEY: 복사한-REST-API-키
```

서버를 다시 실행하면 적용됩니다.

---

## ▶️ 실행 방법

### 백엔드 (http://localhost:8080)

```bash
# backend 폴더에서 실행 (application-local.yml 을 찾는 기준 위치)
cd backend
mvn spring-boot:run
```

로그에 `Started BookCanvasApplication` 이 보이면 성공입니다. 처음 실행하면 시연용 샘플 데이터(사용자 · 피드 · 책장)가 자동으로 들어갑니다.

### 프론트엔드 (http://localhost:5173)

```bash
cd frontend
npm install      # 처음 한 번
npm run dev
```

브라우저에서 **http://localhost:5173** 접속 → 개발자 도구에서 모바일 화면(예: iPhone 14)으로 보면 실제 앱처럼 보입니다.

> 개발 서버는 `/api` 요청을 백엔드(8080)로 프록시하므로 CORS 설정 없이 동작합니다. **백엔드를 먼저 실행**하세요.

### 테스트

```bash
cd backend
mvn test        # 백엔드 단위 · 통합 테스트 (H2 사용, MySQL 불필요)
```

---

## 🎬 시연 가이드

### 시연용 간편 로그인 (카카오 · 구글)

설정 · 프로필 · 로그인 화면의 **카카오 / Google 버튼**을 누르면 바로 로그인됩니다.

| 버튼 | 로그인되는 계정 | 미리 준비된 데이터 |
|---|---|---|
| 카카오 | `moonreader_` | 책장 6권 (완독 인증 3권), 300P, 커뮤니티 글 1개 |
| Google | `구글독자` | 책장 2권 (완독 인증 1권), 100P |

> ⚠️ **실제 카카오/구글 OAuth 가 아닙니다.** 발표 시연을 위해 서버에 준비된 고정 계정으로 로그인합니다.
> 실서비스 배포 시에는 환경 변수 `DEMO_LOGIN_ENABLED=false` 로 꺼야 합니다.

### 추천 시연 순서

1. 비로그인 상태로 커뮤니티 피드 둘러보기 → 좋아요를 누르면 로그인 안내
2. **카카오** 로그인 → 내 책장(✓ 배지 3개) · 프로필(300P) 확인
3. 피드에서 좋아요 · 댓글 → 새로고침해도 유지 (MySQL 저장)
4. 기록하기 → **완독 인증** → 독후감 200자 미만이면 버튼 비활성, 200자 이상 작성 후 인증 → +100P
5. 로그아웃 → **Google** 로그인 → 다른 계정의 책장으로 바뀌는 것 확인
6. 이메일 회원가입 → 설정 → **비밀번호 변경**

### 기타 샘플 계정

커뮤니티 글을 쓴 샘플 사용자(`dawn@demo.bookcanvas.app` 등 5명)는 이메일 로그인으로도 들어갈 수 있습니다. 비밀번호: `demo1234`

### 샘플 데이터 초기화

```sql
DROP DATABASE bookcanvas;
```
그다음 백엔드를 다시 실행하면 DB가 새로 생성되고 샘플 데이터가 다시 들어갑니다.

---

## 🔌 주요 API

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/auth/signup` | 회원가입 (토큰 발급) | - |
| POST | `/api/auth/login` | 로그인 | - |
| POST | `/api/auth/demo-login` | 시연용 카카오/구글 로그인 `{ "provider": "kakao" }` | - |
| GET | `/api/auth/me` | 내 정보 | JWT |
| POST | `/api/auth/password` | 비밀번호 변경 | JWT |
| GET | `/api/books/completions` | 내 책장 | JWT |
| POST / PUT / DELETE | `/api/books/completions[/{id}]` | 책장 추가 · 수정 · 삭제 | JWT |
| GET | `/api/books/search?q=&page=` | 도서 검색 (카카오, 10권씩). 키가 없으면 503 `BOOK_SEARCH_UNAVAILABLE` | JWT |
| GET / POST | `/api/books/shelves` | 책장 목록 · 책장 만들기 (최대 5개, 이름 비우면 "책장 N") | JWT |
| PUT / DELETE | `/api/books/shelves/{id}` | 책장 이름 바꾸기 · 삭제 (책은 첫 번째 책장으로 이동, 마지막 책장은 삭제 불가) | JWT |
| PUT | `/api/books/completions/{id}/shelf` | 책을 다른 책장으로 옮기기 `{ "shelfId": 2 }` | JWT |
| POST / DELETE | `/api/books/completions/{id}/archive` | 보관함으로 옮기기 · 책장으로 꺼내기 (인증한 책도 가능, 기록·포인트 유지) | JWT |
| PUT | `/api/books/completions/order` | 책장 순서 저장 `{ "shelfId": 2, "completionIds": [그 책장 책 id를 원하는 순서로] }` | JWT |
| POST | `/api/books/completion` | 완독 인증 (독후감 200자 이상) | JWT |
| GET | `/api/quotes` | 커뮤니티 피드 | 선택 |
| POST / PUT / DELETE | `/api/quotes[/{id}]` | 글쓰기 · 수정 · 삭제 | JWT |
| POST / DELETE | `/api/quotes/{id}/like` | 좋아요 · 취소 | JWT |
| GET / POST | `/api/quotes/{id}/comments` | 댓글 조회 · 작성 | 조회는 공개 |

인증이 필요한 요청에는 `Authorization: Bearer <accessToken>` 헤더를 붙입니다. 에러는 `{ status, code, message, fieldErrors }` 형식으로 응답합니다.

---

## 🧯 문제 해결

| 증상 | 해결 |
|---|---|
| `Could not resolve placeholder 'DB_PASSWORD'` 또는 `'JWT_SECRET'` | `backend/` 에 `application-local.yml` 이 없거나, `backend/` 가 아닌 곳에서 실행함 → [비밀 설정 파일 만들기](#3-비밀-설정-파일-만들기-️-필수) |
| `Access denied for user 'root'@'localhost'` | `application-local.yml` 의 `DB_PASSWORD` 확인 |
| `Unknown column 'RESERVED' in 'WHERE'` | 3306 포트에 MySQL 이 아닌 **MariaDB** 가 떠 있음 → MariaDB 서비스를 끄고 MySQL 8.0 을 3306 에서 실행 |
| 프론트에서 "서버에 연결할 수 없어요" | 백엔드(8080)가 실행 중인지 확인 |
| VS Code 에 Java 빨간 줄이 수백 개 | `Ctrl+Shift+P` → `Java: Clean Java Language Server Workspace` → `Restart and delete` (Maven 빌드는 정상인데 IDE 캐시만 오래된 경우) |

---

## 🔐 보안 메모

- 비밀번호는 BCrypt 로 암호화해 저장하고, 인증은 서버에 세션을 두지 않는 JWT(24시간) 방식입니다.
- `application-local.yml`, `.env` 는 커밋하지 마세요. 실수로 올렸다면 해당 비밀번호와 JWT 키를 **즉시 변경**하세요.
- 배포 전 체크: `DEMO_LOGIN_ENABLED=false`, `DEMO_SEED_ENABLED=false`, `ddl-auto: validate`
