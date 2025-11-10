-- Aura Project - PostgreSQL Database Schema
-- Generated from: AURA_DATABASE_SCHEMA.md
-- This file contains the complete db schema in raw SQL format

-- Enable UUID extension if needed
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- 1. USERS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    -- User type (main user or companion)
    user_type VARCHAR(20) DEFAULT 'MAIN_USER', -- 'MAIN_USER', 'COMPANION'

    -- Accessibility settings
    is_visually_impaired BOOLEAN DEFAULT true,
    impairment_level VARCHAR(20), -- 'TOTAL_BLINDNESS', 'LOW_VISION'

    -- Account status
    is_active BOOLEAN DEFAULT true,
    is_onboarding_completed BOOLEAN DEFAULT false,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,

    -- Constraints
    CONSTRAINT email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);

-- ============================================================================
-- 2. USER PROFILES TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_profiles (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Interests (JSON array)
    interests JSONB DEFAULT '[]'::jsonb,

    -- Hobbies
    hobbies TEXT,
    hobbies_voice_url TEXT,

    -- Favorite artists
    favorite_artists TEXT,

    -- Additional information
    bio TEXT,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    UNIQUE(user_id)
);

CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX idx_user_profiles_interests ON user_profiles USING gin(interests);

-- ============================================================================
-- 3. USER CONTEXTS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_contexts (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Context type
    context_type VARCHAR(50) NOT NULL,

    -- Context title
    title VARCHAR(255),

    -- Content
    content TEXT NOT NULL,

    -- Voice recording
    voice_url TEXT,
    voice_duration_seconds INTEGER,

    -- Input method
    input_method VARCHAR(20) NOT NULL DEFAULT 'TEXT',

    -- Emotion tags
    emotion_tags TEXT,

    -- Importance level
    importance_level INTEGER CHECK (importance_level >= 1 AND importance_level <= 5),

    -- Companion input
    is_companion_input BOOLEAN DEFAULT false,
    companion_user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_contexts_user_id ON user_contexts(user_id);
CREATE INDEX idx_user_contexts_type ON user_contexts(context_type);
CREATE INDEX idx_user_contexts_created_at ON user_contexts(created_at);

-- ============================================================================
-- 4. USER PREFERENCES TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_preferences (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Preferred commentary style
    narrative_style VARCHAR(50) NOT NULL DEFAULT 'LITERARY',

    -- Preferred length
    preferred_length VARCHAR(20) DEFAULT 'MEDIUM',

    -- TTS settings
    tts_speed DECIMAL(3,1) DEFAULT 1.0 CHECK (tts_speed >= 0.5 AND tts_speed <= 2.0),
    tts_pitch DECIMAL(3,1) DEFAULT 1.0 CHECK (tts_pitch >= 0.5 AND tts_pitch <= 2.0),
    tts_voice VARCHAR(50) DEFAULT 'default',

    -- Language setting
    preferred_language VARCHAR(10) DEFAULT 'ko-KR',

    -- Accessibility settings
    enable_haptic_feedback BOOLEAN DEFAULT true,
    enable_audio_descriptions BOOLEAN DEFAULT true,
    high_contrast_mode BOOLEAN DEFAULT false,

    -- Notification settings
    enable_push_notifications BOOLEAN DEFAULT true,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    UNIQUE(user_id)
);

CREATE INDEX idx_user_preferences_user_id ON user_preferences(user_id);

-- ============================================================================
-- 5. ARTWORKS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS artworks (
    id SERIAL PRIMARY KEY,

    -- Basic artwork information
    title VARCHAR(500) NOT NULL,
    title_en VARCHAR(500),
    artist VARCHAR(255) NOT NULL,
    artist_en VARCHAR(255),

    -- Artwork classification
    artwork_type VARCHAR(50),
    genre VARCHAR(100),

    -- Artwork details
    creation_year INTEGER,
    creation_period VARCHAR(100),
    medium VARCHAR(255),
    dimensions VARCHAR(100),

    -- Collection information
    museum VARCHAR(255),
    museum_en VARCHAR(255),
    museum_location VARCHAR(255),
    current_location VARCHAR(255),

    -- Images
    image_url TEXT NOT NULL,
    thumbnail_url TEXT,
    high_res_url TEXT,

    -- Description
    description TEXT,
    historical_context TEXT,

    -- Metadata
    metadata JSONB DEFAULT '{}'::jsonb,

    -- External links
    wikipedia_url TEXT,
    museum_website_url TEXT,

    -- Search optimization
    search_vector tsvector,

    -- Statistics
    view_count INTEGER DEFAULT 0,
    docent_generation_count INTEGER DEFAULT 0,
    average_rating DECIMAL(3,2),

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_artworks_artist ON artworks(artist);
CREATE INDEX idx_artworks_museum ON artworks(museum);
CREATE INDEX idx_artworks_creation_year ON artworks(creation_year);
CREATE INDEX idx_artworks_search_vector ON artworks USING gin(search_vector);

-- Full-text search update trigger
CREATE OR REPLACE FUNCTION artworks_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('korean', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('korean', COALESCE(NEW.artist, '')), 'A') ||
        setweight(to_tsvector('korean', COALESCE(NEW.description, '')), 'B');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER artworks_search_vector_trigger
    BEFORE INSERT OR UPDATE ON artworks
    FOR EACH ROW EXECUTE FUNCTION artworks_search_vector_update();

-- ============================================================================
-- 6. ARTWORK SEARCHES TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS artwork_searches (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Search method
    search_method VARCHAR(20) NOT NULL,

    -- Search query
    search_query TEXT,
    search_query_voice_url TEXT,

    -- Camera capture
    captured_image_url TEXT,

    -- Location-based search
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    location_name VARCHAR(255),

    -- Search results
    results_count INTEGER DEFAULT 0,
    selected_artwork_id INTEGER REFERENCES artworks(id) ON DELETE SET NULL,

    -- Timestamp
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_artwork_searches_user_id ON artwork_searches(user_id);
CREATE INDEX idx_artwork_searches_method ON artwork_searches(search_method);
CREATE INDEX idx_artwork_searches_created_at ON artwork_searches(created_at);

-- ============================================================================
-- 7. DOCENT SESSIONS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS docent_sessions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    artwork_id INTEGER NOT NULL REFERENCES artworks(id) ON DELETE CASCADE,

    -- Prompt information
    prompt_template TEXT NOT NULL,
    prompt_persona TEXT,
    prompt_task TEXT,
    prompt_context TEXT,
    prompt_form TEXT,

    -- Few-shot examples
    few_shot_examples JSONB DEFAULT '[]'::jsonb,

    -- Generated docent text
    generated_text TEXT NOT NULL,

    -- Gemini API response metadata
    gemini_model VARCHAR(50),
    gemini_temperature DECIMAL(3,2),
    gemini_top_p DECIMAL(3,2),
    gemini_top_k INTEGER,
    generation_time_ms INTEGER,

    -- TTS audio
    tts_audio_url TEXT,
    tts_duration_seconds INTEGER,

    -- Playback statistics
    play_count INTEGER DEFAULT 0,
    total_listening_seconds INTEGER DEFAULT 0,
    completion_rate DECIMAL(5,2),

    -- Status
    status VARCHAR(20) DEFAULT 'COMPLETED',

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_played_at TIMESTAMP
);

CREATE INDEX idx_docent_sessions_user_id ON docent_sessions(user_id);
CREATE INDEX idx_docent_sessions_artwork_id ON docent_sessions(artwork_id);
CREATE INDEX idx_docent_sessions_created_at ON docent_sessions(created_at);
CREATE INDEX idx_docent_sessions_status ON docent_sessions(status);

-- ============================================================================
-- 8. DOCENT FEEDBACKS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS docent_feedbacks (
    id SERIAL PRIMARY KEY,
    docent_session_id INTEGER NOT NULL REFERENCES docent_sessions(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Ratings (1-5)
    emotional_resonance INTEGER NOT NULL CHECK (emotional_resonance >= 1 AND emotional_resonance <= 5),
    imaginative_engagement INTEGER CHECK (imaginative_engagement >= 1 AND imaginative_engagement <= 5),
    emotional_impact INTEGER CHECK (emotional_impact >= 1 AND emotional_impact <= 5),

    -- Overall satisfaction (auto-calculated)
    overall_satisfaction DECIMAL(3,2),

    -- Text feedback
    comment TEXT,

    -- Improvement suggestions
    improvement_suggestions JSONB DEFAULT '{}'::jsonb,

    -- Few-shot learning candidate
    is_few_shot_candidate BOOLEAN DEFAULT false,
    few_shot_selected_at TIMESTAMP,

    -- Timestamp
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_docent_feedbacks_session_id ON docent_feedbacks(docent_session_id);
CREATE INDEX idx_docent_feedbacks_user_id ON docent_feedbacks(user_id);
CREATE INDEX idx_docent_feedbacks_few_shot ON docent_feedbacks(is_few_shot_candidate) WHERE is_few_shot_candidate = true;

-- Auto-calculate overall satisfaction
CREATE OR REPLACE FUNCTION calculate_overall_satisfaction() RETURNS trigger AS $$
BEGIN
    NEW.overall_satisfaction := (
        COALESCE(NEW.emotional_resonance, 0) +
        COALESCE(NEW.imaginative_engagement, 0) +
        COALESCE(NEW.emotional_impact, 0)
    ) / 3.0;
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER calculate_overall_satisfaction_trigger
    BEFORE INSERT OR UPDATE ON docent_feedbacks
    FOR EACH ROW EXECUTE FUNCTION calculate_overall_satisfaction();

-- ============================================================================
-- 9. USER LINKS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_links (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Connected entities
    artwork_id INTEGER REFERENCES artworks(id) ON DELETE CASCADE,
    docent_session_id INTEGER REFERENCES docent_sessions(id) ON DELETE CASCADE,

    -- Link information
    url TEXT NOT NULL,
    title VARCHAR(500),
    description TEXT,

    -- Link type
    link_type VARCHAR(50),

    -- Metadata
    metadata JSONB DEFAULT '{}'::jsonb,

    -- Thumbnail
    thumbnail_url TEXT,

    -- Accessibility
    has_audio_description BOOLEAN DEFAULT false,
    has_subtitles BOOLEAN DEFAULT false,

    -- Timestamp
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_url CHECK (url ~* '^https?://')
);

CREATE INDEX idx_user_links_user_id ON user_links(user_id);
CREATE INDEX idx_user_links_artwork_id ON user_links(artwork_id);
CREATE INDEX idx_user_links_session_id ON user_links(docent_session_id);

-- ============================================================================
-- 10. USER MEMOS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_memos (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Connected entities
    artwork_id INTEGER REFERENCES artworks(id) ON DELETE CASCADE,
    docent_session_id INTEGER REFERENCES docent_sessions(id) ON DELETE CASCADE,

    -- Memo content
    content TEXT NOT NULL,

    -- Voice memo
    voice_url TEXT,
    voice_duration_seconds INTEGER,

    -- Input method
    input_method VARCHAR(20) NOT NULL DEFAULT 'TEXT',

    -- Tags
    tags TEXT,

    -- Memo category
    category VARCHAR(50),

    -- Sharing with companion
    is_shared_with_companion BOOLEAN DEFAULT false,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_memos_user_id ON user_memos(user_id);
CREATE INDEX idx_user_memos_artwork_id ON user_memos(artwork_id);
CREATE INDEX idx_user_memos_session_id ON user_memos(docent_session_id);

-- ============================================================================
-- 11. VOICE RECORDINGS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS voice_recordings (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- File information
    file_url TEXT NOT NULL,
    file_size_bytes BIGINT,
    file_format VARCHAR(20) DEFAULT 'mp3',

    -- Recording information
    duration_seconds INTEGER NOT NULL,
    sample_rate INTEGER,
    bit_rate INTEGER,

    -- Transcription
    transcription TEXT,
    transcription_confidence DECIMAL(5,4),
    transcription_language VARCHAR(10) DEFAULT 'ko-KR',

    -- Related entity (polymorphic)
    related_entity_type VARCHAR(50),
    related_entity_id INTEGER,

    -- Processing status
    processing_status VARCHAR(20) DEFAULT 'UPLOADED',

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

CREATE INDEX idx_voice_recordings_user_id ON voice_recordings(user_id);
CREATE INDEX idx_voice_recordings_entity ON voice_recordings(related_entity_type, related_entity_id);
CREATE INDEX idx_voice_recordings_status ON voice_recordings(processing_status);

-- ============================================================================
-- 12. FEW-SHOT EXAMPLES TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS few_shot_examples (
    id SERIAL PRIMARY KEY,

    -- Few-shot example information
    artwork_id INTEGER NOT NULL REFERENCES artworks(id) ON DELETE CASCADE,
    user_context_summary TEXT NOT NULL,

    -- Exemplar text
    exemplar_text TEXT NOT NULL,

    -- Original session
    source_docent_session_id INTEGER REFERENCES docent_sessions(id),
    source_feedback_id INTEGER REFERENCES docent_feedbacks(id),

    -- Quality metrics
    quality_score DECIMAL(3,2) NOT NULL,
    user_rating INTEGER CHECK (user_rating >= 1 AND user_rating <= 5),

    -- Usage statistics
    usage_count INTEGER DEFAULT 0,
    last_used_at TIMESTAMP,

    -- Category
    category VARCHAR(50),

    -- Active status
    is_active BOOLEAN DEFAULT true,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_few_shot_examples_artwork_id ON few_shot_examples(artwork_id);
CREATE INDEX idx_few_shot_examples_quality ON few_shot_examples(quality_score);
CREATE INDEX idx_few_shot_examples_active ON few_shot_examples(is_active) WHERE is_active = true;

-- ============================================================================
-- 13. USER COMPANIONS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_companions (
    id SERIAL PRIMARY KEY,

    -- Main user (visually impaired)
    main_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Companion
    companion_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Relationship type
    relationship_type VARCHAR(50),

    -- Permission settings
    can_add_context BOOLEAN DEFAULT true,
    can_view_history BOOLEAN DEFAULT true,
    can_add_memos BOOLEAN DEFAULT true,

    -- Status
    status VARCHAR(20) DEFAULT 'ACTIVE',

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    UNIQUE(main_user_id, companion_user_id)
);

CREATE INDEX idx_user_companions_main_user ON user_companions(main_user_id);
CREATE INDEX idx_user_companions_companion ON user_companions(companion_user_id);

-- ============================================================================
-- VIEWS
-- ============================================================================

-- User complete profiles view
CREATE OR REPLACE VIEW user_complete_profiles AS
SELECT
    u.id as user_id,
    u.email,
    u.is_visually_impaired,
    u.impairment_level,
    u.is_onboarding_completed,

    up.interests,
    up.hobbies,

    upref.narrative_style,
    upref.preferred_length,
    upref.tts_speed,

    COUNT(DISTINCT uc.id) as context_count,
    COUNT(DISTINCT ds.id) as docent_session_count,

    u.created_at,
    u.last_login_at
FROM users u
LEFT JOIN user_profiles up ON u.id = up.user_id
LEFT JOIN user_preferences upref ON u.id = upref.user_id
LEFT JOIN user_contexts uc ON u.id = uc.user_id
LEFT JOIN docent_sessions ds ON u.id = ds.user_id
GROUP BY u.id, u.email, u.is_visually_impaired, u.impairment_level,
         u.is_onboarding_completed, up.interests, up.hobbies,
         upref.narrative_style, upref.preferred_length, upref.tts_speed,
         u.created_at, u.last_login_at;

-- Artwork statistics view
CREATE OR REPLACE VIEW artwork_statistics AS
SELECT
    a.id as artwork_id,
    a.title,
    a.artist,
    a.museum,

    COUNT(DISTINCT ds.id) as total_docent_sessions,
    COUNT(DISTINCT df.id) as total_feedbacks,
    AVG(df.overall_satisfaction) as avg_satisfaction,

    COUNT(DISTINCT CASE WHEN df.is_few_shot_candidate THEN df.id END) as few_shot_candidate_count,

    a.view_count,
    a.created_at
FROM artworks a
LEFT JOIN docent_sessions ds ON a.id = ds.artwork_id
LEFT JOIN docent_feedbacks df ON ds.id = df.docent_session_id
GROUP BY a.id, a.title, a.artist, a.museum, a.view_count, a.created_at;

-- User engagement metrics view
CREATE OR REPLACE VIEW user_engagement_metrics AS
SELECT
    u.id as user_id,
    u.email,

    COUNT(DISTINCT ds.id) as total_sessions,
    COUNT(DISTINCT df.id) as total_feedbacks,
    AVG(df.overall_satisfaction) as avg_satisfaction,

    SUM(ds.play_count) as total_plays,
    SUM(ds.total_listening_seconds) as total_listening_time,

    COUNT(DISTINCT uc.id) as context_count,
    COUNT(DISTINCT um.id) as memo_count,
    COUNT(DISTINCT ul.id) as link_count,

    MAX(ds.created_at) as last_session_at,
    MAX(u.last_login_at) as last_login_at
FROM users u
LEFT JOIN docent_sessions ds ON u.id = ds.user_id
LEFT JOIN docent_feedbacks df ON u.id = df.user_id
LEFT JOIN user_contexts uc ON u.id = uc.user_id
LEFT JOIN user_memos um ON u.id = um.user_id
LEFT JOIN user_links ul ON u.id = ul.user_id
GROUP BY u.id, u.email;

-- ============================================================================
-- SCHEMA MIGRATION VERSION TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(50) PRIMARY KEY,
    description TEXT,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Record initial schema
INSERT INTO schema_migrations (version, description)
VALUES ('2025_01_01_initial_schema', 'Initial db schema creation')
ON CONFLICT (version) DO NOTHING;

-- ============================================================================
-- COMPLETION MESSAGE
-- ============================================================================
DO $$
BEGIN
    RAISE NOTICE '✓ Aura db schema created successfully!';
    RAISE NOTICE 'Total tables: 13';
    RAISE NOTICE 'Total views: 3';
    RAISE NOTICE 'Database is ready to use.';
END $$;