# AURA Server

> AI-Powered Museum Docent Backend for Visually Impaired Users

**AURA Server**는 시각장애인을 위한 AI 기반 맞춤형 미술관 도슨트 시스템의 백엔드 서버입니다. Kotlin과 Ktor 프레임워크를 기반으로 구축되었으며, Google Gemini AI를 활용하여 사용자의 개인적 맥락에 최적화된 작품 해설을 생성합니다.

## 주요 기능

### 🎨 지능형 작품 검색 (Artwork API)
- **다중 모달 검색**: 텍스트, 음성, 이미지 기반 작품 검색
- **전문 검색**: PostgreSQL LIKE 연산 기반 고급 검색
- **유사 작품 추천**: 작가, 장르, 작품 유형 기반 추천
- **외부 API 통합**: Google Gemini, Vision AI OCR

### 🤖 AI 도슨트 생성 (Docent API)
- **개인화된 해설**: 사용자 맥락, 선호도, 감정 반영
- **Few-Shot Learning**: 고품질 피드백 기반 지속적 학습
- **다양한 스타일**: LITERARY, CONVERSATIONAL, POETIC, ANALYTICAL
- **TTS 통합**: Google Cloud TTS API로 오디오 생성
- **비동기 처리**: 폴링 방식의 비동기 도슨트 생성

### 💬 피드백 시스템 (Feedback API)
- **다차원 평가**: 공감도, 몰입도, 정서적 영향 측정
- **자동 Few-Shot 생성**: 고품질 피드백(≥4.0) 자동 학습
- **품질 관리**: 저품질 예시 자동 비활성화

### 🔗 리소스 관리 (Links & Memos API)
- **웹 리소스 저장**: 작품 관련 링크 관리
- **텍스트/음성 메모**: 사용자 감상 기록
- **Open Graph 메타데이터**: 자동 링크 정보 추출 (계획)

### 👥 동행자 관리 (Companion API)
- **초대 시스템**: 이메일 또는 초대 코드 기반
- **세밀한 권한 제어**: 컨텍스트, 히스토리, 메모 접근 권한
- **관계 유형**: 가족, 친구, 돌봄 제공자, 자원봉사자

### 🧠 Few-Shot Learning System
- **지능형 선택**: TF-IDF + MMR 알고리즘 기반 예시 선택
- **다차원 스코어링**: 품질(35%), 관련성(30%), 최신성(15%), 선호도(10%), 리소스(10%)
- **Redis 캐싱**: 90%+ 히트율, 10-100배 성능 향상
- **자동 품질 업데이트**: 피드백 기반 가중 평균 재계산

### ⚡ Redis 캐싱 레이어
- **캐시 우선 전략**: 응답 시간 <1ms
- **계층적 무효화**: 사용자/작품 단위 정확한 무효화
- **배치 동기화**: 사용 통계 10분마다 DB 동기화

## 기술 스택

- **프레임워크**: Ktor 3.3.0
- **언어**: Kotlin 2.2.20
- **데이터베이스**: PostgreSQL 42.7.7
- **ORM**: Exposed 0.61.0
- **캐싱**: Redis (Lettuce 6.3.0)
- **인증**: JWT + BCrypt
- **직렬화**: kotlinx.serialization 1.6.2
- **외부 API**:
  - Google Gemini API (AI 텍스트 생성)
  - Google Cloud TTS API (음성 합성)
  - Google Vision AI API (OCR)

## 시작하기

### 필요 조건

- JDK 17 이상
- PostgreSQL 12 이상
- Redis 6.0 이상 (선택 사항, 성능 향상용)
- Google Cloud API 키 (Gemini, TTS, Vision AI)

### 설치

1. 저장소 클론
```bash
git clone https://github.com/your-username/aura-server.git
cd aura-server
```

2. 환경 변수 설정
```bash
# .env 파일 생성
cp .env.example .env

# .env 파일 편집
DATABASE_URL=jdbc:postgresql://localhost:5432/aura_db
DATABASE_USER=your_username
DATABASE_PASSWORD=your_password

JWT_SECRET=your_jwt_secret_key

GEMINI_API_KEY=your_google_gemini_api_key
TTS_API_KEY=your_google_cloud_tts_api_key
VISION_API_KEY=your_google_vision_api_key

REDIS_URL=redis://localhost:6379  # 선택 사항
AUDIO_STORAGE_PATH=storage/audio
```

3. 데이터베이스 생성
```bash
createdb aura_db
```

4. 의존성 설치 및 빌드
```bash
./gradlew build
```

5. 서버 실행
```bash
./gradlew run
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

### Docker로 실행 (선택 사항)

```bash
# Docker 컨테이너 빌드 및 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f
```

## 프로젝트 구조

```
src/main/kotlin/
├── config/                    # 설정 파일
│   ├── RedisConfig.kt        # Redis 연결 설정
│   └── SecurityConfig.kt     # 보안 설정
├── db/                        # 데이터베이스
│   ├── DatabaseSchema.kt     # 테이블 정의
│   ├── DatabaseInitializer.kt # DB 초기화
│   └── CustomColumnTypes.kt  # PostgreSQL JSONB 타입
├── database/extraTables/      # Few-Shot 관련 테이블
│   ├── FewShotExamplesTable.kt
│   └── FewShotExampleResourcesTable.kt
├── model/dto/                 # 데이터 전송 객체
│   ├── ArtworkDTO.kt
│   ├── DocentDTO.kt
│   ├── FeedbackDTO.kt
│   ├── FewShotDTO.kt
│   ├── LinkDTO.kt
│   ├── MemoDTO.kt
│   └── CompanionDTO.kt
├── repository/                # 데이터 접근 계층
│   ├── ArtworkRepository.kt
│   ├── DocentSessionRepository.kt
│   ├── FeedbackRepository.kt
│   ├── FewShotRepository.kt
│   ├── LinkRepository.kt
│   ├── MemoRepository.kt
│   └── CompanionRepository.kt
├── services/                  # 비즈니스 로직
│   ├── ArtworkService.kt
│   ├── ArtworkSearchService.kt
│   ├── DocentService.kt
│   ├── GeminiService.kt
│   ├── TTSService.kt
│   ├── FeedbackService.kt
│   ├── FewShotBuilderService.kt
│   ├── FewShotSelectorService.kt
│   ├── RedisFewShotCacheService.kt
│   ├── LinkService.kt
│   ├── MemoService.kt
│   └── CompanionService.kt
├── routes/                    # API 라우트
│   ├── ArtworkRoutes.kt
│   ├── DocentRoutes.kt
│   ├── FeedbackRoutes.kt
│   ├── LinkRoutes.kt
│   ├── MemoRoutes.kt
│   └── CompanionRoutes.kt
├── utils/                     # 유틸리티
│   ├── PromptBuilder.kt      # AI 프롬프트 생성
│   └── JWTConfig.kt          # JWT 토큰 관리
└── Application.kt             # 앱 진입점

src/main/resources/
├── db/migration/              # SQL 마이그레이션
│   ├── V1__initial_schema.sql
│   └── V2__extend_few_shot_schema.sql
└── application.yaml           # 앱 설정
```

## API 엔드포인트

### 인증 (Authentication)
```
POST   /api/auth/register       # 회원가입
POST   /api/auth/login          # 로그인
POST   /api/auth/refresh        # 토큰 갱신
```

### 사용자 관리 (User Management)
```
POST   /api/users/{userId}/profile      # 프로필 생성
POST   /api/users/{userId}/contexts     # 컨텍스트 추가
POST   /api/users/{userId}/preferences  # 선호도 설정
GET    /api/users/me                    # 내 정보 조회
```

### 작품 (Artworks)
```
GET    /api/artworks                        # 작품 목록 (페이지네이션)
GET    /api/artworks/{artworkId}            # 작품 상세 조회
POST   /api/artworks/search                 # 텍스트 검색
POST   /api/artworks/search/voice           # 음성 검색 🔒
POST   /api/artworks/search/camera          # 이미지 검색 🔒
GET    /api/artworks/search/{sessionId}     # 이미지 검색 결과 조회
GET    /api/artworks/{artworkId}/similar    # 유사 작품 추천
```

### 도슨트 (Docent)
```
POST   /api/docent/generate                      # 도슨트 생성 🔒
GET    /api/docent/sessions/{sessionId}          # 생성 상태 조회
PUT    /api/docent/sessions/{sessionId}/play-stats  # 재생 통계 업데이트 🔒
GET    /api/users/me/docent/history              # 도슨트 히스토리 🔒
```

### 피드백 (Feedback)
```
POST   /api/docent/sessions/{sessionId}/feedback   # 피드백 제출 🔒
GET    /api/docent/sessions/{sessionId}/feedback   # 피드백 조회 🔒
GET    /api/admin/feedbacks                        # 전체 피드백 조회 (관리자)
```

### 링크 (Links)
```
POST   /api/users/{userId}/links              # 링크 추가 🔒
GET    /api/users/{userId}/links              # 링크 목록 조회 🔒
DELETE /api/users/{userId}/links/{linkId}     # 링크 삭제 🔒
```

### 메모 (Memos)
```
POST   /api/users/{userId}/memos              # 메모 생성 🔒
GET    /api/users/{userId}/memos              # 메모 목록 조회 🔒
PUT    /api/users/{userId}/memos/{memoId}     # 메모 수정 🔒
DELETE /api/users/{userId}/memos/{memoId}     # 메모 삭제 🔒
GET    /api/users/{userId}/memos/statistics   # 메모 통계 🔒
```

### 동행자 (Companions)
```
POST   /api/companions/invite                     # 동행자 초대 🔒
GET    /api/companions                            # 동행자 목록 조회 🔒
GET    /api/companions/invitations                # 대기 중인 초대 조회 🔒
PUT    /api/companions/{companionId}/permissions  # 권한 수정 🔒
PUT    /api/companions/{companionId}/accept       # 초대 수락 🔒
PUT    /api/companions/{companionId}/reject       # 초대 거절 🔒
DELETE /api/companions/{companionId}              # 동행자 제거 🔒
GET    /api/companions/statistics                 # 동행자 통계 🔒
POST   /api/companions/invite-code                # 초대 코드 생성 🔒
```

🔒 = 인증 필요

## 환경 변수 설정

### 필수 환경 변수

```bash
# 데이터베이스
DATABASE_URL=jdbc:postgresql://localhost:5432/aura_db
DATABASE_USER=your_username
DATABASE_PASSWORD=your_password

# JWT 인증
JWT_SECRET=your_jwt_secret_key_min_256_bits
JWT_ISSUER=aura-server
JWT_AUDIENCE=aura-client
JWT_REALM=aura

# Google Gemini API (AI 도슨트 생성)
GEMINI_API_KEY=your_google_gemini_api_key

# Google Cloud TTS API (텍스트-음성 변환)
TTS_API_KEY=your_google_cloud_tts_api_key

# Google Vision AI API (이미지 OCR)
VISION_API_KEY=your_google_vision_api_key
```

### 선택 환경 변수

```bash
# Redis (성능 향상)
REDIS_URL=redis://localhost:6379
# 인증이 필요한 경우:
# REDIS_URL=redis://:password@localhost:6379

# 오디오 저장 경로
AUDIO_STORAGE_PATH=storage/audio

# 서버 포트 (기본: 8080)
PORT=8080
```

## 데이터베이스 마이그레이션

### 자동 마이그레이션
서버 시작 시 `src/main/resources/db/migration/` 폴더의 SQL 파일을 자동으로 실행합니다.

```
V1__initial_schema.sql          # 초기 스키마 (사용자, 작품, 도슨트 등)
V2__extend_few_shot_schema.sql  # Few-Shot 학습 시스템 확장
```

### 수동 마이그레이션 (필요 시)
```bash
psql -U your_username -d aura_db -f src/main/resources/db/migration/V1__initial_schema.sql
psql -U your_username -d aura_db -f src/main/resources/db/migration/V2__extend_few_shot_schema.sql
```

### 데이터베이스 초기화 (개발 환경)
```kotlin
// Application.kt에서
DatabaseInitializer.seedSampleData()  // 샘플 데이터 삽입
DatabaseInitializer.cleanAllData()    // 전체 데이터 삭제
DatabaseInitializer.dropAllTables()   # 전체 테이블 삭제
```

## 테스트

### 전체 테스트 실행
```bash
./gradlew test
```

### 통합 테스트
```bash
./gradlew test --tests "com.kevin.AuraIntegrationTest"
```

### Few-Shot 시스템 테스트
```bash
./gradlew test --tests "com.kevin.FewShotSystemTest"
```

### 테스트 커버리지
```bash
./gradlew jacocoTestReport
# 결과: build/reports/jacoco/test/html/index.html
```

## 라이선스 정보

이 프로젝트는 다음 오픈 소스 라이브러리를 사용합니다:

### 핵심 프레임워크

| 패키지 | 버전 | 라이선스 | 용도 |
|---------|---------|---------|---------|
| `io.ktor:ktor-server-*` | 3.3.0 | Apache 2.0 | 웹 프레임워크 |
| `org.jetbrains.kotlin:kotlin-*` | 2.2.20 | Apache 2.0 | 프로그래밍 언어 |

### 데이터베이스 & ORM

| 패키지 | 버전 | 라이선스 | 용도 |
|---------|---------|---------|---------|
| `org.jetbrains.exposed:exposed-*` | 0.61.0 | Apache 2.0 | Kotlin SQL ORM |
| `org.postgresql:postgresql` | 42.7.7 | BSD-2-Clause | PostgreSQL JDBC 드라이버 |
| `com.h2database:h2` | 2.3.232 | MPL 2.0 / EPL 1.0 | H2 Database (테스트용) |

### 캐싱 & 직렬화

| 패키지 | 버전 | 라이선스 | 용도 |
|---------|---------|---------|---------|
| `io.lettuce:lettuce-core` | 6.3.0.RELEASE | Apache 2.0 | Redis 클라이언트 |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.6.2 | Apache 2.0 | JSON 직렬화 |

### 인증 & 보안

| 패키지 | 버전 | 라이선스 | 용도 |
|---------|---------|---------|---------|
| `io.ktor:ktor-server-auth-jwt` | 3.3.0 | Apache 2.0 | JWT 인증 |
| `at.favre.lib:bcrypt` | 0.10.2 | Apache 2.0 | 비밀번호 해싱 |

### 유틸리티

| 패키지 | 버전 | 라이선스 | 용도 |
|---------|---------|---------|---------|
| `io.github.cdimascio:dotenv-kotlin` | 6.4.1 | Apache 2.0 | 환경 변수 관리 |
| `ch.qos.logback:logback-classic` | 1.5.13 | EPL 1.0 / LGPL 2.1 | 로깅 |

### 테스트

| 패키지 | 버전 | 라이선스 | 용도 |
|---------|---------|---------|---------|
| `io.ktor:ktor-server-test-host` | 3.3.0 | Apache 2.0 | 서버 테스트 |
| `org.jetbrains.kotlin:kotlin-test-junit` | 2.2.20 | Apache 2.0 | JUnit 통합 |

### 외부 API

| 서비스 | 라이선스 | 용도 |
|---------|---------|---------|
| Google Gemini API | Google Cloud ToS | AI 텍스트 생성 |
| Google Cloud TTS API | Google Cloud ToS | 음성 합성 |
| Google Vision AI API | Google Cloud ToS | 이미지 OCR |

### 라이선스 준수 요약

이 프로젝트는 독점적으로 허용적 라이선스를 사용합니다:
- **Apache 2.0**: 대부분의 패키지 (Ktor, Kotlin, Exposed, Lettuce, BCrypt 등)
- **BSD-2-Clause**: PostgreSQL JDBC 드라이버
- **EPL 1.0 / LGPL 2.1**: Logback
- **MPL 2.0 / EPL 1.0**: H2 Database (테스트 전용)

모든 라이선스가 허용하는 것:
- ✅ 상업적 사용
- ✅ 수정
- ✅ 배포
- ✅ 개인적 사용

저작권 표시 요구사항:
- 배포 시 저작권 공지 포함
- Apache/BSD 패키지의 라이선스 텍스트 포함
- 소스 코드 배포 시 변경 사항 명시

## 기여하기

기여를 환영합니다! 기여하기 전에 다음을 확인해 주세요:

### 개발 워크플로우

```bash
# 1. 브랜치 생성
git checkout -b feature/your-feature-name

# 2. 코드 작성 및 테스트
./gradlew test

# 3. 린트 검사
./gradlew detekt  # Kotlin 코드 스타일 검사

# 4. 변경사항 커밋
git commit -m "feat: add your feature"

# 5. Push 및 Pull Request 생성
git push origin feature/your-feature-name
```

### 커밋 메시지 규칙

```
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
refactor: 코드 리팩토링
test: 테스트 추가/수정
chore: 빌드 설정 등 기타 변경
```

### 코딩 규칙

1. **Kotlin 코딩 컨벤션 준수**
   - [공식 Kotlin 스타일 가이드](https://kotlinlang.org/docs/coding-conventions.html) 참고

2. **명확한 네이밍**
   - Repository: `{Entity}Repository`
   - Service: `{Feature}Service`
   - DTO: `{Entity}Response`, `{Entity}Request`

3. **문서화**
   - 모든 public 함수에 KDoc 주석 추가
   - 복잡한 로직에 설명 주석

4. **에러 핸들링**
   - Graceful degradation 원칙
   - 상세한 에러 메시지
   - 로그 레벨 적절히 사용 (DEBUG, INFO, WARN, ERROR)

5. **테스트 작성**
   - 새 기능에 단위 테스트 필수
   - 통합 테스트 권장
   - 테스트 커버리지 70% 이상 유지

## 성능 최적화

### Redis 캐싱 전략
- **Few-Shot 풀**: 1시간 TTL, 90%+ 히트율
- **품질 인덱스**: 24시간 TTL, 콜드 스타트 방지
- **사용 통계**: 배치 동기화 (10분마다)

### 데이터베이스 인덱싱
```sql
-- 작품 검색 최적화
CREATE INDEX idx_artworks_title ON artworks(title);
CREATE INDEX idx_artworks_artist ON artworks(artist);

-- Few-Shot 조회 최적화
CREATE INDEX idx_few_shot_examples_quality_score ON few_shot_examples(quality_score);
CREATE INDEX idx_few_shot_examples_effectiveness_score ON few_shot_examples(effectiveness_score);
```

### 비동기 처리
- 도슨트 생성: 비동기 처리 (폴링)
- 이미지 검색: 비동기 처리 (폴링)
- 피드백 처리: Few-Shot 생성 백그라운드 실행

## 로드맵

### 현재 버전 (v1.0.0)
- ✅ 사용자 인증 및 관리
- ✅ 다중 모달 작품 검색
- ✅ AI 도슨트 생성 (Gemini API)
- ✅ Few-Shot Learning System
- ✅ Redis 캐싱 레이어
- ✅ 피드백 시스템
- ✅ 링크 & 메모 관리
- ✅ 동행자 관리

### 계획된 기능 (v2.0.0)
- 🔄 벡터 임베딩 기반 유사도 검색
- 🔄 실시간 스트리밍 도슨트 생성
- 🔄 다국어 지원 확대 (영어, 일본어 등)
- 🔄 WebSocket 기반 실시간 알림
- 🔄 분석 대시보드 (관리자)
- 🔄 자동 Few-Shot 품질 평가 ML 모델
- 🔄 GraphQL API 지원

### 미래 개선 사항 (v3.0.0)
- 🔮 다양한 LLM 모델 지원 (GPT-4, Claude 등)
- 🔮 작품 이미지 분석 (Computer Vision)
- 🔮 사용자 행동 예측 모델
- 🔮 오프라인 모드 지원 (경량 모델)
- 🔮 마이크로서비스 아키텍처 전환

## 모니터링 & 로깅

### 로그 레벨 설정
```yaml
# application.yaml
logger:
  root: INFO
  io.ktor: DEBUG
  com.kevin: DEBUG
```

### 주요 로그 위치
- 도슨트 생성: `DocentService` - 진행 상황 추적
- Few-Shot 선택: `FewShotSelectorService` - 캐시 히트/미스
- Redis 동기화: `RedisFewShotCacheService` - 배치 동기화 결과
- 에러 로그: `ERROR` 레벨, 스택 트레이스 포함

### 성능 메트릭
```bash
# Redis 캐시 통계 확인
curl http://localhost:8080/api/admin/cache/stats

# 응답 예시:
{
  "totalCached": 1523,
  "cacheHitRate": 92.3,
  "averageRetrievalTimeMs": 0.8,
  "topUsedFewShots": [...]
}
```

## 문제 해결

### 일반적인 문제

**1. 데이터베이스 연결 실패**
```
Error: Connection to localhost:5432 refused
```
- PostgreSQL 서버가 실행 중인지 확인: `pg_isready`
- `DATABASE_URL`이 올바른지 확인

**2. Gemini API 에러**
```
Error: 403 Forbidden - API key not valid
```
- `GEMINI_API_KEY` 환경 변수 확인
- API 키가 활성화되어 있는지 확인

**3. Redis 연결 실패**
```
Warning: Redis connection failed, continuing without cache
```
- Redis 서버 실행: `redis-server`
- 또는 `REDIS_URL` 환경 변수 제거 (캐싱 없이 실행)

**4. 테스트 타임아웃**
```
kotlinx.coroutines.test.UncompletedCoroutinesError: After waiting for 1m...
```
- 외부 API 호출로 인한 지연 (정상)
- 테스트 실행 시 `-x test` 옵션으로 스킵 가능: `./gradlew build -x test`

## 지원

문제가 발생하거나 기능 요청이 있으신 경우:
- [Issue 생성](https://github.com/kevinlim17/aura-server/issues)
- 이메일: kevinlim17@icloud.com

## 참고 문서

- [Ktor Documentation](https://ktor.io/docs/)
- [Exposed Wiki](https://github.com/JetBrains/Exposed/wiki)
- [Google Gemini API](https://ai.google.dev/gemini-api/docs)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/docs/)

## 감사의 말

- Google Gemini AI for powerful natural language generation
- Ktor community for excellent web framework
- JetBrains for Kotlin and Exposed ORM
- All contributors and testers

---

## MIT License

```
MIT License

Copyright (c) 2025 Seung-hyeon Lim

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

**Made with ❤️ for accessible museum experiences**