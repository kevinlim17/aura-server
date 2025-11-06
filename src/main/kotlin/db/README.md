# Aura Database Package

데이터베이스 관련 파일을 한 곳에 모아 관리하는 패키지입니다.

## 📁 패키지 구조

```
src/main/kotlin/db/
├── DatabaseSchema.kt      # 14개 테이블 정의 (완전한 스키마)
├── DatabaseConfig.kt      # 데이터베이스 연결 설정
├── DatabaseInitializer.kt # 마이그레이션 및 시딩
├── DatabaseTest.kt        # 테스트 유틸리티
└── README.md             # 이 문서
```

## 🗄️ 데이터베이스 스키마 (14개 테이블)

### 1. **users** - 사용자 계정
사용자 인증 및 기본 정보

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | INT | Primary Key |
| email | VARCHAR(255) | 이메일 (유니크) |
| password_hash | VARCHAR(255) | 암호화된 비밀번호 |
| user_type | VARCHAR(20) | 사용자 유형 (MAIN_USER/COMPANION) |
| is_visually_impaired | BOOLEAN | 시각장애 여부 |
| impairment_level | VARCHAR(20) | 장애 수준 (TOTAL_BLINDNESS/LOW_VISION) |
| is_active | BOOLEAN | 계정 활성화 여부 |
| is_onboarding_completed | BOOLEAN | 온보딩 완료 여부 |
| created_at | TIMESTAMP | 생성 시각 |
| updated_at | TIMESTAMP | 수정 시각 |
| last_login_at | TIMESTAMP | 마지막 로그인 시각 |

### 2. **user_profiles** - 사용자 프로필
사용자의 관심사, 취미, 선호 작가 등

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | INT | Primary Key |
| user_id | INT | Foreign Key → users (유니크) |
| interests | TEXT | 관심사 (JSON 배열) |
| hobbies | TEXT | 취미 |
| hobbies_voice_url | TEXT | 취미 음성 URL |
| favorite_artists | TEXT | 선호 작가 |
| bio | TEXT | 자기소개 |
| created_at | TIMESTAMP | 생성 시각 |
| updated_at | TIMESTAMP | 수정 시각 |

### 3. **user_contexts** - 사용자 컨텍스트
기억, 경험 등 개인적 맥락

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | INT | Primary Key |
| user_id | INT | Foreign Key → users |
| context_type | VARCHAR(50) | 컨텍스트 유형 (MEMORY/EXPERIENCE) |
| title | VARCHAR(255) | 제목 |
| content | TEXT | 내용 |
| voice_url | TEXT | 음성 녹음 URL |
| voice_duration_seconds | INT | 음성 길이 (초) |
| input_method | VARCHAR(20) | 입력 방법 (TEXT/VOICE) |
| emotion_tags | TEXT | 감정 태그 |
| importance_level | INT | 중요도 (1-5) |
| is_companion_input | BOOLEAN | 보호자 입력 여부 |
| companion_user_id | INT | Foreign Key → users (보호자) |
| created_at | TIMESTAMP | 생성 시각 |
| updated_at | TIMESTAMP | 수정 시각 |

### 4. **user_preferences** - 사용자 설정
도슨트 스타일, TTS, 접근성 설정

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | INT | Primary Key |
| user_id | INT | Foreign Key → users (유니크) |
| narrative_style | VARCHAR(50) | 해설 스타일 (LITERARY/CASUAL/etc) |
| preferred_length | VARCHAR(20) | 선호 길이 (SHORT/MEDIUM/LONG) |
| tts_speed | DECIMAL(3,1) | TTS 속도 |
| tts_pitch | DECIMAL(3,1) | TTS 음높이 |
| tts_voice | VARCHAR(50) | TTS 음성 |
| preferred_language | VARCHAR(10) | 선호 언어 |
| enable_haptic_feedback | BOOLEAN | 햅틱 피드백 활성화 |
| enable_audio_descriptions | BOOLEAN | 오디오 설명 활성화 |
| high_contrast_mode | BOOLEAN | 고대비 모드 |
| enable_push_notifications | BOOLEAN | 푸시 알림 활성화 |
| created_at | TIMESTAMP | 생성 시각 |
| updated_at | TIMESTAMP | 수정 시각 |

### 5. **artworks** - 작품 정보
미술 작품 메타데이터

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | INT | Primary Key |
| title | VARCHAR(500) | 작품명 (한글) |
| title_en | VARCHAR(500) | 작품명 (영문) |
| artist | VARCHAR(255) | 작가명 (한글) |
| artist_en | VARCHAR(255) | 작가명 (영문) |
| artwork_type | VARCHAR(50) | 작품 유형 (PAINTING/SCULPTURE/etc) |
| genre | VARCHAR(100) | 장르 |
| creation_year | INT | 제작 연도 |
| creation_period | VARCHAR(100) | 제작 시기 |
| medium | VARCHAR(255) | 재료/기법 |
| dimensions | VARCHAR(100) | 크기 |
| museum | VARCHAR(255) | 소장 미술관 |
| museum_en | VARCHAR(255) | 소장 미술관 (영문) |
| museum_location | VARCHAR(255) | 미술관 위치 |
| current_location | VARCHAR(255) | 현재 위치 |
| image_url | TEXT | 이미지 URL |
| thumbnail_url | TEXT | 썸네일 URL |
| high_res_url | TEXT | 고해상도 이미지 URL |
| description | TEXT | 설명 |
| historical_context | TEXT | 역사적 맥락 |
| metadata | TEXT | 메타데이터 (JSON) |
| wikipedia_url | TEXT | 위키피디아 링크 |
| museum_website_url | TEXT | 미술관 웹사이트 |
| view_count | INT | 조회수 |
| docent_generation_count | INT | 도슨트 생성 횟수 |
| average_rating | DECIMAL(3,2) | 평균 평점 |
| created_at | TIMESTAMP | 생성 시각 |
| updated_at | TIMESTAMP | 수정 시각 |

### 6. **artwork_searches** - 작품 검색 기록
작품 검색 방법 및 결과

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | INT | Primary Key |
| user_id | INT | Foreign Key → users |
| search_method | VARCHAR(20) | 검색 방법 (TEXT/VOICE/CAMERA/LOCATION) |
| search_query | TEXT | 검색 쿼리 |
| search_query_voice_url | TEXT | 음성 검색 URL |
| captured_image_url | TEXT | 캡처 이미지 URL |
| latitude | DECIMAL(10,8) | 위도 |
| longitude | DECIMAL(11,8) | 경도 |
| location_name | VARCHAR(255) | 위치명 |
| results_count | INT | 결과 수 |
| selected_artwork_id | INT | Foreign Key → artworks |
| created_at | TIMESTAMP | 생성 시각 |

### 7. **docent_sessions** - 도슨트 생성 세션
AI 도슨트 생성 및 재생 이력

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | INT | Primary Key |
| user_id | INT | Foreign Key → users |
| artwork_id | INT | Foreign Key → artworks |
| prompt_template | TEXT | 프롬프트 템플릿 |
| prompt_persona | TEXT | 프롬프트 페르소나 |
| prompt_task | TEXT | 프롬프트 작업 |
| prompt_context | TEXT | 프롬프트 컨텍스트 |
| prompt_form | TEXT | 프롬프트 형식 |
| few_shot_examples | TEXT | Few-shot 예시 (JSON) |
| generated_text | TEXT | 생성된 도슨트 텍스트 |
| gemini_model | VARCHAR(50) | Gemini 모델명 |
| gemini_temperature | DECIMAL(3,2) | 온도 설정 |
| gemini_top_p | DECIMAL(3,2) | Top-p 설정 |
| gemini_top_k | INT | Top-k 설정 |
| generation_time_ms | INT | 생성 소요 시간 (밀리초) |
| tts_audio_url | TEXT | TTS 오디오 URL |
| tts_duration_seconds | INT | TTS 길이 (초) |
| play_count | INT | 재생 횟수 |
| total_listening_seconds | INT | 총 청취 시간 |
| completion_rate | DECIMAL(5,2) | 완료율 (%) |
| status | VARCHAR(20) | 상태 (COMPLETED/GENERATING/FAILED) |
| created_at | TIMESTAMP | 생성 시각 |
| last_played_at | TIMESTAMP | 마지막 재생 시각 |

### 8. **docent_feedbacks** - 도슨트 피드백
사용자 피드백 및 평가

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | INT | Primary Key |
| docent_session_id | INT | Foreign Key → docent_sessions |
| user_id | INT | Foreign Key → users |
| emotional_resonance | INT | 개인적 공감도 (1-5) |
| imaginative_engagement | INT | 상상적 몰입도 (1-5) |
| emotional_impact | INT | 정서적 영향 (1-5) |
| overall_satisfaction | DECIMAL(3,2) | 전체 만족도 |
| comment | TEXT | 피드백 텍스트 |
| improvement_suggestions | TEXT | 개선 제안 (JSON) |
| is_few_shot_candidate | BOOLEAN | Few-shot 후보 여부 |
| few_shot_selected_at | TIMESTAMP | Few-shot 선정 시각 |
| created_at | TIMESTAMP | 생성 시각 |

### 9. **user_links** - 사용자 링크
작품 관련 외부 링크

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | INT | Primary Key |
| user_id | INT | Foreign Key → users |
| artwork_id | INT | Foreign Key → artworks |
| docent_session_id | INT | Foreign Key → docent_sessions |
| url | TEXT | URL |
| title | VARCHAR(500) | 제목 |
| description | TEXT | 설명 |
| link_type | VARCHAR(50) | 링크 유형 |
| metadata | TEXT | 메타데이터 (JSON) |
| thumbnail_url | TEXT | 썸네일 URL |
| has_audio_description | BOOLEAN | 오디오 설명 포함 |
| has_subtitles | BOOLEAN | 자막 포함 |
| created_at | TIMESTAMP | 생성 시각 |

### 10. **user_memos** - 사용자 메모
작품/세션 관련 메모

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | INT | Primary Key |
| user_id | INT | Foreign Key → users |
| artwork_id | INT | Foreign Key → artworks |
| docent_session_id | INT | Foreign Key → docent_sessions |
| content | TEXT | 메모 내용 |
| voice_url | TEXT | 음성 메모 URL |
| voice_duration_seconds | INT | 음성 길이 (초) |
| input_method | VARCHAR(20) | 입력 방법 (TEXT/VOICE) |
| tags | TEXT | 태그 |
| category | VARCHAR(50) | 카테고리 |
| is_shared_with_companion | BOOLEAN | 보호자와 공유 여부 |
| created_at | TIMESTAMP | 생성 시각 |
| updated_at | TIMESTAMP | 수정 시각 |

### 11. **voice_recordings** - 음성 녹음
모든 음성 녹음 파일 관리

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | INT | Primary Key |
| user_id | INT | Foreign Key → users |
| file_url | TEXT | 파일 URL |
| file_size_bytes | BIGINT | 파일 크기 (바이트) |
| file_format | VARCHAR(20) | 파일 형식 (mp3/wav/etc) |
| duration_seconds | INT | 길이 (초) |
| sample_rate | INT | 샘플링 레이트 |
| bit_rate | INT | 비트레이트 |
| transcription | TEXT | 텍스트 변환 결과 |
| transcription_confidence | DECIMAL(5,4) | 변환 신뢰도 |
| transcription_language | VARCHAR(10) | 변환 언어 |
| related_entity_type | VARCHAR(50) | 관련 엔티티 유형 |
| related_entity_id | INT | 관련 엔티티 ID |
| processing_status | VARCHAR(20) | 처리 상태 |
| created_at | TIMESTAMP | 생성 시각 |
| processed_at | TIMESTAMP | 처리 완료 시각 |

### 12. **few_shot_examples** - Few-shot 예시
고품질 도슨트 예시 저장

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | INT | Primary Key |
| artwork_id | INT | Foreign Key → artworks |
| user_context_summary | TEXT | 사용자 컨텍스트 요약 |
| exemplar_text | TEXT | 예시 텍스트 |
| source_docent_session_id | INT | Foreign Key → docent_sessions |
| source_feedback_id | INT | Foreign Key → docent_feedbacks |
| quality_score | DECIMAL(3,2) | 품질 점수 |
| user_rating | INT | 사용자 평점 |
| usage_count | INT | 사용 횟수 |
| last_used_at | TIMESTAMP | 마지막 사용 시각 |
| category | VARCHAR(50) | 카테고리 |
| is_active | BOOLEAN | 활성화 여부 |
| created_at | TIMESTAMP | 생성 시각 |
| updated_at | TIMESTAMP | 수정 시각 |

### 13. **user_companions** - 사용자-보호자 관계
시각장애인과 보호자 연결

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | INT | Primary Key |
| main_user_id | INT | Foreign Key → users (시각장애인) |
| companion_user_id | INT | Foreign Key → users (보호자) |
| relationship_type | VARCHAR(50) | 관계 유형 (FAMILY/FRIEND/etc) |
| can_add_context | BOOLEAN | 컨텍스트 추가 권한 |
| can_view_history | BOOLEAN | 히스토리 조회 권한 |
| can_add_memos | BOOLEAN | 메모 추가 권한 |
| status | VARCHAR(20) | 상태 (ACTIVE/INACTIVE) |
| created_at | TIMESTAMP | 생성 시각 |
| updated_at | TIMESTAMP | 수정 시각 |

### 14. **schema_migrations** - 스키마 마이그레이션
데이터베이스 마이그레이션 기록

| 컬럼 | 타입 | 설명 |
|------|------|------|
| version | VARCHAR(50) | 마이그레이션 버전 (Primary Key) |
| description | TEXT | 설명 |
| applied_at | TIMESTAMP | 적용 시각 |

## 🔧 사용 방법

### 1. 데이터베이스 연결

```kotlin
// DatabaseConfig를 통한 연결
val db = DatabaseConfig.connect()

// 연결 정보 확인
val info = DatabaseConfig.getConnectionInfo()
println(info)
```

### 2. 테이블 초기화

```kotlin
// 테이블 생성
DatabaseInitializer.initializeTables(db)

// 테이블 삭제
DatabaseInitializer.dropAllTables(db)

// 데이터베이스 리셋 (삭제 후 재생성)
DatabaseInitializer.resetDatabase(db)
```

### 3. 샘플 데이터 시딩

```kotlin
// 기본 샘플 데이터
DatabaseInitializer.seedSampleData(db)

// 추가 작품 데이터
DatabaseInitializer.seedArtworks(db)

// 데이터 정리
DatabaseInitializer.cleanAllData(db)
```

### 4. 테스트 실행

```bash
# 데이터베이스 테스트 실행
./gradlew run -PmainClass=com.kevin.db.DatabaseTestKt
```

또는 코드에서:

```kotlin
// 연결 및 스키마 테스트
DatabaseTest.testConnection()

// 샘플 데이터로 테스트
DatabaseTest.testWithSampleData()

// 테스트 데이터 정리
DatabaseTest.cleanTestData()
```

## 🚀 애플리케이션 실행

### 1. 환경 변수 설정 (.env 파일)

```bash
DB_URL=jdbc:postgresql://localhost:5432/aura_db
DB_DRIVER=org.postgresql.Driver
DB_USER=seunghyeonlim
DB_PASSWORD=101723
DB_MAX_POOL_SIZE=10
```

### 2. 애플리케이션 실행

```bash
./gradlew run
```

실행 시 자동으로:
- `.env` 파일에서 설정 로드
- PostgreSQL 데이터베이스 연결
- 14개 테이블 생성 (없는 경우)
- 서버 시작 (포트 8080)

### 3. API 엔드포인트 확인

```bash
# 헬스 체크
curl http://localhost:8080/health

# 데이터베이스 정보
curl http://localhost:8080/db-info

# 데이터베이스 통계
curl http://localhost:8080/db-stats
```

## 📊 ERD (Entity Relationship Diagram)

```
┌──────────────┐
│    users     │
└──────┬───────┘
       │
       ├─────► user_contexts (1:N)
       │
       ├─────► docent_history (1:N)
       │              │
       │              │ (N:1)
       │              ▼
       │       ┌──────────────┐
       │       │   artworks   │
       │       └──────────────┘
       │
       └─────► feedback_ratings (1:N)
                      │
                      │ (N:1)
                      ▼
               docent_history
```

## 🔍 쿼리 예시

### 사용자 컨텍스트 조회

```kotlin
transaction(db) {
    UserContexts
        .select { UserContexts.userId eq 1 }
        .forEach { row ->
            println("${row[UserContexts.contextType]}: ${row[UserContexts.content]}")
        }
}
```

### 작품 검색

```kotlin
transaction(db) {
    Artworks
        .select { Artworks.title like "%별이 빛나는%" }
        .forEach { row ->
            println("${row[Artworks.title]} - ${row[Artworks.artist]}")
        }
}
```

### 도슨트 세션 생성

```kotlin
transaction(db) {
    DocentSessions.insert {
        it[userId] = 1
        it[artworkId] = 1
        it[promptTemplate] = "..."
        it[generatedText] = "..."
        it[geminiModel] = "gemini-2.0-flash-exp"
        it[status] = "COMPLETED"
    }
}
```

## 🧪 테스트 가이드

### 단위 테스트 작성

```kotlin
@Test
fun testUserCreation() {
    val db = DatabaseConfig.connectH2()
    DatabaseInitializer.initializeTables(db)

    transaction(db) {
        val userId = Users.insert {
            it[email] = "test@example.com"
            it[passwordHash] = "hash"
        } get Users.id

        assertNotNull(userId)
    }
}
```

### 통합 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트만 실행
./gradlew test --tests "DatabaseTest"
```

## 📝 개발 가이드라인

### 1. 새 테이블 추가 시

1. `DatabaseSchema.kt`에 테이블 정의 추가
2. `DatabaseInitializer.kt`의 `initializeTables()`에 테이블 추가
3. `DatabaseTest.kt`에 테스트 코드 추가
4. 이 README 업데이트

### 2. 마이그레이션

현재는 단순 `SchemaUtils.create()`를 사용합니다.
프로덕션 환경에서는 Flyway나 Liquibase 같은 마이그레이션 도구 사용을 권장합니다.

### 3. 보안 주의사항

- ✅ `.env` 파일은 절대 커밋하지 않기
- ✅ 비밀번호는 반드시 해시화하여 저장
- ✅ SQL Injection 방지 (Exposed ORM 자동 처리)
- ✅ 프로덕션에서는 SSL 연결 사용

## 🐛 트러블슈팅

### 연결 실패

```
Failed to connect to db
```

**해결:**
- PostgreSQL이 실행 중인지 확인
- `.env` 파일의 자격 증명 확인
- 데이터베이스가 생성되어 있는지 확인: `createdb aura_db`

### 테이블 생성 실패

```
Failed to initialize db schema
```

**해결:**
- 데이터베이스 권한 확인
- 기존 테이블과 충돌 확인
- 로그 확인: `application.log`

### 마이그레이션 충돌

```
Table already exists
```

**해결:**
```kotlin
// 데이터베이스 리셋
DatabaseInitializer.resetDatabase(db)
```

## 📚 참고 자료

- [Exposed ORM Documentation](https://github.com/JetBrains/Exposed)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Ktor Database Documentation](https://ktor.io/docs/db.html)
- 프로젝트 스키마: `project_schema/AURA_DATABASE_SCHEMA.md`

## 🎯 다음 단계

- [ ] 서비스 레이어 구현 (CRUD operations)
- [ ] API 엔드포인트 추가
- [ ] 인증/인가 미들웨어
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성
- [ ] API 문서화 (Swagger/OpenAPI)