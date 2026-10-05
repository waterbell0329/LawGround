package com.lawground.question.dto;

import com.lawground.question.entity.Question;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        String subject,
        String stem,
        String questionType,
        List<Choice> choices,
        int providedAnswerNumber,
        LocalDate referenceDate,
        String visibility,
        String answerSource,
        long revision,
        Instant createdAt,
        Instant updatedAt) {
    public record Choice(int number, String text) {}

    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getSubject(),
                question.getStem(),
                question.getQuestionType(),
                question.getChoices().stream()
                        .map(choice -> new Choice(choice.getNumber(), choice.getText()))
                        .toList(),
                question.getProvidedAnswerNumber(),
                question.getReferenceDate(),
                question.getVisibility(),
                question.getAnswerSource(),
                question.getRevision(),
                question.getCreatedAt(),
                question.getUpdatedAt());
    }
}
