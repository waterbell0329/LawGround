package com.lawground.question.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class QuestionChoiceId implements Serializable {
    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "choice_number")
    private short number;

    protected QuestionChoiceId() {}

    public QuestionChoiceId(UUID questionId, short number) {
        this.questionId = questionId;
        this.number = number;
    }

    public short getNumber() {
        return number;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof QuestionChoiceId id
                && Objects.equals(questionId, id.questionId)
                && number == id.number;
    }

    @Override
    public int hashCode() {
        return Objects.hash(questionId, number);
    }
}
