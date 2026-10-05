package com.lawground.question.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_choices")
public class QuestionChoice {
    @EmbeddedId private QuestionChoiceId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("questionId")
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    protected QuestionChoice() {}

    public QuestionChoice(Question question, short number, String text) {
        this.question = question;
        this.id = new QuestionChoiceId(question.getId(), number);
        this.text = text;
    }

    public short getNumber() {
        return id.getNumber();
    }

    public String getText() {
        return text;
    }
}
