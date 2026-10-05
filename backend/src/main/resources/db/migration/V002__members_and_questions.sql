CREATE TABLE members (
    id uuid PRIMARY KEY,
    display_name varchar(80) NOT NULL CHECK (char_length(display_name) BETWEEN 1 AND 80 AND display_name ~ '[^[:space:]]'),
    status varchar(16) NOT NULL CHECK (status IN ('ACTIVE', 'DEACTIVATED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE questions (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL REFERENCES members(id) ON DELETE RESTRICT,
    subject varchar(32) NOT NULL CHECK (subject = 'BROKER_LAW'),
    stem text NOT NULL CHECK (char_length(stem) BETWEEN 1 AND 10000 AND stem ~ '[^[:space:]]'),
    question_type varchar(24) NOT NULL CHECK (question_type IN ('SELECT_CORRECT', 'SELECT_INCORRECT')),
    provided_answer_number smallint NOT NULL CHECK (provided_answer_number BETWEEN 1 AND 5),
    reference_date date NOT NULL,
    visibility varchar(16) NOT NULL CHECK (visibility IN ('PRIVATE', 'PUBLIC')),
    source_kind varchar(24) NOT NULL CHECK (source_kind IN ('USER_INPUT', 'EXAM_IMPORT')),
    answer_source varchar(24) NOT NULL CHECK (answer_source IN ('USER_PROVIDED', 'OFFICIAL_VERIFIED')),
    source_key varchar(200),
    source_url text,
    source_metadata jsonb CHECK (source_metadata IS NULL OR jsonb_typeof(source_metadata) = 'object'),
    content_hash char(64) NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    revision bigint NOT NULL DEFAULT 1 CHECK (revision >= 1),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT questions_source_policy CHECK (
        (source_kind = 'USER_INPUT' AND source_key IS NULL AND visibility = 'PRIVATE' AND answer_source = 'USER_PROVIDED')
        OR (source_kind = 'EXAM_IMPORT' AND source_key IS NOT NULL AND source_key ~ '[^[:space:]]'
            AND source_url IS NOT NULL AND source_url ~ '[^[:space:]]')
    ),
    CONSTRAINT questions_public_policy CHECK (visibility <> 'PUBLIC' OR (source_kind = 'EXAM_IMPORT' AND answer_source = 'OFFICIAL_VERIFIED'))
);

CREATE UNIQUE INDEX questions_active_owner_hash_uq ON questions(owner_id, content_hash) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX questions_active_source_key_uq ON questions(source_key) WHERE source_key IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX questions_owner_created_idx ON questions(owner_id, created_at DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX questions_public_created_idx ON questions(created_at DESC, id DESC) WHERE visibility = 'PUBLIC' AND deleted_at IS NULL;

CREATE TABLE question_choices (
    question_id uuid NOT NULL REFERENCES questions(id) ON DELETE RESTRICT,
    choice_number smallint NOT NULL CHECK (choice_number BETWEEN 1 AND 5),
    text text NOT NULL CHECK (char_length(text) BETWEEN 1 AND 2000 AND text ~ '[^[:space:]]'),
    PRIMARY KEY (question_id, choice_number)
);

-- Serialize mutations before changing children, including moves between parents.
CREATE FUNCTION lock_choice_parents() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        PERFORM id FROM questions WHERE id = NEW.question_id FOR UPDATE;
    ELSIF TG_OP = 'DELETE' THEN
        PERFORM id FROM questions WHERE id = OLD.question_id FOR UPDATE;
    ELSE
        PERFORM id FROM questions WHERE id IN (OLD.question_id, NEW.question_id) ORDER BY id FOR UPDATE;
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END;
$$;

CREATE TRIGGER question_choices_lock BEFORE INSERT OR UPDATE OR DELETE ON question_choices
    FOR EACH ROW EXECUTE FUNCTION lock_choice_parents();

CREATE FUNCTION assert_five_choices(question_uuid uuid) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    PERFORM id FROM questions WHERE id = question_uuid FOR UPDATE;
    IF FOUND AND (SELECT count(*) FROM question_choices WHERE question_id = question_uuid) <> 5 THEN
        RAISE EXCEPTION 'A question must retain exactly five choices'
            USING ERRCODE = '23514', CONSTRAINT = 'question_choices_exactly_five';
    END IF;
END;
$$;

CREATE FUNCTION enforce_five_choices() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_TABLE_NAME = 'questions' THEN
        PERFORM assert_five_choices(NEW.id);
    ELSE
        IF TG_OP <> 'INSERT' THEN PERFORM assert_five_choices(OLD.question_id); END IF;
        IF TG_OP <> 'DELETE' THEN PERFORM assert_five_choices(NEW.question_id); END IF;
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER questions_five_choices AFTER INSERT ON questions
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION enforce_five_choices();
CREATE CONSTRAINT TRIGGER question_choices_five_choices AFTER INSERT OR UPDATE OR DELETE ON question_choices
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION enforce_five_choices();
