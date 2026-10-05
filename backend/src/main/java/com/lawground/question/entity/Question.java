package com.lawground.question.entity;

import com.lawground.question.dto.CreateQuestionRequest;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "questions")
public class Question {
    @Id private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 32)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String stem;

    @Column(name = "question_type", nullable = false, length = 24)
    private String questionType;

    @Column(name = "provided_answer_number", nullable = false)
    private short providedAnswerNumber;

    @Column(name = "reference_date", nullable = false)
    private LocalDate referenceDate;

    @Column(nullable = false, length = 16)
    private String visibility;

    @Column(name = "source_kind", nullable = false, length = 24)
    private String sourceKind;

    @Column(name = "answer_source", nullable = false, length = 24)
    private String answerSource;

    @Column(name = "source_key", length = 200)
    private String sourceKey;

    @Column(name = "source_url", columnDefinition = "text")
    private String sourceUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_metadata", columnDefinition = "jsonb")
    private Map<String, Object> sourceMetadata;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "content_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    private String contentHash;

    @Column(nullable = false)
    private long revision;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id.number ASC")
    private List<QuestionChoice> choices = new ArrayList<>();

    protected Question() {}

    public Question(
            UUID id, UUID ownerId, CreateQuestionRequest request, String contentHash, Instant now) {
        this.id = id;
        this.ownerId = ownerId;
        subject = request.subject().name();
        stem = request.stem();
        questionType = request.questionType().name();
        providedAnswerNumber = request.providedAnswerNumber().shortValue();
        referenceDate = request.referenceDate();
        visibility = "PRIVATE";
        sourceKind = "USER_INPUT";
        answerSource = "USER_PROVIDED";
        this.contentHash = contentHash;
        revision = 1;
        createdAt = now;
        updatedAt = now;
        request.choices().stream()
                .sorted(java.util.Comparator.comparing(CreateQuestionRequest.Choice::number))
                .forEach(
                        choice ->
                                choices.add(
                                        new QuestionChoice(
                                                this,
                                                choice.number().shortValue(),
                                                choice.text())));
    }

    public UUID getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getStem() {
        return stem;
    }

    public String getQuestionType() {
        return questionType;
    }

    public short getProvidedAnswerNumber() {
        return providedAnswerNumber;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public String getVisibility() {
        return visibility;
    }

    public String getAnswerSource() {
        return answerSource;
    }

    public long getRevision() {
        return revision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<QuestionChoice> getChoices() {
        return List.copyOf(choices);
    }
}
