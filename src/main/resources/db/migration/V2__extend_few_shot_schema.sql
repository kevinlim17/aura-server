-- Migration V2: Extend Few-Shot Learning Schema
-- Description: Create few_shot_examples table with extended schema and resource join table
-- Date: 2025-11-09

-- ============================================================================
-- 1. Create few_shot_examples table with full schema
-- ============================================================================

CREATE TABLE IF NOT EXISTS few_shot_examples (
    id SERIAL PRIMARY KEY,

    -- Core few-shot information
    artwork_id INTEGER NOT NULL REFERENCES artworks(id) ON DELETE CASCADE,
    user_context_summary TEXT NOT NULL,
    exemplar_text TEXT NOT NULL,

    -- Source tracking
    source_docent_session_id INTEGER REFERENCES docent_sessions(id),
    source_feedback_id INTEGER REFERENCES docent_feedbacks(id),

    -- Quality metrics
    quality_score DECIMAL(3,2) NOT NULL,
    user_rating INTEGER CHECK (user_rating >= 1 AND user_rating <= 5),
    effectiveness_score DECIMAL(5,2),
    diversity_score DECIMAL(5,2),

    -- Usage statistics
    usage_count INTEGER DEFAULT 0,
    last_used_at TIMESTAMP,

    -- Category
    category VARCHAR(50),

    -- Active status
    is_active BOOLEAN DEFAULT true,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Extended fields (new in V2)
    user_context_ids INTEGER[] DEFAULT '{}',
    user_link_ids INTEGER[] DEFAULT '{}',
    user_memo_ids INTEGER[] DEFAULT '{}',
    companion_input TEXT,
    prompt_metadata JSONB DEFAULT '{}'
);

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_few_shot_examples_artwork_id ON few_shot_examples(artwork_id);
CREATE INDEX IF NOT EXISTS idx_few_shot_examples_quality_score ON few_shot_examples(quality_score DESC);
CREATE INDEX IF NOT EXISTS idx_few_shot_examples_effectiveness_score ON few_shot_examples(effectiveness_score DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_few_shot_examples_diversity_score ON few_shot_examples(diversity_score DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_few_shot_examples_is_active ON few_shot_examples(is_active);

-- Add comments for columns
COMMENT ON COLUMN few_shot_examples.user_context_ids IS 'Array of user_context IDs used in this few-shot example';
COMMENT ON COLUMN few_shot_examples.user_link_ids IS 'Array of user_link IDs used in this few-shot example';
COMMENT ON COLUMN few_shot_examples.user_memo_ids IS 'Array of user_memo IDs used in this few-shot example';
COMMENT ON COLUMN few_shot_examples.companion_input IS 'Companion observation or input for this example';
COMMENT ON COLUMN few_shot_examples.prompt_metadata IS 'Metadata about how the prompt was constructed (JSONB)';
COMMENT ON COLUMN few_shot_examples.effectiveness_score IS 'Effectiveness score of this few-shot example (0-100)';
COMMENT ON COLUMN few_shot_examples.diversity_score IS 'Diversity score to ensure varied examples (0-100)';

-- ============================================================================
-- 2. Create few_shot_example_resources join table
-- ============================================================================

CREATE TABLE IF NOT EXISTS few_shot_example_resources (
    id SERIAL PRIMARY KEY,
    few_shot_example_id INTEGER NOT NULL REFERENCES few_shot_examples(id) ON DELETE CASCADE,
    resource_type VARCHAR(20) NOT NULL CHECK (resource_type IN ('CONTEXT', 'LINK', 'MEMO')),
    resource_id INTEGER NOT NULL,
    relevance_score DECIMAL(3,2) DEFAULT 0.0 CHECK (relevance_score >= 0 AND relevance_score <= 1.0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Ensure unique combinations
    UNIQUE (few_shot_example_id, resource_type, resource_id)
);

-- Add indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_few_shot_example_resources_example_id
    ON few_shot_example_resources(few_shot_example_id);

CREATE INDEX IF NOT EXISTS idx_few_shot_example_resources_type
    ON few_shot_example_resources(resource_type);

CREATE INDEX IF NOT EXISTS idx_few_shot_example_resources_resource
    ON few_shot_example_resources(resource_type, resource_id);

-- Add comments
COMMENT ON TABLE few_shot_example_resources IS 'Join table linking few-shot examples to their resources (contexts, links, memos)';
COMMENT ON COLUMN few_shot_example_resources.resource_type IS 'Type of resource: CONTEXT, LINK, or MEMO';
COMMENT ON COLUMN few_shot_example_resources.resource_id IS 'ID of the resource in its respective table';
COMMENT ON COLUMN few_shot_example_resources.relevance_score IS 'Relevance/contribution score of this resource (0.0-1.0)';

-- ============================================================================
-- 3. Record migration
-- ============================================================================

INSERT INTO schema_migrations (version, description)
VALUES ('V2__extend_few_shot_schema', 'Extended few_shot_examples table and created few_shot_example_resources join table')
ON CONFLICT (version) DO NOTHING;